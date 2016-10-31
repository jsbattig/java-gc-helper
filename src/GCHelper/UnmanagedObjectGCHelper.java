package GCHelper;

import GCHelper.Capability.*;
import GCHelper.Exception.*;
import GCHelper.Interface.*;
import GCHelper.Service.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class UnmanagedObjectGCHelper<THandleClass, THandle> implements HandleRemover<THandleClass, THandle>, Closeable {
    public static boolean consoleLoggingEnabled = false;
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
        UnmanagedObjectContext<THandleClass, THandle> trackedObject = new UnmanagedObjectContext<>(destroyHandle, dependencies);
        do
        {
            if (_trackedObjects.putIfAbsent(handleContainer, trackedObject) == null)
            {
                if(consoleLoggingEnabled)
                    System.out.format("New handle(%s:%s)\r\n", handleClass.toString(), obj.toString());
                if(trackedObject.getDependencies() == null)
                    return;
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
                if(consoleLoggingEnabled)
                    System.out.format("Handle clash(%s:%s)\r\n", handleClass.toString(), obj.toString());
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
            if(consoleLoggingEnabled)
                System.out.format("Handle(%s:%s) refCount++ =%d\r\n", handleClass.toString(), obj.toString(), newRefCount);
            break;
        } while (true);
        if(dependencies == null)
            return;
        for (HandleContainer<THandleClass, THandle> dep : dependencies)
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
            if(consoleLoggingEnabled)
                System.out.format("Handle(%s:%s) refCount-- =%d\r\n", handleClass.toString(), obj.toString(), newRefCount);
            if (newRefCount > 0)
                return; // Object still alive
            if (newRefCount < 0)
                throw new EInvalidRefCount(handleClass.toString(), obj.toString(), newRefCount);
            if (_trackedObjects.remove(handle) == null)
                throw new EFailedObjectRemoval(handle.getHandleClass().toString(), handle.getHandle().toString());
            objContext.DestroyAndFree(obj);
            if(consoleLoggingEnabled)
                System.out.format("DestroyAndFree(%s:%s)\r\n", handleClass.toString(), obj.toString());
            if(objContext.getDependencies() == null)
                return;
            try {
                for (HandleContainer<THandleClass, THandle> dep : objContext.getDependencies())
                    RemoveDependency(handleClass, obj, objContext, dep);
            } catch(EDependencyNotFound e){
                /* This is likely caused because concurrently there was a call to Register who initialized _dependencies field
                *  of our objContext instance. Let's ignore this exception and get out of here */
            }
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
        if (trackedObjectContext.getDependencies() == null)
            trackedObjectContext.initDependencies();
        if (trackedObjectContext.getDependencies().Add(dependency.getHandleClass(), dependency.getHandle())) {
            int newRefCount = depContext.AddRefCount();
            if(consoleLoggingEnabled)
                System.out.format("Dep parent(%s:%s) refCount++ =%d\r\n",
                                  dependency.getHandleClass().toString(), dependency.getHandle().toString(), newRefCount);
        }
    }

    private void RemoveDependency(THandleClass handleClass, THandle obj,
                                  UnmanagedObjectContext<THandleClass, THandle> trackedObjectContext,
                                  HandleContainer<THandleClass, THandle> dependency) throws EObjectNotFound, EDependencyNotFound
    {
        if (trackedObjectContext.getDependencies() == null ||
            !trackedObjectContext.getDependencies().Remove(dependency.getHandleClass(), dependency.getHandle()))
            throw new EDependencyNotFound(dependency.getHandleClass().toString(), dependency.getHandle().toString());
        if(consoleLoggingEnabled)
            System.out.format("Dep child(%s:%s) removed Dep(%s:%s)\r\n", handleClass.toString(), obj.toString(),
                    dependency.getHandleClass().toString(), dependency.getHandle().toString());
        Unregister(dependency.getHandleClass(), dependency.getHandle());
    }

    public void AddDependency(THandleClass handleClass, THandle obj,
                              THandleClass depHandleClass, THandle dep) throws EObjectNotFound
    {
        HandleContainer<THandleClass, THandle> objTuple = new HandleContainer<>(handleClass, obj);
        UnmanagedObjectContext<THandleClass, THandle> objContext;
        if ((objContext = _trackedObjects.get(objTuple)) == null)
            throw new EObjectNotFound(handleClass.toString(), obj.toString());
        if(consoleLoggingEnabled)
            System.out.format("Dep child(%s:%s)\r\n", handleClass.toString(), obj.toString());
        AddDependency(objContext, new HandleContainer<>(depHandleClass, dep));
    }

    public void RemoveDependency(THandleClass handleClass, THandle obj,
                                 THandleClass depHandleClass, THandle dep) throws EObjectNotFound, EDependencyNotFound
    {
        HandleContainer<THandleClass, THandle> objTuple = new HandleContainer<>(handleClass, obj);
        UnmanagedObjectContext<THandleClass, THandle> objContext;
        if ((objContext = _trackedObjects.get(objTuple)) == null)
            throw new EObjectNotFound(handleClass.toString(), obj.toString());
        if(consoleLoggingEnabled)
            System.out.format("Dep child(%s:%s)\r\n", handleClass.toString(), obj.toString());
        HandleContainer<THandleClass, THandle> depTuple = new HandleContainer<>(depHandleClass, dep);
        RemoveDependency(handleClass, obj, objContext, depTuple);
    }
}
