package com.example.todoflipclock.controller;

import com.example.todoflipclock.service.ClockService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

public class FlipClockPane extends VBox {

    private static final int CARD_W = 130;
    private static final int CARD_H = 110;
    private static final int TOP_H = 54;
    private static final int BOT_Y = 55;
    private static final int BOT_H = 55;

    private static final String CARD_STYLE = """
        -fx-background-color: linear-gradient(to bottom, #111827 0%, #111827 49%, #0b1220 50%, #0b1220 100%);
        -fx-text-fill: #f9fafb;
        -fx-font-size: 52px;
        -fx-font-family: 'Menlo', 'Consolas', monospace;
        -fx-font-weight: bold;
        """;

    private final ClockService clockService;

    public FlipClockPane(ClockService clockService) {
        this.clockService = clockService;
        setAlignment(Pos.CENTER);
        buildUi();
        clockService.start();
    }

    private void buildUi() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER);
        row.setDepthTest(javafx.scene.DepthTest.ENABLE);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                PerspectiveCamera cam = new PerspectiveCamera();
                cam.setFieldOfView(35);
                newScene.setCamera(cam);
            }
        });

        FlipCard hourCard = new FlipCard();
        FlipCard minuteCard = new FlipCard();
        FlipCard secondCard = new FlipCard();

        clockService.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            String[] parts = newTime.split(":");
            hourCard.setValue(parts[0], oldTime != null);
            minuteCard.setValue(parts[1], oldTime != null);
            secondCard.setValue(parts[2], oldTime != null);
        });

        String[] init = clockService.currentTimeProperty().get().split(":");
        hourCard.setValue(init[0], false);
        minuteCard.setValue(init[1], false);
        secondCard.setValue(init[2], false);

        row.getChildren().addAll(hourCard, colon(), minuteCard, colon(), secondCard);

        Label dateLabel = new Label();
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        dateLabel.textProperty().bind(clockService.currentDateProperty());
        VBox.setMargin(dateLabel, new Insets(10, 0, 0, 0));

        getChildren().addAll(row, dateLabel);
    }

    private static Label colon() {
        Label l = new Label(":");
        l.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        return l;
    }

    // --------------------------------------------------------------------

    private static class FlipCard extends StackPane {

        private final Label topCurrent;
        private final Label bottomCurrent;
        private final Label topNext;
        private final Label bottomNext;

        private final Rotate topCurrentRot;
        private final Rotate bottomNextRot;

        private String value = "";
        private Timeline anim;
        private boolean animating;

        FlipCard() {
            setMinSize(CARD_W, CARD_H);
            setMaxSize(CARD_W, CARD_H);
            // Dark background shows through the 1px divider gap
            setStyle("-fx-background-color: #0b1220; -fx-background-radius: 8;");

            topCurrent = cardLabel(topClip());
            bottomCurrent = cardLabel(bottomClip());
            topNext = cardLabel(topClip());
            bottomNext = cardLabel(bottomClip());

            // Pivot at the horizontal center line so halves meet at the divider
            topCurrentRot = new Rotate(0, CARD_W / 2.0, TOP_H, 0, Rotate.X_AXIS);
            topCurrent.getTransforms().add(topCurrentRot);

            bottomNextRot = new Rotate(-90, CARD_W / 2.0, TOP_H, 0, Rotate.X_AXIS);
            bottomNext.getTransforms().add(bottomNextRot);

            // Z-order (back → front): next-top, current-bottom, next-bottom, current-top
            getChildren().addAll(topNext, bottomCurrent, bottomNext, topCurrent);
        }

        private static Rectangle topClip() {
            Rectangle r = new Rectangle(CARD_W, TOP_H);
            r.setArcWidth(8);
            r.setArcHeight(8);
            return r;
        }

        private static Rectangle bottomClip() {
            Rectangle r = new Rectangle(0, BOT_Y, CARD_W, BOT_H);
            r.setArcWidth(8);
            r.setArcHeight(8);
            return r;
        }

        private static Label cardLabel(Rectangle clip) {
            Label l = new Label("00");
            l.setMinSize(CARD_W, CARD_H);
            l.setMaxSize(CARD_W, CARD_H);
            l.setAlignment(Pos.CENTER);
            l.setStyle(CARD_STYLE);
            l.setClip(clip);
            return l;
        }

        void setValue(String newValue, boolean animate) {
            if (newValue.equals(value) || animating) return;

            if (!animate) {
                topCurrent.setText(newValue);
                bottomCurrent.setText(newValue);
                value = newValue;
                return;
            }

            animating = true;

            topNext.setText(newValue);
            bottomNext.setText(newValue);

            topCurrentRot.setAngle(0);
            bottomNextRot.setAngle(-90);

            KeyValue kvTop = new KeyValue(topCurrentRot.angleProperty(), 90, Interpolator.EASE_BOTH);
            KeyValue kvBot = new KeyValue(bottomNextRot.angleProperty(), 0, Interpolator.EASE_BOTH);
            KeyFrame kf = new KeyFrame(Duration.millis(300), kvTop, kvBot);

            anim = new Timeline(kf);
            anim.setOnFinished(e -> {
                topCurrent.setText(newValue);
                bottomCurrent.setText(newValue);
                topCurrentRot.setAngle(0);
                bottomNextRot.setAngle(-90);
                value = newValue;
                animating = false;
            });
            anim.play();
        }
    }
}
