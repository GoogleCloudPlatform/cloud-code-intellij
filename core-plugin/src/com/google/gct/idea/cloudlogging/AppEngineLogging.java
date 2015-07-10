package com.google.gct.idea.cloudlogging;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.appengine.Appengine;
import com.google.api.services.appengine.model.ListModulesResponse;
import com.google.api.services.appengine.model.ListVersionsResponse;
import com.google.api.services.logging.Logging;
import com.google.api.services.logging.model.ListLogEntriesResponse;
import com.google.gct.login.CredentialedUser;
import com.google.gct.login.GoogleLogin;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Tool Window controller/assembler for the App Engine Logs Viewer
 * Created by amulyau on 5/27/15.
 */
public class AppEngineLogging implements ToolWindowFactory,Condition<Project> {

  /**ID of App Engine Logs Tool Window*/
  public static final String APP_ENGINE_LOGS_ID = "App Engine Logs";

  /**Content ID for the Content in the App Engine Logs Tool Window*/
  public static final String APP_ENGINE_CONTENT_ID = "App Engine Content";

  /**Logs Containing Content ToolBar Text */
  public static final String APP_END_TOOLBAR_CONTENT_TEXT = "Logs";

  /**App Engine connection*/
  private Appengine myAppengine;
  /**Logging Connection*/
  private Logging myLogging;

  /**View*/
  private AppEngineLogToolWindowView myAppEngineLogToolWindowView;

  /**
   * Controls whether or not the tool window shows up in the bottom of android studio
   * @param project The Project in which the tool window appears
   * @return True if you want the tool window to show up, else false
   */
  @Override
  public boolean value(Project project) {

    return true;
  }

  /**
   * Creates the tool window, populates it with the swing components and sets initial appearence
   * @param project The project in which the tool window appears
   * @param toolWindow The bottom tray area object in which the content appears in
   */
  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

    //hold panels for tool window
    RunnerLayoutUi layoutUi = RunnerLayoutUi.Factory.getInstance(project).create("AppEngine Logs",
                                                                                 "AppEngine Logs",
                                                                                 "AppEngine Logs",
                                                                                 project);

    ContentManager contentManager= toolWindow.getContentManager();
    //content manager holds the content/tab info of the tool window

    toolWindow.setIcon(AppEngineIcons.TOOL_WINDOW_ICON);
    toolWindow.setTitle(APP_ENGINE_LOGS_ID);   //sets title to app engine logs

    //create content for ui elements
    Content appEngLogContent = createAppEngLogContent(layoutUi);

    //UI for tool window
    myAppEngineLogToolWindowView = appEngLogContent.
      getUserData(AppEngineLogToolWindowView.APP_ENG_LOG_TOOL_WINDOW_VIEW_KEY);

    assert myAppEngineLogToolWindowView !=null;  //makes sure this exists!

    //create tool window bar with search, Project Selection, modules,and version
    appEngLogContent.setSearchComponent(myAppEngineLogToolWindowView.
      createSearchComponent());
    addActionListeners();

    //Place panel in tool window
    layoutUi.addContent(appEngLogContent,0, PlaceInGrid.center, false);

    // add refresh button to a side tool bar in the tool window
    layoutUi.getOptions().setLeftToolbar(getSideToolbarActions(),ActionPlaces.UNKNOWN);

    //holds all the tool info UI components
    final JBLoadingPanel mainPanel = new JBLoadingPanel(new BorderLayout(),project);
    mainPanel.add(layoutUi.getComponent(),BorderLayout.CENTER);

    Content content = contentManager.getFactory().createContent(mainPanel,"",true);

    //need this line to remember the information about view
    content.putUserData(AppEngineLogToolWindowView.APP_ENG_LOG_TOOL_WINDOW_VIEW_KEY,
                        myAppEngineLogToolWindowView);

