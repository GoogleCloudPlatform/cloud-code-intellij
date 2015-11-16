package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.debugger.CloudDebugProcessWatcher;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.GctTracking;
import com.google.gct.login.stats.UsageTrackerService.UsageTracker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.IdeFocusManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Dialog shown when user presses the red stop button to
 * ask whether IntelliJ should continue to listen for snapshots.
 */
public class ExitDialog extends DialogWrapper {

  private final JLabel myLabel = new JLabel(GctBundle.getString("clouddebug.continuelistening"));
  private final UsageTracker myTracker;
  private IdeFocusManager focusManager;

  public ExitDialog(@Nullable Project project, UsageTracker tracker) {
    super(project, false, IdeModalityType.MODELESS);
    myTracker = tracker;

    init();

    setOKButtonText(GctBundle.getString("clouddebug.continue"));
    setCancelButtonText(GctBundle.getString("clouddebug.stoplistening"));
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myLabel;
  }

  @Override
  public void doOKAction() {
    super.doOKAction();

    myTracker.trackEvent(
        GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.continuelistening", null);
  }

  @Override
  public void doCancelAction() {
    // stop listening
    CloudDebugProcessWatcher.getInstance().removeWatcher();

    if (getCancelAction().isEnabled()) {
      close(CANCEL_EXIT_CODE);
      myTracker.trackEvent(
          GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.stoplistening", null);
    }
  }

}
