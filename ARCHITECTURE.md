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

Dentro de `:androidApp`, cada tela é uma pasta com `Screen` e `ViewModel`, e
`ui/components` guarda o que se repete entre elas — cartão, linha de lista,
métrica, pílula, seletor de termos, bandeiras, a faixa de idiomas e as cores de
categoria. Nada de estilo mora numa tela só por já estar ali: o mesmo elemento em
dois lugares vira componente antes de divergir num terceiro.

Duas convenções globais que o handoff de idiomas trouxe:

- **Uma cor por tipo de captura** (`ui/components/Categorias.kt`): texto é
  ameixa, áudio é menta, foto é o vermelho do papagaio. A tripla aparece nos alvos
  do leque do `+` e nos discos de Pendentes, e é o que forma a associação no ato
  da captura. O vermelho é categoria, nunca erro. A única exceção é `corDeDescarte`
  — o `+` vermelho de "solte aqui para descartar" —, e ela se paga porque só existe
  **durante a gravação**, quando o alvo da foto já recolheu: os dois sentidos nunca
  dividem a tela. Um segundo vermelho vindo do `error` do tema seria quase igual ao
  primeiro, que é o jeito garantido de tornar os dois ilegíveis.
- **Toda bandeira da faixa tem selo** (`ui/components/FaixaDeIdiomas.kt`): número
  em ameixa quando há o que revisar, tique em menta quando está em dia, ampulheta
  cinza quando o curso ainda não tem nada agendado. Nunca vazio, nunca um "0"
  escrito — o zero é a boa notícia da faixa. A ordem é fixa: o deslize do
  carrossel depende de a posição não mudar.
- **Três durações e três molas** (`ui/components/Movimento.kt`): toda animação sai
  de `Movimento`. `RAPIDO` (150 ms) para o que só reage, `PADRAO` (240 ms) para o
  que entra e sai, `AMPLO` (620 ms) só para o que se lê enquanto corre — o arco do
  anel, a barra da quota, um número contando. A regra que decide entre eles:
  **nada pelo que se espera passa de `PADRAO`**. Entradas são sempre mais longas
  que saídas, porque quem fecha já decidiu sair.

A terceira mola é `molaDeGesto`, mais dura que as outras duas: ela move os alvos
do leque, e um alvo que leva 300 ms para chegar ao lugar ainda está viajando
quando o dedo já decidiu para onde vai. `movimentoReduzido()` lê a escala de
animação do sistema — com ela em zero o halo do `+` some e o botão fica parado.

Do vocabulário de movimento saem quatro peças que as telas reusam: `encolheAoTocar`
(o cartão cede sob o dedo — está dentro de `CartaoDaTela`, não em cada chamada),
`entradaSuave` (a chegada escalonada, com teto de 5 itens e proibida em lista
lazy, onde o certo é `animateItem`), `fracaoAnimada` (devolve `State` para que o
`Canvas` leia o valor na fase de desenho em vez de recompor a cada quadro) e
`contagemAnimada` (só para conquista acumulada; fila e dívida não contam do zero,
senão o atraso vira placar).

O fluxo de captura começa num **gesto**, não numa tela, e cada passo já é
durável. `ui/captura/` guarda as quatro peças: `GestoDeCaptura.kt` é a geometria
pura — onde ficam os três alvos e que alvo um deslocamento escolhe;
`HubDeCaptura.kt` é o `+`, o leque, o véu e a gravação, tudo num overlay de tela
cheia que não intercepta toque nenhum além do próprio botão; `GavetaDeTexto.kt` é
o campo que o toque solto abre; `AvisoDeCaptura.kt` é o cartão de 5 s que confirma
e oferece o atalho. Depois disso a seleção marca os termos (`Selecionar`) e a
confirmação mostra o que entrou enquanto a IA trabalha (`Guardado`).

Três regras do gesto, e todas se pagam:

- **A escolha do alvo é por ângulo, não por acerto do disco.** Passou de 56 dp do
  `+`, o setor em que o dedo está já marca o alvo e o disco correspondente cresce.
  Um menu radial que exija alcançar fisicamente um alvo a 166 dp obriga a mão a se
  reposicionar no meio do gesto, e é assim que um atalho de um gesto só vira dois.
- **A gravação começa ao entrar no alvo, não ao soltar** — o áudio do instante em
  que o dedo chega já está no arquivo. `MINIMO_DE_GRAVACAO_MS` (0,8 s) descarta o
  toque que passou do limiar por acidente, e mede em milissegundos: comparado
  contra um contador de segundos inteiros, o corte descartava toda gravação curta
  e nenhuma outra.
- **Não existe botão de parar.** O dedo não sai da tela entre abrir o leque e
  guardar, e voltar ao `+` desfaz pelo mesmo caminho que fez.

## Captura e fichas

