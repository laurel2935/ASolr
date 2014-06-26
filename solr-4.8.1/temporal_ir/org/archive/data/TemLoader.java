package org.archive.data;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.archive.TDirectory;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic;
import org.archive.nlp.ner.StanfordNER;
import org.archive.util.IOBox;
import org.archive.util.StrStrInt;
import org.archive.util.StrStrStr;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

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
  
  
  ///////////////////////////////
  //document parsing
  ///////////////////////////////
  //[source-encoding]
  public static final String [] LPFields = {"url", "title", "sourcerss", "id", "host", "date", "content", "source-encoding"};
  
  /**
   * parse files: ..._solr.xml
   * **/
  public static List<TreeMap<String, String>> parseSolrFile(String file){
    List<TreeMap<String, String>> solrdocList = new ArrayList<>();
    
    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      //Document xmlDoc = saxBuilder.build(new File(file)); 
      //new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING)
      
      Document xmlDoc = saxBuilder.build(new InputStreamReader(new FileInputStream(file), "UTF-8"));
      Element webtrackElement = xmlDoc.getRootElement();
      //doc list
      List docList = webtrackElement.getChildren("doc");
      for(int i=0; i<docList.size(); i++){        
        Element docElement = (Element)docList.get(i);
        List fieldList = docElement.getChildren("field");
        
        TreeMap<String, String> solrdoc = new TreeMap<>();
        
        for(int j=0; j<fieldList.size(); j++){
          Element fieldElement = (Element)fieldList.get(j);
          String fieldName = fieldElement.getAttributeValue("name");
          String fieldText = fieldElement.getText();
          
          solrdoc.put(fieldName, fieldText);
        }
        
        String idStr = solrdoc.get("id");
        solrdoc.put("id", idStr.substring(1, idStr.length()-1));
        solrdocList.add(solrdoc);           
      }
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    /*
    if(debug){
      TreeMap<String, String> solrdoc = solrdocList.get(1);
      System.out.println(LPFields[0]+"\t"+solrdoc.get(LPFields[0]));
      System.out.println(LPFields[1]+"\t"+solrdoc.get(LPFields[1]));
      System.out.println(LPFields[2]+"\t"+solrdoc.get(LPFields[2]));
      System.out.println(LPFields[3]+"\t"+solrdoc.get(LPFields[3]));
      System.out.println(LPFields[4]+"\t"+solrdoc.get(LPFields[4]));
      System.out.println(LPFields[5]+"\t"+solrdoc.get(LPFields[5]));
      System.out.println(LPFields[6]+"\t"+solrdoc.get(LPFields[6]));
      
      if(solrdoc.containsKey(LPFields[7])){
        System.out.println(LPFields[7]+"\t"+solrdoc.get(LPFields[7]));
      }      
    }
    */
    
    return solrdocList;         
  }
  
  /**
   * parse files: ..._check.xml
   * **/
  public static List<TreeMap<String, String>> parseCheckFile(String file){
    List<TreeMap<String, String>> checkdocList = new ArrayList<>();
    
    ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(file);
    
    HashSet<String> tenseStrSet = new HashSet<>();
    
    try {
      //build a standard pseudo-xml file
      StringBuffer buffer = new StringBuffer();
      buffer.append("<add>");
      for(String line: lineList){
        buffer.append(line);
      }
      buffer.append("</add>");  
      
      SAXBuilder saxBuilder = new SAXBuilder();      
      Document xmlDoc = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes("UTF-8"))));
      
      Element webtrackElement = xmlDoc.getRootElement();
      
      //doc list
      XMLOutputter xmlOutputter = new XMLOutputter();
      
      List docList = webtrackElement.getChildren("doc");
      for(int i=0; i<docList.size(); i++){        
        TreeMap<String, String> checkdoc = new TreeMap<>();
        
        Element docElement = (Element)docList.get(i);
        String id = docElement.getAttributeValue("id");
        checkdoc.put("id", id);
        
        Element metaElement = docElement.getChild("meta-info");
        List tagList = metaElement.getChildren("tag");
        for(int j=0; j<tagList.size(); j++){
          Element tagElement = (Element)tagList.get(j);
          String tagName = tagElement.getAttributeValue("name");
          String tagText = tagElement.getText();
          
          checkdoc.put(tagName, tagText);
        }
        
        Element textElement = docElement.getChild("text");          
        String text = xmlOutputter.outputString(textElement);  
        
        if(debug){
          
          ArrayList<StrStrInt> tripleList = generateSentenceTriple(text);
          
          for(StrStrInt triple: tripleList){
            //initial annotation
            ArrayList<ArrayList<StrStrStr>> seAnnotationList = StanfordNER.suitParsing(triple.second);
            //
            //ArrayList<String> seNounList = StanfordNER.getNounTerms(seAnnotationList.get(0));
            String tenseStr = StanfordNER.getTenseStr(seAnnotationList.get(0));
            
            if(!tenseStrSet.contains(tenseStr)){
              tenseStrSet.add(tenseStr);
            }
          }
        }
        
        checkdoc.put("text", text);    
        
        checkdocList.add(checkdoc);           
      }
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    if(debug){
      TreeMap<String, String> checkdoc = checkdocList.get(100);
      for(Entry<String, String> entry: checkdoc.entrySet()){
         System.out.println(entry.getKey()+"\t"+entry.getValue());
         System.out.println();
      }   
      
      System.out.println();
      System.out.println(tenseStrSet.size());
      for(String tenseStr: tenseStrSet){
        System.out.println(tenseStr);
      }
    }   
    
    return checkdocList;
  }
  
  /**
   * triple list: seXml, seContent, int that indicates whether the queried nouns are included [1,0, -1(not identified)]
   * **/
  public static ArrayList<StrStrInt> generateSentenceTriple(String lpText){
    ArrayList<StrStrInt> tripleList = new ArrayList<>();
    
    try {
      SAXBuilder saxBuilder = new SAXBuilder();      
      Document xmlDoc = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(lpText.getBytes("UTF-8"))));
      
      Element webtrackElement = xmlDoc.getRootElement();
      
      //sen list
      XMLOutputter xmlOutputter = new XMLOutputter();      
      
      List seList = webtrackElement.getChildren("se");
      for(int i=0; i<seList.size(); i++){ 
        Element seElement = (Element)seList.get(i);
        
        String seXml = xmlOutputter.outputString(seElement);
        String seContent = seElement.getValue();
        
        tripleList.add(new StrStrInt(seXml, seContent, -1));        
      }    
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    if(debug){
      for(StrStrInt element: tripleList){
        System.out.println(element.first);
        System.out.println(element.second);
        System.out.println(element.third);
        System.out.println();
      }
    }
    
    return tripleList;
  }
    
  
  ////////////////////
  public static void main(String []args){
    //1
    //TemLoader.loadTemporalQuery();
    
    //2
    //TemLoader.loadTemporalRels();
    
    //3
    String file = "H:/v-haiyu/TaskPreparation/Temporalia/tool/t1.xml";
    TemLoader.parseCheckFile(file);
  }
  
}
