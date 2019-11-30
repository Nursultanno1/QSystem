/*
 *  Copyright (C) 2010 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.common;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.jetty.http.HttpHeader;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.client.QProperties;
import ru.apertum.qsystem.common.cmd.AJsonRPC20;
import ru.apertum.qsystem.common.cmd.CmdParams;
import ru.apertum.qsystem.common.cmd.JsonRPC20;
import ru.apertum.qsystem.common.cmd.JsonRPC20Error;
import ru.apertum.qsystem.common.cmd.JsonRPC20OK;
import ru.apertum.qsystem.common.cmd.RpcBanList;
import ru.apertum.qsystem.common.cmd.RpcGetAdvanceCustomer;
import ru.apertum.qsystem.common.cmd.RpcGetAllServices;
import ru.apertum.qsystem.common.cmd.RpcGetAuthorizCustomer;
import ru.apertum.qsystem.common.cmd.RpcGetBool;
import ru.apertum.qsystem.common.cmd.RpcGetGridOfDay;
import ru.apertum.qsystem.common.cmd.RpcGetGridOfWeek;
import ru.apertum.qsystem.common.cmd.RpcGetInfoTree;
import ru.apertum.qsystem.common.cmd.RpcGetInt;
import ru.apertum.qsystem.common.cmd.RpcGetPostponedPoolInfo;
import ru.apertum.qsystem.common.cmd.RpcGetProperties;
import ru.apertum.qsystem.common.cmd.RpcGetRespTree;
import ru.apertum.qsystem.common.cmd.RpcGetResultsList;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.cmd.RpcGetServerState;
import ru.apertum.qsystem.common.cmd.RpcGetServerState.ServiceInfo;
import ru.apertum.qsystem.common.cmd.RpcGetServiceState;
import ru.apertum.qsystem.common.cmd.RpcGetServiceState.ServiceState;
import ru.apertum.qsystem.common.cmd.RpcGetSrt;
import ru.apertum.qsystem.common.cmd.RpcGetStandards;
import ru.apertum.qsystem.common.cmd.RpcGetTicketHistory;
import ru.apertum.qsystem.common.cmd.RpcGetTicketHistory.TicketHistory;
import ru.apertum.qsystem.common.cmd.RpcGetUsersList;
import ru.apertum.qsystem.common.cmd.RpcInviteCustomer;
import ru.apertum.qsystem.common.cmd.RpcStandInService;
import ru.apertum.qsystem.common.exceptions.ClientException;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.INetProperty;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.qsystem.server.http.CommandHandler;
import ru.apertum.qsystem.server.model.QAdvanceCustomer;
import ru.apertum.qsystem.server.model.QAuthorizationCustomer;
import ru.apertum.qsystem.server.model.QProperty;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.QStandards;
import ru.apertum.qsystem.server.model.QUser;
import ru.apertum.qsystem.server.model.infosystem.QInfoItem;
import ru.apertum.qsystem.server.model.response.QRespItem;
import ru.apertum.qsystem.server.model.results.QResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static org.apache.http.HttpHeaders.USER_AGENT;
import static ru.apertum.qsystem.client.Locales.locMes;
import static ru.apertum.qsystem.common.QLog.log;

/**
 * Содержит статические методы отправки и получения заданий на сервер. любой метод возвращает XML-узел ответа сервера.
 *
 * @author Evgeniy Egorov
 */
@SuppressWarnings({"squid:S3358", "squid:S1192", "squid:S1319"})
public class NetCommander {

    private NetCommander() {
        // do not use.
    }

    private static final JsonRPC20 JSON_RPC = new JsonRPC20();

    /**
     * основная работа по отсылки и получению результата.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param commandName имя команды.
     * @param params      параметры.
     * @return текст-ответ
     * @throws ru.apertum.qsystem.common.exceptions.QException что-то пошло не так.
     */
    public static synchronized String send(INetProperty netProperty, String commandName, CmdParams params) throws QException {
        JSON_RPC.setMethod(commandName);
        JSON_RPC.setParams(params);
        return sendRpc(netProperty, JSON_RPC);
    }

    private static Proxy getProxy(Proxy.Type proxyType) {
        if (QConfig.cfg().getProxy() != null) {
            log().trace("Proxy.Type." + proxyType + ": " + QConfig.cfg().getProxy().host + ":" + QConfig.cfg().getProxy().port);
            return new Proxy(proxyType, new InetSocketAddress(QConfig.cfg().getProxy().host, QConfig.cfg().getProxy().port));
        }
        if (QProperties.get().getProperty("proxy", "hostname") != null && QProperties.get().getProperty("proxy", "port") != null) {
            log().trace("Proxy.Type." + proxyType + ": " + QProperties.get().getProperty("proxy", "hostname").getValue()
                    + ":" + QProperties.get().getProperty("proxy", "port").getValueAsInt());
            return new Proxy(proxyType, new InetSocketAddress(QProperties.get().getProperty("proxy", "hostname").getValue(),
                    QProperties.get().getProperty("proxy", "port").getValueAsInt()));
        } else {
            return null;
        }
    }

