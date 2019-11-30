/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.ub485.core;

import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.NetCommander;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.IButtonDeviceFuctory.IButtonDevice;
import ru.apertum.qsystem.server.model.QUser;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static ru.apertum.qsystem.common.QLog.log;

/**
 * Default buttons.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings("squid:S106")
public class ButtonDevice extends Object implements IButtonDevice {

    public final byte addres;
    public final boolean redirect;
    public final Long redirectServiceId;
    public final Long userId;
    private QUser user;
    private Integer qsize = 0;

    public Integer getQsize() {
        return qsize;
    }

    public void setQsize(int size) {
        qsize = size;
    }

    /**
     * Установить размер очереди к человеку с кнопкой.
     */
    public void setQsize(Integer qsize) {
        if (qsize != 0 && (user == null
                || user.getShadow() == null
                || user.getShadow().getCustomerState() == null
                || user.getShadow().getCustomerState() == CustomerState.STATE_BACK
                || user.getShadow().getCustomerState() == CustomerState.STATE_DEAD
                || user.getShadow().getCustomerState() == CustomerState.STATE_FINISH
                || user.getShadow().getCustomerState() == CustomerState.STATE_POSTPONED
                || user.getShadow().getCustomerState() == CustomerState.STATE_REDIRECT)) {
            beReadyBeep();
        }
        this.qsize = qsize;
    }

    private String serveceName = null;

    public void setServeceName(String serveceName) {
        this.serveceName = serveceName;
    }

    public String getServeceName() {
        return serveceName;
    }

    /**
     * Кнопочное устройство.
     *
     * @param userId            кнопка этого пользователя.
     * @param addres            адрес кнопки.
     * @param redirectServiceId Если устройство может перенаправлять.
     */
    public ButtonDevice(Long userId, byte addres, Long redirectServiceId) {
        this.addres = addres;
        this.userId = userId;
        this.redirectServiceId = redirectServiceId;
        this.redirect = redirectServiceId != null && redirectServiceId != 0;

        mess[0] = 1;
        mess[1] = addres;
        mess[mess.length - 1] = 7;
    }

    @Override
    public String toString() {
        return user == null ? String.valueOf(userId) : user.getName();
    }

    /*
     * mess[2] = 
     * 0x30 – светодиод погашен
     0x31 – включен Красный
     0x32 – включен Зеленый
     0x33 – мигает Красный (200 мс)
     0x34 – мигает Зеленый (200 мс)
     0x35 – мигает Красный (500 мс)
     0x36 – мигает Зеленый (500 мс)
     0x37 – писк (500 мс) + светодиод погашен
     0x38 – писк (500 мс) + включен Красный
     0x39 – писк (500 мс) + включен Зеленый
     0x3A – писк (500 мс) + мигает Красный (200 мс)
     0x3B – писк (500 мс) + мигает Зеленый (200 мс)
     0x3C – писк (500 мс) + мигает Красный (500 мс)
     0x3D – писк (500 мс) + мигает Зеленый (500 мс).
     */
    private final byte[] mess = new byte[4];

    /**
     * Тут вся логика работы кнопок и их нажатия.
     *
     * @param bb это команда от устройства
     */
    @Override
    public void doAction(byte[] bb) {
        byte b = bb[2];
        switch (b) {
            case 0x31:
                System.out.println("b == 0x31 -- 49");
                break;
            case 0x32:
                System.out.println("b == 0x32 -- 50");
                break;
            case 0x33:
                System.out.println("b == 0x33 -- 51");
                break;
            case 0x34:
                System.out.println("b == 0x34 -- 52");
                break;
            default:
                log().error("PIZDEzzzz.....");
                break;
        }
        if (user == null) {
            System.out.println("user == null");
            throw new RuntimeException("User is null. bb=" + Arrays.toString(bb));
        } else {
            if (user.getShadow() == null) {
                log().error("user.getShadow() == null");
            } else {
                if (user.getShadow().getCustomerState() == null) {
                    log().error("user.getShadow().getCustomerState() == null");
                } else {
                    log().trace("user.getShadow().getCustomerState() == " + user.getShadow().getCustomerState());
                }
            }
        }
        // первичный вызов
        if ((user.getShadow() == null
                || user.getShadow().getCustomerState() == null
                || user.getShadow().getCustomerState() == CustomerState.STATE_BACK
                || user.getShadow().getCustomerState() == CustomerState.STATE_DEAD
                || user.getShadow().getCustomerState() == CustomerState.STATE_FINISH
                || user.getShadow().getCustomerState() == CustomerState.STATE_POSTPONED
                || user.getShadow().getCustomerState() == CustomerState.STATE_REDIRECT)
                && (b == 0x31)) {
            //команда вызова кастомера
            log().info("Invite Next Customer by " + user.getName());
            QCustomer cust = null;
            try {
                cust = NetCommander.inviteNextCustomer(UBForm.getForm().getNetProperty(), userId);
            } catch (QException e) {
                System.err.println("!!! ERROR !!! " + e);
            }
            System.out.println("inv ** 0");
            if (cust != null) {
                System.out.println("inv ** 1");
                if (user.getShadow() == null) {
                    user.setShadow(new QUser.Shadow(cust));
                    System.out.println("inv ** 1`");
                }
                user.getShadow().setCustomerState(cust.getState());
                System.out.println("inv ** 2");
                user.getShadow().setOldPref(cust.getPrefix());
                System.out.println("inv ** 3");
                user.getShadow().setOldNom(cust.getNumber());
                System.out.println("inv ** 4");

                //добавляем табло на пульте
                byte[] bytes = mess;
                try {
                    bytes = ("123" + (cust.getFullNumber() + "    ").substring(0, 3) + "7").getBytes("cp1251");
                } catch (UnsupportedEncodingException ex) {
                    System.err.println("!!! ERROR !!! " + ex);
                }
                bytes[0] = 1;
                bytes[1] = addres;
                bytes[bytes.length - 1] = 7;
                //mess[0] = 0x01; // начало
                //mess[10] = 0x07; // конец
                //mess[1] = addr.addres; // адрес
                bytes[2] = 0x21;//0x20; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
                UBForm.sendToDevice(bytes);
                /*
                 //ответ о результате на кнопку
                 mess[2] = 0x36; // – мигает Зеленый (500 мс)
                 UBForm.sendToDevice(mess)
                 */
            } else {
                System.out.println("inv ** 5");
                user.getShadow().setCustomerState(CustomerState.STATE_FINISH);
                System.out.println("inv ** 6");
                lightDown();
                System.out.println("inv ** 7");
            }
            System.out.println("inv ** 8");
            return;
        }

        // повторный вызов
        if ((user != null && user.getShadow() != null && user.getShadow().getCustomerState() != null)
                && (user.getShadow().getCustomerState() == CustomerState.STATE_INVITED || user.getShadow().getCustomerState() == CustomerState.STATE_INVITED_SECONDARY)
                && (b == 0x31)) {
            //команда вызова кастомера
            log().info("Recall by " + user.getName());
            try {
                NetCommander.inviteNextCustomer(UBForm.getForm().getNetProperty(), userId);
            } catch (QException e) {
                System.err.println("!!! ERROR !!! " + e);
            }
            System.out.println("--<>\n");
            return;
        }

        // начало работы
        if ((user != null && user.getShadow() != null && user.getShadow().getCustomerState() != null)
                && (user.getShadow().getCustomerState() == CustomerState.STATE_INVITED || user.getShadow().getCustomerState() == CustomerState.STATE_INVITED_SECONDARY)
                && (b == 0x32)) {
            //команда вызова кастомера
            log().info("get Start Customer by " + user.getName());
            try {
                NetCommander.getStartCustomer(UBForm.getForm().getNetProperty(), userId);
            } catch (QException e) {
                throw new ServerException(e);
            }
            System.out.println("--1\n");
            user.getShadow().setCustomerState(CustomerState.STATE_WORK);
            System.out.println("--2\n");

            //добавляем табло на пульте
            byte[] bytes = mess;
            try {
                bytes = ("123" + (user.getCustomer().getFullNumber() + "    ").substring(0, 3) + "7").getBytes("cp1251");
            } catch (UnsupportedEncodingException ex) {
                System.err.println("!!! ERROR !!! " + ex);
            }
            bytes[0] = 1;
            bytes[1] = addres;
            bytes[bytes.length - 1] = 7;
            //mess[0] = 0x01 // начало
            //mess[10] = 0x07 // конец
            //mess[1] = addr.addres // адрес
            bytes[2] = 0x20;//0x20 // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
            UBForm.sendToDevice(bytes);
            return;
        }

        // отклонить по неявке
        if ((user != null && user.getShadow() != null && user.getShadow().getCustomerState() != null)
                && (user.getShadow().getCustomerState() == CustomerState.STATE_INVITED || user.getShadow().getCustomerState() == CustomerState.STATE_INVITED_SECONDARY)
                && (b == 0x34)) {
            //команда вызова кастомера
            log().info("kill Next Customer by " + user.getName());
            try {
                NetCommander.killNextCustomer(UBForm.getForm().getNetProperty(), userId, null);
            } catch (QException e) {
                log().error("Error kill Next Customer by " + user.getName(), e);
            }
            user.getShadow().setCustomerState(CustomerState.STATE_DEAD);
            //ответ о результате на кнопку
            if (qsize == 0) {
                lightDown();
            } else {
                beReady();
            }
            return;
        }

        // закончить работу
        if ((user != null && user.getShadow() != null && user.getShadow().getCustomerState() != null)
                && (user.getShadow().getCustomerState() == CustomerState.STATE_WORK || user.getShadow().getCustomerState() == CustomerState.STATE_WORK_SECONDARY)
                && (b == 0x34)) {
            //команда завершения работы

            log().info("get Finish Customer by " + user.getName());
            try {
                NetCommander.getFinishCustomer(UBForm.getForm().getNetProperty(), userId, null, -1L, "");
            } catch (QException e) {
                System.err.println("!!! ERROR !!! " + e);
                log().error("getFinishCustomer Error. " + user.getName(), e);
            }
            user.getShadow().setCustomerState(CustomerState.STATE_FINISH);
            //ответ о результате на кнопку
            if (qsize == 0) {
                lightDown();
            } else {
                beReady();
            }
            return;
        }

        //команда  редирект
        if ((user != null && user.getShadow() != null && user.getShadow().getCustomerState() != null)
                && (user.getShadow().getCustomerState() == CustomerState.STATE_WORK || user.getShadow().getCustomerState() == CustomerState.STATE_WORK_SECONDARY)
                && (b == 0x33) && redirect) {
            //команда  редирект
            log().info("redirect Customer by " + user.getName());
            try {
                NetCommander.redirectCustomer(UBForm.getForm().getNetProperty(), userId, null, redirectServiceId, false, "", -1L);
            } catch (QException e) {
                throw new ServerException(e);
            }
            user.getShadow().setCustomerState(CustomerState.STATE_FINISH);
            //ответ о результате на кнопку
            if (qsize == 0) {
                lightDown();
            } else {
                beReady();
            }
        }

        // обновить состояние
        /*
         if (b == 0x34) {
         if (user != null
         || user.getShadow() != null
         || user.getShadow().getCustomerState() != null) {
         if (qsize == 0) {
         lightDown()
         } else {
         beReady()
         }
         } else {
         if (user.getShadow().getCustomerState() == CustomerState.STATE_INVITED
         || user.getShadow().getCustomerState() == CustomerState.STATE_INVITED_SECONDARY) {
         mess[2] = 0x36 // – мигает Зеленый (500 мс)
         UBForm.sendToDevice(mess)
         } else {
         if (user.getShadow().getCustomerState() == CustomerState.STATE_WORK
         || user.getShadow().getCustomerState() == CustomerState.STATE_WORK_SECONDARY) {
         mess[2] = 0x32 //– включен Зеленый
         UBForm.sendToDevice(mess)
         } else {
         if (qsize == 0) {
         lightDown()
         } else
         beReady()
         }
         }
         }
         }
         }
         */
    }

    private void beReady() {
        log().info("beReady()");

        //добавляем табло на пульте
        byte[] bytes = mess;
        try {
            bytes = ("123CAL7").getBytes("cp1251");
        } catch (UnsupportedEncodingException ex) {
            System.err.println("!!! ERROR !!! " + ex);
        }
        bytes[0] = 1;
        bytes[1] = addres;
        bytes[bytes.length - 1] = 7;
        //mess[0] = 0x01; // начало
        //mess[10] = 0x07; // конец
        //mess[1] = addr.addres; // адрес
        bytes[2] = 0x20;//0x20; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
        UBForm.sendToDevice(bytes);

        /*
         mess[2] = 0x34;// – мигает Зеленый (200 мс)
         UBForm.sendToDevice(mess)
         */
    }

    private void beReadyBeep() {
        log().info("beReadyBeep()");

        //добавляем табло на пульте
        byte[] bytes = mess;
        try {
            bytes = ("123CAL7").getBytes("cp1251");
        } catch (UnsupportedEncodingException ex) {
            System.err.println("!!! ERROR !!! " + ex);
        }
        bytes[0] = 1;
        bytes[1] = addres;
        bytes[bytes.length - 1] = 7;
        //mess[0] = 0x01; // начало
        //mess[10] = 0x07; // конец
        //mess[1] = addr.addres; // адрес
        bytes[2] = 0x21;//0x20; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
        UBForm.sendToDevice(bytes);

        /*
         mess[2] = 0x3B; // – писк (500 мс) + мигает Зеленый (200 мс)
         UBForm.sendToDevice(mess)
         */
    }

    private void lightDown() {
        log().info("lightDown()");

        //добавляем табло на пульте
        byte[] bytes = mess;
        try {
            bytes = ("123    7").getBytes("cp1251");
        } catch (UnsupportedEncodingException ex) {
            System.err.println("!!! ERROR !!! " + ex);
        }
        bytes[0] = 1;
        bytes[1] = addres;
        bytes[bytes.length - 1] = 7;
        //mess[0] = 0x01; // начало
        //mess[10] = 0x07; // конец
        //mess[1] = addr.addres; // адрес
        bytes[2] = 0x20;//0x20; // мигание Режим мигания: 0x20 – не мигает; 0x21 – мигает постоянно; 0x22…0x7F – мигает  (N-0x21) раз.
        UBForm.sendToDevice(bytes);
        /*
         mess[2] = 0x30// – светодиод погашен
         UBForm.sendToDevice(mess)
         */
    }

    @Override
    public void getFeedback() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void changeAdress() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void check() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public QUser getUser() {
        return user;
    }

    @Override
    public String getId() {
        byte[] bb = new byte[1];
        bb[0] = addres;
        return new String(bb);
    }

    @Override
    public void setUser(QUser user) {
        this.user = user;
    }
}
