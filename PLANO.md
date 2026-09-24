# WID — Plano do app de notícias de guerra

App mobile de notícias sobre conflitos no Oriente Médio e no mundo, com foco em Israel.
Público: 1 dono + até 4 amigos. Requisitos: **custo zero**, **mobile**, **widget com a notícia principal do dia**.

---

## 1. Decisão mais importante: Android vs iPhone

| | Android | iPhone |
|---|---|---|
| Instalar sem loja | APK direto, grátis | Precisa conta Apple Developer (**US$ 99/ano**) ou conta grátis que **expira a cada 7 dias** e exige Mac + Xcode |
| Widget | Grátis, sem restrição | WidgetKit (Swift), mesmo problema de assinatura |
| Custo zero | ✅ | ❌ na prática |

**Recomendação:** Android nativo. Se algum amigo tiver iPhone, a alternativa é ele usar a versão web (PWA) do mesmo feed, sem widget.

---

## 2. Arquitetura (tudo no free tier)

```
 ┌─────────────────────────── GitHub (grátis) ───────────────────────────┐
 │                                                                        │
 │  GitHub Actions (cron a cada 30 min)                                   │
 │     └─ script Python                                                   │
 │          1. baixa ~15 feeds RSS                                        │
 │          2. filtra por palavras-chave (Israel, Gaza, Irã, Hezbollah…)  │
 │          3. remove duplicatas / agrupa a mesma história                │
 │          4. calcula "notícia principal do dia"                         │
 │          5. grava feed.json + top.json                                 │
 │                                                                        │
 │  GitHub Pages  →  https://<user>.github.io/wid/feed.json               │
 │  GitHub Releases → APK do app (build automático)                       │
 └────────────────────────────────────────────────────────────────────────┘
                 │ HTTPS (JSON estático)
                 ▼
 ┌──────────── Celular Android ────────────┐
 │  App (Kotlin + Jetpack Compose)          │
 │  Widget (Jetpack Glance)                 │
 │  WorkManager atualiza a cada 30–60 min   │
 └──────────────────────────────────────────┘
       (opcional) ntfy.sh → push de "urgente"
```

Por que um "backend" em vez do app ler os RSS direto: o processamento (dedupe, ranking) fica num lugar só, os 5 celulares veem exatamente a mesma "principal do dia", e o widget só precisa baixar um JSON pequeno (economiza bateria).

### Custos

