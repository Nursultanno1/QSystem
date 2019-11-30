package ru.apertum.qsystem.common;


import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Created by Evgeniy Egorov on 24.09.2019.
 */
public class ElProcessorTest {

    @Test
    public void demoJexl() {
        System.out.println("-------------------------->>");

        // Create or retrieve an engine
        JexlEngine jexl = new JexlBuilder().create();

        // Create a context and add data
        JexlContext jc = new MapContext();
        jc.set("foo", new Foo("a", 1));
        jc.set("foo2", new Foo("ab", 2));
        jc.set("usa", new UserSessionAccumulator());

        // Create an expression
        String jexlExp = "'size=' + foo.list.size()  + ';   ' +" +
                "' list(2)=' + foo2.list.get(1)  + ';   ' +" +
                "' foo.i('+foo.i+')<foo2.i('+foo2.i+')=' + (foo.i<foo2.i) + ';   ' ";
        JexlExpression e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        Object o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o);


        jexlExp = "' ife:' + (foo.str == foo2.str ? 'bad' : 'good')  + ';   ' +" +
                "' 1ife:' + (foo.str != foo2.str ? 'good' : 'bad')  + ';   ' +" +
                "' 2ife:' + (foo.i != null && foo2.i != null ? 'not null' : 'bad')  + ';   ' +" +
                "' 3ife:' + (foo.i != null && foo2.i != 55 ? 'not 55' : 'bad')  + ';   ' +" +
                "' 4ife:' + (foo.str != null && foo.str == 'a' ? 'foo.str==a' : 'bad')  + ';   '";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


        jexlExp = " ' usa: ' + usa.get('USA') + ' , ' + usa.getInt('sirius')+ ';   ' +"
                + " ' usaFoo: ' + usa.foo.str + ' , ' + usa.foo.list + ';   ' ";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


        jexlExp = " ' map: ' + usa.foo.map.size() + ' map(map2).str:' + usa.foo.map.get('map2').str";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


        jexlExp = " ' usa: ' + usa.get('USA') + ' , ' + usa.getInt('sirius')+ ';   ' +";
        jexlExp = jexlExp + " ' usaFoo: ' + usa.foo.str + ' , ' + usa.foo.list + ';   ' +";
        jexlExp = jexlExp + " ' map: ' + usa.foo.map.size() + ' map(map2).str:' + usa.foo.map.get('map2').str";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


        jexlExp = " usa.foo.list";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


        jexlExp = "'something'";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


