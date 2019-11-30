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
package ru.apertum.qsystem.server.htmlboard;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import ru.apertum.qsystem.client.forms.AFBoardRedactor;
import ru.apertum.qsystem.client.forms.FParamsEditor;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.QServer;
import ru.apertum.qsystem.server.controller.IIndicatorBoard;
import ru.apertum.qsystem.server.model.QPlanService;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.QUser;
import ru.apertum.qsystem.server.model.QUserList;

import javax.imageio.ImageIO;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Вывод информации на мониторы. Класс-менеджер вывода информации на общее табло в виде монитора.
 *
 * @author Evgeniy Egorov
 */
public class QIndicatorHtmlboard implements IIndicatorBoard {

    public static final String CONTENT_FILE_PATH = "config/html_main_board/content.html";
    private static final String VALUE = "Значение";
    private static final String BLINK = "blink";
    public static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    protected FHtmlBoard indicatorBoard = null;

    /**
     * Замена якорей на реальные значения.
     *
     * @param fileName Это шаблон
     * @return шаблон с замененными реальными значениями
     */
    private String prepareContent(String fileName) {
        final HashMap<String, Object> map = new HashMap<>();
        //****************************************************************************************************************************************************
        //****************************************************************************************************************************************************
        // Для начала отрисуем ближайших по якорям {{next1}}{{next2}}{{next3}}...{{nextN}}:
        // Построем всех ближайших
        prepareNext(map);

        //*****************************************************************************************************************************************************
        //*****************************************************************************************************************************************************
        // Теперь таблица вызванных {inv1_blink} {inv1_ticket} {inv1_point}.
        // Нужно найти всех вызванных и обрабатываемых, посмотреть их статус и заменить данные и id для мигания вызванных
        prepareInvited(map);

        //*****************************************************************************************************************************************************
        //*****************************************************************************************************************************************************
        // Теперь заменим стоящих к операторам
        prepareUsers(map);


        //****************************************************************************************************************************************************
        //****************************************************************************************************************************************************
        // Все производим замену шаблонизатором.
        final Mustache mustache = MUSTACHE_FACTORY.compile(fileName);
        final StringWriter writer = new StringWriter();
        final String html = mustache.execute(writer, map).toString();
        if (html != null) {
            return html;
        } else {
            return "<html><body>Error tablo.</body>";
        }
    }

    private void prepareUsers(HashMap<String, Object> map) {
        final ArrayList<String> invList = new ArrayList<>();
        for (QUser user : QUserList.getInstance().getItems()) {
            int queue = 1;
            final String id = HtmlBoardProps.getInstance().getId(user.getPoint());
            if (user.getCustomer() != null) {
                if (user.getCustomer().getState() == CustomerState.STATE_INVITED || user.getCustomer().getState() == CustomerState.STATE_INVITED_SECONDARY) {
                    invList.add(id);
                    map.put(id + "|blink", BLINK);
                    map.put(id + "|queue" + queue++, user.getCustomer().getFullNumber());
                } else {
                    map.put(id + "|blink", "notblink");
                }
                map.put(id + "|service", user.getCustomer().getService().getName());
                map.put(id + "|discription", user.getCustomer().getService().getDescription());
                map.put(id + "|user", user.getName());
                map.put(id + "|ext", user.getPointExt());
            }
            map.put(id + "|point", user.getPoint());

            // для получения правильной очередности хвоста
            final PriorityQueue<QCustomer> custs = new PriorityQueue<>();
            for (QPlanService pser : user.getPlanServices()) {
                final QService ser = QServiceTree.getInstance().getById(pser.getService().getId());
                ser.getClients().stream().forEach(custs::offer);
                while (!custs.isEmpty()) {
                    map.put(id + "|queue" + queue++, custs.poll().getFullNumber());
                }
            }
        }
    }

