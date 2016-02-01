package com.google.gct.idea.debugger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

import com.google.gct.idea.testing.BasePluginTestCase;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

import org.hamcrest.beans.HasPropertyWithValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudDebugGlobalPollerTest extends BasePluginTestCase {

  private CloudDebugProcessState cloudDebugProcessState;

  @Before
  public void setUp() throws Exception {
    cloudDebugProcessState = new CloudDebugProcessState();
  }

  @Test
  public void testPollForChangesFiresNotificationIfBackgroundListeningFails() throws Exception {
    cloudDebugProcessState.setListenInBackground(true);
    Application application = ApplicationManager.getApplication();
    Notifications handler = Mockito.mock(Notifications.class);
    // sending out notifications relies on several static method calls in
    // com.intellij.notification.Notifications, let's subscribe to them and do verification this way
    application.getMessageBus().connect(application).subscribe(Notifications.TOPIC,
        handler);

    CloudDebugGlobalPoller cloudDebugGlobalPoller = new CloudDebugGlobalPoller();
    cloudDebugGlobalPoller.pollForChanges(cloudDebugProcessState);

    assertFalse(cloudDebugProcessState.isListenInBackground());
    verify(handler)
        .notify(argThat(HasPropertyWithValue.<Notification>hasProperty("title",
                                                            is("Error while connecting to Cloud Debugger backend"))));
  }
}
