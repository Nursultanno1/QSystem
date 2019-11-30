/*
 *  Copyright (C) 2018 {Apertum}Projects. web: www.apertum.ru email: info@apertum.ru
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
package ru.apertum.qsystem.reports.generators;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.http.HttpRequest;
import ru.apertum.qsystem.reports.model.CurRepRecord;
import ru.apertum.qsystem.server.model.QPlanService;
import ru.apertum.qsystem.server.model.QUserList;

import java.util.LinkedList;

/**
 * Evgeniy Egorov.
 *
 * @author Evgeniy Egorov
 */
public class RepCurrentUsers extends ASimpleGenerator {

    public RepCurrentUsers(String href, String resourceNameTemplate) {
        super(href, resourceNameTemplate);
    }

    @Override
    protected JRDataSource getDataSource(HttpRequest request) {
        final LinkedList<CurRepRecord> dataSource = new LinkedList<>();
        QUserList.getInstance().getItems().stream().forEach(user -> {
            int userWorked = 0;
            int userKilled = 0;
            long userAvgTimeWork = 0;
            for (QPlanService plan : user.getPlanServices()) {
                userWorked += plan.getWorked();
                userKilled += plan.getKilled();
                userAvgTimeWork += (plan.getAvgWork() * plan.getWorked());
            }
            userAvgTimeWork = user.getPlanServices().isEmpty() || userWorked == 0 ? 0 : userAvgTimeWork / userWorked;
            for (QPlanService plan : user.getPlanServices()) {
                dataSource.add(new CurRepRecord(user.getName(), plan.getService().getName(),
                        userWorked, userKilled, userAvgTimeWork, plan.getWorked(), plan.getKilled(), plan.getAvgWork()));
            }
        });
        return new JRBeanCollectionDataSource(dataSource);
    }
}
