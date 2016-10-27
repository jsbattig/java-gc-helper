package GCHelper;

public class UnmanagedObjectContext<THandleClass, THandle> {
    private DestroyHandleDelegate<THandle> _destroyHandleDelegate;
    private ConcurrentDependencies<THandleClass, THandle> _concurrentDependencies;

    public UnmanagedObjectContext(DestroyHandleDelegate<THandle> destroyHandleDelegate,
                                  ConcurrentDependencies<THandleClass, THandle> concurrentDependencies) {
        setDestroyHandleDelegate(destroyHandleDelegate);
        setConcurrentDependencies(concurrentDependencies);
    }

    private void setDestroyHandleDelegate(DestroyHandleDelegate<THandle> destroyHandleDelegate){
        _destroyHandleDelegate = destroyHandleDelegate;
    }

    public DestroyHandleDelegate<THandle> getDestroyHandleDelegate() {
        return _destroyHandleDelegate;
    }

    private void setConcurrentDependencies(ConcurrentDependencies<THandleClass, THandle> concurrentDependencies){
        _concurrentDependencies = concurrentDependencies;
    }

    public ConcurrentDependencies<THandleClass, THandle> getConcurrentDependencies() {
        return _concurrentDependencies;
    }

    public void DestroyAndFree(THandle obj)
    {
        if (_destroyHandleDelegate != null)
            _destroyHandleDelegate.DestroyHandle(obj);
    }
}
