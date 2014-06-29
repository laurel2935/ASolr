package org.archive.l2r;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeMap;

import org.archive.IDirectory;
import org.archive.TDirectory;
import org.archive.data.TemLoader;
import org.archive.data.TemLoader.TemRunType;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic.SubtopicType;
import org.archive.search.IndexSearch;
import org.archive.search.IndexSearch.SimType;
import org.archive.search.ResultSlot;
import org.archive.util.IOBox;
import org.archive.util.StrStrInt;
import org.apache.lucene.document.Document;

import ciir.umass.edu.eval.Evaluator;
import ciir.umass.edu.learning.RANKER_TYPE;
import ciir.umass.edu.utilities.MyThreadPool;

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
  public static enum TemMOdelType{Entire, Per};
  
  /**
   * 
   * **/
  public static void generateTrainFile(SimType simType, ArrayList<TemQuery> temQueryList, TreeMap<String,ArrayList<String>> relMap, SubtopicType subtopicType) throws Exception {
    String tFile = TDirectory.ROOT_OUTPUT+"FinalTrainFiles/"+simType.toString()+"_train_"+subtopicType.toString()+".txt";
    
    int slotNumber = 50;
    
    BufferedWriter tWriter = IOBox.getBufferedWriter_UTF8(tFile);
    
    //System.out.println(temQueryList.size());
    int count = 1;
    for(TemQuery temQuery: temQueryList){
      System.out.println((count++)+"\t"+temQuery.getTitle());
      
      /*
      //for test only
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
      ArrayList<ResultSlot> slotList = IndexSearch.initialLuceneSearch(simType, temQuery.getSearchQuery(subtopicType), slotNumber);
      
      for(ResultSlot rSlot: slotList){
        if(relSet.contains(rSlot._docid)){
          rSlot.setGrTruthRelScore(1.0f);          
        }else{
          rSlot.setGrTruthRelScore(0.0f);
        }        
      }
      Collections.sort(slotList);
      
      for(ResultSlot slot: slotList){        
        StringBuffer buffer = new StringBuffer();
        
        //head        
        buffer.append(Float.toString(slot._grTruthRelScore));
        buffer.append("\t");
        
        buffer.append("qid:"+temQuery.getID());
        buffer.append("\t");
        
        //middle
        Document lpDoc = IndexSearch.fetchLPFile(slot._docid);
        
        ArrayList<StrStrInt> tripleList = TemLoader.generateSentenceTriple(lpDoc.get("text"));
        
        if(null==tripleList || 0==tripleList.size()){
          continue;
        }
        
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
  public static void generateTrainFile(SimType simType, TemRunType temRunType, SubtopicType subtopicType) throws Exception{
    ArrayList<TemQuery> temQueryList = TemLoader.loadTemporalQuery(temRunType);
    
    TreeMap<String,ArrayList<String>> relMap = TemLoader.loadTemporalRels();
    
    generateTrainFile(simType, temQueryList, relMap, subtopicType);
  }
  
  /**
   * 
   * **/
  private static boolean evaIni = false;
  private static Evaluator evaluator;  
  //
  private static void generateEntireTrainingFile(SimType simType, String dir){
    File dirFile = new File(dir);
    File [] files = dirFile.listFiles();
    
    ArrayList<String> entireList = new ArrayList<>();
    
    for(File f: files){
      ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(f.getAbsolutePath());
      entireList.addAll(lineList);
    }
    
    String file = dir+simType.toString()+"_entire.txt";
    try {
      BufferedWriter writer = IOBox.getBufferedWriter_UTF8(file);
      for(String line: entireList){
        writer.write(line);
        writer.newLine();
      }
      writer.flush();
      writer.close();
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }    
  }
  /**
   * get models
   * **/
  public static void train(SimType simType, RANKER_TYPE rankerType, TemMOdelType temTrainType, String dir){
    if(!evaIni){
      evaluator = new Evaluator(rankerType, "NDCG@10", "ERR@10");
      evaIni = true;
    }
    
    String modelPrefix = simType.toString()+"_";
    
    if(temTrainType == TemMOdelType.Entire){
      File enFile = new File(dir+simType.toString()+"_entire.txt");
      if(!enFile.exists()){
        generateEntireTrainingFile(simType, dir);
      }
      
      evaluator.evaluate_tem(enFile.getAbsolutePath(), "", "", 1.0, TDirectory.ROOT_OUTPUT+"FinalModels/"+modelPrefix+"entire.model"); 
    }else{
      String trainPrefix = simType.toString()+"_train_";
      
      //
      String aFile = dir+trainPrefix+SubtopicType.atemporal.toString()+".txt";
      evaluator.evaluate_tem(aFile, "", "", 1.0, TDirectory.ROOT_OUTPUT+"FinalModels/"+modelPrefix+"per_"+SubtopicType.atemporal.toString()+".model");      
      //
      String pFile = dir+trainPrefix+SubtopicType.past.toString()+".txt";
      evaluator.evaluate_tem(pFile, "", "", 1.0, TDirectory.ROOT_OUTPUT+"FinalModels/"+modelPrefix+"per_"+SubtopicType.past.toString()+".model"); 
      //
      String rFile = dir+trainPrefix+SubtopicType.recency.toString()+".txt";
      evaluator.evaluate_tem(rFile, "", "", 1.0, TDirectory.ROOT_OUTPUT+"FinalModels/"+modelPrefix+"per_"+SubtopicType.recency.toString()+".model"); 
      //
      String fFile = dir+trainPrefix+SubtopicType.future.toString()+".txt";
      evaluator.evaluate_tem(fFile, "", "", 1.0, TDirectory.ROOT_OUTPUT+"FinalModels/"+modelPrefix+"per_"+SubtopicType.future.toString()+".model"); 
    }
    
    MyThreadPool.getInstance().shutdown();
  }
  
  /**
   * test
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
      //SimType.TFIDF 
      TemTrain.generateTrainFile(SimType.TFIDF, TemRunType.DryRun, SubtopicType.atemporal);      
      TemTrain.generateTrainFile(SimType.TFIDF, TemRunType.DryRun, SubtopicType.future);      
      TemTrain.generateTrainFile(SimType.TFIDF, TemRunType.DryRun, SubtopicType.past);      
      TemTrain.generateTrainFile(SimType.TFIDF, TemRunType.DryRun, SubtopicType.recency);
      
      //
      TemTrain.generateTrainFile(SimType.LM, TemRunType.DryRun, SubtopicType.atemporal);      
      TemTrain.generateTrainFile(SimType.LM, TemRunType.DryRun, SubtopicType.future);      
      TemTrain.generateTrainFile(SimType.LM, TemRunType.DryRun, SubtopicType.past);      
      TemTrain.generateTrainFile(SimType.LM, TemRunType.DryRun, SubtopicType.recency);      
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    //*/
    
    //2 for test use only
    /*
    TemTrain.train();
    */
    
    //3 get each models
    TemTrain.train(SimType.LM, RANKER_TYPE.LAMBDAMART, TemMOdelType.Entire, TDirectory.ROOT_OUTPUT+"FinalTrainFiles/");
  }
}
