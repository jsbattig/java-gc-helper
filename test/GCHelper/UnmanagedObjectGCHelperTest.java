package GCHelper;

import GCHelper.Exception.EInvalidRefCount;
import GCHelper.Exception.EObjectNotFound;
import GCHelper.Interface.DestroyHandleDelegate;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnmanagedObjectGCHelperTest implements DestroyHandleDelegate<Integer> {
    private Integer _handleValue = 0;
    private boolean _destroyCalled;

    public void DestroyHandle(Integer integer) {
        _handleValue = integer;
        _destroyCalled = true;
    }

    @Test
    public void Register() throws EObjectNotFound, EInvalidRefCount {
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<String, Integer>();
        _unmanagedObjectHandler.Register("Hello", 1, this, null);
        assertFalse(_destroyCalled);
        assertEquals((Integer)0, _handleValue);
        _unmanagedObjectHandler.Unregister("Hello", 1);
        assertTrue(_destroyCalled);
        assertEquals((Integer)1, _handleValue);
    }
}
