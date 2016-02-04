package com.google.gct.idea.debugger.ui;

import com.google.gdt.eclipse.login.common.LoginListener;

import com.intellij.execution.process.ProcessHandler;

/**
 * Detaches a Cloud Debug sessions represented by an instance of ProcessHandler on a logout event
 */
public class LogoutDebugProcessDetacher<H extends ProcessHandler> implements LoginListener {

  private H processHandler;

  public LogoutDebugProcessDetacher(H processHandler) {
    this.processHandler = processHandler;
  }

  @Override
  public void statusChanged(boolean loggedIn) {
    if (!loggedIn) {
      detachCloudDebugProcess();
    }
  }

  private void detachCloudDebugProcess() {
    processHandler.detachProcess();
  }
}
