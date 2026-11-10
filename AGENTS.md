# Cloning

A three tier cloning industry chain that boosts colony growth at the cost of stability. These
instructions apply to this repository, including when it is opened without the parent mod
workspace. The workspace `AGENTS.md` one folder up governs releases, packaging, Git identity and
communication.

## Notion

Tasks and history: the **Cloning board** in Andrzej's Notion, under **Starsector mods**
(`collection://28241466-1489-43c0-a24c-8d4eb2fd1574`). Working rules and game API notes:
**Claude Information** (`collection://7bf9b1d6-b9e5-4b62-87f1-757bbaa51919`), starting with
"Start here: working on Starsector mods through Notion". Read the board before working, give every
task a ticket (ticket ids start with CLN) and keep it current. The Notion tools are deferred: load them with ToolSearch first.

Update the board during the work, not only at release time. Before changing files, put the
message on its ticket: a new request gets a new ticket (In progress), and a problem Andrzej
reports on Needs Human Test work moves that ticket to Changes Requested with the notes quoted.
Before ending the turn, update the ticket's body (What was done, How, Commits, Test list) and
its Status, Version and End date. Read the board with `notion-query-data-sources` mode `view`
on `view://3fd20ca7-5d20-81f3-946c-000cd35ab55e` or `notion-fetch` a ticket. SQL and rows mode share a small
quota that runs out. The workspace `AGENTS.md` has the full routine.

## Essentials

- id `oddisz_cloning`, package `cloning`, jar `jars/cloning.jar`, Java 17, Gradle wrapper.
  Depends on LunaLib and LazyLib.
- Build: `./gradlew jar`. There is no packaging script yet (see the "Tidy the Cloning repo"
  ticket). The Git remote is named `starsector-cloning`, not `origin`.
- Development logs in `DEVLOGS/`, release history in `changelog.md`.

## Update metadata

Keep `mod_info.json`, `cloning.version` and `data/config/version/version_files.csv` consistent.
Forum topic 35130.

- updateCheckURL and masterVersionFile:
  `https://raw.githubusercontent.com/andrzejsokolowski/starsector-cloning/main/cloning.version`
- directDownloadURL, asset `cloning.zip`:
  `https://github.com/andrzejsokolowski/starsector-cloning/releases/latest/download/cloning.zip`
