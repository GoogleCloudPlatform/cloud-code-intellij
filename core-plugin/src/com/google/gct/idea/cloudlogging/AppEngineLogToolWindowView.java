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
package com.google.gct.idea.cloudlogging;

import com.google.api.services.appengine.model.ListModulesResponse;
import com.google.api.services.appengine.model.ListVersionsResponse;
import com.google.api.services.appengine.model.Module;
import com.google.api.services.appengine.model.Version;
import com.google.api.services.logging.model.ListLogEntriesResponse;
import com.google.api.services.logging.model.LogEntry;
import com.google.gct.idea.elysium.ProjectSelector;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Controls the view (created via UI designer plugin) and calls the app engine logs
 * Created by amulyau on 5/27/15.
 */
public class AppEngineLogToolWindowView {

  /**Key that identifies this view*/
  public static final Key<AppEngineLogToolWindowView> APP_ENG_LOG_TOOL_WINDOW_VIEW_KEY = Key.create
          ("APP_ENG_TOOL_WINDOW_VIEW_KEY");

  /**
   * Components created by UI Designer*/
  private JBPanel mainInfoPanel;
  private JBScrollPane scrollPane;
  private JTree logs;

  /**Panel for the Top Bar of the Tool Window*/
  final JBPanel toolWindowBarPanel= new JBPanel();

  ///**Button to get the project info (necessary due to Project Selector's touchy nature)*/
  //private JButton projectInfoButton = new JButton("Get Project Info");

  /**Previous Project name*/
  private String prevProject;
  /**name of current project*/
  private String currProject;
  /**project selector combo box that takes care of google account sign in and gets projects list */
  final ProjectSelector projectComboBox = new ProjectSelector();

  /**Modules Combo box to list modules for the project*/
  private final ComboBox moduleComboBox= new ComboBox();
  /**Model for Modules combo box*/
  private DefaultComboBoxModel defaultComboBoxModelModule;
  /**name of current module*/
  private String currModule;

  /**Versions Combo box to list versions for the project*/
  private final ComboBox versionComboBox= new ComboBox();
  /**Model for Versions combo box*/
  private DefaultComboBoxModel defaultComboBoxModelVersion;
  /**name of current version */
  private String currVersion;

  /**Versions Combo box to list versions for the project*/
  private final ComboBox logLevelComboBox= new ComboBox();
  /**Model for Versions combo box*/
  private DefaultComboBoxModel defaultComboBoxModelLogLevel;

  private JButton prevPageButton = new JButton(AppEngineIcons.PREV_PAGE_ICON);
  private JButton nextPageButton = new JButton(AppEngineIcons.PREV_PAGE_ICON);

  String[] logLevelList = {"Critical","Error","Warning","Info","Debug","Any Log Level"};

  private enum LogLevel {CRITICAL, ERROR, WARNING, INFO, DEBUG, ANYLOGLEVEL}


  /**Need this manual width, else the combo box resizes with text*/
  private static final int MIN_COMBOBOX_WIDTH = 135;

