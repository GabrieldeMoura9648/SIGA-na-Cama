# SIGA na cama 🛏️💤 — O Despertador Inteligente com Consciência Acadêmica para Fatec

Aplicativo Android para estudantes de Desenvolvimento de Software Multiplataforma (DSM) da Fatec Diadema. O **Oráculo da Falta** combina a grade cadastrada, eventos acadêmicos e o saldo de faltas para apresentar um alerta contextual; o despertador pode tocar sobre a tela bloqueada e registrar as faltas do dia no Room.

> As mensagens do app representam regras configuradas no produto e não substituem o regulamento oficial, o SIGA, a secretaria ou a coordenação. Confirme com a instituição a vigência e a aplicação de regras acadêmicas.

## O problema resolvido

Conciliar horários, provas e frequência pode gerar ansiedade e decisões baseadas em informações dispersas. O SIGA na cama reúne o acompanhamento de disciplinas e faltas, sinaliza eventos e limites de frequência e lembra que um atestado curto não significa abono automático. A orientação considera o limite configurado por disciplina, a possibilidade de perda do Exame Final por reprovação de frequência e o tratamento indicado no app para componentes de extensão.

## Funcionalidades e telas

- **Onboarding:** configuração inicial e seleção de semestre, com sincronização da grade.
- **Oráculo / Minhas Disciplinas:** consulta do veredito, saldo e ações de ajuste de faltas.
- **Regras de Sobrevivência (Fatec):** resumo de frequência, Exame Final e atestados.
- **Eventos:** agenda de eventos e datas cadastradas.
- **Matéria Extra:** seleção de semestre e catálogo por dropdown, além de cadastro manual.
- **Configurações:** semestre, margem de alerta, notificações, horário do alarme, preview noturno e sincronização.
- **Despertador real:** alarme em tela cheia, soneca de 10 minutos, dispensar e registro das faltas das disciplinas com aula hoje.
- **Preview noturno:** notificação de resumo do veredito para o dia seguinte.

## Requisitos acadêmicos atendidos

- ✅ **Navegação entre telas:** Navigation Compose para onboarding, Oráculo, eventos e configurações.
- ✅ **Preferências assíncronas:** DataStore para apelido, semestre, nível de coragem, URL da API, notificações, onboarding e configuração do alarme.
- ✅ **Persistência local:** Room sobre SQLite para disciplinas, eventos, desculpas e edições personalizadas.
- ✅ **API REST:** Retrofit/Gson para sincronizar o catálogo JSON; em caso de falha, o repositório repõe os dados locais de `DefaultFatecData`.
- ✅ **Notificações e alarmes:** WorkManager para tarefas periódicas/adiáveis; AlarmManager para alarme diário, soneca e preview, com fallback inexato quando o sistema não autoriza alarme exato.

**Nota sobre o JSON offline:** `app/src/main/assets/fatec_api.json` e `fatec_api.json` são arquivos JSON do projeto, mas o fallback executado em runtime atualmente usa `DefaultFatecData`; o repositório não lê o asset JSON como fallback. A disponibilidade do alarme exato também depende da versão do Android e da permissão concedida pelo usuário.

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Kotlin | Linguagem principal |
| Jetpack Compose e Material 3 | Interface Android declarativa |
| Navigation Compose | Navegação entre telas |
| Room / SQLite | Persistência local |
| DataStore Preferences | Preferências reativas do usuário |
| Retrofit e Gson | Consumo e desserialização da API REST |
| WorkManager | Notificações e tarefas adiáveis |
| AlarmManager | Disparos de relógio para alarme e preview |

O projeto usa Gradle Kotlin DSL, Version Catalog e um único módulo Android, `:app`. A configuração declara `minSdk 26`, `compileSdk 36` e `targetSdk 36`.

## Regras de negócio em resumo

- O Oráculo compara as aulas previstas para o dia com o limite de faltas salvo na disciplina.
- Ao registrar uma falta pelo card ou pelo despertador, o app soma o bloco de aulas do encontro (`classesPerDay`); ajustes finos podem ser feitos no diálogo de edição.
- O veredito considera feriados/domingos e dias sem aula, eventos críticos, limite absoluto e margem de segurança escolhida.
- O app alerta sobre a perda do Exame Final quando reprovação por frequência é prevista e diferencia a mensagem para disciplinas extensionistas.
- As informações sobre atestados e Regime Domiciliar são orientativas e devem ser confirmadas com os canais oficiais da Fatec.

Veja a referência completa em [`docs/REGRAS_E_ROADMAP.md`](docs/REGRAS_E_ROADMAP.md).

## Como rodar

### Pré-requisitos

- Android Studio compatível com Android Gradle Plugin 8.13.2.
- JDK 17 configurado no Android Studio/Gradle.
- Android SDK 36 instalado.
- Um emulador ou dispositivo Android; o Motorola Moto E40 é o aparelho-alvo de validação de interface.

### Android Studio

1. Clone o repositório:

   ```bash
   git clone https://github.com/GabrieldeMoura9648/SIGA-na-Cama.git
   ```

2. Abra a pasta `FatecSkip` no Android Studio.
3. Configure o Gradle para usar JDK 17: **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK**.
4. Aguarde **Sync Project with Gradle Files**. Configure o caminho do Android SDK se solicitado; `local.properties` é específico da máquina e não deve ser compartilhado.
5. Selecione um dispositivo/emulador e execute a configuração `app`.

### Terminal

Na raiz do projeto, com JDK 17 e Android SDK configurados:

```powershell
.\gradlew.bat assembleDebug
```

O APK de debug é gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Permissões e demonstração

O Android pode solicitar autorização para notificações e alarmes exatos. Para testar o despertador, ative-o em **Configurações > Despertador do Oráculo & Preview**, conceda as permissões exibidas e use **Testar Alarme Real (5s)**. Para conferir o botão **Vou Faltar Hoje (+Falta)**, a grade precisa ter disciplinas com aula na data corrente.

Consulte o roteiro de demonstração em [`docs/PRESENTATION_SCRIPT.md`](docs/PRESENTATION_SCRIPT.md) e o mapa de camadas em [`docs/CODE_ARCHITECTURE.md`](docs/CODE_ARCHITECTURE.md).
