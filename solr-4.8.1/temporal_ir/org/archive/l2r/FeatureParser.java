package org.archive.l2r;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.archive.TDirectory;
import org.archive.data.query.TemQuery;
import org.archive.data.query.TemSubtopic;
import org.archive.data.query.TemSubtopic.SubtopicType;
import org.archive.nlp.ner.StanfordNER;
import org.archive.util.StrStrInt;
import org.archive.util.StrStrStr;
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

public class FeatureParser {
  public static final boolean debug = false;  
  
  ///////////////////////
  //tense part
  ///////////////////////
  
  //tense group: past [VBD]  | present & future [VBP, VBZ, MD]
  //as VBN and VBG are combined with others, they are not considered
  //in: include queried terms, out: without queried terms
  public static enum TenseGroup{inPast, inPF, outPast, outPF};
  
  //
  public static boolean isPastTenseTag(String tenseTag){
    if(tenseTag.equals("VBD")){
      return true;
    }else{
      return false;
    }
  } 
  //
  public static boolean acceptedTenseTag(String tenseTag){
    if(tenseTag.equals("VBD") 
        || tenseTag.equals("VBP")
        || tenseTag.equals("VBZ")
        || tenseTag.equals("MD")){
      
      return true;      
    }else{
      return false;
    }
  }
  //
  public static HashSet<String> getSubtopicTense(TemSubtopic temSubtopic){
    HashSet<String> subTenSet = new HashSet<>();
    
    ArrayList<ArrayList<StrStrStr>> subAnnotationList = StanfordNER.suitParsing(temSubtopic.getSubtopicTitle());
    for(ArrayList<StrStrStr> subAnnotation: subAnnotationList){
      String tenseStr = StanfordNER.getTenseStr(subAnnotation);
      
      String [] tenseTagArray = tenseStr.split("\\s");
      
      for(int i=0; i<tenseTagArray.length; i++){
        if(acceptedTenseTag(tenseTagArray[i])){
          subTenSet.add(tenseTagArray[i]);
        }
      }
    }
    
    return subTenSet;    
  }
  
  //////////////////////////
  //queried terms
  //////////////////////////
  
  private static boolean includeQueriedTerm(HashSet<String> sentenceTSet, HashSet<String> searchQTSet){
    for(String searchQTerm: searchQTSet){
      if(sentenceTSet.contains(searchQTerm)){
        return true;
      }
    }
    return false;
  }
  
  
  ////////////////////////
  //temporal expressions parsing
  ////////////////////////
  
  //for temporal expression in ..._Check.xml file
  //e.g., 20110529
  private static final DateFormat TimeFormat_YMD = new SimpleDateFormat("yyyyMMdd");
  private static final DateFormat TimeFormat_YM = new SimpleDateFormat("yyyyMM");
  private static final DateFormat TimeFormat_Y = new SimpleDateFormat("yyyy");
  //e.g., 2011-06-04
  private static final DateFormat TimeFormat_DocIssue = new SimpleDateFormat("yyyy-MM-dd");
  //e.g., Feb 28, 2013 GMT+0:00
  private static final DateFormat TimeFormat_QueryIssue = new SimpleDateFormat("MMM dd, yyyy");
  
  private static void test(){
    //20110529
    DateFormat TimeFormat_Check = new SimpleDateFormat("yyyyMMdd");
    //e.g., 2011-06-04
    DateFormat TimeFormat_DocIssue = new SimpleDateFormat("yyyy-MM-dd");
    //e.g., Feb 28, 2013 GMT+0:00
    //DateFormat TimeFormat_QueryIssue = new SimpleDateFormat("yyyy-MM-dd");
    try {
      
      //1
      String t1 = "20110529";      
      System.out.println(TimeFormat_Check.parse(t1).toString());
      
      //2
      String t2 = "2011-06-04";
      System.out.println(TimeFormat_DocIssue.parse(t2).toString());
      
      //3
      String t3 = "Feb 28, 2013 GMT+0:00";
      System.out.println(TimeFormat_QueryIssue.parse(t3).toString());
      
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }    
  }
  
