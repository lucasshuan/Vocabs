# Interface

Critérios de layout que valem para o app inteiro. Não é guia de estilo — cor,
tipografia e formas moram no [Theme.kt](../androidApp/src/main/kotlin/com/jean/vocabs/ui/theme/Theme.kt),
que é o lugar certo para isso. Aqui ficam as decisões que a gente esqueceria e
tornaria a repetir errado.

## A zona do polegar manda no que é ação

**Ação vai embaixo. Informação vai em cima.**

O aparelho é segurado com uma mão e operado com o polegar, que descreve um arco a
partir do canto inferior. O topo da tela é a região mais difícil de alcançar — em
telas de 6,5" você precisa reposicionar o aparelho na mão para tocar lá.

Isso inverte o instinto de web, onde a chamada para ação vai no topo porque o
mouse alcança tudo igual. Aqui:

- **Embaixo:** botões de captura, confirmar, responder um cartão, a barra de abas.
- **Em cima:** saudação, contagens, títulos, qualquer coisa que se lê e não se toca.

A captura mostra por que isso precisa ser **ancorado** e não só ordenado. A
primeira versão a punha por último na coluna rolável da Início, o que parecia
suficiente — mas num dia sem nada a revisar o conteúdo encolhia, e os botões
subiam para o terço superior da tela, exatamente onde o polegar não chega.

Hoje ela mora no **botão central da barra inferior**, que é o alvo mais fácil de
acertar com o polegar de qualquer mão. Isso resolve o problema de vez: a posição
não depende de conteúdo, de tela nem de rolagem, e capturar deixou de custar uma
troca de aba — de qualquer lugar do app são dois toques (abrir o leque, escolher
o formato), e gravar áudio para no mesmo lugar em que começou, porque o próprio
botão vira o "parar".

Exceções conscientes:

- O botão "voltar" fica no topo à esquerda porque é onde o Android inteiro o
  coloca, e contrariar isso custa mais do que o alcance.
- Telas que abrem com o teclado em pé (a de Capturar, a de Transcrever) não
  seguem a regra: metade da tela já está ocupada pelo teclado e a noção de
  "embaixo" muda. Lá a ordem é a da leitura.

## A captura nunca pode custar um toque a mais

O princípio 1 do [PRODUTO.md](PRODUTO.md) é "captura antes de tudo", e o critério
de saída da Fase 1 é capturar em menos de 10 segundos. Cada toque e cada
transição de tela come esse orçamento.

Consequências práticas já pagas:

- A captura fica na barra, alcançável de qualquer aba — nunca é preciso navegar
  até uma tela para começar.
- O campo de trecho pede foco sozinho, então o teclado sobe junto com a tela.
- Áudio começa a gravar no toque em "Áudio", sem tela de confirmação. A gravação
  vive na barra, então trocar de aba no meio dela não a perde.
- Salvar fecha a tela na hora; a ficha é gerada em background.
