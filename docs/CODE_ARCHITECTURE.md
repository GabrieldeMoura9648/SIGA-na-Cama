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
* **O toque de mestre (diferencial):** Criamos a coluna `isCustomized`. Se o aluno editar o nome do professor ou adicionar faltas, quando a API sincronizar de novo, o banco protege a edição do aluno e não sobrescreve!
* **Onde abrir para mostrar ao professor:**
  * Banco: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/AppDatabase.kt`
  * Comandos SQL: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/SubjectDao.kt`
  * Tabelas: acesse na pasta `app/src/main/java/com/example/fatecskip/data/local/entities/SubjectEntity.kt`

---

### 4. API REST (Consumo de Serviço Externo com Retrofit)
* **A ideia simples:** O app busca na nuvem (GitHub Raw JSON) a grade oficial de DSM da Fatec, feriados e lista de desculpas.
* **O toque de mestre (Offline-First):** E se a internet da Fatec cair na hora da apresentação? O `AcademicRepository` tenta buscar na API via `Retrofit`; se falhar, ele lê instantaneamente o arquivo local `fatec_api.json` na pasta `assets/`. O app nunca quebra!
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

## 🗂️ Resumo Rápido das Pastas (Caso peçam um tour pelo código)
* `data/local`: Tudo de SQLite/Room (`AppDatabase`, `DAOs` e `Entities`).
* `data/preferences`: Tudo de DataStore (`UserPreferencesRepository`).
* `data/remote`: Tudo de API REST (`FatecApiService` e `DTOs`).
* `data/repository`: O "cérebro" (`AcademicRepository`) que junta API, SQLite e calcula se você vai reprovar por falta.
* `domain/model`: Onde vive o veredito do Oráculo (`SkipVerdict.kt`).
* `ui/screens`: Todas as telas feitas em Jetpack Compose (`home`, `events`, `onboarding`, `settings`, `alarm`).
* `worker`: Tudo de Notificações e Alarme em segundo plano.
