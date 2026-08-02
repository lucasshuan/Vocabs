# Roadmap Tagarara

## Base implementada

- [x] Marca pública Tagarara, tema claro/escuro, logo, fontes, splash e launcher.
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

## Próximos incrementos

- [ ] Navegação entre termos relacionados e rede lexical local.
- [ ] Captura via compartilhamento do Android e importação Kindle.
- [ ] Notificações de revisão opt-in.
- [ ] Sincronização opcional sem retirar o modo local-first.
- [ ] Mais pares de idiomas somente quando contrato, banco, voz e pronúncia forem
  parametrizados juntos.

Critério de saída de cada incremento: funcionamento em dados reais, acessibilidade
verificada e `test`, `verifySqlDelightMigration`, `lintDebug` e `assembleDebug`
passando.
