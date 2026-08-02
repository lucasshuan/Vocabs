# Arquitetura da Tagarara

O nome público é **Tagarara**. `applicationId`, namespace, nome do banco e rotas
internas continuam `com.jean.vocabs`/Vocabs para atualizar instalações existentes
sem perder dados.

## Módulos

```text
:contracts   DTOs serializáveis compartilhados por app e servidor
:shared      domínio, SQLDelight, retenção e cliente HTTP
:androidApp  Compose, OCR, voz, mídia e exportação
:server      endpoint Ktor e chamada estruturada à IA
```

## Captura e fichas

```text
captura (contexto, formato, mídia, transcrição)
   ├── entrada (intervalo selecionado, tipo, ficha, retenção)
   └── entrada (outro intervalo, inclusive sobreposto)
```

`captura` é o sinal bruto. `entrada` é um alvo selecionado e pode existir sem uma
ficha enquanto a geração está pendente. A migração `4.sqm` transforma cada linha
legada em uma captura-pai e, quando havia alvo, em uma entrada-filha com o mesmo
ID, ficha, erros e histórico de retenção.

Texto cria a captura e todas as entradas numa transação. Os limites são
`[inicio, fim)` e permanecem ligados ao trecho original para o cloze. Uma palavra
selecionada vira `PALAVRA`; dois ou mais tokens contíguos viram `EXPRESSAO`. A IA
recebe esse tipo e o servidor o reinjeta na resposta — não há classificação remota.

As fichas são geradas independentemente com semáforo de duas requisições. Falha
de uma entrada não desfaz as irmãs. Apenas respostas gravadas com sucesso somam
ao `uso_ia` do mês.

Ao excluir uma entrada, o repositório conta as irmãs na mesma transação. A mídia
só é removida quando a última entrada ou a captura inteira desaparece.

## Estados

- `Captura`: `TRANSCREVENDO`, `AGUARDANDO_SELECAO`, `PROCESSADA`.
- `Entrada`: `PENDENTE`, `GERANDO`, `PRONTA`, `ERRO`.

Pendentes combina as duas filas sem misturá-las: transcrição/seleção pertence à
captura; geração pertence à entrada.

## Mídia local

Fotos e áudios vivem em `filesDir/capturas`. Fotos passam pelo modelo latino
empacotado do ML Kit. Áudio é WAV PCM 16 kHz mono. Em API 33+ o arquivo PCM é
entregue ao `SpeechRecognizer` local; sem API/modelo ou em caso de falha, a
captura passa para edição manual.

O ZIP de exportação é criado em `cacheDir/exportacoes`, contém `tagarara.json`
com `schemaVersion` e as mídias referenciadas, e é compartilhado por
`FileProvider` com permissão temporária de leitura.

## Retenção e atividade

Cada entrada pronta mantém pontos e taxa de decaimento. Abaixo de 60 entra na
fila. A revisão registra apenas a primeira tentativa; um erro recoloca o cartão
uma única vez no final da sessão. `dia_revisado` alimenta sequência e heatmap de
84 dias. `uso_ia` usa chave `YYYY-MM`, portanto vira naturalmente no mês local.

## Validação

`androidHostTest` usa SQLite JDBC para executar a migração com dados legados e
testar criação em lote, sobreposição, retenção de mídia, concorrência parcial,
atividade e virada mensal. `verifySqlDelightMigration` compara migrações com o
schema novo.
