/*
 * Copyright (C) 2013 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
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
package ru.apertum.qsystem.server.model.schedule;

import java.util.Date;
import ru.apertum.qsystem.server.model.IidGetter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Списки наборов перерывов для привязки к дневному расписанию.
 *
 * @author Evgeniy Egorov
 */
@Entity
@Table(name = "breaks")
public class QBreaks implements IidGetter, Serializable {

    public QBreaks() {
        // for marshall.
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Наименование плана перерывов.
     */
    @Column(name = "name")
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "breaks_id")
    private Set<QBreak> breaks = new HashSet<>();

    public Set<QBreak> getBreaks() {
        return breaks;
    }

    public void setBreaks(Set<QBreak> breaks) {
        this.breaks = breaks;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        String delim = "";
        for (QBreak qBreak : breaks) {
            builder.append(delim).append(qBreak);
            delim = ", ";
        }
        return name + "(" + builder.toString() + ")";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final QBreaks other = (QBreaks) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.name, other.name) && Objects.equals(this.breaks.size(), other.breaks.size());
    }

    /**
     * Проверить что сейчас. Уже перерыв?
     *
     * @return если да, то вернем перерыв, если нет, то null.
     */
    public QBreak isNow() {
        return isNow(new Date());
    }

    /**
     * Проверить что время из даты попало в перерыв.
     *
     * @param date время из даты на проверку попало в перерыв или нет.
     * @return если да, то вернем перерыв, если нет, то null.
     */
    public QBreak isNow(Date date) {
        for (QBreak brk : breaks) {
            if (brk.isNow(date)) {
                return brk;
            }
        }
        return null;
    }

}
