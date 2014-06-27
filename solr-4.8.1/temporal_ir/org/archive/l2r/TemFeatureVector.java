package org.archive.l2r;

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

public class TemFeatureVector {
  //
  public double [] fArray;
  /*
  public double _ratioOfInPast;
  public double _ratioOfInPF;
  public double _ratioOfOutPast;
  public double _ratioOfOutPF;
  //
  public double _ratioOfSubtopicTense;
  //
  public double _d_InY;
  public double _d_InYM;
  public double _d_InYMD;
  public double _d_OutY;
  public double _d_OutYM;
  public double _d_OutYMD;
  //
  public double _decayY;
  public double _decayM;
  public double _decayD;
  */
  
  public TemFeatureVector(double ratioOfInPast, double ratioOfInPF, double ratioOfOutPast, double ratioOfOutPF,
      double ratioOfSubtopicTense,
      double d_InY, double d_InYM, double d_InYMD, double d_OutY, double d_OutYM, double d_OutYMD,
      double decayY, double decayM, double decayD){
    //
    fArray = new double [14];
    
    fArray[0] = ratioOfInPast;
    fArray[1] = ratioOfInPF;
    fArray[2] = ratioOfOutPast;
    fArray[3] = ratioOfOutPF;
    
    fArray[4] = ratioOfSubtopicTense;
    
    fArray[5] = d_InY;
    fArray[6] = d_InYM;
    fArray[7] = d_InYMD;
    fArray[8] = d_OutY;
    fArray[9] = d_OutYM;
    fArray[10] = d_OutYMD;
    
    fArray[11] = decayY;
    fArray[12] = decayM;
    fArray[13] = decayD;    
  }
  
  //
  public int size(){
    return this.fArray.length;
  }
  
  //
  public String toString(){
    StringBuffer buffer = new StringBuffer();
    
    for(int i=0; i<fArray.length; i++){
      buffer.append((i+1));
      buffer.append(":");
      buffer.append(fArray[i]);
      buffer.append("\t");
    }
    
    return buffer.toString().trim();
  }
  
}
