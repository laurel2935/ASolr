package org.archive.dataset.query;

import java.util.ArrayList;

import org.archive.dataset.query.TemSubtopic.SubtopicType;


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

public class TemQuery {
  String _id;
  String _title;
  String _des;
  String _queryTime;
  ArrayList<TemSubtopic> _subtopicList;
  
  private static boolean useTitle = true;
  private static boolean useDes = true;
  
  public TemQuery(String id, String title, String description, String queryTime){
    this._id = id;
    this._title = title;
    this._des = description;
    this._queryTime = queryTime;    
  }
  
  public void setSubtopicList(ArrayList<TemSubtopic> subtopicList){
    this._subtopicList = subtopicList;
  }
  
  public String getID(){
    return this._id;
  }
  
  public String getTitle(){
    return this._title;
  }
  
  public String getDescription(){
    return this._des;
  }
  
  public String getQueryTime(){
    return this._queryTime.trim();
  }
  
  public ArrayList<TemSubtopic> getSubtopicList(){
    return this._subtopicList;
  }
  
  public String getSearchQuery(){
    String sq = "";
    if(useTitle){
      sq += this._title;
      sq += ".";
    }
    
    if(useDes){
      sq += " ";
      sq += this._des;
      sq += ".";
    }
    
    return sq;
  }
  
  public String getSearchQuery(SubtopicType subtopicType){
    String sq = this.getSearchQuery();
    
    for(TemSubtopic temSubtopic: this._subtopicList){
      if(temSubtopic._subType.trim().equals(subtopicType.name())){
        sq += (" "+temSubtopic._subTitle);
        return sq;
      }
    }
    
    return sq;    
  }
  
  public TemSubtopic getTemSubtopic(SubtopicType subtopicType){
    TemSubtopic reTemSubtopic = null;
    
    for(TemSubtopic temSubtopic: this._subtopicList){
      if(temSubtopic._subType.trim().equals(subtopicType.toString())){       
        reTemSubtopic = temSubtopic;
      }
    }
    
    return reTemSubtopic;
  }
  
  public String toString(){
    StringBuffer buffer = new StringBuffer();
    buffer.append(this._id+"\t"+this._title+"\n"+this._des+"\n"+this._queryTime+"\n");
    for(TemSubtopic subtopic: this._subtopicList){
      buffer.append(subtopic.toString()+"\n");
    }
    return buffer.toString();    
  }  
}
