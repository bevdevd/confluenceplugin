package ut.aaa.com.plugin.security;

import org.junit.Test;
import aaa.com.plugin.security.api.MyPluginComponent;
import aaa.com.plugin.security.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest {
    @Test
    public void testMyName() {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent", component.getName());
    }
}