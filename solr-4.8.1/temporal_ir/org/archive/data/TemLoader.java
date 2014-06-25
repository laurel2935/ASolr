package org.archive.data;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.archive.TDirectory;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic;
import org.archive.util.IOBox;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class TemLoader {
  private static final boolean debug = true;
  
  
  //
  public static ArrayList<TemQuery> loadTemporalQuery(){
    ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(TDirectory.NTCIR11_TIR_DryRunQueryFile);
    
    //build a standard pseudo-xml file
    StringBuffer buffer = new StringBuffer();
    buffer.append("<add>");
    for(String line: lineList){
      buffer.append(line);
    }
    buffer.append("</add>");  
    
    ArrayList<TemQuery> temQueryList = new ArrayList<>();
    
    try {
      SAXBuilder saxBuilder = new SAXBuilder();      
      Document xmlDoc = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes("UTF-8"))));
      
      Element webtrackElement = xmlDoc.getRootElement();          
      
      List topicList = webtrackElement.getChildren("topic");
      for(int i=0; i<topicList.size(); i++){ 
        Element topicElement = (Element)topicList.get(i);
        
        String id = topicElement.getChildText("id");
        String title = topicElement.getChildText("title");
        String des = topicElement.getChildText("description");
        String queryTime = topicElement.getChildText("query_issue_time");
        
        TemQuery temQuery = new TemQuery(id, title, des, queryTime);
        
        ArrayList<TemSubtopic> temSubtopicList = new ArrayList<TemSubtopic>();    
        
        List subtopicList = topicElement.getChild("subtopics").getChildren("subtopic");
        for(int j=0; j<subtopicList.size(); j++){
          Element subtopicElement = (Element)subtopicList.get(j);
          String subtopicId = subtopicElement.getAttributeValue("id");
          String subtopicType = subtopicElement.getAttributeValue("type");
          String subtopicTitle = subtopicElement.getText();
          
          temSubtopicList.add(new TemSubtopic(subtopicId, subtopicType, subtopicTitle));
        }
        
        temQuery.setSubtopicList(temSubtopicList);
        
        temQueryList.add(temQuery);       
      }
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    if(debug){
      System.out.println(temQueryList.get(1).toString());
    }
    
    return temQueryList;  
  }
  //
  public static TreeMap<String,ArrayList<String>> loadTemporalRels(){
    TreeMap<String,ArrayList<String>> relMap = new TreeMap<String,ArrayList<String>>();
    
    ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(TDirectory.NTCIR11_TIR_DryRunRelsFile);
    
    for(String line: lineList){
      String [] array = line.trim().split("\\s");
      
      if(relMap.containsKey(array[0])){
        relMap.get(array[0]).add(array[1]);
      }else {
        ArrayList<String> docList = new ArrayList<>();
        docList.add(array[1]);
        
        relMap.put(array[0], docList);
      }      
    }
    
    if(debug){
      System.out.println(relMap.get("013a"));
    }
    
    return relMap;
  }
  
  public static void main(String []args){
    //1
    //TemLoader.loadTemporalQuery();
    
    //2
    //TemLoader.loadTemporalRels();
  }
  
}
