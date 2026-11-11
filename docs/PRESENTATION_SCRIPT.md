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

* **O que fazer na tela:** Mostre onde coloca o apelido, escolhe o semestre (ex: 4º semestre de DSM) e o perfil de aluno (ex: "No Limite" ou "Calculista"). Clique para ir para a Home.
* **O que falar (marcando 2 requisitos):**
  > "Logo de cara, cumprimos dois requisitos obrigatórios:
  > 1. **Navegação entre telas** (`AppNavHost.kt`): o app sabe sozinho se é a primeira vez do aluno e navega entre o Onboarding, a Home, a agenda de Eventos e as Configurações.
  > 2. **DataStore** (`UserPreferencesRepository.kt`): o semestre que eu escolhi, meu apelido e meu nível de coragem para faltar ficam salvos no DataStore. Mesmo fechando o app, ele lembra exatamente quem eu sou."

---

## 🌐 3. API REST + SQLite/Room na Prática (1:45 - 3:00)
*(Na tela Home, mostre a lista "Minhas Disciplinas" carregada)*

* **O que fazer na tela:**
  1. Mostre os cards das matérias da Fatec com professores e horários.
  2. Clique no botão `+4` (ou `+2`) em uma matéria para somar faltas e veja a barra mudar de cor.
  3. Clique no ícone de **Lápis** (`EditSubjectDialog.kt`) e apague uma matéria com o botão **Retirar da Grade**.
  4. Vá em **+ Matéria Extra**, tente adicionar uma DP no mesmo horário de outra aula. Mostre o aviso de *"Conflito de horário"* substituindo a matéria na grade!
* **O que falar (marcando +2 requisitos):**
  > "Mas de onde vieram todas essas matérias de DSM? Da nossa **API REST** (`FatecApiService.kt`)! Quando abrimos o app, ele faz um *Auto-Sync*: o Retrofit baixa a grade da nuvem e faz um *Smart Merge* para o nosso banco local de forma 100% transparente.
  > Salvamos isso no nosso **SQLite via Room Database** (`AppDatabase.kt`). O mais legal é o nosso **Gerenciador de Conflitos**: se eu puxar uma matéria de DP e ela cair exatamente no mesmo dia e horário da minha grade normal, o banco intercepta a intersecção de dias e já avisa na tela, permitindo substituir uma pela outra para a grade não bugar, respeitando as regras do SIGA!"

---

## 🔮 4. O Oráculo da Falta e as Regras Reais da Fatec (3:00 - 3:45)
*(Aponte para o Card do Oráculo no topo da Home e clique no ícone de informação `ℹ️`)*

* **O que fazer na tela:** Mostre o veredito do dia e abra o modal de regras do SIGA.
* **O que falar:**
  > "Aqui no topo mora o **Oráculo**. Ele cruza os dados do SQLite com o regulamento real da Fatec: limite de 25% de faltas, aviso de que estourar falta bloqueia até o Exame Final, e alerta para matérias de Extensão. Se tiver prova cadastrada na aba de Eventos, o Oráculo fica vermelho na hora e proíbe a soneca!"

---

## ⏰ 5. O Gran Finale: Notificações Locais e Despertador da Preguiça (3:45 - 5:00)
*(Vá na aba Configurações e use os botões de teste de 5 segundos)*

* **O que fazer na tela:**
  1. Clique em **"Testar Notificação (5s)"** para mostrar a notificação padrão chegando no topo com o ícone da caminha.
  2. Clique em **"Testar Alarme Real (5s)"** e bloqueie a tela do celular.
  3. Quando o alarme tocar acendendo a tela (`AlarmRingingActivity.kt`), mostre a mensagem do Oráculo e aperte o botão mágico: **"🛌 Vou Faltar Hoje (+Falta)"**.
  4. Volte para a Home e mostre que a falta foi registrada sozinha no banco!
* **O que falar (marcando o 5º requisito com chave de ouro):**
  > "Para fechar o 5º requisito obrigatório de **Notificações Locais**, nós fomos além: temos o `WorkManager` (`DailyCheckWorker.kt`) que manda notificações diárias e um preview na noite anterior dizendo como será o dia seguinte.
  > E criamos também o **Despertador Real**: quando ele toca de manhã, você nem precisa fazer conta de cabeça. Se o Oráculo liberou o sono, você aperta **'Vou Faltar Hoje'** direto no despertador: ele cala a boca, soma as faltas do dia automaticamente no SQLite e você volta a dormir em paz. Muito obrigado!"
