/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.model.QCustomer;

/**
 * FXML Controller class.
 *
 * @author Evgeniy Egorov
 */
public class CustomerController extends FxController {

    @FXML()
    private VBox area;

    @FXML()
    private Label numberLbl;

    @FXML()
    private Label serviceLbl;

    @FXML()
    private Label customerLbl;

    @FXML()
    private ImageView logo;

    private RpcGetSelfSituation.SelfService selfService;
    private RpcGetSelfSituation.CustomerShort custData;
    private QCustomer customer;

    public QCustomer getCustomer() {
        return customer;
    }

    public RpcGetSelfSituation.SelfService getSelfService() {
        return selfService;
    }

    public RpcGetSelfSituation.CustomerShort getCustData() {
        return custData;
    }

    private DropShadow effectRed;
    private DropShadow effectGreen;

    public DropShadow getEffectRed() {
        return effectRed;
    }

    public DropShadow getEffectGreen() {
        return effectGreen;
    }

    /**
     * Initializes the controller class.
     *
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        effectRed = new DropShadow(30, Color.MAGENTA);
        effectRed.setInput(new Reflection(1, .2, .4, .1));

        effectGreen = new DropShadow(30, Color.GREENYELLOW);
        effectGreen.setInput(new Reflection(1, .2, .4, .1));
    }

    void init(RpcGetSelfSituation.SelfService selfService, RpcGetSelfSituation.CustomerShort custData) {
        this.selfService = selfService;
        this.custData = custData;
        numberLbl.setText(custData.number);
        customerLbl.setText(custData.data);
        serviceLbl.setText(selfService.getServiceName());

        final File logoFile = new File("config/desktop/" + selfService.getId() + ".png");
        if (logoFile.exists()) {
            try (FileInputStream is = new FileInputStream(logoFile)) {
                logo.setImage(new Image(is));
            } catch (IOException ex) {
                QLog.l().logger().warn(ex);
            }
        }
    }

    void init(QCustomer customer) {
        this.customer = customer;
        init(new RpcGetSelfSituation.SelfService(customer.getService(), 0, 0, false, false),
            new RpcGetSelfSituation.CustomerShort(customer.getId(), customer.getFullNumber(), customer.getInputData(), customer.getWaitingMinutes()));
    }

    /**
     * Событие.
     */
    public static class OnClickWaitedHandler implements EventHandler<ActionEvent> {

        protected final DesktopController desktopController;
        private final RpcGetSelfSituation.SelfService selfService;
        private final RpcGetSelfSituation.CustomerShort custData;
        protected final QCustomer customer;

        /**
         * Конструктор события.
         */
        public OnClickWaitedHandler(DesktopController desktopController, RpcGetSelfSituation.SelfService selfService, RpcGetSelfSituation.CustomerShort custData) {
            this.desktopController = desktopController;
            this.selfService = selfService;
            this.custData = custData;
            this.customer = null;
        }

        /**
         * Конструктор события.
         */
        public OnClickWaitedHandler(DesktopController desktopController, QCustomer customer) {
            this.desktopController = desktopController;
            this.customer = customer;
            this.selfService = new RpcGetSelfSituation.SelfService(customer.getService(), 0, 0, false, false);
            this.custData = new RpcGetSelfSituation.CustomerShort(customer.getId(), customer.getFullNumber(), customer.getInputData(), customer.getWaitingMinutes());
        }

        @Override
        public void handle(ActionEvent event) {
            final Stage firstDlgStage = new Stage(QConfig.cfg().isDebug() ? StageStyle.UTILITY : StageStyle.UNDECORATED);
            Group firstDglRootGroup = new Group();
            Scene scene = new Scene(firstDglRootGroup);
            firstDlgStage.setTitle("SelectFirstDlg");
            firstDlgStage.centerOnScreen();
            firstDlgStage.setAlwaysOnTop(true);

            final ControllerAndView<SelectFirstDlgController, Parent> selectFirstCav = new ControllerAndView<>("SelectFirstDlg.fxml", firstDlgStage, scene);
            selectFirstCav.getController().init(selfService, custData);
            selectFirstCav.getController().init(desktopController.getNetProperty(), desktopController);

            final ControllerAndView<CustomerController, Parent> customerCav = new ControllerAndView<>("Customer.fxml", firstDlgStage);
            customerCav.getController().init(selfService, custData);
            selectFirstCav.getController().getClientPane().getChildren().add(customerCav.getView());

            firstDglRootGroup.getChildren().add(selectFirstCav.getView());

            firstDlgStage.initOwner(desktopController.getStage());
            firstDlgStage.initModality(Modality.APPLICATION_MODAL);
            firstDlgStage.show();
            selectFirstCav.getController().showWithEffect(event);
        }
    }

    public static class OnClickPostponedHandler extends OnClickWaitedHandler {

        public OnClickPostponedHandler(DesktopController desktopController, QCustomer customer) {
            super(desktopController, customer);
        }

        @Override
        public void handle(ActionEvent event) {
            final Stage firstDlgStage = new Stage(QConfig.cfg().isDebug() ? StageStyle.UTILITY : StageStyle.UNDECORATED);
            Group firstDglRootGroup = new Group();
            Scene scene = new Scene(firstDglRootGroup);
            firstDlgStage.setTitle("PostponeDlg");
            firstDlgStage.centerOnScreen();
            firstDlgStage.setAlwaysOnTop(true);

            final ControllerAndView<PostponeDlgController, Parent> postponeCav = new ControllerAndView<>("PostponeDlg.fxml", firstDlgStage, scene);
            postponeCav.getController().setCustomer(customer);
            postponeCav.getController().init(desktopController.getNetProperty(), desktopController);

            final ControllerAndView<CustomerController, Parent> customerCav = new ControllerAndView<>("Customer.fxml", firstDlgStage);
            customerCav.getController().init(customer);
            postponeCav.getController().getClientPane().getChildren().add(customerCav.getView());

            firstDglRootGroup.getChildren().add(postponeCav.getView());

            firstDlgStage.initOwner(desktopController.getStage());
            firstDlgStage.initModality(Modality.APPLICATION_MODAL);
            firstDlgStage.show();
            postponeCav.getController().showWithEffect(event);
        }

    }

}
