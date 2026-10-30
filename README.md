# Software Engineering Lab

# 📚 *AI Contract Analyser*

> **Disciplina:** Laboratório de Engenharia de Software  
> **Docente:** Gustavo Moreira Calixto
> **Metodologia:** Scrum / Agile

---

## 👥 Group Members

Name | RA
-----|---
**André Doerner Duarte** | 10427938
**Matheus Leonardo Cardoso Kroeff** | 10426434
**Naoto Ushizaki** | 10437445

---

## 1. 🎯 Introduction
In Brazil, economically vulnerable people often struggle to understand contracts due to complex legal and financial terms, especially those with low functional literacy. As a result, they may sign agreements without fully understanding their costs, deadlines, interest rates, and penalties.

Our project aims to make these contracts easier to understand through an application that analyzes their content and presents it in simplified form. It is developed using the **Scrum agile framework**, organized into **Sprints**, with the three Scrum roles: the **Product Owner**, who prioritizes requirements; the **Scrum Master**, who oversees the process; and the **Development Team**, who implements and delivers features.

---

## 2. 💡 The Project

### 2.1 Motivation

Difficulty understanding contracts can prevent people from having real control over their own financial decisions. In loans, financing, installment purchases, or service agreements, unclear terms can cause important information to go unnoticed.

We saw this as an opportunity to build a tool that simplifies this information while applying the **Software Engineering knowledge gained in the previous semester**, using **Scrum** to organize requirements, responsibilities, and deliveries incrementally. The application aims to promote financial autonomy and reduce uninformed decisions, while allowing the team to put agile development concepts into practice.

### 2.2 Objective

The main goal of the project is to apply Software Engineering knowledge in practice, using **Scrum** as the agile development framework so the team can experience its practices, roles, and processes throughout the system's development. As the product of this process, the team aims to build an application that promotes greater autonomy and confidence in financial decisions, making contracts more accessible to people who have difficulty interpreting them.

To achieve this, the application will use **Artificial Intelligence** to analyze contracts submitted as images or PDFs, translating their terms into plain language and highlighting key information such as **amounts, deadlines, interest rates, penalties, and potential risks**. It may also take into account user-provided information, such as income and financial situation, to offer a contextualized analysis of the commitment being made.

---

### 3. Methodology

The project will be developed using **Scrum** as an **agile framework**, **organizing work into Sprints** and prioritizing features through the *Product Backlog*. The team will track progress through Scrum's events and responsibilities, aiming for incremental deliveries and continuous process improvement.

- **Scrum:** Organizes the project into Sprints, with a Product Backlog, Sprint Backlog, and ongoing tracking and review of deliveries.
- **Product Owner:** Defines and prioritizes requirements, manages the Product Backlog, and validates that deliveries meet the project's goals.
- **Scrum Master:** Oversees Sprints, supports the application of Scrum, removes impediments, and helps organize and improve the process.
- **Development Team:** Implements, tests, and integrates the system's features.

---

## 4. 🚀 Conclusion and Expectations

Through this project, we expect to **put our *Software Engineering* knowledge into practice**, especially understanding **how *Scrum* works and progresses** during product development. Through *Sprints*, we aim to experience requirement definition, backlog prioritization, task distribution, development tracking, and results evaluation firsthand.

- **Learning:** Understand in practice how Scrum roles, events, and artifacts interact during development.
- **Experience:** Learn to organize and adapt the team's work as each Sprint progresses.
- **Application:** Use the concepts learned to build a functional solution while gaining experience with agile development.

---

## 🛠️ Running locally (development)

Requirements: Docker with Compose, Node 22+ and a Gemini API key. Full step-by-step (install from scratch, Linux/WSL/Mac/Windows/Codespaces, troubleshooting) in **[RODAR.md](RODAR.md)**. Short version, from the repository root:

```bash
cp .env.example .env              # then fill in GEMINI_API_KEY (the .env file is ignored by git)
(cd frontend && cp .env.example .env && npm install)   # set VITE_DATA_SOURCE=api in frontend/.env
scripts/rodar.sh                  # backend + PostgreSQL (Docker) + frontend, opens http://localhost:5174
scripts/parar.sh                  # stops everything (data is kept)
```

Inside Claude Code the same two actions are the `/run_kaup` and `/stop_kaup` skills.

Always start the backend before the frontend. Without it the app shows "Não foi possível falar com o servidor".

**Accounts.** Create an account on the "Criar conta" screen (name, e-mail, password of 8+ characters and its confirmation). Each person only sees their own documents. A development administrator account is created on startup (`ADMIN@ADMIN` / `ADMIN`): it can **read** every document but cannot change other people's documents, and its account cannot be deleted. **Set `KAUP_ADMIN_PASSWORD` (or `KAUP_ADMIN_ENABLED=false`) before publishing the system.** The first regular user to sign up adopts any documents that existed before accounts were introduced.

**Password safety, recovery, export and retention.** After 5 wrong passwords in a row the login of that e-mail is blocked for 10 seconds (HTTP 429), never longer. "Esqueci minha senha" is **simulated** for now: no e-mail is sent and the code is always `676869` (`KAUP_CODIGO_FAKE`; set it empty to turn recovery off); the administrator's password can never be changed through the app. The profile screen has "Baixar minhas informações" (a JSON with the account, documents, analyses and conversations; never the password) and documents are deleted automatically after `KAUP_RETENCAO_DIAS` days (default 180; `0` turns it off).

**Tests.** Backend: `cd backend && mvn verify` (or run Maven inside Docker). Frontend: `cd frontend && npm test`.

---

## ✉️ Contact

Feel free to reach out to the team members with questions, suggestions, or collaborations:

* **André Doerner Duarte**
  * 📧 Email: andredoernerd@gmail.com
  * 💼 [LinkedIn](https://www.linkedin.com/in/andredoernerduarte/)
  * 🐙 [GitHub](https://github.com/DedeDuarte)

* **Matheus Leonardo Cardoso Kroeff**
  * 📧 Email: 10426434@mackenzista.com.br
  * 💼 [LinkedIn](https://www.linkedin.com/in/matheus-leonardo-cardoso-kroeff-23838535b/)
  * 🐙 [GitHub](https://github.com/Matheus-Kroeff)

* **Naoto Ushizaki**
  * 📧 Email: 10437445@mackenzista.com.br
  * 💼 [LinkedIn](https://www.linkedin.com/in/naoto-ushizaki-30b94a172/)
  * 🐙 [GitHub](https://github.com/mackcoder)

## 5. 📚 References

* INSTITUTO PAULO MONTENEGRO. **INAF Brasil 2024: alfabetismo do analógico ao digital.** São Paulo, 2024. Available at: https://alfabetismofuncional.org.br/2024_Relatorio_Inaf_DIGITAL.pdf. Accessed on: Aug. 19, 2026.

* SOUZA, Rakel Brandão de et al. **Produção de conhecimento sobre o analfabetismo funcional e as desigualdades sociais no Brasil.** Goiânia, 2025. Available at: https://revistas.ufg.br/revistaufg/article/view/75874. Accessed on: Aug. 19, 2026.

* CORRÊA, Barbara Priscila de Souza; COLETTA, Carolina. **O acesso ao crédito em bancos digitais e fintechs aumenta a probabilidade de endividamento pessoal no Brasil?** 2024. Available at: https://ojs.unifor.br/rca/article/view/14924. Accessed on: Aug. 19, 2026.

