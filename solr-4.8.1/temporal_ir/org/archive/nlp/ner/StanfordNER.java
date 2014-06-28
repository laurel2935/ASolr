package org.archive.nlp.ner;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.archive.TDirectory;
import org.archive.util.StrStr;
import org.archive.util.StrStrStr;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

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

public class StanfordNER {
  private static final boolean debug = false;
  
  //private static String classifier1 = "english.all.3class.distsim.crf.ser.gz";
  //private static String classifier2 = "english.conll.4class.distsim.crf.ser.gz";
  //private static String classifier3 = "english.muc.7class.distsim.crf.ser.gz";
  
  private static String serializedClassifier = TDirectory.ROOT_JAR+"stanford-corenlp-full-2014-06-16/ner/english.all.3class.distsim.crf.ser.gz";
  //
  private static CRFClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
  
  /**
   * named entity recognition
   * **/
  public static List<StrStr> ner(String text){
    List<List<CoreLabel>> classify = classifier.classify(text);
    List<StrStr> results = new ArrayList<>();
    
    for (List<CoreLabel> coreLabels : classify) {
        for (CoreLabel coreLabel : coreLabels) {
            String word = coreLabel.word();
            String answer = coreLabel.get(CoreAnnotations.AnswerAnnotation.class);
            if(!"O".equals(answer)){
                results.add(new StrStr(word, answer));
            }
        }
    }
    
    if(debug){
      for(StrStr ne: results){
        System.out.println(ne.toString());
      }
    }
    
    return results;
  }
  
  /**
   * suit parsing
   * **/
  private static boolean suitIni = false;
  private static Properties suitProps ;
  private static StanfordCoreNLP suitPipeline;
  //props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
  
  public static ArrayList<ArrayList<StrStrStr>> suitParsing(String text){    
    if(!suitIni){
      suitProps = new Properties();
      suitProps.put("annotators", "tokenize, ssplit, pos, lemma, ner");      
      
      suitPipeline = new StanfordCoreNLP(suitProps);
      
      suitIni = true;
    }   
    
    // create an empty Annotation just with the given text
    Annotation document = new Annotation(text);

    // run all Annotators on this text
    suitPipeline.annotate(document);

    // these are all the sentences in this document
    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    
    ArrayList<ArrayList<StrStrStr>> seAnnotationList = new ArrayList<>();

    for(CoreMap sentence: sentences) {
      ArrayList<StrStrStr> seAnnotation = new ArrayList<>();
      // traversing the words in the current sentence a CoreLabel is a CoreMap with additional token-specific methods
      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
        // this is the text of the token
        String word = token.get(TextAnnotation.class);
        // this is the POS tag of the token
        String pos = token.get(PartOfSpeechAnnotation.class);
        // this is the NER label of the token
        String ne = token.get(NamedEntityTagAnnotation.class);   
        
        seAnnotation.add(new StrStrStr(word, pos, ne));
      }
      
      seAnnotationList.add(seAnnotation);
    }
    
    if(debug){
      for(ArrayList<StrStrStr> seAnnotation: seAnnotationList){
        for(StrStrStr e: seAnnotation){
          System.out.println(e.toString());
        }
        System.out.println();
      }
    }
    
    return seAnnotationList;
  }
  
  /**
   * per sentence
   * **/
  public static ArrayList<String> getNounTerms(ArrayList<StrStrStr> elementList){
    ArrayList<String> nTermList = new ArrayList<>();
    
    for(StrStrStr element: elementList){
      if(element.second.startsWith("N") || element.second.startsWith("n") ){
        nTermList.add(element.first);
      }
    }
    
    return nTermList;
  }
  
  /**
   * per sentence
   * **/
  public static String getTenseStr(ArrayList<StrStrStr> elementList){
    String tenseStr = "";
    
    for(StrStrStr element: elementList){
      if(element.second.startsWith("V")){
        tenseStr += (element.second+" ");
      }
    }
    
    return tenseStr.trim();   
  }
  
  
  
  //
  public static void main(String []args){
    //1
    //proper if use named entities? how about keyword?
    //String text = "What has she done in order to help people in the world?";
    //!!! suggest not merely using the subtopic title as a search query, it should be [title + description + subtopic title]
    
    //String text = "What was the state of Japan's economy before abenomics?";
    //StanfordNER.ner(text);
    
    //2
    //String text = "What was the state of Japan's economy before abenomics?";
    String text = "They tell The New York Times";
    StanfordNER.suitParsing(text);
    
    
  } 
}
