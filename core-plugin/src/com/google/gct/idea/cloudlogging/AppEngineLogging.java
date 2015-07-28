package com.google.gct.idea.cloudlogging;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.appengine.Appengine;
import com.google.api.services.appengine.model.ListModulesResponse;
import com.google.api.services.appengine.model.ListVersionsResponse;
import com.google.api.services.logging.Logging;
import com.google.api.services.logging.model.ListLogEntriesResponse;
import com.google.gct.idea.appengine.gradle.facet.AppEngineGradleFacet;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.*;

import icons.GoogleCloudToolsIcons;

/**
 * Tool Window controller/assembler for the App Engine Logs Viewer
 * Created by amulyau on 5/27/15.
 */
public class AppEngineLogging implements ToolWindowFactory, Condition<Project> {

  /**Logs Exceptions and asserts as errors if bug else if network issue/ out of hands => warning*/
  private static final Logger LOG = Logger.getInstance(AppEngineLogging.class);
  private static final String APP_ENGINE_LOGS_ID = "App Engine Logs";
  private static final String APP_ENGINE_CONTENT_ID = "App Engine Content";
  private static final String APP_END_TOOLBAR_CONTENT_TEXT = "Logs";
  private static final String APPENGINE_LOGS_STRING = "AppEngine Logs";
  /**App Engine Logs use*/
  private static final String INTELLIJ_STRING = "IntelliJ"; //changing this does not seem to matter
  private static final String NO_SELECTED_USER = "Error: No User has been Selected, Please Try " +
      "Again";
  private static final String ERROR_COULD_NOT_CREATE_WINDOW = "Could not create App Engine Logs " +
      "Viewer Window";
  private static final String ERROR_COULD_NOT_GET_MODULES = "Could not get Modules List";
  private static final String ERROR_COULD_NOT_GET_VERSIONS = "Could not get Versions List";
  private static final String ERROR_COULD_NOT_GET_LOGS = "Could not get Logs List";

  private Appengine myAppengine;
  private Logging myLogging;

  private AppEngineLogToolWindowView myAppEngineLogToolWindowView;
  private Project project;

  /**
   * Controls whether or not the tool window shows up in the bottom of android studio
   * If any module is an App Engine project, then we do want to allow the App Engine logs to show
   * @param project The Project in which the tool window appears
   * @return True if the project is an app engine project, else false
   */
  @Override
  public boolean value(Project project) {
    Module[] modArray= ModuleManager.getInstance(project).getModules();
    for(Module mod: modArray){
      if(AppEngineGradleFacet.getInstance(mod)!=null){
        return true;
      }
    }
    return false;
  }

  /**
   * Creates the tool window, populates it with the swing components and sets initial appearance
   * @param project The project in which the tool window appears
   * @param toolWindow The bottom tray area object in which the content appears in
   */
  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    this.project = project;

    //hold panels for tool window
    RunnerLayoutUi layoutUi = RunnerLayoutUi.Factory.getInstance(project)
        .create(APPENGINE_LOGS_STRING, APPENGINE_LOGS_STRING, APPENGINE_LOGS_STRING, project);

    ContentManager contentManager = toolWindow.getContentManager(); //holds tabs for tool window

    toolWindow.setIcon(GoogleCloudToolsIcons.CLOUD_TOOL_WINDOW);
    toolWindow.setTitle(APP_ENGINE_LOGS_ID);   //sets title to app engine logs

    ContentToolWindowView contentToolWindowView = createAppEngLogContent(layoutUi); //create ui
    Content appEngLogContent = contentToolWindowView.getContent();

    myAppEngineLogToolWindowView = contentToolWindowView.getToolWindowView();
    if (myAppEngineLogToolWindowView == null) {
      // when this is true, an unavoidable null pointer exception thrown when closing IDE window
      LOG.error(ERROR_COULD_NOT_CREATE_WINDOW);
      return;
    }
    appEngLogContent.setSearchComponent(myAppEngineLogToolWindowView.createSearchComponent());
    addActionListeners();
    layoutUi.addContent(appEngLogContent, 0, PlaceInGrid.center, false);
    layoutUi.getOptions().setLeftToolbar(getSideToolbarActions(), ActionPlaces.UNKNOWN);

