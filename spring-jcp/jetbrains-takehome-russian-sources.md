# JetBrains take-home assignments: findings from Russian sources

Researched 2026-10-05. Nothing was found for the Senior Developer (JCP Licensing & Quotation) role specifically. The material below is about internships and other roles, plus general process notes.

## Process (from Russian sources)

- Internship flow: first technical interview, then the test assignment, then a discussion of the assignment.
- Advice from candidates: before the interview, reopen your submitted solution and refresh your memory of what you wrote, because they ask about it.
- The JetBrains careers page describes the general flow: application review, recruiter interview, technical interview with the team, and a test assignment.

## Concrete task found: "Guu" debugger (developer internship)

Source: https://habr.com/ru/articles/454354/

Task: implement step-by-step execution of a trivial language called Guu.

- Language: procedures declared with `sub`; operators `set` (assign variable), `call` (invoke procedure), `print` (output value).
- Global variable scope; recursive calls must work.
- Debugger commands (minimum):
  - `i` - step into a procedure call
  - `o` - step over a call
  - `trace` - print stack trace with line numbers
  - `var` - list all variable values
- Evaluation criteria: overall functionality, code quality and tests, extensibility (new operators or commands).
- Constraints: publish in a Git repository; any language from TIOBE Top 50 with an open-source compiler.
- The author wrote about 500 lines (Object Pascal) in roughly one hour.

## Analytics internship

Source: https://habr.com/ru/companies/JetBrains-education/articles/597513/

The task was mostly descriptive: explain how you would implement the project, which methods you would use to solve the stated problems, and why.

## Not read or not verified

- sql.ru thread "Собеседование в JetBrains на Java программиста": https://www.sql.ru/forum/1223572/sobesedovanie-v-jetbrains-na-java-programmista (fetch failed, content unread)
- GitHub issue "Тестовое задание" in niktrop/selective-profiling-test: https://github.com/niktrop/selective-what profiling-test/issues/1 (not opened)
- RSDN thread: http://rsdn.org/forum/job/6429811.hot (not opened)
- Habr internship episode: https://habr.com/ru/companies/JetBrains-education/articles/553196/ (not opened)

## General format (English sources, for context)

Sources: https://www.techprep.app/blog/jetbrains-interview-process and Glassdoor.

- About 5-7 days to complete.
- Meant to resemble real work, not a puzzle.
- Judged on clean, maintainable code, avoiding overengineering, and sound design.
- A 60-minute review follows where you defend every decision and discuss scaling.
- AI tools are often allowed, but you must be able to explain all of the output.

## Inference for the JCP Licensing role (not from any source)

Likely themes, based on the job description: idempotent ingestion of purchase/licensing events, duplicate and out-of-order handling, reconciliation, Postgres schema and transactions, quota enforcement, integration tests against a real database, and API versioning. The review would probe consistency and migration trade-offs.
