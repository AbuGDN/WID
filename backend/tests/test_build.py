import json
from datetime import datetime, timezone
from pathlib import Path

from wid.build import CONFIG_DIR, build
from wid.fetch import canonical_url, parse_feed
from wid.keywords import Keywords

FIXTURES = Path(__file__).parent / "fixtures"
NOW = datetime(2026, 9, 24, 10, 0, tzinfo=timezone.utc)
SOURCES = [
    {"name": "IL News", "url": "x", "lang": "en", "weight": 1.2, "file": "israel_en.xml"},
    {"name": "Mundo BR", "url": "x", "lang": "pt", "weight": 1.0, "file": "mundo_pt.xml"},
    {"name": "Intl", "url": "x", "lang": "en", "weight": 1.0, "file": "intl_en.xml"},
]


def kw():
    return Keywords.load(CONFIG_DIR / "keywords.yaml")


def fetched():
    out = []
    for s in SOURCES:
        out += parse_feed((FIXTURES / s["file"]).read_bytes(), s, NOW)
    return out


def test_parse_feed_extracts_fields():
    arts = parse_feed((FIXTURES / "israel_en.xml").read_bytes(), SOURCES[0], NOW)
    first = arts[0]
    assert first.title == "Israel strikes Hezbollah targets in Beirut"
    assert first.url == "https://example-il.com/beirut-strike"
    assert first.summary.startswith("The IDF said")
    assert first.image == "https://example-il.com/img/beirut.jpg"
    pt = parse_feed((FIXTURES / "mundo_pt.xml").read_bytes(), SOURCES[1], NOW)
    assert pt[0].image == "https://example-br.com/f.jpg"


def test_canonical_url_keeps_real_params():
    assert canonical_url("https://a.com/x?utm_source=r&id=5&fbclid=z#frag") == "https://a.com/x?id=5"


def test_relevance_filter():
    k = kw()
    assert k.match("Israel strikes Hezbollah targets in Beirut", "").relevant
    assert not k.match("Tel Aviv stock exchange closes higher", "Markets rallied").relevant
    # "ataque" no futebol não tem região nem outro termo de guerra
    assert not k.match("Seleção vence amistoso em São Paulo", "Ataque brilhou no segundo tempo.").relevant
    m = k.match("Mísseis do Irã atingem Tel Aviv", "")
    assert m.relevant and {"ira", "israel"} <= m.tags


def test_build_clusters_across_languages_and_picks_portuguese_title(tmp_path):
    feed = build(tmp_path, NOW, SOURCES, kw(), fetched(), {})
    titles = [c["title"] for c in feed["clusters"]]
    assert "Tel Aviv stock exchange closes higher" not in titles
    assert "Seleção vence amistoso em São Paulo" not in titles
    assert len(feed["clusters"]) == 2

    top = feed["top_of_day"]
    assert top["sources_count"] == 3
    assert top["title"] == "Israel ataca alvos do Hezbollah em Beirute"
    assert top["lang"] == "pt"
    assert {"israel", "libano"} <= set(top["tags"])

    written = json.loads((tmp_path / "top.json").read_text())
    assert written["top"]["id"] == top["id"]
    assert written["secondary"][0]["title"].startswith("Ukraine says")
    assert (tmp_path / "history" / "2026-09-24.json").exists()
    assert json.loads((tmp_path / "history" / "index.json").read_text())["days"] == ["2026-09-24"]


def test_previous_articles_survive_next_run(tmp_path):
    build(tmp_path, NOW, SOURCES, kw(), fetched(), {})
    # Próxima rodada: feeds não trazem mais nada, mas as últimas 48h continuam.
    feed = build(tmp_path, NOW, SOURCES, kw(), [], {})
    assert feed["top_of_day"]["sources_count"] == 3
    assert len(feed["clusters"]) == 2


def test_urgent_flag(tmp_path):
    k = kw()
    arts = []
    for i in range(5):
        src = {"name": f"S{i}", "lang": "en", "weight": 1.0}
        xml = f"""<rss><channel><item><title>Iran fires missiles at Israel, sirens across Tel Aviv</title>
        <link>https://s{i}.com/a</link><pubDate>Thu, 24 Sep 2026 09:{10 + i}:00 GMT</pubDate></item></channel></rss>"""
        arts += parse_feed(xml.encode(), src, NOW)
    feed = build(tmp_path, NOW, [], k, arts, {})
    assert feed["clusters"][0]["urgent"] is True
