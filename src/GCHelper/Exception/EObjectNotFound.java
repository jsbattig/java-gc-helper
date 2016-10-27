package GCHelper.Exception;

import java.util.Formatter;

public class EObjectNotFound extends EGCHelper {
    public EObjectNotFound(String handleClass, String obj) {
        super(new Formatter().format("Object not found (%s %s)",  handleClass, obj).toString());
    }
}