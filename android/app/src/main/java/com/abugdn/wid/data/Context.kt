package com.abugdn.wid.data

/**
 * Cartões de contexto sobre atores e regiões que aparecem nas notícias.
 * Texto fixo, escrito para dar o pano de fundo; não acompanha os fatos mais recentes.
 */
data class Actor(val key: String, val name: String, val terms: List<String>, val text: String)

const val CONTEXT_DISCLAIMER = "Contexto geral, escrito à mão. Pode não refletir os acontecimentos mais recentes."

val ACTORS = listOf(
    Actor(
        "hamas", "Hamas", listOf("hamas"),
        "Movimento islamista palestino fundado em 1987, durante a Primeira Intifada. Governa a Faixa de Gaza desde 2007, " +
            "depois de vencer as eleições de 2006 e expulsar o Fatah do território. Tem um braço armado (Brigadas al-Qassam) e é " +
            "considerado organização terrorista por Israel, EUA, União Europeia e outros. Comandou o ataque de 7 de outubro de 2023 " +
            "contra Israel, que deu início à guerra em Gaza.",
    ),
    Actor(
        "hezbollah", "Hezbollah", listOf("hezbollah", "hizbollah", "hizbullah", "hezbola"),
        "Partido político e milícia xiita do Líbano, criado nos anos 1980 com apoio do Irã, durante a ocupação israelense do sul " +
            "do país. Tem deputados no Parlamento libanês e um grande arsenal de foguetes e mísseis. Lutou uma guerra contra Israel " +
            "em 2006 e voltou a trocar ataques com Israel a partir de outubro de 2023, com forte escalada em 2024. EUA, Israel e " +
            "outros países o classificam como organização terrorista.",
    ),
    Actor(
        "houthis", "Houthis", listOf("houthi", "huthi", "ansar allah"),
        "Movimento armado xiita zaidita do Iêmen (nome oficial: Ansar Allah). Controla a capital, Sanaa, e boa parte do norte do " +
            "país desde 2014–2015, e enfrentou uma coalizão liderada pela Arábia Saudita na guerra civil. Recebe apoio do Irã. Desde " +
            "o fim de 2023 ataca navios no Mar Vermelho e lança mísseis e drones contra Israel, dizendo agir em solidariedade a Gaza.",
    ),
    Actor(
        "idf", "Forças de Defesa de Israel (FDI/IDF)",
        listOf("idf", "fdi", "forças de defesa de israel", "exército israelense", "exército de israel", "israeli military", "israel defense forces", "israeli army"),
        "As forças armadas de Israel (Tzahal, em hebraico), que reúnem exército, força aérea e marinha. O serviço militar é " +
            "obrigatório para a maioria dos cidadãos judeus e drusos, e centenas de milhares de reservistas são convocados em " +
            "tempos de guerra. \"Exército israelense\", \"FDI\" e \"IDF\" nas notícias se referem a elas.",
    ),
    Actor(
        "irgc", "Guarda Revolucionária do Irã",
        listOf("guarda revolucionária", "revolutionary guard", "irgc", "força quds", "quds force"),
        "Força militar criada após a Revolução Islâmica de 1979 para proteger o regime, separada do exército regular. Controla o " +
            "programa de mísseis e tem grande peso político e econômico no Irã. Seu braço externo, a Força Quds, coordena o apoio " +
            "iraniano a aliados como Hezbollah, Houthis e milícias no Iraque.",
    ),
    Actor(
        "jihad", "Jihad Islâmica Palestina", listOf("jihad islâmica", "islamic jihad"),
        "Grupo armado islamista palestino, menor que o Hamas, atuante em Gaza e na Cisjordânia (com força em Jenin). Recebe apoio " +
            "do Irã, não disputa eleições e costuma combater Israel ao lado do Hamas.",
    ),
    Actor(
        "ap", "Autoridade Palestina", listOf("autoridade palestina", "palestinian authority", "fatah", "abbas"),
        "Governo palestino criado pelos Acordos de Oslo (1993–1995). Administra partes da Cisjordânia e é dominado pelo Fatah, " +
            "partido de Mahmoud Abbas. Perdeu Gaza para o Hamas em 2007. Coopera com Israel em segurança, o que é criticado por " +
            "muitos palestinos.",
    ),
    Actor(
        "unrwa", "UNRWA", listOf("unrwa"),
        "Agência da ONU para refugiados palestinos, criada em 1949. Mantém escolas, clínicas e ajuda humanitária em Gaza, " +
            "Cisjordânia, Jordânia, Líbano e Síria. Israel acusa funcionários de ligação com o Hamas e aprovou leis restringindo sua " +
            "atuação; a agência e vários países contestam as acusações.",
    ),
    Actor(
        "otan", "OTAN", listOf("otan", "nato"),
        "Aliança militar ocidental criada em 1949, com mais de 30 países, incluindo EUA, Reino Unido, França e Alemanha. O artigo 5 " +
            "diz que um ataque a um membro é um ataque a todos. Apoia a Ucrânia com armas e treinamento, sem combater diretamente.",
    ),
)

