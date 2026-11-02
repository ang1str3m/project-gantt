# Employee Management System (Desktop)

JavaFX desktop application for planning and tracking project tasks with a Gantt timeline.

## Highlights
- Tree-table task structure (tasks + subtasks)
- Gantt chart with synchronized scrolling
- Search and status filtering
- Live dashboard metrics (tasks, subtasks, completed, overdue, progress)
- Task dependency constraints (including critical dependencies)
- Financial milestones for scheduling (contract advance date, KS payment date)
- Project autosave and restore prompt
- Export: PDF, PNG, CSV
- User auth with password hashing (PBKDF2 + legacy migration support)

## Tech Stack
- Java 21 (Temurin/OpenJDK)
- JavaFX 21
- Maven
- SQLite (`sqlite-jdbc`)
- Jackson (`jackson-databind`)
- iText7 (PDF)
- JUnit 5

## Run
```bash
./mvnw clean javafx:run
```

## Build
```bash
./mvnw clean package
```

## Tests
```bash
./mvnw test
```

## Dev Quick Start (Windows PowerShell)
```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean test
.\mvnw.cmd javafx:run
```

## Notes
- `JAVA_HOME` must point to JDK 21.
- Maven Wrapper (`mvnw` / `mvnw.cmd`) is preferred to keep Maven version consistent for all developers.

## Project File Format
- Main project format: JSON (`*.json`)
- Dependency syntax in UI:
  - `1.:FS` (finish-to-start)
  - `2.:SS+1` (start-to-start with +1 day lag)
  - `3.:FF-2` (finish-to-finish with -2 day lag)
- Autosave path:
  - `%USERPROFILE%\\.gantrem_app\\autosave_project.json` (Windows)

## Suggested Next Milestones
1. Introduce service layer (`TaskService`, `ReportService`) to reduce controller size.
2. Add undo/redo for task editing operations.
3. Add role model (admin/manager/viewer) for multi-user scenarios.
4. Add full persistence for tasks in SQLite (optional alternative to JSON-only project files).
