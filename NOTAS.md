# Notas

Coisas que não cabem no roadmap nem na arquitetura: o que ainda não foi provado,
o que está torto e o que talvez valha a pena. Uma linha cada.

## Pendências de teste

- **Captura em menos de 10s** (Fase 1) — só fecha com você digitando de verdade; injetar texto por adb não representa ninguém.
- **Captura em menos de 5s nos 3 contextos** (Fase 1.5) — precisa de PC, Kindle e DS na mão, com as mãos ocupadas.
- **Fase 2 inteira** — a matemática foi verificada envelhecendo o banco, mas ninguém revisou por 7 dias seguidos ainda.

## Inconsistências conhecidas

- Não há tela para preencher `origem`. Ela já é exibida ("Foto do Kindle", "Seu contexto" da ficha) e não tem por onde entrar — o campo foi retirado da captura por quase nunca ser preenchido, e nada tomou o lugar dele.
- Duas escalas usam os mesmos três nomes. Em Palavras e na Ficha, "dominada" é força de memória: uma palavra acertada uma vez só carrega o rótulo por algumas horas, porque a memória está mesmo fresca. Nas contagens (Início, Você, Progresso, O que falta) "dominada" é degrau 5, que pede quatro acertos. Os dois estão certos para a pergunta que respondem, e ainda assim a mesma palavra pode aparecer com rótulos diferentes em duas telas.
- O botão da captura diz "Guardar 2 capturas" porque é o texto do handoff, mas o que 2 seleções criam é 1 captura e 2 entradas. O handoff se contradiz aqui: a nota dele diz "uma captura pode render várias fichas". Vale decidir o nome antes de traduzir a interface.
- A onda do player da transcrição é fixa: dez barras de altura decorativa, não a amplitude do áudio.
- A sequência de dias (`dia_revisado`) é global e aparece dentro de uma tela que é por curso. Foi decidido assim de propósito — o hábito é um só, e revisar alemão terça e inglês quarta não devia quebrar nada — mas quem lê "5 dias seguidos" no progresso do inglês não tem como saber disso. Na tela Você isso está resolvido: os três números do topo somam tudo e a quebra por idioma vem depois.
- OCR e voz não seguem o curso. O modelo latino do ML Kit é o único empacotado, então capturar por foto em japonês devolve lixo ou nada, e a tela cai na edição manual sem explicar por quê. Agora que o idioma é escolhido antes de capturar, dá para avisar na própria folha — a informação existe no momento certo e não está sendo usada.
- A faixa de idiomas rola sem limite. O handoff sugere 5 fixos + "ver todos" a partir do sexto curso; com três ela cabe, e o corte não foi implementado. Quem entrar em oito vai arrastar bastante.
- "Remover idioma" mora dentro de "Seu progresso · idioma", e não na tela Você. É alcançável a partir dela (linha → progresso → remover), mas o handoff descreve a remoção como algo do perfil. Uma linha da lista com ação de remover à direita resolveria, ao custo de um alvo de toque a mais numa lista que já é uma porta.
- A tela "Guardado" fecha sozinha em 3,5s **só quando todas as fichas ficaram prontas**. O handoff diz que ela sempre fecha; sumir com a barra de "montando o sentido" no meio contradiz a própria tela, então o timer espera. Vale conferir se o comportamento parece travado quando a IA demora.

## Ideias

- Capturar pelo compartilhar do sistema, sem abrir o app.
- Transcrever um pendente num toque só. O caminho barato não é a IA generativa: o `SpeechRecognizer` do Android transcreve áudio de graça e o ML Kit faz OCR na foto sem rede — é o mesmo raciocínio da seção "Onde a IA não é necessária" do PRODUTO.md. A IA entraria só para limpar o texto, se entrar. O botão devolve o resultado nos campos, e você confirma antes de gerar a ficha: transcrição automática errada que vira ficha sozinha custa dinheiro e produz lixo.
- Taxa de acerto por tipo de exercício, quando houver mais de um exercício (hoje só existe o flashcard).
- Traduzir a interface. Problema separado do prompt e bem maior: todas as strings estão no código, sem recursos do Android. Ficou mais visível agora que dá para aprender 43 idiomas com a interface toda em português.
