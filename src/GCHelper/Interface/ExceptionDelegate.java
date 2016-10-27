package GCHelper.Interface;

import GCHelper.UnmanagedObjectGCHelper;

public interface ExceptionDelegate<THandleClass, THandle> {
    void ExceptionReport(UnmanagedObjectGCHelper<THandleClass, THandle> obj, Exception exception, THandleClass handleClass, THandle handle);
}