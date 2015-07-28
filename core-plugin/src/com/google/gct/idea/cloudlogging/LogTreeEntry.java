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

import com.google.api.client.util.ArrayMap;
import com.google.api.services.logging.model.LogEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import javax.swing.*;

import icons.GoogleCloudToolsIcons;

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
  /**children nodes based on the log lines*/
  private ArrayList<LogTreeEntry> children;
  private Icon severity;

  /**
   * Constructor used for children logs that don't have Log Info only LogMessages
   * @param severity Severity Icon for the LogTreeEntry
   * @param logMessage LogMessage of the children logs.
   */
  public LogTreeEntry(Icon severity, String logMessage) {
    this.children=null;
    this.severity = severity;
    this.logInfo=null;
    this.logMessage=logMessage;
  }

  /**
   * Constructor to call parse the log
   * @param log Log Entry to parse and create LogTreeEntry
   */
  public LogTreeEntry(LogEntry log) {
    this.children = new ArrayList<LogTreeEntry>();
    parseLog(log);
  }

  /**
   * Parse the log to create the proper messages as it is online
   * @param log Log Entry to get information from for the message
   */
  private void parseLog(LogEntry log) {
    String t = "        ";

    String fullTimeUTC = log.getProtoPayload().get("endTime").toString();
    TimeDayChange timeDayChange = convertUTCToLocal(fullTimeUTC);
    String localTime = timeDayChange.getTime();

    String year = fullTimeUTC.substring(0, 4);
    String month = fullTimeUTC.substring(5, 7);
    String  day = fullTimeUTC.substring(8, 10);

    Calendar date = Calendar.getInstance();
    date.set(Calendar.YEAR, Integer.parseInt(year));

    switch (Integer.parseInt(month)) { //doing this due to Magic Constants complaint from java.
      case 1: date.set(Calendar.MONTH, Calendar.JANUARY); break;
      case 2: date.set(Calendar.MONTH, Calendar.FEBRUARY); break;
      case 3: date.set(Calendar.MONTH, Calendar.MARCH); break;
      case 4: date.set(Calendar.MONTH, Calendar.APRIL); break;
      case 5: date.set(Calendar.MONTH, Calendar.MAY); break;
      case 6: date.set(Calendar.MONTH, Calendar.JUNE); break;
      case 7: date.set(Calendar.MONTH, Calendar.JULY); break;
      case 8: date.set(Calendar.MONTH, Calendar.AUGUST); break;
      case 9: date.set(Calendar.MONTH, Calendar.SEPTEMBER); break;
      case 10: date.set(Calendar.MONTH, Calendar.OCTOBER); break;
      case 11: date.set(Calendar.MONTH, Calendar.NOVEMBER); break;
      case 12: date.set(Calendar.MONTH, Calendar.DECEMBER); break;
      default: date.set(Calendar.MONTH, Calendar.JANUARY); //just in case
    }
    date.set(Calendar.DATE, Integer.parseInt(day));

    if(timeDayChange.getDayChange() == -1) { //subtract a day
      date.add(Calendar.DATE, -1);
    } else if (timeDayChange.getDayChange() == 1) {
      date.add(Calendar.DATE, 1);
    }

    String status = log.getProtoPayload().get("status").toString();

    String latency = log.getProtoPayload().get("latency").toString();
    latency = latency.substring(0, latency.length() -1); //remove the s at end
    Double latencyParsed = Double.parseDouble(latency);

    if (latencyParsed.intValue() == 0) {
      latencyParsed *= 1000; //convert to ms
      long latencyParse = Math.round(latencyParsed);
      latency = latencyParse + "ms";
    } else {
      latency = String.format("%.2f", latencyParsed) + "s"; //round to 2 decimal places
    }

    String resource = log.getProtoPayload().get("resource").toString();
    String HTTP = log.getProtoPayload().get("httpVersion").toString();
    String ipAddress = log.getProtoPayload().get("ip").toString();
    String host = log.getProtoPayload().get("host").toString();
    String appEngRelease = "app_engine_release=" + log.getProtoPayload().get("appEngineRelease");

    String logMessageWithTime;
    String childTime;
    ArrayMap<String, String> logMess;
    if (log.getProtoPayload().get("line") != null) {
      ArrayList logMessages = ((ArrayList) log.getProtoPayload().get("line"));
      for (int iterMessages = 0; iterMessages < logMessages.size(); iterMessages++) {
        logMess = (ArrayMap<String, String>) logMessages.get(iterMessages);
        childTime = convertUTCToLocal(logMess.get("time")).time;
        logMessageWithTime = childTime + t + logMess.get("logMessage");
        children.add(new LogTreeEntry(getSeverityIcon(logMess.get("severity")), logMessageWithTime.
            substring(0, logMessageWithTime.length() - 1)));
      }
    }

    String method = log.getProtoPayload().get("method").toString();

    String responseSize = "";

    if (log.getProtoPayload().get("responseSize") == null) {
      responseSize += "0";
    } else {
      responseSize += log.getProtoPayload().get("responseSize").toString();
    }

    String userAgent = "";
    if (log.getProtoPayload().get("userAgent") != null) {
      userAgent += " \"" + log.getProtoPayload().get("userAgent").toString() + '\"';
    }

    String instance = "instance=" + log.getMetadata().getLabels().
        get("appengine.googleapis.com/clone_id");

    String logInfo = localTime + "    "+ status + t + "bytes stuff" + t + latency + t + resource;
    String message = ipAddress + " - - " + "[" + date.get(Calendar.DATE) + "/" +
        date.get(Calendar.MONTH) + "/" + date.get(Calendar.YEAR) + "] " + " \"" +
        method + " " + resource + " " + HTTP + "\" " + status + " " + responseSize + userAgent +
        " - - \"" + host + "\" " + instance + " " + appEngRelease;

    String severity = "";
    if (log.getMetadata().get("severity") != null) {
      severity = log.getMetadata().get("severity").toString();
    }

    this.severity = getSeverityIcon(severity);
    this.logInfo = logInfo;
    this.logMessage = message;
  }

  /**
   * Convert the UTC time we get from the server (formatted a certain way) to the local time
   * @param fullTimeUTC the UTC datetime format we get from the server
   * @return String with the hours:minutes:seconds.milliseconds formatted
   */
  private TimeDayChange convertUTCToLocal(String fullTimeUTC) {
    //get individual components
    int hour = Integer.parseInt(fullTimeUTC.substring(11, 13));
    int minutes = Integer.parseInt(fullTimeUTC.substring(14, 16));
    int seconds = Integer.parseInt(fullTimeUTC.substring(17, 19));
    int milliseconds = Integer.parseInt(fullTimeUTC.substring(20, 23));

    char rMillis = fullTimeUTC.charAt(23);
    if ((rMillis > 52) && (rMillis < 58)) { //rMillis is a number and greater than 4
      milliseconds++;
    }
    //get the local one and find the offset difference in time
    TimeZone tz = TimeZone.getDefault();
    Calendar cal = Calendar.getInstance(tz);
    int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

    int offsetHours = offsetInMillis / 3600000;
    int offsetMinutes = (offsetInMillis / 60000) % 60;

    //mod back to the correct time = clock = on 24 hour
    int finalHours = (((hour + offsetHours) + 23) % 24) + 1;
    int finalMinutes = (((minutes + offsetMinutes) + 59) % 60) + 1;

    if (finalMinutes == 60) {

      finalMinutes = finalMinutes % 60;
    }
    //string to format properly
    StringBuilder timeToReturn = new StringBuilder();

    if (finalHours < 10) {
      timeToReturn.append("0").append(finalHours);
    } else {
      timeToReturn.append(finalHours);
    }

    if (finalMinutes < 10) {
      timeToReturn.append(":0").append(finalMinutes);
    } else {
      timeToReturn.append(":").append(finalMinutes);
    }

    if (seconds < 10) {
      timeToReturn.append(":0").append(seconds);
    } else {
      timeToReturn.append(":").append(seconds);
    }

    if (milliseconds < 10) {
      timeToReturn.append(".00").append(milliseconds);
    } else if (milliseconds < 100) {
      timeToReturn.append(".0").append(milliseconds);
    } else {
      timeToReturn.append(".").append(milliseconds);
    }

    int dayChanges = 0;
    if (hour + offsetHours > 24) {
      dayChanges = 1;
    } else if (hour + offsetHours < 0) {
      dayChanges = -1;
    }

    return new TimeDayChange(timeToReturn.toString(), dayChanges);
  }

  /**
   * Gets the log info
   * @return String log information
   */
  public String getLogInfo() {
    return this.logInfo;
  }

  /**
   * Get the log message
   * @return String log message
   */
  public String getLogMessage() {
    return this.logMessage;
  }

  /**
   * Gets the Icon used to represent severity of the log/ log level
   * @return Icon for the JTree
   */
  public Icon getIcon() {
    return this.severity;
  }

  /**
   * Gets ArrayList of children nodes with log messages to add
   * @return ArrayList<LogTreeEntry> of children nodes
   */
  public ArrayList<LogTreeEntry> getChildren() {
    return children;
  }

  /**
   * Based on the severity string from the log entry, set the icon
   * @param severity String that explains log level
   */
  private Icon getSeverityIcon(String severity) {
    if (severity.trim().equals("")) { //null severity => star = any log level
      return GoogleCloudToolsIcons.ANY_LOG_ICON;
    } else if (severity.equals("CRITICAL")) { //!!!
      return GoogleCloudToolsIcons.CRITICAL_LOG_ICON;
    } else if (severity.equals("ERROR")) { //!!
      return GoogleCloudToolsIcons.ERROR_LOG_ICON;
    } else if (severity.equals("WARNING")) { //!
      return GoogleCloudToolsIcons.WARNING_LOG_ICON;
    } else if (severity.equals("INFO")) {//i
      return GoogleCloudToolsIcons.INFO_LOG_ICON;
    } else if (severity.equals("DEBUG")) {//lambda
      return GoogleCloudToolsIcons.DEBUG_LOG_ICON;
    } else {
      return null;
    }
  }

  /**
   * Allows a string and boolean to pass from method to method to indicate time and if the
   * day also changes.
   */
  private class TimeDayChange {
    /**UTC to current time zone converted time in string form*/
    private String time;
    /**Indicates whether the day changes or not*/
    private int dayChange;

    /**
     * Constructor
     * @param time UTC to current time zone converted time in string format
     * @param dayChange -1 if the day needs to go back a day, 0 if no change,
     *                  else 1 if the day needs to go forward one
     */
    public TimeDayChange(String time, int dayChange) {
      this.time = time;
      this.dayChange = dayChange;
    }

    /**
     * Gets the time in string form
     * @return String of time
     */
    public String getTime() {
      return time;
    }

    /**
     * Get integer to indicate in which direction, the day changed
     * @return -1 to subtract a day, 0 to keep it same, else, 1 to increase day
     */
    public int getDayChange() {
      return dayChange;
    }

  }

}
