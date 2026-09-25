package com.abugdn.wid.data

/**
 * Histórico de novidades por versão (versionCode = número da build no CI).
 *
 * O pop-up mostra todas as versões mais novas que a última que a pessoa confirmou com
 * "não mostrar de novo" — quem pulou versões vê as notas de cada uma. A cada versão com
 * novidades, acrescente uma entrada no topo com o versionCode esperado da próxima build.
 */
data class ChangelogEntry(val versionCode: Long, val items: List<String>)

val CHANGELOG = listOf(
    ChangelogEntry(
        12,
        listOf(
            "📥 Aba Lidas: as notícias que você já abriu saem da lista principal e ficam em \"Lidas\" (tela Hoje).",
            "🔄 O app procura versão nova toda vez que é aberto; não precisa mais ir aos Ajustes.",
            "📰 Principal do dia mais atual: histórias de ontem perdem peso com o tempo e o servidor volta a atualizar a cada 30 minutos.",
        ),
    ),
    ChangelogEntry(
        11,
        listOf(
            "📜 Novidades acumuladas: se você pular versões, este aviso mostra as notas de todas as que perdeu.",
            "⏪ O que você perdeu: depois de mais de 24 h sem abrir o app, um resumo das principais do período.",
            "🌍 Página da região: toque numa região (mapa, filtro ou chip) para ver contexto, marcos, tendência, notícias atuais e as principais dos últimos 30 dias.",
            "🖼 Compartilhar como imagem: cartão com título em português, veículo e número de veículos, pronto para WhatsApp/story.",
            "📖 Modo leitura: tempo estimado de leitura, fonte serifada e espaçamento maior (botão Aa na notícia).",
            "🫥 Imagens sensíveis borradas até você tocar (em notícias com mortos ou feridos). Desligável nos Ajustes.",
            "🔕 Notificações agrupadas: várias notícias juntas viram um grupo só.",
            "📊 Sua semana: quantas notícias você leu, regiões que mais acompanhou e histórias seguidas ainda ativas (aba Arquivo).",
        ),
    ),
    ChangelogEntry(
        10,
        listOf(
            "🗺 Mapa corrigido: não desenha mais por cima do resto da tela ao arrastar. Marcadores agora são círculos com o número de histórias.",
            "🆕 Novo desde a sua leitura: notícias que você já leu mostram quantos veículos chegaram depois, marcados como NOVO.",
            "👤 Pessoas: Netanyahu, Khamenei, Trump, Putin, Zelensky, líderes do Hamas e do Hezbollah e outros, com notícias recentes que os citam.",
            "📰 Perfil dos veículos: toque no nome de um veículo (ⓘ) para ver país, dono e linha editorial.",
            "🔔 Notificações com botões Salvar e Seguir.",
            "📌 Atalhos: segure o ícone do WID para O dia em 1 minuto, Buscar e Salvos.",
            "📶 Economia de dados (Ajustes): sem imagens e downloads grandes só no Wi-Fi.",
            "📁 Pastas e notas nos Salvos: toque no cartão \"Salva\" dentro da notícia.",
            "⚫ Tema preto AMOLED em Ajustes → Aparência.",
        ),
    ),
    ChangelogEntry(
        9,
        listOf(
            "🇺🇸 Estados Unidos: nova região (filtro, mapa, tendência e notificações) e veículos americanos (NPR, CNN, Fox News, Washington Post, Defense News).",
            "▶ O dia em 1 minuto: as 5 principais do dia em cartões de tela cheia.",
            "🛡 Glossário militar: Domo de Ferro, THAAD, Shahed e outros na notícia.",
            "📜 Marcos históricos no contexto de cada região.",
            "🧩 Widget por região.",
        ),
    ),
)

/** Ids usados antes do histórico por versão (um único aviso por vez) -> versão equivalente. */
val LEGACY_WHATS_NEW_IDS = mapOf("2026-09-25-eua" to 9L, "2026-09-25-pessoas" to 10L)
