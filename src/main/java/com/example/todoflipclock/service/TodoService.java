package com.example.todoflipclock.service;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.storage.TodoStorage;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

public class TodoService {
    private final TodoStorage storage;
    private final ObservableList<TodoItem> items;

    public TodoService(TodoStorage storage) {
        this.storage = storage;
        this.items = FXCollections.observableArrayList(storage.load());

        // Persist whenever the list changes. Completion changes are saved by toggleCompleted.
        this.items.addListener((ListChangeListener<TodoItem>) change -> save());
    }

    public ObservableList<TodoItem> getItems() {
        return items;
    }

    public void addTask(String text) {
        String trimmed = text.trim();
        if (!trimmed.isEmpty()) {
            items.add(new TodoItem(trimmed, false));
        }
    }

    public void toggleCompleted(TodoItem item) {
        item.setCompleted(!item.isCompleted());
        save();
    }

    public void deleteTask(TodoItem item) {
        items.remove(item);
    }

    private void save() {
        storage.save(items);
    }
}
