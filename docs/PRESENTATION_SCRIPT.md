# 🎤 Roteiro de Apresentação: "SIGA na Cama" (5 Minutos Sem Tédio)

**Objetivo:** Mostrar que o app resolve a maior dor do estudante da Fatec (calcular se dá para dormir mais um pouco sem perder o semestre) enquanto dá check nos **5 requisitos obrigatórios** de forma leve e divertida.

---

## 🎬 1. Abertura Quebra-Gelo (0:00 - 0:45)
*(Fale olhando para a turma e para o professor, com o celular na mão)*

> "Bom dia/Boa noite, pessoal! Todo aluno da Fatec já viveu aquele dilema filosófico às 6h da manhã num dia chuvoso: **'Será que se eu faltar hoje eu reprovo ou ainda tenho margem?'**
> Abrir o SIGA pelo celular às 6h da manhã dá até tristeza. Pensando nisso, nós criamos o **SIGA na Cama**: o primeiro assistente acadêmico e despertador inteligente que conhece o regulamento do CEETEPS melhor que a gente e responde, na lata, se a sua cama está liberada ou se você precisa levantar correndo!"

---

## 🚀 2. Onboarding, Navegação e DataStore (0:45 - 1:45)
*(Mostre a tela de Onboarding ou Configurações no celular)*

* **O que fazer na tela:** Mostre onde coloca o apelido e escolhe o semestre (ex: 4º semestre de DSM), com os subtítulos bem-humorados da lista. No botão final **Começar**, explique que o app salva o semestre e sincroniza a grade antes de concluir o onboarding e abrir a Home.
* **O que falar (marcando 2 requisitos):**
  > "Logo de cara, cumprimos dois requisitos obrigatórios:
  > 1. **Navegação entre telas** (`AppNavHost.kt`): o app sabe sozinho se é a primeira vez do aluno e navega entre o Onboarding, a Home, a agenda de Eventos e as Configurações.
  > 2. **DataStore** (`UserPreferencesRepository.kt`): o semestre que eu escolhi e meu apelido ficam salvos no DataStore. Mesmo fechando o app, ele lembra exatamente quem eu sou."

---

## 🌐 3. API REST + SQLite/Room na Prática (1:45 - 3:00)
*(Na tela Home, mostre a lista "Minhas Disciplinas" carregada)*

* **O que fazer na tela:**
  1. Mostre os cards das matérias da Fatec com professores e horários. Abra o lápis de uma disciplina com mais de um dia, mostre os campos de horário individuais e salve, por exemplo, Segunda 07:40–11:20 e Terça 09:40–13:00.
  2. Clique no botão `+4` (ou `+2`) em uma matéria para somar faltas e veja a barra mudar de cor.
  3. Clique no ícone de **Lápis** (`EditSubjectDialog.kt`) e apague uma matéria com o botão **Retirar da Grade**.
  4. Vá em **+ Matéria Extra**, tente adicionar uma DP no mesmo horário de outra aula. Mostre o aviso de *"Conflito de horário"* substituindo a matéria na grade!
* **O que falar (marcando +2 requisitos):**
  > "Mas de onde vieram todas essas matérias de DSM? Da nossa **API REST** (`FatecApiService.kt`)! Quando abrimos o app, ele faz um *Auto-Sync*: o Retrofit baixa a grade e o calendário acadêmico da nuvem e faz um *Smart Merge* para o nosso banco local de forma 100% transparente.
  > Salvamos isso no nosso **SQLite via Room Database** (`AppDatabase.kt`), com migrações que mantêm os dados ao atualizar o esquema. Cada disciplina pode ter horários diferentes por dia, e as edições do aluno são preservadas na sincronização. O **Gerenciador de Conflitos** compara os intervalos no mesmo dia e, quando há sobreposição, permite substituir a matéria na grade. A regra de feriado também ficou centralizada em `AcademicRepository.isAcademicHoliday`, combinando Room e BrasilAPI."

