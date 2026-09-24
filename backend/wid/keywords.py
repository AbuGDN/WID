"""Filtro de relevância (é notícia de guerra?) e tags por região."""

import re
from dataclasses import dataclass, field
from pathlib import Path

import yaml


def _compile(terms: list[str]) -> list[tuple[str, re.Pattern]]:
    compiled = []
    for term in terms:
        term = term.lower()
        if term.endswith("*"):
            pattern = r"(?<!\w)" + re.escape(term[:-1]) + r"\w*"
        else:
            pattern = r"(?<!\w)" + re.escape(term) + r"(?!\w)"
        compiled.append((term, re.compile(pattern)))
    return compiled


@dataclass
class Match:
    war_terms: set[str] = field(default_factory=set)
    tags: set[str] = field(default_factory=set)
    urgent: bool = False

    @property
    def relevant(self) -> bool:
        if not self.war_terms:
            return False
        return bool(self.tags) or len(self.war_terms) >= 2

    @property
    def score(self) -> int:
        return 2 * len(self.war_terms) + len(self.tags)


class Keywords:
    def __init__(self, config: dict):
        self.war = _compile(config["war_terms"])
        self.tags = {tag: _compile(terms) for tag, terms in config["tags"].items()}
        self.boost: dict[str, float] = config.get("boost", {})
        self.urgent = _compile(config.get("urgent_terms", []))

    @classmethod
    def load(cls, path: Path) -> "Keywords":
        return cls(yaml.safe_load(path.read_text(encoding="utf-8")))

    def match(self, title: str, summary: str) -> Match:
        text = f"{title}\n{summary}".lower()
        m = Match()
        for term, rx in self.war:
            if rx.search(text):
                m.war_terms.add(term.rstrip("*"))
        for tag, terms in self.tags.items():
            if any(rx.search(text) for _, rx in terms):
                m.tags.add(tag)
        lowered_title = title.lower()
        m.urgent = any(rx.search(lowered_title) for _, rx in self.urgent)
        return m

    def boost_for(self, tags) -> float:
        return max((self.boost.get(t, 1.0) for t in tags), default=1.0)
