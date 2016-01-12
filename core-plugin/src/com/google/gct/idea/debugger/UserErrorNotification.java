package com.google.gct.idea.debugger;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Utility to classify different errors returned by the Cloud Debugger server
 * and determine what, if anything, to tell the user about them.
 */
class UserErrorNotification {

  static void warn(String operation, GoogleJsonResponseException ex) {
    GoogleJsonError details = ex.getDetails();
    int code = details.getCode();
    // there's usually only one of these
    GoogleJsonError.ErrorInfo firstError = details.getErrors().get(0);
    if (code == 403 && "forbidden".equalsIgnoreCase(firstError.getReason())) {
      // Issue #250
      showErrorDialog("Not logged in", details.getMessage());
    } else {
      // don't bother user
    }
  }

  /**
   * Opens an error dialog with the specified title.
   * Ensures that the error dialog is opened on the UI thread.
   *
   * @param message the message to be displayed
   * @param title the title of the error dialog
   */
  private static void showErrorDialog(final String message, @NotNull final String title) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      Messages.showErrorDialog(message, title);
    } else {
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(message, title);
        }
      }, ModalityState.defaultModalityState());
    }
  }


}
