package com.google.gct.idea.cloudlogging;

import com.google.api.services.logging.model.ListLogEntriesResponse;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

/**
 * Mouse Listener for the log level filtering combo box
 * Created by amulyau on 7/27/15.
 */
public class LogLevelComboListener extends MouseAdapter {

  private AppEngineLogging controller;
  private AppEngineLogToolWindowView view;
  private Project project;

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs that have the logs display
   * @param project Current application project (not app engine project)
   */
  public LogLevelComboListener (AppEngineLogging controller, AppEngineLogToolWindowView view,
                                Project project) {
    this.controller = controller;
    this.view = view;
    this.project = project;
  }

  /**
   * What to do when mouse enters the combo box list
   * @param e Mouse event
   */
  public void mouseEntered(MouseEvent e) {
    view.setComboBoxLogLevel(e.getPoint());
  }

  /**
   * What to do when mouse clicks the combo box list
   * @param e Mouse event
   */
  public void mouseClicked(MouseEvent e) {
    view.mouseClickedLogLevel(e.getPoint());
    logLevelChange();
  }

  /**
   * What to do when mouse exits the combo box list
   * @param e Mouse event
   */
  public void mouseExited(MouseEvent e) {
    view.mouseExited(e.getPoint());
  }

  /**
   * When log Level changes, we need to get new logs with the filter
   */
  private void logLevelChange() {
    Task.Backgroundable logTask = new Task.Backgroundable(project, "Getting Next page Logs List",
        false, new PerformInBackgroundOption() {
      @Override
      public boolean shouldStartInBackground() {
        return true;
      }
      @Override
      public void processSentToBackground() {}
    }) {

      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setFraction(0.10);
        progressIndicator.setText("90% to finish");

        ListLogEntriesResponse logResp = controller.getLogs();
        view.clearPageTokens();

        progressIndicator.setFraction(0.33);
        progressIndicator.setText("66% to finish");
        while (view.getCurrPage() != -1) { //get to first page when we refresh logs
          view.decreasePage();
        }
        if ((logResp != null) && (logResp.getEntries() != null)) {
          view.processLogs(logResp);

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              view.setLogs();
            }
          });
        } else {
          progressIndicator.setFraction(0.90);
          progressIndicator.setText("10% to finish");

          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              view.setRootText(view.NO_LOGS_LIST_STRING);
            }
          });
        }
      }
    };
    logTask.queue();
  }

}
