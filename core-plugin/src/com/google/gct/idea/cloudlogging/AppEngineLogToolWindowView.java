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

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.UIUtil;

import org.jdesktop.swingx.renderer.WrappingIconPanel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

import icons.GoogleCloudToolsIcons;

/**
 * The view and all the viewing components
 * Created by amulyau on 5/27/15.
 */
public class AppEngineLogToolWindowView {

  /**Holds the Components that go in the tool window*/
  private JBPanel mainInfoPanel;
  /**scroll pane that holds the tree*/
  private JBScrollPane scrollPane;
  /**Tree that holds the logs*/
  private JTree logs;

  /**Panel for the Top Bar of the Tool Window*/
  private final JBPanel toolWindowBarPanel = new JBPanel();

  /**Previous Project name*/
  private String prevProject;
  /**name of current project*/
  private String currProject;
  /**project selector combo box that takes care of google account sign in and gets projects list */
  private final ProjectSelector projectComboBox = new ProjectSelector();

  /**Modules Combo box to list modules for the project*/
  private final ComboBox moduleComboBox = new ComboBox();
  /**Model for Modules combo box*/
  private DefaultComboBoxModel defaultComboBoxModelModule;
  /**name of current module*/
  private String currModule;

  /**Versions Combo box to list versions for the project*/
  private final ComboBox versionComboBox = new ComboBox();
  /**Model for Versions combo box*/
  private DefaultComboBoxModel defaultComboBoxModelVersion;
  /**name of current version */
  private String currVersion;

  /**List of log levels*/
  private final String[] logLevelList = {"Critical", "Error", "Warning", "Info", "Debug",
      "Any Log Level"};

  /**Previous page button for paging*/
  private JButton prevPageButton;
  /**Next page Button for paging*/
  private JButton nextPageButton;
  /**Need this manual width, else the combo box resizes with text*/
  private static final int MIN_COMBOBOX_WIDTH = 135;
  /**Default text for modules combo box*/
  private static final String DEFAULT_MODULEBOX_STRING = "--Select Module--";
  /**Default Module text*/
  private static final String DEFAULT_MODULE = "default";
  /**Default text for version combo box*/
  private static final String DEFAULT_VERSIONBOX_STRING = "--Select Version--";
  /**Default text for logs text view*/
  private static final String DEFAULT_LOGSTEXT = "Please Select Project to View Logs.";

  /**Text to show for the tool tip for font change*/
  private static final String FONT_TOOL_TIP_TEXT = "Font";

  /**Error Message to show in tree when modules list is empty*/
  public final String NO_MODULES_LIST_STRING = "Error: Empty Modules List, Please Try Again";
  /**Error Message to show in tree when modules list is empty*/
  public final String NO_VERSIONS_LIST_STRING = "Error: Empty Versions List, Please Try Again";
  /**Error Message to show in tree when modules list is empty*/
  public final String NO_LOGS_LIST_STRING = "Error: Empty Logs List, Please Try Again";
  /**Error to display for when User Enters project into */
  public final String ERROR_PROJECT_NOT_EXIST = "Error: Project entered does not exist, " +
      "please try again";
  /**Error message to show when project did not connect*/
  public final String ERROR_PROJECT_DID_NOT_CONNECT = "Error: Project did not connect, please " +
      "try agian";

  /**Helps deal with combo box touchiness issues*/
  private boolean moduleALActive = true;
  /**Helps deal with combo box touchiness issues*/
  private boolean versionALActive = true;

  /**Root of tree that is only visible in the initial screen*/
  private TextAreaNode root = new TextAreaNode(DEFAULT_LOGSTEXT, true,
      null);
  /**tree model for tree*/
  private final DefaultTreeModel treeModel = new DefaultTreeModel(root,true);

