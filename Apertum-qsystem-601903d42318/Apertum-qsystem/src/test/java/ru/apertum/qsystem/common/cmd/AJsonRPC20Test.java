package ru.apertum.qsystem.common.cmd;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.apertum.qsystem.common.exceptions.QException;
import ru.apertum.qsystem.common.exceptions.ServerException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class AJsonRPC20Test {

    AJsonRPC20 rpc20;

    @BeforeMethod
    public void setUp() {
        rpc20 = new JsonRPC20OK(200);
    }

    @Test
    public void testDemarshal() throws QException {
        String json = AJsonRPC20.rpcToJson(rpc20);
        JsonRPC20OK rpc20OK = AJsonRPC20.demarshal(json, JsonRPC20OK.class);
        assertEquals(rpc20OK.getResult(), ((JsonRPC20OK) rpc20).getResult());
    }

    @Test
    public void testDemarshalFail() {
        try{
        assertThrows(QException.class, () -> AJsonRPC20.demarshal("json", JsonRPC20OK.class));
        }catch(Exception e){
            System.out.println(e);
            
        }
    }
}