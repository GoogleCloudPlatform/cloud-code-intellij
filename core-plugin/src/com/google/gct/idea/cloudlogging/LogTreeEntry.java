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

import javax.swing.*;

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
   * @param logInfo log information
   * @param logMessage log message
   */
  public LogTreeEntry(String logInfo, String logMessage) {

      this.logInfo = logInfo;
      this.logMessage = logMessage;

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



}
