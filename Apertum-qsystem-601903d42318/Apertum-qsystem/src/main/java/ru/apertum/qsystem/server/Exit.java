/*
 * Copyright (C) 2014 Evgeniy Egorov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ru.apertum.qsystem.server;

import ru.apertum.qsystem.common.Uses;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;

/**
 * Выключить сервер консольной командой послав exit.
 *
 * @author Evgeniy Egorov
 */
public class Exit {

    /**
     * Выключить сервер консольной командой послав exit.
     */
    public static void main(String[] args) throws IOException {
        sendExit("exit", args.length > 0 && !Uses.isInt(args[0]) ? args[0] : "127.0.0.1",
                args.length > 0 && Uses.isInt(args[0]) ? Integer.parseInt(args[0]) : (args.length > 1 && Uses.isInt(args[1]) ? Integer.parseInt(args[1]) : 3128));
    }

    /**
     * Отослать команду на адрес и порт.
     *
     * @param cmd     команда.
     * @param address адрес.
     * @param port    порт.
     */
    public static void sendExit(String cmd, String address, int port) throws IOException {
        try (final Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), 3000);
            try (final PrintWriter writer = new PrintWriter(socket.getOutputStream())) {
                writer.print(URLEncoder.encode(cmd, "utf-8"));
                writer.flush();
            }
        }
    }
}
