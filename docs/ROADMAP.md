# Roadmap Tagarara

## Base implementada

- [x] Marca pública Tagarara, tema claro/escuro/auto, logo, fontes, splash e launcher.
- [x] Catorze telas conforme o handoff, sobre uma camada de componentes compartilhados.
- [x] 43 idiomas com bandeiras reais, curso ativo, matrícula e troca de idioma nativo.
- [x] Par de idiomas por captura, contrato e prompt parametrizados, voz por locale.
- [x] Escada de degraus, quota do dia e linha do tempo de eventos.
- [x] Captura-pai com múltiplas entradas, inclusive intervalos sobrepostos.
- [x] Migração preservando IDs, fichas, erros, retenção e histórico.
- [x] Seleção local palavra/expressão e aviso não bloqueante de duplicata.
- [x] Texto em lote e geração independente com concorrência máxima de duas.
- [x] OCR latino empacotado e áudio WAV com voz local em API 33+.
- [x] Fallback manual obrigatório para toda transcrição.
- [x] Cloze digitado com primeira tentativa única e repetição de erro.
- [x] Ficha com contexto, memória e termos relacionados.
- [x] Heatmap de 84 dias, métricas e consumo mensal informativo de IA.
- [x] Exportação ZIP versionada com mídia por FileProvider.
- [x] Testes host para migração, seleção, retenção, lote, concorrência, atividade,
  virada mensal e limpeza de mídia.

## Aceitação de campo

- [ ] Cronometrar captura Texto/Áudio/Foto em PC, leitura e console portátil.
- [ ] Validar voz com modelo inglês instalado e fallback sem modelo em aparelho.
- [ ] Validar OCR com iluminação, inclinação e texto pequeno reais.
- [ ] Usar revisão por sete dias e calibrar limiar/decaimento com dados reais.
- [ ] Inspecionar claro/escuro em 340×716, aparelho grande, fonte ampliada e IME.
- [ ] Gerar fichas de verdade num idioma não latino (mandarim, japonês, árabe) e
  conferir se a notação de pronúncia volta como o prompt pede.
- [ ] Conferir a voz nos idiomas escolhidos: sem modelo instalado o botão some, e
  isso precisa ser visto num aparelho antes de virar regra.

## Próximos incrementos

- [ ] Navegação entre termos relacionados e rede lexical local.
- [ ] Captura via compartilhamento do Android e importação Kindle.
- [ ] Notificações de revisão opt-in.
- [ ] Sincronização opcional sem retirar o modo local-first.
- [ ] OCR e transcrição por idioma: o modelo latino empacotado não lê japonês nem
  árabe, e hoje a captura por foto nesses cursos cai direto na edição manual.
- [ ] Excluir um curso. Dá para entrar em 43 idiomas e não dá para sair de nenhum.

Critério de saída de cada incremento: funcionamento em dados reais, acessibilidade
verificada e `test`, `verifySqlDelightMigration`, `lintDebug` e `assembleDebug`
passando.
