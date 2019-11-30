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

package ru.apertum.qsystem.server.model;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Тестируем юзеров.
 *
 * @author Evgeniy Egorov
 */
public class QUserTest {

    private QUser user;
    private String pass = "ая-АЯ190 №;%:?-+/\\!*_az AZ";

    @BeforeClass
    public void setUp() throws Exception {
        user = new QUser();
        user.recoverAccess(pass);
    }

    @Test
    public void testCompareCustomers() throws Exception {
        user.setParolcheg(user.getParolcheg());
        assertTrue("Получили пароль, снова его туда загнали, ставнили", user.isCorrectPassword(pass));
        pass = "";
        user.recoverAccess(pass);
        user.setParolcheg(user.getParolcheg());
        assertTrue("Теперь с пустой строкой.", user.isCorrectPassword(pass));
        pass = "1";
        user.recoverAccess(pass);
        user.setParolcheg(user.getParolcheg());
        assertTrue("Теперь с пустой строкой.", user.isCorrectPassword(pass));
    }

}