package org.archive.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class StandardFormat {	
	/**
	 * time format
	 */
	public static String toStandardFormat(String date, String sourceFormat, String targetFormat){		
		try{ 			
	          SimpleDateFormat sFormat = new SimpleDateFormat(sourceFormat);   
	          SimpleDateFormat tFormat = new SimpleDateFormat(targetFormat);
	          Date sDate = sFormat.parse(date);   
	          String time = tFormat.format(sDate);	          
	          return time;
	      }catch(Exception   ex){   
	          ex.printStackTrace();   
	      } 
	      return null;
	}
	/**
	 * 取得年份
	 */
	public static String getYear(String date, String foramt){
		try{
			SimpleDateFormat Format = new SimpleDateFormat(foramt);
	        Date sDate = Format.parse(date);
	        GregorianCalendar gc =new GregorianCalendar();
	        gc.setTime(sDate);
	        
	        return Integer.toString(gc.get(GregorianCalendar.YEAR));
		}catch(Exception e){
			e.printStackTrace();			
		}
		return null;
	}	
	/**
	 * 序列号命名格式
	 */
	public static String serialFormat(int num, String pattern){
		//String pattern = "00000000";
		DecimalFormat df = new DecimalFormat(pattern);
		return df.format(num);
	}
		
	public static void main(String []args){
		//1
		System.out.println(Integer.valueOf("002"));
	}
	
	//
	public static void mail(String []args) {
	  
    
  }
}

