package com.example.todoflipclock.storage;

import com.example.todoflipclock.model.TodoItem;
import com.example.todoflipclock.model.TodoItem.Priority;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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

    private Optional<TodoItem> parseLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 2) {
            return Optional.empty();
        }

        try {
            if (parts.length == 2) {
                // Old format: completed|Base64(text)
                boolean completed = Boolean.parseBoolean(parts[0]);
                String text = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                return Optional.of(new TodoItem(text, completed, Priority.NONE, null));
            }

            // New format: completed|priority|dueDate|Base64(text)
            boolean completed = Boolean.parseBoolean(parts[0]);
            Priority priority = parsePriority(parts[1]);
            LocalDate dueDate = parseDueDate(parts[2]);
            String text = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
            return Optional.of(new TodoItem(text, completed, priority, dueDate));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String formatLine(TodoItem item) {
        String encodedText = Base64.getEncoder()
                .encodeToString(item.getText().getBytes(StandardCharsets.UTF_8));
        String dueDateStr = item.getDueDate() != null ? item.getDueDate().toString() : "none";
        return item.isCompleted() + "|" + item.getPriority().name() + "|" + dueDateStr + "|" + encodedText;
    }

    private static Priority parsePriority(String s) {
        try {
            return Priority.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Priority.NONE;
        }
    }

    private static LocalDate parseDueDate(String s) {
        if (s == null || s.isBlank() || "none".equals(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
