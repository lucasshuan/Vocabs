# Produto

## O problema

Você capta palavras e expressões novas no meio de outras atividades — jogo, série,
livro — mas não tem como guardar isso rápido, nem como transformar em vocabulário
que gruda de verdade.

## A solução

Um app que (1) captura no momento com fricção quase zero, (2) transforma a captura
numa ficha rica e conectada, e (3) traz você de volta para revisar de formas
variadas.

**O que não é:** mais um Duolingo com lições fixas. O conteúdo nasce da sua vida,
não de um currículo pronto.

## Princípios

Servem para decidir rápido na dúvida e impedir o escopo de inflar.

1. **Captura antes de tudo.** Se a captura tiver fricção, o app morre. Velocidade
   ganha de completude.
2. **Captura e revisão são momentos separados.** Nunca colocar pressão logo após
   capturar — nada de "revise agora".
3. **Uma palavra, muitos caminhos.** Variedade de exercício evita decorar o padrão
   da tela.
4. **Contexto pessoal > definição de dicionário.** A frase onde você viu vale mais
   para a memória.
5. **Não trave em polimento cedo.** Feio que funciona ganha de bonito que não sai
   do papel.
6. **A IA faz o trabalho pesado de conteúdo.** Etimologia, exemplos, exercícios —
   gerados, não digitados.
7. **Local-first e transparente.** Dados no aparelho por padrão. Tudo que custa
   dinheiro (IA, sync) é visível e explicado.

## Core loop

```
Capturar → Enriquecer (ficha) → Conectar (rede) → Revisar → Dominar
                                                      ↑         │
                                                      └─────────┘
                                              (volta espaçadamente)
```

Só captura rápida + ficha simples + revisão espaçada já resolve 80% do problema.
Todo o resto é enriquecimento.

## O que é uma captura

Cada contexto tem uma restrição diferente (jogando você não quer sair do jogo;
lendo, as mãos estão ocupadas). Por isso a solução não é uma tela de captura
perfeita — é **separar captura de processamento**: jogue o sinal cru no app em
segundos e transforme em ficha depois, com calma.

**Modelo unificado**, igual para qualquer contexto:

| Campo | O que é |
|---|---|
| `trecho` | A frase ou pedaço de contexto onde apareceu |
| `alvo` | O que chamou atenção (1 palavra ou várias) |
| `motivo` | "não conheço" ou "quebrou a frase" — opcional, dá para inferir depois |
| `tipo` | Palavra ou expressão — **classificado pela IA**, nunca por você |

**Palavra vs expressão** não é sobre quantidade de palavras, é sobre se o
significado nasce da soma das partes. Teste: procurando só esse termo isolado no
dicionário, o sentido que você viu aparece? Sim → palavra (`ubiquitous`). Não →
expressão (`kick the bucket`, `on the fence`).

Classificar na hora da captura seria fricção extra — e muitas vezes nem está claro
no momento. Vai tudo para o processamento.

**Dois motivos diferentes de captura**, que pedem exercícios diferentes:

- **Vocabulário novo** — você não conhece o termo. Flashcard funciona bem.
- **Trava de leitura** — você entende a frase quase toda, mas uma palavra trava o
  sentido. O exercício valioso não é o flashcard isolado: é mostrar a frase inteira
  de novo depois.

## Retenção: pontos + decaimento

Em vez de SM-2/Anki (muitas variáveis interdependentes), cada palavra tem uma
"força de memória" de 0 a 100 que sobe com acertos e cai sozinha com o tempo.

Três campos por palavra:

| Campo | O que é |
|---|---|
| `pontos` | 0-100, o quanto você lembra agora |
| `taxa_decaimento` | Quantos pontos perde por dia |
| `data_ultima_interacao` | Base do cálculo |

```
pontos_atuais = max(0, pontos_salvos − taxa_decaimento × dias_desde_ultima_interacao)
```

**O ponto crítico são os dois números, não um.** Se todas as palavras decaíssem no
mesmo ritmo, uma que você domina há meses cairia tão rápido quanto uma nova — que é
exatamente a vantagem que a repetição espaçada existe para dar. Por isso a taxa
diminui a cada acerto (ex: divide por 1.5) e sobe a cada erro.

Entra na fila de revisão quando `pontos_atuais < 60`. Sem cron, sem tabela de
intervalos: o cálculo é feito na hora.

**Níveis:** 0-30 aprendendo · 30-70 familiar · 70-100 dominada.

## Monetização

**Sem anúncio, nunca.** O app depende de sessões de foco e hábito repetido —
interromper isso quebra o mecanismo que faz o app funcionar. E anúncio não cobre
custo variável de IA.

**A ideia:** gatear pelo que realmente custa dinheiro (geração por IA), não por
telas escolhidas arbitrariamente. O que é barato de rodar fica ilimitado de graça.

| | |
|---|---|
| **Grátis, ilimitado** | Captura, inbox, flashcard, pontos/decaimento — tudo local |
| **Add-on "IA Boost"** | Etimologia, exemplos personalizados, leitura contextual, exercícios gerados |
| **Add-on "Sync na nuvem"** | Backup e uso em múltiplos aparelhos |
| **Créditos avulsos** | Para quem não quer assinar |

Cada add-on explica **por que** custa: "isso aqui usa IA", "isso aqui usa nuvem" —
não "recurso premium misterioso". Exportação dos dados fica sempre disponível.

**Transparência de verdade** significa mostrar o consumo no momento em que ele
acontece ("42 de 100 gerações este mês"), não escondido nas configurações. Mais:
aviso ao chegar perto do limite, detalhamento por categoria, e comportamento claro
ao estourar.

## Onde a IA não é necessária

Boa parte do que parece caro tem alternativa gratuita:

- **Definição, sinônimos, antônimos, família de palavras** → base lexical aberta
  (WordNet, Wiktionary) baixada uma vez. Funciona offline.
- **Áudio de pronúncia** → TTS nativo do celular.
- **Exercício de fala** → reconhecimento de voz nativo.
- **Cruzadas, matching, flashcard, pontos** → lógica pura, zero IA.

**Onde a IA vale mesmo:** etimologia contada de forma interessante, exemplos a
partir do seu contexto de captura, leitura contextual, gramática adaptativa,
história conectando várias palavras.

## Custos

| Item | Tipo | Ordem de grandeza |
|---|---|---|
| Apple Developer Program | Anual | ~US$ 99 |
| Google Play Console | Único | ~US$ 25 |
| Comissão das lojas | % da receita | 15% até US$ 1M/ano |
| Hospedagem/backend | Mensal | US$ 0-25 no início |
| Domínio + política de privacidade | Anual | ~US$ 10-20 |
| IA por geração | Por chamada | frações de centavo |
| TTS e reconhecimento de fala | — | US$ 0 se usar o nativo |

O custo variável real fica concentrado só na IA generativa. O risco maior não é
conta de infraestrutura — é o seu tempo.
