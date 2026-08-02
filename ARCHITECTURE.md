# Arquitetura

Kotlin Multiplatform, hoje só com target Android. O que é caro de duplicar
(modelo, banco, rede) já mora em módulos compartilhados; a UI é específica de
plataforma.

## Módulos

```
:contracts   DTOs da conversa app↔servidor       (KMP: android + jvm)
:shared      domínio, banco, cliente HTTP        (KMP: android)
:androidApp  UI em Jetpack Compose               (Android)
:server      um endpoint Ktor                    (JVM)
```

Cada um existe por um motivo concreto:

- **`:contracts`** é minúsculo — só data classes `@Serializable`. Existe porque
  tem os targets `android` **e** `jvm`, então app e servidor compilam contra o
  *mesmo* tipo. Mudar o formato da ficha quebra o build dos dois lados em vez de
  virar bug em produção.
- **`:shared`** é a peça que vai para o iOS sem alteração.
- **`:androidApp`** usa Jetpack Compose puro, não Compose Multiplatform. A UI é a
  parte mais barata de reescrever; CMP cobraria imposto de configuração hoje por
  um benefício que só aparece com um Mac na mesa.
- **`:server`** guarda a chave da Anthropic para ela não ficar no aparelho. Sem
  banco, sem contas, sem estado.

## Fluxo de uma captura

```
CapturaScreen  ──salvar──>  repositório  ──INSERT status=PENDENTE──>  SQLite
     │                                                                  │
     └── volta pra Home imediatamente                                   │
                                                                        │
AppContainer.escopo ──> gerarFicha() ──> :server ──> Claude API         │
                              │                                         │
                              └────── UPDATE status=PRONTA ─────────────┘
                                                                        │
HomeScreen  <── Flow do SQLDelight reemite sozinho ─────────────────────┘
```

Foto e áudio entram por um caminho mais curto, que **não** toca a IA:

```
CapturaScreen ──foto/áudio──> arquivo em filesDir + INSERT status=RASCUNHO
                                                             │
                                          InboxScreen  <─────┘
                                                │
                                     (quando você tiver calma)
                                                │
                              ProcessarScreen ──transcrever──> status=PENDENTE
                                                                     │
                                                    daí em diante é o fluxo acima
```

`gerarFicha()` sai em silêncio se `trecho` ou `alvo` estiverem nulos. Marcar ERRO
ali seria mentira: não falhou nada, só falta você transcrever.

Dois pontos que sustentam o critério de saída da Fase 1:

1. **Salvar não espera a IA.** A tela fecha na hora; a ficha chega depois. É o
   princípio "captura e revisão são momentos separados".
2. **A geração roda no escopo da aplicação**, não no `viewModelScope` da tela de
   captura. Como a tela é fechada imediatamente, um escopo preso a ela seria
   cancelado e a entrada ficaria travada em `PENDENTE` para sempre.

A home não faz refresh manual nem polling: ela observa um `Flow` do SQLDelight,
que reemite quando a linha muda.

## Modelo de dados

Uma tabela só ([Vocabs.sq](shared/src/commonMain/sqldelight/com/jean/vocabs/shared/db/Vocabs.sq)),
guardando captura e ficha juntas. A ficha nasce sempre de exatamente uma captura,
então separar em duas tabelas só adicionaria um JOIN.

```
entrada(id, trecho, alvo, origem, criado_em,
        status,                          -- RASCUNHO | PENDENTE | GERANDO | PRONTA | ERRO
        formato, midia_caminho,          -- TEXTO | FOTO | AUDIO  +  arquivo local
        tipo, traducao, definicoes_json, exemplo, ipa,
        erro,
        pontos, taxa_decaimento, data_ultima_interacao, revisoes,
        acertos, erros)                  -- placar por palavra, para a taxa de acerto

dia_revisado(dia, revisoes)              -- calendário de revisões, para a sequência
```

`definicoes_json` é TEXT porque são 1-2 strings que nunca serão consultadas por
conteúdo.

`trecho` e `alvo` **aceitam NULL**: uma captura de foto ou áudio nasce sem
nenhum dos dois, e eles só passam a existir quando você transcreve. `RASCUNHO` é
o estado que marca isso. Guardar `''` no lugar de NULL funcionaria, mas criaria a
regra implícita "string vazia significa não transcrito" — exatamente o tipo de
conhecimento que some da cabeça e volta como bug.