    final JBLoadingPanel mainPanel = new JBLoadingPanel(new BorderLayout(), project);
    mainPanel.add(layoutUi.getComponent(), BorderLayout.CENTER);
    Content content = contentManager.getFactory().createContent(mainPanel, "", true);
    contentManager.addContent(content); //adds the main panel as content to the tool window
  }

  /**
   * Gets and sets the search bar component to the tool bar
   * @param layoutUi layout that contains the tool bar components
   * @return Content and view to add to the tool window
   */
  private ContentToolWindowView createAppEngLogContent(RunnerLayoutUi layoutUi) {
    final AppEngineLogToolWindowView appEngineLogToolWindowView = new AppEngineLogToolWindowView();

    JPanel appEngLogToolWindowViewPanel = appEngineLogToolWindowView.getMainInfoPanel();

    Content appEngLogContent = layoutUi.createContent(APP_ENGINE_CONTENT_ID,
        appEngLogToolWindowViewPanel, APP_END_TOOLBAR_CONTENT_TEXT, GoogleCloudToolsIcons
            .CLOUD_TOOL_WINDOW, null);

    appEngLogContent.setCloseable(false);  //cannot remove content
    appEngLogContent.setPreferredFocusableComponent(appEngLogToolWindowViewPanel);

    return new ContentToolWindowView(appEngineLogToolWindowView, appEngLogContent);
  }

  /**
   * Controls the content (that appear in the form of icons/Buttons) in the side tool bar of the
   * tool window.
   * @return An ActionGroup with the buttons that appear in the side tool bar.
   */
  private ActionGroup getSideToolbarActions() {
    DefaultActionGroup group = new DefaultActionGroup();

    group.add(new RefreshButtonAction(this, myAppEngineLogToolWindowView, project));
    group.add(new TextWrapAction(myAppEngineLogToolWindowView));
    group.add(new logsExpandAction(myAppEngineLogToolWindowView));
    group.add(new TimeOrderAction(this, myAppEngineLogToolWindowView, project));

    return group;
  }

  /**
   * Add Action Listeners to the objects in view
   */
  private void addActionListeners() {
    myAppEngineLogToolWindowView.addModuleActionListener(new ModulesActionListener(this,
        myAppEngineLogToolWindowView, project));

    myAppEngineLogToolWindowView.addVersionActionListener(new VersionsActionListener(this,
        myAppEngineLogToolWindowView, project));

    myAppEngineLogToolWindowView.addProjectSelectedListener(new ProjectSelectorListener(this,
        myAppEngineLogToolWindowView, project));

    myAppEngineLogToolWindowView.addPrevPageButtonListener(new PrevPageButtonListener(this,
        myAppEngineLogToolWindowView, project));

    myAppEngineLogToolWindowView.addNextPageButtonListener(new NextPageButtonListener(this,
        myAppEngineLogToolWindowView, project));

    myAppEngineLogToolWindowView.addLogLevelComboBoxListener(new LogLevelComboListener(this,
        myAppEngineLogToolWindowView, project));
  }

  /**
   * Indicates whether or not AppEngine was created properly
   * @return True if it was not created properly, else false
   */
  public boolean isNotConnected() {
    return myAppengine==null;
  }

  /**
   * Make sure of proper user credentials and make connection to app engine using a new thread
   */
  public void createConnection() {
    CredentialedUser selectedUser = myAppEngineLogToolWindowView.getProjectComboBox().
        getSelectedUser();
    if (selectedUser == null) {
      selectedUser = GoogleLogin.getInstance().getActiveUser();
    }
    if (selectedUser == null) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myAppEngineLogToolWindowView.setRootText(NO_SELECTED_USER);
        }
      });
      return;
    }

    Credential credential = selectedUser.getCredential();
    Appengine.Builder appEngBuilder = new Appengine.Builder(new NetHttpTransport(),
        new JacksonFactory(), credential);
    appEngBuilder = appEngBuilder.setApplicationName(INTELLIJ_STRING);
    myAppengine = appEngBuilder.build();

    myLogging = new Logging.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
        .setApplicationName(INTELLIJ_STRING).build();
  }

  /**
   * Gets the list of modules with the connection to App Engine
   * @return response with list of modules for the application
   */
  public ListModulesResponse getModulesList() {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    ListModulesResponse listModulesResponse;

    try {
      Appengine.Apps.Modules.List moduleList = myAppengine.apps().modules().list("apps/" +
          currentAppID);
      listModulesResponse = moduleList.execute();
    } catch (IOException e1) {
      LOG.warn(ERROR_COULD_NOT_GET_MODULES);
      return null;
    } catch (NullPointerException e2) {
      LOG.warn(ERROR_COULD_NOT_GET_MODULES);
      return null;
    }

    return listModulesResponse;
  }

  /**
   * Gets the list of versions with the connection to App Engine
   * @return response with list of versions for the application
   */
  public ListVersionsResponse getVersionsList() {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    ListVersionsResponse listVersionsResponse;

    try {
      Appengine.Apps.Modules.Versions.List versionsList = myAppengine.apps().modules().versions()
          .list("apps/" + currentAppID + "/modules/" + currModule);
      listVersionsResponse = versionsList.execute();
    }catch (IOException e1) {
      LOG.warn(ERROR_COULD_NOT_GET_VERSIONS);
      return null;
    } catch (NullPointerException e2) {
      LOG.warn(ERROR_COULD_NOT_GET_VERSIONS);
      return null;
    }

    return listVersionsResponse;
  }

  /**
   * Gets the list of Logs using App Engine connection
   * @return Response with List of Log Entries
   */
  public ListLogEntriesResponse getLogs() {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    String currVersion = myAppEngineLogToolWindowView.getCurrVersion();
    ListLogEntriesResponse logResp;

    String filters = "metadata.serviceName=appengine.googleapis.com metadata.labels." +
        "\"appengine.googleapis.com/module_id\"=" + currModule + " metadata.labels." +
        "\"appengine.googleapis.com/version_id\"=" + currVersion;

    String logLevel = myAppEngineLogToolWindowView.getStringCurrLogLevelSelected();
    String filterLogLevel = "";
    if (!logLevel.trim().equals("")) {
      filterLogLevel = " metadata.severity>="+logLevel;
    }
    filters+= filterLogLevel;

    String timeOrder;
    boolean ascTimeOrder = myAppEngineLogToolWindowView.getTimeOrder();

    if (ascTimeOrder) {
      timeOrder = "metadata.timestamp asc";//"metadata.timestamp asc";
    } else {
      timeOrder = "metadata.timestamp desc";
    }

    try {
      logResp = myLogging.projects().logEntries().list("projects/" + currentAppID)
          .setFilter(filters).setOrderBy(timeOrder).execute();
    } catch (IOException e1) {
      LOG.warn(ERROR_COULD_NOT_GET_LOGS);
      return null;
    } catch (NullPointerException e2) {
      LOG.warn(ERROR_COULD_NOT_GET_LOGS);
      return null;
    }

    return logResp;
  }

  /**
   * Asks for next page of logs from app engine
   * @param currPage The current page number
   * @param pageTokens The tokens for each page we get that allows us to go to that page
   * @return Response with List of Log Entries
   */
  public ListLogEntriesResponse askForNextLog(int currPage, ArrayList<String> pageTokens) {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    String currVersion = myAppEngineLogToolWindowView.getCurrVersion();
    ListLogEntriesResponse logResp;
    String filters = "metadata.serviceName=appengine.googleapis.com metadata.labels." +
        "\"appengine.googleapis.com/module_id\"=" + currModule + " metadata.labels." +
        "\"appengine.googleapis.com/version_id\"=" + currVersion;

    String logLevel = myAppEngineLogToolWindowView.getStringCurrLogLevelSelected();
    String filterLogLevel = "";
    if (!logLevel.trim().equals("")) {
      filterLogLevel = " metadata.severity>="+logLevel;
    }

    filters+= filterLogLevel;

    String timeOrder;
    boolean ascTimeOrder = myAppEngineLogToolWindowView.getTimeOrder();
    if (ascTimeOrder) {
      timeOrder = "metadata.timestamp asc";
    } else {
      timeOrder = "metadata.timestamp desc";
    }

    if ((currPage == -1) && (0 == pageTokens.size())) { //no next page
      return null;
    } else if (currPage + 1 == pageTokens.size()) { //no next page due to out of bounds
      return null;
    } else {
      try {
        String pageToken = pageTokens.get(currPage + 1);
        logResp =  myLogging.projects().logEntries().list("projects/" + currentAppID)
            .setFilter(filters).setPageToken(pageToken).setOrderBy(timeOrder).setPageSize(50)
            .execute();
      } catch (IOException e) {
        LOG.warn(ERROR_COULD_NOT_GET_LOGS);
        return null;
      } catch (NullPointerException e1) {
        LOG.warn(ERROR_COULD_NOT_GET_LOGS);
        return null;
      } catch (IndexOutOfBoundsException e3) {
        LOG.warn(ERROR_COULD_NOT_GET_LOGS);
        return null;
      }
    }

    return logResp;
  }

  /**
   * Asks for previous page of logs from app engine
   * @param currPage The current page number
   * @param pageTokens The tokens for each page we get that allows us to go to that page
   * @return Response with List of Log Entries
   */
  public ListLogEntriesResponse askForPreviousLog(int currPage, ArrayList<String> pageTokens) {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    String currVersion = myAppEngineLogToolWindowView.getCurrVersion();
    ListLogEntriesResponse logResp;

    String filters = "metadata.serviceName=appengine.googleapis.com metadata.labels." +
        "\"appengine.googleapis.com/module_id\"=" + currModule + " metadata.labels." +
        "\"appengine.googleapis.com/version_id\"=" + currVersion;

    String logLevel = myAppEngineLogToolWindowView.getStringCurrLogLevelSelected();
    String filterLogLevel = "";
    if (!logLevel.trim().equals("")) {
      filterLogLevel = " metadata.severity>="+logLevel;
    }

    filters+= filterLogLevel;

    String timeOrder;
    boolean ascTimeOrder = myAppEngineLogToolWindowView.getTimeOrder();

    if (ascTimeOrder) {
      timeOrder = "metadata.timestamp asc";
    } else {
      timeOrder = "metadata.timestamp desc";
    }

    if (currPage <= 0) { //means we need to get hte first page:
      return getLogs();
    } else {
      try {
        String pageToken = pageTokens.get(currPage - 1);
        logResp = myLogging.projects().logEntries().list("projects/" + currentAppID).
            setFilter(filters).setPageToken(pageToken).setOrderBy(timeOrder).setPageSize(50).
            execute();
      } catch (IOException e) {
        LOG.warn(ERROR_COULD_NOT_GET_LOGS);
        return null;
      } catch (NullPointerException e1) {
        LOG.warn(ERROR_COULD_NOT_GET_LOGS);
        return null;
      } catch (IndexOutOfBoundsException e3) {
        LOG.warn(ERROR_COULD_NOT_GET_LOGS);
        return null;
      }
    }

    return logResp;
  }

  /**
   * Private inner class for getting Tool Window view and content from createAppEngLogContent
   */
  private class ContentToolWindowView {
    private AppEngineLogToolWindowView toolWindowView;
    private Content content;

    /**
     * Constructor
     * @param toolWindowView View for the tool window
     * @param content Content to hold tool window
     */
    public ContentToolWindowView( AppEngineLogToolWindowView toolWindowView, Content content) {
      this.toolWindowView = toolWindowView;
      this.content = content;
    }

    /**
     * Gets the Tool window view
     * @return AppEngineLogToolWindowView object
     */
    public AppEngineLogToolWindowView getToolWindowView() {
      return this.toolWindowView;
    }

    /**
     * Get the content for tool window
     * @return Content object
     */
    public Content getContent() {
      return this.content;
    }

  }

}
