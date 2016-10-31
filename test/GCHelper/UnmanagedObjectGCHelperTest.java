package GCHelper;

import GCHelper.Exception.EInvalidRefCount;
import GCHelper.Exception.EObjectNotFound;
import GCHelper.Interface.DestroyHandleDelegate;
import GCHelper.Interface.ExceptionDelegate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnmanagedObjectGCHelperTest implements DestroyHandleDelegate<Integer>, ExceptionDelegate<String, Integer> {
    private int _handleValue = 0;
    private boolean _destroyCalled;
    private boolean _throwException;
    private UnmanagedObjectGCHelper<String, Integer> _exceptionObj;
    private Exception _exception;
    private String _exceptionClassHandle;
    private int _exceptionHandle;
    private int _destroyCount;

    public void DestroyHandle(Integer integer) throws Exception {
        _handleValue = integer;
        _destroyCalled = true;
        _destroyCount++;
        if(_throwException)
            throw new Exception("error");
    }

    @Before
    public void Setup(){
        _throwException = false;
        _exceptionObj = null;
        _exception = null;
        _exceptionClassHandle = "";
        _exceptionHandle = 0;
        _destroyCount = 0;
        // UnmanagedObjectGCHelper.consoleLoggingEnabled = true;
    }

    @Test
    public void Register() throws EObjectNotFound, EInvalidRefCount, InterruptedException {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(1, _handleValue);

        _destroyCalled = false;
        _handleValue = 0;
        _unmanagedObjectHandler.Register("Hello", 1, this);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(1, _handleValue);

        _destroyCalled = false;
        _handleValue = 0;
        _unmanagedObjectHandler.Register("Hello", 1);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
    }

    @Test
    public void RegisterAndUnregisterTwice() throws EObjectNotFound, EInvalidRefCount, InterruptedException {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(1, _handleValue);
    }

    @Test
    public void UnregisterThrowsException() throws EObjectNotFound, EInvalidRefCount, InterruptedException {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.setOnException(this);
        assertEquals(this, _unmanagedObjectHandler.getOnException());
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        _throwException = true;
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(1, _handleValue);
        assertEquals(_unmanagedObjectHandler, _exceptionObj);
        assertNotEquals(null, _exception);
        assertEquals("Hello", _exceptionClassHandle);
        assertEquals(1, _exceptionHandle);
    }

    @Test
    public void RegisterThenStopAgentThenUnregister() throws EObjectNotFound, EInvalidRefCount, InterruptedException {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.StopAgent();
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertFalse(_destroyCalled);
        assertNotEquals(1, _handleValue);
    }

    @Override
    public void ExceptionReport(UnmanagedObjectGCHelper<String, Integer> obj, Exception exception, String classHandle, Integer handle) {
        _exceptionObj = obj;
        _exception = exception;
        _exceptionClassHandle = classHandle;
        _exceptionHandle = handle;
    }

    @Test
    public void AddDependency() throws Exception {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        _unmanagedObjectHandler.Register("Hello", 2, this, null);
        _unmanagedObjectHandler.AddDependency("Hello", 1, "Hello", 2);
        _unmanagedObjectHandler.Unregister("Hello", 2);
        Thread.sleep(100);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(2, _handleValue);
        assertEquals(2, _destroyCount);
    }

    @Test
    public void AddAndRemoveDependency() throws Exception {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        _unmanagedObjectHandler.Register("Hello", 2, this, null);
        _unmanagedObjectHandler.AddDependency("Hello", 1, "Hello", 2);
        _unmanagedObjectHandler.Unregister("Hello", 2);
        Thread.sleep(100);
        assertFalse(_destroyCalled);
        assertEquals(0, _handleValue);
        _unmanagedObjectHandler.RemoveDependency("Hello", 1, "Hello", 2);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(2, _handleValue);
        assertEquals(1, _destroyCount);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        Thread.sleep(100);
        assertTrue(_destroyCalled);
        assertEquals(1, _handleValue);
        assertEquals(2, _destroyCount);
    }

    @Test(expected=EObjectNotFound.class)
    public void AddDependencyThrowsException() throws EObjectNotFound, EInvalidRefCount, InterruptedException {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        _unmanagedObjectHandler.AddDependency("Hello", 1, "Hello", 2);
    }
}
