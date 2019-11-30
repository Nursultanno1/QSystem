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

package ru.apertum.qsystem.common.model;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.AssertJUnit.assertTrue;


public class QCustomerTest {

    private QCustomer customer1;
    private QCustomer customer2;

    @BeforeClass
    public void setUp() throws Exception {
        // иницализируем
        customer1 = new QCustomer(1);
        customer2 = new QCustomer(2);
        customer1.setStandTime(new Date());
        customer1.setPriority(1);
        customer2.setStandTime(new Date(System.currentTimeMillis() + 1000));
        customer2.setPriority(1);
    }

    @Test
    public void testCompareCustomers() throws Exception {
        // собственно тест
        //используется отношение "обслужится позднее"(сравнение дает ответ на вопрос "я обслужусь позднее чем тот в параметре?")
        assertTrue("Приоритет один, время разное", customer1.compareTo(customer2) == -1);
        assertTrue("Приоритет один, время разное", customer2.compareTo(customer1) == 1);

        customer1.setPriority(2);
        customer2.setPriority(1);
        assertTrue("Приоритет разный, время разное", customer2.compareTo(customer1) == 1);
        assertTrue("Приоритет разный, время разное", customer1.compareTo(customer2) == -1);

        customer1.setPriority(1);
        customer2.setPriority(2);
        assertTrue("Приоритет разный, время разное", customer2.compareTo(customer1) == -1);
        assertTrue("Приоритет разный, время разное", customer1.compareTo(customer2) == 1);
    }
}