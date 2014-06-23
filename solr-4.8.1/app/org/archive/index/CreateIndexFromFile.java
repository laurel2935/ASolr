package org.archive.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
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

/** 
 * build index from file
 */  
public class CreateIndexFromFile  
{  
    //pdf
    /*
    String fileName = "e:/MyBatis3用户指南中文版.pdf";   
    String solrId = "MyBatis3用户指南中文版.pdf";   
    try  
    {  
        //indexFilesSolrCell(fileName, solrId);  
    }  
    catch (IOException e)  
    {  
        e.printStackTrace();  
    }  
    catch (SolrServerException e)  
    {  
        e.printStackTrace();  
    }
    */
    /*
    public static void indexFilesSolrCell(String fileName, String solrId) throws IOException, SolrServerException  
    {  
        String urlString = "http://localhost:8983/solr/SingleFileTestCore";  
        SolrServer solr = new HttpSolrServer(urlString);  
        ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");  
          
        String contentType="application/pdf";  
        up.addFile(new File(fileName), contentType);  
        up.setParam("literal.id", solrId);  
        up.setParam("uprefix", "attr_");  
        up.setParam("fmap.content", "attr_content");  
        up.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);  
          
        solr.request(up);  
          
        QueryResponse rsp = solr.query(new SolrQuery("*:*"));  
        System.out.println(rsp);  
    } 
    */
  
  //[source-encoding]
  public static final String [] LPFields = {"url", "title", "sourcerss", "id", "host", "date", "content", "source-encoding"};
  
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
    
    //doc    
    public static void indexFiles(String dirStr) {
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
    
    public static void main(String[] args){  
      //1
      String d = "H:/v-haiyu/TaskPreparation/Temporalia/tool/test/";
      CreateIndexFromFile.indexFiles(d);
          
    }
    
}  