O inbox saiu quase de graça, como o plano previa, mas a query final é
`WHERE status != 'PRONTA'` e não `= 'PENDENTE'`: assim ela também recolhe os
rascunhos de mídia e as falhas, que são igualmente "coisas paradas esperando
você". A home usa o complemento, `= 'PRONTA'`.

### Migrações

A Fase 1.5 exigiu remover `NOT NULL` de duas colunas, e o SQLite não sabe alterar
a restrição de uma coluna existente. Por isso
[1.sqm](shared/src/commonMain/sqldelight/com/jean/vocabs/shared/db/1.sqm) recria a
tabela e copia os dados, em vez de um `ALTER TABLE` simples. A ordem das colunas
na tabela recriada é idêntica à do `CREATE TABLE`, senão instalação nova e
instalação migrada divergiriam.

A Fase 2 ([2.sqm](shared/src/commonMain/sqldelight/com/jean/vocabs/shared/db/2.sqm))
só acrescenta colunas, então `ALTER TABLE` basta — adicionar coluna com `NOT NULL`
e `DEFAULT` constante é justamente a alteração que o SQLite faz bem. Como
`ALTER TABLE` anexa no fim, as colunas de retenção também ficam no fim do
`CREATE TABLE`. É por isso que o arquivo não parece organizado por assunto.

O `AndroidSqliteDriver` roda isso sozinho ao ver a versão do schema subir.

## Retenção

Cada ficha carrega **dois** números, não um: quantos pontos de memória você tinha
na última resposta e quantos pontos por dia ela perde. Um número só faria uma
palavra dominada há meses cair no mesmo ritmo de uma nova — exatamente a vantagem
que a repetição espaçada existe para dar. Acerto devolve 100 pontos e divide a
taxa por 1,5; erro zera e multiplica por 3. A palavra entra na fila abaixo de 60.

Toda a matemática mora em
[Retencao.kt](shared/src/commonMain/kotlin/com/jean/vocabs/shared/domain/Retencao.kt),
em funções puras que **recebem `agora`** em vez de ler o relógio. É isso que deixa
o repositório reusar a costura de tempo que ele já tinha injetada, e impede a UI de
consultar um relógio próprio — se a barra da ficha e a fila da home usassem
relógios diferentes, elas discordariam sem sintoma visível.

**A fila é filtrada em Kotlin, não em SQL.** Um `WHERE` com o tempo dentro
pareceria mais direto, mas o `Flow` do SQLDelight só reemite quando a *tabela*
muda, nunca quando o relógio anda: o `agora` ficaria congelado no instante em que
a query foi montada, e a fila pararia de andar sem dar erro. Filtrar em Kotlin
também evita ter a mesma fórmula escrita em duas linguagens.

Isso deixa um caso de fora: o app parado na home no exato instante em que uma
palavra cruza o limiar. Custa um cartão de atraso até o próximo toque, e **não
vale um ticker** — recomporia a home a cada minuto pelo resto da vida do app.
Voltar do background já re-assina o Flow e recalcula.

A sequência de dias usa Julian Day Number local, calculado pelo próprio SQLite:
dias seguidos diferem exatamente em 1, então a sequência é uma subtração, sem
aritmética de calendário no Kotlin (que aqui não tem `kotlinx-datetime`).

`acertos` e `erros` são colunas separadas, e não `erros = revisoes - acertos`. A
subtração seria mais curta e mentiria: as revisões feitas antes de o placar existir
não têm desfecho guardado, e apareceriam todas como erro. Com duas colunas,
`acertos + erros` é o total de que se sabe o resultado, e a taxa sai calculada só
sobre ele — uma palavra com 2 revisões e 1 acerto registrado mostra 100%, não 50%.
Daqui para a frente as três colunas andam juntas; a diferença só existe no passado.

## Mídia

Fotos e áudios ficam em `filesDir/capturas`, dentro do armazenamento privado do
app: não pede permissão nenhuma, não aparece na galeria, e some junto se você
desinstalar — coerente com o "local-first" do produto.

- **Foto** usa o app de câmera do sistema (`ActivityResultContracts.TakePicture`).
  O `CAMERA` **não** é declarado no manifesto de propósito: declarar obrigaria a
  pedir a permissão em runtime, enquanto delegar ao app de câmera não exige nada.
- **Áudio** usa `MediaRecorder` e é a única permissão sensível do app.
- **`FileProvider`** existe porque o app de câmera é outro processo e precisa de
  autorização para escrever no arquivo de destino; passar um `file://` cru dispara
  `FileUriExposedException` desde o Android 7.