| Peça | Serviço | Custo |
|---|---|---|
| Coleta/processamento | GitHub Actions | R$ 0 (repo público: ilimitado; privado: 2.000 min/mês — cron de 30 min usa ~1.450) |
| Hospedagem do JSON | GitHub Pages | R$ 0 (**exige repo público**; ver §7) |
| Distribuição do app | GitHub Releases + [Obtainium](https://github.com/ImranR98/Obtainium) p/ auto‑update | R$ 0 |
| Push (opcional) | ntfy.sh | R$ 0 |
| Tradução (opcional) | ML Kit on‑device no Android | R$ 0 |

---

## 3. Fontes (RSS)

Misturar perspectivas para não ficar enviesado. URLs a validar na implementação.

- **Israel:** Times of Israel, Jerusalem Post, Ynetnews, Haaretz (manchetes)
- **Mundo árabe:** Al Jazeera English, Al‑Monitor
- **Internacionais:** BBC Middle East, The Guardian (Middle East / World), AP (via Google News), DW
- **Em português:** G1 Mundo, Folha Mundo, CNN Brasil Internacional
- **Coringa:** Google News RSS por busca, ex.
  `https://news.google.com/rss/search?q=Israel+guerra&hl=pt-BR&gl=BR&ceid=BR:pt-419`
- **Outros conflitos:** Ucrânia/Rússia, Sudão, Iêmen etc. via Google News RSS por tema

Arquivo `sources.yaml` com: nome, URL, idioma, região, peso.

---

## 4. Processamento (script Python)

1. **Coleta** — `feedparser` + `httpx`, timeout curto; fonte que falhar é ignorada.
2. **Filtro** — lista de palavras-chave PT/EN com pesos (ex.: `Israel`, `IDF`, `Gaza`, `Hamas`, `Hezbollah`, `Irã/Iran`, `Houthi`, `Cisjordânia/West Bank`, `Líbano`, `Síria`, `Ucrânia`…). Tag por conflito.
3. **Agrupamento** — títulos normalizados + similaridade (TF‑IDF/Jaccard). Mesma história em 6 veículos vira 1 item com 6 links.
4. **Ranking da principal do dia** —
   `score = nº de fontes distintas × peso das palavras-chave × decaimento por idade (meia-vida ~6h)`, bônus para tag Israel.
   Opcional depois: LLM com free tier para resumir/escolher (só se continuar grátis).
5. **Saída**
   - `feed.json` — últimas ~48h, agrupadas, com tags e fontes
   - `top.json` — 1 item: título, resumo curto, fonte, horário, link, imagem
   - `history/AAAA-MM-DD.json` — principal de cada dia (arquivo leve)
6. **Push opcional** — se um grupo novo passar de um score alto (ex.: ≥ 8 fontes em 1h), envia para um tópico ntfy privado.

---

## 5. App Android

- **Stack:** Kotlin, Jetpack Compose, Retrofit/Ktor, kotlinx.serialization, Room (cache offline), WorkManager, **Glance** (widget), Coil (imagens).
- **Telas**
  1. **Hoje** — card grande com a principal do dia + lista agrupada
  2. **Filtros** — chips por conflito (Israel/Gaza, Líbano, Irã, Iêmen, Ucrânia, Outros)
  3. **Detalhe** — resumo + lista de veículos que cobriram, abre o link no navegador (Custom Tabs)
  4. **Arquivo** — principal de cada dia anterior
- **Widget** (2 tamanhos)
  - Pequeno: título da principal + horário
  - Médio: título + fonte + nº de veículos + 2 manchetes secundárias
  - Toque abre o app no detalhe. Atualização via WorkManager (mínimo do Android é 15 min; usar 30–60).
- **Idioma:** manchetes no original; botão "traduzir" com ML Kit on‑device (opcional, fase 3).

---

## 6. Distribuição

1. Workflow `build-apk.yml` compila e assina o APK a cada tag `v*` e publica no GitHub Releases.
2. Cada amigo instala o Obtainium uma vez, aponta para o repo, e recebe atualizações automáticas.
3. Keystore de assinatura guardada em GitHub Secrets.

---

## 7. Pontos de atenção

- **Repo público vs privado:** GitHub Pages grátis só em repo público. Opções:
  (a) deixar este repo público (é só código + manchetes públicas — recomendado),
  (b) repo privado para o código + um repo público só com os JSONs,
  (c) Cloudflare Pages/Workers (free tier) no lugar do Pages.
- **Cron do GitHub** atrasa às vezes (5–15 min) e é desativado após 60 dias sem atividade no repo — os commits automáticos de dados já contam como atividade.
- **Direitos autorais:** guardar só título, trecho curto do RSS e link. Não copiar matérias inteiras.
- **Imagens:** usar a URL que vem no RSS; sem re-hospedar.

---

## 8. Fases

| Fase | Entrega | Pronto quando |
|---|---|---|
| 0 | Repo, `sources.yaml`, decisão público/privado | — |
| 1 | Script Python + Actions + Pages publicando `feed.json`/`top.json` | JSON atualiza sozinho a cada 30 min |
| 2 | App Android: tela Hoje + detalhe + cache offline | Lê o feed no celular |
| 3 | Widget Glance | Principal do dia na home |
| 4 | Build automático + Obtainium | Amigos instalam e atualizam sozinhos |
| 5 | Extras: filtros, arquivo, push ntfy, tradução | — |

---

## 9. Perguntas em aberto

1. Todos os 5 usam Android? (define se o plano acima fica como está)
2. Repo público tudo bem? (ver §7)
3. Quer push de "urgente" ou só o widget basta?
4. Manchetes em português, inglês ou os dois?
5. Quais outros conflitos entram além de Israel/Oriente Médio?
