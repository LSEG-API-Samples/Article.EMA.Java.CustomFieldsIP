package com.refinitiv.ema.examples.training.iprovider.example__custom__fields;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class ItemInfo {
    //The initial value of BID and ASK
	private final static double DEFAULT_BID = 20.06d;
	private final static double DEFAULT_ASK = 20.09d;
	//The last(current) BID and ASK
	private double lastBid;
	private double lastAsk;
	//The sum of BID and ASK
	private double sumBid;
	private double sumAsk;
	//The BID and ASK average
	private double bidAvgIntraDay;
	private double askAvgIntraDay;
	//the number of BID/ASK data
	private int numData;
	//The object is used to generate random increase for creating new BID/ASK
	private Random rand;
	
    public ItemInfo() {
    	//The first BID and ASK are the initial values
    	lastBid = DEFAULT_BID;
    	lastAsk = DEFAULT_ASK;
    	//The first sum of BID and ASK are the first BID and ASK.
    	sumBid = lastBid;
    	sumAsk = lastAsk;
    	//The first BID and ASK average are the first BID and ASK.
    	bidAvgIntraDay = lastBid;
    	askAvgIntraDay = lastAsk;
    	//there is only 1 BID and ASK 
    	numData = 1;
    	rand=new Random();
    }
    
    //The method generates new data of a field e.g.BID and ASK by adding increase to data 
    private double newData(double data,double increase){
    	//Rounding the increase to 2 decimals e.g.0.7853612220729844 -> 0.79
    	increase = (BigDecimal.valueOf(increase)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    	double newData = data + increase;
    	//Rounding new data to 2 decimals and return the rounded data
    	return (BigDecimal.valueOf(newData)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    }
    //The method calculates the new average according to the last sum, new data and the number of data.
    private AverageInfo newAverage(double sum,double data,double num) {
    	//Create an AverageInfo object to keep the new average's info which is the method's output.
    	AverageInfo aInfo = new AverageInfo();
    	//Calculate the new sum which is the last sum + new data
    	double rawSum = sum + data;
    	//rounding the new sum to 2 decimals and set it in the AverageInfo object
    	aInfo.sum = (BigDecimal.valueOf(rawSum)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    	//Calculate the average by dividing new sum by the number of data
    	double rawAve = aInfo.sum/num;
    	//Rounding the new average to 2 decimals and set it in the AverageInfo object
    	aInfo.average= (BigDecimal.valueOf(rawAve)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    	//return the AverageInfo object consisting of the new sum and the new average
    	return aInfo;
    }
    
    //The method generates new data of all fields:
    //Refinitiv fields - BID, ASK. In the real world, you can get them by subscribing an item from a feed.
    //The custom fields - BID_AVG_INTRADAY, ASK_AVG_INTRADAY. In the real world, the custom fields may be calculated from Refinitiv fields.  
    //This method should be called when the provider wants to sends new data to the consumer. 
    //In the real world, when the application receives new data (Refinitiv fields). Then,
    //the custom fields based on these Refinitiv fields should be calculated and sent to the consumer e.g.in an Update message
    public void GenerateAllFields() {
    	//Choose the increase value at random
    	//nextDouble() return double value between 0.0 and 1.0 e.g. 0.7853612220729844
    	double increase = rand.nextDouble();
    	//Increase the number of BID/ASK data to include new data
    	++numData;
    	
    	//Call the newData(..) method which generates new dummy value of BID from last BID + increase value
    	lastBid = newData(lastBid,increase);
    	//Calls newAverage(..) method which calculates new BID_AVG_INTRADAY value
    	//from(the sum of BID values + new BID value)/the number of BIDs 
    	AverageInfo bidAveInfo = newAverage(sumBid,lastBid,numData);
    	bidAvgIntraDay = bidAveInfo.average;
    	sumBid = bidAveInfo.sum;
    	
    	//Call newData(..) method which generates new dummy value of ASK from last ASK + increase value
    	//In the real world, ASK is higher than BID so increase value is increase+0.01
    	lastAsk = newData(lastAsk,increase+0.01);
    	//Calls newAverage(..) method which calculates new ASK_AVG_INTRADAY value
    	//from(the sum of ASK values + new ASK value)/the number of ASKs 
    	AverageInfo askAveInfo = newAverage(sumAsk,lastAsk,numData);
    	askAvgIntraDay = askAveInfo.average;
    	sumAsk = askAveInfo.sum;
    }
    //The method which allows other classes to get BID value 
    public double getBID() {
    	return lastBid;
    }
    //The method which allows other classes to get ASK value
    public double getASK() {
    	return lastAsk;
    }
    //The method which allows other classes to get BID_AVG_INTRADAY value
    public double getBidAvgIntraDay() {
    	return bidAvgIntraDay;
    }
   //The method which allows other classes to get ASK_AVG_INTRADAY value
    public double getAskAvgIntraDay() {
    	return askAvgIntraDay;
    }
    //The utility sub class to keep average's info   
    class AverageInfo {
    	//The sum of all values
    	double sum;
    	//The average
    	double average;
    }
}
