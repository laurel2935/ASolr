package org.archive.data.query;

import java.util.ArrayList;

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
  
  public TemQuery(String id, String title, String description, String queryTime){
    this._id = id;
    this._title = title;
    this._des = description;
    this._queryTime = queryTime;    
  }
  
  public void setSubtopicList(ArrayList<TemSubtopic> subtopicList){
    this._subtopicList = subtopicList;
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
