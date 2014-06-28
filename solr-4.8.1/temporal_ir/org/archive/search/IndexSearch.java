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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.archive.TDirectory;
import org.archive.data.TemLoader;
import org.archive.data.TemLoader.TemRunType;
import org.archive.data.query.TemQuery;
import org.archive.util.IOBox;
import org.archive.util.StrStr;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/** Simple command-line based search demo. */
public class IndexSearch {  
  private static final boolean debug = false;

  //example-1
  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = results.totalHits;
    System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    int end = Math.min(numTotalHits, hitsPerPage);
        
    while (true) {
      if (end > hits.length) {
        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
        System.out.println("Collect more (y/n) ?");
        String line = in.readLine();
        if (line.length() == 0 || line.charAt(0) == 'n') {
          break;
        }

        hits = searcher.search(query, numTotalHits).scoreDocs;
      }
      
      end = Math.min(hits.length, start + hitsPerPage);
      
      for (int i = start; i < end; i++) {
        if (raw) {                              // output raw format
          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
          continue;
        }

        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
        if (path != null) {
          System.out.println((i+1) + ". " + path);
          String title = doc.get("title");
          if (title != null) {
            System.out.println("   Title: " + doc.get("title"));
          }
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }
                  
      }

      if (!interactive || end == 0) {
        break;
      }

      if (numTotalHits >= end) {
        boolean quit = false;
        while (true) {
          System.out.print("Press ");
          if (start - hitsPerPage >= 0) {
            System.out.print("(p)revious page, ");  
          }
          if (start + hitsPerPage < numTotalHits) {
            System.out.print("(n)ext page, ");
          }
          System.out.println("(q)uit or enter number to jump to a page.");
          
          String line = in.readLine();
          if (line.length() == 0 || line.charAt(0)=='q') {
            quit = true;
            break;
          }
          if (line.charAt(0) == 'p') {
            start = Math.max(0, start - hitsPerPage);
            break;
          } else if (line.charAt(0) == 'n') {
            if (start + hitsPerPage < numTotalHits) {
              start+=hitsPerPage;
            }
            break;
          } else {
            int page = Integer.parseInt(line);
            if ((page - 1) * hitsPerPage < numTotalHits) {
              start = (page - 1) * hitsPerPage;
              break;
            } else {
              System.out.println("No such page");
            }
          }
        }
        if (quit) break;
        end = Math.min(numTotalHits, start + hitsPerPage);
      }
    }
  }

  //example-2
  /** Simple command-line based search demo. */
  public static void exampleMain(String[] args) throws Exception {
    String usage =
      "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }

    String index = "index";
    String field = "contents";
    String queries = null;
    int repeat = 0;
    boolean raw = false;
    String queryString = null;
    int hitsPerPage = 10;
    
    for(int i = 0;i < args.length;i++) {
      if ("-index".equals(args[i])) {
        index = args[i+1];
        i++;
      } else if ("-field".equals(args[i])) {
        field = args[i+1];
        i++;
      } else if ("-queries".equals(args[i])) {
        queries = args[i+1];
        i++;
      } else if ("-query".equals(args[i])) {
        queryString = args[i+1];
        i++;
      } else if ("-repeat".equals(args[i])) {
        repeat = Integer.parseInt(args[i+1]);
        i++;
      } else if ("-raw".equals(args[i])) {
        raw = true;
      } else if ("-paging".equals(args[i])) {
        hitsPerPage = Integer.parseInt(args[i+1]);
        if (hitsPerPage <= 0) {
          System.err.println("There must be at least 1 hit per page.");
          System.exit(1);
        }
        i++;
      }
    }
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    // :Post-Release-Update-Version.LUCENE_XY:
    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);

    BufferedReader in = null;
    if (queries != null) {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), StandardCharsets.UTF_8));
    } else {
      in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    }
    // :Post-Release-Update-Version.LUCENE_XY:
    QueryParser parser = new QueryParser(Version.LUCENE_48, field, analyzer);
    while (true) {
      if (queries == null && queryString == null) {                        // prompt the user
        System.out.println("Enter query: ");
      }

      String line = queryString != null ? queryString : in.readLine();

      if (line == null || line.length() == -1) {
        break;
      }

      line = line.trim();
      if (line.length() == 0) {
        break;
      }
      
      Query query = parser.parse(line);
      System.out.println("Searching for: " + query.toString(field));
            
      if (repeat > 0) {                           // repeat & time as benchmark
        Date start = new Date();
        for (int i = 0; i < repeat; i++) {
          searcher.search(query, null, 100);
        }
        Date end = new Date();
        System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
      }

      doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);

      if (queryString != null) {
        break;
      }
    }
    reader.close();
  }
  
  ///////////////////////
  //search a lpFile via accessing index of ..._check.xml files
  ///////////////////////
  
  ////parameters for fetch lp files 
  private static String lpIndexDir = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_Temporalia/Temporalia/LPFileIndex/"; 
  private static IndexReader lpIndexReader;
  private static IndexSearcher lpSearcher;
  private static Analyzer lpAnalyzer = new KeywordAnalyzer();  
  private static String lpField = "id";
  private static QueryParser lpParser = new QueryParser(Version.LUCENE_48, lpField, lpAnalyzer);
  private static boolean lpIni = false;
  /****/
  public static Document fetchLPFile(String fileID) throws Exception{
    if(!lpIni){
      lpIndexReader = DirectoryReader.open(FSDirectory.open(new File(lpIndexDir)));
      lpSearcher = new IndexSearcher(lpIndexReader);
      
      lpIni = true;
    }      
   
    Query query = lpParser.parse(fileID);      
    TopDocs results = lpSearcher.search(query, 2);
    ScoreDoc[] hits = results.scoreDocs;
    Document lpDoc = lpSearcher.doc(hits[0].doc); 

    return lpDoc;
  }
  
  ////////////////////////
  //search top-k results via accessing index of ..._solr.xml files
  ////////////////////////
  private static boolean solrIni = false;
  private static String solrIndexDir = "H:/solr-4.8.1/solr/example/solr/SingleFileTestCore/data/index/";
  private static IndexReader solrIndexReader;
  private static IndexSearcher solrSearcher;
  private static Similarity solrSimilarity;
  //:Post-Release-Update-Version.LUCENE_XY:
  private static Analyzer solrAnalyzer;
  private static QueryParser solrParser;
  /**
   * 
   * **/
  public static ArrayList<ResultSlot> initialLuceneSearch(String searchQuery, int slotNumber) throws Exception{    
    // String queryStr = "apple";
    //int resultNum = 10;
    //String field = "content";
    if(!solrIni){
      solrIndexReader = DirectoryReader.open(FSDirectory.open(new File(solrIndexDir)));
      solrSearcher = new IndexSearcher(solrIndexReader);
      solrSimilarity = new LMDirichletSimilarity();
      solrSearcher.setSimilarity(solrSimilarity);
      solrAnalyzer = new StandardAnalyzer(Version.LUCENE_48);
      //solrParser = new QueryParser(Version.LUCENE_48, field, solrAnalyzer);
      solrParser = new MultiFieldQueryParser(Version.LUCENE_48, new String[] {"title", "content"}, solrAnalyzer);
      
      solrIni = true;
    }
    
    Query query = solrParser.parse(searchQuery);
    
    // Collect enough docs to show 5 pages
    TopDocs resultList = solrSearcher.search(query, slotNumber);
    ScoreDoc[] hitList = resultList.scoreDocs;
    
    ArrayList<ResultSlot> slotList = new ArrayList<>();
    for(int i=0; i<hitList.length; i++){
      ScoreDoc hit = hitList[i];
      Document doc = solrSearcher.doc(hit.doc);
      String id = doc.get("id");
      
      slotList.add(new ResultSlot(id, (i+1), hit.score));
    }
    
    if(debug){
      System.out.println("search results:");
      System.out.println();
      for(ScoreDoc hit: hitList){
        System.out.println("doc="+hit.doc+" score="+hit.score);
        Document doc = solrSearcher.doc(hit.doc);
        String id = doc.get("id");
        System.out.println("id\t"+ id);
        System.out.println("-------- lp file -------");
        System.out.println(fetchLPFile(id).get("text"));
        System.out.println();      
      }
    }
    
    return slotList;    
  }
  
  ///////////////////
  //top-10 result for queries of ntcir11_Temporalia_NTCIR-11TQICQueriesFormalRun
  ///////////////////  
  
  private static void getTop10Results(TemRunType runType) throws Exception{
    //queries
    String qFile;
    
    BufferedWriter top20IDWriter;
    BufferedWriter top20SolrWriter;
    BufferedWriter top20CheckWriter;
    
    if(runType == TemRunType.DryRun){
      qFile = TDirectory.ROOT_DATASET+"Temporalia/DryRun/ntcir11_Temporalia_ntcir11-temporalia-tqic-dryrun.txt";
      
      top20IDWriter = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"/top10/idmap_"+TemRunType.DryRun.toString()+".txt");
      top20SolrWriter = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"/top10/solr_"+TemRunType.DryRun.toString()+".txt");
      top20CheckWriter = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"/top10/check_"+TemRunType.DryRun.toString()+".txt");
    }else {
      qFile = TDirectory.ROOT_DATASET+"Temporalia/FormalRun/ntcir11_Temporalia_NTCIR-11TQICQueriesFormalRun.txt";
      
      top20IDWriter = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"/top10/idmap_"+TemRunType.FormalRun.toString()+".txt");
      top20SolrWriter = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"/top10/solr_"+TemRunType.FormalRun.toString()+".txt");
      top20CheckWriter = IOBox.getBufferedWriter_UTF8(TDirectory.ROOT_OUTPUT+"/top10/check_"+TemRunType.FormalRun.toString()+".txt");
    }
    
    ArrayList<String> lineList = IOBox.getLinesAsAList_UTF8(qFile);
    
    //build a standard pseudo-xml file
    StringBuffer buffer = new StringBuffer();
    buffer.append("<add>");
    for(String line: lineList){
      buffer.append(TemLoader.stripNonValidXMLCharacters(line));
    }
    buffer.append("</add>"); 
    
    SAXBuilder saxBuilder = new SAXBuilder();      
    org.jdom.Document xmlDoc = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes("UTF-8"))));   
    Element webtrackElement = xmlDoc.getRootElement();
    List<Element> queryList = webtrackElement.getChildren("query");
    
    ArrayList<StrStr> qList = new ArrayList<>();    
    for(Element query: queryList){
      qList.add(new StrStr(query.getChildText("id").trim(), query.getChildText("query_string").trim()));
    }
    
    //solr search
    solrIndexReader = DirectoryReader.open(FSDirectory.open(new File(solrIndexDir)));
    solrSearcher = new IndexSearcher(solrIndexReader);
    solrSimilarity = new LMDirichletSimilarity();
    solrSearcher.setSimilarity(solrSimilarity);
    solrAnalyzer = new StandardAnalyzer(Version.LUCENE_48);
    solrParser = new MultiFieldQueryParser(Version.LUCENE_48, new String[] {"title", "content"}, solrAnalyzer);
    
    //check search
    lpIndexReader = DirectoryReader.open(FSDirectory.open(new File(lpIndexDir)));
    lpSearcher = new IndexSearcher(lpIndexReader);
    
    //
    
    int count = 1;
    for(StrStr q: qList){
      System.out.println((count++));
      //1
      Query solrQuery = solrParser.parse(q.second);
      TopDocs solrResultList = solrSearcher.search(solrQuery, 20);
      ScoreDoc[] solrHitList = solrResultList.scoreDocs;
      
      ArrayList<String> docidList = new ArrayList<>();
      
      for(int i=0; i<solrHitList.length; i++){
        ScoreDoc solrHit = solrHitList[i];
        Document doc = solrSearcher.doc(solrHit.doc);
        String docid = doc.get("id");
        docidList.add(docid);
      }
      
      //id map
      top20IDWriter.write(q.first);
      top20IDWriter.newLine();
      for(String docid: docidList){
        top20IDWriter.write("\t"+docid);
        top20IDWriter.newLine();
      }
      
      //solr doc
      for(int i=0; i<solrHitList.length; i++){
        ScoreDoc solrHit = solrHitList[i];
        Document solrDoc = solrSearcher.doc(solrHit.doc);
        top20SolrWriter.write(TemLoader.toSolrXml(solrDoc));
        top20SolrWriter.newLine();
      }
      
      //check doc
      for(String docid: docidList){
        Query checkQuery = lpParser.parse(docid);      
        TopDocs checkResults = lpSearcher.search(checkQuery, 2);
        ScoreDoc[] checkHits = checkResults.scoreDocs;
        Document checkDoc = lpSearcher.doc(checkHits[0].doc);
        
        top20CheckWriter.write(TemLoader.toCheckXml(checkDoc));
        top20CheckWriter.newLine();
      }
    }   
    
    //
    top20IDWriter.flush();
    top20IDWriter.close();
    
    top20SolrWriter.flush();
    top20SolrWriter.close();
    
    top20CheckWriter.flush();
    top20CheckWriter.close();    
  }
  
  ////////////////////
  //
  ////////////////////
  public static void main(String[] args) throws Exception{
    //1
    //IndexSearch.performSearch();
    
    //2 top-10
    //Just replace “&” with “&amp;” in your HTML/Javascript code!
    ///*
    try {      
      IndexSearch.getTop10Results(TemRunType.FormalRun);
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    //*/
    
    //3 lpFetch
    /*
    Document lpDocument = IndexSearch.fetchLPFile("lk-20110605040101_1606");
    System.out.println(lpDocument.get("text"));
    */
  }
}
