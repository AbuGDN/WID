"""Download e parsing dos feeds RSS."""

import calendar
import hashlib
import logging
import re
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field
from datetime import datetime, timezone
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

import feedparser
import httpx

from .text import clean_html, clean_summary, truncate

log = logging.getLogger(__name__)

# Vários sites (Times of Israel, Al-Monitor) recusam user-agents de robô.
USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/128.0 Safari/537.36"
)
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
    origin: str = "internacional"
    # Manchetes anteriores deste mesmo link: [{"title": antiga, "at": quando mudou}].
    edits: list[dict] = field(default_factory=list)

    def to_json(self) -> dict:
        data = {
            "id": self.id,
            "title": self.title,
            "summary": self.summary,
            "url": self.url,
            "source": self.source,
            "lang": self.lang,
            "published": iso(self.published),
            "image": self.image,
            "origin": self.origin,
        }
        if self.edits:
            data["edits"] = self.edits
        return data

    @classmethod
    def from_json(cls, data: dict, weight: float = 1.0, origin: str | None = None) -> "Article":
        url = canonical_url(data["url"])
        return cls(
            id=article_id(url),
            title=data["title"],
            summary=clean_summary(data.get("summary", "")),
            url=url,
            source=data["source"],
            lang=data.get("lang", "en"),
            weight=weight,
            published=parse_iso(data["published"]),
            image=data.get("image"),
            origin=origin or data.get("origin", "internacional"),
            edits=data.get("edits", []),
        )


def iso(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_iso(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def canonical_url(url: str) -> str:
    # Redirecionadores que embrulham o link real, ex. redir.folha.com.br/.../*https://www1.folha...
    wrapped = url.find("/*http")
    if wrapped != -1:
        url = url[wrapped + 2:]
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
        summary = clean_summary(clean_html(entry.get("summary") or entry.get("description")))
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
                origin=source.get("origin", "internacional"),
            )
        )
    return articles


def _from_google_news(articles: list[Article]) -> None:
    """Google News põe " - Nome do Veículo" no fim do título e o resumo é só links."""
    for art in articles:
        head, sep, _ = art.title.rpartition(" - ")
        if sep and head:
            art.title = head
        art.summary = ""


def fetch_source(client: httpx.Client, source: dict, now: datetime) -> tuple[list[Article], str | None]:
    """Tenta cada URL da fonte (`url` pode ser uma lista de alternativas)."""
    urls = source["url"] if isinstance(source["url"], list) else [source["url"]]
    errors = []
    for url in urls:
        try:
            resp = client.get(url)
            resp.raise_for_status()
            articles = parse_feed(resp.content, source, now)
            if "news.google.com" in url:
                _from_google_news(articles)
            if articles:
                return articles, None
            errors.append(f"{url}: feed vazio ou inválido")
        except Exception as exc:  # uma fonte quebrada não pode derrubar as outras
            errors.append(f"{url}: {type(exc).__name__} {getattr(getattr(exc, 'response', None), 'status_code', '')}".strip())
    log.warning("falha em %s: %s", source["name"], errors)
    return [], " | ".join(errors)[:400]


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
