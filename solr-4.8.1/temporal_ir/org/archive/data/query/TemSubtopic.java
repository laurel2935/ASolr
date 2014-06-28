package org.archive.data.query;

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

public class TemSubtopic {
  public static enum SubtopicType {atemporal, past, recency, future};
  //
  String _subId;
  String _subType;
  String _subTitle;
  
  public TemSubtopic(String subId, String subType, String subTitle){
    this._subId = subId;
    this._subType = subType;
    this._subTitle = subTitle;
  }
  
  public String getSubtopicID(){
    return this._subId;
  }
  
  public String getSubtopicType(){
    return this._subType;
  }
  
  public String getSubtopicTitle(){
    return this._subTitle;
  }
  
  public String toString(){
    return this._subId+"\t"+this._subType+"\t"+this._subTitle;
  }
}
