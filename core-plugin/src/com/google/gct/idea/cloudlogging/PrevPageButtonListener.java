package com.google.gct.idea.cloudlogging;

import com.google.api.services.logging.model.ListLogEntriesResponse;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * Listener for the Prev Page Button
 * Created by amulyau on 7/14/15.
 */
public class PrevPageButtonListener implements ActionListener {

  private final AppEngineLogging controller;
  private final AppEngineLogToolWindowView view;
  private final Project project;

  /**
   * Constructor
   * @param controller Controller for the App Engine Logs
   * @param view View for the App Engine Logs that have the logs display
   * @param project Current application project (not app engine project)
   */
  public PrevPageButtonListener(AppEngineLogging controller, AppEngineLogToolWindowView view,
                                Project project) {
    this.controller = controller;
    this.view = view;
    this.project = project;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Task.Backgroundable logTask = new Task.Backgroundable(project, "Getting Previous Logs List",
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
        ListLogEntriesResponse logResp = controller
            .askForPreviousLog(view.getCurrPage(), view.getPageTokens());

        progressIndicator.setFraction(0.33);
        progressIndicator.setText("66% to finish");
        if ((logResp != null) && (logResp.getEntries() != null)) {
          if (view.getCurrPage() > -1) {
            view.decreasePage();
          }
          view.processLogs(logResp);
          progressIndicator.setFraction(0.66);
          progressIndicator.setText("33% to finish");

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
