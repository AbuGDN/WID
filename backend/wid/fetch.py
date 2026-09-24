"""Download e parsing dos feeds RSS."""

import calendar
import hashlib
import logging
import re
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime, timezone
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

import feedparser
import httpx

from .text import clean_html, truncate

log = logging.getLogger(__name__)

USER_AGENT = "Mozilla/5.0 (compatible; WID-news-bot/1.0; uso pessoal)"
TIMEOUT = 20.0
_IMG_RE = re.compile(r"<img[^>]+src=[\"']([^\"']+)[\"']", re.I)
_TRACKING_PARAMS = re.compile(r"^(utm_|at_|cmpid$|ocid$|fbclid$|gclid$)")


@dataclass
class Article:
    id: str
    title: str
    summary: str
    url: str
    source: str
    lang: str
    weight: float
    published: datetime
    image: str | None = None

    def to_json(self) -> dict:
        return {
            "id": self.id,
            "title": self.title,
            "summary": self.summary,
            "url": self.url,
            "source": self.source,
            "lang": self.lang,
            "published": iso(self.published),
            "image": self.image,
        }

    @classmethod
    def from_json(cls, data: dict, weight: float = 1.0) -> "Article":
        return cls(
            id=data["id"],
            title=data["title"],
            summary=data.get("summary", ""),
            url=data["url"],
            source=data["source"],
            lang=data.get("lang", "en"),
            weight=weight,
            published=parse_iso(data["published"]),
            image=data.get("image"),
        )


def iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_iso(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def canonical_url(url: str) -> str:
    parts = urlsplit(url.strip())
    query = [(k, v) for k, v in parse_qsl(parts.query, keep_blank_values=True) if not _TRACKING_PARAMS.match(k)]
    return urlunsplit((parts.scheme, parts.netloc, parts.path, urlencode(query), ""))


def article_id(url: str) -> str:
    return hashlib.sha1(canonical_url(url).encode()).hexdigest()[:12]


def _entry_time(entry, now: datetime) -> datetime:
    for key in ("published_parsed", "updated_parsed"):
        parsed = entry.get(key)
        if parsed:
            dt = datetime.fromtimestamp(calendar.timegm(parsed), tz=timezone.utc)
            # Alguns feeds publicam horário no futuro (fuso errado).
            return min(dt, now)
    return now


def _entry_image(entry) -> str | None:
    for key in ("media_content", "media_thumbnail"):
        for media in entry.get(key) or []:
            if media.get("url") and media.get("medium", "image") == "image":
                return media["url"]
    for link in entry.get("links") or []:
        if link.get("rel") == "enclosure" and str(link.get("type", "")).startswith("image"):
            return link.get("href")
    raw = entry.get("summary", "")
    for content in entry.get("content") or []:
        raw += content.get("value", "")
    m = _IMG_RE.search(raw)
    return m.group(1) if m else None


def parse_feed(data: bytes, source: dict, now: datetime) -> list[Article]:
    feed = feedparser.parse(data)
    articles = []
    for entry in feed.entries:
        url = entry.get("link")
        title = clean_html(entry.get("title"))
        if not url or not title:
            continue
        summary = clean_html(entry.get("summary") or entry.get("description"))
        if summary.startswith(title):
            summary = summary[len(title):].strip(" -–:")
        articles.append(
            Article(
                id=article_id(url),
                title=title,
                summary=truncate(summary, 500),
                url=canonical_url(url),
                source=source["name"],
                lang=source.get("lang", "en"),
                weight=float(source.get("weight", 1.0)),
                published=_entry_time(entry, now),
                image=_entry_image(entry),
            )
        )
    return articles


def fetch_source(client: httpx.Client, source: dict, now: datetime) -> tuple[list[Article], str | None]:
    try:
        resp = client.get(source["url"])
        resp.raise_for_status()
        articles = parse_feed(resp.content, source, now)
        if not articles:
            return [], "feed vazio ou inválido"
        return articles, None
    except Exception as exc:  # uma fonte quebrada não pode derrubar as outras
        log.warning("falha em %s: %s", source["name"], exc)
        return [], f"{type(exc).__name__}: {exc}"[:200]


def fetch_all(sources: list[dict], now: datetime) -> tuple[list[Article], dict]:
    status = {}
    articles: list[Article] = []
    headers = {"User-Agent": USER_AGENT, "Accept": "application/rss+xml, application/xml, text/xml, */*"}
    with httpx.Client(headers=headers, timeout=TIMEOUT, follow_redirects=True) as client:
        with ThreadPoolExecutor(max_workers=8) as pool:
            results = pool.map(lambda s: fetch_source(client, s, now), sources)
            for source, (items, error) in zip(sources, results):
                articles.extend(items)
                status[source["name"]] = {"ok": error is None, "items": len(items), "error": error}
    return articles, status
