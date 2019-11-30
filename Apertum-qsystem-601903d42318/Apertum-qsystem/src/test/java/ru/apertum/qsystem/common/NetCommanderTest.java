package ru.apertum.qsystem.common;

import java.util.concurrent.atomic.AtomicBoolean;
import org.dom4j.Element;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import ru.apertum.qsystem.SuiteListenerForDb;
import ru.apertum.qsystem.common.cmd.RpcGetAllServices;
import ru.apertum.qsystem.common.cmd.RpcGetSelfSituation;
import ru.apertum.qsystem.common.cmd.RpcGetServerState;
import ru.apertum.qsystem.common.cmd.RpcGetServiceState;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.model.INetProperty;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.Exit;
import ru.apertum.qsystem.server.QServer;
import ru.apertum.qsystem.server.board.Board;
import ru.apertum.qsystem.server.model.QService;
import ru.apertum.qsystem.server.model.QServiceTree;
import ru.apertum.qsystem.server.model.QUser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Listeners({SuiteListenerForDb.class})
@Test(groups = "NetCommander", singleThreaded = true)
public class NetCommanderTest {

    private int port = 5588;
    private INetProperty netProperty;

    private QService service;
    private QService someService;
    private QUser user;

    private QUser user1;
    private QService srv2usr1;
    private QService srv1usr1;

    @BeforeTest
    public void init() {
        Thread thread = new Thread(() -> QServer.makeSocketAndRun(port));
        thread.setDaemon(true);
        thread.start();
    }

    @AfterTest
    public void done() throws IOException {
        Exit.sendExit("exit", "127.0.0.1", port);
    }


