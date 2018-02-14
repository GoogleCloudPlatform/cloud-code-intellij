/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.intellij.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.v2.model.Breakpoint;
import com.google.api.services.clouddebugger.v2.model.SourceLocation;
import com.google.cloud.tools.intellij.analytics.GctTracking;
import com.google.cloud.tools.intellij.analytics.UsageTrackerProvider;
import com.google.cloud.tools.intellij.debugger.CloudDebugProcessStateController.SetBreakpointHandler;
import com.google.cloud.tools.intellij.debugger.CloudLineBreakpointType.CloudLineBreakpoint;
import com.google.cloud.tools.intellij.util.GctBundle;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Cloud Debugger breakpoint handler is responsible for creating or deleting the breakpoints on
 * the server. It is responsible for creating local IDE representations given a server
 * representation. It stores a relationship between the local IDE representation and the server
 * breakpoint and provides methods for getting one given the other. {@link XBreakpointHandler} is
 * often called by IntelliJ when the user enables or disables breakpoints.
 *
 * <p>This class is threadsafe. All operations can be called by multiple threads.
 *
 * <p>Note there are three breakpoint types managed in the debugger and are named to avoid
 * confusion:
 *
 * <ul>
 *   <li>xIdeBreakpoint : these are {@link XBreakpoint} types that IntelliJ defines and are sealed.
 *   <li>cloudIdeBreakpoint: these are our IDE defined customized breakpoints {@link
 *       CloudLineBreakpoint} with custom properties, etc. It's confusing that there are two
 *       hierarchies for IDE breakpoints, but IntelliJ does this to control some base behavior.
 *   <li>serverBreakpoint: these are the {@link Breakpoint} breakpoints our server gives us via the
 *       client api.
 * </ul>
 */
