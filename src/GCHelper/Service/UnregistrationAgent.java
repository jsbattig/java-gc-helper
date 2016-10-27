package GCHelper.Service;

import GCHelper.Capability.HandleContainer;
import GCHelper.Interface.HandleRemover;
import GCHelper.Threading.AutoResetEvent;

import java.io.Closeable;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UnregistrationAgent<THandleClass, THandleType> implements Closeable, Runnable {
    private Thread _unregistrationThread;
    private HandleRemover<THandleClass, THandleType> _handleRemover;
    private boolean _requestedStop;
    private ConcurrentLinkedQueue<HandleContainer<THandleClass, THandleType>> _unregistrationQueue;
    private AutoResetEvent _eventWaitHandle;

    @Override
    public void close() throws IOException {
        try {
            Stop();
        } catch(InterruptedException e) {
        }
    }

    public UnregistrationAgent(HandleRemover<THandleClass, THandleType> handleRemover)
    {
        _handleRemover = handleRemover;
        _eventWaitHandle = new AutoResetEvent(false);
        _unregistrationQueue = new ConcurrentLinkedQueue<>();
        _unregistrationThread = new Thread(this);
        _unregistrationThread.start();
    }

    public void Enqueue(THandleClass handleClass, THandleType handle)
    {
        if (_requestedStop)
            return;
        _unregistrationQueue.add(new HandleContainer<>(handleClass, handle));
        _eventWaitHandle.set();
    }

    public void run()
    {
        while (true)
        {
            HandleContainer<THandleClass, THandleType> dequeuedHandleContainer;
            try {
                dequeuedHandleContainer = _unregistrationQueue.remove();
            } catch(NoSuchElementException e) {
                if (_requestedStop)
                    return;
                try {
                    _eventWaitHandle.waitOne();
                } catch (InterruptedException e2) {
                    return;
                }
                continue;
            }
            _handleRemover.RemoveAndDestroyHandle(dequeuedHandleContainer.getHandleClass(), dequeuedHandleContainer.getHandle());
        }
    }

    public void Stop() throws InterruptedException
    {
        if (_requestedStop)
            return;
        _requestedStop = true;
        _eventWaitHandle.set();
        _unregistrationThread.join();
    }
}