# Notas

Coisas que não cabem no roadmap nem na arquitetura: o que ainda não foi provado,
o que está torto e o que talvez valha a pena. Uma linha cada.

## Pendências de teste

- **Captura em menos de 10s** (Fase 1) — só fecha com você digitando de verdade; injetar texto por adb não representa ninguém.
- **Captura em menos de 5s nos 3 contextos** (Fase 1.5) — precisa de PC, Kindle e DS na mão, com as mãos ocupadas.
- **Fase 2 inteira** — a matemática foi verificada envelhecendo o banco, mas ninguém revisou por 7 dias seguidos ainda.

## Inconsistências conhecidas

- O app abre em "Palavras", então toda captura custa um toque a mais — atrito contra o princípio 1 ("captura antes de tudo").
- `InboxViewModel.tentarDeNovo` existe e não é chamado por ninguém.
- Origem quase nunca é preenchida em captura de foto/áudio: o campo fica abaixo dos botões de mídia, e você já saiu da tela.
- Uma palavra acertada uma vez só já aparece como "Dominada" por algumas horas — é verdade que a memória está fresca, mas o rótulo soa forte cedo demais.

## Ideias

- Capturar pelo compartilhar do sistema, sem abrir o app.
- Taxa de acerto por tipo de exercício, quando houver mais de um exercício (hoje só existe o flashcard).
