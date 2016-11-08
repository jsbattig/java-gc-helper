package GCHelper.Exception;

import java.util.Formatter;

public class EParentNotFound extends EGCHelper {
    public EParentNotFound(String handleClass, String obj) {
        super(new Formatter().format("Dependency not found (%s %s)",  handleClass, obj).toString());
    }
}
