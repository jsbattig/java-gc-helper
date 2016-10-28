package GCHelper;

import GCHelper.Capability.*;
import GCHelper.Exception.*;
import GCHelper.Interface.*;
import GCHelper.Service.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class UnmanagedObjectGCHelper<THandleClass, THandle> implements HandleRemover<THandleClass, THandle>, Closeable {
    private ConcurrentHashMap<HandleContainer<THandleClass, THandle>, UnmanagedObjectContext<THandleClass, THandle>> _trackedObjects;
    private UnregistrationAgent<THandleClass, THandle> _unregistrationAgent;
    private ExceptionDelegate<THandleClass, THandle> _onException;

    public UnmanagedObjectGCHelper() {
      _trackedObjects = new ConcurrentHashMap<>();
      _unregistrationAgent = new UnregistrationAgent<>(this);
    }

    public void setOnException(ExceptionDelegate<THandleClass, THandle> onException) {
        _onException = onException;
    }

    public ExceptionDelegate<THandleClass, THandle> getOnException() {
        return _onException;
    }

    public void close() throws IOException {
        _unregistrationAgent.close();
    }

    public void StopAgent() throws InterruptedException {
        _unregistrationAgent.Stop();
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
                System.out.println("New object tracked(" + handleClass.toString() + ":" + obj.toString() + ") called");
                for (HandleContainer<THandleClass, THandle> dep : trackedObject.getDependencies())
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
                continue; // Object just dropped and removed from another thread. Let's try again
            /* If object already existed, under normal conditions AddRefCount() must return a value > 1.
             * If it returns <= 1 it means it just got decremented in another thread, reached zero and
             * it's about to be destroyed. So we will have to wait for that to happen and try again our
             * entire operation */
            int newRefCount = existingContextObj.AddRefCount();
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
        for (HandleContainer<THandleClass, THandle> dep : trackedObject.getDependencies())
          AddDependency(trackedObject, dep);
    }

    public void Register(THandleClass handleClass, THandle obj,
                         DestroyHandleDelegate<THandle> destroyHandle) throws EObjectNotFound, EInvalidRefCount {
        Register(handleClass, obj, destroyHandle, null);
    }

    public void Register(THandleClass handleClass, THandle obj) throws EObjectNotFound, EInvalidRefCount {
        Register(handleClass, obj, null, null);
    }

    public void RemoveAndDestroyHandle(THandleClass handleClass, THandle obj)
    {
        try
        {
            HandleContainer<THandleClass, THandle> handle = new HandleContainer<>(handleClass, obj);
            UnmanagedObjectContext<THandleClass, THandle> objContext;
            if ((objContext = _trackedObjects.get(handle)) == null)
                throw new EObjectNotFound(handle.getHandleClass().toString(), handle.getHandle().toString());
            int newRefCount = objContext.ReleaseRefCount();
            if (newRefCount > 0)
                return; // Object still alive
            if (newRefCount < 0)
                throw new EInvalidRefCount(handleClass.toString(), obj.toString(), newRefCount);
            if (_trackedObjects.remove(handle) == null)
                throw new EFailedObjectRemoval(handle.getHandleClass().toString(), handle.getHandle().toString());
            objContext.DestroyAndFree(obj);
            System.out.println("DestroyAndFree(" + handleClass.toString() + ":" + obj.toString() + ") called");
            for (HandleContainer<THandleClass, THandle> dep : objContext.getDependencies())
              Unregister(dep.getHandleClass(), dep.getHandle());
        }
        catch (Exception e)
        {
            if (_onException == null)
                return; // We won't let exception float in our GC helper thread
            _onException.ExceptionReport(this, e, handleClass, obj);
        }
    }

    public void Unregister(THandleClass handleClass, THandle obj) {
        _unregistrationAgent.Enqueue(handleClass, obj);
    }

    private void AddDependency(UnmanagedObjectContext<THandleClass, THandle> trackedObjectContext,
                               HandleContainer<THandleClass, THandle> dependency) throws EObjectNotFound
    {
        UnmanagedObjectContext<THandleClass, THandle> depContext;
        if ((depContext = _trackedObjects.get(dependency)) == null)
            throw new EObjectNotFound(dependency.getHandleClass().toString(), dependency.getHandle().toString());
        if (trackedObjectContext.getDependencies().Add(dependency.getHandleClass(), dependency.getHandle()))
            depContext.AddRefCount();
    }

    private class UnmanagedObjectContextTuple {
        public UnmanagedObjectContext<THandleClass, THandle> context1;
        public UnmanagedObjectContext<THandleClass, THandle> context2;
    }

    private void GetObjectsContexts(HandleContainer obj1,
                                    HandleContainer obj2,
                                    UnmanagedObjectContextTuple contextTuple) throws EObjectNotFound
    {
        if ((contextTuple.context1 = _trackedObjects.get(obj1)) == null)
            throw new EObjectNotFound(obj1.getHandleClass().toString(), obj1.getHandle().toString());
        if ((contextTuple.context2 = _trackedObjects.get(obj2)) == null)
            throw new EObjectNotFound(obj2.getHandleClass().toString(), obj2.getHandle().toString());
    }

    public void AddDependency(THandleClass handleClass, THandle obj,
                              THandleClass depHandleClass, THandle dep) throws EObjectNotFound
    {
        HandleContainer<THandleClass, THandle> objTuple = new HandleContainer<>(handleClass, obj);
        UnmanagedObjectContext<THandleClass, THandle> objContext;
        if ((objContext = _trackedObjects.get(objTuple)) == null)
            throw new EObjectNotFound(handleClass.toString(), obj.toString());
        AddDependency(objContext, new HandleContainer<>(depHandleClass, dep));
    }

    public void RemoveDependency(THandleClass handleClass, THandle obj,
                                 THandleClass depHandleClass, THandle dep) throws EObjectNotFound, EDependencyNotFound
    {
        HandleContainer<THandleClass, THandle> objTuple = new HandleContainer<>(handleClass, obj);
        HandleContainer<THandleClass, THandle> depTuple = new HandleContainer<>(depHandleClass, dep);
        UnmanagedObjectContextTuple objectContextTuple = new UnmanagedObjectContextTuple();
        GetObjectsContexts(objTuple, depTuple, objectContextTuple);
        if (!objectContextTuple.context1.getDependencies().Remove(depTuple.getHandleClass(), depTuple.getHandle()))
            throw new EDependencyNotFound(depHandleClass.toString(), dep.toString());
        Unregister(depHandleClass, dep);
    }
}