val REGION_CONTEXT = mapOf(
    "israel" to "Estado criado em 1948, com cerca de 10 milhões de habitantes. Enfrenta conflitos com palestinos e com grupos " +
        "apoiados pelo Irã em várias frentes (Gaza, Líbano, Iêmen, Síria, Iraque). Benjamin Netanyahu liderou o governo na maior " +
        "parte dos últimos 15 anos.",
    "gaza" to "Faixa costeira de cerca de 365 km² entre Israel e o Egito, com mais de 2 milhões de habitantes, muitos descendentes " +
        "de refugiados de 1948. Israel retirou colonos e tropas em 2005; desde 2007 é governada pelo Hamas, sob bloqueio de Israel " +
        "e do Egito. A guerra iniciada em outubro de 2023 causou destruição e uma crise humanitária enormes.",
    "cisjordania" to "Território entre Israel e a Jordânia, ocupado por Israel desde a Guerra dos Seis Dias (1967). Tem cerca de 3 " +
        "milhões de palestinos e centenas de milhares de colonos israelenses em assentamentos considerados ilegais pela maior parte " +
        "do direito internacional. A Autoridade Palestina administra parte das cidades; operações militares e violência de colonos " +
        "são frequentes.",
    "libano" to "Vizinho ao norte de Israel, com um sistema político dividido entre comunidades religiosas. O Hezbollah domina o sul " +
        "e partes de Beirute; a fronteira com Israel tem a missão de paz da ONU (UNIFIL). Vive grave crise econômica desde 2019.",
    "ira" to "República Islâmica desde 1979, comandada por um líder supremo (o aiatolá Ali Khamenei desde 1989). Rival de Israel, " +
        "EUA e Arábia Saudita, apoia uma rede de aliados conhecida como \"Eixo da Resistência\". Seu programa nuclear é alvo de " +
        "sanções e negociações, e o país trocou ataques diretos com Israel em 2024 e 2025.",
    "iemen" to "O país mais pobre da Península Arábica, em guerra civil desde 2014 entre os Houthis, que controlam o norte, e o " +
        "governo reconhecido internacionalmente, apoiado pela Arábia Saudita. Vive uma das piores crises humanitárias do mundo.",
    "siria" to "Mergulhou em guerra civil a partir de 2011. Em dezembro de 2024, uma ofensiva rebelde liderada pelo grupo HTS " +
        "derrubou Bashar al-Assad, e Ahmed al-Sharaa assumiu um governo de transição. Israel ocupou uma zona-tampão no sul e ataca " +
        "alvos militares no país.",
    "iraque" to "Depois da invasão americana de 2003 e da guerra contra o Estado Islâmico (2014–2017), convive com milícias xiitas " +
        "ligadas ao Irã, algumas das quais atacaram Israel e bases americanas a partir de 2023.",
    "ucrania_russia" to "A Rússia anexou a Crimeia em 2014 e apoiou separatistas no Donbas; em fevereiro de 2022 lançou uma invasão em " +
        "larga escala da Ucrânia. Os combates se concentram no leste e no sul, com uso intenso de drones, mísseis e artilharia. " +
        "A Ucrânia recebe armas e ajuda de países ocidentais.",
    "eua" to "Principal aliado militar de Israel, a quem envia cerca de US$ 3,8 bilhões por ano em ajuda militar, além de " +
        "armas e defesa antimísseis em tempos de guerra. Mantém bases no Golfo (Catar, Bahrein, Emirados, Kuwait), porta-aviões " +
        "na região e o Comando Central (CENTCOM), que coordena as operações no Oriente Médio. Liderou as guerras no Afeganistão e " +
        "no Iraque, a coalizão contra o Estado Islâmico, ataques aos Houthis e, em junho de 2025, bombardeou instalações nucleares " +
        "do Irã. Também é um dos maiores fornecedores de armas à Ucrânia.",
    "sudao" to "Em guerra desde abril de 2023 entre o exército (SAF) e as Forças de Apoio Rápido (RSF), paramilitares. O conflito, " +
        "centrado em Cartum e Darfur, gerou uma das maiores crises de deslocados do mundo e denúncias de atrocidades étnicas.",
)

