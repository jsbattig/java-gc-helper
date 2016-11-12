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
    private volatile boolean _agentRunning;
    private ConcurrentHashMap<HandleContainer<THandleClass, THandle>, UnmanagedObjectContext<THandleClass, THandle>> _trackedObjects;
    private UnregistrationAgent<THandleClass, THandle> _unregistrationAgent;
    private ExceptionDelegate<THandleClass, THandle> _onException;

    public UnmanagedObjectGCHelper() {
        _trackedObjects = new ConcurrentHashMap<>();
        _unregistrationAgent = new UnregistrationAgent<>(this);
        _agentRunning = true;
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
        _agentRunning = false;
        _unregistrationAgent.Stop();
    }

    public void Register(THandleClass handleClass, THandle obj,
                         DestroyHandleDelegate<THandle> destroyHandle,
                         HandleCollection<THandleClass, THandle> parents) throws EObjectNotFound, EInvalidRefCount
    {
        int newRefCount;
        UnmanagedObjectContext<THandleClass, THandle> existingContextObj;
        HandleContainer<THandleClass, THandle> handleContainer = new HandleContainer<>(handleClass, obj);
        UnmanagedObjectContext<THandleClass, THandle> trackedObject = new UnmanagedObjectContext<>(destroyHandle, parents);
        do {
            if ((existingContextObj = _trackedObjects.putIfAbsent(handleContainer, trackedObject)) == null) {
                if (consoleLoggingEnabled)
                    System.out.format("New handle(%s:%s)\r\n", handleClass.toString(), obj.toString());
                if (trackedObject.getParents() == null)
                    return;
                for (HandleContainer<THandleClass, THandle> dep : trackedObject.getParents()) {
                    UnmanagedObjectContext<THandleClass, THandle> depContext;
                    if ((depContext = _trackedObjects.get(dep)) == null)
                        throw new EObjectNotFound(dep.getHandleClass().toString(), dep.getHandle().toString());
                    depContext.AddRefCount();
                }
                return;
            }
            /* If object already existed, under normal conditions AddRefCount() must return a value > 1.
             * If it returns <= 1 it means it just got decremented in another thread, reached zero and
             * it's about to be destroyed. So we will have to wait for that to happen and try again our
             * entire operation */
            newRefCount = existingContextObj.AddRefCount();
            if (newRefCount <= 0)
                throw new EInvalidRefCount(handleClass.toString(), obj.toString(), newRefCount);
            if (newRefCount > 1)
                break;
            if (consoleLoggingEnabled)
                System.out.format("Handle clash(%s:%s)\r\n", handleClass.toString(), obj.toString());
            /* Object is getting removed in another thread. Let's spin while we wait for it to be gone
             * from our _trackedObjects collection */
            while (_trackedObjects.get(handleContainer) != null)
                Thread.yield();
        } while (true);
        /* Object already exists, could be an stale object not yet garbage collected,
         * so we will set the new cleanup methods in place of the current ones */
        existingContextObj.setDestroyHandleDelegate(destroyHandle);
        if(consoleLoggingEnabled)
            System.out.format("Handle(%s:%s) refCount++ =%d\r\n", handleClass.toString(), obj.toString(), newRefCount);
        if(parents == null)
            return;
        for (HandleContainer<THandleClass, THandle> dep : parents)
            AddParent(existingContextObj, dep);
    }

    public void Register(THandleClass handleClass, THandle obj,
                         DestroyHandleDelegate<THandle> destroyHandle) throws EObjectNotFound, EInvalidRefCount {
        Register(handleClass, obj, destroyHandle, null);
    }

    public void Register(THandleClass handleClass, THandle obj) throws EObjectNotFound, EInvalidRefCount {
        Register(handleClass, obj, null, null);
    }

    public void RemoveAndCallDestroyHandleDelegate(THandleClass handleClass, THandle obj)
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
            try {
                objContext.DestroyAndFree(obj);
                if (consoleLoggingEnabled)
                    System.out.format("DestroyAndFree(%s:%s)\r\n", handleClass.toString(), obj.toString());
                if (objContext.getParents() == null)
                    return;
                for (HandleContainer<THandleClass, THandle> dep : objContext.getParents())
                    RemoveParent(handleClass, obj, objContext, dep);
            } finally {
                if (_trackedObjects.remove(handle) == null)
                    throw new EFailedObjectRemoval(handle.getHandleClass().toString(), handle.getHandle().toString());
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
        /* the following code as regards to _agentRunning is not thread safe. But I don't want we pay the cost of a lock operation here
         * not even a spinlock call. We want Unregister to be as fast as possible. Worst thing that can happen we may leak some unmanaged object
         * that gets inserted into _unregistrationAgent queue and never destroyed */
        if(_agentRunning)
            _unregistrationAgent.Enqueue(handleClass, obj);
        else
            RemoveAndCallDestroyHandleDelegate(handleClass, obj);
    }

    private void AddParent(UnmanagedObjectContext<THandleClass, THandle> trackedObjectContext,
                           HandleContainer<THandleClass, THandle> parent) throws EObjectNotFound
    {
        UnmanagedObjectContext<THandleClass, THandle> depContext;
        if ((depContext = _trackedObjects.get(parent)) == null)
            throw new EObjectNotFound(parent.getHandleClass().toString(), parent.getHandle().toString());
        if (trackedObjectContext.getParents() == null)
            trackedObjectContext.initParentCollection();
        if (trackedObjectContext.getParents().Add(parent.getHandleClass(), parent.getHandle())) {
            int newRefCount = depContext.AddRefCount();
            if(consoleLoggingEnabled)
                System.out.format("Dep parent(%s:%s) refCount++ =%d\r\n",
                                  parent.getHandleClass().toString(), parent.getHandle().toString(), newRefCount);
        }
    }

    private void RemoveParent(THandleClass handleClass, THandle obj,
                              UnmanagedObjectContext<THandleClass, THandle> trackedObjectContext,
                              HandleContainer<THandleClass, THandle> parent) throws EObjectNotFound, EParentNotFound
    {
        if (trackedObjectContext.getParents() == null ||
            !trackedObjectContext.getParents().Remove(parent.getHandleClass(), parent.getHandle()))
            throw new EParentNotFound(parent.getHandleClass().toString(), parent.getHandle().toString());
        if(consoleLoggingEnabled)
            System.out.format("Dep child(%s:%s) removed Dep(%s:%s)\r\n", handleClass.toString(), obj.toString(),
                    parent.getHandleClass().toString(), parent.getHandle().toString());
        Unregister(parent.getHandleClass(), parent.getHandle());
    }

    public void AddParent(THandleClass handleClass, THandle obj,
                          THandleClass depHandleClass, THandle dep) throws EObjectNotFound
    {
        HandleContainer<THandleClass, THandle> objTuple = new HandleContainer<>(handleClass, obj);
        UnmanagedObjectContext<THandleClass, THandle> objContext;
        if ((objContext = _trackedObjects.get(objTuple)) == null)
            throw new EObjectNotFound(handleClass.toString(), obj.toString());
        if(consoleLoggingEnabled)
            System.out.format("Dep child(%s:%s)\r\n", handleClass.toString(), obj.toString());
        AddParent(objContext, new HandleContainer<>(depHandleClass, dep));
    }

    public void RemoveParent(THandleClass handleClass, THandle obj,
                             THandleClass depHandleClass, THandle dep) throws EObjectNotFound, EParentNotFound
    {
        HandleContainer<THandleClass, THandle> objTuple = new HandleContainer<>(handleClass, obj);
        UnmanagedObjectContext<THandleClass, THandle> objContext;
        if ((objContext = _trackedObjects.get(objTuple)) == null)
            throw new EObjectNotFound(handleClass.toString(), obj.toString());
        if(consoleLoggingEnabled)
            System.out.format("Dep child(%s:%s)\r\n", handleClass.toString(), obj.toString());
        HandleContainer<THandleClass, THandle> depTuple = new HandleContainer<>(depHandleClass, dep);
        RemoveParent(handleClass, obj, objContext, depTuple);
    }
}
