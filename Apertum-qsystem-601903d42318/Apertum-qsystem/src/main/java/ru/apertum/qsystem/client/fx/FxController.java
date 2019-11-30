/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.security.SecureRandom;

/**
 * Такой базовый контроллер для FX форм.
 *
 * @author Evgeniy Egorov
 */
public abstract class FxController implements Initializable {

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    protected void closeWithEffect(ActionEvent event) {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(1);
        timeline.setAutoReverse(false);
        final KeyValue kv = new SecureRandom().nextFloat() > .5
            ? new KeyValue(new WritableValue<Double>() {
            @Override
            public Double getValue() {
                return getStage().getY();
            }

            @Override
            public void setValue(Double value) {
                getStage().setY(value);
            }
        }, getStage().getY() + 1000)
            : new KeyValue(new WritableValue<Double>() {
            @Override
            public Double getValue() {
                return getStage().getX();
            }

            @Override
            public void setValue(Double value) {
                getStage().setX(value);
            }
        }, getStage().getX() + 1000);
        final KeyFrame kf = new KeyFrame(Duration.millis(200), kv);

        timeline.getKeyFrames().add(kf);
        timeline.setOnFinished(evt -> getStage().close());
        timeline.play();
    }

    protected void showWithEffect(ActionEvent event) {
        final Timeline timeline = new Timeline();
        timeline.setCycleCount(1);
        timeline.setAutoReverse(false);
        final KeyValue kv = new SecureRandom().nextFloat() > 0.5
            ? new KeyValue(new WritableValue<Double>() {
            @Override
            public Double getValue() {
                return stage.getY() - 1000;
            }

            @Override
            public void setValue(Double value) {
                stage.setY(value);
            }
        }, stage.getY())
            : new KeyValue(new WritableValue<Double>() {
            @Override
            public Double getValue() {
                return stage.getX() - 1000;
            }

            @Override
            public void setValue(Double value) {
                stage.setX(value);
            }
        }, stage.getX());
        final KeyFrame kf = new KeyFrame(Duration.millis(200), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

}
