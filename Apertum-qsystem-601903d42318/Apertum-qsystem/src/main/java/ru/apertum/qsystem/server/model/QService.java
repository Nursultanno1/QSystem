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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import ru.apertum.qsystem.client.Locales;
import ru.apertum.qsystem.common.CustomerState;
import ru.apertum.qsystem.common.QLog;
import ru.apertum.qsystem.common.Uses;
import ru.apertum.qsystem.common.exceptions.ServerException;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.extra.ICustomerChangePosition;
import ru.apertum.qsystem.hibernate.Dao;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.qsystem.server.model.calendar.QCalendar;
import ru.apertum.qsystem.server.model.schedule.QBreak;
import ru.apertum.qsystem.server.model.schedule.QBreaks;
import ru.apertum.qsystem.server.model.schedule.QSchedule;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Модель данных для функционирования очереди. Включает в себя: - структуру хранения - методы доступа - методы манипулирования - логирование итераций Главный
 * класс модели данных. Содержит объекты всех кастомеров в очереди к этой услуге. Имеет все необходимые методы для манипулирования кастомерами в пределах одной
 * очереди
 *
 * @author Evgeniy Egorov
 */
@Entity
@Table(name = "services")
public class QService extends DefaultMutableTreeNode implements ITreeIdGetter, Transferable {

    /**
     * множество кастомеров, вставших в очередь к этой услуге.
     */
    @Transient
    private final PriorityQueue<QCustomer> customers = new PriorityQueue<>();

    private PriorityQueue<QCustomer> getCustomers() {
        return customers;
    }

    @Transient
    //@Expose
    //@SerializedName("clients")
    private final LinkedBlockingDeque<QCustomer> clients = new LinkedBlockingDeque<>(customers);

    /**
     * Это все кастомеры стоящие к этой услуге в виде списка Только для бакапа на диск.
     *
     * @return
     */
    public LinkedBlockingDeque<QCustomer> getClients() {
        return clients;
    }

    @Id
    @Column(name = "id")
    //@GeneratedValue(strategy = GenerationType.AUTO) авто нельзя, т.к. id нужны для формирования дерева.
    @Expose
    @SerializedName("id")
    private Long id = new Date().getTime();