/** Glossário de armas e sistemas que aparecem com frequência. */
val GLOSSARY = listOf(
    Actor(
        "domo", "Domo de Ferro", listOf("domo de ferro", "cúpula de ferro", "iron dome"),
        "Sistema israelense de defesa contra foguetes e projéteis de curto alcance (4 a 70 km), em operação desde 2011. Calcula a " +
            "trajetória e só dispara interceptadores contra o que vai cair em área habitada. É a primeira camada da defesa aérea de Israel.",
    ),
    Actor(
        "funda", "Funda de Davi", listOf("funda de davi", "david's sling", "davids sling"),
        "Camada intermediária da defesa aérea israelense, feita com os EUA, para foguetes pesados, mísseis de cruzeiro e " +
            "mísseis balísticos de médio alcance (até cerca de 300 km).",
    ),
    Actor(
        "arrow", "Arrow (Hetz)", listOf("arrow 2", "arrow 3", "arrow-2", "arrow-3", "sistema arrow", "arrow system"),
        "Camada superior da defesa israelense contra mísseis balísticos de longo alcance, como os lançados pelo Irã e pelos " +
            "Houthis. O Arrow 3 intercepta fora da atmosfera.",
    ),
    Actor(
        "thaad", "THAAD", listOf("thaad"),
        "Sistema americano de defesa contra mísseis balísticos na fase final do voo, dentro e logo acima da atmosfera. Os EUA " +
            "instalaram uma bateria em Israel em 2024 para reforçar a defesa contra o Irã.",
    ),
    Actor(
        "patriot", "Patriot", listOf("patriot"),
        "Sistema americano de defesa aérea contra aviões, mísseis de cruzeiro e balísticos. É usado pela Ucrânia contra mísseis " +
            "russos, inclusive os hipersônicos Kinzhal, e por vários países do Golfo e da Europa.",
    ),
    Actor(
        "shahed", "Drones Shahed", listOf("shahed", "geran"),
        "Drones de ataque iranianos de baixo custo, que voam até o alvo e explodem ("drones kamikaze"). A Rússia os usa em " +
            "massa contra a Ucrânia (fabricados localmente como Geran-2), e o Irã e os Houthis contra Israel e navios.",
    ),
    Actor(
        "himars", "HIMARS", listOf("himars"),
        "Lançador de foguetes americano montado em caminhão, com alcance de cerca de 80 km (ou 300 km com mísseis ATACMS). " +
            "Ficou conhecido pelo uso ucraniano contra depósitos e comandos russos desde 2022.",
    ),
    Actor(
        "atacms", "ATACMS", listOf("atacms"),
        "Míssil balístico tático americano, com alcance de até cerca de 300 km, lançado por HIMARS. O uso ucraniano contra o " +
            "território russo foi autorizado pelos EUA no fim de 2024.",
    ),
    Actor(
        "storm", "Storm Shadow / SCALP", listOf("storm shadow", "scalp"),
        "Míssil de cruzeiro anglo-francês lançado de aviões, com alcance de mais de 250 km, fornecido à Ucrânia.",
    ),
    Actor(
        "kinzhal", "Kinzhal", listOf("kinzhal"),
        "Míssil balístico russo lançado de aviões, divulgado pela Rússia como hipersônico (muito acima de 5 vezes a velocidade do " +
            "som). Usado contra alvos na Ucrânia.",
    ),
    Actor(
        "tomahawk", "Tomahawk", listOf("tomahawk"),
        "Míssil de cruzeiro americano de longo alcance (mais de 1.500 km), lançado de navios e submarinos. Usado contra alvos " +
            "na Síria, no Iêmen e em outros conflitos.",
    ),
    Actor(
        "gbu57", "Bomba antibunker GBU-57", listOf("gbu-57", "bunker buster", "bunker-buster", "antibunker", "destruidora de bunkers"),
        "Bomba americana de cerca de 13,6 toneladas feita para destruir alvos subterrâneos fortificados. Só o bombardeiro B-2 a " +
            "carrega. Foi usada pela primeira vez em junho de 2025 contra a instalação nuclear iraniana de Fordow.",
    ),
    Actor(
        "f35", "F-35", listOf("f-35", "f35"),
        "Caça furtivo americano de quinta geração, usado por Israel (versão F-35I "Adir") e por vários aliados. Teve papel " +
            "central nos ataques israelenses ao Irã.",
    ),
)

/** Atores e armas citados na notícia (título, resumo e tradução). */
fun Cluster.actors(translated: (String) -> String): List<Actor> {
    val text = normalize("$title\n$summary\n${translated(title)}\n${translated(summary)}")
    return (ACTORS + GLOSSARY).filter { actor ->
        actor.terms.any { Regex("(?<![\\p{L}\\d])" + Regex.escape(normalize(it)) + "s?(?![\\p{L}\\d])").containsMatchIn(text) }
    }
}