public class CloudBreakpointHandler
    extends XBreakpointHandler<XLineBreakpoint<CloudLineBreakpointProperties>> {

  public static final Key<String> CLOUD_ID = Key.create("CloudId");

  private static final Logger LOG = Logger.getInstance(CloudBreakpointHandler.class);
  private final Map<String, XBreakpoint> ideBreakpoints =
      new ConcurrentHashMap<String, XBreakpoint>();
  private final CloudDebugProcess process;
  private PsiManager psiManager;
  private ServerToIdeFileResolver fileResolver;

  /** Initializes the handler. */
  public CloudBreakpointHandler(
      @NotNull CloudDebugProcess process, ServerToIdeFileResolver fileResolver) {
    super(CloudLineBreakpointType.class);
    this.process = process;
    setPsiManager(PsiManager.getInstance(this.process.getXDebugSession().getProject()));
    this.fileResolver = fileResolver;
  }

  /**
   * Called when the user "reactivates" an existing snapshot to create a new breakpoint, this clones
   * state into a new breakpoint. It is different from "createIdeRepresentationsIfNecessary" in the
   * following respects:
   *
   * <p>
   *
   * <ul>
   *   <li>It only operates on final state breakpoints.
   *   <li>It always clones them into a new IDE breakpoint.
   *   <li>It does not set "created by server = true", so when control flow comes back into
   *       register, it will register with the server.
   * </ul>
   */
  public void cloneToNewBreakpoints(@NotNull final List<Breakpoint> serverBreakpoints) {
    for (Breakpoint serverBreakpoint : serverBreakpoints) {
      if (!Boolean.TRUE.equals(serverBreakpoint.getIsFinalState())) {
        continue;
      }

      Project currentProject = process.getXDebugSession().getProject();
      final XBreakpointManager manager =
          XDebuggerManager.getInstance(process.getXDebugSession().getProject())
              .getBreakpointManager();
      if (serverBreakpoint.getLocation() == null) {
        LOG.warn(
            "attempted to clone a breakpoint without a source location: "
                + StringUtil.notNullize(serverBreakpoint.getId()));
        continue;
      }

      String path = serverBreakpoint.getLocation().getPath();
      if (Strings.isNullOrEmpty(path)) {
        continue;
      }

      final VirtualFile file = fileResolver.getFileFromPath(currentProject, path);
      final int line = serverBreakpoint.getLocation().getLine() - 1;
      if (file == null) {
        LOG.warn(
            "attempted to clone a breakpoint whose file doesn't exist locally: "
                + StringUtil.notNullize(serverBreakpoint.getLocation().getPath()));
        continue;
      }

      final XLineBreakpoint existing =
          manager.findBreakpointAtLine(CloudLineBreakpointType.getInstance(), file, line);
      if (existing != null) {
        ApplicationManager.getApplication()
            .runWriteAction(
                new Runnable() {
                  @Override
                  public void run() {
                    manager.removeBreakpoint(existing);
                  }
                });
      }

      final CloudLineBreakpointProperties properties = new CloudLineBreakpointProperties();
      final Breakpoint finalserverBreakpoint = serverBreakpoint;
      ApplicationManager.getApplication()
          .runWriteAction(
              new Runnable() {
                @Override
                public void run() {
                  if (finalserverBreakpoint.getExpressions() != null
                      && finalserverBreakpoint.getExpressions().size() > 0) {
                    properties.setWatchExpressions(
                        finalserverBreakpoint
                            .getExpressions()
                            .toArray(new String[finalserverBreakpoint.getExpressions().size()]));
                  }

                  XLineBreakpoint<CloudLineBreakpointProperties> newxIdeBreakpoint =
                      manager.addLineBreakpoint(
                          CloudLineBreakpointType.getInstance(), file.getUrl(), line, properties);

                  // Condition, watches.
                  if (!Strings.isNullOrEmpty(finalserverBreakpoint.getCondition())) {
                    newxIdeBreakpoint.setCondition(finalserverBreakpoint.getCondition());
                  }
                }
              });
    }

    UsageTrackerProvider.getInstance()
        .trackEvent(GctTracking.CLOUD_DEBUGGER_CLONE_BREAKPOINTS)
        .ping();
  }

  /**
   * Called when new breakpoints are encountered in polling the server, this method possibly creates
   * local representations of those breakpoints if there isn't one already at that line.
   */
  public void createIdeRepresentationsIfNecessary(
      @NotNull final List<Breakpoint> serverBreakpoints) {
    boolean addedBreakpoint = false;
    for (final Breakpoint serverBreakpoint : serverBreakpoints) {
      if (Boolean.TRUE.equals(serverBreakpoint.getIsFinalState())) {
        continue;
      }

      if (ideBreakpoints.containsKey(serverBreakpoint.getId())) {
        final XBreakpoint xIdeBreakpoint = ideBreakpoints.get(serverBreakpoint.getId());
        com.intellij.debugger.ui.breakpoints.Breakpoint cloudIdeBreakpoint =
            BreakpointManager.getJavaBreakpoint(xIdeBreakpoint);

        if (cloudIdeBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
          CloudLineBreakpointType.CloudLineBreakpoint cloudIdeLineBreakpoint =
              (CloudLineBreakpointType.CloudLineBreakpoint) cloudIdeBreakpoint;
          cloudIdeLineBreakpoint.setVerified(true);
          cloudIdeLineBreakpoint.setErrorMessage(null);
          process.updateBreakpointPresentation(cloudIdeLineBreakpoint);
        }
        continue;
      }

      Project currentProject = process.getXDebugSession().getProject();
      final XBreakpointManager manager =
          XDebuggerManager.getInstance(process.getXDebugSession().getProject())
              .getBreakpointManager();
      if (serverBreakpoint.getLocation() == null) {
        continue;
      }

      String path = serverBreakpoint.getLocation().getPath();
      if (Strings.isNullOrEmpty(path)) {
        continue;
      }

      final VirtualFile file = fileResolver.getFileFromPath(currentProject, path);
      final int line = serverBreakpoint.getLocation().getLine() - 1;
      if (file == null) {
        continue;
      }
      final XLineBreakpoint existingXIdeBreakpoint =
          manager.findBreakpointAtLine(CloudLineBreakpointType.getInstance(), file, line);
      if (existingXIdeBreakpoint != null && existingXIdeBreakpoint.isEnabled()) {
        continue;
      }
      if (existingXIdeBreakpoint != null) {
        ApplicationManager.getApplication()
            .runWriteAction(
                new Runnable() {
                  @Override
                  public void run() {
                    manager.removeBreakpoint(existingXIdeBreakpoint);
                  }
                });
      }

      final CloudLineBreakpointProperties properties = new CloudLineBreakpointProperties();
      properties.setCreatedByServer(true);
      addedBreakpoint = true;
      ApplicationManager.getApplication()
          .runWriteAction(
              new DoUpdateIdeWithBreakpoint(
                  manager, file, line, properties, serverBreakpoint, ideBreakpoints, process));
    }

    if (addedBreakpoint) {
      // If we added a new breakpoint, the snapshot list needs to be refreshed.
      // However, since adding is async depending on the ability to invoke a write action,
      // we want to queue the refresh after all the write actions we just did.
      ApplicationManager.getApplication()
          .runWriteAction(
              new Runnable() {
                @Override
                public void run() {
                  process.fireBreakpointsChanged();
                }
              });
    }
  }

  /** Called when the user deletes a snapshot from the snapshot list. */
  public void deleteBreakpoint(@NotNull Breakpoint serverBreakpoint) {
    if (!Boolean.TRUE.equals(serverBreakpoint.getIsFinalState())) {
      setStateToDisabled(serverBreakpoint);
    }
    process.getStateController().deleteBreakpointAsync(serverBreakpoint.getId());
  }

  /**
   * Returns the XBreakpoint corresponding to the given server breakpoint.
   *
   * @param serverBreakpoint the server breakpoint representation
   * @return the local IDE representation in x-breakpoint form, if enabled
   */
  @Nullable
  public XBreakpoint getEnabledXBreakpoint(@NotNull Breakpoint serverBreakpoint) {
    XBreakpoint ideBreakpoint = getXBreakpoint(serverBreakpoint);
    return (ideBreakpoint == null || !ideBreakpoint.isEnabled()) ? null : ideBreakpoint;
  }

  /**
   * Returns the XBreakpoint corresponding to the given server breakpoint.
   *
   * @param serverBreakpoint The server breakpoint representation
   * @return the local IDE representation in x-breakpoint form
   */
  @Nullable
  public XBreakpoint getXBreakpoint(@Nullable Breakpoint serverBreakpoint) {
    if (serverBreakpoint == null) {
      return null;
    }
    return ideBreakpoints.get(serverBreakpoint.getId());
  }

  /**
   * Called when the user clicks a pending breakpoint. We find the local IDE representation and
   * navigate to its line of code.
   */
  public void navigateTo(@NotNull Breakpoint serverBreakpoint) {
    final XBreakpoint xIdeBreakpoint = ideBreakpoints.get(serverBreakpoint.getId());
    if (xIdeBreakpoint != null
        && xIdeBreakpoint.getSourcePosition() != null
        && process.getXDebugSession() != null) {
      xIdeBreakpoint
          .getSourcePosition()
          .createNavigatable(process.getXDebugSession().getProject())
          .navigate(true);
    }
  }

  /**
   * Called by IntelliJ when the user has enabled or created a new breakpoint. Creates the server
   * breakpoint.
   *
   * @param ideBreakpoint breakpoint to register
   */
  @Override
  public void registerBreakpoint(
      @NotNull final XLineBreakpoint<CloudLineBreakpointProperties> ideBreakpoint) {
    if (ideBreakpoint.getSourcePosition() == null
        || !ideBreakpoint.isEnabled()
        || !(ideBreakpoint.getType() instanceof CloudLineBreakpointType)) {
      return;
    }
    com.intellij.debugger.ui.breakpoints.Breakpoint cloudIdeBreakpoint =
        BreakpointManager.getJavaBreakpoint(ideBreakpoint);
    if (!(cloudIdeBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint)) {
      LOG.error(
          "breakpoint was not of the correct type to create on the cloud.  It was not a "
              + "CloudLineBreakpoint");
      return;
    }

    final CloudLineBreakpointType.CloudLineBreakpoint cloudIdeLineBreakpoint =
        (CloudLineBreakpointType.CloudLineBreakpoint) cloudIdeBreakpoint;
    if (ideBreakpoint.getProperties().isCreatedByServer()) {
      // This newly created IDE breakpoint could be the result of syncing with the server state,
      // in which case, there is nothing to do.  Just return.
      // Note that we need to implement this flag as a transient property because this method
      // gets called during construction.
      return;
    }
    if (ideBreakpoint.getProperties().isAddedOnServer()
        && !ideBreakpoint.getProperties().isDisabledByServer()) {
      // If this breakpoint was already added, but was hit offline, let's not add it again once we
      // resume a debug session. unless this reigster request comes from re-enabling a previously
      // disabled breakpoint
      return;
    }

    PsiFile javaFile = psiManager.findFile(ideBreakpoint.getSourcePosition().getFile());
    if (!(javaFile instanceof PsiJavaFile)) {
      return;
    }
    SourceLocation location = new SourceLocation();
    // Sending the file as com/package/example/Class.java to Cloud Debugger because it plays nice
    // with the CDB plugin. See ServerToIdeFileResolver.
    location.setPath(ServerToIdeFileResolver.getCloudPathFromJavaFile((PsiJavaFile) javaFile));
    location.setLine(ideBreakpoint.getSourcePosition().getLine() + 1);

    Breakpoint serverNewBreakpoint = new Breakpoint();
    serverNewBreakpoint.setLocation(location);
    if (ideBreakpoint.getConditionExpression() != null) {
      serverNewBreakpoint.setCondition(ideBreakpoint.getConditionExpression().getExpression());
    }

    List<String> watches = cloudIdeLineBreakpoint.getWatchExpressions();
    if (watches != null) {
      serverNewBreakpoint.setExpressions(watches);
    }

    // The breakpoint will enter error state asynchronously.  For now, we state that its verified.
    process
        .getStateController()
        .setBreakpointAsync(
            serverNewBreakpoint,
            new SetBreakpointHandler() {
              @Override
              public void onSuccess(@NotNull final String id) {
                Runnable runnable =
                    new Runnable() {
                      @Override
                      public void run() {
                        if (!Strings.isNullOrEmpty(id)) {
                          if (!cloudIdeLineBreakpoint.isEnabled()) {
                            process
                                .getStateController()
                                .deleteBreakpointAsync(id); // race condition
                          } else { // Success.
                            // Mark as added so we don't add it again.
                            ideBreakpoint.getProperties().setAddedOnServer(true);
                            cloudIdeLineBreakpoint.setErrorMessage(null);
                            process.updateBreakpointPresentation(cloudIdeLineBreakpoint);
                          }
                        } else {
                          // TODO(joaomartins): Why couldn't the breakpoint be set? Improve this
                          // message.
                          cloudIdeLineBreakpoint.setErrorMessage(
                              GctBundle.getString("clouddebug.errorset"));
                          process.updateBreakpointPresentation(cloudIdeLineBreakpoint);
                        }
                        if (!Strings.isNullOrEmpty(id)) {
                          ideBreakpoint.getProperties().setDisabledByServer(false);
                          String oldId = ideBreakpoint.getUserData(CLOUD_ID);
                          if (!Strings.isNullOrEmpty(oldId)) {
                            ideBreakpoints.remove(oldId);
                          }

                          ideBreakpoint.putUserData(CLOUD_ID, id);
                          ideBreakpoints.put(id, ideBreakpoint);
                        }
                      }
                    };
                if (ApplicationManager.getApplication().isUnitTestMode()) {
                  runnable.run();
                } else {
                  SwingUtilities.invokeLater(runnable);
                }
              }

              @Override
              public void onError(String errorMessage) {
                cloudIdeLineBreakpoint.setErrorMessage(errorMessage);
                process.updateBreakpointPresentation(cloudIdeLineBreakpoint);
              }
            });
  }

  void setPsiManager(PsiManager psiManager) {
    this.psiManager = psiManager;
  }

  /**
   * Called when the server records a new snapshot, we find the IDE representation and disable it.
   */
  public void setStateToDisabled(@NotNull Breakpoint serverBreakpoint) {
    final XBreakpoint ideBreakpoint = ideBreakpoints.get(serverBreakpoint.getId());
    if (ideBreakpoint != null
        && ideBreakpoint.getProperties() instanceof CloudLineBreakpointProperties) {
      CloudLineBreakpointProperties properties =
          (CloudLineBreakpointProperties) ideBreakpoint.getProperties();
      properties.setDisabledByServer(true);
      SwingUtilities.invokeLater(
          new Runnable() {
            @Override
            public void run() {
              ideBreakpoint.setEnabled(false);
            }
          });
    }
  }

  @Override
  public void unregisterBreakpoint(
      @NotNull XLineBreakpoint<CloudLineBreakpointProperties> ideBreakpoint, boolean temporary) {
    // If the state was set to disabled as a result of a server update,
    // then we do not need to update the server side.
    if (!ideBreakpoint.getProperties().isDisabledByServer()) {
      String breakpointId = ideBreakpoint.getUserData(CLOUD_ID);
      if (!Strings.isNullOrEmpty(breakpointId)) {
        process.getStateController().deleteBreakpointAsync(breakpointId);
      } else {
        LOG.warn("could not delete breakpoint because it was not added through the cloud handler.");
      }
    }
    // reset this flag: either it has been disabled by the server or the client has deleted it, in
    // both cases we need to add it again, if it is re-enabled
    ideBreakpoint.getProperties().setAddedOnServer(false);
  }
}
