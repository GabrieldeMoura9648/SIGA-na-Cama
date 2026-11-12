# 🧠 Arquitetura Descomplicada — SIGA na Cama

Este guia foi feito para explicar o código para o professor sem enrolação. Embora o app tenha recursos avançados (como Despertador Real e consulta diária de faltas), tudo gira em torno dos **5 requisitos obrigatórios**.

---

## 🎯 Os 5 Requisitos Obrigatórios (Onde mostrar no código)

### 1. Navegação entre Telas (Jetpack Navigation Compose)
* **A ideia simples:** É o "GPS" do aplicativo. Ele decide se o aluno vai para a tela de Boas-Vindas (Onboarding) na primeira vez que abre o app ou direto para a Tela Principal (Meu Dia), além de controlar a barra inferior (Meu Dia, Eventos e Configurações).
* **Onde abrir para mostrar ao professor:**
  * Acesse na pasta `app/src/main/java/com/example/fatecskip/ui/navigation/AppNavHost.kt`
  * Acesse na pasta `app/src/main/java/com/example/fatecskip/ui/navigation/Screen.kt`
* **Pergunta de banca:** *"Como vocês passam de uma tela para outra?"*
  * **Resposta:** Usamos o `NavHost` com rotas tipadas na `Screen.kt` e o `navController.navigate()` para trocar de tela sem recriar a `MainActivity`.

---

### 2. DataStore (Armazenamento de Preferências)
* **A ideia simples:** É o "Post-it" da memória do app. Guarda configurações leves e rápidas do aluno (chave-valor): apelido, semestre atual (1º ao 6º), conclusão do Onboarding, preferências de notificação e horários do despertador e do preview noturno.
* **Onde abrir para mostrar ao professor:**
  * Acesse na pasta `app/src/main/java/com/example/fatecskip/data/preferences/UserPreferencesRepository.kt`
* **Pergunta de banca:** *"Por que usar DataStore aqui e não o SQLite?"*
  * **Resposta:** Porque são configurações simples de usuário (booleanos, inteiros e strings curtas). O Preferences DataStore lê isso de forma assíncrona usando Kotlin `Flow`, sem travar a tela.

---

