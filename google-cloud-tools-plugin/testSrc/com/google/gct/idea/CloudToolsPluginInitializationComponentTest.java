package com.google.gct.idea;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.api.client.util.Lists;
import com.google.gct.idea.debugger.CloudDebugConfigType;
import com.google.gct.idea.util.IntelliJPlatform;
import com.google.gct.idea.util.PlatformInfo;

import com.intellij.util.PlatformUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

/**
 * Tests to validate initialization on supported platforms
 */
public class CloudToolsPluginInitializationComponentTest {

  @Test
  public void testInitComponent_AndroidStudio() {
    CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());

    spy.initComponent("AndroidStudio", false);
    verify(spy).initComponent("AndroidStudio", false);
    verifyNoMoreInteractions(spy);
  }

  @Test
  public void testInitComponent_AndroidStudioWithDebugger() {
    CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());
    doNothing().when(spy).enableCloudDebugger();

    spy.initComponent("AndroidStudio", true);
    verify(spy).initComponent("AndroidStudio", true);
    verify(spy).enableCloudDebugger();
    verifyNoMoreInteractions(spy);
  }

  @Test
  public void testInitComponent_IdeaPlatforms() {
    for (boolean enableCdb : new boolean[]{true, false}) {
      for (String platform : PlatformInfo.SUPPORTED_PLATFORMS) {

        CloudToolsPluginInitializationComponent spy = spy(
            new CloudToolsPluginInitializationComponent());
        doNothing().when(spy).enableCloudDebugger();
        doNothing().when(spy).enableFeedbackUtil();

        spy.initComponent(platform, enableCdb);
        verify(spy).initComponent(platform, enableCdb);
        verify(spy).enableCloudDebugger();
        verify(spy).enableFeedbackUtil();
        verifyNoMoreInteractions(spy);
      }
    }
  }

  @Test
  public void testInitComponent_OtherPlatforms() {
    List<String> platforms = Lists.newArrayList();
    for (IntelliJPlatform platform : IntelliJPlatform.values()) {
      platforms.add(platform.getPlatformPrefix());
    }

    platforms.add("SomeGibberishPlatformASDF1234");
    platforms.removeAll(PlatformInfo.SUPPORTED_PLATFORMS);

    for (String platform : platforms) {
      CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());

      spy.initComponent(platform, false);
      verify(spy).initComponent(platform, false);
      verifyNoMoreInteractions(spy);
    }
  }

}
