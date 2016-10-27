package GCHelper.Exception;

import java.util.Formatter;

public class EFailedObjectRemoval extends EGCHelper {
    public EFailedObjectRemoval(String handleClass, String obj) {
        super(new Formatter().format("Failed to remove object (%s %s)",  handleClass, obj).toString());
    }
}
