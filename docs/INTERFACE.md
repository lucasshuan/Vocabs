# Interface Tagarara

O handoff em `handoff/` é a referência das oito telas, em claro e escuro. O tema
segue o sistema e usa Figtree no corpo e Bricolage Grotesque em títulos e termos,
ambas empacotadas.

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
| `EstadoVazio` | Palavras, Pendentes e Revisão |
| `ParDeIdiomas` / `BandeiraCircular` | cabeçalho do Início e linha do Perfil |

As bandeiras são desenhadas, não emoji: o indicador regional depende da fonte do
sistema e vira retângulo ou duas letras em vários aparelhos.

## Navegação

A barra inferior tem Início, Palavras, captura central, Pendentes e Perfil, só
com ícones — os rótulos sob eles competiam com o botão de captura, que precisa ser
o alvo óbvio; o nome sobrevive no `contentDescription`. O botão central abre
direto a tela com abas Texto/Áudio/Foto; não há leque flutuante.

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
- **Captura:** abas de formato, trecho único e o seletor; o botão guarda quantas
  capturas foram marcadas.
- **Pendentes:** capturas cruas com disco de formato acima das fichas em geração.
- **Transcrever:** mídia, transcrição local editável e o mesmo seletor.
- **Ficha:** termo, pronúncia e tradução no topo; força de memória, seu contexto
  (com a barra de ameixa), definições e termos relacionados; compartilhar e
  excluir ficam no overflow.
- **Revisão:** barra de progresso no topo, cloze contra o próprio trecho com a
  lacuna sublinhada em ameixa, resposta digitada e "Não lembro" como saída.
- **Perfil:** heatmap de 12 semanas com a sequência, três números, par de idiomas,
  gerações por IA do mês e exportação.

Estados de carregamento, vazio, processamento e erro fazem parte da tela e nunca
substituem conteúdo real por exemplos fixos.
