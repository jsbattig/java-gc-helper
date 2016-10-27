package GCHelper;

public interface IHandleRemover<THandleClass, THandleType> {
    void RemoveAndDestroyHandle(THandleClass handleClass, THandleType obj);
}
