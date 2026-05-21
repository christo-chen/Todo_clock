package com.example.todoflipclock.controller;

import com.example.todoflipclock.service.CountdownService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CountdownPane extends VBox {

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
        taskLabel.getStyleClass().add("countdown-task");
        taskLabel.setVisible(false);
        taskLabel.setManaged(false);

        // Time display
        minuteLabel.getStyleClass().add("countdown-card");
        secondLabel.getStyleClass().add("countdown-card");

        Label colon = new Label(":");
        colon.getStyleClass().add("clock-colon");

        HBox timeRow = new HBox(12, minuteLabel, colon, secondLabel);
        timeRow.setAlignment(Pos.CENTER);

        // Preset buttons
        HBox presetRow = new HBox(8);
        presetRow.setAlignment(Pos.CENTER);

        for (int mins : PRESETS) {
            ToggleButton btn = new ToggleButton(mins + "m");
            btn.setToggleGroup(presetGroup);
            btn.getStyleClass().add("preset-btn");
            btn.setUserData(mins);
            btn.setOnAction(e -> {
                service.setDuration(mins);
                customInput.clear();
            });
            presetRow.getChildren().add(btn);
        }

        customInput.setPromptText("min");
        customInput.getStyleClass().add("preset-input");
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
        startBtn.getStyleClass().add("control-btn");
        startBtn.setDefaultButton(true);
        startBtn.setOnAction(e -> {
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

        pauseBtn.getStyleClass().add("control-btn");
        pauseBtn.setOnAction(e -> service.pause());

        resetBtn.getStyleClass().add("control-btn");
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
            taskLabel.setText(has ? "Timer: " + newVal : "");
            taskLabel.setVisible(has);
            taskLabel.setManaged(has);
        });

        startBtn.setDisable(false);
        pauseBtn.setDisable(true);
    }
}
