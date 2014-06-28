package org.archive.data;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import org.jdom.output.Format;
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
  private static final boolean debug = false;
  
  //
  public static enum TemRunType{DryRun, FormalRun};
  
  
  //
  public static ArrayList<TemQuery> loadTemporalQuery(TemRunType temRunType){
    ArrayList<String> lineList;
    if(temRunType == TemRunType.DryRun){
      lineList = IOBox.getLinesAsAList_UTF8(TDirectory.NTCIR11_TIR_DryRunQueryFile);
    }else{
      lineList = IOBox.getLinesAsAList_UTF8(TDirectory.NTCIR11_TIR_FormalRunQueryFile);
    }    
    
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
  
  public static String stripNonValidXMLCharacters(String in) {
    StringBuffer out = new StringBuffer(); // Used to hold the output.
    char current; // Used to reference the current character.

    if (in == null || ("".equals(in))) return ""; // vacancy test.
    
    for (int i = 0; i < in.length(); i++) {
        current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
        if ((current == 0x9) ||
            (current == 0xA) ||
            (current == 0xD) ||
            ((current >= 0x20) && (current <= 0xD7FF)) ||
            ((current >= 0xE000) && (current <= 0xFFFD)) ||
            ((current >= 0x10000) && (current <= 0x10FFFF)))
            out.append(current);
    }
    
    return out.toString();
}
  //
  //[source-encoding]
  public static final String [] LPFields = {"url", "title", "sourcerss", "id", "host", "date", "content", "source-encoding"};
  
  public static String toSolrXml(org.apache.lucene.document.Document solrDoc){
    StringBuffer buffer = new StringBuffer();
    buffer.append("<doc>\n");
    
    buffer.append("<id>"+solrDoc.get("id")+"</id>\n");
    buffer.append("<url>"+solrDoc.get("url")+"</url>\n");
    buffer.append("<sourcerss>"+solrDoc.get("sourcerss")+"</sourcerss>\n");
    buffer.append("<host>"+solrDoc.get("host")+"</host>\n");
    buffer.append("<date>"+solrDoc.get("date")+"</date>\n");
    
    String code = solrDoc.get("source-encoding");
    if(null != code){
      buffer.append("<source-encoding>"+code+"</source-encoding>\n");
    }    
    
    buffer.append("<title>"+solrDoc.get("title")+"</title>\n");
    buffer.append("<content>"+solrDoc.get("content")+"</content>\n");    
    
    buffer.append("</doc>");
    
    return buffer.toString();    
    /*
    Element docElement = new Element("doc");
    Document rawDoc = new Document(docElement);
    rawDoc.setRootElement(docElement);
    
    Element idElement = new Element("id");
    idElement.addContent(solrDoc.get("id"));
    rawDoc.getRootElement().addContent(idElement);       
    
    Element urlElement = new Element("url");
    urlElement.addContent(solrDoc.get("url"));
    rawDoc.getRootElement().addContent(urlElement);    
    
    Element rssElement = new Element("sourcerss");
    rssElement.addContent(solrDoc.get("sourcerss"));
    rawDoc.getRootElement().addContent(rssElement);
    
    Element hostElement = new Element("host");
    hostElement.addContent(solrDoc.get("host"));
    rawDoc.getRootElement().addContent(hostElement);
    
    Element dateElement = new Element("date");
    dateElement.addContent(solrDoc.get("date"));
    rawDoc.getRootElement().addContent(dateElement);
    
    Element codeElement = new Element("source-encoding");
    String code = solrDoc.get("source-encoding");
    if(null != code){
      codeElement.addContent(code);
      rawDoc.getRootElement().addContent(codeElement);
    }    
    
    Element titleElement = new Element("title");
    titleElement.addContent(solrDoc.get("title"));
    rawDoc.getRootElement().addContent(titleElement);
    
    Element conElement = new Element("text");
    conElement.addContent(solrDoc.get("content"));
    rawDoc.getRootElement().addContent(conElement);
    
    XMLOutputter xmlOutput = new XMLOutputter();      
    xmlOutput.setFormat(Format.getPrettyFormat());
       
    return xmlOutput.outputString(rawDoc);
    */
  }
  
  public static String toCheckXml(org.apache.lucene.document.Document checkDoc){
    StringBuffer buffer = new StringBuffer();
    
    buffer.append("<doc>\n");
    
    buffer.append("<id>"+checkDoc.get("id")+"</id>\n");
    buffer.append("<url>"+checkDoc.get("url")+"</url>\n");
    buffer.append("<sourcerss>"+checkDoc.get("sourcerss")+"</sourcerss>\n");
    buffer.append("<host>"+checkDoc.get("host")+"</host>\n");
    buffer.append("<date>"+checkDoc.get("date")+"</date>\n");
    
    String code = checkDoc.get("source-encoding");
    if(null != code){
      buffer.append("<source-encoding>"+code+"</source-encoding>\n");
    }    
    
    buffer.append("<title>"+checkDoc.get("title")+"</title>\n");
    buffer.append(checkDoc.get("text")+"\n");    
    
    buffer.append("</doc>");
    
    return buffer.toString(); 
    /*
    Element docElement = new Element("doc");
    Document rawDoc = new Document(docElement);
    rawDoc.setRootElement(docElement);
    
    Element idElement = new Element("id");
    idElement.addContent(checkDoc.get("id"));
    rawDoc.getRootElement().addContent(idElement);       
    
    Element urlElement = new Element("url");
    urlElement.addContent(checkDoc.get("url"));
    rawDoc.getRootElement().addContent(urlElement);    
    
    Element rssElement = new Element("sourcerss");
    rssElement.addContent(checkDoc.get("sourcerss"));
    rawDoc.getRootElement().addContent(rssElement);
    
    Element hostElement = new Element("host");
    hostElement.addContent(checkDoc.get("host"));
    rawDoc.getRootElement().addContent(hostElement);
    
    Element dateElement = new Element("date");
    dateElement.addContent(checkDoc.get("date"));
    rawDoc.getRootElement().addContent(dateElement);
    
    Element codeElement = new Element("source-encoding");
    String code = checkDoc.get("source-encoding");
    if(null != code){
      codeElement.addContent(code);
      rawDoc.getRootElement().addContent(codeElement);
    }    
    
    Element titleElement = new Element("title");
    titleElement.addContent(checkDoc.get("title"));
    rawDoc.getRootElement().addContent(titleElement);
    
    Element conElement = new Element("text");
    conElement.addContent(checkDoc.get("text"));
    rawDoc.getRootElement().addContent(conElement);
    
    XMLOutputter xmlOutput = new XMLOutputter();   
    xmlOutput.setFormat(Format.getPrettyFormat());
       
    return xmlOutput.outputString(rawDoc);
    */
  }
  
  /**
   * parse files: ..._solr.xml
   * **/
  public static List<TreeMap<String, String>> parseSolrFile(PrintStream logOddFile, String file){
    try {
      List<TreeMap<String, String>> solrdocList = new ArrayList<>();
      
      SAXBuilder saxBuilder = new SAXBuilder();
      //Document xmlDoc = saxBuilder.build(new File(file)); 
      //new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING)
      
      ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(file);
      StringBuffer buffer = new StringBuffer();
      
      for(String line: lineList){
        String lineStriped = stripNonValidXMLCharacters(line);
        buffer.append(lineStriped);
      }
      
      //Document xmlDoc = saxBuilder.build(new InputStreamReader(new FileInputStream(file), "UTF-8"));
      Document xmlDoc = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes("UTF-8"))));
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
      
      //all docs
      return solrdocList;
      
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
      logOddFile.print(file);
      logOddFile.print("\n");
    }

    return null;
    
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
  }
  
  /**
   * parse files: ..._check.xml
   * **/
  public static List<TreeMap<String, String>> parseCheckFile(PrintStream logOddFile, String file){
    List<TreeMap<String, String>> checkdocList = null;
    
    try {
      checkdocList = new ArrayList<>();
      ArrayList<String> rawLineList = IOBox.getLinesAsAList_UTF8(file);    
      
      ArrayList<String> lineList = new ArrayList<>();
      for(String rawLine: rawLineList){
        String line = stripNonValidXMLCharacters(rawLine);
        lineList.add(line);
      }
      
      HashSet<String> tenseStrSet = new HashSet<>();
      
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
        
        /*
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
        */
        
        checkdoc.put("text", text);    
        
        checkdocList.add(checkdoc);           
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
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
      
      logOddFile.print(file);
      logOddFile.print("\n");
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
    TemLoader.parseCheckFile(null, file);
  }
  
}
