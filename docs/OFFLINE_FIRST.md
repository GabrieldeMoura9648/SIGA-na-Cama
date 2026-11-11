# Cache Offline-First e fallback do alarme

## Ciclo do cache de áudio

Quando o aluno seleciona um som, `ExternalSoundViewModel` chama `ExternalSoundRepository.downloadAndCacheSound()`. O repositório baixa o MP3 com OkHttp para `filesDir/custom_alarms`, no armazenamento interno privado do app. Antes do download, remove os arquivos existentes nesse diretório para limitar o espaço ocupado. Depois, o caminho e o nome do novo áudio são gravados no Preferences DataStore por `UserPreferencesRepository`.

O alarme toca esse arquivo local; portanto, depois de baixado, o som personalizado pode ser reproduzido sem uma conexão de rede ativa. A busca e o download inicial do som continuam dependendo de conectividade.

## Fail-Safe no toque do alarme

`AlarmRingingActivity.startAlarmSound()` tenta obter o caminho salvo, confirmar que o arquivo existe e tem tamanho maior que zero e preparar o `MediaPlayer` local em loop. Se o caminho não estiver configurado ou o arquivo estiver ausente/vazio, o fluxo segue para o fallback. Se ocorrer uma exceção ao acessar ou preparar o áudio — por exemplo, erro de I/O ou arquivo inválido — o `catch` registra o erro e também segue para o fallback.

O fallback usa `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)` e reproduz o toque nativo do Android. Assim, problemas no áudio personalizado não impedem intencionalmente o alarme de tentar tocar. Se o dispositivo não fornecer um URI padrão ou não for possível iniciar o ringtone, a atividade registra o caso no log.

## Agendamento

O `AlarmScheduler` usa `AlarmManager` para os disparos diários e a soneca. Quando alarmes exatos não são permitidos pelo sistema, usa `setAndAllowWhileIdle` como alternativa menos precisa. As regras do modo automático e manual estão descritas em [`CODE_ARCHITECTURE.md`](CODE_ARCHITECTURE.md#despertador-inteligente-automático-e-manual).
