package com.example.todoflipclock.controller;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.service.ClockService;
import com.example.todoflipclock.service.CountdownService;
import com.example.todoflipclock.service.TodoService;
import com.example.todoflipclock.storage.TodoStorage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainController {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");

    private final ClockService clockService = new ClockService();
    private final CountdownService countdownService = new CountdownService();
    private final TodoService todoService = new TodoService(new TodoStorage());
    private final BorderPane root = new BorderPane();

    // Top area
    private final FlipClockPane flipClockPane;
    private final CountdownPane countdownPane;
    private final ToggleButton clockTab = new ToggleButton("Clock");
    private final ToggleButton countdownTab = new ToggleButton("Countdown");
    private final Button themeBtn = new Button();

    // Input row state
    private final ComboBox<TodoItem.Priority> priorityCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();

    // Captured for keyboard shortcuts
    private TextField taskInput;
    private ListView<TodoItem> listView;

    // Theme
    private boolean darkTheme;

    public MainController(boolean darkTheme) {
        this.darkTheme = darkTheme;
        flipClockPane = new FlipClockPane(clockService);
        countdownPane = new CountdownPane(countdownService);
        buildUi();
    }

    public Scene createScene() {
        Scene scene = new Scene(root, 760, 480);
        scene.getStylesheets().add(
                getClass().getResource("/styles/main.css").toExternalForm());
        if (darkTheme) {
            scene.getStylesheets().add(
                    getClass().getResource("/styles/dark.css").toExternalForm());
        }
        setupKeyboardShortcuts(scene);
        return scene;
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    // -----------------------------------------------------------------------
    // UI construction
    // -----------------------------------------------------------------------

    private void buildUi() {
        root.getStyleClass().add("root-bg");
        root.setPadding(new Insets(24));
        root.setTop(buildTopArea());
        root.setCenter(createTodoView());

        Platform.runLater(() -> root.getScene().getWindow().setOnCloseRequest(event -> {
            clockService.stop();
            countdownService.stop();
        }));
    }

    // -----------------------------------------------------------------------
    // Top area — toggle bar + theme button + display stack
    // -----------------------------------------------------------------------

    private VBox buildTopArea() {
        ToggleGroup tg = new ToggleGroup();
        clockTab.setToggleGroup(tg);
        countdownTab.setToggleGroup(tg);
        clockTab.setSelected(true);

        clockTab.getStyleClass().add("tab-btn");
        countdownTab.getStyleClass().add("tab-btn");

        clockTab.setOnAction(e -> {
            if (clockTab.isSelected()) showClock();
        });
        countdownTab.setOnAction(e -> {
            if (countdownTab.isSelected()) showCountdown();
        });

        themeBtn.getStyleClass().add("theme-btn");
        updateThemeIcon();
        themeBtn.setOnAction(e -> toggleTheme());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toggleBar = new HBox(8, clockTab, countdownTab, spacer, themeBtn);
        toggleBar.setAlignment(Pos.CENTER_LEFT);
        toggleBar.setPadding(new Insets(0, 0, 16, 0));

        StackPane displayStack = new StackPane(flipClockPane, countdownPane);
        showClock();

        VBox top = new VBox(toggleBar, displayStack);
        top.setAlignment(Pos.CENTER);
        return top;
    }

    private void showClock() {
        flipClockPane.setVisible(true);
        flipClockPane.setManaged(true);
        countdownPane.setVisible(false);
        countdownPane.setManaged(false);
    }

    private void showCountdown() {
        flipClockPane.setVisible(false);
        flipClockPane.setManaged(false);
        countdownPane.setVisible(true);
        countdownPane.setManaged(true);
    }

    private void switchToCountdown() {
        countdownTab.setSelected(true);
        showCountdown();
    }

    // -----------------------------------------------------------------------
    // Theme toggle
    // -----------------------------------------------------------------------

    private void toggleTheme() {
        darkTheme = !darkTheme;
        Scene scene = root.getScene();
        if (scene == null) return;

        String darkCss = getClass().getResource("/styles/dark.css").toExternalForm();
        if (darkTheme) {
            scene.getStylesheets().add(darkCss);
        } else {
            scene.getStylesheets().remove(darkCss);
        }
        updateThemeIcon();
    }

    private void updateThemeIcon() {
        themeBtn.setText(darkTheme ? "☀" : "☾");
    }

    // -----------------------------------------------------------------------
    // Keyboard shortcuts
    // -----------------------------------------------------------------------

    private void setupKeyboardShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Ctrl/Cmd+N — focus task input
            if (event.getCode() == KeyCode.N && event.isShortcutDown()) {
                if (taskInput != null) {
                    taskInput.requestFocus();
                    event.consume();
                }
            }
            // Ctrl/Cmd+D — toggle theme
            else if (event.getCode() == KeyCode.D && event.isShortcutDown()) {
                toggleTheme();
                event.consume();
            }
            // Delete / Backspace — delete selected task (when list is focused)
            else if ((event.getCode() == KeyCode.DELETE
                    || event.getCode() == KeyCode.BACK_SPACE)
                    && listView != null
                    && listView.isFocused()
                    && !(event.getTarget() instanceof TextField)) {
                deleteSelectedTask();
                event.consume();
            }
        });
    }

    private void deleteSelectedTask() {
        if (listView == null) return;
        TodoItem selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Task");
        confirm.setHeaderText("Delete \"" + selected.getText() + "\"?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                todoService.deleteTask(selected);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Todo panel
    // -----------------------------------------------------------------------

    private VBox createTodoView() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("todo-panel");

        Label title = new Label("Tasks");
        title.getStyleClass().add("todo-title");

        HBox inputRow = createInputRow();

        listView = new ListView<>(todoService.getItems());
        listView.setPlaceholder(createEmptyPlaceholder());
        listView.setCellFactory(view -> new TodoItemCell(todoService, listView, this::onStartTaskTimer));
        VBox.setVgrow(listView, Priority.ALWAYS);

        panel.getChildren().addAll(title, inputRow, listView);
        return panel;
    }

    private VBox createEmptyPlaceholder() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        Label icon = new Label("📋");
        icon.getStyleClass().add("empty-state-icon");
        Label text = new Label("No tasks yet.\nPress Ctrl+N to add one.");
        text.getStyleClass().add("empty-state");
        box.getChildren().addAll(icon, text);
        return box;
    }

    // -----------------------------------------------------------------------
    // Input row
    // -----------------------------------------------------------------------

    private HBox createInputRow() {
        taskInput = new TextField();
        taskInput.setPromptText("Add a task");
        taskInput.getStyleClass().add("todo-input");
        HBox.setHgrow(taskInput, Priority.ALWAYS);

        priorityCombo.getItems().setAll(TodoItem.Priority.values());
        priorityCombo.setValue(TodoItem.Priority.NONE);
        priorityCombo.getStyleClass().add("priority-combo");
        priorityCombo.setButtonCell(new PriorityListCell());
        priorityCombo.setCellFactory(p -> new PriorityListCell());

        datePicker.getStyleClass().add("date-picker");
        datePicker.setPromptText("Due date");
        datePicker.setEditable(false);
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d != null ? d.format(DATE_FMT) : "";
            }

            @Override
            public LocalDate fromString(String s) {
                return null;
            }
        });

        Button addButton = new Button("Add");
        addButton.getStyleClass().add("add-btn");
        addButton.setDefaultButton(true);
        addButton.setOnAction(e -> commitAdd());
        taskInput.setOnAction(e -> commitAdd());

        return new HBox(8, taskInput, priorityCombo, datePicker, addButton);
    }

    private void commitAdd() {
        String text = taskInput.getText();
        if (text.trim().isEmpty()) return;
        todoService.addTask(text, priorityCombo.getValue(), datePicker.getValue());
        taskInput.clear();
        priorityCombo.setValue(TodoItem.Priority.NONE);
        datePicker.setValue(null);
    }

    // -----------------------------------------------------------------------
    // Countdown from todo
    // -----------------------------------------------------------------------

    private void onStartTaskTimer(TodoItem item) {
        countdownPane.startForTask(item.getText(), 25);
        switchToCountdown();
    }

    // -----------------------------------------------------------------------
    // List cell
    // -----------------------------------------------------------------------

    private static class TodoItemCell extends ListCell<TodoItem> {
        private final TodoService todoService;
        private final ListView<TodoItem> listView;
        private final java.util.function.Consumer<TodoItem> onStartTimer;
        private final HBox row = new HBox(8);
        private final Rectangle priorityBar = new Rectangle(4, 34);
        private final CheckBox completedBox = new CheckBox();
        private final Label textLabel = new Label();
        private final TextField editField = new TextField();
        private final Label dateLabel = new Label();
        private final Button deleteButton = new Button("Delete");

        private boolean editing;

        TodoItemCell(TodoService todoService, ListView<TodoItem> listView,
                     java.util.function.Consumer<TodoItem> onStartTimer) {
            this.todoService = todoService;
            this.listView = listView;
            this.onStartTimer = onStartTimer;
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 2, 4, 2));

            priorityBar.setArcWidth(2);
            priorityBar.setArcHeight(2);

            textLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(textLabel, Priority.ALWAYS);

            editField.getStyleClass().add("edit-field");
            editField.setVisible(false);
            editField.setManaged(false);
            HBox.setHgrow(editField, Priority.ALWAYS);

            deleteButton.getStyleClass().add("delete-btn");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(
                    priorityBar, completedBox, textLabel, editField, spacer, dateLabel, deleteButton);

            setupEdit();
            setupDrag();
        }

        // --- inline edit ---

        private void setupEdit() {
            textLabel.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) startEditing();
            });

            editField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    commitEdit();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    abortEdit();
                }
            });

            editField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal && editing) commitEdit();
            });
        }

        private void startEditing() {
            if (editing || isEmpty()) return;
            editing = true;
            textLabel.setVisible(false);
            textLabel.setManaged(false);
            editField.setText(textLabel.getText());
            editField.setVisible(true);
            editField.setManaged(true);
            editField.requestFocus();
            editField.selectAll();
        }

        private void commitEdit() {
            if (!editing) return;
            editing = false;
            TodoItem item = getItem();
            if (item != null && !editField.getText().trim().isEmpty()) {
                item.textProperty().set(editField.getText().trim());
                todoService.save();
            }
            editField.setVisible(false);
            editField.setManaged(false);
            textLabel.setVisible(true);
            textLabel.setManaged(true);
        }

        private void abortEdit() {
            if (!editing) return;
            editing = false;
            editField.setVisible(false);
            editField.setManaged(false);
            textLabel.setVisible(true);
            textLabel.setManaged(true);
        }

        // --- drag reorder ---

        private void setupDrag() {
            row.setOnDragDetected(e -> {
                if (isEmpty()) return;
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(getIndex()));
                db.setContent(content);
                e.consume();
            });

            row.setOnDragOver(e -> {
                if (e.getGestureSource() != row && e.getDragboard().hasString()) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });

            row.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                if (db.hasString()) {
                    int from = Integer.parseInt(db.getString());
                    int to = getIndex();
                    if (from != to) {
                        todoService.moveTask(from, to);
                        listView.getSelectionModel().select(to);
                    }
                    e.setDropCompleted(true);
                } else {
                    e.setDropCompleted(false);
                }
                e.consume();
            });
        }

        // --- render ---

        @Override
        protected void updateItem(TodoItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                editing = false;
                editField.setVisible(false);
                editField.setManaged(false);
                textLabel.setVisible(true);
                textLabel.setManaged(true);
                return;
            }

            // Priority color bar
            TodoItem.Priority p = item.getPriority();
            priorityBar.setVisible(p != TodoItem.Priority.NONE);
            priorityBar.setFill(switch (p) {
                case HIGH -> Color.valueOf("#ef4444");
                case MEDIUM -> Color.valueOf("#f59e0b");
                case LOW -> Color.valueOf("#3b82f6");
                case NONE -> Color.TRANSPARENT;
            });

            // Checkbox
            completedBox.setSelected(item.isCompleted());
            completedBox.setOnAction(event -> {
                todoService.toggleCompleted(item);
                getListView().refresh();
            });

            // Text with CSS classes
            textLabel.setText(item.getText());
            textLabel.getStyleClass().removeAll("task-text", "task-text-completed");
            textLabel.getStyleClass().add(item.isCompleted() ? "task-text-completed" : "task-text");

            // Due date
            LocalDate due = item.getDueDate();
            if (due != null) {
                dateLabel.setText(due.format(DATE_FMT));
                boolean overdue = !item.isCompleted() && due.isBefore(LocalDate.now());
                dateLabel.getStyleClass().removeAll("date-badge", "date-badge-overdue");
                dateLabel.getStyleClass().add(overdue ? "date-badge-overdue" : "date-badge");
                dateLabel.setVisible(true);
                dateLabel.setManaged(true);
            } else {
                dateLabel.setVisible(false);
                dateLabel.setManaged(false);
            }

            // Delete
            deleteButton.setOnAction(event -> todoService.deleteTask(item));

            // Right-click context menu
            MenuItem timerItem = new MenuItem("Start 25-minute timer");
            timerItem.setOnAction(e -> onStartTimer.accept(item));
            ContextMenu menu = new ContextMenu(timerItem);
            row.setOnContextMenuRequested(e ->
                    menu.show(row, e.getScreenX(), e.getScreenY()));

            setText(null);
            setGraphic(row);
        }
    }

    // -----------------------------------------------------------------------
    // Priority combo rendering
    // -----------------------------------------------------------------------

    private static class PriorityListCell extends ListCell<TodoItem.Priority> {
        @Override
        protected void updateItem(TodoItem.Priority item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label l = new Label(switch (item) {
                case HIGH -> "● High";
                case MEDIUM -> "● Medium";
                case LOW -> "● Low";
                case NONE -> "None";
            });
            l.getStyleClass().add(switch (item) {
                case HIGH -> "priority-high";
                case MEDIUM -> "priority-medium";
                case LOW -> "priority-low";
                case NONE -> "priority-none";
            });
            setText(null);
            setGraphic(l);
        }
    }
}