    /**
     * Отсылаем команду и получаем ответ. По TCP.
     *
     * @param netProperty сеть.
     * @param jsonRpc     команда.
     * @return текстовый ответ.
     * @throws QException упало.
     */
    @SuppressWarnings("squid:S3776")
    private static synchronized String sendRpc(INetProperty netProperty, JsonRPC20 jsonRpc) throws QException {
        final String message;
        Gson gson = GsonPool.getInstance().borrowGson();
        try {
            message = gson.toJson(jsonRpc);
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        final String data;

        if (QConfig.cfg().getHttpRequestType().doUseHttp() && !(jsonRpc.getMethod().startsWith("#") || "empty".equalsIgnoreCase(jsonRpc.getMethod()))) {
            try {
                data = QConfig.cfg().getHttpRequestType().isPost() ? sendPost(netProperty, message, jsonRpc) : sendGet(netProperty, jsonRpc);
            } catch (Exception ex) {
                throw new QException(locMes("no_response_from_server"), ex);
            }
        } else {
            final Proxy proxy = getProxy(Proxy.Type.SOCKS);

            log().trace("Task \"" + jsonRpc.getMethod() + "\" on " + netProperty.getAddress().getHostAddress() + ":" + netProperty.getPort() + "#\n" + message);
            try (final Socket socket = proxy == null ? (QConfig.cfg().doNotUseProxy() ? new Socket(Proxy.NO_PROXY) : new Socket()) : new Socket(proxy)) {
                log().trace("Socket was created.");
                socket.connect(new InetSocketAddress(netProperty.getAddress(), netProperty.getPort()), 15000);

                final PrintWriter writer = new PrintWriter(socket.getOutputStream());
                writer.print(URLEncoder.encode(message, "utf-8"));
                log().trace("Sending...");
                writer.flush();
                log().trace("Reading...");
                final StringBuilder sb = new StringBuilder();
                final Scanner in = new Scanner(socket.getInputStream());
                while (in.hasNextLine()) {
                    sb.append(in.nextLine()).append("\n");
                }
                data = URLDecoder.decode(sb.toString(), "utf-8");
                sb.setLength(0);
                writer.close();
                in.close();
            } catch (IOException ex) {
                Uses.closeSplash();
                throw new QException(locMes("no_connect_to_server"), ex);
            } catch (Exception ex) {
                throw new QException(locMes("no_response_from_server"), ex);
            }
            log().trace("Response:\n" + data);
        }

        gson = GsonPool.getInstance().borrowGson();
        try {
            final JsonRPC20Error rpc = gson.fromJson(data, JsonRPC20Error.class);
            if (rpc == null) {
                throw new QException(locMes("error_on_server_no_get_response"));
            }
            if (rpc.getError() != null) {
                throw new QException(locMes("tack_failed") + " " + rpc.getError().getCode() + ":" + rpc.getError().getMessage());
            }
        } catch (JsonSyntaxException ex) {
            throw new QException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return data;
    }

    // HTTP POST request
    private static String sendPost(INetProperty netProperty, String outputData, JsonRPC20 jsonRpc) throws Exception { // NOSONAR
        String url = "http://" + netProperty.getAddress().getHostAddress() + ":" + QConfig.cfg().getHttpProtocol() + CommandHandler.CMD_URL_PATTERN;
        log().trace("HTTP POST request \"" + jsonRpc.getMethod() + "\" on " + url + "\n" + outputData);
        final URL obj = new URL(url);
        final Proxy proxy = getProxy(Proxy.Type.HTTP);

        final StringBuilder response;
        final HttpURLConnection con = (HttpURLConnection) (proxy == null
                ? (QConfig.cfg().doNotUseProxy() ? obj.openConnection(Proxy.NO_PROXY) : obj.openConnection())
                : obj.openConnection(proxy));
        try {
            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty(HttpHeader.CONTENT_TYPE.asString(), "text/json; charset=UTF-8");

            // Send post request
            con.setDoOutput(true);

            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF8"))) {
                out.append(outputData);
                out.flush();
            }

            if (con.getResponseCode() != 200) {
                log().error("HTTP POST response code = " + con.getResponseCode());
                throw new QException(locMes("no_connect_to_server"));
            }

            response = readResponseFromStream(con.getInputStream());
        } finally {
            con.disconnect();
        }

        //result
        final String res = response.toString();
        response.setLength(0);
        log().trace("HTTP POST response:\n" + res);
        return res;
    }

    private static StringBuilder readResponseFromStream(InputStream inputStream) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String inputLine;
            final StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response;
        }
    }

    // HTTP GET request
    private static String sendGet(INetProperty netProperty, JsonRPC20 jsonRpc) throws Exception { // NOSONAR
        final String p = jsonRpc.getParams() == null ? "" : jsonRpc.getParams().toString();
        final String url = "http://" + netProperty.getAddress().getHostAddress() + ":" + QConfig.cfg().getHttpProtocol() + CommandHandler.CMD_URL_PATTERN + "?"
                + CmdParams.CMD + "=" + URLEncoder.encode(jsonRpc.getMethod(), "utf-8") + "&"
                + p;
        log().trace("HTTP GET request \"" + jsonRpc.getMethod() + "\" on " + url + "\n" + p);
        final URL obj = new URL(url);
        final Proxy proxy = getProxy(Proxy.Type.HTTP);

        final HttpURLConnection con = (HttpURLConnection) (proxy == null
                ? (QConfig.cfg().doNotUseProxy() ? obj.openConnection(Proxy.NO_PROXY) : obj.openConnection())
                : obj.openConnection(proxy));
        final StringBuilder response;
        try {
            //add reuqest header
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", USER_AGENT);

            if (con.getResponseCode() != 200) {
                log().error("HTTP GET response code = " + con.getResponseCode());
                throw new QException(locMes("no_connect_to_server"));
            }
            response = readResponseFromStream(con.getInputStream());
        } finally {
            con.disconnect();
        }

        //result
        final String res = response.toString();
        response.setLength(0);
        log().trace("HTTP GET response:\n" + res);
        return res;
    }

    /**
     * Выполнение универсальной команды.
     *
     * @param netProperty сеть. Сетевае параметры
     * @param cmdName     Имя команды. По нему регистрируется исполнитель на сервере.
     * @param params      Параметры команды.
     * @return Некий результат. Подумать, будет ли нормально маршалиться.
     */
    public static AJsonRPC20 runCmd(INetProperty netProperty, String cmdName, CmdParams params) throws QException {
        log().info("Выполнение универсальной команды.");
        // загрузим ответ
        final String res = send(netProperty, cmdName, params);
        return AJsonRPC20.demarshal(res, AJsonRPC20.class);
    }

    /**
     * Получение возможных услуг.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @return XML-ответ
     */
    public static RpcGetAllServices.ServicesForWelcome getServices(INetProperty netProperty) throws QException {
        log().info("Получение возможных услуг.");
        // загрузим ответ
        String res = send(netProperty, Uses.TASK_GET_SERVICES, null);
        final RpcGetAllServices allServices = AJsonRPC20.demarshal(res, RpcGetAllServices.class);
        QServiceTree.sailToStorm(allServices.getResult().getRoot(), service -> {
            final QService perent = (QService) service;
            perent.getChildren().forEach(svr -> svr.setParent(perent));
        });
        return allServices.getResult();
    }

    /**
     * Постановка в очередь.
     *
     * @param netProperty сеть. netProperty параметры соединения с сервером.
     * @param serviceId   услуга, в которую пытаемся встать.
     * @param password    пароль того кто пытается выполнить задание.
     * @param priority    приоритет.
     * @param inputData   введенный текст посетителем.
     * @return Созданный кастомер.
     */
    public static QCustomer standInService(INetProperty netProperty, long serviceId, String password, int priority, String inputData) throws QException {
        log().info("Встать в очередь.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.parolcheg = password;
        params.priority = priority;
        params.textData = inputData;
        params.language = Locales.getInstance().getNameOfPresentLocale(); // передадим выбранный язык интерфейса
        final String res = send(netProperty, Uses.TASK_STAND_IN, params);
        final RpcStandInService rpc = AJsonRPC20.demarshal(res, RpcStandInService.class);
        return rpc.getResult();
    }

    /**
     * Постановка в очередь.
     *
     * @param netProperty сеть. netProperty параметры соединения с сервером.
     * @param servicesId  услуги, в которые пытаемся встать. Требует уточнения что это за трехмерный массив. Это пять списков. Первый это вольнопоследовательные
     *                    услуги. Остальные четыре это зависимопоследовательные услуги, т.е. пока один не закончится на другой не переходить. Что такое элемент списка. Это тоже
     *                    список. Первый элемент это та самая комплексная услуга(ID). А остальные это зависимости, т.е. если есть еще не оказанные услуги но назначенные, которые в
     *                    зависимостях, то их надо оказать.
     * @param password    пароль того кто пытается выполнить задание.
     * @param priority    приоритет.
     * @param inputData   введенный текст посетителем.
     * @return Созданный кастомер.
     */
    public static QCustomer standInSetOfServices(INetProperty netProperty, LinkedList<LinkedList<LinkedList<Long>>> servicesId, String password, int priority, String inputData) {
        log().info("Встать в очередь комплексно.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.complexId = servicesId;
        params.parolcheg = password;
        params.priority = priority;
        params.textData = inputData;
        String res = null;
        try {
            res = send(netProperty, Uses.TASK_STAND_COMPLEX, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcStandInService rpc;
        try {
            rpc = gson.fromJson(res, RpcStandInService.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Сделать услугу временно неактивной или разблокировать временную неактивность.
     *
     * @param netProperty сеть. netProperty параметры соединения с сервером.
     * @param serviceId   услуга, которую пытаемся править
     * @param reason      причина неактивности.
     */
    public static void changeTempAvailableService(INetProperty netProperty, long serviceId, String reason) {
        log().info("Сделать услугу временно неактивной/активной.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.textData = reason;
        try {
            send(netProperty, Uses.TASK_CHANGE_TEMP_AVAILABLE_SERVICE, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
    }

    /**
     * Узнать сколько народу стоит к услуге и т.д.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param serviceId   id услуги о которой получаем информацию
     * @return количество предшествующих.
     * @throws QException упало при отсыле.
     */
    public static ServiceState aboutService(INetProperty netProperty, long serviceId) throws QException {
        log().info("Узнать сколько народу стоит к услуге.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        String res;
        try {
            res = send(netProperty, Uses.TASK_ABOUT_SERVICE, params);
        } catch (QException ex) {// вывод исключений
            throw new QException(locMes("command_error"), ex);
        }
        final RpcGetServiceState rpc = AJsonRPC20.demarshal(res, RpcGetServiceState.class);
        return rpc.getResult();
    }

    /**
     * Получить всю очередь к услуге и т.д.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param serviceId   id услуги о которой получаем информацию
     * @return количество предшествующих.
     * @throws QException упало при отсыле.
     */
    public static ServiceState getServiceConsistency(INetProperty netProperty, long serviceId) throws QException {
        log().info("Встать в очередь.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        String res = null;
        try {
            res = send(netProperty, Uses.TASK_GET_SERVICE_CONSISANCY, params);
        } catch (QException ex) {// вывод исключений
            throw new QException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetServiceState rpc;
        try {
            rpc = gson.fromJson(res, RpcGetServiceState.class);
        } catch (JsonSyntaxException ex) {
            throw new QException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Узнать можно ли вставать в услугу с такими введенными данными.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param serviceId   id услуги о которой получаем информацию
     * @param inputData   введенная ботва
     * @return 1 - превышен, 0 - можно встать. 2 - забанен
     * @throws QException упало при отсыле.
     */
    public static int aboutServicePersonLimitOver(INetProperty netProperty, long serviceId, String inputData) throws QException {
        log().info("Узнать можно ли вставать в услугу с такими введенными данными.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.textData = inputData;
        String res = null;
        try {
            res = send(netProperty, Uses.TASK_ABOUT_SERVICE_PERSON_LIMIT, params);
        } catch (QException ex) {// вывод исключений
            throw new QException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetInt rpc;
        try {
            rpc = gson.fromJson(res, RpcGetInt.class);
        } catch (JsonSyntaxException ex) {
            throw new QException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получение описания всех юзеров для выбора себя.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @return XML-ответ все юзеры системы
     */
    public static LinkedList<QUser> getUsers(INetProperty netProperty) throws QException {
        log().info("Получение описания всех юзеров для выбора себя.");
        // загрузим ответ
        String res = null;
        try {
            res = send(netProperty, Uses.TASK_GET_USERS, null);
        } catch (QException e) {// вывод исключений
            Uses.closeSplash();
            throw new QException(locMes("command_error2"), e);
        }
        final RpcGetUsersList rpc = AJsonRPC20.demarshal(res, RpcGetUsersList.class);
        return rpc.getResult();
    }

    /**
     * Получение описания очередей для юзера.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param userId      id пользователя для которого идет опрос
     * @return список обрабатываемых услуг с количеством кастомеров в них стоящих и обрабатываемый кастомер если был
     * @throws ru.apertum.qsystem.common.exceptions.QException упало при отсыле.
     */
    public static RpcGetSelfSituation.SelfSituation getSelfServices(INetProperty netProperty, long userId) throws QException {
        return getSelfServices(netProperty, userId, null);
    }

    /**
     * Получение описания очередей для юзера.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      id пользователя для которого идет опрос.
     * @param forced      получить ситуацию даже если она не обновлялась за последнее время.
     * @return список обрабатываемых услуг с количеством кастомеров в них стоящих и обрабатываемый кастомер если был.
     * @throws ru.apertum.qsystem.common.exceptions.QException упало при отсыле.
     */
    public static RpcGetSelfSituation.SelfSituation getSelfServices(INetProperty netProperty, long userId, Boolean forced) throws QException {
        log().info("Получение описания очередей для юзера.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.textData = QConfig.cfg().getPointN();
        params.requestBack = forced;
        String res;
        try {
            res = send(netProperty, Uses.TASK_GET_SELF_SERVICES, params);
        } catch (QException e) {// вывод исключений
            Uses.closeSplash();
            throw new QException(locMes("command_error2"), e);
        }
        final RpcGetSelfSituation rpc = AJsonRPC20.demarshal(res, RpcGetSelfSituation.class);
        return rpc.getResult();
    }

    /**
     * Проверка на то что такой юзер уже залогинен в систему.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      id пользователя для которого идет опрос.
     * @return false - запрешено, true - новый.
     */
    public static boolean getSelfServicesCheck(INetProperty netProperty, long userId) throws QException {
        log().info("Получение описания очередей для юзера.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.textData = QConfig.cfg().getPointN();
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_SELF_SERVICES_CHECK, params);
        } catch (QException e) {// вывод исключений
            Uses.closeSplash();
            throw new QException(locMes("command_error2"), e);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetBool rpc = gson.fromJson(res, RpcGetBool.class);
        return rpc.getResult();
    }

    /**
     * Получение слeдующего юзера из очередей, обрабатываемых юзером.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      оператор
     * @return ответ-кастомер следующий по очереди.
     */
    public static QCustomer inviteNextCustomer(INetProperty netProperty, long userId) throws QException {
        log().info("Получение следующего юзера из очередей, обрабатываемых юзером.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        final String res = send(netProperty, Uses.TASK_INVITE_NEXT_CUSTOMER, params);
        final RpcInviteCustomer rpc = AJsonRPC20.demarshal(res, RpcInviteCustomer.class);
        return rpc.getResult();
    }

    /**
     * Получение слeдующего юзера из одной конкретной очереди, обрабатываемой юзером.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      оператор
     * @param serviceId   услуга
     * @return ответ-кастомер следующий по очереди.
     */
    public static QCustomer inviteNextCustomer(INetProperty netProperty, long userId, long serviceId) throws QException {
        log().info("Получение слeдующего юзера из одной конкретной очереди, обрабатываемой юзером.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.serviceId = serviceId;
        final String res = send(netProperty, Uses.TASK_INVITE_NEXT_CUSTOMER, params);
        final RpcInviteCustomer rpc = AJsonRPC20.demarshal(res, RpcInviteCustomer.class);
        return rpc.getResult();
    }

    /**
     * Получение, блядь, любого кастомера из очередей.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      оператор
     * @param num         номер чела из очереди.
     * @return ответ-кастомер следующий по очереди.
     */
    public static QCustomer inviteNextCustomer(INetProperty netProperty, long userId, String num) {
        return inviteNextCustomer(netProperty, userId, num, null);
    }

    /**
     * Получение, блядь, любого кастомера из очередей.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      оператор
     * @param num         либо ID либо num, но ID в приоритете.
     * @param customerId  либо ID либо num, но ID в приоритете.
     * @return ответ-кастомер следующий по очереди.
     */
    public static QCustomer inviteNextCustomer(INetProperty netProperty, long userId, String num, Long customerId) {
        QLog.l().logger().info("Получение, блядь, любого кастомера из очередей юзера из очередей, обрабатываемых юзером.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.textData = num;
        params.customerId = customerId;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_INVITE_NEXT_CUSTOMER, params);
        } catch (QException e) {// вывод исключений
            throw new ClientException("Невозможно получить ответ от сервера. " + e.toString());
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcInviteCustomer rpc;
        try {
            rpc = gson.fromJson(res, RpcInviteCustomer.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(Locales.locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Удаление вызванного юзером кастомера.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param userId      оператор
     * @param customerId  переключиться на этого при параллельном приеме, NULL если переключаться не надо
     */
    public static void killNextCustomer(INetProperty netProperty, long userId, Long customerId) throws QException {
        log().info("Удаление вызванного юзером кастомера.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.customerId = customerId;
        send(netProperty, Uses.TASK_KILL_NEXT_CUSTOMER, params);
    }

    /**
     * Перемещение вызванного юзером кастомера в пул отложенных.
     *
     * @param netProperty     сеть.     параметры соединения с сервером
     * @param userId          оператор
     * @param customerId      переключиться на этого при параллельном приеме, NULL если переключаться не надо
     * @param status          просто строка. берется из возможных состояний завершения работы
     * @param postponedPeriod Период отложенности в минутах. 0 - бессрочно;
     * @param isMine          отложенный пользователь будет видет только отложившему оператору
     */
    public static void customerToPostpone(INetProperty netProperty, long userId, Long customerId, String status, int postponedPeriod, boolean isMine) throws QException {
        customerToPostpone(netProperty, userId, customerId, status, postponedPeriod, isMine, null);
    }

    /**
     * Перемещение вызванного юзером кастомера в пул отложенных.
     *
     * @param netProperty     сеть.     параметры соединения с сервером
     * @param userId          оператор
     * @param customerId      переключиться на этого при параллельном приеме, NULL если переключаться не надо
     * @param status          просто строка. берется из возможных состояний завершения работы
     * @param postponedPeriod Период отложенности в минутах. 0 - бессрочно;
     * @param isMine          отложенный пользователь будет видет только отложившему оператору
     * @param strict          false - Если потребуется искать кастомера не только своего вызванного, но и произвольного из всей толпы для отложения.
     */
    public static void customerToPostpone(INetProperty netProperty, long userId, Long customerId,
                                          String status, int postponedPeriod, boolean isMine, Boolean strict) throws QException {
        log().info("Перемещение вызванного юзером кастомера в пул отложенных.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.customerId = customerId;
        params.textData = status;
        params.postponedPeriod = postponedPeriod;
        params.isMine = isMine ? userId : null;
        params.strict = strict;
        send(netProperty, Uses.TASK_CUSTOMER_TO_POSTPON, params);
    }

    /**
     * Изменение отложенному кастомеру статеса.
     *
     * @param netProperty       сеть.       параметры соединения с сервером
     * @param postponCustomerId меняем этому кастомеру
     * @param status            просто строка. берется из возможных состояний завершения работы
     */
    public static void postponeCustomerChangeStatus(INetProperty netProperty, long postponCustomerId, String status) throws QException {
        log().info("Перемещение вызванного юзером кастомера в пул отложенных.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.customerId = postponCustomerId;
        params.textData = status;
        send(netProperty, Uses.TASK_POSTPON_CHANGE_STATUS, params);
    }

    /**
     * Начать работу с вызванным кастомером.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param userId      оператор
     */
    public static void getStartCustomer(INetProperty netProperty, long userId) throws QException {
        log().info("Начать работу с вызванным кастомером.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        send(netProperty, Uses.TASK_START_CUSTOMER, params);
    }

    /**
     * Закончить работу с вызванным кастомером.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param userId      оператор
     * @param customerId  переключиться на этого при параллельном приеме, NULL если переключаться не надо
     * @param resultId    результат обработки кастомера юзером.
     * @param comments    это если закончили работать с редиректенным и его нужно вернуть
     * @return получим ровно того же кастомера с которым работали.
     */
    public static QCustomer getFinishCustomer(INetProperty netProperty, long userId, Long customerId, Long resultId, String comments) throws QException {
        log().info("Закончить работу с вызванным кастомером.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.customerId = customerId;
        params.resultId = resultId;
        params.textData = comments;
        final String res = send(netProperty, Uses.TASK_FINISH_CUSTOMER, params);
        final RpcStandInService rpc = AJsonRPC20.demarshal(res, RpcStandInService.class);
        return rpc.getResult();
    }

    /**
     * Переадресовать клиента в другую очередь.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param userId      оператор
     * @param customerId  переключиться на этого при параллельном приеме, NULL если переключаться не надо
     * @param serviceId   услуга
     * @param requestBack возвращать или нет
     * @param resultId    результат обработки кастомера юзером.
     * @param comments    комментарии при редиректе
     */
    public static void redirectCustomer(INetProperty netProperty, long userId, Long customerId,
                                        long serviceId, boolean requestBack, String comments, Long resultId) throws QException {
        log().info("Переадресовать клиента в другую очередь.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.customerId = customerId;
        params.serviceId = serviceId;
        params.requestBack = requestBack;
        params.resultId = resultId;
        params.textData = comments;
        send(netProperty, Uses.TASK_REDIRECT_CUSTOMER, params);
    }

    /**
     * Получение описания состояния сервера.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @return XML-ответ
     */
    public static LinkedList<ServiceInfo> getServerState(INetProperty netProperty) throws QException {
        log().info("Получение описания состояния сервера.");
        // загрузим ответ
        String res = send(netProperty, Uses.TASK_SERVER_STATE, null);
        final RpcGetServerState rpc = AJsonRPC20.demarshal(res, RpcGetServerState.class);
        return rpc.getResult();
    }

    /**
     * Получение описания состояния пункта регистрации.
     *
     * @param netProperty        сеть.        параметры соединения с пунктом регистрации
     * @param message            что-то вроде названия команды для пункта регистрации
     * @param dropTicketsCounter сбросить счетчик выданных талонов или нет
     * @return некий ответ от пункта регистрации, вроде прям как строка для вывода
     */
    public static String getWelcomeState(INetProperty netProperty, String message, boolean dropTicketsCounter) {
        log().info("Получение описания состояния пункта регистрации.");
        // загрузим ответ
        String res = null;
        final CmdParams params = new CmdParams();
        params.dropTicketsCounter = dropTicketsCounter;
        try {
            res = send(netProperty, message, params);
        } catch (QException e) {
            throw new ClientException(locMes("bad_response") + "\n" + e.toString());
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetSrt rpc;
        try {
            rpc = gson.fromJson(res, RpcGetSrt.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Добавить сервис в список обслуживаемых юзером использую параметры. Используется при добавлении на горячую.
     *
     * @param netProperty сеть. параметры соединения с пунктом регистрации
     * @param serviceId   услуга
     * @param userId      оператор
     * @param coeff       приоритетность.
     * @return содержить строковое сообщение о результате.
     */
    public static String setServiseFire(INetProperty netProperty, long serviceId, long userId, int coeff) {
        log().info("Привязка услуги пользователю на горячую.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.serviceId = serviceId;
        params.coeff = coeff;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_SET_SERVICE_FIRE, params);
        } catch (QException e) {
            throw new ClientException(locMes("bad_response") + "\n" + e.toString());
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetSrt rpc;
        try {
            rpc = gson.fromJson(res, RpcGetSrt.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Удалить сервис из списока обслуживаемых юзером использую параметры. Используется при добавлении на горячую.
     *
     * @param netProperty сеть. параметры соединения с пунктом регистрации
     * @param serviceId   услуга
     * @param userId      пользователь
     * @return содержить строковое сообщение о результате.
     */
    public static String deleteServiseFire(INetProperty netProperty, long serviceId, long userId) {
        log().info("Удаление услуги пользователю на горячую.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.serviceId = serviceId;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_DELETE_SERVICE_FIRE, params);
        } catch (QException e) {
            throw new ClientException(locMes("bad_response") + "\n" + e.toString());
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetSrt rpc;
        try {
            rpc = gson.fromJson(res, RpcGetSrt.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получение конфигурации главного табло - ЖК или плазмы. Это XML-файл лежащий в папку приложения mainboard.xml
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @return корень XML-файла mainboard.xml
     * @throws DocumentException принятый текст может не преобразоваться в XML
     */
    public static Element getBoardConfig(INetProperty netProperty) throws DocumentException {
        log().info("Получение конфигурации главного табло - ЖК или плазмы.");
        // загрузим ответ
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_BOARD_CONFIG, null);
        } catch (QException e) {
            throw new ClientException(locMes("bad_response") + "\n" + e.toString());
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetSrt rpc;
        try {
            rpc = gson.fromJson(res, RpcGetSrt.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return DocumentHelper.parseText(rpc.getResult()).getRootElement();
    }

    /**
     * Сохранение конфигурации главного табло - ЖК или плазмы. Это XML-файл лежащий в папку приложения mainboard.xml
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param boardConfig корень xml настроек главного табло.
     */
    public static void saveBoardConfig(INetProperty netProperty, Element boardConfig) {
        log().info("Сохранение конфигурации главного табло - ЖК или плазмы.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.textData = boardConfig.asXML();
        try {
            send(netProperty, Uses.TASK_SAVE_BOARD_CONFIG, params);
        } catch (QException e) {
            throw new ClientException(locMes("bad_response") + "\n" + e.toString());
        }
    }

    /**
     * Получение дневной таблици с данными для предварительной записи включающими информацию по занятым временам и свободным.
     *
     * @param netProperty      сеть.      netProperty параметры соединения с сервером.
     * @param serviceId        услуга, в которую пытаемся встать.
     * @param date             день недели за который нужны данные.
     * @param advancedCustomer ID авторизованного кастомера
     * @return класс с параметрами и списком времен
     */
    public static RpcGetGridOfDay.GridDayAndParams getPreGridOfDay(INetProperty netProperty, long serviceId, Date date, long advancedCustomer) {
        log().info("Получить таблицу дня");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.date = date.getTime();
        params.customerId = advancedCustomer;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_GRID_OF_DAY, params);
        } catch (QException e) {// вывод исключений
            throw new ClientException(locMes("command_error2"), e);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetGridOfDay rpc;
        try {
            rpc = gson.fromJson(res, RpcGetGridOfDay.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получение недельной таблици с данными для предварительной записи.
     *
     * @param netProperty      сеть.      netProperty параметры соединения с сервером.
     * @param serviceId        услуга, в которую пытаемся встать.
     * @param date             первый день недели за которую нужны данные.
     * @param advancedCustomer ID авторизованного кастомера
     * @return класс с параметрами и списком времен
     */
    public static RpcGetGridOfWeek.GridAndParams getGridOfWeek(INetProperty netProperty, long serviceId, Date date, long advancedCustomer) {
        log().info("Получить таблицу");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.date = date.getTime();
        params.customerId = advancedCustomer;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_GRID_OF_WEEK, params);
        } catch (QException e) {// вывод исключений
            throw new ClientException(locMes("command_error2"), e);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetGridOfWeek rpc;
        try {
            rpc = gson.fromJson(res, RpcGetGridOfWeek.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Предварительная запись в очередь.
     *
     * @param netProperty      сеть.      netProperty параметры соединения с сервером.
     * @param serviceId        услуга, в которую пытаемся встать.
     * @param date             Дата записи
     * @param advancedCustomer ID авторизованного кастомер. -1 если нет авторизации
     * @param inputData        введеные по требованию услуги данные клиентом, может быть null если не вводили
     * @param comments         комментарий по предварительно ставящемуся клиенту если ставят из админки или приемной
     * @return предварительный кастомер
     */
    public static QAdvanceCustomer standInServiceAdvance(INetProperty netProperty, long serviceId, Date date, long advancedCustomer, String inputData, String comments) {
        log().info("Записать предварительно в очередь.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.date = date.getTime();
        params.customerId = advancedCustomer;
        params.textData = inputData;
        params.comments = comments;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_ADVANCE_STAND_IN, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetAdvanceCustomer rpc;
        try {
            rpc = gson.fromJson(res, RpcGetAdvanceCustomer.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Предварительная запись в очередь.
     *
     * @param netProperty сеть. netProperty параметры соединения с сервером.
     * @param advanceID   идентификатор предварительно записанного.
     * @return XML-ответ.
     */
    public static RpcStandInService standAndCheckAdvance(INetProperty netProperty, Long advanceID) {
        log().info("Постановка предварительно записанных в очередь.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.customerId = advanceID;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_ADVANCE_CHECK_AND_STAND, params);
        } catch (QException e) {// вывод исключений
            throw new ClientException(locMes("command_error2"), e);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcStandInService rpc;
        try {
            rpc = gson.fromJson(res, RpcStandInService.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc;
    }

    /**
     * Удаление предварительной записи в очередь.
     *
     * @param netProperty сеть. netProperty параметры соединения с сервером.
     * @param advanceID   идентификатор предварительно записанного.
     * @return XML-ответ.
     */
    public static JsonRPC20OK removeAdvancedCustomer(INetProperty netProperty, Long advanceID) {
        log().info("Удаление предварительно записанных в очередь.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.customerId = advanceID;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_REMOVE_ADVANCE_CUSTOMER, params);
        } catch (QException e) {// вывод исключений
            throw new ClientException(locMes("command_error2"), e);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final JsonRPC20OK rpc;
        try {
            rpc = gson.fromJson(res, JsonRPC20OK.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc;
    }

    /**
     * Рестарт сервера.
     *
     * @param netProperty сеть. параметры соединения с сервером
     */
    public static void restartServer(INetProperty netProperty) {
        log().info("Команда на рестарт сервера.");
        try {
            send(netProperty, Uses.TASK_RESTART, null);
        } catch (QException e) {// вывод исключений
            throw new ClientException(locMes("command_error2"), e);
        }
    }

    /**
     * Получение Дерева отзывов.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @return XML-ответ
     */
    public static QRespItem getResporseList(INetProperty netProperty) {
        log().info("Команда на получение дерева отзывов.");
        String res = null;
        try {
            // загрузим ответ
            res = send(netProperty, Uses.TASK_GET_RESPONSE_LIST, null);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        if (res == null) {
            return null;
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetRespTree rpc;
        try {
            rpc = gson.fromJson(res, RpcGetRespTree.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Оставить отзыв.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param serviceID   услуга, может быть null
     * @param userID      оператор, может быть null
     * @param customerID  кастомер
     * @param clientData  номер талона, не null
     * @param resp        выбранн отзыв
     */
    public static void setResponseAnswer(INetProperty netProperty, QRespItem resp, Long userID, Long serviceID, Long customerID, String clientData) {
        log().info("Отправка выбранного отзыва.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.responseId = resp.getId();
        params.serviceId = serviceID;
        params.userId = userID;
        params.customerId = customerID;
        params.textData = clientData;
        params.comments = resp.data;
        try {
            send(netProperty, Uses.TASK_SET_RESPONSE_ANSWER, params);
        } catch (QException ex) {// вывод исключений
            throw new ServerException(locMes("command_error"), ex);
        }
    }

    /**
     * Получение информационного дерева.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @return XML-ответ.
     */
    public static QInfoItem getInfoTree(INetProperty netProperty) {
        log().info("Команда на получение информационного дерева.");
        String res = null;
        try {
            // загрузим ответ
            res = send(netProperty, Uses.TASK_GET_INFO_TREE, null);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        if (res == null) {
            return null;
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetInfoTree rpc;
        try {
            rpc = gson.fromJson(res, RpcGetInfoTree.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получение описания залогинившегося юзера.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param id          авторизация клиента.
     * @return XML-ответ.
     */
    public static QAuthorizationCustomer getClientAuthorization(INetProperty netProperty, String id) {
        log().info("Получение описания авторизованного пользователя.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.clientAuthId = id;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_CLIENT_AUTHORIZATION, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetAuthorizCustomer rpc;
        try {
            rpc = gson.fromJson(res, RpcGetAuthorizCustomer.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получение списка возможных результатов работы с клиентом.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @return свисок возможных завершений работы
     */
    public static LinkedList<QResult> getResultsList(INetProperty netProperty) {
        log().info("Команда на получение списка возможных результатов работы с клиентом.");
        final String res;
        try {
            // загрузим ответ RpcGetResultsList
            res = send(netProperty, Uses.TASK_GET_RESULTS_LIST, null);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetResultsList rpc;
        try {
            rpc = gson.fromJson(res, RpcGetResultsList.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Изменение приоритета кастомеру.
     *
     * @param netProperty сеть. параметры соединения с сервером.
     * @param prioritet   приоритет
     * @param customer    кастомер
     * @return Текстовый ответ о результате
     */
    public static String setCustomerPriority(INetProperty netProperty, int prioritet, String customer) {
        log().info("Команда на повышение приоритета кастомеру.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.priority = prioritet;
        params.clientAuthId = customer;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_SET_CUSTOMER_PRIORITY, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetSrt rpc;
        try {
            rpc = gson.fromJson(res, RpcGetSrt.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Пробить номер клиента. Стоит в очереди или отложен или вообще не найден.
     *
     * @param netProperty    сеть.    параметры соединения с сервером
     * @param customerNumber номер посетителя.
     * @return Текстовый ответ о результате
     */
    public static TicketHistory checkCustomerNumber(INetProperty netProperty, String customerNumber) {
        log().info("Команда проверки номера кастомера.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.clientAuthId = customerNumber;
        final String res;
        try {
            res = send(netProperty, Uses.TASK_CHECK_CUSTOMER_NUMBER, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetTicketHistory rpc;
        try {
            rpc = gson.fromJson(res, RpcGetTicketHistory.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получить список отложенных кастомеров.
     *
     * @param netProperty сеть.
     * @return список отложенных кастомеров.
     */
    public static LinkedList<QCustomer> getPostponedPoolInfo(INetProperty netProperty) {
        log().info("Команда на обновление пула отложенных.");
        // загрузим ответ
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_POSTPONED_POOL, null);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetPostponedPoolInfo rpc;
        try {
            rpc = gson.fromJson(res, RpcGetPostponedPoolInfo.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Получить список забаненных введенных данных.
     *
     * @param netProperty сеть.
     * @return список отложенных кастомеров
     */
    public static LinkedList<String> getBanedList(INetProperty netProperty) {
        log().info("Команда получение списка забаненных.");
        // загрузим ответ
        final String res;
        try {
            res = send(netProperty, Uses.TASK_GET_BAN_LIST, null);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcBanList rpc;
        try {
            rpc = gson.fromJson(res, RpcBanList.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getBanList();
    }

    /**
     * Вызов отложенного кастомера.
     *
     * @param netProperty сеть.
     * @param userId      id юзера который вызывает
     * @param id          это ID кастомера которого вызываем из пула отложенных, оно есть т.к. с качстомером давно работаем
     * @return Вызванный из отложенных кастомер.
     */
    public static QCustomer invitePostponeCustomer(INetProperty netProperty, long userId, Long id) {
        log().info("Команда на вызов кастомера из пула отложенных.");
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.customerId = id;
        // загрузим ответ
        final String res;
        try {
            res = send(netProperty, Uses.TASK_INVITE_POSTPONED, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }

        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcInviteCustomer rpc;
        try {
            rpc = gson.fromJson(res, RpcInviteCustomer.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Рестарт главного табло.
     *
     * @param serverNetProperty сеть к серверу.
     */
    public static void restartMainTablo(INetProperty serverNetProperty) {
        log().info("Команда на рестарт главного табло.");
        // загрузим ответ
        try {
            send(serverNetProperty, Uses.TASK_RESTART_MAIN_TABLO, null);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
    }

    /**
     * Изменение приоритетов услуг оператором.
     *
     * @param netProperty сеть.
     * @param userId      id юзера который вызывает.
     * @param smartData   датка
     */
    public static void changeFlexPriority(INetProperty netProperty, long userId, String smartData) {
        log().info("Изменение приоритетов услуг оператором.");
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.textData = smartData;
        // загрузим ответ
        try {
            send(netProperty, Uses.TASK_CHANGE_FLEX_PRIORITY, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
    }

    /**
     * Изменение бегущей строки на табло из админской проги.
     *
     * @param netProperty сеть. параметры соединения с сервером
     * @param text        новая строка
     * @param nameSection именованое секции параметров.
     */
    public static void setRunningText(INetProperty netProperty, String text, String nameSection) {
        log().info("Получение описания авторизованного пользователя.");
        // загрузим ответ
        final CmdParams params = new CmdParams();
        params.textData = text;
        params.infoItemName = nameSection;
        try {
            send(netProperty, Uses.TASK_CHANGE_RUNNING_TEXT_ON_BOARD, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
    }

    /**
     * Получить норрмативы.
     *
     * @param netProperty сеть.
     * @return класс нормативов.
     */
    public static QStandards getStandards(INetProperty netProperty) throws QException {
        log().info("Команда получение нормативов.");
        // загрузим ответ
        final String res = send(netProperty, Uses.TASK_GET_STANDARDS, null);
        final RpcGetStandards rpc = AJsonRPC20.demarshal(res, RpcGetStandards.class);
        return rpc.getResult();
    }

    /**
     * Изменение приоритетов услуг оператором.
     *
     * @param netProperty сеть.
     * @param userId      id юзера который вызывает.
     * @param lock        блокируем или нет.
     * @return
     */
    public static boolean setBussy(INetProperty netProperty, long userId, boolean lock) throws QException {
        log().info("Изменение приоритетов услуг оператором.");
        final CmdParams params = new CmdParams();
        params.userId = userId;
        params.requestBack = lock;
        // загрузим ответ
        final String res = send(netProperty, Uses.TASK_SET_BUSSY, params);
        final RpcGetBool rpc = AJsonRPC20.demarshal(res, RpcGetBool.class);
        return rpc.getResult();
    }

    /**
     * Получить параметры из ДБ из сервера.
     *
     * @param netProperty сеть.
     * @return мапа с секциями
     */
    public static LinkedHashMap<String, ServerProps.Section> getProperties(INetProperty netProperty) throws QException {
        log().info("Получить параметры.");
        final CmdParams params = new CmdParams();
        // загрузим ответ
        final String res = send(netProperty, Uses.TASK_GET_PROPERTIES, params);
        final RpcGetProperties rpc = AJsonRPC20.demarshal(res, RpcGetProperties.class);
        return rpc.getResult();
    }

    /**
     * Изменить и сохранить параметеры в ДБ на сервере.
     *
     * @param netProperty сеть.
     * @param properties  параметры.
     * @return Список свежих свойств
     */
    public static LinkedHashMap<String, ServerProps.Section> saveProperties(INetProperty netProperty, List<QProperty> properties) {
        log().info("Изменить и сохранить параметеры в ДБ на сервере.");
        final CmdParams params = new CmdParams();
        params.properties = properties;
        // загрузим ответ
        final String res;
        try {
            res = send(netProperty, Uses.TASK_INIT_PROPERTIES, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetProperties rpc;
        try {
            rpc = gson.fromJson(res, RpcGetProperties.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Если таких параметров нет, то создать их в ДБ на сервере.
     *
     * @param netProperty сеть.
     * @param properties  параметры.
     * @return Список свежих свойств.
     */
    public static LinkedHashMap<String, ServerProps.Section> initProperties(INetProperty netProperty, List<QProperty> properties) {
        log().info("Если таких параметров нет, то создать их в ДБ на сервере.");
        final CmdParams params = new CmdParams();
        params.properties = properties;
        // загрузим ответ
        final String res;
        try {
            res = send(netProperty, Uses.TASK_SAVE_PROPERTIES, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
        final Gson gson = GsonPool.getInstance().borrowGson();
        final RpcGetProperties rpc;
        try {
            rpc = gson.fromJson(res, RpcGetProperties.class);
        } catch (JsonSyntaxException ex) {
            throw new ClientException(locMes("bad_response") + "\n" + ex.toString());
        } finally {
            GsonPool.getInstance().returnGson(gson);
        }
        return rpc.getResult();
    }

    /**
     * Выставить параметры рулона с билетами на бабине.
     *
     * @param netProperty сеть. сеть.
     * @param serviceId   эту услугу надо обновить
     * @param first       в новом рулоне он первый
     * @param last        рулон длинной такой
     * @param current     сейчас в рулоне этот текущий номер
     */
    public static void initRoll(INetProperty netProperty, Long serviceId, int first, int last, int current) {
        log().info("Изменение приоритетов услуг оператором.");
        final CmdParams params = new CmdParams();
        params.serviceId = serviceId;
        params.first = first;
        params.last = last;
        params.current = current;
        // отправим запрос
        try {
            send(netProperty, Uses.TASK_REINIT_ROLL, params);
        } catch (QException ex) {// вывод исключений
            throw new ClientException(locMes("command_error"), ex);
        }
    }
}
