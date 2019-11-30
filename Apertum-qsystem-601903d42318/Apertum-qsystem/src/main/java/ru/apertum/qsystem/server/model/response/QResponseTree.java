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
package ru.apertum.qsystem.server.model.response;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import ru.apertum.qsystem.hibernate.Dao;
import ru.apertum.qsystem.server.controller.ServerEvents;
import ru.apertum.qsystem.server.model.ATreeModel;

import java.util.Date;
import java.util.LinkedList;

/**
 * Дерево отзывов для подсистемы.
 *
 * @author Evgeniy Egorov
 */
public class QResponseTree extends ATreeModel<QRespItem> {

    public static QResponseTree getInstance() {
        return QInfoTreeHolder.INSTANCE;
    }

    @Override
    protected LinkedList<QRespItem> load() {
        return new LinkedList<>(Dao.get().findByCriteria(DetachedCriteria.forClass(QRespItem.class)
            .setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY)
            .add(Property.forName("deleted").isNull())
            .addOrder(Property.forName("id").asc())));
    }

    private static class QInfoTreeHolder {

        private static final QResponseTree INSTANCE = new QResponseTree();
    }

    private QResponseTree() {
        super();
        ServerEvents.getInstance().registerListener(this::createTree);
    }

    /**
     * Построить дерево начиная от корня.
     *
     * @param root корень.
     */
    public static void formTree(QRespItem root) {
        root.getChildren().stream().map(resp -> {
            resp.setParent(root);
            return resp;
        }).forEach(QResponseTree::formTree);
    }

    /**
     * Сохранить дерево отзывов.
     */
    @Override
    public void save() {
        deleted.stream().forEach(t -> QResponseTree.sailToStorm(t, service -> {
            final QRespItem qs = (QRespItem) service;
            qs.setDeleted(new Date());
            if (!deleted.contains(qs)) {
                deleted.add(qs);
            }
        }));
        Dao.get().saveOrUpdateAll(deleted);
        deleted.clear();
        Dao.get().saveOrUpdateAll(getNodes());
    }
}
