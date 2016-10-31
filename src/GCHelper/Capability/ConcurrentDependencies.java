package GCHelper.Capability;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentDependencies<THandleClass, THandle> implements Iterable<HandleContainer<THandleClass, THandle>>
{
    private ConcurrentHashMap<HandleContainer<THandleClass, THandle>, Integer> _container;

    public ConcurrentDependencies() {
        _container = new ConcurrentHashMap<>();
    }

    public boolean Add(THandleClass handleClass, THandle dep)
    {
        return _container.putIfAbsent(new HandleContainer<>(handleClass, dep), 0) == null;
    }

    public boolean Remove(THandleClass handleClass, THandle dep)
    {
        return _container.remove(new HandleContainer<>(handleClass, dep)) != null;
    }

    public Iterator<HandleContainer<THandleClass, THandle>> iterator(){
        return _container.keySet().iterator();
    }
}
