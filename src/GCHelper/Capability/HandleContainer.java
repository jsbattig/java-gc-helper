package GCHelper.Capability;

import java.lang.Object;

public class HandleContainer<THandleClass, THandle> {
    private final THandleClass _handleClass;
    private final THandle _handle;

    public HandleContainer(THandleClass handleClass, THandle handle) {
        _handleClass = handleClass;
        _handle = handle;
    }

    @Override
    public int hashCode() {
        return _handleClass.hashCode() ^ _handle.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o.getClass().equals(this.getClass())) &&
               ((HandleContainer<THandleClass, THandle>) o)._handleClass.equals(_handleClass) &&
               ((HandleContainer<THandleClass, THandle>) o)._handle.equals(_handle);
    }

    public THandleClass getHandleClass() {
        return _handleClass;
    }

    public THandle getHandle() {
        return _handle;
    }
}
