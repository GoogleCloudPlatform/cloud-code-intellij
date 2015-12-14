package com.google.gct.idea.debugger.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

/**
 * Opens the Cloud Debugger help page.
 */
public class CloudDebugHelpAction extends AnAction {
  private static final Icon icon = IconLoader.getIcon("/actions/help.png");
  private String url;

  public CloudDebugHelpAction(String url) {
    this.url = url;
  }

  public void actionPerformed(AnActionEvent event) {
    openUrl();
  }

  /**
   * Opens the URL in a browser with BrowserUtil.
   */
  protected void openUrl() {
    BrowserUtil.browse(url);
  }

  /**
   * Sets the help button's icon and label text.
   */
  public void update(AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    presentation.setIcon(icon);
    presentation.setText(CommonBundle.getHelpButtonText());
  }
}
