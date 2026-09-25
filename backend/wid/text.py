"""Limpeza de texto e tokenização usadas no filtro e no agrupamento."""

import html
import re
import unicodedata

_TAG_RE = re.compile(r"<[^>]+>")
_WS_RE = re.compile(r"\s+")
_WORD_RE = re.compile(r"[a-z0-9]+|[\u05d0-\u05ea]+|[\u0621-\u064a]+")
_HEBREW_RE = re.compile(r"[\u05d0-\u05ea]")
_ARABIC_RE = re.compile(r"[\u0621-\u064a]")

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
    "discurso": "speech", "onu": "un", "americano": "usa", "americana": "usa", "american": "usa",
    "pentagono": "pentagon",
}

# Expressões de várias palavras viram um token só antes da tokenização.
PHRASES = [
    (re.compile(r"\bwest bank\b"), "westbank"),
    (re.compile(r"\bestados unidos\b|\bu\.s\.(?=\W|$)|\bu\.s\b"), "usa"),
    (re.compile(r"\bunited states\b"), "usa"),
    (re.compile(r"\bcasa branca\b|\bwhite house\b"), "whitehouse"),
    (re.compile(r"\bnações unidas\b|\bunited nations\b"), "un"),
    (re.compile(r"\bfaixa de gaza\b|\bgaza strip\b"), "gaza"),
]


def strip_accents(text: str) -> str:
    decomposed = unicodedata.normalize("NFKD", text)
    return "".join(c for c in decomposed if not unicodedata.combining(c))


# --- Hebraico e árabe ---
# Prefixos colados na palavra: hebraico ו ה ב ל מ ש כ ("בעזה" = "em Gaza"); árabe و ف ب ل ك + ال.
HE_PREFIXES = "והבלמשכ"
AR_PREFIXES = "وفبلك"

# Já normalizados (sem aspas de sigla, sem hamza), sem prefixo. Mapeiam para os tokens em
# inglês/português para que a mesma história agrupe entre os idiomas.
CANON_RTL = {
    # hebraico
    "ישראל": "israel", "צהל": "idf", "עזה": "gaza", "חמאס": "hamas", "חיזבאללה": "hezbollah",
    "לבנון": "lebanon", "איראן": "iran", "טהרן": "tehran", "נתניהו": "netanyahu", "תימן": "yemen",
    "חותים": "houthi", "סוריה": "syria", "רוסיה": "russia", "אוקראינה": "ukraine", "טראמפ": "trump",
    "טיל": "missile", "טילים": "missile", "רקטה": "rocket", "רקטות": "rocket", "הרוגים": "killed",
    "נהרגו": "killed", "פצועים": "hurt", "נפצעו": "hurt", "חטופים": "hostage", "חטוף": "hostage",
    "תקיפה": "attack", "מתקפה": "attack", "תקף": "attack", "רפיח": "rafah", "ביירות": "beirut",
    "דמשק": "damascus", "עיראק": "iraq", "ירושלים": "jerusalem", "חמינאי": "khamenei",
    "פוטין": "putin", "זלנסקי": "zelensky", "כטבם": "drone", "רחפן": "drone", "חייל": "soldier",
    "חיילים": "soldier", "צבא": "army", "מלחמה": "war", "מצרים": "egypt", "קטאר": "qatar",
    "סעודיה": "saudi", "ארהב": "usa", "יירוט": "intercept", "כנסת": "knesset", "מוסד": "mossad",
    # árabe
    "اسرائيل": "israel", "اسرائيلي": "israel", "غزة": "gaza", "حماس": "hamas", "لبنان": "lebanon",
    "ايران": "iran", "ايراني": "iran", "طهران": "tehran", "نتنياهو": "netanyahu", "يمن": "yemen",
    "حوثي": "houthi", "حوثيين": "houthi", "سوريا": "syria", "روسيا": "russia", "اوكرانيا": "ukraine",
    "ترامب": "trump", "صاروخ": "missile", "صواريخ": "missile", "قتلى": "killed", "قتيلا": "killed",
    "شهداء": "killed", "شهيدا": "killed", "جرحى": "hurt", "رهائن": "hostage", "اسرى": "hostage",
    "غارة": "attack", "غارات": "attack", "هجوم": "attack", "قصف": "attack", "رفح": "rafah",
    "بيروت": "beirut", "دمشق": "damascus", "عراق": "iraq", "قدس": "jerusalem", "خامنئي": "khamenei",
    "بوتين": "putin", "زيلينسكي": "zelensky", "مسيرة": "drone", "مسيرات": "drone", "جيش": "army",
    "حرب": "war", "مصر": "egypt", "قطر": "qatar", "سعودية": "saudi", "واشنطن": "washington",
    "ضفة": "westbank", "احتلال": "israel", "اسرائيلية": "israel", "اسرائيليين": "israel",
    "حوثيون": "houthi", "صاروخا": "missile", "فلسطيني": "palestinian", "فلسطينيا": "palestinian",
    "فلسطينيين": "palestinian", "مقتل": "killed", "تقتل": "killed", "يقتل": "killed", "حيفا": "haifa",
    "حزب": "hezbollah", "ايرانية": "iran", "لبنانية": "lebanon", "لبناني": "lebanon",
    "פלסטינים": "palestinian", "חיפה": "haifa", "איראני": "iran", "איראנית": "iran",
    "ישראלי": "israel", "ישראלית": "israel", "ישראלים": "israel", "חותי": "houthi",
}

