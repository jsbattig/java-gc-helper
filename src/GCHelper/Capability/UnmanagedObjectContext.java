package GCHelper.Capability;

import GCHelper.Interface.DestroyHandleDelegate;
import java.util.concurrent.atomic.AtomicInteger;

public class UnmanagedObjectContext<THandleClass, THandle> {
    private DestroyHandleDelegate<THandle> _destroyHandleDelegate;
    private HandleCollection<THandleClass, THandle> _parents;
    private AtomicInteger _refCount;

    public UnmanagedObjectContext(DestroyHandleDelegate<THandle> destroyHandleDelegate,
                                  HandleCollection<THandleClass, THandle> parentCollection) {
        setDestroyHandleDelegate(destroyHandleDelegate);
        setParentCollection(parentCollection);
        _refCount = new AtomicInteger(1);
    }

    public void setDestroyHandleDelegate(DestroyHandleDelegate<THandle> destroyHandleDelegate){
        _destroyHandleDelegate = destroyHandleDelegate;
    }

    public DestroyHandleDelegate<THandle> getDestroyHandleDelegate() {
        return _destroyHandleDelegate;
    }

    private void setParentCollection(HandleCollection<THandleClass, THandle> parentCollection){
        _parents = parentCollection;
    }

    public HandleCollection<THandleClass, THandle> getParents() {
        return _parents;
    }

    public void initParentCollection() {
        _parents = new HandleCollection<>();
    }

    public void DestroyAndFree(THandle obj) throws Exception
    {
        if (_destroyHandleDelegate != null)
            _destroyHandleDelegate.DestroyHandle(obj);
    }

    public int AddRefCount()
    {
        return _refCount.incrementAndGet();
    }

    public int ReleaseRefCount()
    {
        return _refCount.decrementAndGet();
    }
}
