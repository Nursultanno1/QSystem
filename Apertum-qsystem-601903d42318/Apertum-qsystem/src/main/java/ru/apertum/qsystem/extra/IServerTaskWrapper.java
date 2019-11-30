package ru.apertum.qsystem.extra;

import ru.apertum.qsystem.common.cmd.AJsonRPC20;
import ru.apertum.qsystem.common.cmd.CmdParams;

/**
 * Интерфейс для плагинов с логикой как на киоске так и на сервере.
 */
public interface IServerTaskWrapper extends IExtra {

    /**
     * Предобработка выполнения команды на сервере. Может перекрыть саму команду полностью если вернется результат не NULL.
     * Отработают все плагины перед выполнением команды и последний не нуловый результат отправится в ответ клиенту.
     *
     * @param task      Задание для сервера.
     * @param cmdParams входные параметры
     * @param ipAdress  источник команды
     * @param ip        источник команды
     * @return результат выполнения команды, соблюдать протокол jsonRPC2.0. NULL если обработка не потребовалась и продолжаем штатно.
     */
    AJsonRPC20 handleStandInParamsOnServerBefore(ITask task, CmdParams cmdParams, String ipAdress, byte[] ip);

    /**
     * Постобработка выполнения команды на сервере. Команда вернула результат и его еще раз перекроем плагином, если вернется результат не NULL, то отправить его.
     * Отработают все плагины после выполнения команды и последний не нуловый результат отправится в ответ клиенту, или если все результаты нуловые,
     * то отправится оригинальный результат команды.
     *
     * @param task      Задание для сервера.
     * @param cmdParams входные параметры
     * @param ipAdress  источник команды
     * @param ip        источник команды
     * @param result    Отработала таска, результат такой, с ним выполним постаброботку плагином.
     * @return результат выполнения команды, соблюдать протокол jsonRPC2.0. NULL если обработка не потребовалась и отправляем что уже есть.
     */
    AJsonRPC20 handleStandInParamsOnServerAfter(ITask task, CmdParams cmdParams, String ipAdress, byte[] ip, AJsonRPC20 result);
}
