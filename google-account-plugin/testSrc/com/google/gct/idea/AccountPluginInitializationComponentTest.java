package com.google.gct.idea;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.gct.idea.util.IntelliJPlatform;
import com.google.gct.idea.util.PlatformInfo;

import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.PlatformUtils;

import java.util.List;

public class AccountPluginInitializationComponentTest {

  @Test
  public void testInitComponent_Idea() {
    for (String platform : PlatformInfo.SUPPORTED_PLATFORMS) {
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platform);
      Application mockApplication = mock(Application.class);
      when(mockApplication.isUnitTestMode()).thenReturn(false);
      ApplicationManager.setApplication(mockApplication, mock(Disposable.class));


      AccountPluginInitializationComponent spy = spy(new AccountPluginInitializationComponent());
      doNothing().when(spy).enableUsageTracking();
      doNothing().when(spy).enableFeedbackUtil();

      spy.initComponent();
      verify(spy).initComponent();
      verify(spy).enableFeedbackUtil();
      verify(spy).enableUsageTracking();
      verifyNoMoreInteractions(spy);
    }
  }

  @Test
  public void testInitComponent_OtherPlatforms() {
    List<String> platforms = Lists.newArrayList();
    for (IntelliJPlatform platform : IntelliJPlatform.values()) {
      platforms.add(platform.getPlatformPrefix());
    }
    platforms.add("AndroidStudio");

    platforms.removeAll(PlatformInfo.SUPPORTED_PLATFORMS);

    for (String platform : platforms) {
      System.setProperty(PlatformUtils.PLATFORM_PREFIX_KEY, platform);
      AccountPluginInitializationComponent spy = spy(new AccountPluginInitializationComponent());
      spy.initComponent();
      verify(spy).initComponent();
      verifyNoMoreInteractions(spy);
    }
  }

}