  //variance w.r.t. year, month, day
  //  
  private static double yearVariance(ArrayList<String> yExpList){
    if(null==yExpList || yExpList.size()<=1){
      return 0;
    }
    
    double v = 0.0;
    try {
      ArrayList<Double> yList = new ArrayList<Double>();
      
      for(String yearStr: yExpList){
        Date yDate = TimeFormat_Y.parse(yearStr);
        yList.add(yDate.getYear()*1.0);
      }
      
      v = getVariance(yList);      
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    return v;
  }
  //
  private static double monthVariance(ArrayList<String> ymExpList){
    if(null==ymExpList || ymExpList.size()<=1){
      return 0;
    }
    
    double v = 0.0;
    try {
      ArrayList<Double> ymList = new ArrayList<>();
      
      for(String ymStr: ymExpList){
        ymList.add(TimeFormat_YM.parse(ymStr).getMonth()*1.0);
      }
      
      v = getVariance(ymList);
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    return v;
  }
  //
  private static double dayVariance(ArrayList<String> ymdExpList){
    if(null==ymdExpList || ymdExpList.size()<=1){
      return 0;
    }
    
    double v = 0.0;
    try {
      ArrayList<Double> ymdList = new ArrayList<>();
      
      for(String ymdStr: ymdExpList){
        ymdList.add(TimeFormat_YMD.parse(ymdStr).getDay()*1.0);
      }
      
      v = getVariance(ymdList);
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    return v;
  }
  
  //
  private static double getVariance(ArrayList<Double> dList){
    double sum = 0.0;
    for(Double d: dList){
      sum += d;
    }
    
    double avg = sum/dList.size();
    
    sum = 0.0;
    for(Double d: dList){
      sum += Math.pow((d-avg), 2);
    }
    
    return sum/dList.size();    
  }
 
  //
  private static double decayEfficient(int difference){
    return Math.pow(0.5, Math.abs(difference));
  }
  
  //////////////////////////////////////
  
  
  public static TemFeatureVector docFeatures(TemQuery temQuery, SubtopicType subtopicType, ArrayList<StrStrInt> tripleList, org.apache.lucene.document.Document lpDoc){
    //searchQuery feature
    //<1>
    String searchQuery = temQuery.getSearchQuery(subtopicType);
    HashSet<String> sqNounSet = new HashSet<>();
    
    ArrayList<ArrayList<StrStrStr>> sqAnnotationList = StanfordNER.suitParsing(searchQuery);
    for(ArrayList<StrStrStr> sqAnnotation: sqAnnotationList){
      ArrayList<String> sqNounList = StanfordNER.getNounTerms(sqAnnotation);
      sqNounSet.addAll(sqNounList);
    }
    //<2> tense of subtopic title----!
    HashSet<String> subtopicTenseSet = getSubtopicTense(temQuery.getTemSubtopic(subtopicType));
    
    //per doc parameter
    //(1) tense
    int [] tenseArray = new int[4];
    for(int i=0; i<tenseArray.length; i++){
      tenseArray[i] = 0;
    }
    //(2) temporal expressions
    //in: co-occurred with queried terms
    ArrayList<String> inTemporalExpList = new ArrayList<>();
    int inSenCount = 0;
    ArrayList<String> outTemporalExpList = new ArrayList<>();
    int outSenCount = 0;
    
    ArrayList<ArrayList<ArrayList<StrStrStr>>> parsedList = null;
    boolean useLoadedVersion = false;
    //----
    String prefix = Integer.toString(tripleList.hashCode());    
    String key = Integer.toString(tripleList.size())+"_"+StanfordNER.encryptToMD5(tripleList.get(0).toString());
    String bufferFile = TDirectory.TemBufferDir+prefix+"_"+key+".ser";
    File bFile = new File(bufferFile);
    
    if(bFile.exists()){      
      if(debug){
        System.out.println("loading buffered file...");
      }
      try {
        FileInputStream fis = new FileInputStream(bufferFile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        parsedList = (ArrayList<ArrayList<ArrayList<StrStrStr>>>) ois.readObject();
        ois.close();
        useLoadedVersion = true;
        
        if(debug){
          for(ArrayList<ArrayList<StrStrStr>> seAnn: parsedList){
            for(ArrayList<StrStrStr> ann: seAnn){
              System.out.println(ann.toString());
            }
          }
        } 
      } catch (Exception e) {
        // TODO: handle exception
        e.printStackTrace();
      }      
      if(null == parsedList){
        System.err.println("loading buffered file error!");
        System.exit(1);   
      }        
    }
    //----
    
    if(!useLoadedVersion){
      parsedList = new ArrayList<>();
    }
    //per sentence
    for(int k=0; k<tripleList.size(); k++){ 
      StrStrInt triple = tripleList.get(k);
      
      //-------sentence.txt-----
      //initial annotation
      ArrayList<ArrayList<StrStrStr>> seAnnotationList;
      if(useLoadedVersion){
        seAnnotationList = parsedList.get(k);
      }else{
        seAnnotationList = StanfordNER.suitParsing(triple.second);
        
        parsedList.add(seAnnotationList);
      }
      
      //noun terms in a sentence
      HashSet<String> seNounSet = new HashSet<>();
      for(ArrayList<StrStrStr> seAnnotation: seAnnotationList){
        ArrayList<String> seNounList = StanfordNER.getNounTerms(seAnnotation);
        seNounSet.addAll(seNounList);
      }
      //whether the queried terms are included
      if(includeQueriedTerm(seNounSet, sqNounSet)){
        triple.third = 1;
        inSenCount++;
      }else{
        triple.third = 0;
        outSenCount++;
      }
      
      //-------sentence.xml-----
      try {
        SAXBuilder saxBuilder = new SAXBuilder();      
        Document xmlDoc = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(triple.first.getBytes("UTF-8")))); 
        Element seElement = xmlDoc.getRootElement();
        
        ArrayList<String> tExpressionList = new ArrayList<>();
        List<Element> tElementList = seElement.getChildren("t");
        for(Element tElement: tElementList){
          String tExpression = tElement.getAttributeValue("val");
          if(null != tExpression){
            tExpressionList.add(tExpression);
          }
        }
        
        if(triple.third > 0){
          inTemporalExpList.addAll(tExpressionList);
        }else{
          outTemporalExpList.addAll(tExpressionList);
        }
      } catch (Exception e) {
        // TODO: handle exception
        e.printStackTrace();
      }
      
      //----tense part----
      if(null!=seAnnotationList && seAnnotationList.size()>0){
        String tenseStr = StanfordNER.getTenseStr(seAnnotationList.get(0));
        String [] tenseTagArray = tenseStr.split("\\s");
        
        if(null!=tenseTagArray && tenseTagArray.length>0){
          for(int i=0; i<tenseTagArray.length; i++){
            if(acceptedTenseTag(tenseTagArray[i])){
              if(isPastTenseTag(tenseTagArray[i])){
                if(triple.third > 0){
                  tenseArray[TenseGroup.inPast.ordinal()] += 1;
                }else{
                  tenseArray[TenseGroup.outPast.ordinal()] += 1;
                }            
              }else{
                if(triple.third > 0){
                  tenseArray[TenseGroup.inPF.ordinal()] += 1;
                }else{
                  tenseArray[TenseGroup.outPF.ordinal()] += 1;
                }
              }
            }
          }
        }
      }                
    }
    
    //----tense features--(5)--
    int totalTenseCount = 0;
    for(int i=0; i<tenseArray.length; i++){
      totalTenseCount += tenseArray[i];
    }
    //feature 1-4
    double ratioOfInPast, ratioOfInPF, ratioOfOutPast, ratioOfOutPF;
    ratioOfInPast = tenseArray[TenseGroup.inPast.ordinal()]/(1.0*totalTenseCount);
    ratioOfInPF = tenseArray[TenseGroup.inPF.ordinal()]/(1.0*totalTenseCount);
    ratioOfOutPast = tenseArray[TenseGroup.outPast.ordinal()]/(1.0*totalTenseCount);
    ratioOfOutPF = tenseArray[TenseGroup.outPF.ordinal()]/(1.0*totalTenseCount);
    
    //w.r.t. subtopic's tense
    //feature 5
    double ratioOfSubtopicTense = 0;
    for(String subtopicTense: subtopicTenseSet){
      if(acceptedTenseTag(subtopicTense)){
        if(isPastTenseTag(subtopicTense)){
          ratioOfSubtopicTense += ratioOfInPast;
          ratioOfSubtopicTense += ratioOfOutPast;
        }else{
          ratioOfSubtopicTense += ratioOfInPF;
          ratioOfSubtopicTense += ratioOfOutPF;
        }
      }
    }
    
    //----temporal features
    //----doc-side
    //feature 6-11
    double d_InY, d_InYM, d_InYMD, d_OutY, d_OutYM, d_OutYMD;
    
    ArrayList<String> inY_ExpList = new ArrayList<>();
    ArrayList<String> inYM_ExpList = new ArrayList<>();
    ArrayList<String> inYMD_ExpList = new ArrayList<>();
    for(String inTemExp: inTemporalExpList){
      if(4 == inTemExp.trim().length()){
        inY_ExpList.add(inTemExp.trim());
      }else if(6 == inTemExp.trim().length()){
        inYM_ExpList.add(inTemExp.trim());
      }else if(8 == inTemExp.trim().length()){
        inYMD_ExpList.add(inTemExp.trim());
      }
    }
    
    d_InY = yearVariance(inY_ExpList);
    d_InYM = monthVariance(inYM_ExpList);
    d_InYMD = dayVariance(inYMD_ExpList);
    
    ArrayList<String> outY_ExpList = new ArrayList<>();
    ArrayList<String> outYM_ExpList = new ArrayList<>();
    ArrayList<String> outYMD_ExpList = new ArrayList<>();
    for(String outTemExp: outTemporalExpList){
      if(4 == outTemExp.trim().length()){
        outY_ExpList.add(outTemExp.trim());
      }else if(6 == outTemExp.trim().length()) {
        outYM_ExpList.add(outTemExp.trim()); 
      }else if(8 == outTemExp.trim().length()){
        outYMD_ExpList.add(outTemExp.trim());
      }
    }
    
    d_OutY = yearVariance(outY_ExpList);
    d_OutYM = monthVariance(outYM_ExpList);
    d_OutYMD = dayVariance(outYMD_ExpList);
    
    //---w.r.t. query issue
    //feature 12-14
    double decayY = 0, decayM = 0, decayD = 0;
    
    try {
      Date queryIssueTime = TimeFormat_QueryIssue.parse(temQuery.getQueryTime());
      Date docIssueTime = TimeFormat_DocIssue.parse(lpDoc.get("date"));
      
      decayY = decayEfficient(queryIssueTime.getYear() - docIssueTime.getYear());
      decayM = decayEfficient(queryIssueTime.getMonth() - docIssueTime.getMonth());
      decayD = decayEfficient(queryIssueTime.getDay() - docIssueTime.getDay());
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    
    //-----
    TemFeatureVector temFeatureVector = new TemFeatureVector(ratioOfInPast, ratioOfInPF, ratioOfOutPast, ratioOfOutPF, 
        ratioOfSubtopicTense, 
        d_InY, d_InYM, d_InYMD, d_OutY, d_OutYM, d_OutYMD,
        decayY, decayM, decayD);
    
    //-----buffer
    if(!useLoadedVersion){
      try {
        FileOutputStream fos = new FileOutputStream(bufferFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(parsedList);
        oos.close();
      } catch (Exception e) {
        // TODO: handle exception
        e.printStackTrace();
      } 
    }     
    //-----buffer
    
    return temFeatureVector;    
  }
  
  
  
  //////////////////////////
  public static void main(String []args){
    //1
    FeatureParser.test();
  }
  
}
