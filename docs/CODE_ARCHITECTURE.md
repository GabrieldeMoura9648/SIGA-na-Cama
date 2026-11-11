# 🧠 Arquitetura Descomplicada — SIGA na Cama

Este guia foi feito para explicar o código para o professor sem enrolação. Embora o app tenha recursos avançados (como Despertador Real e Oráculo da Falta), tudo gira em torno dos **5 requisitos obrigatórios**.

---

## 🎯 Os 5 Requisitos Obrigatórios (Onde mostrar no código)

### 1. Navegação entre Telas (Jetpack Navigation Compose)
* **A ideia simples:** É o "GPS" do aplicativo. Ele decide se o aluno vai para a tela de Boas-Vindas (Onboarding) na primeira vez que abre o app ou direto para a Tela Principal (Home), além de controlar a barra inferior (Home, Eventos e Configurações).
* **Onde abrir para mostrar ao professor:**
  * Acesse na pasta `app/src/main/java/com/example/fatecskip/ui/navigation/AppNavHost.kt`
  * Acesse na pasta `app/src/main/java/com/example/fatecskip/ui/navigation/Screen.kt`
* **Pergunta de banca:** *"Como vocês passam de uma tela para outra?"*
  * **Resposta:** Usamos o `NavHost` com rotas tipadas na `Screen.kt` e o `navController.navigate()` para trocar de tela sem recriar a `MainActivity`.

---

### 2. DataStore (Armazenamento de Preferências)
* **A ideia simples:** É o "Post-it" da memória do app. Guarda configurações leves e rápidas do aluno (chave-valor): apelido, semestre atual (1º ao 6º), estilo de aluno (ex: "Focado", "Calculista"), se já fez o Onboarding e o horário do despertador.
* **Onde abrir para mostrar ao professor:**
  * Acesse na pasta `app/src/main/java/com/example/fatecskip/data/preferences/UserPreferencesRepository.kt`
* **Pergunta de banca:** *"Por que usar DataStore aqui e não o SQLite?"*
  * **Resposta:** Porque são configurações simples de usuário (booleanos, inteiros e strings curtas). O Preferences DataStore lê isso de forma assíncrona usando Kotlin `Flow`, sem travar a tela.

---

### 3. SQLite / Room Database (Persistência de Dados)
* **A ideia simples:** É o "Caderno Oficial" onde ficam as tabelas pesadas e estruturadas: as matérias do semestre (`subjects`), os eventos/provas (`academic_events`) e as desculpas (`excuses`).
* **Bloqueio de Conflito de Horário (Art. 26 - Acomodação de Horários):** Se o aluno puxar uma matéria atrasada (DP) ou adiantada de outro semestre, o banco verifica a intersecção de `daysOfWeek` e `classTime`. Havendo conflito, o app aplica a política de substituição (uma matéria sai para a outra entrar), garantindo que não existam aulas duplicadas no mesmo horário!
* **O toque de mestre (diferencial):** Criamos a coluna `isCustomized`. Se o aluno editar o nome do professor ou adicionar faltas, quando a API sincronizar de novo, o banco protege a edição do aluno e não sobrescreve!
* **Onde abrir para mostrar ao professor:**
  * Banco: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/AppDatabase.kt`
  * Comandos SQL: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/SubjectDao.kt`
  * Tabelas: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/entities/SubjectEntity.kt`

---

### 4. API REST (Consumo de Serviço Externo com Retrofit)
* **A ideia simples:** O app busca na nuvem (GitHub Raw JSON) a grade oficial de DSM da Fatec, feriados e lista de desculpas.
* **Auto-Sync + Smart Merge:** Ao abrir o app (dentro do `HomeViewModel.init`), o Retrofit bate na nuvem em *background*. Ele funde (merge) as novidades (como troca de professores ou horários divididos na semana) para o SQLite, mas respeita a flag `isCustomized` limpando apenas matérias que foram mescladas ou deixaram de existir no catálogo da Fatec. Tudo automático sem o usuário precisar clicar num botão de atualizar.
* **O toque de mestre (Offline-First):** E se a internet da Fatec cair na hora da apresentação? O `AcademicRepository` tenta buscar na API via `Retrofit`; se falhar, ele lê instantaneamente a grade oficial salva em memória via `DefaultFatecData`. O app nunca quebra!
* **Onde abrir para mostrar ao professor:**
  * Interface HTTP: acesse na pasta `app/src/main/java/com/example/fatecskip/data/remote/FatecApiService.kt`
  * Lógica de Sincronização: acesse na pasta `app/src/main/java/com/example/fatecskip/data/repository/AcademicRepository.kt`
  * JSON local de segurança: acesse na pasta `app/src/main/assets/fatec_api.json`

---

### 5. Notificações Locais (Agendamento e Exibição)
* **A ideia simples:** Fizemos em dose dupla para garantir nota máxima!
  1. **Notificação na Barra de Status (`WorkManager`):** Todo dia (ou no preview noturno) o app checa no banco se você pode faltar amanhã e manda uma notificação com ícone personalizado de caminha (`ic_notification_bed.xml`).
  2. **Despertador de Verdade (`AlarmManager`):** Acende a tela bloqueada do celular na hora exata com a frase do Oráculo e o botão **"🛌 Vou Faltar Hoje (+Falta)"**, que já soma a falta direto no SQLite e deixa você voltar a dormir.
* **Onde abrir para mostrar ao professor:**
  * Criação da Notificação: acesse na pasta `app/src/main/java/com/example/fatecskip/worker/NotificationHelper.kt`
  * Trabalho em 2º plano: acesse na pasta `app/src/main/java/com/example/fatecskip/worker/DailyCheckWorker.kt`
  * Despertador Exato: acesse na pasta `app/src/main/java/com/example/fatecskip/worker/AlarmScheduler.kt` e `app/src/main/java/com/example/fatecskip/ui/screens/alarm/AlarmRingingActivity.kt`

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
* **Automático:** obtém o semestre selecionado e consulta as disciplinas por meio do `AcademicRepository`. Procura o próximo horário de aula hoje ou amanhã, seleciona o início mais cedo e subtrai o `alarmAutoOffsetMinutes` configurado. O padrão do offset é 60 minutos.
* **Sem aula futura hoje ou amanhã:** o comportamento atual mantém o horário fixo configurado como fallback; o scheduler não avança repetidamente até encontrar um próximo dia com aula.
* **Precisão:** usa `setExactAndAllowWhileIdle` quando permitido pelo Android e recorre a `setAndAllowWhileIdle` quando a permissão de alarme exato não está disponível.

---

## 🗂️ Resumo Rápido das Pastas (Caso peçam um tour pelo código)
* `data/local`: Tudo de SQLite/Room (`AppDatabase`, `DAOs` e `Entities`).
* `data/preferences`: Tudo de DataStore (`UserPreferencesRepository`).
* `data/remote`: A API REST acadêmica (`FatecApiService` e DTOs) e o cliente HTTP do MyInstants (`MyInstantsApiService`).
* `data/repository`: Repositórios acadêmicos e de sons externos, que conectam fontes remotas, persistência local e regras de domínio.
* `domain/model`: Onde vive o veredito do Oráculo (`SkipVerdict.kt`).
* `ui/screens`: Todas as telas feitas em Jetpack Compose (`home`, `events`, `onboarding`, `settings`, `alarm`).
* `worker`: Tudo de Notificações e Alarme em segundo plano.
