package com.example.todoflipclock.controller;

import com.example.todoflipclock.service.CountdownService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CountdownPane extends VBox {

    private static final String CARD_STYLE = """
        -fx-background-color: linear-gradient(to bottom, #111827 0%, #111827 49%, #0b1220 50%, #0b1220 100%);
        -fx-text-fill: #f9fafb;
        -fx-font-size: 52px;
        -fx-font-family: 'Menlo', 'Consolas', monospace;
        -fx-font-weight: bold;
        -fx-background-radius: 8;
        -fx-min-width: 110;
        -fx-min-height: 100;
        -fx-alignment: center;
        """;

    private static final int[] PRESETS = {5, 15, 25, 45, 60};

    private final CountdownService service;
    private final Label taskLabel = new Label();
    private final Label minuteLabel = new Label("00");
    private final Label secondLabel = new Label("00");
    private final Button startBtn = new Button("Start");
    private final Button pauseBtn = new Button("Pause");
    private final Button resetBtn = new Button("Reset");
    private final TextField customInput = new TextField();
    private final ToggleGroup presetGroup = new ToggleGroup();

    public CountdownPane(CountdownService service) {
        this.service = service;
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(0));
        buildUi();
        bindToService();
    }

    public void startForTask(String taskName, int minutes) {
        service.stop();
        service.taskNameProperty().set(taskName);
        service.setDuration(minutes);
        service.start();
    }

    private void buildUi() {
        // Task name label
        taskLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #6b7280; -fx-font-weight: bold;");
        taskLabel.setVisible(false);
        taskLabel.setManaged(false);

        // Time display
        minuteLabel.setStyle(CARD_STYLE);
        secondLabel.setStyle(CARD_STYLE);

        Label colon = new Label(":");
        colon.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        HBox timeRow = new HBox(12, minuteLabel, colon, secondLabel);
        timeRow.setAlignment(Pos.CENTER);

        // Preset buttons
        HBox presetRow = new HBox(8);
        presetRow.setAlignment(Pos.CENTER);

        for (int mins : PRESETS) {
            ToggleButton btn = new ToggleButton(mins + "m");
            btn.setToggleGroup(presetGroup);
            btn.setPrefHeight(32);
            btn.setStyle("-fx-font-size: 13px;");
            btn.setUserData(mins);
            btn.setOnAction(e -> {
                service.setDuration(mins);
                customInput.clear();
            });
            presetRow.getChildren().add(btn);
        }

        customInput.setPromptText("min");
        customInput.setPrefWidth(50);
        customInput.setPrefHeight(32);
        customInput.setStyle("-fx-font-size: 13px;");
        customInput.setOnAction(e -> {
            try {
                int mins = Integer.parseInt(customInput.getText().trim());
                if (mins > 0 && mins <= 999) {
                    service.setDuration(mins);
                    presetGroup.selectToggle(null);
                }
            } catch (NumberFormatException ignored) {
            }
        });
        presetRow.getChildren().add(customInput);

        // Control buttons
        startBtn.setPrefHeight(36);
        startBtn.setPrefWidth(80);
        startBtn.setDefaultButton(true);
        startBtn.setOnAction(e -> {
            // If no duration set and custom input has a value, parse it
            if (!service.isRunning()) {
                try {
                    String txt = customInput.getText().trim();
                    if (!txt.isEmpty()) {
                        int mins = Integer.parseInt(txt);
                        if (mins > 0 && mins <= 999) {
                            service.setDuration(mins);
                            presetGroup.selectToggle(null);
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            service.start();
        });

        pauseBtn.setPrefHeight(36);
        pauseBtn.setPrefWidth(80);
        pauseBtn.setOnAction(e -> service.pause());

        resetBtn.setPrefHeight(36);
        resetBtn.setPrefWidth(80);
        resetBtn.setOnAction(e -> {
            service.reset();
            customInput.clear();
        });

        HBox controlRow = new HBox(10, startBtn, pauseBtn, resetBtn);
        controlRow.setAlignment(Pos.CENTER);

        getChildren().addAll(taskLabel, timeRow, presetRow, controlRow);
    }

    private void bindToService() {
        service.remainingTimeProperty().addListener((obs, oldVal, newVal) -> {
            String[] parts = newVal.split(":");
            minuteLabel.setText(parts[0]);
            secondLabel.setText(parts[1]);
        });

        service.isRunningProperty().addListener((obs, oldVal, running) -> {
            startBtn.setDisable(running);
            pauseBtn.setDisable(!running);
        });

        service.taskNameProperty().addListener((obs, oldVal, newVal) -> {
            boolean has = newVal != null && !newVal.isEmpty();
            taskLabel.setText(has ? "计时: " + newVal : "");
            taskLabel.setVisible(has);
            taskLabel.setManaged(has);
        });

        // Initial state
        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
    }
}
