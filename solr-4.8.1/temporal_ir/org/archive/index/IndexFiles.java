package org.archive.index;

import java.io.File;
import java.io.IOException;
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
import org.archive.data.TemLoader;

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
        
        List<TreeMap<String, String>> solrdocList = TemLoader.parseSolrFile(f.getAbsolutePath());        
        
        for (TreeMap<String, String> solrdoc: solrdocList) {            
          
          SolrInputDocument doc = new SolrInputDocument();
          doc.addField(TemLoader.LPFields[0], solrdoc.get(TemLoader.LPFields[0]));
          doc.addField(TemLoader.LPFields[1], solrdoc.get(TemLoader.LPFields[1]));
          doc.addField(TemLoader.LPFields[2], solrdoc.get(TemLoader.LPFields[2]));
          doc.addField(TemLoader.LPFields[3], solrdoc.get(TemLoader.LPFields[3]));
          doc.addField(TemLoader.LPFields[4], solrdoc.get(TemLoader.LPFields[4]));
          doc.addField(TemLoader.LPFields[5], solrdoc.get(TemLoader.LPFields[5]));
          doc.addField(TemLoader.LPFields[6], solrdoc.get(TemLoader.LPFields[6]));
          if(solrdoc.containsKey(TemLoader.LPFields[7])){
            doc.addField(TemLoader.LPFields[7], solrdoc.get(TemLoader.LPFields[7]));
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
           List<TreeMap<String, String>> checkdocList = TemLoader.parseCheckFile(f.getAbsolutePath()); 
           
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
