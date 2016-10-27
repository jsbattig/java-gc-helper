package GCHelper.Service;

import GCHelper.Interface.HandleRemover;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnregistrationAgentTest implements HandleRemover<String, Integer> {
    private String _handleClassDestroyed;
    private Integer _handleDestroyed;

    @Test
    public void close() throws Exception {
        UnregistrationAgent<String, Integer> agent = new UnregistrationAgent<>(this);
        agent.close();
    }

    @Test
    public void enqueue() throws Exception {
        UnregistrationAgent<String, Integer> agent = new UnregistrationAgent<>(this);
        agent.Enqueue("Hello", 1);
        Thread.sleep(200);
        assertEquals("Hello", _handleClassDestroyed);
        assertEquals((Integer)1, _handleDestroyed);
        agent.close();
    }

    @Test
    public void stop() throws Exception {
        UnregistrationAgent<String, Integer> agent = new UnregistrationAgent<>(this);
        agent.Stop();
        agent.Enqueue("Hello", 1);
        Thread.sleep(200);
        assertNotEquals("Hello", _handleClassDestroyed);
        assertNotEquals((Integer)1, _handleDestroyed);
    }

    @Override
    public void RemoveAndDestroyHandle(String handleClass, Integer obj) {
        _handleClassDestroyed = handleClass;
        _handleDestroyed = obj;
    }
}