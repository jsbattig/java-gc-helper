package GCHelper;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class UnmanagedObjectGCHelper<THandleClass, THandle> implements IHandleRemover<THandleClass, THandle>, Closeable {
    private ConcurrentHashMap<HandleContainer<THandleClass, THandle>, UnmanagedObjectContext<THandleClass, THandle>> _trackedObjects;

    public UnmanagedObjectGCHelper() {
      _trackedObjects = new ConcurrentHashMap<HandleContainer<THandleClass, THandle>, UnmanagedObjectContext<THandleClass, THandle>>();
    }

    public void RemoveAndDestroyHandle(THandleClass handleClass, THandle obj) {

    }

    public void close() throws IOException {

    }
}
