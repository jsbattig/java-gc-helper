package GCHelper;

import java.lang.Object;

public class HandleContainer<THandleClass, THandle> {
    private THandleClass _handleClass;
    private THandle _handle;

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
        return ((HandleContainer<THandleClass, THandle>) o)._handleClass == _handleClass &&
               ((HandleContainer<THandleClass, THandle>) o)._handle == _handle;
    }
}
