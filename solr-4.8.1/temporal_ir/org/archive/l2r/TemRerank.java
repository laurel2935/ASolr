package org.archive.l2r;

import java.io.BufferedWriter;
import java.util.ArrayList;

import org.apache.lucene.document.Document;
import org.archive.data.TemLoader;
import org.archive.data.TemLoader.TemRunType;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic.SubtopicType;
import org.archive.search.IndexSearch;
import org.archive.search.ResultSlot;
import org.archive.util.IOBox;
import org.archive.util.StrStrDouble;
import org.archive.util.StrStrInt;

import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.learning.RANKER_TYPE;

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

public class TemRerank {
  
  private static final String GroupID = "TUTA1";
  /****/
  public static void temRerank(String runFile, String runid) throws Exception{
    BufferedWriter runWriter = IOBox.getBufferedWriter_UTF8(runFile);
    
    //formal run queries
    ArrayList<TemQuery> temQueryList = TemLoader.loadTemporalQuery(TemRunType.FormalRun);
    
    for(TemQuery temQuery: temQueryList){
      //1
      String r1 = temRerank(runid, temQuery, SubtopicType.atemporal);
      runWriter.write(r1);
      runWriter.newLine();
      
      //2
      String r2 = temRerank(runid, temQuery, SubtopicType.future);
      runWriter.write(r2);
      runWriter.newLine();
      
      //3
      String r3 = temRerank(runid, temQuery, SubtopicType.past);
      runWriter.write(r3);
      runWriter.newLine();
      
      //4
      String r4 = temRerank(runid, temQuery, SubtopicType.recency);
      runWriter.write(r4);      
    }
    
    runWriter.flush();
    runWriter.close();    
  }
  /**
   * 
   * **/
  public static String temRerank(String runid, TemQuery temQuery, SubtopicType subtopicType) throws Exception{
    int slotNumber = 100;
    
    //initial retrieval run
    ArrayList<ResultSlot> slotList = IndexSearch.initialLuceneSearch(temQuery.getSearchQuery(subtopicType), slotNumber);
    
    ArrayList<String> rankedList = rerank(temQuery, subtopicType, slotList);
    
    StringBuffer buffer = new StringBuffer();
    
    String id = temQuery.getTemSubtopic(subtopicType).getSubtopicID();
    
    for(int i=0; i<rankedList.size(); i++){
      String docid = rankedList.get(i);
      
      buffer.append(id+"\t"+(i+1)+"\t"+docid+"\t"+GroupID+"\t"+runid+"\n");
    }
    
    return buffer.toString().trim();
  }
  /****/
  private static Evaluator evaluator = new Evaluator(RANKER_TYPE.LAMBDAMART, "NDCG@10", "ERR@10");
  
  public static ArrayList<String> rerank(TemQuery temQuery, SubtopicType subtopicType, ArrayList<ResultSlot> slotList) throws Exception{
    
    StringBuffer buffer = new StringBuffer();
    
    for(ResultSlot slot: slotList){      
      //head
      /*
      if(relSet.contains(slot._docid)){
        buffer.append(1);          
      }else{
        buffer.append(0);
      }
      buffer.append("\t");
      */
      
      buffer.append("qid:"+temQuery.getID());
      buffer.append("\t");
      
      //middle
      Document lpDoc = IndexSearch.fetchLPFile(slot._docid);
      
      ArrayList<StrStrInt> tripleList = TemLoader.generateSentenceTriple(lpDoc.get("text"));
      
      TemFeatureVector temFeatureVector = FeatureParser.docFeatures(temQuery, subtopicType, tripleList, lpDoc);
      
      buffer.append(temFeatureVector.toString());
      buffer.append("\t");
      
      //tail
      int r = temFeatureVector.size()+1;
      buffer.append(r);
      buffer.append(":");
      buffer.append(slot._score);
      buffer.append("\t");
      
      //descriptioin
      buffer.append("\t#docid="+slot._docid+"\n");
    }    

    //???
    ArrayList<StrStrDouble> docList = evaluator.score("", buffer.toString().trim());
    
    ArrayList<String> rerankedDocList = new ArrayList<>();
    
    for(StrStrDouble doc: docList){
      rerankedDocList.add(doc.second);
    }
    
    return rerankedDocList;  
  }
  
  //score test
  //results demonstrate the consistence !
  public static void test(){
    ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8("H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_Ranklib/MQ2008/Fold1/test.txt");
    StringBuffer buffer = new StringBuffer();
    for(String line: lineList){
      if(line.length() > 0){
        buffer.append(line.trim()+"\n");
      }
    }
    //
    ArrayList<StrStrDouble> docList = evaluator.score("H:/v-haiyu/CodeBench/Pool_Output/Output_Ranklib/mymodel.txt", buffer.toString().trim());
    for(StrStrDouble doc: docList){
      System.out.println(doc.toString());
    }
  }
  
  /////////////////////
  //main
  /////////////////////
  public static void main(String []args){
    //1
    TemRerank.test();
  }
  
}
