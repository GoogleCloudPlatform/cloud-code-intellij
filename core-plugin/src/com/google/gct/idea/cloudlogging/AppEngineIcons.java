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

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Holds all the icons for the App Engine Logs Viewer Plugin
 * Created by amulyau on 6/22/15.
 */
public class AppEngineIcons {

  /**
   * Loads the Icons given the path
   * @param path String path of the location for the icons
   * @return Icon from the path of the location
   */
  private static Icon load(String path) {

    return IconLoader.getIcon(path, AppEngineIcons.class);
  }


  /**Icon for the Tool Window*/
  public static final Icon TOOL_WINDOW_ICON = load("/icons/cloudPlatform.png");

  /**Icon for the Tool Window Content*/
  public static final Icon TOOL_WINDOW_CONTENT_ICON =  load("/icons/cloudPlatform.png");

  /**Icon for the Toggle Icon for Text Wrap*/
  public static final Icon WRAP_TOGGLE_ICON = load("/icons/cloudPlatform.png");

  /**Icon for the Toggle Icon for Text Wrap*/
  public static final Icon EXPAND_TOGGLE_ICON = load("/icons/refresh.png");

  /**Icon for the Toggle Icon for Time Order Ascending*/
  public static final Icon ASC_ORDER_ICON = load("/icons/cloudPlatform.png");

  /**Icon for the Toggle Icon for the prev page button aka < */
  public static final Icon PREV_PAGE_ICON = load("/icons/refresh.png");

  /**Icon for the Toggle Icon for next page button aka > */
  public static final Icon NEXT_PAGE_ICON = load("/icons/cloudPlatform.png");

  /**Icon for the Font change label that pops up the slider to get the text. */
  public static final Icon FONT_CHANGE_ICON = load("/icons/cloudPlatform.png");

  /**Refresh Button (in left side tool bar of tool window)'s Icon*/
  public static final Icon REFRESH_BUTTON_ICON = load("/icons/refresh.png");

  /**Icon for JTree for Critical Log */
  public static final Icon CRITICAL_LOG_ICON = load("/icons/debugdeleteall.png");

  /**Icon for JTree for Error Log */
  public static final Icon ERROR_LOG_ICON = load("/icons/debugdeleteall.png");

  /**Icon for JTree for Warning Log */
  public static final Icon WARNING_LOG_ICON = load ("/icons/debugdeleteall.png");

  /**Icon for JTree for Info Log */
  public static final Icon INFO_LOG_ICON = load("/icons/debugdeleteall.png");

  /**Icon for JTree for Debug Log */
  public static final Icon DEBUG_LOG_ICON = load("/icons/debugdeleteall.png");

  /**Icon for JTree for Any Log */
  public static final Icon ANY_LOG_ICON = load("/icons/debugdeleteall.png");

  /**Icon for JTree for Root that asks to select project*/
  public static final Icon ROOT_ICON = load("/icons/debugdeleteall.png");

}