  private static final String DEFAULT_LOGLEVEL_STRING = "--Select Log Level--";
  /**Default text for modules combo box*/
  private static final String DEFAULT_MODULEBOX_STRING = "--Select Module--";
  /**Default Module text*/
  private static final String DEFAULT_MODULE = "default";
  /**Default text for version combo box*/
  private static final String DEFAULT_VERSIONBOX_STRING= "--Select Version--";
  /**Default text for logs text view*/
  private static final String DEFAULT_LOGSTEXT = "Please Select Project to View Logs.";
  /**Module List is empty error message  */
  private static final String ERROR_MODULES_LIST_NULL = "Error: Module list recieved is empty.";
  /** Module List is empty error message box title  */
  private static final String ERROR_MODULES_LIST_TITLE= "Empty Module List";
  /**Version List is empty error message */
  private static final String ERROR_VERSIONS_LIST_NULL = "Error: Version list recieved is empty.";
  /**Version List is empty error message box title */
  private static final String ERROR_VERSIONS_LIST_TITLE= "Empty Version List";
  /**Logs List is empty error message */
  private static final String ERROR_LOGS_LIST_NULL = "Error: Logs list recieved is empty.";
  /**Logs List is empty error message box title */
  private static final String ERROR_LOGS_LIST_TITLE= "Empty Logs List";
  ///**Text to show for the tool tip for logs are not text wrapped*/
  //private static final String WRAP_TOOL_TIP_TEXT = "Wrap Text";
  ///**Text to show for the tool tip for logs are text wrapped*/
  //private static final String UN_WRAP_TOOL_TIP_TEXT = "Un-Wrap Text";
  ///**Text to show for the tool tip for logs are not expanded*/
  //private static final String EXPAND_TOOL_TIP_TEXT = "Expand Logs";
  ///**Text to show for the tool tip for logs are expanded*/
  //private static final String UN_EXPAND_WRAP_TOOL_TIP_TEXT = "Un-Expand Logs";
  /**Text to show for the tool tip for font change*/
  private static final String FONT_TOOL_TIP_TEXT = "Font";


  /**Helps deal with combo box touchiness issues*/
  private boolean moduleALActive = true;
  /**Helps deal with combo box touchiness issues*/
  private boolean versionALActive = true;

  /**Root of tree that is only visible in the initial screen*/
  private TextAreaNode root = new TextAreaNode(DEFAULT_LOGSTEXT,true, AppEngineIcons.ROOT_ICON);
  /**tree model for tree*/
  private DefaultTreeModel treeModel = new DefaultTreeModel(root,true);

  int fontSize = 12;
  /**String representing tree wrapping*/
  private String treeTextWrap = (""+false)+fontSize;
  /**boolean value that indicates if logs are expanded or not*/
  private boolean logsExpanded = false;

  /**Holds the page numbers if we need to go to previous page*/
  private ArrayList<String> pageTokens = new ArrayList<String>();
  private int currPage = -1;

  /**Label on which you hover to show the text change slider*/
  private JBLabel fontSizeChange;

  /**Font change slider */
  private JSlider fontSlider;

  /**Allows the font slider to show up as as pop up when you hover over fontSizeChange Label*/
  private JBPopupMenu popUpSliderMenu;

  /**Minimum font available to select on fontSlider menu*/
  private int MIN_FONT =12;
  /**Maximum font available to select on fontSlider menu*/
  private int MAX_FONT =36;
  /**Initial font selected on fontSlider menu*/
  private int INIT_FONT =12;
  /**Spacing for Large Ticks on slider menu*/
  private int MAJOR_SPACING = 12;
  /**Spacing for Small Ticks on slider menu*/
  private int MINOR_SPACING = 6;

  /**Panel with the Search Box area*/
  private JBPanel searchBox = new JBPanel();

  /**Boolean to represent if the logs are sorted asc time stamp order (true) else desc (false)*/
  private boolean ascTimeOrder = true;

  /**Current app id*/
  private String currentAppID;


  /**
   * gets the panel with the UI tool window bar elements
   * @return JPanel with tool window bar elements
   */
  public JBPanel getMainInfoPanel(){

    return mainInfoPanel;
  }

