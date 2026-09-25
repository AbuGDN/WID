"""Gera os JSONs publicados: feed.json, top.json, history/ e sources_status.json.

Uso: python -m wid.build --out ../site
"""

import argparse
import json
import logging
from datetime import datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

import yaml

from .cluster import build_clusters, cluster_json, is_urgent
from .fetch import Article, fetch_all, iso, parse_iso
from .keywords import Keywords

log = logging.getLogger("wid")

CONFIG_DIR = Path(__file__).resolve().parent.parent / "config"
LOCAL_TZ = ZoneInfo("America/Sao_Paulo")
KEEP_WINDOW = timedelta(hours=48)
TOP_WINDOW = timedelta(hours=24)
URGENT_WINDOW = timedelta(hours=2)
URGENT_MIN_SOURCES = 5
SECONDARY_COUNT = 4
STATS_DAYS = 30


def load_previous(out: Path, weights: dict[str, float], origins: dict[str, str]) -> list[Article]:
    path = out / "feed.json"
    if not path.exists():
        return []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        log.warning("feed.json anterior ilegível: %s", exc)
        return []
    return [
        Article.from_json(a, weights.get(a["source"], 1.0), origins.get(a["source"]))
        for c in data.get("clusters", [])
        for a in c.get("articles", [])
    ]


def merge(previous: list[Article], fresh: list[Article]) -> list[Article]:
    """Une por id. O registro antigo vence, para o horário de publicação ficar estável."""
    by_id: dict[str, Article] = {}
    for art in fresh:
        if art.id not in by_id or art.weight > by_id[art.id].weight:
            by_id[art.id] = art
    for art in previous:
        by_id[art.id] = art
    return list(by_id.values())


def write_json(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    tmp.replace(path)


def write_stats(out: Path, today, started_today: list[dict]) -> None:
    """stats/daily.json: quantas histórias começaram em cada dia, por região (últimos 30 dias)."""
    path = out / "stats" / "daily.json"
    try:
        days = {d["date"]: d for d in json.loads(path.read_text(encoding="utf-8"))["days"]}
    except (OSError, ValueError, KeyError):
        days = {}
    counts: dict[str, int] = {}
    for c in started_today:
        for tag in c["tags"]:
            counts[tag] = counts.get(tag, 0) + 1
    days[today.isoformat()] = {"date": today.isoformat(), "total": len(started_today), "counts": counts}
    keep = sorted(days)[-STATS_DAYS:]
    write_json(path, {"days": [days[d] for d in keep]})


def build(out: Path, now: datetime, sources: list[dict], kw: Keywords, fetched: list[Article], status: dict) -> dict:
    weights = {s["name"]: float(s.get("weight", 1.0)) for s in sources}
    origins = {s["name"]: s["origin"] for s in sources if "origin" in s}
    articles = merge(load_previous(out, weights, origins), fetched)
    articles = [
        a for a in articles
        if now - a.published <= KEEP_WINDOW and kw.match(a.title, a.summary).relevant
    ]

    clusters = build_clusters(articles, kw)
    items = [
        cluster_json(c, kw, now, is_urgent(c, now, URGENT_MIN_SOURCES, URGENT_WINDOW))
        for c in clusters
    ]
    items.sort(key=lambda c: c["score"], reverse=True)

    recent = [c for c in items if now - parse_iso(c["updated"]) <= TOP_WINDOW]
    recent.sort(key=lambda c: c["day_score"], reverse=True)
    top = recent[0] if recent else None

    feed = {"version": 1, "generated_at": iso(now), "top_of_day": top, "clusters": items}
    write_json(out / "feed.json", feed)
    write_json(out / "top.json", {
        "version": 1,
        "generated_at": iso(now),
        "top": top,
        "secondary": [
            {k: c[k] for k in ("id", "title", "source", "lang", "sources_count", "published", "url")}
            for c in recent[1:1 + SECONDARY_COUNT]
        ],
    })

    # Principal de cada dia (fuso de Brasília). Reescrito a cada rodada; a última do dia fica.
    today = now.astimezone(LOCAL_TZ).date()
    started_today = [c for c in items if parse_iso(c["published"]).astimezone(LOCAL_TZ).date() == today]
    if started_today:
        best = max(started_today, key=lambda c: c["day_score"])
        write_json(out / "history" / f"{today.isoformat()}.json", {"date": today.isoformat(), "top": best})
    days = sorted((p.stem for p in (out / "history").glob("*.json") if p.stem != "index"), reverse=True)
    write_json(out / "history" / "index.json", {"days": days})

    write_stats(out, today, started_today)

    write_json(out / "sources_status.json", {"generated_at": iso(now), "sources": status})
    (out / ".nojekyll").touch()
    return feed


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, required=True, help="pasta do site (branch gh-pages)")
    parser.add_argument("--config", type=Path, default=CONFIG_DIR)
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")

    sources = yaml.safe_load((args.config / "sources.yaml").read_text(encoding="utf-8"))["sources"]
    kw = Keywords.load(args.config / "keywords.yaml")
    now = datetime.now(timezone.utc).replace(microsecond=0)

    fetched, status = fetch_all(sources, now)
    ok = sum(1 for s in status.values() if s["ok"])
    log.info("%d/%d fontes ok, %d artigos baixados", ok, len(sources), len(fetched))
    if ok == 0:
        raise SystemExit("nenhuma fonte respondeu; mantendo o feed anterior")

    args.out.mkdir(parents=True, exist_ok=True)
    feed = build(args.out, now, sources, kw, fetched, status)
    top = feed["top_of_day"]
    log.info("%d histórias; principal: %s", len(feed["clusters"]), top["title"] if top else "—")


if __name__ == "__main__":
    main()
