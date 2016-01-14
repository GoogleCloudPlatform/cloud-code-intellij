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

  private Properties properties;

  @Before
  public void setUp() {
    properties = System.getProperties();
  }

  @After
  public void tearDown() {
    System.setProperties(properties);
  }

  @Test
  public void testInitComponent_AndroidStudio() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "AndroidStudio");
    System.setProperty(CloudDebugConfigType.GCT_DEBUGGER_ENABLE, "false");

    CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());

    spy.initComponent();
    verify(spy).initComponent();
    verifyNoMoreInteractions(spy);
  }

  @Test
  public void testInitComponent_AndroidStudioWithDebugger() {
    System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "AndroidStudio");
    System.setProperty(CloudDebugConfigType.GCT_DEBUGGER_ENABLE, "true");

    CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());
    doNothing().when(spy).enableCloudDebugger();

    spy.initComponent();
    verify(spy).initComponent();
    verify(spy).enableCloudDebugger();
    verifyNoMoreInteractions(spy);
  }

  @Test
  public void testInitComponent_IdeaPlatforms() {
    for (String platform : PlatformInfo.SUPPORTED_PLATFORMS) {
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platform);

      CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());
      doNothing().when(spy).enableCloudDebugger();
      doNothing().when(spy).enableFeedbackUtil();

      spy.initComponent();
      verify(spy).initComponent();
      verify(spy).enableCloudDebugger();
      verify(spy).enableFeedbackUtil();
      verifyNoMoreInteractions(spy);
    }
  }

  @Test
  public void testInitComponent_OtherPlatforms() {
    List<String> platforms = Lists.newArrayList();
    for (IntelliJPlatform platform : IntelliJPlatform.values()) {
      platforms.add(platform.getPlatformPrefix());
    }

    platforms.removeAll(PlatformInfo.SUPPORTED_PLATFORMS);

    for (String platform : platforms) {
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platform);
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, "HopefullyAGibberishPlatform");

      CloudToolsPluginInitializationComponent spy = spy(new CloudToolsPluginInitializationComponent());

      spy.initComponent();
      verify(spy).initComponent();
      verifyNoMoreInteractions(spy);
    }
  }

}
