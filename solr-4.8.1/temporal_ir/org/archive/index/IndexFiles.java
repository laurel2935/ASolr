package org.archive.index;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.archive.TDirectory;
import org.archive.util.IOBox;
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

/** 
 * build index from files
 */  
public class IndexFiles  
{  
  private static final boolean debug = true;
  ///////////////////////////
  //index ..._solr.xml files
  ///////////////////////////  
  //[source-encoding]
  public static final String [] LPFields = {"url", "title", "sourcerss", "id", "host", "date", "content", "source-encoding"};
  
  /**
   * parse files: ..._solr.xml
   * **/
  private static List<TreeMap<String, String>> parseSolrFile(String file){
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
   * index files: ..._solr.xml
   * **/    
  public static void indexFiles_solr(String dirStr) {
    try {        
      String urlString = "http://localhost:8983/solr/SingleFileTestCore";  
      SolrServer server = new HttpSolrServer(urlString); 
      
      File dirFile = new File(dirStr);
      File [] files = dirFile.listFiles();
      
      for(File f: files){
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        
        List<TreeMap<String, String>> solrdocList = parseSolrFile(f.getAbsolutePath());        
        
        for (TreeMap<String, String> solrdoc: solrdocList) {            
          
          SolrInputDocument doc = new SolrInputDocument();
          doc.addField(LPFields[0], solrdoc.get(LPFields[0]));
          doc.addField(LPFields[1], solrdoc.get(LPFields[1]));
          doc.addField(LPFields[2], solrdoc.get(LPFields[2]));
          doc.addField(LPFields[3], solrdoc.get(LPFields[3]));
          doc.addField(LPFields[4], solrdoc.get(LPFields[4]));
          doc.addField(LPFields[5], solrdoc.get(LPFields[5]));
          doc.addField(LPFields[6], solrdoc.get(LPFields[6]));
          if(solrdoc.containsKey(LPFields[7])){
            doc.addField(LPFields[7], solrdoc.get(LPFields[7]));
          }
           
          docs.add(doc);
        }
        //执行添加
        server.add(docs);
        server.commit();          
      }       
    }catch (MalformedURLException e) {
      e.printStackTrace();
    }catch (SolrServerException e) {
      e.printStackTrace();
    }catch (IOException e) {
      e.printStackTrace();
    }
  }

  
  //////////////////////////////
  //index ..._check.xml files
  //////////////////////////////
  /**
   * parse files: ..._check.xml
   * **/
  private static List<TreeMap<String, String>> parseCheckFile(String file){
    List<TreeMap<String, String>> checkdocList = new ArrayList<>();
    
    ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(file);
    
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
    }   
    
    return checkdocList;
  }

  /**
   * index files: ..._check.xml
   * **/
  public static void indexFiles_check(String dirStr){
     String indexPath = TDirectory.LPFileIndexPath;
     
     try {
       System.out.println("Indexing to directory '" + indexPath + "'...");

           Directory dir = FSDirectory.open(new File(indexPath));          
           Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
           IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
           boolean create = true;
           if(create) {
             // Create a new index in the directory, removing any previously indexed documents:
             iwc.setOpenMode(OpenMode.CREATE);
           }else {
             // Add new documents to an existing index:
             iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
           }
           IndexWriter indexWriter = new IndexWriter(dir, iwc);
                   
           Date start = new Date();
           
         File dirFile = new File(dirStr);
         File [] files = dirFile.listFiles();        
         for(File f: files){
           List<org.apache.lucene.document.Document> docs = new ArrayList<org.apache.lucene.document.Document>();
           List<TreeMap<String, String>> checkdocList = parseCheckFile(f.getAbsolutePath()); 
           
           for(TreeMap<String, String> checkdoc: checkdocList){
             // make a new, empty document
             org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
                 
                 Field idField = new StringField("id", checkdoc.get("id"), Field.Store.YES);
               doc.add(idField);
             for(Entry<String, String> entry: checkdoc.entrySet()){
                if(!entry.getKey().equals("id")){
                  StoredField storeField = new StoredField(entry.getKey(), entry.getValue());
                  doc.add(storeField);
                }
             }
             
             docs.add(doc);
           }
           
           for(org.apache.lucene.document.Document doc: docs){             
                 indexWriter.addDocument(doc);
           } 
         }
         
         indexWriter.close();
         Date end = new Date();
       System.out.println(end.getTime() - start.getTime() + " total milliseconds");
     } catch (Exception e) {
       // TODO: handle exception
       e.printStackTrace();
     }
  }
  
    
  //////////////////////////////
    
  public static void main(String[] args){  
      //1
      //String d = "H:/v-haiyu/TaskPreparation/Temporalia/tool/test/";
      //CreateIndexFromFile.indexFiles(d);
          
  }
  
}  
