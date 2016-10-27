package GCHelper.Capability;

import GCHelper.Capability.HandleContainer;
import org.junit.Test;

import static org.junit.Assert.*;

public class HandleContainerTest {
    @Test
    public void hashCodeTest() throws Exception {
        HandleContainer<String, Integer> container1 = new HandleContainer<String, Integer>("hello", 1);
        HandleContainer<String, Integer> container2 = new HandleContainer<String, Integer>("hello", 1);
        HandleContainer<String, Integer> container3 = new HandleContainer<String, Integer>("hello ", 1);
        HandleContainer<String, Integer> container4 = new HandleContainer<String, Integer>("hello", 2);
        assertEquals(container1.hashCode(), container2.hashCode());
        assertNotEquals(container1.hashCode(), container3.hashCode());
        assertNotEquals(container1.hashCode(), container4.hashCode());
    }

    @Test
    public void equalsTest() throws Exception {
        HandleContainer<String, Integer> container1 = new HandleContainer<String, Integer>("hello", 1);
        HandleContainer<String, Integer> container2 = new HandleContainer<String, Integer>("hello", 1);
        HandleContainer<String, Integer> container3 = new HandleContainer<String, Integer>("hello ", 1);
        HandleContainer<String, Integer> container4 = new HandleContainer<String, Integer>("hello", 2);
        assertTrue(container1.equals(container2));
        assertFalse(container1.equals(container3));
        assertFalse(container1.equals(container4));
    }
}