import json
from datetime import datetime, timedelta, timezone

from wid.analysis import (
    cluster_figures,
    cluster_framing,
    cluster_sides,
    extract_figures,
    region_tension,
    update_first,
    update_sagas,
)
from wid.fetch import iso

NOW = datetime(2026, 9, 25, 12, 0, tzinfo=timezone.utc)


def art(source, title, summary="", origin="internacional", minutes_ago=60):
    return {
        "id": f"{source}-{hash(title) % 10000}", "source": source, "title": title, "summary": summary,
        "origin": origin, "published": iso(NOW - timedelta(minutes=minutes_ago)), "lang": "en", "url": "u",
    }


def cluster(cid, title, articles, tags=("israel",), published=None, urgent=False):
    return {
        "id": cid, "title": title, "summary": "", "lang": "en", "source": articles[0]["source"], "url": "u",
        "published": published or min(a["published"] for a in articles),
        "updated": max(a["published"] for a in articles),
        "tags": list(tags), "sources_count": len({a["source"] for a in articles}),
        "urgent": urgent, "articles": articles,
    }


def test_extract_figures_pt_en():
    assert extract_figures("Russian attacks kill 10, injure 61 across Ukraine") == {"killed": 10, "injured": 61}
    assert extract_figures("Ataque deixa pelo menos 14 mortos e 30 feridos em Kiev") == {"killed": 14, "injured": 30}
    assert extract_figures("Death toll rises to 1,200 after strike") == {"killed": 1200}
    # Anos e idades não contam.
    assert extract_figures("Em 2026, garoto de 14 anos morre") == {}


def test_divergent_figures():
    c = cluster("c1", "t", [
        art("Kyiv Independent", "Russian attacks kill 10 across Ukraine"),
        art("Times of Israel", "Russian strike kills 14 in Kyiv"),
        art("G1", "Ataque russo em Kiev", ""),
    ])
    figs = cluster_figures(c)
    assert figs["killed"]["by_source"] == {"Kyiv Independent": 10, "Times of Israel": 14}
    assert figs["killed"]["divergent"] is True


def test_framing_by_origin():
    c = cluster("c2", "t", [
        art("JPost", "IDF kills Hamas terrorists in Gaza operation", origin="israel"),
        art("Al Jazeera", "Israeli attack kills Palestinian fighters in Gaza", origin="arabe"),
    ])
    groups = {g["group"]: g["by_origin"] for g in cluster_framing(c)}
    assert groups["Como chamam os grupos armados"] == {"arabe": ["combatentes"], "israel": ["terroristas"]}
    assert groups["Como chamam a ação militar"] == {"arabe": ["ataque"], "israel": ["operação"]}


def test_tension_and_spike():
    history = [{"date": f"2026-09-{d:02d}", "total": 2, "counts": {"libano": 2}} for d in range(11, 25)]
    items = [
        cluster(f"l{i}", "Israel launches missile barrage on Lebanon", [art(f"S{j}", "missile") for j in range(5)],
                tags=("libano",), published=iso(NOW - timedelta(hours=1, minutes=i)), urgent=i == 0)
        for i in range(4)
    ]
    result = region_tension(items, history, "2026-09-25", NOW)["libano"]
    assert result["spike"] is True and result["spike_ratio"] >= 3
    assert result["tension"] >= 50 and result["level"] in ("alta", "crítica")


def test_no_alarm_without_history():
    items = [cluster("x", "Troops clash", [art("A", "troops")], tags=("sudao",))]
    result = region_tension(items, [], "2026-09-25", NOW)["sudao"]
    assert result["spike"] is False


def test_sagas_link_days(tmp_path):
    day1 = cluster("d1", "Iran proposes ceasefire plan with Israel", [
        art("A", "Iran proposes ceasefire plan with Israel", minutes_ago=60 * 26),
        art("B", "Iran ceasefire plan proposed to Israel", minutes_ago=60 * 25),
    ])
    update_sagas(tmp_path, [day1], NOW - timedelta(hours=24))
    day2 = cluster("d2", "Israel rejects Iran ceasefire plan", [
        art("C", "Israel rejects Iran ceasefire plan"),
        art("D", "Israel says no to Iranian ceasefire plan"),
    ])
    items = [day2]
    update_sagas(tmp_path, items, NOW)
    saga = items[0]["saga"]
    assert saga["chapter"] == 2 and saga["total"] == 2
    assert [ch["cluster_id"] for ch in saga["chapters"]] == ["d1", "d2"]


def test_first_reporter(tmp_path):
    items = [cluster("big", "t", [
        art("Times of Israel", "x", minutes_ago=90),
        art("BBC World", "x", minutes_ago=60),
        art("G1", "x", minutes_ago=30),
    ])]
    data = update_first(tmp_path, items, NOW)
    top = data["ranking"][0]
    assert top["source"] == "Times of Israel" and top["firsts"] == 1 and top["lead_min"] == 30
    assert json.loads((tmp_path / "stats" / "first.json").read_text())["big_stories"] == 1


def test_ages_are_not_casualties_and_overlapping_terms_are_not_framing():
    assert extract_figures("Russian drone attack kills 14-year-old boy in Kyiv") == {}
    c = cluster("c3", "t", [
        art("G1", "Ataques russos em operação no leste", origin="brasil"),
        art("BBC World", "Russian attacks in the east", origin="internacional"),
    ])
    assert cluster_framing(c) == []


def test_sides_opposed_and_one_side():
    both = {"articles": [art("Al Jazeera", "t", origin="arabe"), art("Ynetnews", "t", origin="israel")]}
    assert cluster_sides(both) == "opostos"
    usa = {"articles": [art("Al Arabiya", "t", origin="arabe"), art("CNN", "t", origin="eua")]}
    assert cluster_sides(usa) == "opostos"
    one = {"articles": [art(s, "t", origin="israel") for s in ("Ynet", "Walla", "Maariv")]}
    assert cluster_sides(one) == "um_lado"
    small = {"articles": [art(s, "t", origin="israel") for s in ("Ynet", "Walla")]}
    assert cluster_sides(small) is None
    neutral = {"articles": [art(s, "t", origin="brasil") for s in ("G1", "Folha", "Estadão")]}
    assert cluster_sides(neutral) is None
