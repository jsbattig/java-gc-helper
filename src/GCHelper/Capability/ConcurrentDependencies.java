package GCHelper.Capability;

import GCHelper.Capability.HandleContainer;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDependencies<THandleClass, THandle> implements Iterable<HandleContainer<THandleClass, THandle>>
{
    private ConcurrentHashMap<HandleContainer<THandleClass, THandle>, Integer> _container;

    public ConcurrentDependencies() {
        _container = new ConcurrentHashMap<HandleContainer<THandleClass, THandle>, Integer>();
    }

    public boolean Add(THandleClass handleClass, THandle dep)
    {
        return _container.putIfAbsent(new HandleContainer<THandleClass, THandle>(handleClass, dep), 0) == null;
    }

    public boolean Remove(THandleClass handleClass, THandle dep)
    {
        return _container.remove(new HandleContainer<THandleClass, THandle>(handleClass, dep)) != null;
    }

    public Iterator<HandleContainer<THandleClass, THandle>> iterator(){
        return _container.keySet().iterator();
    }
}
