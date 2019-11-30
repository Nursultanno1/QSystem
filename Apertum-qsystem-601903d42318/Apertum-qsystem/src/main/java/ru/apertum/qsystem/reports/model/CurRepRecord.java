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
package ru.apertum.qsystem.reports.model;

/**
 * Запись в отчете по текущему состоянию.
 *
 * @author Evgeniy Egorov
 */
public class CurRepRecord {

    public CurRepRecord() {
    }

    /**
     * В срезе юзеров.
     *
     * @param user            имя пользователя
     * @param service         название услуге
     * @param userWorked      параметр юзера
     * @param userKilled      параметр юзера
     * @param userAverageWork параметр юзера
     * @param worked          параметр услуги, в группе по юзеру
     * @param killed          параметр услуги, в группе по юзеру
     * @param avgTimeWork     параметр услуги, в группе по юзеру
     */
    @SuppressWarnings("squid:S00107")
    public CurRepRecord(String user, String service, int userWorked, int userKilled, long userAverageWork, int worked, int killed, long avgTimeWork) {
        this.user = user;
        this.service = service;
        this.userWorked = userWorked;
        this.userKilled = userKilled;
        this.userAverageWork = userAverageWork;
        this.worked = worked;
        this.killed = killed;
        this.avgTimeWork = avgTimeWork;
    }

    /**
     * В разрезе услуг.
     *
     * @param user               имя юзера
     * @param service            название услуги
     * @param serviceWorked      параметр услуги
     * @param serviceKilled      параметр услуги
     * @param serviceAverageWork параметр услуги
     * @param serviceWait        параметр услуги
     * @param serviceAverageWait параметр услуги
     * @param worked             параметр юзера, в группе по услуге
     * @param killed             параметр юзера, в группе по услуге
     * @param avgTimeWork        параметр юзера, в группе по услуге
     */
    @SuppressWarnings("squid:S00107")
    public CurRepRecord(String user, String service, int serviceWorked, int serviceKilled, long serviceAverageWork, int serviceWait,
                        long serviceAverageWait, int worked, int killed, long avgTimeWork) {
        this.user = user;
        this.service = service;
        this.serviceWorked = serviceWorked;
        this.serviceKilled = serviceKilled;
        this.serviceAverageWork = serviceAverageWork;
        this.serviceWait = serviceWait;
        this.serviceAverageWait = serviceAverageWait;
        this.worked = worked;
        this.killed = killed;
        this.avgTimeWork = avgTimeWork;
    }

    private String user;
    private String service;
    //
    //
    private int userWorked;
    private int userKilled;
    private long userAverageWork;
    //
    //
    private int serviceWorked;
    private int serviceKilled;
    private long serviceAverageWork;
    private int serviceWait;
    private long serviceAverageWait;
    //--
    private int worked;
    private int killed;
    private long avgTimeWork;

    public long getAvgTimeWork() {
        return avgTimeWork;
    }

    public void setAvgTimeWork(long avgTimeWork) {
        this.avgTimeWork = avgTimeWork;
    }

    public int getKilled() {
        return killed;
    }

    public void setKilled(int killed) {
        this.killed = killed;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public long getServiceAverageWait() {
        return serviceAverageWait;
    }

    public void setServiceAverageWait(long serviceAverageWait) {
        this.serviceAverageWait = serviceAverageWait;
    }

    public long getServiceAverageWork() {
        return serviceAverageWork;
    }

    public void setServiceAverageWork(long serviceAverageWork) {
        this.serviceAverageWork = serviceAverageWork;
    }

    public int getServiceKilled() {
        return serviceKilled;
    }

    public void setServiceKilled(int serviceKilled) {
        this.serviceKilled = serviceKilled;
    }

    public int getServiceWait() {
        return serviceWait;
    }

    public void setServiceWait(int serviceWait) {
        this.serviceWait = serviceWait;
    }

    public int getServiceWorked() {
        return serviceWorked;
    }

    public void setServiceWorked(int serviceWorked) {
        this.serviceWorked = serviceWorked;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getUserAverageWork() {
        return userAverageWork;
    }

    public void setUserAverageWork(long userAverageWork) {
        this.userAverageWork = userAverageWork;
    }

    public int getUserKilled() {
        return userKilled;
    }

    public void setUserKilled(int userKilled) {
        this.userKilled = userKilled;
    }

    public int getUserWorked() {
        return userWorked;
    }

    public void setUserWorked(int userWorked) {
        this.userWorked = userWorked;
    }

    public int getWorked() {
        return worked;
    }

    public void setWorked(int worked) {
        this.worked = worked;
    }
}
