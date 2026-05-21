package com.example.todoflipclock.controller;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.service.ClockService;
import com.example.todoflipclock.service.CountdownService;
import com.example.todoflipclock.service.TodoService;
import com.example.todoflipclock.storage.TodoStorage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
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

    // Top area — toggle between clock and countdown
    private final FlipClockPane flipClockPane;
    private final CountdownPane countdownPane;
    private final ToggleButton clockTab = new ToggleButton("Clock");
    private final ToggleButton countdownTab = new ToggleButton("Countdown");

    // Input row state
    private final ComboBox<TodoItem.Priority> priorityCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();

    public MainController() {
        flipClockPane = new FlipClockPane(clockService);
        countdownPane = new CountdownPane(countdownService);
        buildUi();
    }

    public Parent getView() {
        return root;
    }

    private void buildUi() {
        root.setStyle("-fx-background-color: #f3f4f6;");
        root.setPadding(new Insets(24));
        root.setTop(buildTopArea());
        root.setCenter(createTodoView());

        Platform.runLater(() -> root.getScene().getWindow().setOnCloseRequest(event -> {
            clockService.stop();
            countdownService.stop();
        }));
    }

    // -----------------------------------------------------------------------
    // Top area — toggle bar + display stack
    // -----------------------------------------------------------------------

    private VBox buildTopArea() {
        ToggleGroup tg = new ToggleGroup();
        clockTab.setToggleGroup(tg);
        countdownTab.setToggleGroup(tg);
        clockTab.setSelected(true);

        String tabBase = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 6 18;"
                + " -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 0;";
        String tabActive = tabBase
                + "-fx-background-color: #111827; -fx-text-fill: white;";
        String tabInactive = tabBase
                + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280;";

        clockTab.setStyle(tabActive);
        countdownTab.setStyle(tabInactive);

        clockTab.setOnAction(e -> {
            if (clockTab.isSelected()) {
                showClock();
                clockTab.setStyle(tabActive);
                countdownTab.setStyle(tabInactive);
            }
        });
        countdownTab.setOnAction(e -> {
            if (countdownTab.isSelected()) {
                showCountdown();
                countdownTab.setStyle(tabActive);
                clockTab.setStyle(tabInactive);
            }
        });

        HBox toggleBar = new HBox(8, clockTab, countdownTab);
        toggleBar.setAlignment(Pos.CENTER);
        toggleBar.setPadding(new Insets(0, 0, 16, 0));

        StackPane displayStack = new StackPane(flipClockPane, countdownPane);
        showClock(); // initial state

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
        String tabActive = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 6 18;"
                + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 0;"
                + "-fx-background-color: #111827; -fx-text-fill: white;";
        String tabInactive = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 6 18;"
                + "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-width: 0;"
                + "-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280;";
        countdownTab.setStyle(tabActive);
        clockTab.setStyle(tabInactive);
    }

    // -----------------------------------------------------------------------
    // Todo panel
    // -----------------------------------------------------------------------

    private VBox createTodoView() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");

        Label title = new Label("Tasks");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        HBox inputRow = createInputRow();

        ListView<TodoItem> listView = new ListView<>(todoService.getItems());
        listView.setPlaceholder(new Label("No tasks yet."));
        listView.setCellFactory(view -> new TodoItemCell(todoService, listView, this::onStartTaskTimer));
        VBox.setVgrow(listView, Priority.ALWAYS);

        panel.getChildren().addAll(title, inputRow, listView);
        return panel;
    }

    // -----------------------------------------------------------------------
    // Input row
    // -----------------------------------------------------------------------

    private HBox createInputRow() {
        TextField input = new TextField();
        input.setPromptText("Add a task");
        input.setPrefHeight(40);
        HBox.setHgrow(input, Priority.ALWAYS);

        priorityCombo.getItems().setAll(TodoItem.Priority.values());
        priorityCombo.setValue(TodoItem.Priority.NONE);
        priorityCombo.setPrefWidth(95);
        priorityCombo.setPrefHeight(40);
        priorityCombo.setButtonCell(new PriorityListCell());
        priorityCombo.setCellFactory(p -> new PriorityListCell());

        datePicker.setPrefWidth(130);
        datePicker.setPrefHeight(40);
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
        addButton.setPrefHeight(40);
        addButton.setDefaultButton(true);
        addButton.setOnAction(e -> commitAdd(input));
        input.setOnAction(e -> commitAdd(input));

        return new HBox(8, input, priorityCombo, datePicker, addButton);
    }

    private void commitAdd(TextField input) {
        String text = input.getText();
        if (text.trim().isEmpty()) return;
        todoService.addTask(text, priorityCombo.getValue(), datePicker.getValue());
        input.clear();
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

            editField.setPrefHeight(30);
            editField.setVisible(false);
            editField.setManaged(false);
            HBox.setHgrow(editField, Priority.ALWAYS);

            dateLabel.setStyle("-fx-font-size: 11px; -fx-padding: 2 6; -fx-background-radius: 4;");

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

            // Text
            textLabel.setText(item.getText());
            textLabel.setStyle(item.isCompleted()
                    ? "-fx-text-fill: #6b7280; -fx-strikethrough: true;"
                    : "-fx-text-fill: #111827;");

            // Due date
            LocalDate due = item.getDueDate();
            if (due != null) {
                dateLabel.setText(due.format(DATE_FMT));
                boolean overdue = !item.isCompleted() && due.isBefore(LocalDate.now());
                dateLabel.setStyle(overdue
                        ? "-fx-font-size: 11px; -fx-padding: 2 6; -fx-background-radius: 4;"
                          + " -fx-background-color: #fef2f2; -fx-text-fill: #dc2626; -fx-font-weight: bold;"
                        : "-fx-font-size: 11px; -fx-padding: 2 6; -fx-background-radius: 4;"
                          + " -fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;");
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
            l.setStyle("-fx-text-fill: " + switch (item) {
                case HIGH -> "#ef4444";
                case MEDIUM -> "#f59e0b";
                case LOW -> "#3b82f6";
                case NONE -> "#6b7280";
            } + "; -fx-font-weight: bold; -fx-font-size: 13px;");
            setText(null);
            setGraphic(l);
        }
    }
}
