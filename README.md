# SIGA na Cama 🛏️💤 — O Despertador Inteligente com Consciência Acadêmica para Fatec

[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/GabrieldeMoura9648/SIGA-na-Cama/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

Aplicativo Android para estudantes de Desenvolvimento de Software Multiplataforma (DSM) da Fatec Diadema. O **SIGA na Cama** combina a grade cadastrada, eventos acadêmicos e o saldo de faltas para apresentar um alerta contextual; o despertador pode tocar sobre a tela bloqueada e registrar as faltas do dia no Room.

> As mensagens do app representam regras configuradas no produto e não substituem o regulamento oficial, o SIGA, a secretaria ou a coordenação. Confirme com a instituição a vigência e a aplicação de regras acadêmicas.

## O problema resolvido

Conciliar horários, provas e frequência pode gerar ansiedade e decisões baseadas em informações dispersas. O SIGA na cama reúne o acompanhamento de disciplinas e faltas, sinaliza eventos e limites de frequência e lembra que um atestado curto não significa abono automático. A orientação considera o limite configurado por disciplina, a possibilidade de perda do Exame Final por reprovação de frequência e o tratamento indicado no app para componentes de extensão.

## Funcionalidades e telas

- **Onboarding:** configuração inicial, apelido e seleção de semestre, com sincronização da grade.
- **Meu Dia / Minhas Disciplinas:** consulta do veredito, saldo e ações de ajuste de faltas.
- **Regras de Sobrevivência (Fatec):** resumo de frequência, Exame Final e atestados.
- **Eventos:** agenda de eventos e datas cadastradas.
- **Matéria Extra:** seleção de semestre e catálogo por dropdown, além de cadastro manual.
- **Configurações:** semestre, notificações, horário do alarme, horário do preview noturno e sincronização.
- **Despertador inteligente:** modo manual com horário fixo ou modo automático calculado a partir da primeira aula em um dia letivo futuro, com antecedência configurável. No automático, ignora feriados nacionais consultados na BrasilAPI e feriados estaduais/municipais cadastrados para São Paulo e Diadema; inclui alarme em tela cheia, soneca de 10 minutos, dispensar e registro das faltas das disciplinas com aula hoje.
- **Sons personalizados:** busca de sons do MyInstants Brasil com Safe Search acadêmico, prévia em streaming e download para cache privado offline; se o áudio personalizado falhar, o alarme tenta tocar o ringtone nativo do Android.
- **Preview noturno:** notificação “Agenda de Amanhã — SIGA na Cama” com as aulas, o veredito do dia seguinte e o estado do despertador. O horário é configurável (padrão 21:00).

## Requisitos acadêmicos atendidos

- ✅ **Navegação entre telas:** Navigation Compose para onboarding, Meu Dia, eventos e configurações.
- ✅ **Preferências assíncronas:** DataStore para apelido, semestre, URL da API, notificações, onboarding, alarmes e horário do preview noturno.
- ✅ **Persistência local:** Room sobre SQLite para disciplinas, eventos, desculpas e edições personalizadas.
- ✅ **API REST:** Retrofit/Gson para sincronizar o catálogo JSON; em caso de falha, o repositório repõe os dados locais de `DefaultFatecData`.
- ✅ **Consulta de feriados:** Retrofit/Gson consulta a BrasilAPI por ano; o modo automático também reconhece 9 de julho (São Paulo) e 8 de dezembro (Diadema). A consulta é assíncrona e os resultados nacionais são mantidos em memória durante a execução.
- ✅ **Sons externos e conectividade:** OkHttp consulta diretamente o MyInstants Brasil; o parser HTML usa UTF-8 e blacklist local. O áudio selecionado é armazenado em cache privado para reprodução offline.
- ✅ **Notificações e alarmes:** WorkManager para tarefas periódicas/adiáveis; AlarmManager para alarme diário, soneca e preview, com fallback inexato quando o sistema não autoriza alarme exato.

**Nota sobre os dados acadêmicos:** `fatec_api.json` é a fonte configurada para sincronização da grade e do calendário acadêmico; o fallback de catálogo executado em runtime usa `DefaultFatecData`, não o asset JSON. Os feriados nacionais fixos foram removidos do JSON por serem consultados à BrasilAPI. Permanecem no calendário acadêmico Carnaval, Sexta-feira Santa, Corpus Christi e datas institucionais como a folga do Dia do Professor e o Dia do Servidor Público. Se a consulta de feriados nacionais falhar, o modo automático cancela o alarme diário por segurança; o modo manual não depende dessa consulta. A disponibilidade do alarme exato também depende da versão do Android e da permissão concedida pelo usuário.

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Kotlin | Linguagem principal |
| Jetpack Compose e Material 3 | Interface Android declarativa |
| Navigation Compose | Navegação entre telas |
| Room / SQLite | Persistência local |
| DataStore Preferences | Preferências reativas do usuário |
| Retrofit e Gson | Consumo das APIs acadêmica e de feriados |
| OkHttp | Requisições diretas ao MyInstants e download do áudio personalizado |
| WorkManager | Notificações e tarefas adiáveis |
| AlarmManager | Disparos de relógio para alarme e preview |

Os fluxos de APIs, feriados, agendamento inteligente, sons, cache offline e fallback estão documentados em [`docs/CODE_ARCHITECTURE.md`](docs/CODE_ARCHITECTURE.md), [`docs/API.md`](docs/API.md) e [`docs/OFFLINE_FIRST.md`](docs/OFFLINE_FIRST.md).

O projeto usa Gradle Kotlin DSL, Version Catalog e um único módulo Android, `:app`. A configuração declara `minSdk 26`, `compileSdk 36` e `targetSdk 36`.

## Regras de negócio em resumo

- A consulta diária compara as aulas previstas para o dia com o limite de faltas salvo na disciplina.
- Ao registrar uma falta pelo card ou pelo despertador, o app soma o bloco de aulas do encontro (`classesPerDay`); ajustes finos podem ser feitos no diálogo de edição.
- O veredito considera feriados/domingos e dias sem aula, eventos críticos, limite absoluto e margem fixa de segurança. O agendamento automático pula feriados nacionais consultados na BrasilAPI e os feriados fixos de São Paulo e Diadema.
- O app alerta sobre a perda do Exame Final quando reprovação por frequência é prevista e diferencia a mensagem para disciplinas extensionistas.
- Quando a próxima falta excederia o limite da disciplina, o alerta identifica a reprovação por frequência com o título “GAME OVER”.
- As informações sobre atestados e Regime Domiciliar são orientativas e devem ser confirmadas com os canais oficiais da Fatec.

Veja a arquitetura e o fluxo de agendamento em [`docs/CODE_ARCHITECTURE.md`](docs/CODE_ARCHITECTURE.md) e a integração de APIs em [`docs/API.md`](docs/API.md).

## Como Rodar / Instalação

### 📲 Opção 1: Instalação Direta no Celular via APK (Mais Rápido — Sem Android Studio)

1. Acesse a aba oficial de [**Releases do Repositório**](https://github.com/GabrieldeMoura9648/SIGA-na-Cama/releases) pelo celular Android ou computador.
2. Na versão mais recente (Latest Release), baixe o arquivo `.apk` disponível em *Assets*.
3. Abra o arquivo baixado no seu Android (caso solicitado, autorize a permissão de *"Instalar apps de fontes desconhecidas"* do navegador/arquivos).
4. Conclua a instalação e abra o **SIGA na Cama**!

### 💻 Opção 2: Rodar pelo Código-Fonte (Android Studio)

1. Clone o repositório:
   ```bash
   git clone https://github.com/GabrieldeMoura9648/SIGA-na-Cama.git
   ```
2. Abra a pasta raiz no **Android Studio** (certifique-se de utilizar o **JDK 17** nas configurações do Gradle).
3. Aguarde o Gradle Sync finalizar e clique em **Run ('app')** em um dispositivo físico ou emulador Android 8.0+ (API 26+).

## Permissões e demonstração

O Android pode solicitar autorização para notificações e alarmes exatos. Em **Configurações > Despertador & Preview**, o usuário pode ativar o preview noturno e escolher seu horário; o padrão é **21:00**. O preview usa o título **“Agenda de Amanhã — SIGA na Cama”** e apresenta disciplinas previstas, o veredito do dia seguinte e o estado do despertador. Para testar o despertador, conceda as permissões exibidas e use **Testar Alarme Real (5s)**. Para conferir o botão **Vou Faltar Hoje (+Falta)**, a grade precisa ter disciplinas com aula na data corrente.

Consulte o roteiro de demonstração em [`docs/PRESENTATION_SCRIPT.md`](docs/PRESENTATION_SCRIPT.md) e o mapa de camadas em [`docs/CODE_ARCHITECTURE.md`](docs/CODE_ARCHITECTURE.md).