    contentManager.addContent(content); //adds the main panel as content to the tool window
  }

  /**
   * Gets and sets the search bar component to the tool bar
   * @param layoutUi layout that contains the tool bar components
   * @return Content to add to the tool window
   */
  private static Content createAppEngLogContent(RunnerLayoutUi layoutUi){

    final AppEngineLogToolWindowView appEngineLogToolWindowView = new AppEngineLogToolWindowView();
    //sets all of layout and contains other layout information!

    JPanel appEngLogToolWindowViewPanel = appEngineLogToolWindowView.getMainInfoPanel();
    //gets the panel for the toolwindow content

    Content appEngLogContent = layoutUi.createContent(APP_ENGINE_CONTENT_ID,
                                                      appEngLogToolWindowViewPanel,
                                                      APP_END_TOOLBAR_CONTENT_TEXT ,
                                                      AppEngineIcons.TOOL_WINDOW_CONTENT_ICON, null);

    //need this to get info properly in place!
    appEngLogContent.putUserData(AppEngineLogToolWindowView.APP_ENG_LOG_TOOL_WINDOW_VIEW_KEY,
                                 appEngineLogToolWindowView);

    appEngLogContent.setCloseable(false);  //cannot close the content = good
    appEngLogContent.setPreferredFocusableComponent(appEngLogToolWindowViewPanel); //focus on the
    // panel info

    return appEngLogContent;
  }
  /**
   * Controls the content (that appear in the form of icons/Buttons) in the side tool bar of the
   * tool window.
   * @return An ActionGroup with the buttons that appear in the side tool bar.
   */
  private ActionGroup getSideToolbarActions() {

    DefaultActionGroup group = new DefaultActionGroup();

    group.add(new RefreshButtonAction(this, myAppEngineLogToolWindowView));
    group.add(new textWrapAction(myAppEngineLogToolWindowView));
    group.add(new logsExpandAction(myAppEngineLogToolWindowView));
    group.add(new timeOrderAction(myAppEngineLogToolWindowView));

    return group;
  }

  /**
   * Add Action Listeners to the objects in view
   */
  private void addActionListeners() {

    myAppEngineLogToolWindowView
      .addModuleActionListener(new ModulesActionListener(this, myAppEngineLogToolWindowView));

    myAppEngineLogToolWindowView.addVersionActionListener(new VersionsActionListener(this, myAppEngineLogToolWindowView));

    myAppEngineLogToolWindowView.addProjectInfoButtonListener(new ProjectSelectorListener(this, myAppEngineLogToolWindowView));

    myAppEngineLogToolWindowView.addScrollActionListener(new ScrollActionListener(this,myAppEngineLogToolWindowView));

  }

  /**
   * Make sure of proper user credentials and make connection to app engine
   */
  public void createConnection(){

    CredentialedUser selectedUser = myAppEngineLogToolWindowView.getProjectComboBox().getSelectedUser();

    if(selectedUser==null) {
      selectedUser = GoogleLogin.getInstance().getActiveUser();
    }

    assert selectedUser != null;
    Credential credential = selectedUser.getCredential();

    Appengine.Builder appEngBuilder = new Appengine.Builder(new NetHttpTransport(),
                                                            new JacksonFactory(),credential);
    appEngBuilder = appEngBuilder.setApplicationName("Android Studio");

    myAppengine = appEngBuilder.build();

    myLogging = new Logging.Builder(new NetHttpTransport(), new JacksonFactory(),
                                    credential).setApplicationName("Android Studio").build();

  }

  /**
   * Gets the list of modules with the connection to App Engine
   * @return response with list of modules for the application
   */
  public ListModulesResponse getModulesList(){

    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    ListModulesResponse listModulesResponse = null;

    try {

      Appengine.Apps.Modules.List moduleList = myAppengine.apps().modules().list(
        "apps/" + currentAppID);
      listModulesResponse = moduleList.execute();
    }catch (IOException e1) {

      e1.printStackTrace();
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
    ListVersionsResponse listVersionsResponse = null;

    try {

      Appengine.Apps.Modules.Versions.List versionsList = myAppengine.apps().modules().versions().
        list("apps/" +
             currentAppID +
             "/modules/" +
             currModule);
      listVersionsResponse = versionsList.execute();
    }catch (IOException e1) {

      e1.printStackTrace();
    }

    return listVersionsResponse;
  }

  /**
   * Gets the list of Logs using App Engine connection
   * @return Response with List of Log Entries
   */
  public ListLogEntriesResponse getLogs(){

    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    String currVersion = myAppEngineLogToolWindowView.getCurrVersion();
    ListLogEntriesResponse logResp=null;

    /* working original statement
    logResp = myLogging.projects().logEntries().list("projects/" + currentAppID)
        .setFilter("metadata.serviceName=appengine.googleapis.com metadata.labels." +
                   "\"appengine.googleapis.com/module_id\"=" +
                   currModule +" metadata.labels." +
                   "\"appengine.googleapis.com/version_id\"=" +
                   currVersion).execute();
    */
    //+"&orderBy=metadata.timestamp desc"

    String filters = "metadata.serviceName=appengine.googleapis.com metadata.labels." +
                     "\"appengine.googleapis.com/module_id\"=" +currModule +
                     " metadata.labels." +"\"appengine.googleapis.com/version_id\"="
                     +currVersion;

    //filters+=" metadata.timestamp > \"2015-05-25T12:00:01Z\"";

    String timeOrder;
    boolean ascTimeOrder=myAppEngineLogToolWindowView.getTimeOrder();
      if(ascTimeOrder) {
      timeOrder= "metadata.timestamp asc";//"metadata.timestamp asc";
    }else{
      timeOrder = "metadata.timestamp desc";
    }
    try {
      //System.out.println(myLogging.projects().logEntries().list("projects/" + currentAppID)
      //     .setFilter("metadata.serviceName=appengine.googleapis.com metadata.labels." +
      //                "\"appengine.googleapis.com/module_id\"=" +
      //                currModule +" metadata.labels." +
      //                "\"appengine.googleapis.com/version_id\"=" +
      //                currVersion).setOrderBy("metadata.timestamp desc").setPageSize(1).buildHttpRequestUsingHead().getUrl().toString());

      logResp = myLogging.projects().logEntries().list("projects/" + currentAppID)
        .setFilter(filters).setOrderBy(timeOrder).execute();

    }catch (IOException e1) {

      e1.printStackTrace();
    }


    return logResp;
  }



  public ListLogEntriesResponse askForNextLog(int currPage, ArrayList<String> pageTokens) {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    String currVersion = myAppEngineLogToolWindowView.getCurrVersion();
    ListLogEntriesResponse logResp=null;
    String filters = "metadata.serviceName=appengine.googleapis.com metadata.labels." +
                     "\"appengine.googleapis.com/module_id\"=" +currModule +
                     " metadata.labels." +"\"appengine.googleapis.com/version_id\"="
                     +currVersion;
    String timeOrder;
    boolean ascTimeOrder=myAppEngineLogToolWindowView.getTimeOrder();
    if(ascTimeOrder) {
      timeOrder= "metadata.timestamp asc";
    }else{
      timeOrder = "metadata.timestamp desc";
    }

    if(currPage==-1 && 0==pageTokens.size()){//do this becaues 0th page = getLogs and 1st page is the 0th index in pageTokens (aka size 1)
      //no next page
      return null;
    }else if(currPage+1==pageTokens.size()){ //no next page due to out of bounds
      return null;
    }else {
      try {
        String pageToken = pageTokens.get(currPage+1);
        logResp =  myLogging.projects().logEntries().list("projects/" + currentAppID)
          .setFilter(filters).setPageToken(pageToken).setOrderBy(timeOrder).setPageSize(50).execute();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    return logResp;
  }
  public ListLogEntriesResponse askForPreviousLog(int currPage, ArrayList<String> pageTokens) {
    String currentAppID = myAppEngineLogToolWindowView.getCurrentAppID();
    String currModule = myAppEngineLogToolWindowView.getCurrModule();
    String currVersion = myAppEngineLogToolWindowView.getCurrVersion();
    ListLogEntriesResponse logResp=null;
    String filters = "metadata.serviceName=appengine.googleapis.com metadata.labels." +
                     "\"appengine.googleapis.com/module_id\"=" +currModule +
                     " metadata.labels." +"\"appengine.googleapis.com/version_id\"="
                     +currVersion;

    String timeOrder;
    boolean ascTimeOrder=myAppEngineLogToolWindowView.getTimeOrder();
    if(ascTimeOrder) {
      timeOrder= "metadata.timestamp asc";
    }else{
      timeOrder = "metadata.timestamp desc";
    }
    if(currPage<=0){ //means we need to get hte first page:
      return getLogs();
    }else{
      try {
        String pageToken = pageTokens.get(currPage-1);
        logResp= myLogging.projects().logEntries().list("projects/" + currentAppID)
          .setFilter(filters).setPageToken(pageToken).setOrderBy(timeOrder).setPageSize(50).execute();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    return logResp;

  }
}