    @Override
    public Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }

    /**
     * Если нужно для услуги что-то сохранять в системных параметрах, то это надо сохранять в секцию для этой конкретной услуги.
     *
     * @return Имя секции в системных параметах для конкретно этой услуги.
     */
    public String getSectionName() {
        return "srv_" + id.toString();
    }

    /**
     * признак удаления с проставленим даты.
     */
    @Column(name = "deleted")
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date deleted;

    public Date getDeleted() {
        return deleted;
    }

    public void setDeleted(Date deleted) {
        this.deleted = deleted;
    }

    /**
     * Состояние услуги. 1 - доступна, 0 - недоступна, -1 - невидима, 2 - только для предвариловки, 3 - заглушка, 4 - не для предвариловки, 5 - рулон.
     */
    @Column(name = "status")
    @Expose
    @SerializedName("status")
    private Integer status;

    /**
     * Состояние услуги. Влияет на состояние кнопки на киоске, при редиректе.
     *
     * @return 1 - доступна, 0 - недоступна, -1 - невидима, 2 - только для предвариловки, 3 - заглушка, 4 - не для предвариловки, 5 - рулон.
     */
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * Набор статусов услуги, при которых услуга участвует в работе очереди, т.е. к ней могут стоять люди. Это не заглушка и не неактивная.
     */
    public static final Set<Integer> STATUS_FOR_USING = new HashSet<>(Arrays.asList(-1, 1, 2, 4, 5));//NOSONAR

    /**
     * Набор статусов услуги, при которых услуга НЕ участвует в работе очереди, т.е. к ней НЕ могут стоять люди. Это заглушка или неактивная.
     */
    public static final Set<Integer> STATUS_FOR_STUB = new HashSet<>(Arrays.asList(0, 3));//NOSONAR
    /**
     * Пунктов регистрации может быть много. Наборы кнопок на разных киосках могут быть разные. Указание для какого пункта регистрации услуга, 0-для всех, х-для
     * киоска х.
     */
    @Column(name = "point")
    @Expose
    @SerializedName("point")
    private Integer point = 0;

    /**
     * Пунктов регистрации может быть много. Наборы кнопок на разных киосках могут быть разные. Указание для какого пункта регистрации услуга, 0-для всех, х-для
     * киоска х.
     *
     * @return Наборы кнопок на разных киосках могут быть разные.
     */
    public Integer getPoint() {
        return point;
    }

    public void setPoint(Integer point) {
        this.point = point;
    }

    /**
     * Норматив. Среднее время оказания этой услуги. Зачем надо? Не знаю. Пока для маршрутизации при медосмотре. Может потом тоже применем.
     */
    @Column(name = "duration")
    @Expose
    @SerializedName("duration")
    private Integer duration = 1;

    /**
     * Норматив. Среднее время оказания этой услуги. Зачем надо? Не знаю. Пока для маршрутизации при медосмотре. Может потом тоже применем.
     *
     * @return Норматив.
     */
    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * Время обязательного ожидания посетителя.
     */
    @Column(name = "expectation")
    @Expose
    @SerializedName("exp")
    private Integer expectation = 0;

    /**
     * Время обязательного ожидания посетителя.
     *
     * @return в минутах
     */
    public Integer getExpectation() {
        return expectation;
    }

    public void setExpectation(Integer expectation) {
        this.expectation = expectation;
    }

    /**
     * шаблон звукового приглашения. null или 0... - использовать родительский. Далее что играем а что нет.
     */
    @Column(name = "sound_template")
    @Expose
    @SerializedName("sound_template")
    private String soundTemplate;

    /**
     * шаблон звукового приглашения. null или 0... - использовать родительский. Далее что играем а что нет.
     *
     * @return шаблон звукового приглашения.
     */
    public String getSoundTemplate() {
        return soundTemplate;
    }

    public void setSoundTemplate(String soundTemplate) {
        this.soundTemplate = soundTemplate;
    }

    @Column(name = "advance_limit")
    @Expose
    @SerializedName("advance_limit")
    private Integer advanceLimit = 1;

    public Integer getAdvanceLimit() {
        return advanceLimit;
    }

    public void setAdvanceLinit(Integer advanceLimit) {
        this.advanceLimit = advanceLimit;
    }

    @Column(name = "day_limit")
    @Expose
    @SerializedName("day_limit")
    private Integer dayLimit = 0;

    public Integer getDayLimit() {
        return dayLimit;
    }

    public void setDayLimit(Integer dayLimit) {
        this.dayLimit = dayLimit;
    }

    @Column(name = "person_day_limit")
    @Expose
    @SerializedName("person_day_limit")
    private Integer personDayLimit = 0;

    public Integer getPersonDayLimit() {
        return personDayLimit;
    }

    public void setPersonDayLimit(Integer personDayLimit) {
        this.personDayLimit = personDayLimit;
    }

    @Column(name = "inputed_as_number")
    @Expose
    @SerializedName("inputed_as_number")
    private Integer inputedAsNumber = 0;

    /**
     * Если требуется использовать введенные пользователем данные как его номер в очереди. 0 - генерировать как обычно. enteredAsNumber - использовать первые
     * enteredAsNumber символов как номер в очереди
     *
     * @return 0 - генерировать как обычно. enteredAsNumber - использовать первые enteredAsNumber символов как номер в очереди.
     */
    public Integer getInputedAsNumber() {
        return inputedAsNumber;
    }

    public void setInputedAsNumber(Integer inputedAsNumber) {
        this.inputedAsNumber = inputedAsNumber;
    }

    /**
     * Это ограничение в днях, в пределах которого можно записаться вперед при предварительной записи может быть null или 0 если нет ограничения.
     */
    @Column(name = "advance_limit_period")
    @Expose
    @SerializedName("advance_limit_period")
    private Integer advanceLimitPeriod = 0;

    public Integer getAdvanceLimitPeriod() {
        return advanceLimitPeriod;
    }

    public void setAdvanceLimitPeriod(Integer advanceLimitPeriod) {
        this.advanceLimitPeriod = advanceLimitPeriod;
    }

    /**
     * Деление сетки предварительной записи.
     */
    @Column(name = "advance_time_period")
    @Expose
    @SerializedName("advance_time_period")
    private Integer advanceTimePeriod = 60;

    public Integer getAdvanceTimePeriod() {
        return advanceTimePeriod;
    }

    public void setAdvanceTimePeriod(Integer advanceTimePeriod) {
        this.advanceTimePeriod = advanceTimePeriod;
    }

    /**
     * Способ вызова клиента юзером 1 - стандартно 2 - backoffice, т.е. вызов следующего без табло и звука, запершение только редиректом.
     */
    @Column(name = "enable")
    @Expose
    @SerializedName("enable")
    private Integer enable = 1;

    /**
     * Способ вызова клиента юзером 1 - стандартно 2 - backoffice, т.е. вызов следующего без табло и звука, запершение только редиректом.
     *
     * @return int index.
     */
    public Integer getEnable() {
        return enable;
    }

    public void setEnable(Integer enable) {
        this.enable = enable;
    }

    @Column(name = "seq_id")
    private Integer seqId = 0;

    public Integer getSeqId() {
        return seqId;
    }

    public void setSeqId(Integer seqId) {
        this.seqId = seqId;
    }

    /**
     * Требовать или нет от пользователя после окончания работы с клиентом по этой услуге обозначить результат этой работы выбрав пункт из словаря результатов.
     */
    @Column(name = "result_required")
    @Expose
    @SerializedName("result_required")
    private Boolean resultRequired = false;

    /**
     * Требовать или нет от пользователя после окончания работы с клиентом по этой услуге обозначить результат этой работы выбрав пункт из словаря результатов.
     *
     * @return да или нет.
     */
    public Boolean getResultRequired() {
        return resultRequired;
    }

    public void setResultRequired(Boolean resultRequired) {
        this.resultRequired = resultRequired;
    }

    /**
     * Требовать или нет на пункте регистрации ввода от клиента каких-то данных перед постановкой в очередь после выбора услуги.
     */
    @Column(name = "input_required")
    @Expose
    @SerializedName("input_required")
    private Boolean inputRequired = false;

    /**
     * Требовать или нет на пункте регистрации ввода от клиента каких-то данных перед постановкой в очередь после выбора услуги.
     *
     * @return да или нет.
     */
    public Boolean getInputRequired() {
        return inputRequired;
    }

    public void setInputRequired(Boolean inputRequired) {
        this.inputRequired = inputRequired;
    }

    /**
     * На главном табло вызов по услуге при наличии третьей колонке делать так, что эту третью колонку заполнять не стройкой у юзера, а введенной пользователем
     * строчкой.
     */
    @Column(name = "inputed_as_ext")
    @Expose
    @SerializedName("inputed_as_ext")
    private Boolean inputedAsExt = false;

    /**
     * На главном табло вызов по услуге при наличии третьей колонке делать так, что эту третью колонку заполнять не стройкой у юзера, а введенной пользователем
     * строчкой.
     *
     * @return Разрешение выводить на табло введеные посетителем на киоске(или еще как) данные.
     */
    public Boolean getInputedAsExt() {
        return inputedAsExt;
    }

    public void setInputedAsExt(Boolean inputedAsExt) {
        this.inputedAsExt = inputedAsExt;
    }

    /**
     * Заголовок окна при вводе на пункте регистрации клиентом каких-то данных перед постановкой в очередь после выбора услуги. Также печатается на талоне рядом
     * с введенными данными.
     */
    @Column(name = "input_caption")
    @Expose
    @SerializedName("input_caption")
    private String inputCaption = "";

    /**
     * Заголовок окна при вводе на пункте регистрации клиентом каких-то данных перед постановкой в очередь после выбора услуги. Также печатается на талоне рядом
     * с введенными данными.
     *
     * @return текст заголовка
     */
    public String getInputCaption() {
        return inputCaption;
    }

    public void setInputCaption(String inputCaption) {
        this.inputCaption = inputCaption;
    }

    /**
     * html текст информационного сообщения перед постановкой в очередь. Если этот параметр пустой, то не требуется показывать информационную напоминалку на
     * пункте регистрации.
     */
    @Column(name = "pre_info_html")
    @Expose
    @SerializedName("pre_info_html")
    private String preInfoHtml = "";

    public String getPreInfoHtml() {
        return preInfoHtml;
    }

    public void setPreInfoHtml(String preInfoHtml) {
        this.preInfoHtml = preInfoHtml;
    }

    /**
     * текст для печати при необходимости перед постановкой в очередь.
     */
    @Column(name = "pre_info_print_text")
    @Expose
    @SerializedName("pre_info_print_text")
    private String preInfoPrintText = "";

    public String getPreInfoPrintText() {
        return preInfoPrintText;
    }

    public void setPreInfoPrintText(String preInfoPrintText) {
        this.preInfoPrintText = preInfoPrintText;
    }

    /**
     * текст для печати при необходимости перед постановкой в очередь.
     */
    @Column(name = "ticket_text")
    @Expose
    @SerializedName("ticket_text")
    private String ticketText = "";

    public String getTicketText() {
        return ticketText;
    }

    public void setTicketText(String ticketText) {
        this.ticketText = ticketText;
    }

    /**
     * текст для вывода на главное табло в шаблоны панели вызванного и третью колонку пользователя.
     */
    @Column(name = "tablo_text")
    @Expose
    @SerializedName("tablo_text")
    private String tabloText = "";

    /**
     * текст для вывода на главное табло в шаблоны панели вызванного и третью колонку пользователя.
     *
     * @return строчеп из БД.
     */
    public String getTabloText() {
        return tabloText;
    }

    public void setTabloText(String tabloText) {
        this.tabloText = tabloText;
    }

    /**
     * Расположение кнопки на пункте регистрации.
     */
    @Column(name = "but_x")
    @Expose
    @SerializedName("but_x")
    private Integer butX = 100;
    @Column(name = "but_y")
    @Expose
    @SerializedName("but_y")
    private Integer butY = 100;
    @Column(name = "but_b")
    @Expose
    @SerializedName("but_b")
    private Integer butB = 100;
    @Column(name = "but_h")
    @Expose
    @SerializedName("but_h")
    private Integer butH = 100;

    public Integer getButB() {
        return butB;
    }

    public void setButB(Integer butB) {
        this.butB = butB;
    }

    public Integer getButH() {
        return butH;
    }

    public void setButH(Integer butH) {
        this.butH = butH;
    }

    public Integer getButX() {
        return butX;
    }

    public void setButX(Integer butX) {
        this.butX = butX;
    }

    public Integer getButY() {
        return butY;
    }

    public void setButY(Integer butY) {
        this.butY = butY;
    }

    /**
     * последний номер, выданный последнему кастомеру при номерировании клиентов обособлено в услуге. тут такой замут. когда услугу создаешь из json где-то на
     * клиенте, то там же спринг-контекст не поднят да и нужно это только в качестве данных.
     */
    @Transient
    private int lastNumber = Integer.MIN_VALUE;
    /**
     * последний номер, выданный последнему кастомеру при номерировании клиентов общем рядом для всех услуг. Ограничение самого минимально возможного номера
     * клиента при сквозном нумерировании происходит при определении параметров нумерации.
     */
    @Transient
    private static volatile int lastStNumber = Integer.MIN_VALUE;

    public QService() {
        super();
    }

    @Override
    public String toString() {
        return getName().trim().isEmpty() ? "<NO_NAME>" : getName();
    }

    /**
     * Получить номер для сделующего кастомера. Произойдет инкремент счетчика номеров.
     *
     * @return номер следующего талона.
     */
    public int getNextNumber() {
        synchronized (QService.class) {
            final int firstNum;
            final int lastNum;
            final QProperty propNum = ServerProps.getInstance().getProperty(getSectionName(), Uses.KEY_TICKET_NUMBERING);
            if (propNum != null && Uses.getIntsFromString2(propNum.getValue()).length > 1) {
                final int[] ints = Uses.getIntsFromString2(propNum.getValue());
                firstNum = ints[0];
                lastNum = ints[1];
            } else {
                firstNum = ServerProps.getInstance().getProps().getFirstNumber();
                lastNum = ServerProps.getInstance().getProps().getLastNumber();
            }

            // услуга-рулон сбрасывается по своему, у нее в пропертях есть первый и последний номер.
            // У услуги-рулона всегда своя нумерация! Не сквозная.
            if (lastNumber == Integer.MIN_VALUE) {
                lastNumber = firstNum - 1;
            }
            if (!ServerProps.getInstance().getProps().getNumering() && getStatus() != 5 && lastStNumber == Integer.MIN_VALUE) {
                lastStNumber = firstNum - 1;
            }
            if (lastNumber >= lastNum) {
                clearNextNumber();
            }
            if (!ServerProps.getInstance().getProps().getNumering() && getStatus() != 5 && lastStNumber >= lastNum) {
                clearNextStNumber();
            }

            // учтем вновь поставленного. прибавим одного к количеству сегодня пришедших к данной услуге
            final int today = new GregorianCalendar().get(GregorianCalendar.DAY_OF_YEAR);
            if (today != day) {
                day = today;
                setCountPerDay(0);
            }
            countPerDay++;

            // 0 - общая нумерация, 1 - для каждой услуги своя нумерация 
            // У услуги-рулона всегда своя нумерация!
            if (ServerProps.getInstance().getProps().getNumering() || getStatus() == 5) {
                return ++lastNumber;
            } else {
                return ++lastStNumber;
            }
        }
    }

    // чтоб каждый раз в бд не лазить для проверки сколько предварительных сегодня по этой услуге
    @Transient
    private int dayY = -100; // для смены дня проверки
    @Transient
    private int dayAdvs = -100; // для смены дня проверки

    /**
     * Узнать сколько предварительно записанных для этой услуги на дату.
     *
     * @param date        на эту дату узнаем количество записанных предварительно
     * @param strictStart false - просто количество записанных на этот день, true - количество записанных на этот день начиная с времени date
     * @return количество записанных предварительно
     */
    public int getAdvancedCount(Date date, boolean strictStart) {
        final GregorianCalendar forDay = new GregorianCalendar();
        forDay.setTime(date);

        final GregorianCalendar today = new GregorianCalendar();
        if (!strictStart && forDay.get(GregorianCalendar.DAY_OF_YEAR) == today.get(GregorianCalendar.DAY_OF_YEAR)
                && dayY != today.get(GregorianCalendar.DAY_OF_YEAR)) {
            dayY = today.get(GregorianCalendar.DAY_OF_YEAR);
            dayAdvs = -100;
        }
        if (!strictStart && forDay.get(GregorianCalendar.DAY_OF_YEAR) == today.get(GregorianCalendar.DAY_OF_YEAR) && dayAdvs >= 0) {
            return dayAdvs;
        }

        final DetachedCriteria dc = DetachedCriteria.forClass(QAdvanceCustomer.class);
        dc.setProjection(Projections.rowCount());
        if (!strictStart) {
            forDay.set(GregorianCalendar.HOUR_OF_DAY, 0);
            forDay.set(GregorianCalendar.MINUTE, 0);
        }
        final Date todayM = forDay.getTime();
        forDay.set(GregorianCalendar.HOUR_OF_DAY, 23);
        forDay.set(GregorianCalendar.MINUTE, 59);
        dc.add(Restrictions.between("advanceTime", todayM, forDay.getTime()));
        dc.add(Restrictions.eq("service", this));
        final Long cnt = (Long) (Dao.get().findByCriteria(dc).get(0));
        final int i = cnt.intValue();

        forDay.setTime(date);
        if (!strictStart && forDay.get(GregorianCalendar.DAY_OF_YEAR) == today.get(GregorianCalendar.DAY_OF_YEAR)) {
            dayAdvs = i;
        }

        QLog.l().logger().trace("Посмотрели сколько предварительных записалось в " + getName() + ". Их " + i);
        return i;
    }

    /**
     * Иссяк лимит на одинаковые введенные данные в день по услуге или нет.
     *
     * @param data что-то.
     * @return true - превышен, в очередь становиться нельзя; false - можно в очередь встать
     */
    public boolean isLimitPersonPerDayOver(String data) {
        final int today = new GregorianCalendar().get(GregorianCalendar.DAY_OF_YEAR);
        if (today != day) {
            day = today;
            setCountPerDay(0);
        }
        return getPersonDayLimit() != 0 && getPersonDayLimit() <= getCountPersonsPerDay(data);
    }

    private int getCountPersonsPerDay(String data) {
        int cnt = 0;
        cnt = customers.stream().filter(customer -> data.equalsIgnoreCase(customer.getInputData())).map(item -> 1).reduce(cnt, Integer::sum);
        if (getPersonDayLimit() <= cnt) {
            return cnt;
        }
        QLog.l().logger().trace("Загрузим уже обработанных кастомеров с такими же данными \"" + data + "\"");
        // Загрузим уже обработанных кастомеров
        final GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.HOUR, 0);
        gc.set(GregorianCalendar.MINUTE, 0);
        gc.set(GregorianCalendar.SECOND, 0);
        final Date start = gc.getTime();
        gc.add(GregorianCalendar.DAY_OF_YEAR, 1);
        final Date finish = gc.getTime();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        QLog.l().logger().trace("FROM QCustomer a WHERE "
                + " start_time >'" + dateFormat.format(start) + "' "
                + " and start_time <= '" + dateFormat.format(finish) + "' "
                + " and  input_data = '" + data + "' "
                + " and service_id = " + getId());
        final List<QCustomer> custs = Dao.get().find("FROM QCustomer a WHERE "
                + " start_time >'" + dateFormat.format(start) + "' "
                + " and start_time <= '" + dateFormat.format(finish) + "' "
                + " and  input_data = '" + data + "' "
                + " and service_id = " + getId());
        QLog.l().logger().trace("Загрузили уже обработанных кастомеров с такими же данными \"" + data + "\". Их " + (cnt + custs.size()));
        return cnt + custs.size();
    }

    /**
     * Иссяк лимит на возможных обработанных в день по услуге или нет.
     *
     * @return true - превышен, в очередь становиться нельзя; false - можно в очередь встать.
     */
    public boolean isLimitPerDayOver() {
        final Date now = new Date();
        int advCusts = getAdvancedCount(now, true); //сколько предварительнозаписанных уже есть в очереди в оставшееся время(true)
        final int today = new GregorianCalendar().get(GregorianCalendar.DAY_OF_YEAR);
        if (today != day) {
            day = today;
            setCountPerDay(0);
        }
        final long p = getPossibleTickets();
        final long c = getCountPerDay();
        final boolean f = getDayLimit() != 0 && (p <= c + advCusts);
        if (f) {
            QLog.l().logger().trace("Customers overflow: DayLimit()=" + getDayLimit() + " && PossibleTickets=" + p + " <= CountPerDay=" + c + " + advCusts=" + advCusts);
        }
        return f;
    }

    /**
     * Получить количество талонов, которые все еще можно выдать учитывая ограничение на время работы с одним клиетом.
     *
     * @return оставшееся время работы по услуге / ограничение на время работы с одним клиетом.
     */
    public long getPossibleTickets() {
        if (getDayLimit() != 0) {
            // подсчитаем ограничение на выдачу талонов
            final GregorianCalendar gc = new GregorianCalendar();
            final Date now = new Date();
            gc.setTime(now);
            long dif = getSchedule().getWorkInterval(gc.getTime()).finish.getTime() - now.getTime();

            int ii = gc.get(GregorianCalendar.DAY_OF_WEEK) - 1;
            if (ii < 1) {
                ii = 7;
            }
            final QBreaks qb;
            switch (ii) {
                case 1:
                    qb = getSchedule().getBreaks1();
                    break;
                case 2:
                    qb = getSchedule().getBreaks2();
                    break;
                case 3:
                    qb = getSchedule().getBreaks3();
                    break;
                case 4:
                    qb = getSchedule().getBreaks4();
                    break;
                case 5:
                    qb = getSchedule().getBreaks5();
                    break;
                case 6:
                    qb = getSchedule().getBreaks6();
                    break;
                case 7:
                    qb = getSchedule().getBreaks7();
                    break;
                default:
                    throw new AssertionError();
            }
            if (qb != null) {// может вообще перерывов нет
                for (QBreak br : qb.getBreaks()) {
                    if (br.getToTime().after(now)) {
                        if (br.getFromTime().before(now)) {
                            dif = dif - (br.getToTime().getTime() - now.getTime());
                        } else {
                            dif = dif - br.diff();
                        }
                    }
                }
            }
            QLog.l().logger().trace("Осталось рабочего времени " + (dif / 1000 / 60) + " минут. Если на каждого " + getDayLimit() + " минут, то остается принять " + (dif / 1000 / 60 / getDayLimit()) + " посетителей.");
            return dif / 1000 / 60 / getDayLimit();
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Сколько кастомеров уже прошло услугу сегодня.
     */
    @Transient
    @Expose
    @SerializedName("countPerDay")
    private int countPerDay = 0;

    public void setCountPerDay(int countPerDay) {
        this.countPerDay = countPerDay;
    }

    public int getCountPerDay() {
        return countPerDay;
    }

    /**
     * Текущий день, нужен для учета количества кастомеров обработанных в этой услуге в текущий день.
     */
    @Transient
    @Expose
    @SerializedName("day")
    private int day = 0;

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    /**
     * Сбросить нумерацию талонов. Номер станет на единицу меньше настройки и при получении следующего будет выдан начальный номер талона.
     * Если есть настройка srv_[serviceId].ticket_numbering = "from [number] to [number]" то применится она. Иначе настройка из админки.
     */
    public void clearNextNumber() {
        final QProperty propNum = ServerProps.getInstance().getProperty(getSectionName(), Uses.KEY_TICKET_NUMBERING);
        if (propNum != null && Uses.getIntsFromString2(propNum.getValue()).length > 1) {
            final int[] ints = Uses.getIntsFromString2(propNum.getValue());
            lastNumber = ints[0] - 1;
        } else {
            lastNumber = ServerProps.getInstance().getProps().getFirstNumber() - 1;
        }
    }

    /**
     * Можно изменить текущий номер талона. Это нужно если у вас услуга-рулон. Повесили новый пулон, там на конце болтается какой-то номер, его выставили
     * текущим.
     *
     * @param newCurrent этот и будет текущим.
     */
    public void reinitNextNumber(int newCurrent) {
        lastNumber = newCurrent - 1;
    }

    public static void clearNextStNumber() {
        lastStNumber = ServerProps.getInstance().getProps().getFirstNumber() - 1;
    }

    /**
     * Привосстановлении ситуации из сохраненного файла. Сохраненный кастомер стоял в очереди и ждал, но его еще никто не звал.
     *
     * @param customer сохраненный.
     */
    public void addCustomerForRecoveryOnly(QCustomer customer) {
        if (customer.getPrefix() != null) {
            final int number = customer.getNumber();
            // тут бы не нужно проверять последний выданный если это происходит с редиректенныйм
            if (CustomerState.STATE_REDIRECT != customer.getState()
                    && CustomerState.STATE_WAIT_AFTER_POSTPONED != customer.getState()
                    && CustomerState.STATE_WAIT_COMPLEX_SERVICE != customer.getState()) {
                if (number > lastNumber) {
                    lastNumber = number;
                }
                if (number > lastStNumber) {
                    lastStNumber = number; //NOSONAR
                }
            }
        }
        addCustomer(customer);
    }

    // ***************************************************************************************
    // ********************  МЕТОДЫ УПРАВЛЕНИЯ ЭЛЕМЕНТАМИ И СТРУКТУРЫ ************************  
    // ***************************************************************************************

    /**
     * Добавить в очередь при этом проставится название сервиса, в который всрал, и его описание, если у кастомера нету префикса, то проставится и префикс.
     *
     * @param customer это кастомер которого добавляем в очередь к услуге
     */
    public void addCustomer(QCustomer customer) {
        if (customer.getPrefix() == null) {
            customer.setPrefix(getPrefix());
        }
        if (!getCustomers().add(customer)) {
            throw new ServerException("Невозможно добавить нового кастомера в хранилище кастомеров.");
        }

        // поддержка расширяемости плагинами/ определим куда влез клиент
        QCustomer before = null;
        QCustomer after = null;
        for (Iterator<QCustomer> itr = getCustomers().iterator(); itr.hasNext(); ) {
            final QCustomer c = itr.next();
            if (!customer.getId().equals(c.getId())) {
                if (customer.compareTo(c) > 0) {
                    // c - первее, определяем before
                    if (before == null) {
                        before = c;
                    } else {
                        if (before.compareTo(c) < 0) {
                            before = c;
                        }
                    }
                } else {
                    if (customer.compareTo(c) != 0) {
                        // c - после, определяем after
                        if (after == null) {
                            after = c;
                        } else {
                            if (after.compareTo(c) > 0) {
                                after = c;
                            }
                        }
                    }
                }
            }
        }
        // поддержка расширяемости плагинами
        for (final ICustomerChangePosition event : ServiceLoader.load(ICustomerChangePosition.class)) {
            QLog.l().logger().info("Вызов SPI расширения. Описание: " + event.getDescription());
            event.insert(customer, before, after);
        }

        clients.clear();
        clients.addAll(getCustomers());
    }

    /**
     * Всего хорошего, все свободны!.
     */
    public void freeCustomers() {
        // поддержка расширяемости плагинами
        for (final ICustomerChangePosition event : ServiceLoader.load(ICustomerChangePosition.class)) {
            QLog.l().logger().info("Вызов  SPI расширения. Описание: " + event.getDescription());
            for (Iterator<QCustomer> itr = getCustomers().iterator(); itr.hasNext(); ) {
                event.remove(itr.next());
            }
        }
        getCustomers().clear();
        clients.clear();
        clients.addAll(getCustomers());
    }

    /**
     * Получить, но не удалять. NoSuchElementException при неудаче.
     *
     * @return первого в очереди кастомера.
     */
    public QCustomer getCustomer() {
        return getCustomers().element();
    }

    /**
     * Получить и удалить. NoSuchElementException при неудаче.
     *
     * @return первого в очереди кастомера.
     */
    public QCustomer removeCustomer() {
        final QCustomer customer = getCustomers().remove();

        // поддержка расширяемости плагинами
        for (final ICustomerChangePosition event : ServiceLoader.load(ICustomerChangePosition.class)) {
            QLog.l().logger().info("Вызов SPI  расширения. Описание: " + event.getDescription());
            event.remove(customer);
        }

        clients.clear();
        clients.addAll(getCustomers());
        return customer;
    }

    /**
     * Удалить любого в очереди кастомера.
     *
     * @param customer удаляемый кастомер.
     * @return может вернуть false при неудаче.
     */
    public boolean removeCustomer(QCustomer customer) {
        final Boolean res = getCustomers().remove(customer);
        if (customer != null && res) {
            // поддержка расширяемости плагинами
            for (final ICustomerChangePosition event : ServiceLoader.load(ICustomerChangePosition.class)) {
                QLog.l().logger().info("Вызов SPI расширения.  Описание: " + event.getDescription());
                event.remove(customer);
            }
        }
        clients.clear();
        clients.addAll(getCustomers());
        return res;
    }

    /**
     * Получить но не удалять. null при неудаче.
     *
     * @return первого в очереди кастомера.
     */
    public QCustomer peekCustomer() {
        return getCustomers().peek();
    }

    /**
     * Получить и удалить. может вернуть null при неудаче.
     *
     * @return первого в очереди кастомера.
     */
    public QCustomer polCustomer() {
        final QCustomer customer = getCustomers().poll();
        if (customer != null) {
            // поддержка расширяемости плагинами
            for (final ICustomerChangePosition event : ServiceLoader.load(ICustomerChangePosition.class)) {
                QLog.l().logger().info("Вызов SPI расширения. Описание: " + event.getDescription());
                event.remove(customer);
            }
        }

        clients.clear();
        clients.addAll(getCustomers());
        return customer;
    }

    /**
     * Получение количества кастомеров, стоящих в очереди.
     *
     * @return количество кастомеров в этой услуге.
     */
    public int getCountCustomers() {
        return getCustomers().size();
    }

    /**
     * Определим общую длмну очереди к этой услуге. Т.е. сколько реально будет вызвано разных посетителей, которые мешают сразу попасть именно к этой услуге.
     * <br>
     * Бежим по юзерам и смотрим обрабатывают ли они услугу <br>
     * если да, то возьмем все услуги юзера и сложим всех кастомеров в очередях <br>
     * самую маленькую сумму отправим в ответ по запросу.
     *
     * @return размер толпы перед попаданием к этой услуге.
     */
    public int getQueueSize() {
        // бежим по юзерам и смотрим обрабатывают ли они услугу
        // если да, то возьмем все услуги юзера и  сложим всех кастомеров в очередях
        // самую маленькую сумму отправим в ответ по запросу.
        int min = Integer.MAX_VALUE;
        for (QUser user : QUserList.getInstance().getItems()) {
            if (user.hasService(getId())) {
                // теперь по услугам юзера
                int sum = 0;
                for (QPlanService planServ : user.getPlanServices()) {
                    final QService service = QServiceTree.getInstance().getById(planServ.getService().getId());
                    sum = sum + service.getCountCustomers();
                }
                if (min > sum) {
                    min = sum;
                }
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * Поменяем приоритет по номеру.
     *
     * @param number      номер
     * @param newPriority новый приоритет.
     * @return да или нет.
     */
    public boolean changeCustomerPriorityByNumber(String number, int newPriority) {
        for (QCustomer customer : getCustomers()) {
            if (number.equalsIgnoreCase(customer.getPrefix() + (customer.getNumber() < 1 ? "" : customer.getNumber()))) {
                customer.setPriority(newPriority);
                removeCustomer(customer); // убрать из очереди
                addCustomer(customer);// перепоставили чтобы очередность переинлексиловалась
                return true;
            }
        }
        return false;
    }

    /**
     * Выгрызть кастомера по ID. При нехороших действиях.
     *
     * @param number этого грызем.
     * @return выгрызыный.
     */
    public QCustomer gnawOutCustomerByNumber(String number) {
        for (QCustomer customer : getCustomers()) {
            if (number.equalsIgnoreCase(customer.getPrefix() + (customer.getNumber() < 1 ? "" : customer.getNumber()))) {
                removeCustomer(customer); // убрать из очереди
                return customer;
            }
        }
        return null;
    }

    /**
     * Выгрызть кастомера по ID. При нехороших действиях.
     *
     * @param customerId этого грызем.
     * @return выгрызыный.
     */
    public QCustomer gnawOutCustomerById(Long customerId) {
        for (QCustomer customer : getCustomers()) {
            if (customerId.equals(customer.getId())) {
                removeCustomer(customer); // убрать из очереди
                return customer;
            }
        }
        return null;
    }

    /**
     * Описание услуги.
     */
    @Expose
    @SerializedName("description")
    @Column(name = "description")
    private String description;

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Префикс услуги.
     */
    @Expose
    @SerializedName("service_prefix")
    @Column(name = "service_prefix")
    private String prefix = "";

    public void setPrefix(String prefix) {
        this.prefix = prefix == null ? "" : prefix;
    }

    public String getPrefix() {
        return prefix == null ? "" : prefix;
    }

    /**
     * Наименование услуги.
     */
    @Expose
    @SerializedName("name")
    @Column(name = "name")
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Надпись на кнопке услуги.
     */
    @Expose
    @SerializedName("buttonText")
    @Column(name = "button_text")
    private String buttonText;

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    /**
     * Группировка услуг.
     */
    @Expose
    @SerializedName("parentId")
    @Column(name = "prent_id")
    private Long parentId;

    @Override
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "link_service_id")
    private QService link;

    public QService getLink() {
        return link;
    }

    public void setLink(QService link) {
        this.link = link;
    }

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "schedule_id")
    private QSchedule schedule;

    public QSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(QSchedule schedule) {
        this.schedule = schedule;
    }

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "calendar_id")
    private QCalendar calendar;

    public QCalendar getCalendar() {
        return calendar;
    }

    public void setCalendar(QCalendar calendar) {
        this.calendar = calendar;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "services_id")
    @Expose
    @SerializedName("langs")
    private Set<QServiceLang> langs = new HashSet<>();

    public Set<QServiceLang> getLangs() {
        return langs;
    }

    public void setLangs(Set<QServiceLang> langs) {
        this.langs = langs;
    }

    @Transient
    private HashMap<String, QServiceLang> qslangs = null;

    /**
     * Прлучить язык с текстами для услуги.
     *
     * @param nameLocale этой локали.
     */
    public QServiceLang getServiceLang(String nameLocale) {
        if (qslangs == null) {
            qslangs = new HashMap<>();
            getLangs().stream().forEach(sl -> qslangs.put(sl.getLang(), sl));
        }
        return qslangs.get(nameLocale);
    }

    public enum Field {

        /**
         * Надпись на кнопке.
         */
        BUTTON_TEXT,
        /**
         * заголовок ввода клиентом.
         */
        INPUT_CAPTION,
        /**
         * читаем перед тем как встать в очередь.
         */
        PRE_INFO_HTML,
        /**
         * печатаем подсказку перед тем как встать в очередь.
         */
        PRE_INFO_PRINT_TEXT,
        /**
         * текст на талоте персонально услуги.
         */
        TICKET_TEXT,
        /**
         * описание услуги.
         */
        DESCRIPTION,
        /**
         * имя услуги.
         */
        NAME
    }


    /**
     * Локализируем тексты.
     *
     * @param field филд для локализации.
     * @return текст на нужном языке.
     */
    public String getTextToLocale(Field field) {
        final String nl = Locales.getInstance().getNameOfPresentLocale();
        final QServiceLang sl = getServiceLang(nl);
        switch (field) {
            case BUTTON_TEXT:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getButtonText() == null || sl.getButtonText().isEmpty()
                        ? getButtonText() : sl.getButtonText();
            case INPUT_CAPTION:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getInputCaption() == null || sl.getInputCaption().isEmpty()
                        ? getInputCaption() : sl.getInputCaption();
            case PRE_INFO_HTML:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getPreInfoHtml() == null || sl.getPreInfoHtml().isEmpty()
                        ? getPreInfoHtml() : sl.getPreInfoHtml();
            case PRE_INFO_PRINT_TEXT:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getPreInfoPrintText() == null || sl.getPreInfoPrintText().isEmpty()
                        ? getPreInfoPrintText() : sl.getPreInfoPrintText();
            case TICKET_TEXT:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getTicketText() == null || sl.getTicketText().isEmpty()
                        ? getTicketText() : sl.getTicketText();
            case DESCRIPTION:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getDescription() == null || sl.getDescription().isEmpty()
                        ? getDescription() : sl.getDescription();
            case NAME:
                return !Locales.getInstance().isWelcomeMultylangs() || sl == null || sl.getName() == null || sl.getName().isEmpty() ? getName() : sl.getName();
            default:
                throw new AssertionError();
        }
    }

    /**
     * Если не NULL и не пустая, то эта услуга недоступна и сервер обламает постановку в очередь выкинув причину из этого поля на пункт регистрации.
     */
    @Transient
    private String tempReasonUnavailable;

    public String getTempReasonUnavailable() {
        return tempReasonUnavailable;
    }

    public void setTempReasonUnavailable(String tempReasonUnavailable) {
        this.tempReasonUnavailable = tempReasonUnavailable;
    }
    //*******************************************************************************************************************
    //*******************************************************************************************************************
    //********************** Реализация методов узла в дереве ***********************************************************

    /**
     * По сути группа объединения услуг или коернь всего дерева. То во что включена данныя услуга.
     */
    @Transient
    private QService parentService;
    @Transient
    @Expose
    @SerializedName("inner_services")
    private final LinkedList<QService> childrenOfService = new LinkedList<>();

    @SuppressWarnings("squid:S1319")
    public LinkedList<QService> getChildren() {
        return childrenOfService;
    }

    @Override
    public void addChild(ITreeIdGetter child) {
        if (!childrenOfService.contains((QService) child)) { // бывает что добавляем повторно ужедобавленный
            childrenOfService.add((QService) child);
        }
    }

    @Override
    public QService getChildAt(int childIndex) {
        return childrenOfService.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return childrenOfService.size();
    }

    @Override
    public QService getParent() {
        return parentService;
    }

    @Override
    public int getIndex(TreeNode node) {
        return childrenOfService.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @Override
    public Enumeration children() {
        return Collections.enumeration(childrenOfService);
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        child.setParent(this);
        this.childrenOfService.add(index, (QService) child);
    }

    @Override
    public void remove(int index) {
        this.childrenOfService.remove(index);
    }

    @Override
    public void remove(MutableTreeNode node) {
        this.childrenOfService.remove((QService) node);
    }

    @Override
    public void removeFromParent() {
        getParent().remove(getParent().getIndex(this));
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        parentService = (QService) newParent;
        if (parentService != null) {
            setParentId(parentService.id);
        } else {
            parentId = null;
        }
    }

    /**
     * data flavor used to get back a DnDNode from data transfer.
     */
    public static final DataFlavor DND_NODE_FLAVOR = new DataFlavor(QService.class, "Drag and drop Node");
    /**
     * list of all flavors that this DnDNode can be transfered as.
     */
    protected static DataFlavor[] flavors = {QService.DND_NODE_FLAVOR};

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return true;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (this.isDataFlavorSupported(flavor)) {
            return this;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
