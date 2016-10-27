package GCHelper.Exception;

import java.util.Formatter;

public class EInvalidRefCount extends EGCHelper {
    public EInvalidRefCount(String handleClass, String obj, int refCount) {
        super(new Formatter().format("Invalid refcount value reached: %i (%s %s)", refCount, handleClass, obj).toString());
    }
}
