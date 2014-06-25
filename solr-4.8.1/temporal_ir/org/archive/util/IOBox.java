package org.archive.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

public class IOBox {
  
  private static final String DEFAULT_ENCODING = "UTF-8";
  
  public static BufferedReader getBufferedReader_UTF8(String targetFile) throws IOException{
    File file = new File(targetFile);
    if(!file.exists()){
      return null;
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING));
    return reader;
  }
  
  
  public static ArrayList<String> getLinesAsAList_UTF8(String targetFile){
    try {
      ArrayList<String> lineList = new ArrayList<String>();
      //
      BufferedReader reader = getBufferedReader_UTF8(targetFile);
      String line = null;     
      while(null != (line=reader.readLine())){
        if(line.length() > 0){          
          lineList.add(line);         
        }       
      }
      reader.close();
      return lineList;      
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    return null;
  }
  
  public static BufferedWriter getBufferedWriter_UTF8(String targetFile) throws IOException{
    //check exist
    File file = new File(targetFile);
    if(!file.exists()){
      file.createNewFile();
      file = new File(targetFile);
    }
    //generate
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), DEFAULT_ENCODING));
    return writer;
  }
  
}
