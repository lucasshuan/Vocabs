# Roadmap

Cada fase tem um **critério de saída**. Só passe para a próxima quando ele for
satisfeito — é isso que impede polir uma fase indefinidamente.

Regra de processo: revise os riscos comuns (no fim deste arquivo) antes de começar
cada fase nova.

---

## Fase 0 — Validação pessoal · 1-2 semanas

Provar que o hábito de captura funciona **antes** de construir qualquer coisa.

- [ ] Capturar palavras por 1-2 semanas com qualquer ferramenta que já existe (notas, planilha)
- [ ] Observar: você captura no momento ou esquece? Quantas por dia?
- [ ] Anotar em que contexto as palavras mais surgem

**Critério de saída:** lista real de 20-40 palavras, com contexto identificado.

---

## Fase 1 — MVP de captura + ficha · 2-4 semanas

**Telas:** Captura · Ficha · Lista/home

- [x] Armazenamento local no aparelho, sem backend/autenticação de usuário
- [x] Tela de captura (trecho + alvo, origem opcional)
- [x] Ficha gerada automaticamente: tradução, 1-2 definições, 1 exemplo, IPA
- [x] Classificação automática palavra vs expressão
- [x] Lista/home simples
- [ ] **Critério de saída:** captura em menos de 10s, ficha em menos de 1 minuto

> **Ficha: cronometrada e aprovada com folga.** Medida em 01/08/2026 com
> `claude-opus-5`. Fim a fim dentro do app (toque em "Salvar" até `status =
> PRONTA`): **6,65s**. Direto no servidor, 5 amostras: 4,61 / 4,98 / 5,10 / 5,50 /
> 6,24s — média 5,05s. O teto de 60s tem quase 90% de folga sobrando, então não há
> motivo para trocar por um modelo mais rápido: a classificação PALAVRA vs
> EXPRESSAO acertou 5 de 5.
>
> A entrada vira `GERANDO` em **0,13s**, ou seja, salvar não espera a IA de fato.
> O app abre a frio em ~1,05s.
>
> **Captura: falta o teste humano.** O que sobra dos 10s depois do cold start são
> ~9s de digitação, e isso não dá para medir por automação — injetar texto via adb
> não representa ninguém. Só fecha com você, o celular na mão e uma palavra que
> apareceu de verdade.
>
> Desvio do plano original: existe um backend (`:server`) intermediando a chamada
> de IA, para a chave não ficar no aparelho. Os dados de vocabulário continuam
> 100% locais.

---

## Fase 1.5 — Inbox multi-formato · 1-2 semanas

**Telas novas:** Inbox · Captura por foto · Captura por áudio · Processamento manual

- [x] Captura por foto (sem OCR — você transcreve depois)
- [x] Captura por áudio memo (sem transcrição automática)
- [x] Tela de inbox, separada das fichas prontas
- [x] Tela de processamento manual (ver a mídia e transcrever)
- [ ] Testar nos 3 contextos reais: PC, Kindle, DS
- [ ] **Critério de saída:** capturar em qualquer um dos 3 contextos em menos de 5s

> **Implementado e testado no emulador nos três formatos.** Áudio grava com um
> toque e para com outro; foto usa o app de câmera do sistema via FileProvider;
> os dois entram como `RASCUNHO` e viram ficha depois da transcrição — o caminho
> foto → transcrição → ficha foi percorrido inteiro (`verdant`, PALAVRA).
>
> O inbox saiu quase de graça, como previsto, mas não como `status = 'PENDENTE'`:
> virou `status != 'PRONTA'`, que também recolhe rascunhos e erros. A home passou
> a listar só o que já é ficha — é essa a separação que a fase pedia.
>
> **Falta o teste de campo.** Os 5s em PC, Kindle e DS dependem de você com os
> aparelhos na mão; o emulador não simula ter as mãos ocupadas segurando um livro.

---

## Fase 2 — Retenção ativa · 3-5 semanas

**Tela nova:** Revisão/flashcard · **Mudam:** Home (indicador de revisões) · Ficha (barra de pontos)

- [x] Sistema de pontos (0-100) por palavra
- [x] Taxa de decaimento variável (desce no acerto, sobe no erro)
- [x] Cálculo sob demanda: `max(0, pontos − taxa × dias)`
- [x] Exercício de flashcard
- [x] Indicador "X pra revisar hoje" na home
- [x] Barra de progresso na ficha
- [ ] **Critério de saída:** 7 dias seguidos revisando, sentindo que lembra palavras de 3+ dias atrás

