# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
mvn javafx:run

# Compile only (no run)
mvn compile

# Package macOS .app image
./scripts/package-app.sh

# Package macOS .dmg installer
./scripts/package-dmg.sh
```

There are no tests yet.

## Architecture

This is a JavaFX 21 desktop app (Java 17+, Maven) with two features: a card-style digital clock and a to-do list. All UI is built programmatically in Java — there is no FXML.

**Entry point:** `MainApp.java` — extends `Application`, creates a `MainController`, wraps its view in a 760×480 `Scene`.

**Layers:**

- `controller/MainController.java` — owns a `BorderPane` root; builds clock (top) and todo (center) UI regions. Inner class `TodoItemCell` provides the custom `ListCell` rendering with checkbox + text + delete button, styled based on completion state.
- `service/ClockService.java` — drives a `Timeline` at 1 Hz, updating a `ReadOnlyStringWrapper` with `HH:mm:ss` formatted time. Callers bind to `currentTimeProperty()`.
- `service/TodoService.java` — wraps an `ObservableList<TodoItem>` and `TodoStorage`. Listens for list changes to auto-save. `toggleCompleted` saves explicitly since checkbox toggles mutate item state without triggering a list add/remove.
- `model/TodoItem.java` — two JavaFX properties: `text` (StringProperty) and `completed` (BooleanProperty).
- `storage/TodoStorage.java` — reads/writes `~/.todo-flip-clock/tasks.txt`. Format: one task per line, `completed|Base64(text)`. Base64 avoids delimiter/special-character issues. Corrupt lines are silently skipped.

**Data flow:** `ClockService` → property binding → `MainController` clock labels update automatically. `TodoService` ↔ `ObservableList` ↔ `ListView` — the list stays in sync via JavaFX bindings; persistence happens on every mutation.
