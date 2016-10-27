package GCHelper.Exception;

public class EDependencyObjectNotFound extends EObjectNotFound {
    public EDependencyObjectNotFound(String handleClass, String obj, int refCount) {
        super(handleClass, obj);
    }
}