  private int fontSize = 12;
  /**String representing tree wrapping*/
  private boolean treeTextWrap = false;
  /**boolean value that indicates if logs are expanded or not*/
  private boolean logsExpanded = false;
  /**Current maximum log level's index number*/
  int logLevel = 5;

  /**Holds the page numbers if we need to go to previous page*/
  private final ArrayList<String> pageTokens = new ArrayList<String>();

  /**Current Page number*/
  private int currPage = -1;

  /**Label on which you hover to show the text change slider*/
  private JBLabel fontSizeChange;

  /**Font change slider */
  private JSlider fontSlider;

  /**Allows the font slider to show up as as pop up when you hover over fontSizeChange Label*/
  private JBPopupMenu popUpSliderMenu;

  /**Minimum font available to select on fontSlider menu*/
  private final int MIN_FONT = 12;
  /**Maximum font available to select on fontSlider menu*/
  private final int MAX_FONT = 36;
  /**Initial font selected on fontSlider menu*/
  private final int INIT_FONT = 12;
  /**Spacing for Large Ticks on slider menu*/
  private final int MAJOR_SPACING = 12;
  /**Spacing for Small Ticks on slider menu*/
  private final int MINOR_SPACING = 6;

  /**Panel with the Search Box area*/
  private final JBPanel searchBox = new JBPanel();

  /**Boolean to represent if the logs are sorted asc time stamp order (true) else desc (false)*/
  private boolean ascTimeOrder = false;

  /**Stores the index of the default module in the combo box*/
  private int defaultModIndex = -1;
  /**Stores if there is a next page token so that we can process and then set the GUI for logs*/
  private boolean nextPageTokenExist;

  /**Current app id*/
  private String currentAppID;

  /**
   * Constructor to add all elements properly and get Tree to show.
   */
  public AppEngineLogToolWindowView(){

    createPanelComponents();

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

    LafManager.getInstance().addLafManagerListener(new LafManagerListener() {
      @Override
      public void lookAndFeelChanged(LafManager lafManager) {
        registerUI();
      }
    });

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//    this.scrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
//      @Override
//      public void adjustmentValueChanged(AdjustmentEvent e) {
//        logs.repaint();
//      }
//    });
//
//    logs.addTreeExpansionListener(new TreeExpansionListener() {
//      @Override
//      public void treeExpanded(TreeExpansionEvent event) {
//        registerUI();
//      }
//
//      @Override
//      public void treeCollapsed(TreeExpansionEvent event) {
//        registerUI();
//      }
//    });
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  }

  /**
   * gets the panel with the UI tool window bar elements
   * @return JPanel with tool window bar elements
   */
  public JBPanel getMainInfoPanel() {

    return mainInfoPanel;
  }

