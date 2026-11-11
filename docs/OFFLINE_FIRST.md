# Cache Offline-First e fallback do alarme

## Ciclo do cache de áudio

Quando o aluno seleciona um som, `ExternalSoundViewModel` chama `ExternalSoundRepository.downloadAndCacheSound()`. O repositório baixa o MP3 com OkHttp para `filesDir/custom_alarms`, no armazenamento interno privado do app. Antes do download, remove os arquivos existentes nesse diretório para limitar o espaço ocupado. Depois, o caminho e o nome do novo áudio são gravados no Preferences DataStore por `UserPreferencesRepository`.

O alarme toca esse arquivo local; portanto, depois de baixado, o som personalizado pode ser reproduzido sem uma conexão de rede ativa. A busca e o download inicial do som continuam dependendo de conectividade.

## Consulta de feriados e disponibilidade offline

`HolidayRepository` consulta a BrasilAPI de forma assíncrona e mantém a lista nacional em memória por ano durante a execução atual. Os feriados fixos de 9 de julho (São Paulo) e 8 de dezembro (Diadema) são reconhecidos localmente, sem rede. Os feriados específicos da Fatec — como Carnaval, Corpus Christi e folgas institucionais — continuam no calendário acadêmico configurado em `fatec_api.json` e são sincronizados separadamente para a agenda e as notificações.

O agendamento automático precisa validar o próximo dia letivo pela BrasilAPI. Se a consulta falhar e não houver dados nacionais disponíveis em cache na execução, o scheduler cancela o alarme diário automático por segurança e registra a falha no log; ele não assume que o dia seja letivo. O modo manual do despertador continua funcionando sem a consulta de feriados. A lista recebida da BrasilAPI não é persistida em disco, então a consulta pode ser necessária novamente após o processo do app ser encerrado.

## Fail-Safe no toque do alarme

`AlarmRingingActivity.startAlarmSound()` tenta obter o caminho salvo, confirmar que o arquivo existe e tem tamanho maior que zero e preparar o `MediaPlayer` local em loop. Se o caminho não estiver configurado ou o arquivo estiver ausente/vazio, o fluxo segue para o fallback. Se ocorrer uma exceção ao acessar ou preparar o áudio — por exemplo, erro de I/O ou arquivo inválido — o `catch` registra o erro e também segue para o fallback. Erros assíncronos do player também liberam o recurso e acionam o mesmo fallback.

O fallback usa `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)` e reproduz o toque nativo do Android. Assim, problemas no áudio personalizado não impedem intencionalmente o alarme de tentar tocar. Se o dispositivo não fornecer um URI padrão ou não for possível iniciar o ringtone, a atividade registra o caso no log.

## Agendamento

O `AlarmScheduler` usa `AlarmManager` para os disparos diários, a soneca e o preview noturno. O horário do preview é salvo no Preferences DataStore como hora e minuto, com padrão 21:00, e pode ser alterado em Configurações. Ao mudar o horário com o preview ativo, o próximo disparo é reagendado; após cada disparo normal, o `AlarmReceiver` agenda o seguinte. Quando alarmes exatos não são permitidos pelo sistema, usa `setAndAllowWhileIdle` como alternativa menos precisa. As regras do modo automático e manual estão descritas em [`CODE_ARCHITECTURE.md`](CODE_ARCHITECTURE.md#despertador-inteligente-automático-e-manual).
