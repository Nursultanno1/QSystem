/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.client.fx;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.model.QCustomer;

import static ru.apertum.qsystem.common.CustomerState.STATE_DEAD;
import static ru.apertum.qsystem.common.CustomerState.STATE_FINISH;
import static ru.apertum.qsystem.common.CustomerState.STATE_INVITED;
import static ru.apertum.qsystem.common.CustomerState.STATE_INVITED_SECONDARY;
import static ru.apertum.qsystem.common.CustomerState.STATE_WORK;
import static ru.apertum.qsystem.common.CustomerState.STATE_WORK_SECONDARY;

/**
 * Контроллер рабочего стола.
 *
 * @author Evgeniy Egorov
 */
public class DesktopController extends UserController {

    @FXML
    private FlowPane area;

    @FXML()
    public void close() {
        getStage().close();
    }

    @FXML()
    public void refresh() {
        refreshSituation();
    }

    /**
     * Описание того, сколько народу стоит в очередях к этому юзеру, ну и прочее(потом)mess Не использовать на прямую.
     * <br>
     * see setSituation(Element plan)
     */
    private RpcGetSelfSituation.SelfSituation userPlan;

    /**
     * Кастомер, с которым работает юзер.
     */
    private QCustomer customer = null;

    public QCustomer getCustomer() {
        return customer;
    }

    public RpcGetSelfSituation.SelfSituation getUserPlan() {
        return userPlan;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        QLog.l().logger().info("Desktop was initialized.");
    }

    public void refreshSituation() {
        refreshSituation(null);
    }

    /**
     * Обновим ситуацию на рабочем столе.
     *
     * @param forced принудительно или нет, если обновляли недавно.
     */
    public void refreshSituation(Boolean forced) {
        //Получаем состояние очередей для юзера
        try {
            setSituation(NetCommander.getSelfServices(getNetProperty(), getUser().getId(), forced));
        } catch (QException th) {
            QLog.l().logger().error("Ошибка при обновлении состояния: ", th);
        }
    }

    /**
     * Определяет какова ситуация в очереди к пользователю.
     *
     * @param plan - ситуация в XML
     */
    private void setSituation(RpcGetSelfSituation.SelfSituation plan) {
        QLog.l().logger().trace("Обновляем видимую ситуацию.");
        if (plan.getSelfservices() == null) {
            return;
        }

        // посмотрим, не приехал ли кастомер, который уже вызванный
        // если приехал, то его надо учесть
        setCustomer(plan.getCustomer());
        if (plan.getCustomer() != null) {
            QLog.l().logger().trace("От сервера приехал кастомер, который обрабатывается юзером.");
            area.getChildren().add(makeCustomerView(customer));
        }

        area.getChildren().clear();
        plan.getPostponedList().forEach(customerPostponed -> {
            final Parent view = makeCustomerView(customerPostponed);
            if (view != null) {// чужих не добавляем
                area.getChildren().add(view);
            }
        });
        plan.getSelfservices().forEach(service
            -> service.getLine().forEach(customerShort
            -> area.getChildren().add(makeCustomerView(service, customerShort)))
        );
        //теперь описание очередей новое
        userPlan = plan;
    }

    private Parent makeCustomerView(QCustomer customer) {
        if (customer.getState() == STATE_DEAD || customer.getState() == STATE_FINISH) {
            return null;
        }
        final ControllerAndView<CustomerController, Parent> customerCav = new ControllerAndView<>("Customer.fxml", getStage());
        customerCav.getController().init(customer);
        if (customer.getState() == STATE_INVITED || customer.getState() == STATE_INVITED_SECONDARY
            || customer.getState() == STATE_WORK || customer.getState() == STATE_WORK_SECONDARY) {
            customerCav.getView().setOnMouseClicked(event -> new CustomerController.OnClickPostponedHandler(this, customer).handle(null));
            customerCav.getView().setEffect(customerCav.getController().getEffectRed());
            return customerCav.getView();
        }
        if (customer.getIsMine() != null) {
            if (customer.getIsMine().equals(getUser().getId())) {
                customerCav.getView().setOnMouseClicked(event -> new CustomerController.OnClickPostponedHandler(this, customer).handle(null));
                customerCav.getView().setEffect(customerCav.getController().getEffectRed());
            } else {
                return null; // Чужой закрался!
            }
        } else {
            customerCav.getView().setOnMouseClicked(event -> new CustomerController.OnClickWaitedHandler(this, customer).handle(null));
            customerCav.getView().setEffect(customerCav.getController().getEffectGreen());
        }
        return customerCav.getView();
    }

    private Parent makeCustomerView(RpcGetSelfSituation.SelfService selfService, RpcGetSelfSituation.CustomerShort custData) {
        final ControllerAndView<CustomerController, Parent> controllerAndView = new ControllerAndView<>("Customer.fxml", getStage());
        controllerAndView.getView().setOnMouseClicked(event -> new CustomerController.OnClickWaitedHandler(this, selfService, custData).handle(null));
        controllerAndView.getController().init(selfService, custData);
        return controllerAndView.getView();
    }

    /**
     * Устанавливаем кастомера для работы. Может быть NULL
     *
     * @param customer С ним работаем. Может быть NULL.
     */
    public void setCustomer(QCustomer customer) {
        this.customer = customer;
    }

}