    private void prepareNext(HashMap<String, Object> map) {
        final LinkedList<String> nexts = new LinkedList<>(); // Это все ближайшие по порядку
        final PriorityQueue<QCustomer> customers = new PriorityQueue<>();
        QServiceTree.getInstance().getNodes().stream().filter(QService::isLeaf).forEach(service ->
                service.getClients().stream().forEach(customers::add)
        );
        QCustomer qCust = customers.poll();
        while (qCust != null) {
            nexts.add(qCust.getFullNumber());
            qCust = customers.poll();
        }
        // Теперь ближайшие в списке, произведем замены по якорям {{next1}}{{next2}}{{next3}}...{{nextN}}.
        int posNext = 1;
        for (String next : nexts) {
            map.put("next" + posNext++, next);
        }
    }

    private void prepareInvited(HashMap<String, Object> map) {
        final LinkedList<QCustomer> onBosrd = new LinkedList<>();
        for (QUser user : QUserList.getInstance().getItems()) {
            if (user.getCustomer() != null) {
                int pos = 0;
                for (QCustomer qCustomer : onBosrd) {
                    if (qCustomer.getCallTime().before(user.getCustomer().getCallTime())) {
                        break;
                    }
                    pos++;
                }
                onBosrd.add(pos, user.getCustomer());
            }
        }
        // Заменяем строки вызванных
        int posOnBoard = 1;
        for (QCustomer bCust : onBosrd) {
            map.put("ticket" + posOnBoard, bCust.getFullNumber());
            map.put("point" + posOnBoard, bCust.getUser().getPoint());
            map.put("ext" + posOnBoard, bCust.getUser().getPointExt());
            if (bCust.getState().equals(CustomerState.STATE_INVITED) || bCust.getState().equals(CustomerState.STATE_INVITED_SECONDARY)) {
                map.put(BLINK + posOnBoard, BLINK);
            } else {
                map.put(BLINK + posOnBoard, "notblink");
            }
            posOnBoard++;
        }
    }

    /**
     * Создадим форму, спозиционируем, сконфигурируем и покажем.
     */
    protected void initIndicatorBoard() {
        if (!Paths.get(CONTENT_FILE_PATH).toFile().exists()) {
            throw new ServerException("Не найден " + CONTENT_FILE_PATH, new FileNotFoundException(CONTENT_FILE_PATH));
        }

        if (indicatorBoard == null) {
            indicatorBoard = new FHtmlBoard();
            try {
                indicatorBoard.setIconImage(ImageIO.read(QServer.class.getResource("/ru/apertum/qsystem/client/forms/resources/recent.png")));
            } catch (IOException ex) {
                QLog.l().logger().error(ex);
            }
            // Определим форму нв монитор
            indicatorBoard.toPosition(QConfig.cfg().isDebug(), 20, 20);

            indicatorBoard.loadContent(prepareContent(CONTENT_FILE_PATH));

            java.awt.EventQueue.invokeLater(() -> indicatorBoard.setVisible(true));
        } else {
            indicatorBoard.loadContent(prepareContent(CONTENT_FILE_PATH));
        }
    }

    public QIndicatorHtmlboard() {
        QLog.l().logger().info("Создание HTML табло для телевизоров или мониторов. Шаблон табло в \"" + CONTENT_FILE_PATH + "\"");
    }

