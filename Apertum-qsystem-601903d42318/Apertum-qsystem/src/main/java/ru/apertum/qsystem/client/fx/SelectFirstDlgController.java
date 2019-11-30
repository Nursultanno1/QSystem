/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.SoundPlayer;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.model.QCustomer;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class.
 *
 * @author Evgeniy Egorov
 */
public class SelectFirstDlgController extends UserController {

    @FXML()
    private AnchorPane clientPane;

    public AnchorPane getClientPane() {
        return clientPane;
    }

    @FXML()
    private Button inviteBtn;

    @FXML()
    private Button closeBtn;

    @FXML()
    private Button pickUpBtn;

    @FXML()
    private WebView info;

    private RpcGetSelfSituation.SelfService selfService;
    private RpcGetSelfSituation.CustomerShort custData;

    public RpcGetSelfSituation.SelfService getSelfService() {
        return selfService;
    }

    public RpcGetSelfSituation.CustomerShort getCustData() {
        return custData;
    }

    void init(RpcGetSelfSituation.SelfService selfService, RpcGetSelfSituation.CustomerShort custData) {
        this.selfService = selfService;
        this.custData = custData;

        loadHtmlToWebView("config/desktop/" + selfService.getId() + ".html", info);
    }

    /**
     * Initializes the controller class.
     *
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        QLog.l().logger().info("SelectFirstDlg was initialized.");
    }

    /**
     * Customer going to be invited.
     *
     * @param event событие FX.
     */
    @FXML()
    public void invite(ActionEvent event) {
        // Вызываем кастомера
        final QCustomer cust = NetCommander.inviteNextCustomer(getNetProperty(), getUser().getId(), null, custData.id);
        // проговорим голосом вызов с компа оператора если есть настроичка
        if (cust != null && QConfig.cfg().needVoice()) {
            final boolean isFirst = true;
            new Thread(()
                    -> SoundPlayer.inviteClient(cust.getService(), cust, cust.getPrefix() + (cust.getNumber() < 1 ? "" : cust.getNumber()), getUser().getPoint(), isFirst)
            ).start();
        }
        if (cust == null) {
            close(event);
        } else {
            close(event);
            inviteHandle(getStage().getOwner(), cust);
        }
    }

    @FXML()
    public void close(ActionEvent event) {
        closeWithEffect(event);
    }

    /**
     * Customer going to be selected.
     *
     * @param event событие FX.
     */
    @FXML()
    public void pickUp(ActionEvent event) {
        try {
            NetCommander.customerToPostpone(getNetProperty(), getUser().getId(), custData.id, "Picked Up", 0, true, false);
        } catch (QException e) {
            throw new ClientException(e);
        }
        getDesktopController().refreshSituation();
        closeWithEffect(event);
    }
}
