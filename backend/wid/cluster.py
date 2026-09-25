"""Agrupa a mesma história de veículos diferentes e calcula os scores."""

from dataclasses import dataclass, field
from datetime import datetime, timedelta

from .fetch import Article, iso
from .keywords import Keywords
from .text import tokens

MERGE_WINDOW = timedelta(hours=18)
MIN_SHARED = 3
MIN_OVERLAP = 0.5
HOT_HALF_LIFE_H = 6.0


@dataclass
class Cluster:
    articles: list[Article] = field(default_factory=list)
    token_sets: list[set[str]] = field(default_factory=list)
    tags: set[str] = field(default_factory=set)
    relevance: int = 0
    urgent_term: bool = False

    @property
    def first(self) -> Article:
        return min(self.articles, key=lambda a: a.published)

    @property
    def start(self) -> datetime:
        return self.first.published

    @property
    def latest(self) -> datetime:
        return max(a.published for a in self.articles)

    @property
    def id(self) -> str:
        return self.first.id

    @property
    def sources(self) -> set[str]:
        return {a.source for a in self.articles}

    def lead(self) -> Article:
        """Artigo que dá título ao grupo: português primeiro, depois link direto (não Google
        News, que não tem resumo nem permite baixar o texto), depois peso da fonte."""
        return max(
            self.articles,
            key=lambda a: (a.lang == "pt", "news.google.com" not in a.url, a.weight, -a.published.timestamp()),
        )

    def similarity(self, toks: set[str], published: datetime) -> bool:
        if abs(published - self.start) > MERGE_WINDOW:
            return False
        for other in self.token_sets:
            shared = len(toks & other)
            if shared >= MIN_SHARED and shared / min(len(toks), len(other)) >= MIN_OVERLAP:
                return True
        return False


def build_clusters(articles: list[Article], kw: Keywords) -> list[Cluster]:
    clusters: list[Cluster] = []
    for art in sorted(articles, key=lambda a: a.published):
        match = kw.match(art.title, art.summary)
        toks = tokens(art.title)
        target = next((c for c in clusters if c.similarity(toks, art.published)), None)
        if target is None:
            target = Cluster()
            clusters.append(target)
        target.articles.append(art)
        target.token_sets.append(toks)
        target.tags |= match.tags
        target.relevance = max(target.relevance, match.score)
        target.urgent_term |= match.urgent
    return clusters


def base_score(c: Cluster, kw: Keywords) -> float:
    source_weight = sum(max(a.weight for a in c.articles if a.source == s) for s in c.sources)
    return source_weight * (1 + min(c.relevance, 10) / 10) * kw.boost_for(c.tags)


def hot_score(c: Cluster, kw: Keywords, now: datetime) -> float:
    age_h = (now - c.latest).total_seconds() / 3600
    return base_score(c, kw) * 0.5 ** (age_h / HOT_HALF_LIFE_H)


def is_urgent(c: Cluster, now: datetime, min_sources: int, window: timedelta) -> bool:
    """História nova que muitos veículos cobriram rapidamente."""
    if now - c.start > window:
        return False
    early = {a.source for a in c.articles if a.published - c.start <= window}
    return len(early) >= min_sources or (c.urgent_term and len(early) >= 2)


def cluster_json(c: Cluster, kw: Keywords, now: datetime, urgent: bool) -> dict:
    lead = c.lead()
    image = lead.image or next((a.image for a in c.articles if a.image), None)
    return {
        "id": c.id,
        "title": lead.title,
        "summary": lead.summary,
        "url": lead.url,
        "source": lead.source,
        "lang": lead.lang,
        "image": image,
        "published": iso(c.start),
        "updated": iso(c.latest),
        "tags": sorted(c.tags),
        "sources_count": len(c.sources),
        "score": round(hot_score(c, kw, now), 3),
        "day_score": round(base_score(c, kw), 3),
        "urgent": urgent,
        "articles": [a.to_json() for a in sorted(c.articles, key=lambda a: a.published, reverse=True)],
    }