  /**
   * Creates main panel contents for the tool window
   */
  private void createPanelComponents() {

    mainInfoPanel = new JBPanel();
    mainInfoPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));

    logs = new JTree(); //use JTree because Tree overwrites the customUI we want
    logs.setBackground(UIUtil.getBgFillColor(logs));
    scrollPane = new JBScrollPane(logs);
    mainInfoPanel.add(scrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
        null, null, 0, false));
  }

  /**
   * Changes the text wrap state to the given boolean value
   * @param value If true, then the text is wrapped
   */
  public void changeTextWrapState(boolean value) {

    treeTextWrap = value;
  }

  /**
   * Change the state of the all logs expanded state
   * @param value If True then all logs are expanded.
   */
  public void changeLogsExpandState(boolean value) {

    logsExpanded = value;
  }

  /**
   * Changes the time order to the value provided
   * @param value Boolean value to change the ascending time order to.
   *              If true then ascending.
   */
  public void changeTimeOrder(boolean value) {

    ascTimeOrder = value;
  }

  /**
   * Expand all logs
   */
  public void expandLogs() {

    for (int i = 0; i < logs.getRowCount(); i++) {
      logs.expandRow(i);
    }
  }

  /**
   * Collapse all logs
   */
  public void collapseLogs() {

    for (int j = 0; j < logs.getRowCount(); j++) {
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
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        popUpSliderMenu.show(e.getComponent(),e.getX(),e.getY());
      }
    });

    fontSlider.addMouseListener(new MouseAdapter() {
      public int oldValue = INIT_FONT;

      @Override
      public void mouseReleased(MouseEvent e) {
        if ((!fontSlider.getValueIsAdjusting()) && (fontSlider.getValue() != oldValue)) {
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

  /**
   * Change the logs text size with the given value
   * @param value Integer value of the font size to set.
   */
  private void changeLogsTextSize(int value) {

    fontSize = value;
    registerUI();
  }

  /**
   * Log Level currently selected's index value is returned
   * @return integer value that represents the index in the combo box of the maximum log level
   */
  public int getCurrLogLevelSelected() {

    return logLevel;
  }

  /**
   * Set the current Log Level Selected
   * @param logLevel Integer value of the maximum log level index
   */
  public void setCurrLogLevelSelected(int logLevel) {

    if((logLevel < 0) || (logLevel > 5)) {
      this.logLevel = 5;
    } else {
      this.logLevel = logLevel;
    }
  }

  /**
   * Returns what the combobox width should be
   * @return Int value of combobox width
   */
  public int getMinComboboxWidth() {

    return MIN_COMBOBOX_WIDTH;
  }

  /**
   * Indicates whether logs are expanded or not
   * @return Boolean value of whether logs are expanded (true) or not (false)
   */
  public boolean getLogsExpanded() {

    return logsExpanded;
  }

  /**
   * Gets the state of text wrapping in the logs tree
   * @return true if the text is wrapped, else false
   */
  public boolean getTextWrap() {

    return treeTextWrap;
  }

  /**
   * Gets the current font size of the logs
   * @return Font size
   */
  public int getFontSize() {

    return fontSize;
  }

  /**
   * Resets the UI for tree to redraw properly
   */
  public void registerUI() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        logs.setUI(new BasicWideNodeTreeUI(AppEngineLogToolWindowView.this));
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

    moduleComboBox.setBackground(UIUtil.getTextFieldBackground());
    moduleComboBox.setModel(defaultComboBoxModelModule);
    moduleComboBox.setEnabled(false);
    moduleComboBox.setEditable(false);
    moduleComboBox.setMinimumAndPreferredWidth(MIN_COMBOBOX_WIDTH); //necessary = resizes with text

    versionComboBox.setBackground(UIUtil.getTextFieldBackground());
    versionComboBox.setModel(defaultComboBoxModelVersion);
    versionComboBox.setEnabled(false);
    versionComboBox.setEditable(false);
    versionComboBox.setMinimumAndPreferredWidth(MIN_COMBOBOX_WIDTH);

    WrappingIconPanel wp = new WrappingIconPanel();/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    fontSizeChange = new JBLabel(AllIcons.Actions.Menu_replace);

    fontSizeChange.setToolTipText(FONT_TOOL_TIP_TEXT);

    fontSlider = new JSlider(JSlider.VERTICAL, MIN_FONT, MAX_FONT, INIT_FONT); //orientation, min, max,initial value
    fontSlider.setPaintTicks(true);
    fontSlider.setSnapToTicks(true);
    fontSlider.setMajorTickSpacing(MAJOR_SPACING);
    fontSlider.setMinorTickSpacing(MINOR_SPACING);
    fontSlider.setPaintLabels(true);

    popUpSliderMenu = new JBPopupMenu();
    popUpSliderMenu.add(fontSlider);

    prevPageButton = new JButton(AllIcons.Actions.Back);
    nextPageButton = new JButton(AllIcons.Actions.Forward);
    prevPageButton.setToolTipText("Previous Page");
    nextPageButton.setToolTipText("Next Page");
    prevPageButton.setDisabledIcon(AllIcons.Duplicates.SendToTheLeftGrayed);
    nextPageButton.setDisabledIcon(AllIcons.Duplicates.SendToTheRightGrayed);

    nextPageButton.setEnabled(false);
    prevPageButton.setEnabled(false);

    addActionListeners();

    projectComboBox.setEnabled(true);

    toolWindowBarPanel.add(projectComboBox);
    toolWindowBarPanel.add(moduleComboBox);
    toolWindowBarPanel.add(versionComboBox);

    final JBPanel searchComponentPanel = new JBPanel();
    searchComponentPanel.setLayout(new BoxLayout(searchComponentPanel, BoxLayout.X_AXIS));
    searchComponentPanel.add(prevPageButton);
    searchComponentPanel.add(nextPageButton);
    searchComponentPanel.add(fontSizeChange);
    searchComponentPanel.add(new LogLevelCustomCombo(this));
    searchComponentPanel.add(getSearchComponent()); //has the search bar!!
    searchComponentPanel.add(toolWindowBarPanel); //has only the combo boxes for module and version

    return searchComponentPanel;
  }

  /**
   * Resets the text area and search components to their original state
   */
  public void resetComponents() {

    defaultComboBoxModelModule.removeAllElements();
    defaultComboBoxModelModule.addElement(DEFAULT_MODULEBOX_STRING);
    moduleComboBox.setEnabled(false);

    defaultComboBoxModelVersion.removeAllElements();
    defaultComboBoxModelVersion.addElement(DEFAULT_VERSIONBOX_STRING);
    versionComboBox.setEnabled(false);

    fontSize=INIT_FONT;
    root = new TextAreaNode(DEFAULT_LOGSTEXT,true, null);
    treeModel.setRoot(root);
    root.setAllowsChildren(false);
    logs.setRootVisible(true);
    nextPageButton.setEnabled(false);
    prevPageButton.setEnabled(false);
    treeModel.reload();
  }

  /**
   * Sets the root visible
   */
  public void setTreeRootVisible() {

    logs.setRootVisible(true);
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

    return new JBLabel("Search Box goes here");
  }

  /**
   * Gets the Project Combo Box (Project Selector)
   * @return Project Selector
   */
  public ProjectSelector getProjectComboBox() {

    return projectComboBox;
  }

  /**
   * get the current project
   * @return current project string
   */
  public String getCurrProject() {

    return currProject;
  }

  /**
   * Set the Current Project with with the project description from the Project Selector
   */
  public void setCurrProject() {

    currProject = projectComboBox.getProjectDescription();
  }

  /**
   * Gets the boolean value of whether time order is ascending or descending for the logs
   * @return true if the logs are ascending, else false
   */
  public boolean getTimeOrder() {

    return ascTimeOrder;
  }

  /**
   * Get the previous project
   * @return previous project string
   */
  public String getPrevProject() {

    return prevProject;
  }

  /**
   * Set the previous project based on the Project Selector
   */
  public void setPrevProject() {

    prevProject = projectComboBox.getProjectDescription();
  }

  /**
   * Get the boolean that helps deal with Modules combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the module
   * @return boolean value of the module AL Active variable
   */
  public boolean getModuleALActive() {

    return this.moduleALActive;
  }

  /**
   * Get the boolean that helps deal with Version combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the Version
   * @return boolean value of the Version AL Active variable
   */
  public boolean getVersionALActive() {

    return this.versionALActive;
  }

  /**
   * Get the boolean that helps deal with Modules combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the module
   * @param bool boolean value to set the variable to
   */
  public void setModuleALActive(boolean bool) {

    this.moduleALActive = bool;
  }

  /**
   * Get the boolean that helps deal with Version combo box change issues
   * Issue = Combo Box is touchy when it comes to selecting the Version
   * @param bool boolean value to set the variable to
   */
  public void setVersionALActive(boolean bool) {

    this.versionALActive = bool;
  }

  /**
   * Get the Current App ID
   * @return Current APP ID string
   */
  public String getCurrentAppID() {

    return this.currentAppID;
  }

  /**
   * Set the Current App ID using hte Project Selector
   */
  public void setCurrentAppID() {

    currentAppID = projectComboBox.getText();
  }

  /**
   * Get the Current version
   * @return Current Version String
   */
  public String getCurrVersion() {

    return this.currVersion;
  }

  /**
   * Set the Current Version from the version combo box
   */
  public void setCurrVersion() {

    currVersion = (String)versionComboBox.getSelectedItem();
  }

  /**
   * get the Current Module
   * @return Current Module String
   */
  public String getCurrModule() {

    return this.currModule;
  }

  /**
   * Set the Current Module based on the module combo box
   */
  public void setCurrModule() {

    currModule = (String)moduleComboBox.getSelectedItem();
  }

  /**
   * Clears the page tokens so when we change the time order, we start from no page tokens.
   */
  public void clearPageTokens() {

    pageTokens.clear();
  }

  /**
   * clear the Combo boxes by removing all elements
   */
  public void clearComboBoxes() {

    defaultComboBoxModelModule.removeAllElements();
    defaultComboBoxModelVersion.removeAllElements();
  }

  /**
   * Enables Module Combo Box
   */
  public void setEnabledModuleComboBox() {

    moduleComboBox.setEnabled(true);
  }

  /**
   * Enables Version Combo Box
   */
  public void setEnabledVersionComboBox() {

    versionComboBox.setEnabled(true);
  }

  /**
   * Add Action Listener to Module Combo Box
   * @param modulesActionListener Modules Action Listener object
   */
  public void addModuleActionListener(ModulesActionListener modulesActionListener) {

    moduleComboBox.addActionListener(modulesActionListener);
  }

  /**
   * Add Action Listener to Version Combo Box
   * @param versionsActionListener Version Action Listener object
   */
  public void addVersionActionListener(VersionsActionListener versionsActionListener) {

    versionComboBox.addActionListener(versionsActionListener);
  }

  /**
   * Add Action Listener to Project Selector combo box
   * @param projectSelectorListener Project Selector Listener
   */
  public void addProjectSelectedListener(ProjectSelectorListener projectSelectorListener) {

    projectComboBox.getDocument().addDocumentListener(projectSelectorListener);
  }

  /**
   * Add Button Listener to Prev Page Button
   * @param prevPageButtonListener Prev Page Button Listener
   */
  public void addPrevPageButtonListener(PrevPageButtonListener prevPageButtonListener) {

    this.prevPageButton.addActionListener(prevPageButtonListener);
  }

  /**
   * Add Button Listener to Next Page Button
   * @param nextPageButtonListener Next Page Button Listener
   */
  public void addNextPageButtonListener(NextPageButtonListener nextPageButtonListener) {

    this.nextPageButton.addActionListener(nextPageButtonListener);
  }

  /**
   * Process the Modules List from the server to get the strings to set to GUI
   * @param listModulesResponse Response from the server with the modules list
   * @return ArrayList<String> of modules in string form to set to the GUI
   */
  public ArrayList<String> processModulesList(ListModulesResponse listModulesResponse) {

    if ((listModulesResponse == null)|| (listModulesResponse.getModules() == null)) {
      return null;
    }

    int defaultModuleIndex = -1;
    Module defaultModule = null;
    Module singleModule;
    ArrayList<String> modulesList = new ArrayList<String>();

    for (int modListIter = 0; modListIter < listModulesResponse.getModules().size(); modListIter++){
      singleModule = listModulesResponse.getModules().get(modListIter);
      modulesList.add(singleModule.getId());
      if (singleModule.getId().equals(DEFAULT_MODULE)){
        defaultModuleIndex = modListIter;
        defaultModule = singleModule;
      }
    }
    if (defaultModule != null) {
      this.defaultModIndex = defaultModuleIndex;
    } else {
      this.defaultModIndex = 0;
      defaultModule = listModulesResponse.getModules().get(0);
    }

    currModule = defaultModule.getId();

    return modulesList;
  }

  /**
   * Set the Module List to the GUI
   * @param modList ArrayList<String> of elements to set the GUI Module Combo Box to.
   */
  public void setModulesList(ArrayList<String> modList) {

    if (modList == null) {
      return;
    }

    setModuleALActive(false);
    defaultComboBoxModelModule.removeAllElements();

    for (String module: modList){
      defaultComboBoxModelModule.addElement(module);
    }

    defaultComboBoxModelModule.setSelectedItem(modList.get(defaultModIndex));

    setModuleALActive(true);
  }

  /**
   * Process the Versions List from the server to get the strings to set to GUI
   * @param listVersionsResponse Response from the server with the versions list
   * @return  ArrayList<String> of versions in string form to set to the GUI
   */
  public ArrayList<String> processVersionsList (ListVersionsResponse listVersionsResponse) {

    if((listVersionsResponse == null) || (listVersionsResponse.getVersions() == null)) {
      return null;
    }

    Version singleVersion;
    ArrayList<String> versionsList = new ArrayList<String>();

    for(int versIter = 0; versIter < listVersionsResponse.getVersions().size(); versIter++){
      singleVersion = listVersionsResponse.getVersions().get(versIter);
      versionsList.add(singleVersion.getId());
    }
    currVersion = listVersionsResponse.getVersions().get(0).getId();

    return versionsList;
  }

  /**
   * Set the Version List in GUI
   * @param versionsList ArrayList<String> to set the GUI Version Combo Box to
   */
  public void setVersionsList(ArrayList<String> versionsList) {

    if(versionsList == null) {
      return;
    }

    setVersionALActive(false);

    defaultComboBoxModelVersion.removeAllElements();
    for(String version: versionsList){
      defaultComboBoxModelVersion.addElement(version);
    }
    //set version to the first one in combobox
    defaultComboBoxModelVersion.setSelectedItem(defaultComboBoxModelVersion.getElementAt(0));

    setVersionALActive(true);
  }

  public void threadProcessAndSetLogs(ListLogEntriesResponse logResp){

    if ((logResp != null) && (logResp.getEntries() != null)) {
      processLogs(logResp);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          setLogs();
        }
      });
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          setRootText(NO_LOGS_LIST_STRING);
        }
      });
    }
  }

  /**
   * Process logs and get them ready to set to GUI tree
   * @param logResp Log Response from the server
   */
  public void processLogs(ListLogEntriesResponse logResp){

    root = new TextAreaNode(DEFAULT_LOGSTEXT, true, null);

    String nextPageToken = logResp.getNextPageToken();
    if ((nextPageToken != null) && (((currPage == -1) && (this.pageTokens.size() == 0)) ||
        (currPage+1 == this.pageTokens.size()))) {//page numbers go 0+ and size goes 1 +
      this.pageTokens.add(nextPageToken);
    }

    if (currPage+1 != this.pageTokens.size() && this.pageTokens.size() != 0) {
      this.nextPageTokenExist = true;
    }

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (currPage != -1) {
          prevPageButton.setEnabled(true);
        } else {
          prevPageButton.setEnabled(false);
        }

        if (nextPageTokenExist) {
          nextPageButton.setEnabled(true);
          nextPageTokenExist = false; //reset for next time usage
        } else {
          nextPageButton.setEnabled(false);
        }

      }
    });

    LogTreeEntry logTreeEntry;
    TextAreaNode parent;
    TextAreaNode child;


    for (int logEntriesIter = logResp.getEntries().size()-1; logEntriesIter > -1; logEntriesIter--) {
      LogEntry log = logResp.getEntries().get(logEntriesIter);
      logTreeEntry = new LogTreeEntry(log);
      parent = new TextAreaNode(logTreeEntry.getLogInfo(), true, logTreeEntry.getIcon());
      child = new TextAreaNode(logTreeEntry.getLogMessage(), false, null); //parent messa so no icon
      parent.add(child);

      for (LogTreeEntry childNode: logTreeEntry.getChildren()) {
        child = new TextAreaNode(childNode.getLogMessage(), false, childNode.getIcon());
        parent.add(child);
      }
      root.add(parent);
    }
  }

  /**
   * Set the list of Logs to the GUI
   */
  public void setLogs() {
    treeModel.setRoot(root);
    logs.setRootVisible(false);
    treeModel.reload();

    scrollPane.getVerticalScrollBar().setValue(0); //else confusing if it goes to middle or end!!

    if (logsExpanded) { //default is to not expand so need to specify this if the expanded toggle button is selected
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

    if (defaultComboBoxModelModule.getIndexOf(prevModule) > 0) {
      defaultComboBoxModelModule.setSelectedItem(prevModule);
    } else {
      defaultComboBoxModelModule.setSelectedItem(DEFAULT_MODULE);
    }
  }

  /**
   *  When refresh button is clicked, makes sure that the current version is set back the
   * version combo box. If the version has been deleted, it will use the first alphabetical version
   * @param prevVersion  String previous version that was the version selected before we reset
   *                   the modules, versions and logs.
   */
  public void setCurrVersionToVersionComboBox(String prevVersion) {

    if (defaultComboBoxModelVersion.getIndexOf(prevVersion) > 0) {
      defaultComboBoxModelVersion.setSelectedItem(prevVersion);
    } else {
      defaultComboBoxModelVersion.setSelectedItem(defaultComboBoxModelVersion.getElementAt(0));
    }
  }

  /**
   * gets the Array list of page tokens for getting the logs
   * @return ArrayList<String> page tokens we get from each log response to indicate if there
   * is a next page
   */
  public ArrayList<String> getPageTokens() {

    return this.pageTokens;
  }

  /**
   * Gets the Current Page Number
   * @return Integer value of the current page number
   */
  public int getCurrPage() {

    return this.currPage;
  }

  /**
   * Decrease the current Page number
   */
  public void decreasePage() {

    this.currPage--;
  }

  /**
   * Increase the page number
   */
  public void increasePage() {

    this.currPage++;
  }

  public void setRootText(String errorMessage) {

    setModuleALActive(false);
    setVersionALActive(false);
    resetComponents();
    setModuleALActive(true);
    setVersionALActive(true);

    root = new TextAreaNode(errorMessage, true, AllIcons.General.Error);
    treeModel.setRoot(root);
    treeModel.reload();
    logs.setRootVisible(true);
  }

  public String[] getLogLevelList() {
    return logLevelList;
  }

  public ArrayList<JBLabel> createLogLevelList() {

    ArrayList<JBLabel> logLevels = new ArrayList<JBLabel>();
    logLevels.add(new JBLabel("Critical", GoogleCloudToolsIcons.CRITICAL_LOG_ICON, JBLabel.RIGHT));
    logLevels.add(new JBLabel("Error", GoogleCloudToolsIcons.ERROR_LOG_ICON, JBLabel.RIGHT));
    logLevels.add(new JBLabel("Warning", GoogleCloudToolsIcons.WARNING_LOG_ICON, JBLabel.RIGHT));
    logLevels.add(new JBLabel("Info", GoogleCloudToolsIcons.INFO_LOG_ICON, JBLabel.RIGHT));
    logLevels.add(new JBLabel("Debug", GoogleCloudToolsIcons.DEBUG_LOG_ICON, JBLabel.RIGHT));
    logLevels.add(new JBLabel("Any Log Info", GoogleCloudToolsIcons.ANY_LOG_ICON, JBLabel.RIGHT));

    return logLevels;
  }

}
