# Vocabs

App de vocabulário que captura palavras e expressões em inglês no momento em que
elas aparecem — jogando, lendo, assistindo — e transforma cada captura numa ficha
gerada por IA.

Os dados ficam no aparelho. O servidor só intermedia a chamada de IA.

**Estado:** Fases 1, 1.5 e 2 implementadas. A captura acontece por texto, foto ou
áudio (foto e áudio ficam no inbox até você transcrever), a ficha leva ~6s de ponta
a ponta, e cada palavra tem força de memória que decai sozinha e volta pra fila de
revisão. Falta o uso real: os critérios de saída que dependem de você — capturar em
segundos nos contextos reais e revisar 7 dias seguidos — ainda não foram cumpridos.

## Rodar

Pré-requisito: Android Studio (traz o JDK e o Android SDK). `JAVA_HOME` e
`ANDROID_HOME` já estão definidas nesta máquina, no nível do usuário — se algum dia
o `gradlew` reclamar de `JAVA_HOME is not set`, é isto que faltou:

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', "C:\Program Files\Android\Android Studio\jbr", 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
# abra um terminal novo depois disso
```

Os segredos ficam no `.env` na raiz (ignorado pelo git, modelo em `.env.example`).
Cole sua chave da Anthropic na linha `ANTHROPIC_API_KEY=` e pronto — não precisa
redigitar nada a cada terminal novo:

```
ANTHROPIC_API_KEY=sk-ant-...
APP_TOKEN=token-de-teste-local
```

Variáveis de ambiente, se definidas, têm precedência sobre o arquivo — é assim que
CI e produção sobrescrevem sem depender dele.

```powershell
.\gradlew.bat :server:run               # backend em localhost:8080
.\gradlew.bat :androidApp:installDebug
```

Se os acentos saírem tortos no console (`Vari├ível`), o terminal está em code page
legada: `chcp 65001` resolve na sessão.

**Não precisa configurar endereço.** O app descobre onde o servidor está: num
emulador usa `10.0.2.2` (que é como ele enxerga o localhost da sua máquina) e num
celular físico usa o IP desta máquina na rede local, detectado na hora de compilar.
O token sai do mesmo `.env` que o servidor lê, então os dois lados sempre batem.

Se a detecção errar — várias placas de rede, servidor noutra máquina — preencha
`SERVIDOR_LAN` no `.env` e recompile. Para celular físico, os dois aparelhos
precisam estar no mesmo Wi-Fi, e pode ser necessário liberar a porta 8080 no
firewall do Windows.

## Documentação

| Arquivo | O que tem |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Módulos, fluxo de uma captura, decisões técnicas |
| [docs/PRODUTO.md](docs/PRODUTO.md) | Visão, princípios, core loop, retenção, monetização |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Fases, escopo e critérios de saída |
| [docs/EXERCICIOS-E-METRICAS.md](docs/EXERCICIOS-E-METRICAS.md) | Catálogo de minigames e painel de métricas |
| [docs/NOTAS.md](docs/NOTAS.md) | O que falta provar, o que está torto, ideias soltas |
