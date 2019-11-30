/*
 *  Copyright (C) 2011 egorov
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
package ru.apertum.qsystem.server;

import org.dom4j.Element;
import ru.apertum.qsystem.client.forms.AFBoardRedactor;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.controller.IIndicatorBoard;
import ru.apertum.qsystem.server.controller.QIndicatorBoardMonitorJaxb;
import ru.apertum.qsystem.server.htmlboard.QIndicatorHtmlboard;
import ru.apertum.qsystem.server.model.QUser;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Главное табло. Точка доступа.
 *
 * @author egorov
 */
public class MainBoard {

    private MainBoard() {
    }

    /**
     * Возвращает кастомную реализация, которая может подгрузиться из плагина.
     * Полная изоляция реализаций друг от друга.
     *
     * @return Реализация интерфейса гдавного табло.
     */
    public static IIndicatorBoard getInstance() {
        return MainBoardHolder.INSTANCE;
    }

    private static class MainBoardHolder {

        private static final IIndicatorBoard INSTANCE = setup();

        private static IIndicatorBoard setup() {
            if (ServerProps.getInstance().getProperty(IIndicatorBoard.SECTION, IIndicatorBoard.SHOW, "1").toLowerCase().matches("0|(false)|(no)|(not)|(hide)|(hiden)|(off)")) {
                return new MultyBoard(new ArrayList<>());
            }
            final boolean bClassicType = IIndicatorBoard.CLASSIC.equalsIgnoreCase(ServerProps.getInstance()
                    .getProperty(IIndicatorBoard.SECTION, IIndicatorBoard.PARAMETER, IIndicatorBoard.CLASSIC));
            final List<IIndicatorBoard> boards = new ArrayList<>();
            if (bClassicType) {
                boards.add(new QIndicatorBoardMonitorJaxb());
                boards.get(0).setConfigFile("config/mainboard.xml");
            } else {
                boards.add(new QIndicatorHtmlboard());
            }
            // поддержка расширяемости плагинами
            for (final IIndicatorBoard board : ServiceLoader.load(IIndicatorBoard.class)) {
                QLog.l().logger().info("Main boards UID=\"{}\" is created. Description: {}", board.getUID(), board.getDescription());
                boards.add(board);
            }
            return new MultyBoard(boards);
        }
    }

    /**
     * Т.к. таблух может быть много, например главное и плагинное, то вот делаем такого разветвителя.
     */
    private static final class MultyBoard implements IIndicatorBoard {

        final List<IIndicatorBoard> boards;

        private MultyBoard(List<IIndicatorBoard> boards) {
            this.boards = boards;
        }


        @Override
        public void inviteCustomer(QUser user, QCustomer customer) {
            boards.forEach(board -> board.inviteCustomer(user, customer));
        }

        @Override
        public void workCustomer(QUser user) {
            boards.forEach(board -> board.workCustomer(user));
        }

        @Override
        public void killCustomer(QUser user) {
            boards.forEach(board -> board.killCustomer(user));
        }

        @Override
        public void close() {
            boards.forEach(IIndicatorBoard::close);
        }

        @Override
        public void refresh() {
            boards.forEach(IIndicatorBoard::refresh);
        }

        @Override
        public void showBoard() {
            boards.forEach(IIndicatorBoard::showBoard);
        }

        @Override
        public void clear() {
            boards.forEach(IIndicatorBoard::clear);
        }

        @Override
        public void setConfigFile(String configFile) {
            boards.forEach(board -> board.setConfigFile(configFile));
        }

        @Override
        public Element getConfig() {
            return boards.get(0).getConfig();
        }

        @Override
        public void saveConfig(Element element) {
            boards.forEach(board -> board.saveConfig(element));
        }

        @Override
        public AFBoardRedactor getRedactor() {
            return boards.get(0).getRedactor();
        }

        @Override
        public Object getBoardForm() {
            return boards.get(0).getUID();
        }

        @Override
        public String getDescription() {
            return boards.get(0).getDescription();
        }

        @Override
        public long getUID() {
            return boards.get(0).getUID();
        }
    }
}
