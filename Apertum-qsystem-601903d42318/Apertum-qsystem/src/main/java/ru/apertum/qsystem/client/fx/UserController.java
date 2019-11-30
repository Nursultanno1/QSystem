/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.model.IClientNetProperty;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.model.QUser;

import java.io.File;
import java.io.IOException;

/**
 * Контроллер для пользователей.
 *
 * @author Evgeniy Egorov
 */
public abstract class UserController extends FxController {

    private QUser user;

    private IClientNetProperty netProperty;

    private DesktopController desktopController;

    public DesktopController getDesktopController() {
        return desktopController;
    }

    public QUser getUser() {
        return user;
    }

    public void setUser(QUser user) {
        this.user = user;
    }

    public IClientNetProperty getNetProperty() {
        return netProperty;
    }

    public void setNetProperty(IClientNetProperty netProperty) {
        this.netProperty = netProperty;
    }

    /**
     * Проставим данные в контроллер.
     */
    public void init(IClientNetProperty netProperty, DesktopController desktopController) {
        this.netProperty = netProperty;
        this.user = desktopController.getUser();
        this.desktopController = desktopController;
    }

    protected void inviteHandle(Window parentStage, QCustomer customer) {
        final Stage inviteStage = new Stage(QConfig.cfg().isDebug() ? StageStyle.UTILITY : StageStyle.UNDECORATED);
        Group rootGroup = new Group();
        Scene scene = new Scene(rootGroup);
        inviteStage.setTitle("InviteDlg");
        inviteStage.centerOnScreen();
        inviteStage.setAlwaysOnTop(true);

        final ControllerAndView<InviteDlgController, Parent> inviteCav = new ControllerAndView<>("InviteDlg.fxml", inviteStage, scene);
        inviteCav.getController().setCustomer(customer);
        inviteCav.getController().init(getNetProperty(), getDesktopController());

        final ControllerAndView<CustomerController, Parent> customerCav = new ControllerAndView<>("Customer.fxml", inviteStage, scene);
        customerCav.getController().init(customer);
        inviteCav.getController().getClientPane().getChildren().add(customerCav.getView());

        rootGroup.getChildren().add(inviteCav.getView());

        inviteStage.initOwner(parentStage);
        inviteStage.initModality(Modality.APPLICATION_MODAL);
        inviteStage.show();
        inviteCav.getController().showWithEffect(null);
    }

    protected void loadHtmlToWebView(String fileName, WebView webView) {
        File htmlFile = new File(fileName);
        if (htmlFile.exists()) {
            try {
                webView.getEngine().load(htmlFile.toURI().toURL().toString());
            } catch (IOException ex) {
                QLog.l().logger().warn(ex);
            }
        } else {
            htmlFile = new File("config/desktop/service.html");
            if (htmlFile.exists()) {
                try {
                    webView.getEngine().load(htmlFile.toURI().toURL().toString());
                } catch (IOException ex) {
                    QLog.l().logger().warn(ex);
                }
            } else {
                webView.getEngine().load(this.getClass().getResource("/ru/apertum/qsystem/client/fx/service.html").toString());
            }
        }
    }

}