---

## 4. Consulta diária e as Regras Reais da Fatec (3:00 - 3:45)
*(Aponte para o card de planejamento no topo da Home e clique no ícone de informação `ℹ️`)*

* **O que fazer na tela:** Mostre o veredito do dia e abra o modal de regras do SIGA.
* **O que falar:**
  > "Aqui no topo está a consulta diária. Ela cruza os dados do SQLite com o regulamento real da Fatec: limite de 25% de faltas, aviso de que estourar falta bloqueia até o Exame Final, e alerta para matérias de Extensão. Se tiver prova cadastrada na aba de Eventos, o alerta fica vermelho na hora e recomenda não faltar!"

---

## ⏰ 5. O Gran Finale: Notificações Locais e Despertador da Preguiça (3:45 - 5:00)
*(Vá na aba Configurações e use os botões de teste de 5 segundos)*

* **O que fazer na tela:**
  1. Em **Alarme Matinal Real**, mostre os modos **Automático (Pela 1ª aula)** e **Manual (Fixo)**. No automático, escolha uma antecedência; no manual, configure um horário fixo.
  2. Explique que o modo automático consulta a primeira aula futura e, na regra unificada, pula feriados vindos da BrasilAPI, feriados locais de São Paulo/Diadema e eventos cadastrados no Room. O app também não registra falta em feriado e usa fallback seguro se a rede falhar.
  3. Em **Som Atual**, abra **Escolher Meme / Som da API**. Mostre a busca inicial temática e os chips de sons do MyInstants Brasil; pré-visualize um resultado e selecione-o para baixar e salvar no cache local.
  4. Em **Preview na Noite Anterior**, mostre o horário padrão **21:00**, abra **Alterar horário do preview** e explique que a escolha é salva e reagendada.
  5. Clique em **"Testar Preview de Amanhã (5s)"** para mostrar a notificação chegando no topo. Ela usa o título **"Agenda de Amanhã — SIGA na Cama"** e traz aulas, resumo acadêmico e estado do despertador.
  6. Clique em **"Testar Alarme Real (5s)"** e bloqueie a tela do celular.
  7. Quando o alarme tocar acendendo a tela (`AlarmRingingActivity.kt`), mostre o resumo acadêmico e aperte o botão: **"🛌 Vou Faltar Hoje (+Falta)"**.
  8. Volte para a Home e mostre que a falta foi registrada sozinha no banco!
* **O que falar (marcando o 5º requisito com chave de ouro):**
  > "Para fechar o 5º requisito obrigatório de **Notificações Locais**, nós fomos além: temos o `WorkManager` (`DailyCheckWorker.kt`) que manda notificações diárias e um preview na noite anterior, em um horário escolhido pelo aluno, dizendo como será o dia seguinte. O worker só começa a avaliar depois que o onboarding termina e as notificações estão habilitadas; o primeiro trabalho periódico ainda tem um atraso inicial de seis horas.
  > O **Despertador Inteligente** pode usar um horário manual ou calcular o toque pela primeira aula em um dia letivo futuro, usando o horário próprio daquele dia e descontando a antecedência escolhida. Se o cálculo automático falhar, recorre temporariamente ao horário manual e mostra um aviso em Configurações. Ele usa a regra unificada de feriados para ignorar datas não letivas. Também dá para buscar sons no MyInstants Brasil, ouvir uma prévia e manter o MP3 selecionado no cache privado para uso offline. Se o arquivo personalizado falhar, o app tenta o toque de alarme nativo do Android.
  > A restauração após reinicialização do Android também ficou pronta com `BootReceiver`, que reprograma o alarme e o preview a partir do DataStore. Quando o alarme toca, você não precisa fazer conta de cabeça: confira o resumo da sua grade e aperte **'Vou Faltar Hoje'** direto no despertador; ele soma as faltas do dia automaticamente no SQLite. Muito obrigado!"
