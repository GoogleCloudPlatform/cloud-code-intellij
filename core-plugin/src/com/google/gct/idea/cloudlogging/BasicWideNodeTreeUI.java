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
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * Created by amulyau on 6/23/15.
 */
public class BasicWideNodeTreeUI extends BasicTreeUI {
  private JTree tree;



  public BasicWideNodeTreeUI(){
    super();
  }

  @Override
  public void installUI(JComponent c){
    if(c==null){
      System.out.println("Oh no null");
    }else{
      tree = (JTree)c;
      super.installUI(c);
    }
  }

  @Override
  protected void prepareForUIInstall(){
    super.prepareForUIInstall();
    //  lastWidth = tree.getParent().getWidth();
  }


  @Override
  protected TreeCellRenderer createDefaultCellRenderer(){
    return new LogsTreeCellRenderer();
  }

  @Override
  protected AbstractLayoutCache.NodeDimensions createNodeDimensions(){
    return new NodeDimensionsHandler();
  }

  public class NodeDimensionsHandler extends AbstractLayoutCache.NodeDimensions{
    @Override
    public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle size) {
      //using renderer not editor
      if(currentCellRenderer!=null){
        Component comp;
        comp = currentCellRenderer.getTreeCellRendererComponent(tree,value,tree.isRowSelected(row), expanded,treeModel.isLeaf(value), row, false);
        if(tree!=null){
          rendererPane.add(comp);
          comp.validate();
        }
        //we know comp = text area always =>
        JTextArea textArea = ((PanelExtend)comp).getLogText();
        Icon icon = ((PanelExtend)comp).getLableIcon().getIcon();

        Dimension preferredSize = comp.getPreferredSize();
        //   System.out.println("gets here?!");
        if(size!=null){
          size.x = getRowX(row, depth);
          // System.out.println("last width: "+lastWidth);
          boolean textWrap;
          float fontSize;
          if(tree.getName().contains("true")){
            textWrap = true;

          }else{
            textWrap = false;
          }
          fontSize = Integer.parseInt(tree.getName().substring(tree.getName().indexOf('e')+1));
          //     System.out.println("font size: "+ fontSize);
          textArea.setFont(textArea.getFont().deriveFont(fontSize));
          //     System.out.println("font size after set: "+((PanelExtend)comp).getLogText().getFont().getSize());

          FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
          if(textArea.getText().trim().equals("...Load Previous Page...") || textArea.getText().trim().equals("...Load Next Page...")) {
            if(textWrap==true){
              size.width = SwingUtilities.computeStringWidth(fm, textArea.getText());
              size.height = fm.getHeight()*5;
            }else{
              size.width = tree.getParent().getWidth() + size.x + 3;
              size.height = fm.getHeight()*5;
            }

          }else {

            Dimension dim = getTextDimensions(tree.getParent().getWidth(), size.x, depth, textArea, icon, textWrap, textArea.getPreferredSize().getWidth());

            size.width = ((int)dim.getWidth());
            size.height = ((int)dim.getHeight());
          }
        }else{
          size = new Rectangle(getRowX(row,depth),0,preferredSize.width,preferredSize.height);
        }
        return size;

      }
      return null;
    }
  }

  private static Dimension getTextDimensions(int currentWidth, int sizeX, int depth, JTextArea textArea, Icon icon, boolean textWrap, double defaultWidth) {
    FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
    String text =textArea.getText();


    int lines =0;
    int lastSpace = 0;
    int x=0;
    int lineWidth;// =0;
    int y=0;
    int newCurrWidth=currentWidth-sizeX-icon.getIconWidth()-3;//+SwingUtilities.computeStringWidth(fm,text.substring(text.length()-1, text.length()));//-(depth*3);

    if(!textWrap){
      int largestWidth = 0;
      for (; y < text.length(); y++) {
        lineWidth = SwingUtilities.computeStringWidth(fm, text.substring(x, y));
        char space = text.charAt(y);

        if (space == '\n') {
          x = y + 1;
          y++;
          lines++;
          continue;
        }
        if(lineWidth>largestWidth){
          largestWidth=lineWidth;
        }
      }
      lines++;
      if(lines<1){
        lines = 1;
      }

      int height = fm.getHeight()*lines;
      //     System.out.println("fm: "+fm.getHeight()+"\t\t lines: "+lines+"\t\t height: "+height);
      return new Dimension(largestWidth+sizeX+icon.getIconWidth()+3,height);

    }else { //text wrap is true


      for (; y < text.length(); y++) {
        lineWidth = SwingUtilities.computeStringWidth(fm, text.substring(x, y));
        char space = text.charAt(y);
        if (space == ' ' || space == '\t') {
          lastSpace = y;
        }
        else if (space == '\n') {
          x = y + 1;
          y++;
          lines++;
          continue;
        }

        if (lineWidth == newCurrWidth) {
          //  x=lastSpace; //realistic word examples this works else x=y works...
          if (lastSpace > x && lastSpace < y) {
            x = lastSpace;
            y = lastSpace;
          }
          else {
            x = y;
          }
          lines++;
        }
        else if (lineWidth > newCurrWidth) {//width too large => go back to when it was less than as it never equals
          int z = y;
          for (; z > 0; z--) {
            lineWidth = SwingUtilities.computeStringWidth(fm, text.substring(x, z));
            if (lineWidth < newCurrWidth) {
              if (lastSpace > x && lastSpace < y) {
                x = lastSpace;
                y = lastSpace;
                lines++;
                break;
              }
              else {
                x = z;
                y = z;
                lines++;
                break;
              }
            }
          }

        }
      }

      if(x!=0 && x<y){ //still some left
        lines++;
      }

    }

    // lines++;
    if(lines<1){
      lines=1;
    }


    int height = fm.getHeight()*lines;
    //     System.out.println("fm: "+fm.getHeight()+"\t\t lines: "+lines+"\t\t height: "+height);
    return new Dimension(currentWidth-sizeX-3,height);

  }

}