> **Implementado e verificado no emulador**, envelhecendo o banco à mão para não
> depender de esperar dias. Acerto devolve 100 pontos e divide a taxa por 1,5;
> erro zera e multiplica por 3 — que é o valor que faz o erro cortar o intervalo
> pela metade. A escada de acertos seguidos dá 1 · 1,5 · 2,25 · 3,4 · 5,1 · 7,6
> dias, então a terceira revisão de cada palavra já cai depois de 3+ dias.
>
> Um erro no meio da sessão traz o cartão de volta uma vez, no fim da fila, mas
> **só a primeira resposta é gravada** — a repetição é para você ver a palavra de
> novo, não para apagar o erro.
>
> O flashcard mostra o trecho capturado com o alvo apagado, virando um cloze
> contra o seu próprio contexto; no verso a frase volta inteira. É o que o
> documento de produto pede para a "trava de leitura", e sai sem custo de IA.
>
> **O critério de saída depende de uso real**: 7 dias seguidos não dá para
> simular. A sequência já é registrada (tabela `dia_revisado`), então o app conta
> por você — é só usar.

---

## Fase 3 — Rede de associações · 3-4 semanas

**Tela nova:** Montar palavra (afixos) · **Muda:** Ficha (palavras relacionadas)

A parte diferenciada da ideia: palavras puxando outras palavras.

- [ ] Sinônimos, antônimos, família de palavras
- [ ] Collocations
- [ ] Navegação entre palavras conectadas na ficha
- [ ] Jogo de montar palavra (radical + afixos)
- [ ] **Critério de saída:** "passear" por 3-5 palavras relacionadas a partir de 1 capturada, e aprender algo

---

## Fase 4 — Exercícios fáceis (IA) · 4-6 semanas

**Tela nova:** Hub de exercícios (com indicador de consumo de IA)

Priorize 2-3, **não os 6 de uma vez**.

- [ ] Hub de exercícios
- [ ] Leitura contextual
- [ ] Gramática/cloze
- [ ] Desembaralhar frase
- [ ] Associação (reaproveita a rede da Fase 3)
- [ ] Adivinhar pela definição
- [ ] Rodada relâmpago
- [ ] Verdadeiro ou falso de uso
- [ ] Registro/formalidade
- [ ] Indicador de consumo de IA no hub
- [ ] **Critério de saída:** cada palavra dominada passou por 2+ tipos de exercício

---

## Fase 5 — Áudio e cruzadas · 3-4 semanas

- [ ] TTS a partir de frases geradas por IA (não áudio real)
- [ ] Ditado por áudio
- [ ] Palavras cruzadas
- [ ] Matching (palavra/definição, expressão/significado)
- [ ] Revisão em lote (história com várias palavras do dia)
- [ ] **Critério de saída:** exercício de escuta disponível para qualquer palavra

---

## Fase 6 — Fala e conteúdo real · contínuo, mais tardio

As partes mais caras tecnicamente. Só depois do resto maduro.

- [ ] Reconhecimento de voz nativo
- [ ] Exercício de fala com comparação de pronúncia
- [ ] Avaliar fontes externas para trechos reais (atenção a licenciamento)
- [ ] Tela de leitura com conteúdo real
- [ ] **Critério de saída:** sem prazo fixo — entra quando o resto estiver estável no uso diário

---

## Fase 7 — Polimento e hábito · contínuo

- [ ] OCR automático nas fotos
- [ ] Transcrição automática dos áudios
- [ ] Exportação do Kindle Vocabulary Builder
- [ ] Captura via compartilhamento do sistema
- [ ] Home com decks (por origem, por status)
- [ ] Gamificação leve (streak, progresso visual)
- [ ] Notificações de revisão
- [ ] Painel de transparência de uso
- [ ] Add-on "IA Boost"
- [ ] Add-on "Sync na nuvem"
- [ ] Exportação de dados sempre disponível

---

## Fase 8 — Painel de métricas · 2-3 semanas

Só faz sentido com volume real de dados. Detalhes em
[EXERCICIOS-E-METRICAS.md](EXERCICIOS-E-METRICAS.md).

- [ ] Funil de conversão e backlog do inbox — os sinais mais importantes
- [ ] Crescimento de vocabulário e heatmap de consistência
- [ ] Taxa de acerto ao longo do tempo e por tipo de exercício
- [ ] Origem do conteúdo e padrões de dificuldade
- [ ] Nível estimado de inglês (CEFR) — por último, depende de fonte externa
- [ ] **Critério de saída:** entender o progresso num relance, sem calcular nada de cabeça

---

## Riscos comuns

| Armadilha | Resposta |
|---|---|
| "Vou deixar a ficha perfeita antes de usar" | Não. Feia e simples valida o loop; estética é fase tardia. |
| "Vou construir os 6 jogos de uma vez" | Não. Comece com 1, use por uma semana, só então adicione. |
| "Preciso decidir toda a arquitetura de dados antes" | Não. Comece com o mínimo e evolua com necessidade real. |
| "Preciso de OCR e reconhecimento de voz já" | Comece com um campo de texto. Você pode nem sentir falta. |

## Métricas nas fases 0-2

Antes do painel completo, acompanhe só o essencial — pode ser manual:

- Quantas palavras você captura por semana (constância importa mais que volume)
- Quantas revisões por semana
- De 10 palavras capturadas há 2 semanas, quantas você lembra sem olhar a ficha?