### 3. SQLite / Room Database (Persistência de Dados)
* **A ideia simples:** É o "Caderno Oficial" onde ficam as tabelas pesadas e estruturadas: as matérias do semestre (`subjects`), os eventos/provas (`academic_events`) e as desculpas (`excuses`).
* **Horários por dia:** O diálogo `EditSubjectDialog` permite selecionar os dias da disciplina e informar um intervalo para cada dia. A grade grava `classTime` como blocos identificados pelo dia, por exemplo `Seg 07:40 - 11:20 | Ter 09:40 - 13:00`; o cadastro manual de matéria extra também permite essa configuração.
* **Bloqueio de conflito de horário:** Ao incluir uma matéria de outro semestre, o repositório verifica os dias em comum e compara os intervalos daquele dia em minutos. Se houver sobreposição, aplica a política de substituição da grade.
* **O toque de mestre (diferencial):** A coluna `isCustomized` marca edições do usuário. A sincronização da API preserva o nome, os dias, os horários, o limite de faltas e a quantidade diária de aulas da disciplina personalizada, além dos demais dados armazenados.
* **Migrações Room:** `AppDatabase` registra as migrações 1→2 e 2→3 para adicionar colunas sem descartar a grade e os dados salvos; o fallback destrutivo fica restrito a downgrade.
* **Onde abrir para mostrar ao professor:**
  * Banco: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/AppDatabase.kt`
  * Comandos SQL: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/SubjectDao.kt`
  * Tabelas: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/entities/SubjectEntity.kt`

---

### 4. APIs REST (Consumo de Serviços Externos com Retrofit)
* **A ideia simples:** O app busca na nuvem (GitHub Raw JSON) a grade acadêmica, eventos do calendário institucional e lista de desculpas. Uma integração separada consulta os feriados nacionais na BrasilAPI.
* **Sincronização inicial + Smart Merge:** Ao tocar em **Começar** no fim do onboarding, o app salva apelido e semestre e sincroniza a grade escolhida antes de marcar o onboarding como concluído e navegar para a Home. A `HomeViewModel` também mantém um *auto-sync* em background ao ser criada. A sincronização funde novidades (como troca de professores ou horários divididos na semana) para o SQLite, respeitando a flag `isCustomized`.
* **O toque de mestre (Offline-First):** E se a internet da Fatec cair na hora da apresentação? O `AcademicRepository` tenta buscar na API via `Retrofit`; se falhar, ele lê instantaneamente a grade oficial salva em memória via `DefaultFatecData`. O app nunca quebra!
* **Onde abrir para mostrar ao professor:**
  * Interface acadêmica: acesse `app/src/main/java/com/example/fatecskip/data/remote/FatecApiService.kt`
  * Consulta de feriados: acesse `app/src/main/java/com/example/fatecskip/data/remote/HolidayApiService.kt` e `app/src/main/java/com/example/fatecskip/data/repository/HolidayRepository.kt`
  * Sincronização acadêmica: acesse `app/src/main/java/com/example/fatecskip/data/repository/AcademicRepository.kt`
  * JSON da grade e calendário institucional: `fatec_api.json` (o asset local não é o fallback automático em runtime)

---

### 5. Notificações Locais (Agendamento e Exibição)
* **A ideia simples:** Fizemos em dose dupla para garantir nota máxima!
  1. **Notificação na Barra de Status (`WorkManager`):** O `DailyCheckWorker` avalia o dia seguinte. No preview noturno, cria uma notificação com o título **“Agenda de Amanhã — SIGA na Cama”** e um corpo com as disciplinas e horários, o resumo de frequência e o estado do despertador.
  2. **Despertador de Verdade (`AlarmManager`):** Acende a tela bloqueada do celular na hora exata com o resumo da grade e o botão **"🛌 Vou Faltar Hoje (+Falta)"**, que já soma a falta direto no SQLite e deixa você voltar a dormir.
* **Proteção do onboarding:** `DailyCheckWorker` retorna sucesso sem avaliar o dia ou notificar enquanto o onboarding não estiver concluído ou as notificações estiverem desativadas. O trabalho periódico recebe atraso inicial de 6 horas. A sincronização inicial do semestre acontece no botão final do onboarding, antes de marcá-lo como concluído.
* **Preview noturno configurável:** `UserPreferencesRepository` persiste hora e minuto separados, com padrão 21:00. `SettingsScreen` usa um `TimePickerDialog`; ao salvar, `SettingsViewModel` atualiza a preferência e reagenda o preview se ele estiver ativo. `AlarmScheduler.scheduleNightPreview()` só agenda após a conclusão do onboarding e sempre escolhe um horário futuro: se o horário de hoje já passou ou é o mesmo minuto, agenda para o dia seguinte. Ao disparar, `AlarmReceiver` agenda o próximo preview e encaminha a avaliação ao `DailyCheckWorker`, que aplica as mesmas verificações de onboarding e notificações.
* **Restauração após boot:** `BootReceiver` escuta `ACTION_BOOT_COMPLETED`, lê as preferências do DataStore e restaura os agendamentos habilitados quando o aparelho reinicia. O preview noturno só é restaurado após a conclusão do onboarding.
* **Permissões do sistema:** no Android 13+, o app solicita `POST_NOTIFICATIONS` no onboarding e em Configurações. A conclusão do onboarding continua mesmo se a permissão for negada; sem autorização do sistema, as notificações não podem ser exibidas.
* **Onde abrir para mostrar ao professor:**
  * Criação da Notificação: acesse na pasta `app/src/main/java/com/example/fatecskip/worker/NotificationHelper.kt`
  * Trabalho em 2º plano: acesse na pasta `app/src/main/java/com/example/fatecskip/worker/DailyCheckWorker.kt`
  * Despertador, preview e restauração após reinício: acesse `app/src/main/java/com/example/fatecskip/worker/AlarmScheduler.kt`, `app/src/main/java/com/example/fatecskip/worker/AlarmReceiver.kt` e `app/src/main/java/com/example/fatecskip/worker/BootReceiver.kt`
  * Preferências e seletor de horário: acesse `app/src/main/java/com/example/fatecskip/data/preferences/UserPreferencesRepository.kt` e `app/src/main/java/com/example/fatecskip/ui/screens/settings/SettingsScreen.kt`

---

## ⏰ Sons externos e agendamento inteligente

### Fluxo MVVM dos sons do MyInstants

O seletor de som personalizado é dividido entre interface, estado e acesso a dados:

* **View:** `SettingsScreen.kt` abre o modal isolado `ExternalSoundDialog.kt`. A busca inicial é temática, e os chips rápidos são `☀️ Bom Dia`, `⏰ Acorda`, `🐓 Galo`, `😂 Risada`, `🚨 Sirene` e `🔥 Em Alta BR`.
* **ViewModel:** `ExternalSoundViewModel.kt` expõe sons, carregamento, mensagens e som selecionado por `StateFlow`. Inicia a busca por “bom dia”, controla a prévia por streaming com `MediaPlayer` e solicita a persistência do som escolhido.
* **Repository / dados:** `ExternalSoundRepository.kt` busca e analisa os resultados do site, filtra termos impróprios e baixa o MP3 selecionado para o cache privado local. `MyInstantsApiService.kt` configura o `OkHttpClient` usado nessa integração.
* **Persistência:** `UserPreferencesRepository.kt` usa Preferences DataStore para guardar o caminho e o nome do som selecionado. O arquivo de áudio fica no armazenamento interno do app, separadamente das preferências.

Detalhes das URLs, parser e blacklist estão em [`API.md`](API.md); o ciclo de cache e o fallback do player estão em [`OFFLINE_FIRST.md`](OFFLINE_FIRST.md).

### Despertador Inteligente: automático e manual

`AlarmScheduler.scheduleDailyAlarm()` consulta o modo e as preferências no DataStore e agenda o próximo alarme com `AlarmManager`:

* **Manual (Fixo):** usa a hora e o minuto configurados pelo aluno.
* **Automático:** obtém o semestre selecionado e consulta as disciplinas por meio do `AcademicRepository`. Procura o próximo horário em um dia futuro com aula, escolhe o início mais cedo e subtrai o `alarmAutoOffsetMinutes` configurado. O padrão do offset é 60 minutos.
* **Horários por dia:** `AlarmScheduler` lê o bloco correspondente ao dia da semana, como `Seg` ou `Ter`, e usa o início desse intervalo. Um horário sem dia indicado continua sendo usado como horário geral.
* **Feriados:** antes de agendar, consulta `HolidayRepository`, que combina feriados nacionais da BrasilAPI com 9 de julho (São Paulo) e 8 de dezembro (Diadema). Datas reconhecidas como feriado são puladas. Os feriados e folgas próprios do calendário Fatec, como Carnaval e Dia do Professor, permanecem no calendário acadêmico sincronizado; eles são usados na agenda e nas notificações do `DailyCheckWorker`.
* **Falha na consulta de feriados:** se a consulta externa falhar, `AcademicRepository.isAcademicHoliday()` registra a falha e segue sem marcar a data como feriado; isso não aciona o fallback manual. O fallback do horário manual é usado quando uma exceção interrompe o cálculo automático do alarme.
* **Fallback do cálculo automático:** se ocorrer uma exceção durante o cálculo, o alarme usa temporariamente a hora manual configurada. A preferência `KEY_ALARM_FALLBACK_WARNING` registra essa condição e Configurações exibe um aviso; o aviso é limpo quando um cálculo automático é concluído.
* **Sem aula futura:** após procurar até 366 dias, se nenhuma data letiva puder ser agendada, o alarme diário automático é cancelado.
* **Precisão:** usa `setExactAndAllowWhileIdle` quando permitido pelo Android e recorre a `setAndAllowWhileIdle` quando a permissão de alarme exato não está disponível.

---

## 🗂️ Resumo Rápido das Pastas (Caso peçam um tour pelo código)
* `data/local`: Tudo de SQLite/Room (`AppDatabase`, `DAOs` e `Entities`).
* `data/preferences`: Tudo de DataStore (`UserPreferencesRepository`).
* `data/remote`: APIs REST acadêmica (`FatecApiService`) e de feriados (`HolidayApiService`), seus DTOs e o cliente HTTP do MyInstants (`MyInstantsApiService`).
* `data/repository`: Repositórios acadêmicos, de feriados e de sons externos, que conectam fontes remotas, persistência local e regras de domínio.
* `domain/model`: Onde vive o veredito da consulta diária (`SkipVerdict.kt`).
* `ui/screens`: Todas as telas feitas em Jetpack Compose (`home`, `events`, `onboarding`, `settings`, `alarm`).
* `worker`: Tudo de Notificações e Alarme em segundo plano.