RTL_STOPWORDS = set(
    """
    אחרי לאחר בין כדי אבל היום אמר עוד הוא היא הם אשר גם יותר אחד שני בעקבות נגד מול כל
    על אל את של עם לא מה זה כך
    في من على الى عن التي الذي بعد قبل مع هذا هذه ذلك كان قال بين خلال حول ضد منذ عند اليوم
    امس حتى او ان انه لم لن ما كما وقد قد تم اكثر
    """.split()
)

RTL_PHRASES = [
    (re.compile(r"הפסקת אש"), "ceasefire"),
    (re.compile(r"הבית הלבן"), "whitehouse"),
    (re.compile(r"ארצות הברית"), "usa"),
    (re.compile(r"יהודה ושומרון|הגדה המערבית"), "westbank"),
    (re.compile(r"حزب الله"), "hezbollah"),
    (re.compile(r"وقف اطلاق النار"), "ceasefire"),
    (re.compile(r"الولايات المتحدة"), "usa"),
    (re.compile(r"البيت الابيض"), "whitehouse"),
    (re.compile(r"الضفة الغربية"), "westbank"),
]

def _norm_key(word: str) -> str:
    return strip_accents(normalize_rtl(word))


_HE_ACRONYM = re.compile(r"(?<=[\u05d0-\u05ea])[\"'\u05f3\u05f4]+(?=[\u05d0-\u05ea])")
_AR_FOLD = str.maketrans({"أ": "ا", "إ": "ا", "آ": "ا", "ٱ": "ا", "ى": "ي", "ـ": None})
_AR_DIACRITICS = re.compile(r"[\u064b-\u065f\u0670]")


def normalize_rtl(text: str) -> str:
    """Tira aspas de siglas hebraicas (צה"ל -> צהל), hamza e sinais vocálicos do árabe."""
    text = _HE_ACRONYM.sub("", text)
    text = _AR_DIACRITICS.sub("", text.translate(_AR_FOLD))
    return text


_CANON_RTL = {_norm_key(k): v for k, v in CANON_RTL.items()}
_RTL_STOPWORDS = {_norm_key(w) for w in RTL_STOPWORDS}
_RTL_PHRASES = [(re.compile(_norm_key(rx.pattern)), repl) for rx, repl in RTL_PHRASES]


def _rtl_token(word: str) -> str | None:
    """Canoniza uma palavra em hebraico/árabe tirando prefixos; None se for palavra vazia."""
    if word in _RTL_STOPWORDS:
        return None
    if _HEBREW_RE.match(word):
        stem = word
        for _ in range(3):
            if stem in _CANON_RTL:
                return _CANON_RTL[stem]
            if len(stem) > 3 and stem[0] in HE_PREFIXES:
                stem = stem[1:]
            else:
                break
        return None if len(word) < 3 else word
    stem = word
    for _ in range(3):
        for cand in (stem, stem[2:] if stem.startswith("ال") else None):
            if cand and cand in _CANON_RTL:
                return _CANON_RTL[cand]
        if len(stem) > 3 and stem[0] in AR_PREFIXES:
            stem = stem[1:]
        else:
            break
    return None if len(word) < 3 else word


def clean_html(raw: str | None) -> str:
    if not raw:
        return ""
    text = _TAG_RE.sub(" ", raw)
    text = html.unescape(text)
    return _WS_RE.sub(" ", text).strip()


# Lixo comum que os feeds colocam no resumo.
_SUMMARY_JUNK = [
    re.compile(r"✅\s*Siga o canal.*?WhatsApp\s*(➡️|➡)?", re.I),
    re.compile(r"\s*Leia mais\s*\(\d{2}/\d{2}/\d{4}\s*-\s*\d{1,2}h\d{2}\)\s*$", re.I),
    re.compile(r"\s*The post .{0,300}? appeared first on .{0,80}$", re.I),
    re.compile(r"\s*Continue reading\.*\s*$", re.I),
    re.compile(r"\s*\[(…|\.\.\.)\]\s*$"),
]


def clean_summary(text: str) -> str:
    for rx in _SUMMARY_JUNK:
        text = rx.sub(" ", text)
    return _WS_RE.sub(" ", text).strip()


def truncate(text: str, limit: int) -> str:
    if len(text) <= limit:
        return text
    cut = text[:limit].rsplit(" ", 1)[0]
    return cut.rstrip(".,;:") + "…"


def tokens(text: str) -> set[str]:
    """Tokens significativos, sem acento, com um plural simples removido.

    Nomes próprios (Netanyahu, Gaza, Hezbollah) sobrevivem igual em PT e EN,
    o que permite agrupar a mesma história entre idiomas.
    """
    text = text.lower()
    for rx, repl in PHRASES:
        text = rx.sub(repl, text)
    text = strip_accents(normalize_rtl(text))
    for rx, repl in _RTL_PHRASES:
        text = rx.sub(repl, text)
    out = set()
    for word in _WORD_RE.findall(text):
        if _HEBREW_RE.match(word) or _ARABIC_RE.match(word):
            if tok := _rtl_token(word):
                out.add(tok)
            continue
        if word in ("un", "usa"):
            out.add(word)
            continue
        if len(word) < 3 or word in STOPWORDS or word.isdigit():
            continue
        if len(word) > 4 and word.endswith("s"):
            word = word[:-1]
        out.add(CANON.get(word, word))
    return out
