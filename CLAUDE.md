# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
mvn javafx:run

# Compile only (no run)
mvn compile

# Run unit tests
mvn test

# Package macOS .app image
./scripts/package-app.sh

# Package macOS .dmg installer
./scripts/package-dmg.sh
```

## Architecture

JavaFX 21 desktop app (Java 17+, Maven). All UI is programmatic — no FXML.

**Entry point:** `MainApp.java` loads config from `~/.todo-flip-clock/config.properties` (theme, window position/size), creates `MainController` with theme preference, calls `controller.createScene()` which loads CSS, then shows the stage.

**Layers:**

- `controller/MainController.java` — top area has a Clock/Countdown toggle (`ToggleButton`) and theme toggle (☀/☾). Todo panel below with inline editing, priority ComboBox, DatePicker, drag reorder, right-click "Start 25-minute timer" context menu. Keyboard shortcuts: Ctrl+N (focus input), Ctrl+D (toggle theme), Delete (delete selected with confirmation). Inner `TodoItemCell` handles priority color bar, overdue date badges, inline edit, and drag-and-drop.
- `controller/FlipClockPane.java` — split-flap clock with `FlipCard` inner class. Each card uses 4 clipped `Label`s in a `StackPane`, animated via `Timeline` + `Rotate` transforms (X-axis, 300ms). PerspectiveCamera for 3D depth.
- `controller/CountdownPane.java` — preset time buttons (5/15/25/45/60m + custom input), styled time display, Start/Pause/Reset. `startForTask(name, mins)` called from todo context menu.
- `service/ClockService.java` — 1 Hz `Timeline`, exposes `currentTimeProperty()` (HH:mm:ss) and `currentDateProperty()` (yyyy年MM月dd日 EEEE).
- `service/CountdownService.java` — 1 Hz `Timeline`, `remainingTimeProperty()` (MM:SS), `isRunningProperty()`, plays system beep on completion. `setRemainingSeconds()` exposed for tests.
- `service/TodoService.java` — `ObservableList<TodoItem>` with auto-save listener; `addTask()`, `toggleCompleted()`, `deleteTask()`, `moveTask()`, `save()`.
- `model/TodoItem.java` — properties: text, completed, priority (HIGH/MEDIUM/LOW/NONE), dueDate (nullable LocalDate).
- `storage/TodoStorage.java` — `~/.todo-flip-clock/tasks.txt`. Format: `completed|priority|dueDate|Base64(text)`. Backward-compatible with old 2-field format. Corrupt lines skipped.
- **CSS:** `main.css` (all styles) + `dark.css` (dark overrides), loaded from `src/main/resources/styles/`. Theme toggling adds/removes dark.css from scene stylesheets.
- **Tests:** JUnit 5, 62 tests across 5 test classes (`@TempDir` for file tests).
