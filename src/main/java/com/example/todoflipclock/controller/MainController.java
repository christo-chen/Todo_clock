package com.example.todoflipclock.controller;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.service.ClockService;
import com.example.todoflipclock.service.TodoService;
import com.example.todoflipclock.storage.TodoStorage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class MainController {
    private final ClockService clockService = new ClockService();
    private final TodoService todoService = new TodoService(new TodoStorage());
    private final BorderPane root = new BorderPane();

    public MainController() {
        buildUi();
    }

    public Parent getView() {
        return root;
    }

    private void buildUi() {
        root.setStyle("-fx-background-color: #f3f4f6;");
        root.setPadding(new Insets(24));
        root.setTop(new FlipClockPane(clockService));
        root.setCenter(createTodoView());

        Platform.runLater(() -> root.getScene().getWindow().setOnCloseRequest(event -> clockService.stop()));
    }

    private VBox createTodoView() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");

        Label title = new Label("Tasks");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        TextField input = new TextField();
        input.setPromptText("Add a task");
        input.setPrefHeight(40);

        Button addButton = new Button("Add");
        addButton.setPrefHeight(40);
        addButton.setDefaultButton(true);
        addButton.setOnAction(event -> {
            todoService.addTask(input.getText());
            input.clear();
        });

        input.setOnAction(addButton.getOnAction());

        HBox inputRow = new HBox(10, input, addButton);
        HBox.setHgrow(input, Priority.ALWAYS);

        ListView<TodoItem> listView = new ListView<>(todoService.getItems());
        listView.setPlaceholder(new Label("No tasks yet."));
        listView.setCellFactory(view -> new TodoItemCell(todoService));
        VBox.setVgrow(listView, Priority.ALWAYS);

        panel.getChildren().addAll(title, inputRow, listView);
        return panel;
    }

    private static class TodoItemCell extends ListCell<TodoItem> {
        private final TodoService todoService;

        private TodoItemCell(TodoService todoService) {
            this.todoService = todoService;
        }

        @Override
        protected void updateItem(TodoItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }

            CheckBox completedBox = new CheckBox();
            completedBox.setSelected(item.isCompleted());
            completedBox.setOnAction(event -> {
                todoService.toggleCompleted(item);
                getListView().refresh();
            });

            Label text = new Label(item.getText());
            text.setMaxWidth(Double.MAX_VALUE);
            text.setStyle(item.isCompleted()
                    ? "-fx-text-fill: #6b7280; -fx-strikethrough: true;"
                    : "-fx-text-fill: #111827;");

            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(event -> todoService.deleteTask(item));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, completedBox, text, spacer, deleteButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 2, 6, 2));

            setText(null);
            setGraphic(row);
        }
    }
}