        jexlExp = "55";
        e = jexl.createExpression(jexlExp);
        // Now evaluate the expression, getting the result
        o = e.evaluate(jc);
        System.out.println("---------------------------------------------------");
        System.out.println(jexlExp);
        System.out.println("result class is " + o.getClass() + " : " + o.toString());


    }

    public static class Foo2 {

        public String str = "op-str";
        int i = 13;

        public Foo2(String str, int i) {
            this.str = str;
            this.i = i;
        }

        public Foo2() {

        }

    }

    public static class Foo {

        private final String strNull = null;
        private final String str;
        private final int i;
        private final ArrayList<String> list = new ArrayList();
        private final HashMap<String, Foo2> map = new HashMap();

        public Foo(String str, int i) {
            this.str = str;
            this.i = i;
            list.add("str1");
            list.add("str2");
            list.add("str3");

            map.put("map1", new Foo2("map1-key", 11));
            map.put("map2", new Foo2("map2-key", 12));
            map.put("map3", new Foo2("map3-key", 13));
        }

        public String getStr() {
            return str;
        }

        public int getI() {
            return i;
        }

        public ArrayList getList() {
            return list;
        }

        public String summ() {
            return list.toString();
        }

        public HashMap<String, Foo2> getMap() {
            return map;
        }
    }

    public static class UserSessionAccumulator {

        private final Foo foo;

        public UserSessionAccumulator() {
            foo = new Foo("fooUsa", 100500);
        }

        public String get(String name) {
            return name + "!!!";

        }

        public int getInt(String name) {
            return name.length();
        }

        public Foo getFoo() {
            return foo;
        }
    }

    @Test
    public static void demoProcess() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("foo", 101505);
        ctx.put("roo", 111222);
        ctx.put("boo1", true);
        ctx.put("boo2", true);
        ctx.put("boo3", true);
        ctx.put("boo4", false);
        ctx.put("usa", new UserSessionAccumulator());
        String exp = "asdasd ${foo}asdasd ${foo}asdasd ${roo}asd";
        assertEquals(exp.length(), ElProcessor.get().process(exp, ctx).length());
        exp = "${foo}asdasd ${roo}asdasd ${foo}asd";
        assertEquals(exp.length(), ElProcessor.get().process(exp, ctx).length());
        exp = "asdasd ${foo}asdasd ${roo}asdasd ${foo}";
        assertEquals(exp.length(), ElProcessor.get().process(exp, ctx).length());
        exp = "${foo}";
        assertEquals(exp.length(), ElProcessor.get().process(exp, ctx).length());
        exp = "asdasd";
        assertEquals(exp.length(), ElProcessor.get().process(exp, ctx).length());

        exp = "boo1 || boo2 || boo3";
        assertTrue((Boolean) ElProcessor.get().evaluate(exp, ctx));

        exp = "${boo1 || boo2 || boo3}";
        assertEquals("true", ElProcessor.get().process(exp, ctx));

        exp = "${boo1 && boo2 && boo4 ? true : false}";
        assertEquals("false", ElProcessor.get().process(exp, ctx));
        exp = "boo4 || boo2 && boo3 ? true : false";
        assertTrue((Boolean) ElProcessor.get().evaluate(exp, ctx));

        exp = "${(usa.get('a') == 'a!' || usa.get('a') == 'a!!' || usa.get('a') == 'a!!!') ? true : false}";
        assertEquals("true", ElProcessor.get().process(exp, ctx));

        ctx.put("utl", new org.apache.commons.lang3.ArrayUtils());
        exp = "utl.contains( ['a', 'b'], 'a' )";
        assertTrue((Boolean) ElProcessor.get().evaluate(exp, ctx));
    }

    @Test
    public void testEvaluate() throws Exception {
        String s = "имЯz1!@#$%W%&&Ва:_-+*/цук**(ds2Отчество";
        String s2 = s.replaceAll(ElProcessor.ONLY_LETTERS_REGEX, "_");
        assertEquals(s.length(), s2.length());
        assertFalse(s2.contains(":"));
        assertFalse(s2.contains("-"));
        assertFalse(s2.contains("$"));
        assertTrue(s2.startsWith("имЯ"));
        assertTrue(s2.endsWith("Отчество"));

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("foo", 101505);
        ctx.put("roo", 111222);
        ctx.put("arr", new int[]{23, 23});
        ctx.put("str", "string");
        ctx.put("str_ops", "strange");
        String exp = "foo < roo";
        Object o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof Boolean);
        assertTrue((Boolean) o);

        exp = "foo + roo";
        o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof Integer);
        assertTrue(o.equals(101505 + 111222));

        exp = "arr[0] - arr[1]";
        o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof Integer);
        assertTrue(o.equals(0));

        exp = "str.size()";
        o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof Integer);
        assertTrue(o.equals(6));

        exp = "foo.toString()";
        o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof String);
        assertTrue(o.equals("101505"));

        exp = "foo > roo ? 123 : 456";
        o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof Integer);
        assertTrue(o.equals(456));

        exp = "str_ops.size()";
        o = ElProcessor.get().evaluate(exp, ctx);
        assertTrue(o instanceof Integer);
        assertTrue(o.equals(7));
    }

    @Test(expectedExceptions = org.apache.commons.jexl3.JexlException.class)
    public void testNotEL() throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("foo", 101505);
        ctx.put("roo", 111222);
        ctx.put("boo1", true);
        ctx.put("boo2", true);
        ctx.put("boo3", true);
        ctx.put("boo4", false);
        ctx.put("usa", new UserSessionAccumulator());
        String exp = "asdasd ${foo} asdasd ${notInCtx001} asdasd ${roo}asd";
        ElProcessor.get().process(exp, ctx);
        exp = "${not_In_Ctx002}";
        ElProcessor.get().process(exp, ctx);
    }

    @Test(expectedExceptions = org.apache.commons.jexl3.JexlException.class)
    public void testNotEL2() throws Exception {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("foo", 101505);
        ctx.put("roo", 111222);
        ctx.put("boo1", true);
        ctx.put("boo2", true);
        ctx.put("boo3", true);
        ctx.put("boo4", false);
        ctx.put("usa", new UserSessionAccumulator());
        String exp = "${not_In_Ctx002}";
        ElProcessor.get().process(exp, ctx);
    }

}
