package GCHelper.Capability;

import GCHelper.Interface.DestroyHandleDelegate;

import java.util.concurrent.atomic.AtomicInteger;

public class UnmanagedObjectContext<THandleClass, THandle> {
    private DestroyHandleDelegate<THandle> _destroyHandleDelegate;
    private ConcurrentDependencies<THandleClass, THandle> _concurrentDependencies;
    private AtomicInteger _refCount;

    public UnmanagedObjectContext(DestroyHandleDelegate<THandle> destroyHandleDelegate,
                                  ConcurrentDependencies<THandleClass, THandle> concurrentDependencies) {
        setDestroyHandleDelegate(destroyHandleDelegate);
        setConcurrentDependencies(concurrentDependencies);
        _refCount = new AtomicInteger(1);
    }

    public void setDestroyHandleDelegate(DestroyHandleDelegate<THandle> destroyHandleDelegate){
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

    public Integer AddRefCount()
    {
        return _refCount.incrementAndGet();
    }

    public Integer ReleaseRefCount()
    {
        return _refCount.decrementAndGet();
    }
}
