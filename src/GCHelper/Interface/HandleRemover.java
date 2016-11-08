package GCHelper.Interface;

public interface HandleRemover<THandleClass, THandleType> {
    void RemoveAndCallDestroyHandleDelegate(THandleClass handleClass, THandleType obj);
}
