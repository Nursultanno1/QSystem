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
package ru.apertum.qsystem.server.controller;

import ru.apertum.qsystem.client.forms.AFBoardRedactor;
import ru.apertum.qsystem.client.forms.FBoardConfigJaxb;
import ru.apertum.qsystem.client.forms.FIndicatorBoargJaxb;
import ru.apertum.qsystem.common.QConfig;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.server.board.Board;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Вывод информации на мониторы. Класс-менеджер вывода информации на общее табло в виде монитора.
 *
 * @author Evgeniy Egorov
 */
public class QIndicatorBoardMonitorJaxb extends QIndicatorBoardMonitor {

    @Override
    public String getDescription() {
        return "Default Tablo JAXB.";
    }

    /**
     * Создадим форму, спозиционируем, сконфигурируем и покажем.
     */
    @Override
    protected void initIndicatorBoard() {
        setConfigFile("config/mainboard.xml");
        if (indicatorBoard == null) {
            final Board board;
            try {
                board = Board.unmarshal(new File(getConfigFile()));
            } catch (JAXBException | FileNotFoundException ex) {
                QLog.l().logger().error("Невозможно прочитать файл конфигурации главного табло. " + ex.getMessage());
                return;
            }
            indicatorBoard = FIndicatorBoargJaxb.getIndicatorBoard(board);
            if (indicatorBoard == null) {
                QLog.l().logger().warn("Табло не демонстрируется. Отключено в настройках.");
                return;
            }
            try {
                indicatorBoard.setIconImage(ImageIO.read(QIndicatorBoardMonitorJaxb.class.getResource("/ru/apertum/qsystem/client/forms/resources/recent.png")));
            } catch (IOException ex) {
                QLog.l().logger().error(ex);
            }
            // Определим форму на монитор
            indicatorBoard.toPosition(QConfig.cfg().isDebug(), board.pointX, board.pointY);

            // ушло в абстрактный метод setLinesCount(indicatorBoard.getLinesCount())
            setPause(indicatorBoard.getPause());
            if (!getRecords().isEmpty()) {
                showOnBoard(new LinkedList<>(getRecords().values()));
            }

            java.awt.EventQueue.invokeLater(() -> indicatorBoard.setVisible(true));
        }
    }

    @Override
    public AFBoardRedactor getRedactor() {
        if (boardConfig == null) {
            boardConfig = FBoardConfigJaxb.getBoardConfig(null, false);
        }
        return boardConfig;
    }
}
