# Interface Tagarara

O handoff em `handoff/` é a referência das catorze telas, em claro e escuro. O
tema é escolhido em Configurações — claro, escuro ou seguindo o aparelho — e usa
Figtree no corpo e Bricolage Grotesque em títulos e termos, ambas empacotadas.

## Semântica visual

- **Ameixa:** marca, seleção e ações.
- **Menta:** progresso, memória e conclusão.
- **Lilás-cinza:** superfícies de apoio e estados neutros.
- **Vermelho:** somente erros e ações destrutivas.

Nenhuma cor decora. O cartão claro tem contorno de 1 px para se separar do fundo
quase branco; o escuro não tem nenhum, porque a superfície já se destaca sozinha
— `LocalTemaEscuro` existe para que essa regra seja decidida num lugar só.

Todo alvo de toque tem pelo menos 48 dp. Conteúdo crítico é rolável para 340×716,
teclado aberto e fonte ampliada.

## Logo

São dois desenhos, e os masters ficam em `docs/marca/`:

- `logo-tagarara.png` — o papagaio recortado, usado no cabeçalho do Início e na
  splash, sobre o fundo da tela.
- `logo-app-externo.png` — a versão redonda, usada **só** no ícone do launcher.

O ícone é um adaptive icon: a tela tem 108 dp e o launcher recorta os 72 dp
centrais antes de aplicar a máscara. O disco é gerado cobrindo 76 dp — maior que
o recorte de propósito, para que a máscara corte o próprio disco e nunca revele o
fundo. É daí que vinha o halo: com a arte terminando dentro do recorte, o que
aparecia em volta era a borda verde do papagaio e, depois, o anel de ameixa do
`background`. Ao regerar o PNG, escale pelo **menor** lado da caixa do conteúdo —
o disco de origem é levemente achatado (846×823) e escalar pelo maior deixa uma
fresta em cima e embaixo.

`minSdk` 26 torna o adaptive icon universal: não existe variante legada de ícone.

## Componentes compartilhados

`ui/components/` guarda o que se repete entre telas. Um elemento que aparece em
dois lugares vira componente antes de divergir num terceiro:

| Componente | Onde aparece |
|---|---|
| `CartaoDaTela` | toda superfície de conteúdo, em todas as telas |
| `CartaoMetrica` | Início, Perfil, resumo da Revisão |
| `LinhaDeLista` | Pendentes, Perfil, aviso de captura do Início |
| `RotuloDeSecao` | rótulo de apoio de todas as seções |
| `BotaoPrincipal` / `AcaoSecundaria` | ação principal e saída discreta de cada tela |
| `PilulaSelecionavel` | filtros de Palavras e abas de formato da Captura |
| `SeletorDeTermos` / `ChipsDeSelecao` | Captura e Transcrever |
| `BarraDeMemoria` | cartão de Palavras (curta) e ficha (inteira) |
| `TipoBadge` | Palavras e Ficha |
| `EstadoVazio` | Palavras, Pendentes, Revisão, Novo idioma, Dia a dia e O que falta |
| `ParDeIdiomas` / `BandeiraCircular` | Início, Você e Novo idioma |
| `CabecalhoDeDentro` | as cinco páginas de dentro |
| `FaixaDaSemana` | Seu progresso e Dia a dia |
| `AnelDeProgresso` | força média do Início e estoque do Progresso |
| `LinhaDeUsoDeIa` | Você e Seu progresso |

## Bandeiras

São a coleção **circle-flags** (MIT, atribuição em `docs/TERCEIROS.md`),
convertida de SVG para VectorDrawable sem redesenhar nada — desenhos feitos à mão,
com as proporções e os brasões certos. 43 arquivos, 37 KB no total, em
`res/drawable/bandeira_*.xml`.

Duas alternativas foram descartadas. Emoji depende da fonte do sistema e vira
retângulo ou duas letras em vários aparelhos. Desenhar em `Canvas` funcionava
enquanto havia duas bandeiras e não escala para 43: a do Brasil não é um losango
com um círculo e a da Coreia do Sul não é desenhável de cabeça.

A máscara circular dos SVGs originais não foi convertida — quem recorta é o
`Modifier.clip` do Compose, que tem antialiasing de verdade, enquanto um
`clip-path` dentro do VectorDrawable serrilha a borda em algumas versões. O mapa
de código para `R.drawable` é explícito porque `getIdentifier()` é invisível para
o R8: as 43 bandeiras sairiam do APK e só a tela mostraria o estrago.

## Navegação

