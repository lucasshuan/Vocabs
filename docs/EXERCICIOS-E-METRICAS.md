# Exercícios e métricas

## Catálogo de minigames

Os exercícios cobrem leitura, escuta, fala e gramática — mas cada um tem custo de
implementação bem diferente. **Escolha a ordem pela complexidade real, não só pelo
valor pedagógico.**

### Fácil — só texto gerado por IA

| Exercício | O que testa |
|---|---|
| **Leitura contextual** | Trecho curto e natural usando a palavra |
| **Gramática** | Cloze, múltipla escolha, reescrever frase |
| **Associação** | Correlatas/antônimas — reaproveita a rede da Fase 3 |
| **Intruso** | 4-5 palavras, uma fora do grupo semântico — testa a vizinhança de significado |
| **Adivinhar pela definição** | Inverso do flashcard: força recuperação ativa em vez de reconhecimento |
| **Verdadeiro ou falso de uso** | Frase às vezes certa, às vezes estranha — pega nuance, não só significado |
| **Registro/formalidade** | Escolher a palavra certa para email formal vs conversa |
| **Desembaralhar frase** | Reforça gramática e uso juntos |
| **Revisão em lote** | Texto curto usando várias palavras do dia — mostra elas convivendo |
| **Rodada relâmpago** | Flashcard contra o tempo — é só UI sobre o que já existe |

### Médio — mais lógica de UI, ainda sem fonte externa

| Exercício | Observação |
|---|---|
| **Matching** | Palavras de um lado, definições do outro — bom para revisar em lote |
| **Matching de expressões** | Mesma mecânica, focada em expressões idiomáticas |
| **Montar a palavra (afixos)** | Radical + prefixos/sufixos — reforça família de palavras |

### Médio — depende de fonte externa

- **Trechos reais** (livros, jornais, música): evita citação inventada, mas exige
  integração com fonte de dados e traz questão de direitos autorais.
- **Áudio de trechos reais**: não existe jeito simples de buscar áudio contendo a
  palavra X. TTS a partir de frase gerada é muito mais viável.

### Difícil — processamento em tempo real

- **Fala prática**: exige speech-to-text para comparar com a pronúncia esperada.
  É o mais caro da lista — e o que mais diferencia o app de um Anki genérico.
- **Palavras cruzadas**: não é complexo tecnicamente, mas dá trabalho de UI/lógica
  para ficar bem feito.

### Ordem sugerida

1. Leitura contextual
2. Gramática
3. Associação
4. Áudio via TTS
5. Palavras cruzadas
6. Fala prática — por último, se beneficia do app já maduro
7. Trechos reais — complemento futuro, não bloqueante

**Fora de escopo:** jogos com imagem (vocabulário abstrato não se presta) e
qualquer mecânica multiplayer ou ranking (você é o único usuário).

---

## Painel de métricas

Métricas bem escolhidas viram motivação — mas só depois que houver dados reais,
senão os gráficos ficam vazios.

### Sobre o seu inglês

**Crescimento**
- Palavras capturadas / aprendidas / dominadas ao longo do tempo (linha acumulativa)
- Novas palavras por semana (barras)
- Proporção palavra vs expressão

**Consistência**
- Streak atual e recorde
- Heatmap de atividade (tipo contribution graph do GitHub)
- Dias ativos no mês

**Tempo**
- Tempo total e médio por sessão
- Horário do dia em que você mais estuda

**Desempenho**
- Taxa de acerto ao longo do tempo (deveria subir)
- Taxa de acerto por tipo de exercício (pode ir bem em flashcard e mal em áudio)
- Palavras mais difíceis (mais erros, ou decaimento rápido mesmo com revisão)

**Origem e padrões**
- De onde vêm suas palavras — jogo, livro, série (pizza)
- Que tipo de coisa mais te pega, usando o motivo da captura como tag: verbos com
  múltiplos sentidos, falsos cognatos, gírias

**Nível estimado (CEFR)** — comparar palavras dominadas com uma lista pública de
frequência. Não é prova de proficiência, é uma proxy motivadora. É o único item que
depende de fonte externa, mas é integração de uma vez só.

### Sobre o app, não sobre o seu inglês

Este grupo não mede seu progresso — mede se o funil (captura → processamento →
revisão → domínio) está vazando em algum ponto. **Importa mais do que parece: se o
funil vazar entre captura e processamento, o problema não é falta de motivação sua,
é fricção do app — e isso é acionável.**

- **Funil de conversão**: quantas capturas viram fichas, quantas fichas chegam a ser
  revisadas. Mostra exatamente onde você perde palavras.
- **Backlog do inbox ao longo do tempo**: se essa linha só cresce, você captura mais
  rápido do que processa. Provavelmente o alerta mais importante de todos.
- **Latência captura → processamento**: quanto maior, menor a chance de você voltar.
- **Eficiência por método**: qual formato (foto, áudio, texto) mais fica parado sem
  processar. Pode revelar que áudio é rápido de gravar e chato de transcrever.
- **Eficiência por origem**: pode mostrar que palavras do Kindle, que já vêm com
  contexto rico, chegam a "dominada" mais fácil.
- **Duplicidade**: quantas vezes você captura a mesma palavra sem perceber — sinal de
  que o app poderia avisar "você já tem isso".
