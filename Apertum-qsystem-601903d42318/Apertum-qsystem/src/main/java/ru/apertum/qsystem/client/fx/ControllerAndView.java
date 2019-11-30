package ru.apertum.qsystem.client.fx;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.common.exceptions.ClientException;

public final class ControllerAndView<C extends FxController, V extends Parent> {

    private static final ResourceBundle LOCALISATION = ResourceBundle.getBundle("ru.apertum.qsystem.client.fx.i18n", Locales.getInstance().getLangCurrent());

    public String txt(String key) {
        return LOCALISATION.getString(key);
    }

    private final V view;
    private final C controller;

    public ControllerAndView(C controller, V view) {
        this.controller = controller;
        this.view = view;
    }

    /**
     * Загрузим GUI из файла и определим для него контроллер. Сцену положим на Форму stage.
     *
     * @param fxml  Это fxml файл для загрузки GUI.
     * @param stage Это форма, которая будет достапна к контроллере.
     * @param scene Сцена присваивается на форму. Сцена уже может быть сложной и содержать много контролов или группу.
     *              Если scrnr == null, то создадим сцену, ей сразу передадим view. И все это положим на форму stage.
     */
    public ControllerAndView(String fxml, Stage stage, Scene scene) {
        this(fxml, stage);
        if (scene == null) {
            stage.setScene(new Scene(getView()));
        } else {
            stage.setScene(scene);
        }
    }

    /**
     * Загрузим GUI из файла и определим для него контроллер.
     *
     * @param fxml  Это fxml файл для загрузки GUI.
     * @param stage Это форма, которая будет достапна к контроллере.
     */
    public ControllerAndView(String fxml, Stage stage) {
        final FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml), LOCALISATION);
        try {
            this.view = (V) loader.load();
        } catch (IOException ex) {
            throw new ClientException(ex);
        }
        this.controller = (C) loader.getController();
        controller.setStage(stage);
    }

    public V getView() {
        return view;
    }

    public C getController() {
        return controller;
    }
}
