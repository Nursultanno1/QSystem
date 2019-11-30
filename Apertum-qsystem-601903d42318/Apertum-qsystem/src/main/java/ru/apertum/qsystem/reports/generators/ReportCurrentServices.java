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
package ru.apertum.qsystem.reports.generators;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.http.HttpRequest;
import ru.apertum.qsystem.reports.model.CurRepRecord;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.QUser;
import ru.apertum.qsystem.server.model.QUserList;

import java.util.LinkedList;

/**
 * Evgeniy Egorov.
 *
 * @author Evgeniy Egorov
 */
public class ReportCurrentServices extends ASimpleGenerator {

    public ReportCurrentServices(String href, String resourceNameTemplate) {
        super(href, resourceNameTemplate);
    }

    @Override
    protected JRDataSource getDataSource(HttpRequest request) {
        final LinkedList<CurRepRecord> dataSource = new LinkedList<>();
        QServiceTree.getInstance().getNodes().stream().filter(QService::isLeaf).forEach(service -> {
            int serviceWorked = 0;
            int serviceKilled = 0;
            long serviceAvgTimeWork = 0;
            long serviceAvgTimeWait = 0;
            for (QUser user : QUserList.getInstance().getItems()) {
                if (user.hasService(service)) {
                    serviceWorked += user.getPlanService(service).getWorked();
                    serviceKilled += user.getPlanService(service).getKilled();
                    serviceAvgTimeWork += (user.getPlanService(service).getAvgWork() * user.getPlanService(service).getWorked());
                    serviceAvgTimeWait += (user.getPlanService(service).getAvgWait() * user.getPlanService(service).getWorked());
                }
            }
            serviceAvgTimeWork = serviceWorked == 0 ? 0 : serviceAvgTimeWork / serviceWorked;
            serviceAvgTimeWait = serviceWorked == 0 ? 0 : serviceAvgTimeWait / serviceWorked;
            for (QUser user : QUserList.getInstance().getItems()) {
                if (user.hasService(service)) {
                    dataSource.add(new CurRepRecord(user.getName(), service.getName(),
                            serviceWorked, serviceKilled, serviceAvgTimeWork, service.getCountCustomers(), serviceAvgTimeWait,
                            user.getPlanService(service).getWorked(), user.getPlanService(service).getKilled(), user.getPlanService(service).getAvgWork()));
                }
            }
        });
        return new JRBeanCollectionDataSource(dataSource);
    }
}
