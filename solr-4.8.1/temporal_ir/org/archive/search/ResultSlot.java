package org.archive.search;

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

public class ResultSlot implements Comparable{
  public float _grTruthRelScore;
  //
  public String _docid;
  public int _rank;
  public double _score;
  
  ResultSlot(String docid, int rank, double score){
    this._docid = docid;
    this._rank = rank;
    this._score = score;
    
    this._grTruthRelScore = 0;
  }
  
  public void setGrTruthRelScore(float grTruthRelScore){
    this._grTruthRelScore = grTruthRelScore;
  }
  
  public int compareTo(Object cmpObj){
    ResultSlot cmp = (ResultSlot)cmpObj;
    
    if(this._grTruthRelScore > cmp._grTruthRelScore){
      return -1;      
    }else if(this._grTruthRelScore < cmp._grTruthRelScore){
      return 1;
    }else{
      return 0;
    }   
  }
  
  
}
