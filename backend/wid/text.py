"""Limpeza de texto e tokenização usadas no filtro e no agrupamento."""

import html
import re
import unicodedata

_TAG_RE = re.compile(r"<[^>]+>")
_WS_RE = re.compile(r"\s+")
_WORD_RE = re.compile(r"[a-z0-9]+")

STOPWORDS = set(
    """
    a o as os um uma uns umas de do da dos das em no na nos nas por pelo pela pelos
    pelas para pra com sem sob sobre entre e ou mas que se ao aos à às é foi sao ser
    diz dizem apos contra como mais menos ja nao sim seu sua seus suas ele ela eles
    elas isso esta este essa esse ha tem vai apos ate onde quando quem qual
    the an of to in on at for from by with and or but is are was were be been has
    have had it its this that these those as after over into amid says said say new
    will would could can may not no more than about against during out up
    """.split()
)

# Equivalências PT/EN (já sem acento, depois do corte de plural) para que a
# mesma história em idiomas diferentes compartilhe tokens.
CANON = {
    "beirute": "beirut", "teera": "tehran", "ira": "iran", "irania": "iran", "iraniano": "iran",
    "iranian": "iran", "siria": "syria", "syrian": "syria", "libano": "lebanon",
    "lebanese": "lebanon", "iemen": "yemen", "yemeni": "yemen", "ucrania": "ukraine",
    "ukrainian": "ukraine", "russo": "russia", "russian": "russia",
    "israelense": "israel", "israeli": "israel", "palestino": "palestinian",
    "cisjordania": "westbank", "damasco": "damascus", "iraque": "iraq", "bagda": "baghdad",
    "sudao": "sudan", "egito": "egypt", "catar": "qatar", "arabia": "saudi", "saudita": "saudi",
    "eua": "usa", "otan": "nato", "atinge": "hit", "atingem": "hit", "atingiu": "hit",
    "ataca": "attack", "ataque": "attack", "atacou": "attack", "atacam": "attack",
    "strike": "attack", "airstrike": "attack", "bombardeio": "attack", "bombardeia": "attack",
    "bombardeou": "attack", "missil": "missile", "missei": "missile",
    "foguete": "rocket", "cessar": "ceasefire",
    "tregua": "ceasefire", "truce": "ceasefire", "refen": "hostage", "refem": "hostage",
    "morto": "killed", "morte": "killed", "mata": "killed",
    "matou": "killed", "kill": "killed", "dead": "killed", "tropa": "troop", "exercito": "army",
    "soldado": "soldier", "alvo": "target", "escola": "school",
    "acordo": "deal", "agreement": "deal", "negociacao": "talk", "negotiation": "talk",
    "fronteira": "border", "sul": "south", "norte": "north", "lider": "leader",
    "chefe": "leader", "comandante": "commander", "filho": "son", "filha": "daughter",
    "embaixador": "ambassador", "ferido": "hurt", "wounded": "hurt", "injured": "hurt",
    "presidente": "president", "primeiro": "prime", "ministro": "minister",
    "civi": "civilian", "civil": "civilian", "paquistao": "pakistan", "afeganistao": "afghanistan",
    "discurso": "speech", "onu": "un",
}

# Expressões de várias palavras viram um token só antes da tokenização.
PHRASES = [
    (re.compile(r"\bwest bank\b"), "westbank"),
    (re.compile(r"\bestados unidos\b|\bu\.s\.(?=\W|$)|\bu\.s\b"), "usa"),
    (re.compile(r"\bunited states\b"), "usa"),
    (re.compile(r"\bnações unidas\b|\bunited nations\b"), "un"),
    (re.compile(r"\bfaixa de gaza\b|\bgaza strip\b"), "gaza"),
]


def clean_html(raw: str | None) -> str:
    if not raw:
        return ""
    text = _TAG_RE.sub(" ", raw)
    text = html.unescape(text)
    return _WS_RE.sub(" ", text).strip()


def truncate(text: str, limit: int) -> str:
    if len(text) <= limit:
        return text
    cut = text[:limit].rsplit(" ", 1)[0]
    return cut.rstrip(".,;:") + "…"


def strip_accents(text: str) -> str:
    decomposed = unicodedata.normalize("NFKD", text)
    return "".join(c for c in decomposed if not unicodedata.combining(c))


def tokens(text: str) -> set[str]:
    """Tokens significativos, sem acento, com um plural simples removido.

    Nomes próprios (Netanyahu, Gaza, Hezbollah) sobrevivem igual em PT e EN,
    o que permite agrupar a mesma história entre idiomas.
    """
    text = text.lower()
    for rx, repl in PHRASES:
        text = rx.sub(repl, text)
    out = set()
    for word in _WORD_RE.findall(strip_accents(text)):
        if word in ("un", "usa"):
            out.add(word)
            continue
        if len(word) < 3 or word in STOPWORDS or word.isdigit():
            continue
        if len(word) > 4 and word.endswith("s"):
            word = word[:-1]
        out.add(CANON.get(word, word))
    return out
