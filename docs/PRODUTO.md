# Produto: Tagarara

Tagarara transforma termos encontrados em jogos, livros, séries e conversas em
fichas ligadas ao contexto pessoal. Não há currículo fixo: o conteúdo nasce da
vida da pessoa.

## Princípios

1. Captura antes de tudo; salvar fecha a tela imediatamente.
2. Captura e revisão são momentos separados.
3. Contexto pessoal vale mais que definição isolada.
4. Dados e automações locais são o padrão; custo de IA é transparente.
5. Uma falha nunca apaga trabalho que já deu certo.
6. Exportação permanece disponível sem conta.

## Modelo mental

Uma **captura** é o trecho bruto, textual, fotográfico ou falado. Ela pode originar
várias **entradas**: `fence` e `on the fence`, por exemplo, podem ser escolhidos
no mesmo contexto e seus intervalos podem se sobrepor.

O tipo é objetivo e local:

- um token selecionado → `PALAVRA`;
- dois ou mais tokens contíguos → `EXPRESSAO`.

Pontuação externa não participa do alvo; apóstrofos e hífens internos participam.
Duplicatas são avisadas, nunca bloqueadas.

## Loop principal

```text
Capturar → selecionar alvos → gerar fichas → revisar por cloze → dominar
```

Foto usa OCR offline. Áudio usa reconhecimento local quando a plataforma oferece
entrada de arquivo e sempre mantém digitação manual como saída segura.

## Ficha e revisão

A ficha contém termo, pronúncia, tipo, tradução contextual, força de memória,
trecho pessoal, definições, exemplo e 3–6 termos relacionados. TTS do aparelho
faz a reprodução.

A revisão apaga exatamente o intervalo selecionado e pede a resposta digitada.
A comparação ignora caixa e espaços repetidos, mas preserva letras, acentos e
pontuação significativa. Erro e “Não lembro” revelam a resposta, contam uma vez
e recolocam o cartão uma vez.

## Transparência

O perfil mostra vocabulário, dominadas, taxa de acerto, 84 dias de atividade,
par Português→Inglês e gerações de IA do mês. `100` é somente a referência visual:
não é quota de segurança e não bloqueia geração. O ZIP exportado inclui JSON
versionado e mídias para permitir portabilidade real.
