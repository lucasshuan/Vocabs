# Notas

Coisas que não cabem no roadmap nem na arquitetura: o que ainda não foi provado,
o que está torto e o que talvez valha a pena. Uma linha cada.

## Pendências de teste

- **Captura em menos de 10s** (Fase 1) — só fecha com você digitando de verdade; injetar texto por adb não representa ninguém.
- **Captura em menos de 5s nos 3 contextos** (Fase 1.5) — precisa de PC, Kindle e DS na mão, com as mãos ocupadas.
- **Fase 2 inteira** — a matemática foi verificada envelhecendo o banco, mas ninguém revisou por 7 dias seguidos ainda.

## Inconsistências conhecidas

- Não há tela para preencher `origem`. Ela já é exibida ("Foto do Kindle", "Seu contexto" da ficha) e não tem por onde entrar — o campo foi retirado da captura por quase nunca ser preenchido, e nada tomou o lugar dele.
- Uma palavra acertada uma vez só já aparece como "dominada" por algumas horas — é verdade que a memória está fresca, mas o rótulo soa forte cedo demais.
- O botão da captura diz "Guardar 2 capturas" porque é o texto do handoff, mas o que 2 seleções criam é 1 captura e 2 entradas. O handoff se contradiz aqui: a nota dele diz "uma captura pode render várias fichas". Vale decidir o nome antes de traduzir a interface.
- A onda do player da transcrição é fixa: dez barras de altura decorativa, não a amplitude do áudio.

## Ideias

- Capturar pelo compartilhar do sistema, sem abrir o app.
- Transcrever um pendente num toque só. O caminho barato não é a IA generativa: o `SpeechRecognizer` do Android transcreve áudio de graça e o ML Kit faz OCR na foto sem rede — é o mesmo raciocínio da seção "Onde a IA não é necessária" do PRODUTO.md. A IA entraria só para limpar o texto, se entrar. O botão devolve o resultado nos campos, e você confirma antes de gerar a ficha: transcrição automática errada que vira ficha sozinha custa dinheiro e produz lixo.
- Taxa de acerto por tipo de exercício, quando houver mais de um exercício (hoje só existe o flashcard).
- Deixar o usuário escolher idioma nativo e alvo. O prompt já é parametrizado (`ParDeIdiomas`); falta o contrato levar os dois idiomas, o banco guardar em que par cada entrada nasceu (senão regerar ficha antiga vira lixo), o TTS sair de `Locale.US` e o campo `ipa` virar `pronuncia` — IPA não serve para mandarim nem japonês.
- Traduzir a interface. Problema separado do prompt e bem maior: todas as strings estão no código, sem recursos do Android.
