package com.google.gct.idea.debugger.ui;

import com.google.gct.idea.debugger.CloudDebugProcessWatcher;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.GctTracking;
import com.google.gct.login.stats.UsageTrackerService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Dialog shown when user presses the red stop button to
 * ask whether IntelliJ should continue to listen for snapshots.
 */
public class ExitDialog extends DialogWrapper {

  private final JLabel myLabel = new JLabel(GctBundle.getString("clouddebug.continuelistening"));

  public ExitDialog(@Nullable Project project) {
    super(project, false, IdeModalityType.MODELESS);

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

    UsageTrackerService.getInstance().trackEvent(
        GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.continuelistening", null);
  }

  @Override
  public void doCancelAction() {
    // stop listening
    CloudDebugProcessWatcher.getInstance().removeWatcher();

    if (getCancelAction().isEnabled()) {
      close(CANCEL_EXIT_CODE);
      UsageTrackerService.getInstance().trackEvent(
          GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "close.stoplistening", null);
    }
  }
}
