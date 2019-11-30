package ru.apertum.qsystem.server.controller;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import ru.apertum.qsystem.SuiteListenerForDb;
import ru.apertum.qsystem.common.cmd.AJsonRPC20;
import ru.apertum.qsystem.common.cmd.CmdParams;
import ru.apertum.qsystem.common.cmd.RpcBanList;
import ru.apertum.qsystem.common.cmd.RpcGetAllServices;
import ru.apertum.qsystem.common.cmd.RpcGetInt;
import ru.apertum.qsystem.common.cmd.RpcGetServiceState;
import ru.apertum.qsystem.common.model.QCustomer;
import ru.apertum.qsystem.server.ServerProps;
import ru.apertum.qsystem.server.model.QServiceTree;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Listeners({SuiteListenerForDb.class})
public class ExecuterTest {

    CmdParams cmdParams;

    @BeforeMethod
    public void setUp() {
        cmdParams = new CmdParams();
    }

    @Test
    public void testGetInstance() {
        AJsonRPC20 rpc20 = Executer.getInstance().getServicesTask.process(cmdParams, "localhost", new byte[]{127, 0, 0, 1});
        assertNotNull(rpc20);
        assertTrue(rpc20 instanceof RpcGetAllServices);
        RpcGetAllServices allServices = (RpcGetAllServices) rpc20;
        assertEquals(allServices.getResult().getRoot().getName(), "Root of services");
        assertTrue(allServices.getResult().getRoot().getChildren().size() > 0);
    }

    @Test
    public void testAboutTask() {
        // услуга всегда доступна по расписанию.
        cmdParams.serviceId = QServiceTree.getInstance().getById(1540578278210L).getId();
        AJsonRPC20 rpc20 = Executer.getInstance().aboutTask.process(cmdParams, "localhost", new byte[]{127, 0, 0, 1});
        assertNotNull(rpc20);
        assertTrue(rpc20 instanceof RpcGetServiceState);
        RpcGetServiceState serviceState = (RpcGetServiceState) rpc20;
        assertEquals(serviceState.getResult().getLenghtLine(), 0);
        assertEquals(serviceState.getResult().getCode(), 0);

        // услгуга всегда недоступна по расписанию
        cmdParams.serviceId = QServiceTree.getInstance().getById(1540578931493L).getId();
        rpc20 = Executer.getInstance().aboutTask.process(cmdParams, "localhost", new byte[]{127, 0, 0, 1});
        assertNotNull(rpc20);
        assertTrue(rpc20 instanceof RpcGetServiceState);
        serviceState = (RpcGetServiceState) rpc20;
        assertEquals(serviceState.getResult().getLenghtLine(), 0);
        assertEquals(serviceState.getResult().getCode(), 1000000011);

        // услуга не назначена ни одному оператору
        cmdParams.serviceId = QServiceTree.getInstance().getById(1540579009027L).getId();
        rpc20 = Executer.getInstance().aboutTask.process(cmdParams, "localhost", new byte[]{127, 0, 0, 1});
        assertNotNull(rpc20);
        assertTrue(rpc20 instanceof RpcGetServiceState);
        serviceState = (RpcGetServiceState) rpc20;
        assertEquals(serviceState.getResult().getLenghtLine(), 0);
        assertEquals(serviceState.getResult().getCode(), 1000000000);
    }

    @Test
    public void testAboutServicePersonLimit() {
        cmdParams.textData = "";
        cmdParams.serviceId = QServiceTree.getInstance().getRoot().getChildren().get(0).getId();
        AJsonRPC20 rpc20 = Executer.getInstance().aboutServicePersonLimit.process(cmdParams, "localhost", new byte[]{127, 0, 0, 1});
        assertNotNull(rpc20);
        assertTrue(rpc20 instanceof RpcGetInt);
        RpcGetInt getInt = (RpcGetInt) rpc20;
        assertEquals(getInt.getResult(), 0);


        QCustomer customer = new QCustomer(13);
        customer.setInputData("baned");
        ServerProps.getInstance().getProps().setBlackTime(100500);
        RpcBanList.getInstance().addToBanList(customer);
        cmdParams.textData = "baned";
        cmdParams.serviceId = QServiceTree.getInstance().getRoot().getChildren().get(0).getId();
        rpc20 = Executer.getInstance().aboutServicePersonLimit.process(cmdParams, "localhost", new byte[]{127, 0, 0, 1});
        assertNotNull(rpc20);
        assertTrue(rpc20 instanceof RpcGetInt);
        getInt = (RpcGetInt) rpc20;
        assertEquals(getInt.getResult(), 2);

        RpcBanList.getInstance().getBanList().forEach(RpcBanList.getInstance()::deleteFromBanList);
    }
}