# Conectividade, parser e segurança

## Dados acadêmicos da Fatec

`FatecApiService.kt` usa Retrofit e Gson para baixar o catálogo acadêmico configurado no app. O `AcademicRepository` sincroniza disciplinas, eventos, feriados do calendário acadêmico e desculpas com o Room. Se a sincronização falhar, os dados padrão de `DefaultFatecData` são usados como fallback; o app não carrega `app/src/main/assets/fatec_api.json` como fallback automático.

## Feriados

`HolidayApiService.kt` consulta de forma assíncrona `GET https://brasilapi.com.br/api/feriados/v1/{year}` e converte a resposta em `HolidayDto` (`date`, `name` e `type`). O `HolidayRepository.isHoliday(LocalDate)` combina essa resposta com os feriados fixos de 9 de julho (estadual de São Paulo) e 8 de dezembro (municipal de Diadema); os feriados nacionais são armazenados em memória por ano. As datas anuais de calendário acadêmico ou institucionais — por exemplo, Carnaval, Corpus Christi e folgas definidas pela Fatec — continuam na fonte acadêmica e são sincronizadas separadamente no Room. Por isso, retirar duplicatas nacionais do JSON não remove as datas específicas da instituição.

O modo automático do `AlarmScheduler` percorre datas futuras em que há aula e pula as que `HolidayRepository` identificar como feriado. Se a API falhar durante essa validação, o alarme diário automático é cancelado para evitar um disparo em data não validada; o erro e a decisão são registrados no log. O modo manual não consulta a API de feriados.

## Sons do MyInstants Brasil

Os sons externos são consultados diretamente no site oficial, sem proxy ou serviço intermediário:

* Em alta no Brasil: `GET https://www.myinstants.com/pt/index/br/`
* Busca: `GET https://www.myinstants.com/pt/search/?name=<consulta codificada>`

`ExternalSoundRepository.kt` executa essas requisições com o `OkHttpClient` configurado em `MyInstantsApiService.kt`, incluindo `User-Agent: Mozilla/5.0` e timeouts de 8 segundos. Esta integração HTML usa OkHttp diretamente; Retrofit é usado nas APIs acadêmica e de feriados, sem intermediar as chamadas ao MyInstants.

O repositório analisa o HTML recebido em tempo de execução por regex, extraindo identificador, título e caminho do MP3 dos botões do site. Também aceita o formato alternativo de `share(...)`. Os bytes da resposta são convertidos explicitamente com UTF-8, e o parser decodifica entidades HTML básicas e escapes Unicode antes de apresentar os títulos.

## Safe Search acadêmica

O modal inicia com a busca temática “bom dia” para evitar que o ranking geral seja a primeira lista apresentada. A lista `blockedKeywords` no repositório filtra localmente, sem distinção entre maiúsculas e minúsculas, qualquer resultado cujo título, ID ou URL contenha termos bloqueados. O filtro também é aplicado aos resultados de “Em Alta BR”; ele reduz a exposição acidental de conteúdo impróprio/NSFW, mas não substitui moderação editorial do catálogo remoto.

## Cache de áudio

Ao selecionar um resultado, o MP3 é baixado para o armazenamento interno privado do app e o caminho/nome são persistidos no Preferences DataStore. O funcionamento em reprodução e o fallback do alarme estão descritos em [`OFFLINE_FIRST.md`](OFFLINE_FIRST.md).
