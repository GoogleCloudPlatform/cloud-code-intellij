package com.google.gct.idea.debugger;

import org.junit.Assert;
import org.junit.Test;

public class CloudDebugProcessHandlerTest {

    private CloudDebugProcessHandler handler = new CloudDebugProcessHandler(null);

    @Test
    public void testIsSilentlyDisposed() {
        Assert.assertTrue(handler.isSilentlyDestroyOnClose());
    }

    @Test
    public void testDetachIsDefault() {
        Assert.assertTrue(handler.detachIsDefault());
    }

    @Test
    public void testGetProcessInput() {
        Assert.assertNull(handler.getProcessInput());
    }
}
