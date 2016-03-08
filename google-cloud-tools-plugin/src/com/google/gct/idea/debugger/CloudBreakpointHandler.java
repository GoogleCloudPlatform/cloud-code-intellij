/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.google.gct.idea.debugger;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.clouddebugger.model.Breakpoint;
import com.google.api.services.clouddebugger.model.SourceLocation;
import com.google.gct.idea.debugger.CloudDebugProcessStateController.SetBreakpointHandler;
import com.google.gct.idea.util.GctBundle;
import com.google.gct.idea.util.GctTracking;
import com.google.gct.stats.UsageTrackerProvider;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

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
 * <ul>
 * <li>xIdeBreakpoint : these are {@link XBreakpoint} types that IntelliJ defines and are sealed.
 * <li>cloudIdeBreakpoint: these are our IDE defined customized breakpoints
 *                 {@link com.google.gct.idea.debugger.CloudLineBreakpointType.CloudLineBreakpoint}
 *                 with custom properties, etc. It's confusing that there are two hierarchies for
 *                 IDE breakpoints, but IntelliJ does this to control some base behavior.
 * <li>serverBreakpoint: these are the {@link Breakpoint} breakpoints our server gives us via the
 *                 client api.
 */
public class CloudBreakpointHandler
    extends XBreakpointHandler<XLineBreakpoint<CloudLineBreakpointProperties>> {
  public static final Key<String> CLOUD_ID = Key.create("CloudId");

  private static final Logger LOG = Logger.getInstance(CloudBreakpointHandler.class);
  private final Map<String, XBreakpoint> myIdeBreakpoints = new ConcurrentHashMap<String, XBreakpoint>();
  private final CloudDebugProcess myProcess;
  private PsiManager myPsiManager;
  private ServerToIDEFileResolver fileResolver;

  public CloudBreakpointHandler(@NotNull CloudDebugProcess process,
      ServerToIDEFileResolver fileResolver) {
    super(CloudLineBreakpointType.class);
    myProcess = process;
    setPsiManager(PsiManager.getInstance(myProcess.getXDebugSession().getProject()));
    this.fileResolver = fileResolver;
  }

  /**
   * Called when the user "reactivates" an existing snapshot to create a new breakpoint, this clones
   * state into a new breakpoint. It is different from "createIdeRepresentationsIfNecessary" in the
   * following respects:
   *
   * <ul>
   * <li>It only operates on final state breakpoints.
   * <li>It always clones them into a new IDE breakpoint.
   * <li>It does not set "created by server = true", so when control flow comes back into register,
   *     it will register with the server.
   * </ul>
   */
  @SuppressFBWarnings(value="DM_STRING_CTOR", justification="Warning due to string concatenation")
  public void cloneToNewBreakpoints(@NotNull final List<Breakpoint> serverBreakpoints) {
    for (Breakpoint serverBreakpoint : serverBreakpoints) {
      if (serverBreakpoint.getIsFinalState() != Boolean.TRUE) {
        continue;
      }

      Project currentProject = myProcess.getXDebugSession().getProject();
      final XBreakpointManager manager =
        XDebuggerManager.getInstance(myProcess.getXDebugSession().getProject())
            .getBreakpointManager();
      if (serverBreakpoint.getLocation() == null) {
        LOG.warn("attempted to clone a breakpoint without a source location: " +
                 StringUtil.notNullize(serverBreakpoint.getId()));
        continue;
      }

      String path = serverBreakpoint.getLocation().getPath();
      if (Strings.isNullOrEmpty(path)) {
        continue;
      }

      final VirtualFile file = fileResolver.getFileFromPath(currentProject, path);
      final int line = serverBreakpoint.getLocation().getLine() - 1;
      if (file == null) {
        LOG.warn("attempted to clone a breakpoint whose file doesn't exist locally: " +
                 StringUtil.notNullize(serverBreakpoint.getLocation().getPath()));
        continue;
      }

      final XLineBreakpoint existing = manager.findBreakpointAtLine(
          CloudLineBreakpointType.getInstance(), file, line);
      if (existing != null) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          @Override
          public void run() {
            manager.removeBreakpoint(existing);
          }
        });
      }

      final CloudLineBreakpointProperties properties = new CloudLineBreakpointProperties();
      final Breakpoint finalserverBreakpoint = serverBreakpoint;
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          if (finalserverBreakpoint.getExpressions() != null
              && finalserverBreakpoint.getExpressions().size() > 0) {
            properties.setWatchExpressions(
                finalserverBreakpoint.getExpressions().toArray(
                    new String[finalserverBreakpoint.getExpressions().size()]));
          }

          XLineBreakpoint<CloudLineBreakpointProperties> newxIdeBreakpoint =
              manager.addLineBreakpoint(
                  CloudLineBreakpointType.getInstance(), file.getUrl(), line, properties);

          // Condition, watches.
          if (!Strings.isNullOrEmpty(finalserverBreakpoint.getCondition())) {
            newxIdeBreakpoint.setCondition(finalserverBreakpoint.getCondition());
          }

          UsageTrackerProvider.getInstance().trackEvent(
              GctTracking.CATEGORY, GctTracking.CLOUD_DEBUGGER, "create.breakpoint.clone", null);
        }
      });
    }
  }

  /**
   * Called when new breakpoints are encountered in polling the server, this method possibly creates
   * local representations of those breakpoints if there isn't one already at that line.
   */
  public void createIdeRepresentationsIfNecessary(@NotNull final List<Breakpoint> serverBreakpoints)
  {
    boolean addedBreakpoint = false;
    for (final Breakpoint serverBreakpoint : serverBreakpoints) {
      if (serverBreakpoint.getIsFinalState() == Boolean.TRUE) {
        continue;
      }

      if (myIdeBreakpoints.containsKey(serverBreakpoint.getId())) {
        final XBreakpoint xIdeBreakpoint = myIdeBreakpoints.get(serverBreakpoint.getId());
        com.intellij.debugger.ui.breakpoints.Breakpoint cloudIdeBreakpoint =
            BreakpointManager.getJavaBreakpoint(xIdeBreakpoint);

        if (cloudIdeBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint) {
          CloudLineBreakpointType.CloudLineBreakpoint cloudIdeLineBreakpoint =
              (CloudLineBreakpointType.CloudLineBreakpoint) cloudIdeBreakpoint;
          cloudIdeLineBreakpoint.setVerified(true);
          cloudIdeLineBreakpoint.setErrorMessage(null);
          myProcess.updateBreakpointPresentation(cloudIdeLineBreakpoint);
        }
        continue;
      }

      Project currentProject = myProcess.getXDebugSession().getProject();
      final XBreakpointManager manager = XDebuggerManager.getInstance(
          myProcess.getXDebugSession().getProject()).getBreakpointManager();
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
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
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
          .runWriteAction(new DoUpdateIdeWithBreakpoint(manager,
                                                            file,
                                                            line,
                                                            properties,
                                                            serverBreakpoint,
                                                            myIdeBreakpoints,
                                                            myProcess));
    }

    if (addedBreakpoint) {
      // If we added a new breakpoint, the snapshot list needs to be refreshed.
      // However, since adding is async depending on the ability to invoke a write action,
      // we want to queue the refresh after all the write actions we just did.
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          myProcess.fireBreakpointsChanged();
        }
      });
    }
  }

  /**
   * Called when the user deletes a snapshot from the snapshot list.
   */
  public void deleteBreakpoint(@NotNull Breakpoint serverBreakpoint) {
    if (serverBreakpoint.getIsFinalState() != Boolean.TRUE) {
      setStateToDisabled(serverBreakpoint);
    }
    myProcess.getStateController().deleteBreakpointAsync(serverBreakpoint.getId());
  }

  /**
   * Returns the XBreakpoint corresponding to the given server breakpoint
   *
   * @param serverBreakpoint the server breakpoint representation
   * @return the local IDE representation in x-breakpoint form, if enabled
   */
  @Nullable
  public XBreakpoint getEnabledXBreakpoint(@NotNull Breakpoint serverBreakpoint) {
    XBreakpoint xIdeBreakpoint = getXBreakpoint(serverBreakpoint);
    return (xIdeBreakpoint == null || !xIdeBreakpoint.isEnabled()) ? null : xIdeBreakpoint;
  }

  /**
   * Returns the XBreakpoint corresponding to the given server breakpoint
   *
   * @param serverBreakpoint The server breakpoint representation
   * @return the local IDE representation in x-breakpoint form
   */
  @Nullable
  public XBreakpoint getXBreakpoint(@Nullable Breakpoint serverBreakpoint) {
    if (serverBreakpoint == null) {
      return null;
    }
    return myIdeBreakpoints.get(serverBreakpoint.getId());
  }

  /**
   * Called when the user clicks a pending breakpoint. We find the local IDE representation and
   * navigate to its line of code.
   */
  public void navigateTo(@NotNull Breakpoint serverBreakpoint) {
    final XBreakpoint xIdeBreakpoint = myIdeBreakpoints.get(serverBreakpoint.getId());
    if (xIdeBreakpoint != null
        && xIdeBreakpoint.getSourcePosition() != null
        && myProcess.getXDebugSession() != null) {
      xIdeBreakpoint.getSourcePosition().createNavigatable(
          myProcess.getXDebugSession().getProject()).navigate(true);
    }
  }

  /**
   * Called by IntelliJ when the user has enabled or created a new breakpoint. Creates the server
   * breakpoint.
   *
   * @param xIdeBreakpoint breakpoint to register
   */
  @Override
  public void registerBreakpoint(
      @NotNull final XLineBreakpoint<CloudLineBreakpointProperties> xIdeBreakpoint) {
    if (xIdeBreakpoint.getSourcePosition() == null
        || !xIdeBreakpoint.isEnabled()
        || !(xIdeBreakpoint.getType() instanceof CloudLineBreakpointType)) {
      return;
    }
    com.intellij.debugger.ui.breakpoints.Breakpoint cloudIdeBreakpoint =
      BreakpointManager.getJavaBreakpoint(xIdeBreakpoint);
    if (!(cloudIdeBreakpoint instanceof CloudLineBreakpointType.CloudLineBreakpoint)) {
      LOG.error("breakpoint was not of the correct type to create on the cloud.  It was not a " +
                "CloudLineBreakpoint");
      return;
    }

    final CloudLineBreakpointType.CloudLineBreakpoint cloudIdeLineBreakpoint =
      (CloudLineBreakpointType.CloudLineBreakpoint)cloudIdeBreakpoint;
    if (xIdeBreakpoint.getProperties().isCreatedByServer()) {
      // This newly created IDE breakpoint could be the result of syncing with the server state,
      // in which case, there is nothing to do.  Just return.
      // Note that we need to implement this flag as a transient property because this method
      // gets called during construction.
      return;
    }
    if (xIdeBreakpoint.getProperties().isAddedOnServer() && !xIdeBreakpoint.getProperties().isDisabledByServer()) {
      // If this breakpoint was already added, but was hit offline, let's not add it again once we
      // resume a debug session. unless this reigster request comes from re-enabling a previously disabled
      // breakpoint
      return;
    }

    PsiFile javaFile = myPsiManager.findFile(xIdeBreakpoint.getSourcePosition().getFile());
    if (!(javaFile instanceof PsiJavaFile)) {
      return;
    }
    SourceLocation location = new SourceLocation();
    // Sending the file as com/package/example/Class.java to Cloud Debugger because it plays nice
    // with the CDB plugin. See ServerToIDEFileResolver.
    location.setPath(ServerToIDEFileResolver.getCloudPathFromJavaFile((PsiJavaFile) javaFile));
    location.setLine(xIdeBreakpoint.getSourcePosition().getLine() + 1);

    Breakpoint serverNewBreakpoint = new Breakpoint();
    serverNewBreakpoint.setLocation(location);
    if (xIdeBreakpoint.getConditionExpression() != null) {
      serverNewBreakpoint.setCondition(xIdeBreakpoint.getConditionExpression().getExpression());
    }

    List<String> watches = cloudIdeLineBreakpoint.getWatchExpressions();
    if (watches != null) {
      serverNewBreakpoint.setExpressions(watches);
    }

    // The breakpoint will enter error state asynchronously.  For now, we state that its verified.
    myProcess.getStateController()
        .setBreakpointAsync(serverNewBreakpoint, new SetBreakpointHandler() {
          @Override
          public void onSuccess(@NotNull final String id) {
            Runnable r = new Runnable() {
              @Override
              public void run() {
                if (!Strings.isNullOrEmpty(id)) {
                  if (!cloudIdeLineBreakpoint.isEnabled()) {
                    myProcess.getStateController().deleteBreakpointAsync(id); //race condition
                  } else { // Success.
                    // Mark as added so we don't add it again.
                    xIdeBreakpoint.getProperties().setAddedOnServer(true);
                    cloudIdeLineBreakpoint.setErrorMessage(null);
                    myProcess.updateBreakpointPresentation(cloudIdeLineBreakpoint);
                  }
                } else {
                  // TODO(joaomartins): Why couldn't the breakpoint be set? Improve this message.
                  cloudIdeLineBreakpoint.setErrorMessage(GctBundle.getString("clouddebug.errorset"));
                  myProcess.updateBreakpointPresentation(cloudIdeLineBreakpoint);
                }
                if (!Strings.isNullOrEmpty(id)) {
                  xIdeBreakpoint.getProperties().setDisabledByServer(false);
                  String oldId = xIdeBreakpoint.getUserData(CLOUD_ID);
                  if (!Strings.isNullOrEmpty(oldId)) {
                    myIdeBreakpoints.remove(oldId);
                  }

                  xIdeBreakpoint.putUserData(CLOUD_ID, id);
                  myIdeBreakpoints.put(id, xIdeBreakpoint);
                }
              }
            };
            if (ApplicationManager.getApplication().isUnitTestMode()) {
              r.run();
            }
            else {
              SwingUtilities.invokeLater(r);
            }
          }

          @Override
          public void onError(String errorMessage) {
            cloudIdeLineBreakpoint.setErrorMessage(errorMessage);
            myProcess.updateBreakpointPresentation(cloudIdeLineBreakpoint);
          }
        });
  }

  void setPsiManager(PsiManager psiManager) {
    myPsiManager = psiManager;
  }

  /**
   * Called when the server records a new snapshot, we find the IDE representation and disable it.
   *
   * @param serverBreakpoint
   */
  public void setStateToDisabled(@NotNull Breakpoint serverBreakpoint) {
    final XBreakpoint xIdeBreakpoint = myIdeBreakpoints.get(serverBreakpoint.getId());
    if (xIdeBreakpoint != null
        && xIdeBreakpoint.getProperties() instanceof CloudLineBreakpointProperties) {
      CloudLineBreakpointProperties properties =
          (CloudLineBreakpointProperties)xIdeBreakpoint.getProperties();
      properties.setDisabledByServer(true);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          xIdeBreakpoint.setEnabled(false);
        }
      });
    }
  }

  @Override
  public void unregisterBreakpoint(
      @NotNull XLineBreakpoint<CloudLineBreakpointProperties> xIdeBreakpoint,
      boolean temporary) {
    // If the state was set to disabled as a result of a server update,
    // then we do not need to update the server side.
    if (!xIdeBreakpoint.getProperties().isDisabledByServer()) {
      String breakpointId = xIdeBreakpoint.getUserData(CLOUD_ID);
      if (!Strings.isNullOrEmpty(breakpointId)) {
        myProcess.getStateController().deleteBreakpointAsync(breakpointId);
      } else {
        LOG.warn("could not delete breakpoint because it was not added through the cloud handler.");
      }
    }
    // reset this flag: either it has been disabled by the server or the client has deleted it, in
    // both cases we need to add it again, if it is re-enabled
    xIdeBreakpoint.getProperties().setAddedOnServer(false);
  }
}
