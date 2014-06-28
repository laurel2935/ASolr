package org.archive.l2r;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import org.archive.IDirectory;
import org.archive.TDirectory;
import org.archive.data.TemLoader;
import org.archive.data.TemLoader.TemRunType;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic.SubtopicType;
import org.archive.search.IndexSearch;
import org.archive.search.ResultSlot;
import org.archive.util.IOBox;
import org.archive.util.StrStrInt;
import org.apache.lucene.document.Document;

import ciir.umass.edu.eval.Evaluator;

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
  
  /**
   * 
   * **/
  public static void generateTrainFile(ArrayList<TemQuery> temQueryList, TreeMap<String,ArrayList<String>> relMap, SubtopicType subtopicType) throws Exception {
    String tFile = TDirectory.ROOT_OUTPUT+"train_"+subtopicType.toString()+".txt";
    
    int slotNumber = 50;
    
    BufferedWriter tWriter = IOBox.getBufferedWriter_UTF8(tFile);
    
    //System.out.println(temQueryList.size());
    int count = 1;
    for(TemQuery temQuery: temQueryList){
      System.out.println((count++)+"\t"+temQuery.getTitle());
      
      /*
      if(count>3){
        break;
      }
      */
      
      //labeled rels
      String subtopicID = temQuery.getTemSubtopic(subtopicType).getSubtopicID();
      ArrayList<String> relList = relMap.get(subtopicID);
      
      //set of relevant docid
      HashSet<String> relSet = new HashSet<>();
      relSet.addAll(relList);
      
      //initial retrieval run
      ArrayList<ResultSlot> slotList = IndexSearch.initialLuceneSearch(temQuery.getSearchQuery(subtopicType), slotNumber);
      
      for(ResultSlot slot: slotList){
        
        StringBuffer buffer = new StringBuffer();
        
        //head
        if(relSet.contains(slot._docid)){
          buffer.append(1);          
        }else{
          buffer.append(0);
        }
        buffer.append("\t");
        
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
        buffer.append("\t#docid="+slot._docid);
        
        //output
        tWriter.write(buffer.toString().trim());
        tWriter.newLine();        
      }      
    }
    
    tWriter.flush();
    tWriter.close();    
  } 
  
  /**
   * 
   * **/
  public static void generateTrainFile(TemRunType temRunType, SubtopicType subtopicType) throws Exception{
    ArrayList<TemQuery> temQueryList = TemLoader.loadTemporalQuery(temRunType);
    
    TreeMap<String,ArrayList<String>> relMap = TemLoader.loadTemporalRels();
    
    generateTrainFile(temQueryList, relMap, subtopicType);
  }
  
  
  /**
   * 
   * **/
  public static void train(){
    String output_train = TDirectory.ROOT_OUTPUT + "l2r/train/";
    
    String aBlank = " ";
    String argStr = "";
    //1
    /*
    //java -jar bin/RankLib.jar -load mymodel.txt -rank MQ2008/Fold1/test.txt -score myscorefile.txt
        
    argStr += ("-load"+aBlank);
    argStr += (IDirectory.ROOT_OUTPUT+"mymodel.txt"+aBlank);
    
    argStr += ("-rank"+aBlank);
    argStr += (IDirectory.ROOT_DATASET+"MQ2008/Fold1/test.txt"+aBlank);
    
    argStr += ("-score"+aBlank);
    argStr += (IDirectory.ROOT_OUTPUT+"myscorefile.txt");   
    
    //
    Evaluator.runTest(argStr);
    */
    //2
    //java -cp bin/RankLib.jar ciir.umass.edu.features.FeatureManager -input MQ2008/Fold1/train.txt -output mydata/ -shuffle
    
    //java -jar bin/RankLib.jar -train MQ2008/Fold1/train.txt -ranker 4 -kcv 5 -kcvmd models/ -kcvmn ca -metric2t NDCG@10 -metric2T ERR@10
    argStr += ("-train"+aBlank);
    argStr += (TDirectory.ROOT_OUTPUT+"train_Atemporal.txt"+aBlank);
    
    argStr += ("-ranker"+aBlank);
    argStr += ("4"+aBlank);
    
    argStr += ("-kcv"+aBlank);
    argStr += ("5"+aBlank);
    
    argStr += ("-kcvmd"+aBlank);
    argStr += (TDirectory.ROOT_OUTPUT+"models/"+aBlank);
    
    argStr += ("-kcvmn"+aBlank);
    argStr += ("ca"+aBlank);
    
    argStr += ("-metric2t"+aBlank);
    argStr += ("NDCG@10"+aBlank);
    
    argStr += ("-metric2T"+aBlank);
    argStr += ("ERR@10");
    
    //
    Evaluator.runTest(argStr);
  }
  
  
  //////////////////
  //main
  //////////////////
  public static void main(String []args){
    ////1
    ///*
    try {
      TemTrain.generateTrainFile(TemRunType.DryRun, SubtopicType.atemporal);
      
      TemTrain.generateTrainFile(TemRunType.DryRun, SubtopicType.future);
      
      TemTrain.generateTrainFile(TemRunType.DryRun, SubtopicType.past);
      
      TemTrain.generateTrainFile(TemRunType.DryRun, SubtopicType.recency);
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    //*/
    
    //2
    //TemTrain.train();
    
    
  }
}
