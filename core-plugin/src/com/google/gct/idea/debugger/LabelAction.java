package com.google.gct.idea.debugger;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * A no-op action to stuff a label into a toolbar.
 */
class LabelAction extends AnAction {
  public LabelAction(String text) {
    super(text);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }
}
