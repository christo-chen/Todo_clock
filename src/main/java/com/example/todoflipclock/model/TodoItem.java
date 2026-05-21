package com.example.todoflipclock.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TodoItem {
    private final StringProperty text = new SimpleStringProperty();
    private final BooleanProperty completed = new SimpleBooleanProperty();

    public TodoItem(String text, boolean completed) {
        this.text.set(text);
        this.completed.set(completed);
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
}
