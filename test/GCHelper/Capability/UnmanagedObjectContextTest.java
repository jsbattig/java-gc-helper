package GCHelper.Capability;

import GCHelper.Interface.DestroyHandleDelegate;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnmanagedObjectContextTest implements DestroyHandleDelegate<Integer> {
    @Test
    public void addRefCount() throws Exception {
        UnmanagedObjectContext<String, Integer> context = new UnmanagedObjectContext<String, Integer>(this, null);
        assertEquals(2, context.AddRefCount());
    }

    @Test
    public void releaseRefCount() throws Exception {
        UnmanagedObjectContext<String, Integer> context = new UnmanagedObjectContext<String, Integer>(this, null);
        assertEquals(2, context.AddRefCount());
        assertEquals(1, context.ReleaseRefCount());
    }

    private boolean _destroyCalled;
    private Integer _handleValue;

    public void DestroyHandle(Integer integer) {
        _handleValue = integer;
        _destroyCalled = true;
    }

    @Test
    public void getDestroyHandleDelegate() throws Exception {
        UnmanagedObjectContext<String, Integer> context = new UnmanagedObjectContext<String, Integer>(this, null);
        assertEquals(this, context.getDestroyHandleDelegate());
    }

    @Test
    public void getConcurrentDependencies() throws Exception {
        HandleCollection<String, Integer> deps = new HandleCollection<String, Integer>();
        UnmanagedObjectContext<String, Integer> context = new UnmanagedObjectContext<String, Integer>(this, deps);
        assertEquals(deps, context.getParents());
    }

    @Test
    public void destroyAndFree() throws Exception {
        _destroyCalled = false;
        _handleValue = 0;
        UnmanagedObjectContext<String, Integer> context = new UnmanagedObjectContext<String, Integer>(this, null);
        context.DestroyAndFree(1);
        assertTrue(_destroyCalled);
        assertEquals((Integer)1, _handleValue);
    }
}