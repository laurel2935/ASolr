package org.archive.l2r;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import org.archive.TDirectory;
import org.archive.data.TemLoader;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic.SubtopicType;
import org.archive.search.IndexSearch;
import org.archive.search.ResultSlot;
import org.archive.util.IOBox;
import org.archive.util.StrStrInt;
import org.apache.lucene.document.Document;

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

public class TemTrain {
  
  public static void generateTrainFile(ArrayList<TemQuery> temQueryList, TreeMap<String,ArrayList<String>> relMap, SubtopicType subtopicType) throws Exception {
    String tFile = TDirectory.ROOT_OUTPUT+"train_"+subtopicType.toString()+".txt";
    
    int slotNumber = 50;
    String field = "content";
    
    BufferedWriter tWriter = IOBox.getBufferedWriter_UTF8(tFile);
    
    for(TemQuery temQuery: temQueryList){
      //labeled rels
      String subtopicID = temQuery.getTemSubtopic(subtopicType).getSubtopicID();
      ArrayList<String> relList = relMap.get(subtopicID);
      HashSet<String> relSet = new HashSet<>();
      relSet.addAll(relList);
      
      //initial retrieval run
      ArrayList<ResultSlot> slotList = IndexSearch.initialLuceneSearch(temQuery.getSearchQuery(subtopicType), slotNumber, field);
      
      for(ResultSlot slot: slotList){
        
        StringBuffer buffer = new StringBuffer();
        
        //head
        if(relSet.contains(slot._docid)){
          buffer.append(1);          
        }else{
          buffer.append(0);
        }
        buffer.append("\t");
        buffer.append(slot._docid);
        buffer.append("\t");
        
        //middle
        Document lpDoc = IndexSearch.fetchLPFile(slot._docid);
        
        ArrayList<StrStrInt> tripleList = TemLoader.generateSentenceTriple(lpDoc.get("text"));
        
        TemFeatureVector temFeatureVector = FeatureParser.docFeatures(tWriter, temQuery, subtopicType, tripleList, lpDoc);
        
        buffer.append(temFeatureVector.toString());
        buffer.append("\t");
        
        //tail
        int r = temFeatureVector.size()+1;
        buffer.append(r);
        buffer.append(":");
        buffer.append(slot._score);
        
        //output
        tWriter.write(buffer.toString().trim());
        tWriter.newLine();        
      }      
    }
    
    tWriter.flush();
    tWriter.close();    
  } 
  
  //
  public static void generateTrainFile(SubtopicType subtopicType) throws Exception{
    ArrayList<TemQuery> temQueryList = TemLoader.loadTemporalQuery();
    
    TreeMap<String,ArrayList<String>> relMap = TemLoader.loadTemporalRels();
    
    generateTrainFile(temQueryList, relMap, subtopicType);
  }
  
  
  //////////////////
  public static void main(String []args){
    //1
    try {
      TemTrain.generateTrainFile(SubtopicType.Atemporal);
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    
  }
}
