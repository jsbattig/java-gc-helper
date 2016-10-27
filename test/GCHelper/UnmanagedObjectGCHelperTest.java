package GCHelper;

import org.junit.Assert;
import org.junit.Test;

public class UnmanagedObjectGCHelperTest {
    @Test
    public void BasicTest(){
        UnmanagedObjectGCHelper<String, Integer> _unmanagedObjectHandler = new UnmanagedObjectGCHelper<String, Integer>();
        Assert.assertTrue(_unmanagedObjectHandler != null);
    }
}
