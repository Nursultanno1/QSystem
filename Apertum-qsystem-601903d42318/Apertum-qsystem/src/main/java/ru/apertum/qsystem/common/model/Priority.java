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

import java.util.Arrays;
import ru.apertum.qsystem.common.Uses;

/**
 * Реализация преоритета очередников.
 * приоритет - целое число.
 * чем больше число, тем выше приоритет.
 * ограничения на возможный приоритет находятся в Uses.
 * по умолчанию приоритет Uses.PRIORITY_NORMAL;
 *
 * @author Evgeniy Egorov
 */
public final class Priority implements IPriority {

    private int priorityGrade = Uses.PRIORITY_NORMAL;

    public Priority(int priority) {
        set(priority);
    }

    @Override
    public void set(int priority) {
        if (Arrays.binarySearch(Uses.PRIORITYS, priority) == -1) {
            throw new IllegalArgumentException("Не возможно установить значение приоритета."
                + " Значение " + priority
                + " не принадлежит допустимым значениям: " + Arrays.toString(Uses.PRIORITYS));
        }
        this.priorityGrade = priority;
    }

    @Override
    public int get() {
        return priorityGrade;
    }

    /**
     * сравнение двух приоритетов.
     * приоритет - целое число, чем больше число тем выше приоритет
     * 1 - выше, чем приоритет в параметре
     * -1 - ниже, чем приоритет в параметре
     *
     * @param priority с чем сравниваем.
     * @return 0 - приоритеты равны
     */
    @Override
    public int compareTo(IPriority priority) {
        int res = 0;
        if (this.get() > priority.get()) {
            res = 1;
        } else {
            if (this.get() < priority.get()) {
                res = -1;
            }
        }
        return res;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IPriority) {
            return this.compareTo((IPriority) obj) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return priorityGrade;
    }
}
