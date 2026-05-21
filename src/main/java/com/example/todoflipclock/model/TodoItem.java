package com.example.todoflipclock.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class TodoItem {

    public enum Priority {
        HIGH, MEDIUM, LOW, NONE
    }

    private final StringProperty text = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty();
    private final ObjectProperty<Priority> priority = new SimpleObjectProperty<>(Priority.NONE);
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>();

    public TodoItem(String text, boolean completed) {
        this(text, completed, Priority.NONE, null);
    }

    public TodoItem(String text, boolean completed, Priority priority, LocalDate dueDate) {
        this.text.set(text);
        this.completed.set(completed);
        this.priority.set(priority);
        this.dueDate.set(dueDate);
    }

    public String getText() {
        return text.get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public void setCompleted(boolean completed) {
        this.completed.set(completed);
    }

    public BooleanProperty completedProperty() {
        return completed;
    }

    public Priority getPriority() {
        return priority.get();
    }

    public void setPriority(Priority priority) {
        this.priority.set(priority);
    }

    public ObjectProperty<Priority> priorityProperty() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate.get();
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate.set(dueDate);
    }

    public ObjectProperty<LocalDate> dueDateProperty() {
        return dueDate;
    }
}
