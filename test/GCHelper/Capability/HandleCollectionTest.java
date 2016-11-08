package GCHelper.Capability;

import GCHelper.Capability.HandleCollection;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

public class HandleCollectionTest {
    private HandleCollection<String, Integer> _handles;
    @Before
    public void Setup()
    {
        _handles = new HandleCollection<String, Integer>();
    }

    @Test
    public void add() throws Exception {
        assertTrue(_handles.Add("Hello", 1));
        assertFalse(_handles.Add("Hello", 1));
    }

    @Test
    public void remove() throws Exception {
        assertFalse(_handles.Remove("Hello", 1));
        assertTrue(_handles.Add("Hello", 1));
        assertTrue(_handles.Remove("Hello", 1));
        assertFalse(_handles.Remove("Hello", 1));
    }

    @Test
    public void iterator() throws Exception {
        Integer counter = 0;
        for(HandleContainer<String, Integer> handle : _handles)
            counter++;
        assertEquals((Integer)0, counter);
        _handles.Add("Hello", 1);
        _handles.Add("Hello 2", 1);
        for(HandleContainer<String, Integer> handle : _handles)
            counter++;
        assertEquals((Integer)2, counter);
    }
}