    @BeforeMethod()
    public void setUp() {
        netProperty = new INetProperty() {
            @Override
            public Integer getPort() {
                return port;
            }

            @Override
            public InetAddress getAddress() {
                try {
                    return InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @AfterMethod
    public void next() {
    }

    @Test(groups = "NetCommander")
    public void testGetServices() throws QException {
        RpcGetAllServices.ServicesForWelcome services = NetCommander.getServices(netProperty);
        assertNotNull(services);
        assertEquals(services.getRoot().getName(), "Root of services");
        assertTrue(services.getRoot().getChildren().size() > 0);
        someService = services.getRoot().getChildren().getLast();
        QServiceTree.sailToStorm(services.getRoot(), srv -> {
            QService qService = (QService) srv;
            if (qService.getId().equals(2L)) {
                service = qService;
            }
            if (qService.getId().equals(1548933145503L)) {
                srv1usr1 = qService;
            }
            if (qService.getId().equals(1548941906510L)) {
                srv2usr1 = qService;
            }
        });
        assertNotNull(service);
        assertNotNull(srv1usr1);
        assertNotNull(srv2usr1);
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testGetServices")
    public void testAboutService() throws QException {
        assertNotNull(service);
        RpcGetServiceState.ServiceState serviceState = NetCommander.aboutService(netProperty, service.getId());
        assertNotNull(serviceState);
        assertEquals(serviceState.getLenghtLine(), 0);
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testAboutService")
    public void testGetUsers() throws QException {
        LinkedList<QUser> users = NetCommander.getUsers(netProperty);
        assertNotNull(users);
        assertTrue(users.size() > 0);
        users.forEach(usr -> {
            if (usr.getId().equals(2L)) {
                user = usr;
            }
            if (usr.getId().equals(3L)) {
                user1 = usr;
            }
        });
        assertNotNull(user);
        assertNotNull(user1);
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testGetUsers")
    public void testTotalCircleForKillCustomer() throws Exception {
        assertNotNull(user);
        assertNotNull(service);
        QCustomer customer = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer);

        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);
        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            if (selfService.getId() == service.getId()) {
                assertEquals(selfService.getLine().size(), 1);
            } else {
                assertTrue(selfService.getLine().isEmpty());
            }
        });

        QCustomer invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer, invitedCustomer);

        NetCommander.killNextCustomer(netProperty, user.getId(), null);

        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testTotalCircleForKillCustomer")
    public void testTotalCircleForSimpleWorkCustomer() throws Exception {
        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);

        assertNotNull(user);
        assertNotNull(service);

        // Толпа чуваков к услуге
        RpcGetServiceState.ServiceState consistency = NetCommander.getServiceConsistency(netProperty, service.getId());
        assertNotNull(consistency);
        assertEquals(consistency.getCode(), 0);
        assertEquals(consistency.getLenghtLine(), 0);
        assertEquals(consistency.getClients().size(), 0);

        // ставим двух
        QCustomer customer1 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer1);
        int limitOver = NetCommander.aboutServicePersonLimitOver(netProperty, service.getId(), null);
        assertEquals(limitOver, 0); // можно вставать
        Thread.sleep(200);
        QCustomer customer2 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer2);

        // Толпа чуваков к услуге
        consistency = NetCommander.getServiceConsistency(netProperty, service.getId());
        assertNotNull(consistency);
        assertEquals(consistency.getCode(), 0);
        assertEquals(consistency.getLenghtLine(), 2);
        assertEquals(consistency.getClients().size(), 2);

        assertEquals(customer1.getNumber() + 1, customer2.getNumber());

        LinkedList<RpcGetServerState.ServiceInfo> serverState = NetCommander.getServerState(netProperty);

        assertNotNull(serverState);
        //сейчас просто заведено 4 услуги, добавить при изменениии
        assertEquals(serverState.size(), 6);
        //ждут двое поставленных
        assertEquals(serverState.getFirst().getCountWait(), 2);

        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            if (selfService.getId() == service.getId()) {
                assertEquals(selfService.getLine().size(), 2);
            } else {
                assertTrue(selfService.getLine().isEmpty());
            }
        });

        // вызовем первого
        QCustomer invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer1, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        QCustomer finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer1, finishCustomer);

        // вызовем второго
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer2, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer2, finishCustomer);

        // никого не осталось
        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testTotalCircleForSimpleWorkCustomer")
    public void testTotalCircleForRedirectCustomer() throws Exception {
        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);

        assertNotNull(user);
        assertNotNull(service);

        // ставим двух
        QCustomer customer1 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer1);
        Thread.sleep(200);
        QCustomer customer2 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer2);

        assertEquals(customer1.getNumber() + 1, customer2.getNumber());

        Long newServiceId = null;

        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        for (RpcGetSelfSituation.SelfService selfService1 : selfServices.getSelfservices()) {
            if (selfService1.getId() == service.getId()) {
                assertEquals(selfService1.getLine().size(), 2);
            } else {
                if (newServiceId == null) {
                    newServiceId = selfService1.getId();
                }
                assertTrue(selfService1.getLine().isEmpty());
            }
        }
        assertNotNull(newServiceId);

        // вызовем первого
        QCustomer invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer1, invitedCustomer);

        // заредиректим в себя
        NetCommander.getStartCustomer(netProperty, user.getId());
        NetCommander.redirectCustomer(netProperty, user.getId(), null, service.getId(), false, "", -1L);


        // вызовем, а он второй, т.к. первый ушел в свою, это значит в хвост уходит
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer2, invitedCustomer);
        assertEquals(invitedCustomer.getService().getId(), service.getId());

        // редиректим в другую услугу
        NetCommander.getStartCustomer(netProperty, user.getId());
        NetCommander.redirectCustomer(netProperty, user.getId(), null, newServiceId, false, "", -1L);

        // вызовем, а он второй но из другой услуги стал с приоритетом, т.к. редиректнутый
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer2, invitedCustomer);
        assertEquals(invitedCustomer.getService().getId(), newServiceId);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        QCustomer finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer2, finishCustomer);

        // теперь вызовем первого, который был редиректнут в свою услуги и стоял в хвосте, пока мы второго редиректили и обрабатывали.
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer1, invitedCustomer);
        assertEquals(invitedCustomer.getService().getId(), service.getId());

        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer1, finishCustomer);

        // никого не осталось
        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testTotalCircleForRedirectCustomer")
    public void testTotalCircleForPostponeCustomer() throws Exception {
        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);

        assertNotNull(user);
        assertNotNull(service);

        // поставим в очередь
        QCustomer customer = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer);


        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            if (selfService.getId() == service.getId()) {
                assertEquals(selfService.getLine().size(), 1);
            } else {
                assertTrue(selfService.getLine().isEmpty());
            }
        });

        // вызываем из очереди, там один стоит
        QCustomer invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer, invitedCustomer);

        // отложим
        NetCommander.getStartCustomer(netProperty, user.getId());
        NetCommander.customerToPostpone(netProperty, user.getId(), invitedCustomer.getId(), "TestPostpone", 0, false);

        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });

        // есть один отложенный
        LinkedList<QCustomer> poolInfo = NetCommander.getPostponedPoolInfo(netProperty);
        assertNotNull(poolInfo);
        assertEquals(poolInfo.size(), 1);
        assertEquals(poolInfo.get(0).getPostponedStatus(), "TestPostpone");

        QCustomer postponedCustomer = poolInfo.get(0);

        // Поменяем статус отложенному
        NetCommander.postponeCustomerChangeStatus(netProperty, postponedCustomer.getId(), "NewPostponestatus");

        // статус поменялся
        poolInfo = NetCommander.getPostponedPoolInfo(netProperty);
        assertNotNull(poolInfo);
        assertEquals(poolInfo.size(), 1);
        assertEquals(poolInfo.get(0).getPostponedStatus(), "NewPostponestatus");

        // вызовем отложенного
        NetCommander.invitePostponeCustomer(netProperty, user.getId(), postponedCustomer.getId());

        // в отложенных никого
        poolInfo = NetCommander.getPostponedPoolInfo(netProperty);
        assertNotNull(poolInfo);
        assertEquals(poolInfo.size(), 0);

        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        QCustomer finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(invitedCustomer, finishCustomer);

        // никого не осталось
        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testTotalCircleForPostponeCustomer")
    public void testGetBoardConfig() throws Exception {
        Element element = NetCommander.getBoardConfig(netProperty);
        assertNotNull(element);
        Board board = null;
        try {
            board = Board.unmarshal(element);
        } catch (Exception ex) {
            assertTrue(false);
        }
        assertNotNull(board);
        assertNotNull(board.parameters);
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testGetBoardConfig")
    public void testChangeTempAvailableService() throws Exception {
        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);

        assertNotNull(user);
        assertNotNull(service);

        RpcGetServiceState.ServiceState serviceState = NetCommander.aboutService(netProperty, service.getId());
        assertNotNull(serviceState);
        assertEquals(serviceState.getLenghtLine(), 0);
        assertEquals(serviceState.getCode(), 0);
        assertEquals(serviceState.getMessage(), null);

        NetCommander.changeTempAvailableService(netProperty, service.getId(), "RingOff");

        serviceState = NetCommander.aboutService(netProperty, service.getId());
        assertNotNull(serviceState);
        assertEquals(serviceState.getLenghtLine(), 0);
        assertEquals(serviceState.getCode(), 2);
        assertEquals(serviceState.getMessage(), "RingOff");

        NetCommander.changeTempAvailableService(netProperty, service.getId(), null);

        serviceState = NetCommander.aboutService(netProperty, service.getId());
        assertNotNull(serviceState);
        assertEquals(serviceState.getLenghtLine(), 0);
        assertEquals(serviceState.getCode(), 0);
        assertEquals(serviceState.getMessage(), null);

        // никого и не было
        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testChangeTempAvailableService")
    public void testTotalCircleForDirectInviteCustomer() throws Exception {
        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);

        assertNotNull(user);
        assertNotNull(service);

        // Толпа чуваков к услуге
        RpcGetServiceState.ServiceState consistency = NetCommander.getServiceConsistency(netProperty, service.getId());
        assertNotNull(consistency);
        assertEquals(consistency.getCode(), 0);
        assertEquals(consistency.getLenghtLine(), 0);
        assertEquals(consistency.getClients().size(), 0);

        // ставим пятерых
        QCustomer customer1 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer1);
        int limitOver = NetCommander.aboutServicePersonLimitOver(netProperty, service.getId(), null);
        assertEquals(limitOver, 0); // можно вставать
        Thread.sleep(200);
        QCustomer customer2 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer2);
        Thread.sleep(200);
        QCustomer customer3 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer3);
        Thread.sleep(200);
        QCustomer customer4 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer4);
        Thread.sleep(200);
        QCustomer customer5 = NetCommander.standInService(netProperty, service.getId(), "1", 1, "");
        assertNotNull(customer5);

        // Толпа чуваков к услуге
        consistency = NetCommander.getServiceConsistency(netProperty, service.getId());
        assertNotNull(consistency);
        assertEquals(consistency.getCode(), 0);
        assertEquals(consistency.getLenghtLine(), 5);
        assertEquals(consistency.getClients().size(), 5);

        assertEquals(customer1.getNumber() + 1, customer2.getNumber());

        LinkedList<RpcGetServerState.ServiceInfo> serverState = NetCommander.getServerState(netProperty);

        assertNotNull(serverState);
        //сейчас просто заведено 4 услуги, добавить при изменениии
        assertEquals(serverState.size(), 6);
        //ждут 5-ро поставленных
        assertEquals(serverState.getFirst().getCountWait(), 5);

        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            if (selfService.getId() == service.getId()) {
                assertEquals(selfService.getLine().size(), 5);
            } else {
                assertTrue(selfService.getLine().isEmpty());
            }
        });

        // вызовем конкретно из очереди, но из пустой - 1го
        QCustomer invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId(), someService.getId());
        assertEquals(customer1, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        QCustomer finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer1, finishCustomer);


        // вызовем конкретно из очереди, из целой челов - 2го
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId(), service.getId());
        assertEquals(customer2, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer2, finishCustomer);

        // вызовем 4го по номеру
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId(), customer4.getSemiNumber());
        assertEquals(customer4, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer4, finishCustomer);

        // вызовем 5го по ID
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId(), customer3.getSemiNumber(), customer5.getId());
        assertEquals(customer5, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer5, finishCustomer);

        // вызовем 3го просто
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer3, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer3, finishCustomer);

        // никого не осталось
        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }

    @Test(groups = "NetCommander", dependsOnMethods = "testTotalCircleForDirectInviteCustomer")
    public void testTotalCircleForAddedFireService() throws Exception {
        boolean check = NetCommander.getSelfServicesCheck(netProperty, user.getId());
        assertTrue(check);

        assertNotNull(user);
        assertNotNull(service);

        // никого небыло к этому юзеру
        RpcGetSelfSituation.SelfSituation selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });

        // ставим двух
        QCustomer customer1 = NetCommander.standInService(netProperty, srv1usr1.getId(), "1", 1, "");
        assertNotNull(customer1);
        Thread.sleep(200);
        QCustomer customer2 = NetCommander.standInService(netProperty, srv1usr1.getId(), "1", 1, "");
        assertNotNull(customer2);

        // назначим услугу с людьми
        String serviseFire = NetCommander.setServiseFire(netProperty, srv1usr1.getId(), user.getId(), 1);
        assertTrue(serviseFire.contains(srv1usr1.getId().toString()));
        assertTrue(serviseFire.contains(user.getId().toString()));

        // теперь у чувака двое стоят к нему.
        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        final AtomicBoolean f = new AtomicBoolean(false);
        selfServices.getSelfservices().forEach(selfService -> {
            if (selfService.getId() == srv1usr1.getId()) {
                f.set(true);
                assertEquals(selfService.getLine().size(), 2);
            } else {
                assertTrue(selfService.getLine().isEmpty());
            }
        });
        assertTrue(f.get());

        // вызовем первого первым юзером
        QCustomer invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user.getId());
        assertEquals(customer1, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user.getId());
        QCustomer finishCustomer = NetCommander.getFinishCustomer(netProperty, user.getId(), null, -1L, "");
        assertEquals(customer1, finishCustomer);

        // отвяжем услугу на горячую
        String deleteServiseFire = NetCommander.deleteServiseFire(netProperty, srv1usr1.getId(), user.getId());
        assertTrue(deleteServiseFire.contains(srv1usr1.getId().toString()));
        assertTrue(deleteServiseFire.contains(user.getId().toString()));

        // никого к первому нет.
        selfServices = NetCommander.getSelfServices(netProperty, user.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });

        // вызовем второго вторым юзером
        invitedCustomer = NetCommander.inviteNextCustomer(netProperty, user1.getId());
        assertEquals(customer2, invitedCustomer);
        // обработаем
        NetCommander.getStartCustomer(netProperty, user1.getId());
        finishCustomer = NetCommander.getFinishCustomer(netProperty, user1.getId(), null, -1L, "");
        assertEquals(customer2, finishCustomer);

        // никого не осталось и ко второму
        selfServices = NetCommander.getSelfServices(netProperty, user1.getId());
        selfServices.getSelfservices().forEach(selfService -> {
            assertTrue(selfService.getLine().isEmpty());
        });
    }
}