  /**
   * Constructor to get Tree to display properly.
   */
  public AppEngineLogToolWindowView(){
    createPanelComponents();
    // mainInfoPanel.remove(scrollPane);


    //JBPanel treePanel = new JBPanel(new GridLayout(3,1));
    //  treePanel.add(new JBLabel("Loading Previous Page of Logs ..."));

    //this.scrollPane = new JBScrollPane(logs);
    //mainInfoPanel.add(scrollPane);

    logs.setModel(treeModel);
    logs.setRootVisible(true);
    logs.setShowsRootHandles(true);
    logs.setEditable(false);
    registerUI();

    mainInfoPanel.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        registerUI();
      }
    });

    this.scrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      @Override
      public void adjustmentValueChanged(AdjustmentEvent e) {
        logs.repaint();
      }
    });

    logs.addTreeExpansionListener(new TreeExpansionListener() {
      @Override
      public void treeExpanded(TreeExpansionEvent event) {
        registerUI();
      }

      @Override
      public void treeCollapsed(TreeExpansionEvent event) {
        registerUI();
      }
    });

  }

  private void createPanelComponents() {
    mainInfoPanel = new JBPanel();
    mainInfoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));


    logs = new JTree(); //use JTree because Tree overwrites the customUI we want
    logs.setName(treeTextWrap);
    scrollPane = new JBScrollPane(logs);
    mainInfoPanel.add(scrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
                                                      null, null, 0, false));

  }

  public void changeTextWrapState(boolean value){
    treeTextWrap = ""+value+fontSize;
    logs.setName(treeTextWrap);

  }

  public void changeLogsExpandState(boolean value){
    logsExpanded=value;
  }

  public void changeTimeOrder(boolean value){
    ascTimeOrder=value;
  }

  public void expandLogs(){
    for(int i = 0; i<logs.getRowCount(); i++) {
      logs.expandRow(i);
    }
  }

  public void collapseLogs(){
    for(int j = 0; j<logs.getRowCount(); j++) {
      logs.collapseRow(j);
    }
  }

  private void addActionListeners() {
    fontSizeChange.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        popUpSliderMenu.show(e.getComponent(),e.getX(),e.getY());
      }

      @Override
      public void mousePressed(MouseEvent e){
        super.mousePressed(e);
        popUpSliderMenu.show(e.getComponent(),e.getX(),e.getY());
      }
    });



    fontSlider.addMouseListener(new MouseAdapter() {
      public int oldValue = INIT_FONT;
      @Override
      public void mouseReleased(MouseEvent e) {
        if(!fontSlider.getValueIsAdjusting() && fontSlider.getValue()!=oldValue){
          oldValue = fontSlider.getValue();
          changeLogsTextSize(oldValue);
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        popUpSliderMenu.setVisible(false);
      }
    });



  }

  private void changeLogsTextSize(int value) {
    // System.out.println("value: "+value);
    if(logs.getName().contains("false")){
      logs.setName("false"+value);
    }else{
      logs.setName("true"+value);
    }
    registerUI();

  }

  /**
   * Indicates whether logs are expanded or not
   * @return Boolean value of whether logs are expanded (true) or not (false)
   */
  public boolean getLogsExpanded(){
    return logsExpanded;
  }

  /**
   * Gets the state of text wrapping in the logs tree
   * @return true if the text is wrapped, else false
   */
  public boolean getTextWrap(){
    if(logs.getName().contains("false")){
      return false;
    }else{
      return true;
    }
  }


  /**
   * Resets the UI for tree to redraw properly
   */
  public void registerUI() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        logs.setUI(new BasicWideNodeTreeUI());
      }
    });
  }


  /**
   * Create the combo boxes and search bar elements that filter and search the logs
   * @return JComponent panel that holds the applicable filters on the logs
   */
  @NotNull
  public JComponent createSearchComponent() {

    defaultComboBoxModelModule = new DefaultComboBoxModel(new String[]{DEFAULT_MODULEBOX_STRING});
    defaultComboBoxModelVersion = new DefaultComboBoxModel(new String[]{DEFAULT_VERSIONBOX_STRING});

    moduleComboBox.setModel(defaultComboBoxModelModule);
    moduleComboBox.setEnabled(false);
    moduleComboBox.setEditable(false);
    moduleComboBox.setMinimumAndPreferredWidth(MIN_COMBOBOX_WIDTH); //necessary = resizes with text

    versionComboBox.setModel(defaultComboBoxModelVersion);
    versionComboBox.setEnabled(false);
    versionComboBox.setEditable(false);
    versionComboBox.setMinimumAndPreferredWidth(MIN_COMBOBOX_WIDTH);


    //textWrapButton = new JToggleButton("NoWrap",AppEngineIcons.NO_WRAP_TOGGLE_ICON,false);
    //textWrapButton.setSelectedIcon(AppEngineIcons.WRAP_TOGGLE_ICON);
    //textWrapButton.setDisabledIcon(AppEngineIcons.NO_WRAP_TOGGLE_ICON);
    //textWrapButton.setToolTipText(WRAP_TOOL_TIP_TEXT);

    //expandAllLogs= new JToggleButton("not Expanded",AppEngineIcons.NO_WRAP_TOGGLE_ICON,false);
    //expandAllLogs.setSelectedIcon(AppEngineIcons.WRAP_TOGGLE_ICON);
    //expandAllLogs.setDisabledIcon(AppEngineIcons.NO_WRAP_TOGGLE_ICON);
    //expandAllLogs.setToolTipText(EXPAND_TOOL_TIP_TEXT);


    defaultComboBoxModelLogLevel = new DefaultComboBoxModel();
    logLevelComboBox.setModel(defaultComboBoxModelLogLevel);
    logLevelComboBox.setRenderer(new LogLevelComboBoxRenderer());
    logLevelComboBox.setEditor(new LogLevelComboBoxEditor());


    for(int h=0; h<logLevelList.length; h++){
      Icon icon;
      if(logLevelList[h].equals("Critical")) {
        icon = AppEngineIcons.CRITICAL_LOG_ICON;
      }else if(logLevelList[h].equals("Error")) {
        icon = AppEngineIcons.ERROR_LOG_ICON;
      }else if(logLevelList[h].equals("Warning")) {
        icon = AppEngineIcons.WARNING_LOG_ICON;
      }else if(logLevelList[h].equals("Info")) {
        icon = AppEngineIcons.INFO_LOG_ICON;
      }else if(logLevelList[h].equals("Debug")) {
        icon = AppEngineIcons.DEBUG_LOG_ICON;
      } else if(logLevelList[h].equals("Any Log Level")) {
        icon = AppEngineIcons.ANY_LOG_ICON;
      }else {
        icon = AppEngineIcons.ANY_LOG_ICON;
      }
      JBLabel newLabel = new JBLabel(logLevelList[h],icon, JBLabel.RIGHT);
      defaultComboBoxModelLogLevel.addElement(newLabel);
      //logLevelComboBox.addItem(newLabel);
    }


    fontSizeChange = new JBLabel(AppEngineIcons.FONT_CHANGE_ICON);

    fontSizeChange.setToolTipText(FONT_TOOL_TIP_TEXT);

    fontSlider = new JSlider(JSlider.VERTICAL, MIN_FONT, MAX_FONT, INIT_FONT); //orientation, min, max,initial value
    fontSlider.setPaintTicks(true);
    fontSlider.setSnapToTicks(true);
    fontSlider.setMajorTickSpacing(MAJOR_SPACING);
    fontSlider.setMinorTickSpacing(MINOR_SPACING);
    fontSlider.setPaintLabels(true);

    popUpSliderMenu = new JBPopupMenu();
    popUpSliderMenu.add(fontSlider);

    addActionListeners();

    projectComboBox.setEnabled(true);

    //toolWindowBarPanel.add(fontSizeChange);
    //toolWindowBarPanel.add(logLevelComboBox);
    //toolWindowBarPanel.add(expandAllLogs);
    // toolWindowBarPanel.add();
    toolWindowBarPanel.add(projectComboBox);
    toolWindowBarPanel.add(moduleComboBox);
    toolWindowBarPanel.add(versionComboBox);

    final JBPanel searchComponentPanel = new JBPanel();
    searchComponentPanel.setLayout(new BoxLayout(searchComponentPanel, BoxLayout.X_AXIS));
    searchComponentPanel.add(fontSizeChange);
    searchComponentPanel.add(logLevelComboBox);
    searchComponentPanel.add(getSearchComponent()); //has the search bar!!
    searchComponentPanel.add(toolWindowBarPanel); //has only the combo boxes for module and version

    return searchComponentPanel;
  }



  /**
   * Resets the text area and search components to their original state
   */
  public void resetComponents(){

    defaultComboBoxModelModule.removeAllElements();
    defaultComboBoxModelModule.addElement(DEFAULT_MODULEBOX_STRING);
    moduleComboBox.setEnabled(false);

    defaultComboBoxModelVersion.removeAllElements();
    defaultComboBoxModelVersion.addElement(DEFAULT_VERSIONBOX_STRING);
    versionComboBox.setEnabled(false);

    root.setUserObject(DEFAULT_LOGSTEXT);
    root.setAllowsChildren(false);
    logs.setRootVisible(true);
    treeModel.reload();
  }

  /**
   * Sets the root visible/invisible
   * @param treeRootVisible boolean value to set root visible (true) or invisible (false)
   */
  public void setTreeRootVisible(boolean treeRootVisible) {

    logs.setRootVisible(treeRootVisible);
  }

  /**
   * Search component is on seperate panel from filters so gets the search componenet
   * @return JComponenet with the search component
   */
  private JComponent getSearchComponent() {

    searchBox.add(getTextFilterSearch());
    return searchBox;
  }

  /**
   * Creates the search component that filters the text
   * @return Component that does text searching
   */
  private static Component getTextFilterSearch() {

    JBLabel searchBox = new JBLabel("Search Box goes here");
    return searchBox;
  }

  /**
   * Gets the Project Combo Box (Project Selector)
   * @return Project Selector
   */
  public ProjectSelector getProjectComboBox(){

    return projectComboBox;
  }

  /**
   * get the current project
   * @return current project string
   */
  public String getCurrProject(){

    return currProject;
  }

  /**
   * Set the Current Project with with the project description from the Project Selector
   */
  public void setCurrProject(){

    currProject=projectComboBox.getProjectDescription();
    //System.out.println("curre Project Desc: "+currProject); //gets TestAppEng2
    //System.out.println("Curr project text: "+projectComboBox.getText()); //gets testappeng2

  }

  public boolean getTimeOrder(){
    return ascTimeOrder;
  }

  /**
   * Get the previous project
   * @return previous project string
   */
  public String getPrevProject(){

    return prevProject;
  }

  /**
   * Set the previous project based on the Project Selector
   */
  public void setPrevProject(){

    prevProject=projectComboBox.getProjectDescription();
  }

  /**
   * Get the boolean that helps deal with Modules combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the module
   * @return boolean value of the module AL Active variable
   */
  public boolean getModuleALActive(){

    return this.moduleALActive;
  }

  /**
   * Get the boolean that helps deal with Version combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the Version
   * @return boolean value of the Version AL Active variable
   */
  public boolean getVersionALActive(){

    return this.versionALActive;
  }

  /**
   * Get the boolean that helps deal with Modules combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the module
   * @param bool boolean value to set the variable to
   */
  public void setModuleALActive(boolean bool){

    this.moduleALActive = bool;
  }

  /**
   * Get the boolean that helps deal with Version combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the Version
   * @param bool boolean value to set the variable to
   */
  public void setVersionALActive(boolean bool){

    this.versionALActive = bool;
  }

  /**
   * Get the Current App ID
   * @return Current APP ID string
   */
  public String getCurrentAppID(){

    return this.currentAppID;
  }

  /**
   * Set the Current App ID using hte Project Selector
   */
  public void setCurrentAppID(){

    currentAppID = projectComboBox.getText();
  }

  /**
   * Get the Current version
   * @return Current Version String
   */
  public String getCurrVersion(){

    return this.currVersion;
  }

  /**
   * Set the Current Version from the version combo box
   */
  public void setCurrVersion(){

    currVersion= (String)versionComboBox.getSelectedItem();
  }

  /**
   * get the Current Module
   * @return Current Module String
   */
  public String getCurrModule(){

    return this.currModule;
  }

  /**
   * Set the Current Module based on the module combo box
   */
  public void setCurrModule(){

    currModule = (String)moduleComboBox.getSelectedItem();
  }

  /**
   * Returns vertical scroll bar of tree
   * @return JScrollBar which is vertical scroll bar of tree
   */
  public JScrollBar getVerticalScrollBar(){

    return this.scrollPane.getVerticalScrollBar();
  }

  /**
   * clear the Combo boxes by removing all elements
   */
  public void clearComboBoxes(){

    defaultComboBoxModelModule.removeAllElements();
    defaultComboBoxModelVersion.removeAllElements();
  }

  /**
   * Set Enable Module Combo Box
   * @param bool boolean value to set enabled
   */
  public void setEnabledModuleComboBox(boolean bool){

    moduleComboBox.setEnabled(bool);
  }

  /**
   * Set Enable Version Combo Box
   * @param bool boolean value to set enabled
   */
  public void setEnabledVersionComboBox(boolean bool){

    versionComboBox.setEnabled(bool);
  }

  /**
   * Add Action Listener to Module Combo Box
   * @param modulesActionListener Modules Action Listener object
   */
  public void addModuleActionListener(ModulesActionListener modulesActionListener){

    moduleComboBox.addActionListener(modulesActionListener);
  }

  /**
   * Add Action Listener to Version Combo Box
   * @param versionsActionListener Version Action Listener object
   */
  public void addVersionActionListener(VersionsActionListener versionsActionListener){

    versionComboBox.addActionListener(versionsActionListener);
  }

  /**
   * Add Action Listener to Project Selector combo box
   * @param projectSelectorListener Project Selector Listener
   */
  public void addProjectInfoButtonListener(ProjectSelectorListener projectSelectorListener){

    projectComboBox.getDocument().addDocumentListener(projectSelectorListener);

  }

  public void addScrollActionListener(ScrollActionListener scrollActionListener){

    this.scrollPane.getVerticalScrollBar().addAdjustmentListener(scrollActionListener);
  }

  /**
   * Set the Modules List
   * @param listModulesResponse Response from the server with the modules list
   */
  public void setModulesList(ListModulesResponse listModulesResponse){

    if(listModulesResponse==null|| listModulesResponse.getModules()==null){
      Messages.showErrorDialog(ERROR_MODULES_LIST_NULL, ERROR_MODULES_LIST_TITLE);
      return;
    }

    setModuleALActive(false);
    defaultComboBoxModelModule.removeAllElements();

    int defaultModuleIndex=0;
    Module defaultModule = null;
    Module singleModule;

    for(int modListIter=0; modListIter<listModulesResponse.getModules().size(); modListIter++){
      singleModule = listModulesResponse.getModules().get(modListIter);
      defaultComboBoxModelModule.addElement(singleModule.getId());
      if(singleModule.getId().equals(DEFAULT_MODULE)){
        defaultModuleIndex=modListIter;
        defaultModule=singleModule;
      }
    }

    if(defaultModule!=null) {

      defaultComboBoxModelModule.setSelectedItem(defaultComboBoxModelModule.
        getElementAt(defaultModuleIndex));
    }else{

      defaultComboBoxModelModule.setSelectedItem(defaultComboBoxModelModule.getElementAt(0));
      defaultModule = listModulesResponse.getModules().get(0);
    }

    currModule=defaultModule.getId();

    setModuleALActive(true);
  }

  /**
   * Set the Versions List
   * @param listVersionsResponse The response from the server with the versions list
   */
  public void setVersionsList(ListVersionsResponse listVersionsResponse){

    if(listVersionsResponse==null || listVersionsResponse.getVersions()==null){
      Messages.showErrorDialog(ERROR_VERSIONS_LIST_NULL, ERROR_VERSIONS_LIST_TITLE);
      return;
    }

    setVersionALActive(false);

    defaultComboBoxModelVersion.removeAllElements();
    Version singleVersion;

    for(int versListIter=0; versListIter<listVersionsResponse.getVersions().size();versListIter++){

      singleVersion = listVersionsResponse.getVersions().get(versListIter);
      defaultComboBoxModelVersion.addElement(singleVersion.getId());
    }

    currVersion=listVersionsResponse.getVersions().get(0).getId();

    //set version to the first one in combobox
    defaultComboBoxModelVersion.setSelectedItem(defaultComboBoxModelVersion.getElementAt(0));

    setVersionALActive(true);
  }

  /**
   * Set the Logs List
   * @param logResp The response from the server with the logs list
   */
  public void setLogs(ListLogEntriesResponse logResp){

    if(logResp==null || logResp.getEntries()==null){ //error in case of wrong module type
      Messages.showErrorDialog(ERROR_LOGS_LIST_NULL, ERROR_LOGS_LIST_TITLE);
      return;
    }
    root.setAllowsChildren(true);
    root.removeAllChildren();

    treeModel.reload();

    String nextPageToken = logResp.getNextPageToken();
    if(nextPageToken!=null && ((currPage==-1 && this.pageTokens.size()==0) || currPage+1==this.pageTokens.size())){//page numbers go 0+ and size goes 1 +
      this.pageTokens.add(nextPageToken);
    }

    LogTreeEntry logTreeEntry;
    TextAreaNode parent;
    TextAreaNode child;

    if(currPage!=-1) {
      parent = new TextAreaNode("...Load Previous Page...", false, null);
      treeModel.insertNodeInto(parent, root, root.getChildCount());
    }

    //get logs and add them to the tree
    for(int logEntriesIter = 0; logEntriesIter< logResp.getEntries().size(); logEntriesIter++) {
      LogEntry log = logResp.getEntries().get(logEntriesIter);
      logTreeEntry = new LogTreeEntry(log);

      if (!logTreeEntry.getLogMessage().trim().isEmpty()) { //has log message

        parent = new TextAreaNode(logTreeEntry.getLogInfo(),true, logTreeEntry.getSeverityIcon());
        child = new TextAreaNode(logTreeEntry.getLogMessage(),false, logTreeEntry.getSeverityIcon());
        parent.add(child);
      }else {

        parent = new TextAreaNode(logTreeEntry.getLogInfo(),false, logTreeEntry.getSeverityIcon());
      }

      treeModel.insertNodeInto(parent,root,root.getChildCount()); //insert logs in order in bottom
    }

    if(nextPageToken!=null){
      parent = new TextAreaNode("...Load Next Page...", false, null);
      treeModel.insertNodeInto(parent, root, root.getChildCount());
    }

    treeModel.reload();

    if(logsExpanded){ //default is to not expand so need to specify this if the expanded toggle button is selected
      expandLogs();
    }

  }

  /**
   * When refresh button is clicked, makes sure that the current module is set back the
   * module combo box. If the module has been deleted, it will use the default module
   * @param prevModule String previous Module that was the module selected before we reset
   *                   the modules, versions and logs.
   */
  public void setCurrModuleToModuleComboBox(String prevModule) {

    if(defaultComboBoxModelModule.getIndexOf(prevModule)>0){

      defaultComboBoxModelModule.setSelectedItem(prevModule);
    }else{

      defaultComboBoxModelModule.setSelectedItem(DEFAULT_MODULE);
    }

  }

  /**
   *  When refresh button is clicked, makes sure that the current version is set back the
   * version combo box. If the version has been deleted, it will use the last alphabetical version
   * @param prevVersion  String previous version that was the version selected before we reset
   *                   the modules, versions and logs.
   */
  public void setCurrVersionToVersionComboBox(String prevVersion){

    if(defaultComboBoxModelVersion.getIndexOf(prevVersion)>0){

      defaultComboBoxModelVersion.setSelectedItem(prevVersion);
    }
  }

  public ArrayList<String> getPageTokens() {
    return this.pageTokens;
  }

  public int getCurrPage() {
    return this.currPage;
  }

  public void decreasePage(){
    this.currPage--;
  }

  public void increasePage(){
    this.currPage++;
  }
}
