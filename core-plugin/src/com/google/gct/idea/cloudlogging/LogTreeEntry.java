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

import com.google.api.services.logging.model.LogEntry;

import javax.swing.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This class is solely for making it easier to print the logs in tree form.
 * It parses the Log Entries to get the information for the tree form of logs
 * Created by amulyau on 6/8/15.
 */
public class LogTreeEntry {

  /**Contains the log Info*/
  private String logInfo;

  /**Actual Message that is to be used as child for logInfo*/
  private String logMessage;

  /**
   * Icon each log message has for what level of log it is
   */
  private Icon severity;

  /**
   * Constructor to call parse the log
   * @param log Log Entry to parse and create LogTreeEntry
   */
  public LogTreeEntry(LogEntry log) {

    parseLog(log);
  }

  /**
   * Parse the log to create the proper messages as it is online
   * @param log Log Entry to get information from for the message
   */
  public void parseLog(LogEntry log){
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

    String severity = "";
    if(log.getMetadata().get("severity")!=null){

      severity = log.getMetadata().get("severity").toString();

    }
    setSeverityIcon(severity);
    this.logInfo = logInfo;
    this.logMessage = logMessage;

  }

  /**
   * Convert the UTC time we get from the server (formatted a certain way) to the local time
   * @param fullTimeUTC the UTC datetime format we get from the server
   * @return String with the hours:minutes:seconds.milliseconds formatted
   */
  private static String convertUTCToLocal(String fullTimeUTC) {
    //System.out.println("full timeUTC: "+fullTimeUTC);
    //get individual components
    int hour = Integer.parseInt(fullTimeUTC.substring(11, 13));
    int minutes = Integer.parseInt(fullTimeUTC.substring(14, 16));
    int seconds = Integer.parseInt(fullTimeUTC.substring(17, 19));
    int milliseconds = Integer.parseInt(fullTimeUTC.substring(20, 23));

    //get the local one and find the offset difference in time
    TimeZone tz = TimeZone.getDefault();
    Calendar cal = Calendar.getInstance(tz);
    int offsetInMillis = tz.getOffset(cal.getTimeInMillis());
   // System.out.println("millis: "+cal.getTimeInMillis());

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

    if(finalMinutes<10) {

      timeToReturn+=":0"+(finalMinutes);
    }else{

      timeToReturn+=":"+(finalMinutes);
    }

    if(seconds<10){

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
  //  System.out.println("good time: "+timeToReturn);
    return timeToReturn;
  }


  /**
   * Gets the log info
   * @return String log information
   */
  public String getLogInfo(){

    return this.logInfo;
  }

  /**
   * Get the log message
   * @return String log message
   */
  public String getLogMessage(){

    return this.logMessage;
  }

  /**
   * Gets the Icon used to represent severity of the log/ log level
   * @return Icon for the Jtree
   */
  public Icon getSeverityIcon(){

    return this.severity;
  }

  /**
   * Based on the severity string from the log entry, set the icon
   * @param severity String that explains log level
   */
  private void setSeverityIcon(String severity) {

    if(severity.trim().equals("")){ //null severity => star = any log level

      this.severity = AppEngineIcons.ANY_LOG_ICON;
    }else if(severity.equals("CRITICAL")){ //!!!

      this.severity = AppEngineIcons.CRITICAL_LOG_ICON;
    }else if(severity.equals("ERROR")){ //!!

      this.severity = AppEngineIcons.ERROR_LOG_ICON;
    }else if(severity.equals("WARNING")){ //!

      this.severity = AppEngineIcons.WARNING_LOG_ICON;
    }else if(severity.equals("INFO")){//i

      this.severity = AppEngineIcons.INFO_LOG_ICON;
    }else if(severity.equals("DEBUG")){//lambda

      this.severity = AppEngineIcons.DEBUG_LOG_ICON;
    }

  }
}