```text
captura (contexto, formato, mídia, transcrição, par de idiomas)
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

## Cursos

O par de idiomas mora na **captura**, não na entrada: um trecho está numa língua
só, e toda seleção dentro dele herda essa língua por construção. É o que permite
regerar uma ficha antiga no idioma em que ela nasceu depois de a pessoa trocar de
curso — o pedido leva o par (`GerarFichaRequest`), e o servidor recusa um par que
não esteja no catálogo em vez de cair no padrão.

O catálogo de idiomas fica em `:contracts` porque os dois lados precisam da mesma
lista por motivos diferentes: a interface mostra o nome em português e a bandeira,
o prompt cita o nome em inglês, o banco guarda o código. Do idioma alvo o servidor
só precisa saber mais uma coisa — em que notação a pronúncia se escreve (IPA por
padrão, pinyin no mandarim, kana + romaji no japonês).

Qual curso está aberto é preferência do aparelho (`Preferencias`) e entra no
repositório como fluxo. **Só a Início é recortada por ele**: ela é um carrossel
com uma página por curso, e deslizar entre páginas *é* trocar o curso aberto.
Vocabulários, Pendentes e Você mostram sempre os três idiomas juntos, com o
idioma marcado item a item — um filtro que continuasse ligado ao mudar de aba
faria palavras sumirem sem que ninguém tivesse pedido.

Os três recortes convivem via `Escopo`, um parâmetro com padrão em toda leitura
recortável de `VocabRepository`:

```text
Escopo.CursoAberto   padrão — Início, revisão, geração
Escopo.Curso(alvo)   "Seu progresso · francês", sem trocar o curso aberto
Escopo.Todos         Vocabulários, Pendentes, Você
```

O filtro é aplicado em memória, e não em SQL: o curso aberto é um fluxo de
preferência, e uma consulta parametrizada por ele reabriria o cursor a cada
deslize do carrossel.

O idioma é decidido **no ato da gravação**, não no da seleção, e **não é
perguntado**: a captura vai para o curso aberto no hub, congelado no instante em
que o dedo desce, e `capturarTexto`/`capturarTrecho`/`capturarMidia` aceitam o
par escolhido. Por isso toda linha de Pendentes tem idioma no subtexto, e por
isso um texto colado que nunca chegou ao "Guardar" fica na fila com a língua
certa em vez de se perder. `alterarIdiomaDaCaptura` conserta a escolha enquanto a
captura não virou fichas — depois disso existem entradas nascidas nesse par.

A folha que pedia o destino antes de deixar capturar foi removida com o handoff
do gesto: ela cobrava um toque em toda captura para acertar as poucas em que o
curso não era o que estava na tela, e a correção já existia em Pendentes, onde o
erro é visível e desfazer custa um toque.

A troca de curso saiu do perfil; lá ficaram só adicionar, remover
(`Preferencias.desmatricular`, que nunca esvazia a lista) e o idioma-base.

## Estados

- `Captura`: `TRANSCREVENDO`, `AGUARDANDO_SELECAO`, `PROCESSADA`.
- `Entrada`: `PENDENTE`, `GERANDO`, `PRONTA`, `ERRO`.

Pendentes combina as duas filas sem misturá-las: transcrição/seleção pertence à
captura; geração pertence à entrada. Nada é descartado sem a pessoa pedir —
cancelar a seleção, fechar o app ou perder a conexão no meio da transcrição
deixa a captura em `AGUARDANDO_SELECAO`, com o idioma já escolhido, pronta para
continuar de onde parou. É o que faz gravar primeiro nunca custar nada.

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
uma única vez no final da sessão. `dia_revisado` alimenta sequência e semana.
`uso_ia` usa chave `YYYY-MM`, portanto vira naturalmente no mês local.

Sobre a mesma retenção existem **duas** leituras, e elas respondem perguntas
diferentes:

- **Força de memória** (`pontosEm`): quanto se lembra agora. Decai sozinha com o
  tempo. É o que a ficha e a lista de Palavras mostram.
- **Degrau** (`Degraus`, 1 a 5): quão longe se chegou. Sai da taxa de decaimento,
  que já é o histórico de acertos comprimido, e só muda quando um cartão é
  respondido. É o que "O que falta" mostra, e o que conta as dominadas em toda
  tela de número — força de memória daria um total diferente a cada hora.

`evento` é a linha do tempo, append-only: captura, ficha pronta, acerto, erro e
mudança de nível, cada linha com o dia local já resolvido. Ela existe porque a
retenção guarda só o estado de agora e não responde "o que eu fiz terça". A
migração reconstrói apenas as capturas — o desfecho das revisões antigas não
estava guardado, pelo mesmo motivo que `acertos` e `erros` nasceram em zero na
migração 3.

A **quota do dia** não é meta escolhida: é o que já saiu hoje mais o que ainda
está na fila. Uma meta fixa seria inalcançável no dia em que 30 palavras vencem
juntas e já estaria batida num dia sem nada a revisar.

## Validação

`androidHostTest` usa SQLite JDBC para executar as migrações com dados legados e
testar criação em lote, sobreposição, retenção de mídia, concorrência parcial,
atividade, virada mensal, escopo de curso, quota, degraus e linha do tempo.
`verifySqlDelightMigration` compara migrações com o schema novo.
