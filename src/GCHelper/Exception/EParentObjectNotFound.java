package GCHelper.Exception;

public class EParentObjectNotFound extends EObjectNotFound {
    public EParentObjectNotFound(String handleClass, String obj, int refCount) {
        super(handleClass, obj);
    }
}