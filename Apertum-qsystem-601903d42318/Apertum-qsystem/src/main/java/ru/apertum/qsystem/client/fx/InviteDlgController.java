/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.SoundPlayer;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.model.QCustomer;

import java.net.URL;
import java.util.ResourceBundle;

import static ru.apertum.qsystem.common.CustomerState.STATE_INVITED;
import static ru.apertum.qsystem.common.CustomerState.STATE_INVITED_SECONDARY;
import static ru.apertum.qsystem.common.CustomerState.STATE_WORK;
import static ru.apertum.qsystem.common.CustomerState.STATE_WORK_SECONDARY;

/**
 * FXML Controller class.
 *
 * @author Evgeniy Egorov
 */
public class InviteDlgController extends UserController {

    @FXML()
    private AnchorPane clientPane;

    public AnchorPane getClientPane() {
        return clientPane;
    }

    private QCustomer customer;

    public void setCustomer(QCustomer customer) {
        this.customer = customer;
    }

    public QCustomer getCustomer() {
        return customer;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        QLog.l().logger().info("InviteDlg was initialized.");
    }

    private long lastInvite = 0;

    /**
     * Customer going to be invited.
     *
     * @param event событие FX.
     */
    @FXML()
    public void invite(ActionEvent event) {
        if (System.currentTimeMillis() - lastInvite < 10_000) {
            return;
        }
        lastInvite = System.currentTimeMillis();
        if (customer.getState() != STATE_INVITED && customer.getState() != STATE_INVITED_SECONDARY) {
            QLog.l().logger().error("Customer \"" + customer + "\" have strange state = " + customer.getState().name());
        }

        final QCustomer cust;
        if (customer.getState() == STATE_WORK || customer.getState() == STATE_WORK_SECONDARY) {
            // у нас кастомер уже в работе, его только "прикончить" можно. Аномалия, но учтена.
            cust = customer;
        } else {
            // Вызываем кастомера
            cust = NetCommander.inviteNextCustomer(getNetProperty(), getUser().getId(), null, customer.getId());
        }

        // проговорим голосом вызов с компа оператора если есть настроичка
        if (cust != null && QConfig.cfg().needVoice()) {
            final boolean isFirst = false;
            new Thread(() ->
                    SoundPlayer.inviteClient(cust.getService(), cust, cust.getPrefix() + (cust.getNumber() < 1 ? "" : cust.getNumber()), getUser().getPoint(), isFirst)
            ).start();
        }
        if (cust == null) {
            QLog.l().logger().warn("Customer == NULL. Такого не должно быть при простом вызове.");
            closeWithEffect(event);
        }
    }

    /**
     * Customer going to be finished.
     *
     * @param event событие FX.
     */
    @FXML()
    public void finish(ActionEvent event) {
        QLog.l().logger().trace("Customer going to be finished. " + customer);
        // Переводим кастомера в разряд обрабатываемых
        try {
            NetCommander.getStartCustomer(getNetProperty(), getUser().getId());
            QLog.l().logger().debug("Customer was started. " + customer);
            // вернется кастомер и возможно он еще не домой а по списку услуг. Список определяется при старте кастомера в обработку специяльным юзером в регистратуре
            String resComments = getUser().getName() + ": \n_______________________\n" + customer.getTempComments();
            final QCustomer cust = NetCommander.getFinishCustomer(getNetProperty(), getUser().getId(), null, -1L, resComments);
            QLog.l().logger().info("Customer \"" + cust + "\" was finished. " + customer);
        } catch (QException e) {
            QLog.l().logger().error("Error. getFinishCustomer. User=\"" + getUser() + "\".", e);
            throw new ClientException(e);
        }
        getDesktopController().refreshSituation();
        closeWithEffect(event);
    }

}
