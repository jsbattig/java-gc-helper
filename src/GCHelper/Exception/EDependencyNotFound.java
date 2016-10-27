package GCHelper.Exception;

import java.util.Formatter;

public class EDependencyNotFound extends EGCHelper {
    public EDependencyNotFound(String handleClass, String obj, int refCount) {
        super(new Formatter().format("Dependency not found (%s %s)",  handleClass, obj).toString());
    }
}
