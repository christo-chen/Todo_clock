package com.example.todoflipclock.storage;

import com.example.todoflipclock.model.TodoItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class TodoStorage {
    private final Path storageFile;

    public TodoStorage() {
        this(Path.of(System.getProperty("user.home"), ".todo-flip-clock", "tasks.txt"));
    }

    public TodoStorage(Path storageFile) {
        this.storageFile = storageFile;
    }

    public List<TodoItem> load() {
        if (!Files.exists(storageFile)) {
            return new ArrayList<>();
        }

        try {
            List<TodoItem> items = new ArrayList<>();
            for (String line : Files.readAllLines(storageFile, StandardCharsets.UTF_8)) {
                parseLine(line).ifPresent(items::add);
            }
            return items;
        } catch (IOException ex) {
            System.err.println("Could not load tasks: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<TodoItem> items) {
        try {
            Files.createDirectories(storageFile.getParent());

            List<String> lines = items.stream()
                    .map(this::formatLine)
                    .toList();

            Files.write(storageFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Could not save tasks: " + ex.getMessage());
        }
    }

    private java.util.Optional<TodoItem> parseLine(String line) {
        String[] parts = line.split("\\|", 2);
        if (parts.length != 2) {
            return java.util.Optional.empty();
        }

        try {
            boolean completed = Boolean.parseBoolean(parts[0]);
            String text = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return java.util.Optional.of(new TodoItem(text, completed));
        } catch (IllegalArgumentException ex) {
            return java.util.Optional.empty();
        }
    }

    private String formatLine(TodoItem item) {
        String encodedText = Base64.getEncoder()
                .encodeToString(item.getText().getBytes(StandardCharsets.UTF_8));
        return item.isCompleted() + "|" + encodedText;
    }
}
