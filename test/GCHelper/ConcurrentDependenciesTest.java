package GCHelper;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

public class ConcurrentDependenciesTest {
    private ConcurrentDependencies<String, Integer> _concurrentDependencies;

    @Before
    public void Setup()
    {
        _concurrentDependencies = new ConcurrentDependencies<String, Integer>();
    }

    @Test
    public void add() throws Exception {
        assertTrue(_concurrentDependencies.Add("Hello", 1));
        assertFalse(_concurrentDependencies.Add("Hello", 1));
    }

    @Test
    public void remove() throws Exception {
        assertFalse(_concurrentDependencies.Remove("Hello", 1));
        assertTrue(_concurrentDependencies.Add("Hello", 1));
        assertTrue(_concurrentDependencies.Remove("Hello", 1));
        assertFalse(_concurrentDependencies.Remove("Hello", 1));
    }

    @Test
    public void find() throws Exception {
        assertFalse(_concurrentDependencies.Find("Hello", 1));
        assertTrue(_concurrentDependencies.Add("Hello", 1));
        assertTrue(_concurrentDependencies.Find("Hello", 1));
    }

    @Test
    public void iterator() throws Exception {
        Integer counter = 0;
        for(HandleContainer<String, Integer> handle : _concurrentDependencies)
            counter++;
        assertEquals((Integer)0, counter);
        _concurrentDependencies.Add("Hello", 1);
        _concurrentDependencies.Add("Hello 2", 1);
        for(HandleContainer<String, Integer> handle : _concurrentDependencies)
            counter++;
        assertEquals((Integer)2, counter);
    }
}