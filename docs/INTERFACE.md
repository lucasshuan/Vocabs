# Interface Tagarara

O handoff em `handoff/` é a referência das oito telas. O tema segue o sistema e
usa Figtree no corpo e Bricolage Grotesque em títulos/termos, ambas empacotadas.

## Semântica visual

- **Ameixa:** marca, seleção e ações.
- **Menta:** progresso, memória e conclusão.
- **Lilás-cinza:** superfícies de apoio e estados neutros.
- **Vermelho:** somente erros e ações destrutivas.

O logo do papagaio aparece no cabeçalho, splash e launcher. Todo alvo de toque
tem pelo menos 48 dp. Conteúdo crítico é rolável para 340×716, teclado aberto e
fonte ampliada.

## Navegação

A barra inferior tem Início, Palavras, captura central, Pendentes e Perfil. O
botão central abre diretamente a tela com abas Texto/Áudio/Foto; não há leque
flutuante.

## Telas

- **Início:** logo/par de idiomas, força média, CTA de revisão, captura mais
  antiga, totais e fichas do dia.
- **Palavras:** coluna única, busca por alvo/tradução e filtros de memória.
- **Captura:** trecho único; toque seleciona token e arraste seleciona intervalo.
  Editar o trecho limpa todas as seleções.
- **Pendentes:** separa transcrição/seleção de capturas e geração de fichas.
- **Transcrever:** mídia, resultado local editável e o mesmo seletor multi-alvo.
- **Ficha:** conteúdo e memória; relacionados expandem localmente; compartilhar e
  excluir ficam no overflow.
- **Revisão:** cloze digitado, feedback imediato e resposta revelada no erro.
- **Perfil:** heatmap de 84 dias, métricas, idiomas, IA mensal e exportação.

Estados de carregamento, vazio, processamento e erro fazem parte da tela e nunca
substituem conteúdo real por exemplos fixos.
