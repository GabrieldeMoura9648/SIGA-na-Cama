# Conectividade, parser e segurança

## Dados acadêmicos da Fatec

`FatecApiService.kt` usa Retrofit e Gson para baixar o catálogo acadêmico configurado no app. O `AcademicRepository` sincroniza os dados com o Room e mantém seus próprios mecanismos de fallback acadêmico.

## Feriados

`HolidayApiService.kt` consulta de forma assíncrona `GET https://brasilapi.com.br/api/feriados/v1/{year}`. O `HolidayRepository` mantém em memória os feriados nacionais recebidos por ano e também reconhece 9 de julho (estadual de São Paulo) e 8 de dezembro (municipal de Diadema). O agendamento automático ignora datas de aula que sejam feriados; se a consulta remota falhar, desativa o alarme automático por segurança e registra a decisão no log.

## Sons do MyInstants Brasil

Os sons externos são consultados diretamente no site oficial, sem proxy ou serviço intermediário:

* Em alta no Brasil: `GET https://www.myinstants.com/pt/index/br/`
* Busca: `GET https://www.myinstants.com/pt/search/?name=<consulta codificada>`

`ExternalSoundRepository.kt` executa essas requisições com o `OkHttpClient` configurado em `MyInstantsApiService.kt`, incluindo `User-Agent: Mozilla/5.0` e timeouts de 8 segundos. Esta integração HTML usa OkHttp diretamente; Retrofit permanece na integração com a API de dados acadêmicos e não intermedeia as chamadas ao MyInstants.

O repositório analisa o HTML recebido em tempo de execução por regex, extraindo identificador, título e caminho do MP3 dos botões do site. Também aceita o formato alternativo de `share(...)`. Os bytes da resposta são convertidos explicitamente com UTF-8, e o parser decodifica entidades HTML básicas e escapes Unicode antes de apresentar os títulos.

## Safe Search acadêmica

O modal inicia com a busca temática “bom dia” para evitar que o ranking geral seja a primeira lista apresentada. A lista `blockedKeywords` no repositório filtra localmente, sem distinção entre maiúsculas e minúsculas, qualquer resultado cujo título, ID ou URL contenha termos bloqueados. O filtro também é aplicado aos resultados de “Em Alta BR”; ele reduz a exposição acidental de conteúdo impróprio/NSFW, mas não substitui moderação editorial do catálogo remoto.

## Cache de áudio

Ao selecionar um resultado, o MP3 é baixado para o armazenamento interno privado do app e o caminho/nome são persistidos no Preferences DataStore. O funcionamento em reprodução e o fallback do alarme estão descritos em [`OFFLINE_FIRST.md`](OFFLINE_FIRST.md).