    @Override
    public Element getConfig() {
        StringBuilder tr = new StringBuilder("<Параметры>\n")
                .append("   <Параметер Наименование=\"top.size\" Тип=\"1\" Значение=\"" + HtmlBoardProps.getInstance().topSize)
                .append("\"/>\n   <Параметер Наименование=\"top.url\" Тип=\"3\" Значение=\"" + HtmlBoardProps.getInstance().topUrl)
                .append("\"/>\n   <Параметер Наименование=\"left.size\" Тип=\"1\" Значение=\"" + HtmlBoardProps.getInstance().leftSize)
                .append("\"/>\n   <Параметер Наименование=\"left.url\" Тип=\"3\" Значение=\"" + HtmlBoardProps.getInstance().leftUrl)
                .append("\"/>\n   <Параметер Наименование=\"right.size\" Тип=\"1\" Значение=\"" + HtmlBoardProps.getInstance().rightSize)
                .append("\"/>\n   <Параметер Наименование=\"right.url\" Тип=\"3\" Значение=\"" + HtmlBoardProps.getInstance().rightUrl)
                .append("\"/>\n   <Параметер Наименование=\"bottom.size\" Тип=\"1\" Значение=\"" + HtmlBoardProps.getInstance().bottomSize)
                .append("\"/>\n   <Параметер Наименование=\"bottom.url\" Тип=\"3\" Значение=\"" + HtmlBoardProps.getInstance().bottomUrl)
                .append("\"/>\n   <Параметер Наименование=\"need_reload\" Тип=\"4\" Значение=\"" + (HtmlBoardProps.getInstance().needReload ? "1" : "0") + "\"/>\n");

        for (String key : HtmlBoardProps.getInstance().getAddrs().keySet()) {
            tr.append("   <Параметер Наименование=\"" + key + "\" Тип=\"3\" Значение=\"" + HtmlBoardProps.getInstance().getAddrs().get(key) + "\" " + Uses.TAG_BOARD_READ_ONLY + "=\"true\"/>\n");
        }

        tr.append("</Параметры>");
        final Document document;
        try {
            document = DocumentHelper.parseText(tr.toString());
        } catch (DocumentException ex) {
            throw new ServerException(ex);
        }
        return document.getRootElement();
    }

    @Override
    public void saveConfig(Element element) {

        final List<Element> elist = element.elements("Параметер");

        elist.forEach(elem -> {
            switch (elem.attributeValue("Наименование")) {
                case "top.size":
                    HtmlBoardProps.getInstance().topSize = Integer.parseInt(elem.attributeValue(VALUE));
                    break;
                case "top.url":
                    HtmlBoardProps.getInstance().topUrl = elem.attributeValue(VALUE);
                    break;
                case "left.size":
                    HtmlBoardProps.getInstance().leftSize = Integer.parseInt(elem.attributeValue(VALUE));
                    break;
                case "left.url":
                    HtmlBoardProps.getInstance().leftUrl = elem.attributeValue(VALUE);
                    break;
                case "right.size":
                    HtmlBoardProps.getInstance().rightSize = Integer.parseInt(elem.attributeValue(VALUE));
                    break;
                case "right.url":
                    HtmlBoardProps.getInstance().rightUrl = elem.attributeValue(VALUE);
                    break;
                case "bottom.size":
                    HtmlBoardProps.getInstance().bottomSize = Integer.parseInt(elem.attributeValue(VALUE));
                    break;
                case "bottom.url":
                    HtmlBoardProps.getInstance().bottomUrl = elem.attributeValue(VALUE);
                    break;
                case "need_reload":
                    HtmlBoardProps.getInstance().needReload = "1".equals(element.attributeValue(VALUE));
                    break;
                default:
                    QLog.l().logger().error("Параметер для HTML очень странный: " + elem.attributeValue("Наименование"));
            }
            HtmlBoardProps.getInstance().saveProps();
        });
    }

    @Override
    public AFBoardRedactor getRedactor() {
        if (boardConfig == null) {
            boardConfig = FParamsEditor.getParamsEditor(null, false);
        }
        return boardConfig;
    }

    /**
     * Используемая ссылка на диалоговое окно. Singleton
     */
    private FParamsEditor boardConfig;

    @Override
    public void showBoard() {
        QLog.l().logger().trace("Показываем HTML табло");
        initIndicatorBoard();
        QLog.l().logger().trace("HTML табло должно уже показаться.");
    }

    /**
     * Выключить информационное табло.
     */
    @Override
    public synchronized void close() {
        QLog.l().logger().trace("Закрываем HTML табло");
        if (indicatorBoard != null) {
            indicatorBoard.setVisible(false);
            indicatorBoard = null;
        }
    }

    @Override
    public void refresh() {
        QLog.l().logger().trace("Обновляем HTML табло");
        close();
        indicatorBoard = null;
        initIndicatorBoard();
    }

    @Override
    public void clear() {
        // Тут чичтить нечего.
    }

    @Override
    public void setConfigFile(String configFile) {
        throw new UnsupportedOperationException("Not  supported yet.");
    }

    @Override
    public String getDescription() {
        return "Плагин табло со стационарными позициями.";
    }

