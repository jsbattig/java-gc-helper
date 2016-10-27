package GCHelper;

import GCHelper.Capability.ConcurrentDependencies;
import GCHelper.Capability.HandleContainer;
import GCHelper.Capability.UnmanagedObjectContext;
import GCHelper.Exception.EFailedObjectRemoval;
import GCHelper.Exception.EInvalidRefCount;
import GCHelper.Exception.EObjectNotFound;
import GCHelper.Interface.DestroyHandleDelegate;
import GCHelper.Interface.HandleRemover;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class UnmanagedObjectGCHelper<THandleClass, THandle> implements HandleRemover<THandleClass, THandle>, Closeable {
    private ConcurrentHashMap<HandleContainer<THandleClass, THandle>, UnmanagedObjectContext<THandleClass, THandle>> _trackedObjects;

    public UnmanagedObjectGCHelper() {
      _trackedObjects = new ConcurrentHashMap<HandleContainer<THandleClass, THandle>, UnmanagedObjectContext<THandleClass, THandle>>();
    }

    public void RemoveAndDestroyHandle(THandleClass handleClass, THandle obj) {

    }

    public void close() throws IOException {

    }

    public void Register(THandleClass handleClass, THandle obj,
                         DestroyHandleDelegate<THandle> destroyHandle,
                         ConcurrentDependencies<THandleClass, THandle> dependencies) throws EObjectNotFound, EInvalidRefCount
    {
        HandleContainer<THandleClass, THandle> handleContainer = new HandleContainer<>(handleClass, obj);
        if(dependencies == null)
            dependencies = new ConcurrentDependencies<>();
        UnmanagedObjectContext<THandleClass, THandle> trackedObject = new UnmanagedObjectContext<>(destroyHandle, dependencies);
        do
        {
            if (_trackedObjects.putIfAbsent(handleContainer, trackedObject) == null)
            {
                for (HandleContainer<THandleClass, THandle> dep : trackedObject.getConcurrentDependencies())
                {
                    UnmanagedObjectContext<THandleClass, THandle> depContext;
                    if ((depContext = _trackedObjects.get(dep)) == null)
                        throw new EObjectNotFound(dep.getHandleClass().toString(), dep.getHandle().toString());
                    depContext.AddRefCount();
                }
                return;
            }
            UnmanagedObjectContext<THandleClass, THandle> existingContextObj;
            if ((existingContextObj = _trackedObjects.get(handleContainer)) == null)
                continue; /* Object just dropped and removed from another thread. Let's try again */
            /* If object already existed, under normal conditions AddRefCount() must return a value > 1.
             * If it returns <= 1 it means it just got decremented in another thread, reached zero and
             * it's about to be destroyed. So we will have to wait for that to happen and try again our
             * entire operation */
            Integer newRefCount = existingContextObj.AddRefCount();
            if (newRefCount <= 0)
                throw new EInvalidRefCount(handleClass.toString(), obj.toString(), newRefCount);
            if (newRefCount == 1)
            {
                /* Object is getting removed in another thread. Let's spin while we wait for it to be gone
                 * from our _trackedObjects container */
                while (_trackedObjects.get(handleContainer) != null)
                    Thread.yield();
                continue;
            }
            trackedObject = existingContextObj;
            /* Object already exists, could be an stale object not yet garbage collected,
             * so we will set the new cleanup methods in place of the current ones */
            trackedObject.setDestroyHandleDelegate(destroyHandle);
            break;
        } while (true);
        for (HandleContainer<THandleClass, THandle> dep : trackedObject.getConcurrentDependencies())
        AddDependency(trackedObject, dep);
    }

    public void Unregister(THandleClass handleClass, THandle obj)
    {
        try
        {
            HandleContainer<THandleClass, THandle> handle = new HandleContainer<>(handleClass, obj);
            UnmanagedObjectContext<THandleClass, THandle> objContext;
            if ((objContext = _trackedObjects.get(handle)) == null)
                throw new EObjectNotFound(handle.getHandleClass().toString(), handle.getHandle().toString());
            Integer newRefCount = objContext.ReleaseRefCount();
            if (newRefCount > 0)
                return; // Object still alive
            if (newRefCount < 0)
                throw new EInvalidRefCount(handleClass.toString(), obj.toString(), newRefCount);
            if (_trackedObjects.remove(handle) == null)
                throw new EFailedObjectRemoval(handle.getHandleClass().toString(), handle.getHandle().toString());
            objContext.DestroyAndFree(obj);
            for (HandleContainer<THandleClass, THandle> dep : objContext.getConcurrentDependencies())
              Unregister(dep.getHandleClass(), dep.getHandle());
        }
        catch (Exception e)
        {
            /*if (OnUnregisterException == null)
                return;
            OnUnregisterException(this, e, handleClass, obj);*/
        }
    }

    public void AddDependency(UnmanagedObjectContext<THandleClass, THandle> trackedObject, HandleContainer<THandleClass, THandle> dependency) {

    }
}
