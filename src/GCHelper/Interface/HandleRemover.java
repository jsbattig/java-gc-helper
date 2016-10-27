package GCHelper.Interface;

public interface HandleRemover<THandleClass, THandleType> {
    void RemoveAndDestroyHandle(THandleClass handleClass, THandleType obj);
}
