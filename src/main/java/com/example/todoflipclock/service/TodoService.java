package com.example.todoflipclock.service;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.model.TodoItem.Priority;
import com.example.todoflipclock.storage.TodoStorage;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.time.LocalDate;

public class TodoService {
    private final TodoStorage storage;
    private final ObservableList<TodoItem> items;

    public TodoService(TodoStorage storage) {
        this.storage = storage;
        this.items = FXCollections.observableArrayList(storage.load());

        this.items.addListener((ListChangeListener<TodoItem>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved() || change.wasReplaced()) {
                    save();
                }
            }
        });
    }

    public ObservableList<TodoItem> getItems() {
        return items;
    }

    public void addTask(String text, Priority priority, LocalDate dueDate) {
        String trimmed = text.trim();
        if (!trimmed.isEmpty()) {
            items.add(new TodoItem(trimmed, false, priority, dueDate));
        }
    }

    public void toggleCompleted(TodoItem item) {
        item.setCompleted(!item.isCompleted());
        save();
    }

    public void deleteTask(TodoItem item) {
        items.remove(item);
    }

    public void moveTask(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= items.size() || toIndex < 0 || toIndex >= items.size()) {
            return;
        }
        TodoItem item = items.remove(fromIndex);
        items.add(toIndex, item);
    }

    public void save() {
        storage.save(items);
    }
}
