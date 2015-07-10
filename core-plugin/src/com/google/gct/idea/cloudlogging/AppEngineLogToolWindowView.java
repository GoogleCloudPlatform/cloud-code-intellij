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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.TimeZone;

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
  private JPanel mainInfoPanel;
  private JBScrollPane scrollPane;
  private JTextArea logs;

  /**Panel for the Top Bar of the Tool Window*/
  final JPanel toolWindowBarPanel= new JPanel();

  /**Button to get the project info (necessary due to Project Selector's touchy nature)*/
  private JButton projectInfoButton = new JButton("Get Project Info");

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

  /**Need this manual width, else the combo box resizes with text*/
  private static final int MIN_COMBOBOX_WIDTH = 135;

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

  /**Helps deal with combo box touchiness issues*/
  private boolean moduleALActive = true;
  /**Helps deal with combo box touchiness issues*/
  private boolean versionALActive = true;

  /**Panel with the Search Box area*/
  private JPanel searchBox = new JPanel();

  /**Current app id*/
  private String currentAppID;

  /**
   * gets the panel with the UI tool window bar elements
   * @return JPanel with tool window bar elements
   */
  public JPanel getMainInfoPanel(){

    return mainInfoPanel;
  }

  /**
   * Constructor to get Tree to display properly.
   */
  public AppEngineLogToolWindowView(){

    logs.setEditable(false);
    logs.setLineWrap(true);
    logs.setWrapStyleWord(true);
    logs.setText(DEFAULT_LOGSTEXT);
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

    projectComboBox.setEnabled(true);

    projectInfoButton.setEnabled(true);

    toolWindowBarPanel.add(projectInfoButton);
    toolWindowBarPanel.add(projectComboBox);
    toolWindowBarPanel.add(moduleComboBox);
    toolWindowBarPanel.add(versionComboBox);

    final JPanel searchComponentPanel = new JPanel();
    searchComponentPanel.setLayout(new BoxLayout(searchComponentPanel,BoxLayout.X_AXIS));
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

    logs.setText(DEFAULT_LOGSTEXT);
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

    JLabel searchBox = new JLabel("Search Box goes here");
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
   * Add Action Listener to the Get Project Info Button
   * @param projectInfoButtonListener Project Info Button Listener
   */
  public void addProjectInfoButtonListener(ProjectInfoButtonListener projectInfoButtonListener){

    projectInfoButton.addActionListener(projectInfoButtonListener);
  }

  /**
   * Set the Modules List
   * @param listModulesResponse Response from the server with the modules list
   */
  public void setModulesList(ListModulesResponse listModulesResponse){

    if(listModulesResponse==null){
      Messages.showErrorDialog(ERROR_MODULES_LIST_NULL, ERROR_MODULES_LIST_TITLE);
      return;
    }

    setModuleALActive(false);

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

    if(listVersionsResponse==null){
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

    LogTreeEntry logTreeEntry;
    String toPrint = "";

    //get logs and add them to the tree
    for(int logEntriesIter = 0; logEntriesIter< logResp.getEntries().size(); logEntriesIter++) {

      LogEntry log = logResp.getEntries().get(logEntriesIter);
      logTreeEntry = printProperly(log);

      if (!logTreeEntry.getLogMessage().trim().isEmpty()) { //has log message

        toPrint += logTreeEntry.getLogInfo() + '\n' + '\t' + logTreeEntry.getLogMessage()+'\n';
      }else {

        toPrint += logTreeEntry.getLogInfo() + '\n';
      }

      toPrint+="________________________________________________________________________________\n";
    }

    logs.setText(toPrint);

  }

  /**
   * Get the information for each log entry and print it properly as App Engine does
   * @param log Log Entry to get the information from.
   */
  private static LogTreeEntry printProperly(LogEntry log){

    String t = "        ";

    String fullTimeUTC = log.getProtoPayload().get("endTime").toString();
    String localTime = convertUTCToLocal(fullTimeUTC);
    String year = fullTimeUTC.substring(0, 4);
    String month = fullTimeUTC.substring(5, 7);
    String  day = fullTimeUTC.substring(8, 10);
    String status = log.getProtoPayload().get("status").toString();

    String latency=log.getProtoPayload().get("latency").toString();
    latency=latency.substring(0,latency.length()-1); //remove the s at end
    Double latencyParsed = Double.parseDouble(latency);

    if(latencyParsed.intValue()==0){

      latencyParsed *= 1000; //convert to ms
      long latencyParse = Math.round(latencyParsed);
      latency=latencyParse+"ms";
    }else{

      latency = String.format("%.2f", latencyParsed)+"s"; //round to 2 decimal places
    }

    String resource = log.getProtoPayload().get("resource").toString();
    String HTTP = log.getProtoPayload().get("httpVersion").toString();
    String ipAddress = log.getProtoPayload().get("ip").toString();
    String host = log.getProtoPayload().get("host").toString();
    String appEngRelease = "app_engine_release="+log.getProtoPayload().get("appEngineRelease");

    String message = "";

    if(log.getProtoPayload().get("line")!=null){

      String line = log.getProtoPayload().get("line").toString();
      String messageTime= line.substring(line.indexOf("time=")+5, line.indexOf(", severity="));
      String finalMessageTime = convertUTCToLocal(messageTime);

      message = finalMessageTime+"\t" + line.substring(line.indexOf("logMessage=") + 11,
                                                       line.indexOf("}]"));
    }
    String method = log.getProtoPayload().get("method").toString();

    String responseSize="";

    if(log.getProtoPayload().get("responseSize")==null){

      responseSize+="0";
    }else{

      responseSize+=log.getProtoPayload().get("responseSize").toString();
    }

    String userAgent = "";

    if(log.getProtoPayload().get("userAgent")!=null){

      userAgent+=" \""+log.getProtoPayload().get("userAgent").toString()+'\"';
    }

    String instance="instance="+log.getMetadata().getLabels().
      get("appengine.googleapis.com/clone_id");


    String logInfo =localTime+"    "+status+t+ "bytes stuff"+t+latency+t+resource+'\n'+ipAddress+
                    " - - "+"["+day+"/"+month+"/"+year+"] "+" \""+method+" "+resource+" "+
                    HTTP+"\" "+status+" "+responseSize+userAgent+" - - \""+host+"\" "+instance+" "+
                    appEngRelease;
    String logMessage = message;

    return new LogTreeEntry(logInfo, logMessage);

  }

  /**
   * Convert the UTC time we get from the server (formatted a certain way) to the local time
   * @param fullTimeUTC the UTC datetime format we get from the server
   * @return String with the hours:minutes:seconds.milliseconds formatted
   */
  private static String convertUTCToLocal(String fullTimeUTC) {

    //get individual components
    int hour = Integer.parseInt(fullTimeUTC.substring(11, 13));
    int minutes = Integer.parseInt(fullTimeUTC.substring(14, 16));
    int seconds = Integer.parseInt(fullTimeUTC.substring(17, 19));
    int milliseconds = Integer.parseInt(fullTimeUTC.substring(20, 23));

    //get the local one and find the offset difference in time
    TimeZone tz = TimeZone.getDefault();
    Calendar cal = Calendar.getInstance(tz);
    int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

    int offsetHours = offsetInMillis/3600000;
    int offsetMinutes =(offsetInMillis/60000)%60;

    //mod back to the correct time = cloud = on 12 hour clock not 24 hour
    int finalHours = (((hour+offsetHours)+11)  % 12) +1;
    int finalMinutes = (((minutes+offsetMinutes)+59)%60)+1;

    if(finalMinutes==60){

      finalMinutes=finalMinutes%60;
    }

    //string to format properly
    String timeToReturn = "";

    if(finalHours<10){

      timeToReturn+="0"+finalHours;
    }else{

      timeToReturn+=finalHours;
    }

    if(finalMinutes<0) {

      timeToReturn+=":0"+(finalMinutes);
    }else{

      timeToReturn+=":"+(finalMinutes);
    }

    if(seconds<0){

      timeToReturn+=":0"+seconds;
    }else{

      timeToReturn+=":"+seconds;
    }

    if(milliseconds<10){

      timeToReturn+=".00"+milliseconds;
    }else if(milliseconds<100){

      timeToReturn+=".0"+milliseconds;
    }else{

      timeToReturn+="."+milliseconds;
    }

    return timeToReturn;
  }

}