A barra inferior tem Início, Palavras, captura central, Pendentes e Perfil, só
com ícones — os rótulos sob eles competiam com o botão de captura, que precisa ser
o alvo óbvio; o nome sobrevive no `contentDescription`.

O botão central abre um **leque** com Texto, Áudio e Foto. O handoff pedia que ele
fosse direto para a tela de abas, e a decisão foi revista: o formato é a primeira
escolha de toda captura, e resolvê-la antes de a tela abrir tira um toque do
caminho que precisa caber em segundos. As abas continuam lá dentro — quem entrou
por Foto e mudou de ideia troca sem sair.

Três detalhes do leque não são enfeite:

- **Arco, não coluna.** Uma coluna cresceria por cima da lista e a última opção
  ficaria longe do polegar. O arco mantém as três à mesma distância do botão.
- **O `+` gira 45° e vira `×`.** O mesmo alvo desfaz o que ele fez.
- **Saída escalonada, do centro para fora.** Mostra de onde as opções vieram. A
  abertura tem mola com quique; o fechamento é curto e seco, porque desfazer não
  é momento de se demorar.

O véu que escurece o fundo é o alvo de fechar — é o gesto que se tenta antes de
procurar o botão — e o "voltar" do sistema fecha o leque antes de sair da tela.

O botão **não** mora dentro de `BarraInferior`: `Surface` recorta o próprio
conteúdo, e o arco precisa passar da borda de cima da barra. A barra deixa o meio
vago e a camada de cima compõe o leque sobre o vão, com os mesmos insets e a
mesma `ALTURA_DA_BARRA`.

Progresso, Dia a dia, O que falta e Configurações são **páginas de dentro**: abrem
com voltar no topo, a barra continua visível e a aba Perfil continua acesa. Novo
idioma sobe em tela cheia, como Captura e Revisão, porque é uma escolha que
termina em confirmação.

## Seleção de termos

Toque escolhe uma palavra, arraste escolhe uma expressão contígua. **O realce vive
só enquanto o dedo está na tela.** Terminado o gesto, o trecho volta ao normal e o
que foi escolhido aparece embaixo como pílula, com o ✕ que desfaz. Intervalos
podem se sobrepor (`fence` e `on the fence` ao mesmo tempo), e pintá-los todos de
uma vez viraria uma mancha que não diz mais quantas seleções existem — a lista de
pílulas diz. Editar o trecho limpa todas as seleções.

## Telas

- **Início:** logo e par de idiomas, anel de força média, saudação por horário,
  CTA de revisão, aviso de capturas paradas, três números e as fichas do dia.
- **Palavras:** coluna única, busca por alvo/tradução, filtros de memória e cartão
  com barra curta, nível e quando a palavra volta ("revisar agora" / "em 2d 4h").
- **Captura:** abas de formato já na escolhida no leque, trecho único e o seletor;
  o botão guarda quantas capturas foram marcadas.
- **Pendentes:** capturas cruas com disco de formato acima das fichas em geração.
- **Transcrever:** mídia, transcrição local editável e o mesmo seletor.
- **Ficha:** termo, pronúncia e tradução no topo; força de memória, seu contexto
  (com a barra de ameixa), definições e termos relacionados; compartilhar e
  excluir ficam no overflow.
- **Revisão:** barra de progresso no topo, cloze contra o próprio trecho com a
  lacuna sublinhada em ameixa, resposta digitada e "Não lembro" como saída.
- **Você** (aba Perfil): idioma de partida, faixa horizontal de cursos com "N de
  M", cartão de progresso do curso aberto, consumo de IA, configurações e
  exportação. A faixa sangra até as bordas de propósito — o corte do último
  cartão é o que diz que ela anda.
- **Seu progresso:** a semana com a quota do dia, o estoque de palavras em anel e
  faixas, e os três números de apoio. Os dois primeiros blocos são portas.
- **Dia a dia:** a mesma semana e, abaixo, o que aconteceu em cada dia. Dia sem
  nada aparece escrito; pular os vazios encostaria terça em sexta.
- **O que falta:** cada palavra com o degrau em que está e quantos acertos faltam
  para o próximo nome, mais o resumo das já dominadas.
- **Configurações:** só tema, num segmentado de largura total.
- **Novo idioma:** busca, os cursos que já existem e a lista do que sobra. Serve
  também para trocar o idioma de partida — mesma lista, mesma linha, outro verbo
  no botão.

Estados de carregamento, vazio, processamento e erro fazem parte da tela e nunca
substituem conteúdo real por exemplos fixos.