A decodificação da foto reduz a imagem via `inSampleSize` antes de virar bitmap —
sem isso, uma foto de vários megapixels custaria dezenas de MB de heap por tela.

## Servidor

`POST /v1/ficha` — recebe `{trecho, alvo}`, devolve a ficha. Autenticado por um
token compartilhado (`APP_TOKEN`) no header.

Usa **structured outputs** na chamada à Claude API: o schema obriga a resposta a
vir no formato exato de `FichaResponse`. Sem isso o modelo às vezes devolve o JSON
embrulhado em markdown, e você acaba escrevendo regex para extrair.

O prompt é a única coisa do projeto escrita em inglês; comentários e identificadores
seguem em português. O idioma da instrução e o da saída são independentes — a
tradução sai em português porque a instrução manda, não porque a instrução esteja
em português — e uma cópia traduzida do prompt por idioma nativo seria manter N
versões de uma prosa calibrada, sendo que o teste de PALAVRA vs EXPRESSAO é a
parte mais sutil da ficha. Os idiomas saem de `ParDeIdiomas`, hoje sempre o mesmo
par: o que varia de verdade é o **alvo** (notação de pronúncia e exemplos do teste),
não o nativo, que só diz em que língua escrever.

Falhas viram `503` com mensagem curta — para o app isso significa "tente de novo",
não "desista". A causa completa fica no log do servidor.

## Armadilhas já resolvidas

- **`usesCleartextTraffic` só em debug.** O servidor local é `http://` e o Android
  bloqueia isso desde a versão 9. Em release não existe.
- **Detectar emulador por "generic" não funciona mais.** A receita que circula por
  aí testa se `Build.BRAND` ou `Build.FINGERPRINT` começam com `generic`; um
  emulador atual reporta brand `google` e fingerprint `google/sdk_gphone64.../...`
  e passaria por aparelho físico. O sinal confiável é `Build.HARDWARE`, que vale
  `ranchu` (emulador atual) ou `goldfish` (antigo) — ver
  [Ambiente.kt](shared/src/androidMain/kotlin/com/jean/vocabs/shared/Ambiente.kt).
  Isso decide entre `10.0.2.2` e o IP da máquina na LAN, e errar não dá erro
  claro: dá timeout, fácil de confundir com servidor fora do ar.
- **O IP da LAN é detectado no build, não digitado.** `isSiteLocalAddress` filtra
  só as faixas privadas, o que descarta sozinho os adaptadores de VPN — que
  entregam endereços fora delas e são a causa clássica de o app apontar para o
  lugar errado. `SERVIDOR_LAN` no `.env` sobrescreve. O `APP_TOKEN` sai da mesma
  fonte que o servidor lê, então não existe mais o par de valores para manter em
  sincronia à mão.
- **Timeout de 90s no cliente HTTP.** O default do Ktor derrubaria a requisição no
  meio da geração.
- **AGP 9 tem Kotlin embutido.** Módulos KMP usam `com.android.kotlin.multiplatform.library`
  (não `com.android.library`), e o `:androidApp` **não** aplica `kotlin.android`.
  Todo tutorial anterior ao AGP 9 erra nisso.
- **`INSERT ... ON CONFLICT DO UPDATE` não existe no minSdk.** O UPSERT chegou no
  SQLite 3.24, que só veio com o Android 10 (API 29); o `minSdk` aqui é 26, com o
  3.19, e o driver usa o SQLite *do sistema*, não um empacotado. Um UPSERT
  compilaria, passaria no emulador moderno e quebraria num Android 8. O calendário
  de revisões usa `INSERT OR IGNORE` + `UPDATE` numa transação.
- **Parâmetro dentro de função SQL vira String.** O SQLDelight não consegue inferir
  o tipo de um `?` usado dentro de `julianday(...)` e gera `String`. Uma operação
  aritmética (`:instante + 0`) força o contexto numérico e ele sai como `Long`.

## Quando o iOS entrar

`:contracts` e `:shared` ganham o target `iosArm64` e uma implementação de
`DatabaseDriverFactory` que devolve `NativeSqliteDriver` — é a única costura de
plataforma do projeto, e ela é uma interface justamente para isso. A UI você
decide entre SwiftUI ou migrar para Compose Multiplatform; nenhuma das duas exige
mexer no `:shared`.
