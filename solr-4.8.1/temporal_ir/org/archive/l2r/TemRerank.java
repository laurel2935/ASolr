package org.archive.l2r;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.Version;
import org.archive.TDirectory;
import org.archive.dataset.TemLoader;
import org.archive.dataset.TemLoader.TemRunType;
import org.archive.dataset.query.TemQuery;
import org.archive.dataset.query.TemSubtopic.SubtopicType;
import org.archive.l2r.TemTrain.TemModelType;
import org.archive.nlp.ner.StanfordNER;
import org.archive.search.IndexSearch;
import org.archive.search.IndexSearch.SimType;
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
  public static final boolean debug = true;
  
  private static final String GroupID = "TUTA1";
  /****/
  public static void temRerank(SimType simType, TemModelType modelType, int runOrder) throws Exception{
    String runid = "TUTA1-TIR-RUN-"+Integer.toString(runOrder);
    
    String runFile = TDirectory.ROOT_OUTPUT+"FinalRuns/"+runid+".txt";
    
    BufferedWriter runWriter = IOBox.getBufferedWriter_UTF8(runFile);
    runWriter.write("id\trank\tdoc_id\tgroup_id\trun_id");
    runWriter.newLine();
    
    //formal run queries
    ArrayList<TemQuery> temQueryList = TemLoader.loadTemporalQuery(TemRunType.FormalRun);
    
    int count = 1;
    for(TemQuery temQuery: temQueryList){
      System.out.println((count++)+"\t"+temQuery.getTitle());
      
      //1
      String r1 = temRerank(simType, modelType, runid, temQuery, SubtopicType.atemporal);
      runWriter.write(r1);
      runWriter.newLine();
      
      if(debug){
        System.out.println("atemporal");
      }      
      
      //2
      String r2 = temRerank(simType, modelType, runid, temQuery, SubtopicType.future);
      runWriter.write(r2);
      runWriter.newLine();
      
      if(debug){
        System.out.println("future");
      }      
      
      //3
      String r3 = temRerank(simType, modelType, runid, temQuery, SubtopicType.past);
      runWriter.write(r3);
      runWriter.newLine();
      
      if(debug){
        System.out.println("past");
      }      
      
      //4
      String r4 = temRerank(simType, modelType, runid, temQuery, SubtopicType.recency);
      runWriter.write(r4);
      runWriter.newLine();
      
      if(debug){
        System.out.println("recency");
      }
      
    }
    
    runWriter.flush();
    runWriter.close();    
  }
  /**
   * 
   * **/
  public static String temRerank(SimType simType, TemModelType modelType, String runid, TemQuery temQuery, SubtopicType subtopicType) throws Exception{
    int slotNumber = 100;
    
    //initial retrieval run
    ArrayList<ResultSlot> slotList = IndexSearch.initialLuceneSearch(simType, temQuery.getSearchQuery(subtopicType), slotNumber);
    
    ArrayList<String> rankedList = rerank(simType, modelType, temQuery, subtopicType, slotList);
    
    StringBuffer buffer = new StringBuffer();
    
    String id = temQuery.getTemSubtopic(subtopicType).getSubtopicID();
    
    for(int i=0; i<rankedList.size(); i++){
      String docid = rankedList.get(i);
      
      buffer.append(id+"\t"+(i+1)+"\t"+docid+"\t"+GroupID+"\t"+runid+"\n");
    }
    
    return buffer.toString().trim();
  }
  /**
   * personalized evaluator, that output ranked docid, given the pre-trained model
   * **/
  private static Evaluator evaluator = new Evaluator(RANKER_TYPE.LAMBDAMART, "NDCG@10", "ERR@10");
  
  public static ArrayList<String> rerank(SimType simType, TemModelType modelType, TemQuery temQuery, SubtopicType subtopicType, ArrayList<ResultSlot> slotList) throws Exception{
    
    StringBuffer buffer = new StringBuffer();
    
    for(ResultSlot slot: slotList){    
      Document lpDoc = IndexSearch.fetchLPFile(slot._docid);      
      ArrayList<StrStrInt> tripleList = TemLoader.generateSentenceTriple(lpDoc.get("text"));
      
      if(null==tripleList || tripleList.size()==0){
        System.out.println("Null triplelist -!!!!!!!!!!!!!!!!!- "+slot._docid);
        continue;
      }      
      
      //head            
      //all zero for reranking
      buffer.append(0);
      buffer.append("\t");
      
      buffer.append("qid:"+temQuery.getID());
      buffer.append("\t");
      
      //middle            
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
    String modelFile = null;
    if(modelType == TemModelType.Entire){
      modelFile = TDirectory.ROOT_OUTPUT+"FinalModels/"+simType.toString()+"_entire.model";
    }else{
      if(subtopicType == SubtopicType.atemporal){
        modelFile = TDirectory.ROOT_OUTPUT+"FinalModels/"+simType.toString()+"_per_"+SubtopicType.atemporal.toString()+".model";
      }else if(subtopicType == SubtopicType.future){
        modelFile = TDirectory.ROOT_OUTPUT+"FinalModels/"+simType.toString()+"_per_"+SubtopicType.future.toString()+".model";
      }else if(subtopicType == SubtopicType.recency){
        modelFile = TDirectory.ROOT_OUTPUT+"FinalModels/"+simType.toString()+"_per_"+SubtopicType.recency.toString()+".model";
      }else if(subtopicType == SubtopicType.past){
        modelFile = TDirectory.ROOT_OUTPUT+"FinalModels/"+simType.toString()+"_per_"+SubtopicType.past.toString()+".model";
      }else{
        System.err.println("SubtopicType Error!");
        System.exit(1);
      }      
    }
    
    ArrayList<StrStrDouble> docList = evaluator.score(modelType.ordinal(), subtopicType.ordinal(), modelFile, buffer.toString().trim());
    
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
    /*
    ArrayList<StrStrDouble> docList = evaluator.score("H:/v-haiyu/CodeBench/Pool_Output/Output_Ranklib/mymodel.txt", buffer.toString().trim());
    for(StrStrDouble doc: docList){
      System.out.println(doc.toString());
    }
    */
  }
  
  //pre-check
  public static void precheck() throws Exception{
  //formal run queries
    ArrayList<TemQuery> temQueryList = TemLoader.loadTemporalQuery(TemRunType.FormalRun);
    
    StandardAnalyzer solrAnalyzer = new StandardAnalyzer(Version.LUCENE_48);
    //solrParser = new QueryParser(Version.LUCENE_48, field, solrAnalyzer);
    QueryParser solrParser = new MultiFieldQueryParser(Version.LUCENE_48, new String[] {"title", "content"}, solrAnalyzer);
    
    int count= 1;
    for(TemQuery temQuery: temQueryList){
      System.out.println((count++)+temQuery.getTitle());
      //
      StanfordNER.suitParsing(temQuery.getSearchQuery(SubtopicType.atemporal));
      solrParser.parse(temQuery.getSearchQuery(SubtopicType.atemporal));
      
      StanfordNER.suitParsing(temQuery.getSearchQuery(SubtopicType.future));
      solrParser.parse(temQuery.getSearchQuery(SubtopicType.future));
      
      StanfordNER.suitParsing(temQuery.getSearchQuery(SubtopicType.past));
      solrParser.parse(temQuery.getSearchQuery(SubtopicType.past));
      
      StanfordNER.suitParsing(temQuery.getSearchQuery(SubtopicType.recency));
      solrParser.parse(temQuery.getSearchQuery(SubtopicType.recency));
      
    }
  }
  
  //result-check
  public static void resultCheck(String dir){
    try {
      File dirFile = new File(dir);
      File [] runFiles = dirFile.listFiles();
      
      for(File run: runFiles){
        ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(run.getAbsolutePath());
        
        int count = 0;
        for(int k=1; k<lineList.size(); k++){
          String line = lineList.get(k);
          
          if(line.length() > 0){
            count ++;
            String [] fieldArray = line.split("\t");
            String docid = fieldArray[2];
            
            Document lpDocument = IndexSearch.fetchLPFile(docid);
            if(null==lpDocument){
              System.out.println(run.getAbsolutePath());
              System.err.println("Null for:\t"+line);
            }else if(!lpDocument.get("id").equals(docid)){
              System.out.println(run.getAbsolutePath());
              System.out.println("Zero size for:\t"+line);
            }            
          }
        }
        System.out.println(count);
      }      
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
  }
  
  /////////////////////
  //main
  /////////////////////
  public static void main(String []args){
    //1
    //TemRerank.test();
    
    //2
    /*
    try {
      TemRerank.precheck();
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    */
    
    //3
    /*    
    //!!! formal run  !!!
    try {
      //run-1: LM, Entire 
      //run-2: LM, Per
      //run-3: TFIDF, Entire
      //TemRerank.temRerank(SimType.LM, TemModelType.Entire, 1);    
      //TemRerank.temRerank(SimType.LM, TemModelType.Per, 2); 
      TemRerank.temRerank(SimType.TFIDF, TemModelType.Entire, 3); 
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    */ 
    
    //4
    TemRerank.resultCheck(TDirectory.ROOT_OUTPUT+"FinalRuns/");
    /*
    try {
      BufferedWriter writer = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"line.txt");
      writer.write("id\trank\tdoc_id\tgroup_id\trun_id");
      writer.flush();
      writer.close();
    } catch (Exception e) {
      // TODO: handle exception
    }
    */
    
  }
  
}
