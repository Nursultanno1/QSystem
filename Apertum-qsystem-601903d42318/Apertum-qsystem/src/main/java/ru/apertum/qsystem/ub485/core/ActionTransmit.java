/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.apertum.qsystem.ub485.core;

import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.extra.IButtonDeviceFuctory;

import java.util.ServiceLoader;

/**
 * Класс содержит код для распараллеливаия обработки пришедшего пакета.
 *
 * @author Evgeniy Egorov
 */
public class ActionTransmit implements Runnable {

    private byte[] bytes;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public ActionTransmit() {
        this.bytes = new byte[0];
    }

    @Override
    public void run() {
        // поддержка расширяемости плагинами
        IButtonDeviceFuctory devFuctory = null;
        final ServiceLoader<IButtonDeviceFuctory> serviceLoader = ServiceLoader.load(IButtonDeviceFuctory.class);
        if (serviceLoader != null && serviceLoader.iterator().hasNext()) {
            devFuctory = serviceLoader.iterator().next();
            QLog.l().logger().info("Invoke SPI ext. Description: " + devFuctory.getDescription());
        }

        if (devFuctory != null || (bytes.length == 4 && bytes[0] == 0x01 && bytes[3] == 0x07)) {
            // должно быть 4 байта, иначе коллизия
            final IButtonDeviceFuctory.IButtonDevice dev = devFuctory == null
                ? AddrProp.getInstance().getAddrByRSAddr(bytes[1])
                : devFuctory.getButtonDevice(bytes, UBForm.getUsers());
            if (dev == null) {
                throw new ServerException("Anknown address from user device. " + (devFuctory == null ? "Hohlov device." : (devFuctory.toString() + " key=" + new String(bytes))));
            }
            dev.doAction(bytes);
        } else {
            QLog.l().logger().error("Collision! Package lenght is not 4 bytes.");
        }
    }
}