    @Override
    public long getUID() {
        return 2;
    }

    @Override
    public Object getBoardForm() {
        return indicatorBoard;
    }

    @Override
    public void customerStandIn(QCustomer customer) {
        QLog.l().logger().trace("Поставили кастомера на BS табло");
        if (indicatorBoard != null) {
            if (HtmlBoardProps.getInstance().isNeedReload()) {
                indicatorBoard.loadContent(prepareContent(CONTENT_FILE_PATH));
            }
            indicatorBoard.getBfx().executeJavascript("customerStandIn(" + makeParam(customer) + ")");
        }
    }

    /**
     * Переопределено что бы вызвать появление таблички с номером вызванного поверх главного табло.
     */
    @Override
    public synchronized void inviteCustomer(QUser user, QCustomer customer) {
        QLog.l().logger().trace("Приглшием кастомера на BS табло");
        if (indicatorBoard != null) {
            if (HtmlBoardProps.getInstance().isNeedReload()) {
                indicatorBoard.loadContent(prepareContent(CONTENT_FILE_PATH));
            }
            indicatorBoard.getBfx().executeJavascript("inviteCustomer(" + makeParam(user, customer) + ")");
        }
    }

    @Override
    public void workCustomer(QUser user) {
        QLog.l().logger().trace("Работа с кастомером на BS табло");
        if (indicatorBoard != null) {
            if (HtmlBoardProps.getInstance().isNeedReload()) {
                indicatorBoard.loadContent(prepareContent(CONTENT_FILE_PATH));
            }
            indicatorBoard.getBfx().executeJavascript("workCustomer(" + makeParam(user, user.getCustomer()) + ")");
        }
    }

    @Override
    public void killCustomer(QUser user) {
        QLog.l().logger().trace("Убираем кастомера на BS табло");
        if (indicatorBoard != null) {
            if (HtmlBoardProps.getInstance().isNeedReload()) {
                indicatorBoard.loadContent(prepareContent(CONTENT_FILE_PATH));
            }
            indicatorBoard.getBfx().executeJavascript("killCustomer(" + makeParam(user, user.getCustomer() == null ? user.getShadow().getOldCustomer() : user.getCustomer()) + ")");
        }
    }

    /**
     * Пример.
     * {"user":{"name":"Ivanov", "point":"222", "ext":"<b>ext field</b>"}, "servece":{"name":"Spravka", prefix:"A", "description":"Long horn"},
     * "customer":{prefix:"A", "number":"159", "data":"null"}}
     */
    private String makeParam(QUser user, QCustomer customer) {
        //{"user":{"name":"Ivanov", "point":"222", "ext":"<b>ext field</b>"},
        // "servece":{"name":"Spravka", prefix:"A", "description":"Long horn"}, "customer":{prefix:"A", "number":"159", "data":"null"}} )
        return "{\"user\":{\"name\":\"" + user.getName() + "\", \"point\":\"" + user.getPoint() + "\"}, "
                + "\"servece\":{\"name\":\"" + customer.getService().getName() + "\", prefix:\"" + customer.getService().getPrefix()
                + "\", \"description\":\"" + customer.getService().getDescription() + "\"},"
                + " \"customer\":{prefix:\"" + customer.getPrefix() + "\", \"number\":\"" + customer.getNumber() + "\", \"data\":\"" + customer.getInputData() + "\"}}";
    }

    private String makeParam(QCustomer customer) {
        //{"user":{"name":"Ivanov", "point":"222", "ext":"<b>ext field</b>"},
        // "servece":{"name":"Spravka", prefix:"A", "description":"Long horn"}, "customer":{prefix:"A", "number":"159", "data":"null"}} )
        return "{\"servece\":{\"name\":\"" + customer.getService().getName() + "\", prefix:\"" + customer.getService().getPrefix()
                + "\", \"description\":\"" + customer.getService().getDescription() + "\"},"
                + " \"customer\":{prefix:\"" + customer.getPrefix() + "\", \"number\":\"" + customer.getNumber() + "\", \"data\":\"" + customer.getInputData() + "\"}}";
    }
}
