/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Serhiy Lymar
 */
public class TimeUtils {
    
    public static String millisToDate(long millis){
        return DateFormat.getDateInstance(DateFormat.LONG).format(millis);    
    }
    
    
    public static long dateToMillis (String dateString ){ 
        long result=0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
	// String dateString = "22-03-2017 11:18:32";
	try{
           //formatting the dateString to convert it into a Date 
	   Date date = sdf.parse(dateString);
	   // System.out.println("Given Time in milliseconds : "+date.getTime());
	   Calendar calendar = Calendar.getInstance();
	   //Setting the Calendar date and time to the given date and time
	   calendar.setTime(date);
	   // System.out.println("Given Time in milliseconds : "+calendar.getTimeInMillis());
           result = calendar.getTimeInMillis();           
	}catch(ParseException e){
	   e.printStackTrace();
	}
        return result;
    }
    
}
