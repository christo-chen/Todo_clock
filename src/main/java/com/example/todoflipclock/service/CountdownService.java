package com.example.todoflipclock.service;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Duration;

import java.awt.Toolkit;

public class CountdownService {

    private final ReadOnlyStringWrapper remainingTime = new ReadOnlyStringWrapper("00:00");
    private final ReadOnlyBooleanWrapper isRunning = new ReadOnlyBooleanWrapper(false);
    private final StringProperty taskName = new SimpleStringProperty("");

    private Timeline timeline;
    private int totalSeconds;
    private int remainingSeconds;
    private Runnable onFinished;

    public CountdownService() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void setDuration(int minutes) {
        if (isRunning.get()) return;
        totalSeconds = Math.max(1, minutes) * 60;
        remainingSeconds = totalSeconds;
        updateDisplay();
    }

    public void start() {
        if (remainingSeconds <= 0) return;
        isRunning.set(true);
        timeline.play();
    }

    public void pause() {
        isRunning.set(false);
        timeline.stop();
    }

    public void reset() {
        timeline.stop();
        isRunning.set(false);
        remainingSeconds = totalSeconds;
        updateDisplay();
    }

    public void stop() {
        timeline.stop();
        isRunning.set(false);
        remainingSeconds = 0;
        totalSeconds = 0;
        updateDisplay();
        taskName.set("");
    }

    public ReadOnlyStringProperty remainingTimeProperty() {
        return remainingTime.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty isRunningProperty() {
        return isRunning.getReadOnlyProperty();
    }

    public StringProperty taskNameProperty() {
        return taskName;
    }

    public void setOnFinished(Runnable callback) {
        this.onFinished = callback;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    private void tick() {
        remainingSeconds--;
        updateDisplay();

        if (remainingSeconds <= 0) {
            timeline.stop();
            isRunning.set(false);
            Toolkit.getDefaultToolkit().beep();
            if (onFinished != null) {
                onFinished.run();
            }
        }
    }

    private void updateDisplay() {
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        remainingTime.set(String.format("%02d:%02d", m, s));
    }

    /** Exposed so tests can set a very short remaining time. */
    void setRemainingSeconds(int seconds) {
        remainingSeconds = seconds;
        updateDisplay();
    }
}
