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

import com.google.api.services.logging.model.ListLogEntriesResponse;

import javax.swing.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

/**
 * Created by amulyau on 6/26/15.
 */
public class ScrollActionListener implements AdjustmentListener {

  JScrollBar vertical;
  AppEngineLogging controller;
  AppEngineLogToolWindowView view;
  int prevValue = 0; //need to know if scrolling up or down. so that when scrolling up and get to min => get more logs
  //makes sure not to fire when we first start basically

  public ScrollActionListener(AppEngineLogging controller, AppEngineLogToolWindowView view){
    this.controller= controller;
    this.view = view;
    this.vertical = view.getVerticalScrollBar();
  }

  @Override
  public void adjustmentValueChanged(AdjustmentEvent e) {

    boolean askForNext = (((vertical.getValue()+vertical.getModel().getExtent())==vertical.getMaximum()) && (prevValue!=0));
    boolean ascTimeOrder = view.getTimeOrder();
    boolean askForPrev = (vertical.getValue() ==vertical.getMinimum() && prevValue>vertical.getMinimum());

    if (askForNext && ascTimeOrder){
      //based on time macro, ask for later/earlier logs via logResponse
      System.out.println("at bottom ascending");
      System.out.println("view.getCurrpage: " + view.getCurrPage());
      ListLogEntriesResponse logResp =
        controller.askForNextLog(view.getCurrPage(), view.getPageTokens());

      if(logResp!=null) {
        view.increasePage();
        view.setLogs(logResp);

        prevValue=0;
        vertical.setValue(0);
        return;
      }
    }else if(askForPrev && ascTimeOrder){
      //based on time macro, ask for earlier/later logs via logResponse
      System.out.println("at top ascending");
      System.out.println("view.getCurrpage: "+view.getCurrPage());

      ListLogEntriesResponse logResp =
        controller.askForPreviousLog(view.getCurrPage(), view.getPageTokens());
      if (logResp != null) {
        if(view.getCurrPage()>=0) {
          view.decreasePage();
        }
        view.setLogs(logResp);

      }

    }else if(askForNext && !ascTimeOrder){ //descending logs and we ask for next
      //based on time macro, ask for earlier/later logs via logResponse
      System.out.println("at top descending");
      System.out.println("view.getCurrpage: "+view.getCurrPage());

      ListLogEntriesResponse logResp =
        controller.askForPreviousLog(view.getCurrPage(), view.getPageTokens());
      if (logResp != null) {
        if(view.getCurrPage()>=0) {
          view.decreasePage();
        }
        view.setLogs(logResp);

      }
    }else if(askForPrev && !ascTimeOrder){ //descending logs and we ask for prev
      //based on time macro, ask for later/earlier logs via logResponse
      System.out.println("at bottom descending");
      System.out.println("view.getCurrpage: " + view.getCurrPage());
      ListLogEntriesResponse logResp =
        controller.askForNextLog(view.getCurrPage(), view.getPageTokens());

      if(logResp!=null) {
        view.increasePage();
        view.setLogs(logResp);

        prevValue=0;
        vertical.setValue(0);
        return;
      }
    }
    prevValue = e.getValue();
  }
}
