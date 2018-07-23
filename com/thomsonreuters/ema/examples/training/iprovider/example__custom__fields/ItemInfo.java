package com.thomsonreuters.ema.examples.training.iprovider.example__custom__fields;

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
    
    public double getBID() {
    	return lastBid;
    }
    public double getASK() {
    	return lastAsk;
    }
    public double getBidAvgIntraDay() {
    	return bidAvgIntraDay;
    }
    public double getAskAvgIntraDay() {
    	return askAvgIntraDay;
    }
   
    //The method generates new data of a field e.g.BID and ASK by adding increase to data 
    public double newData(double data,double increase){
    	//System.out.println("rawinc="+increase);//remove
    	//Rounding the increase to 2 decimals e.g.0.7853612220729844 -> 0.79
    	increase = (BigDecimal.valueOf(increase)).setScale(2,RoundingMode.HALF_UP).doubleValue();
        //System.out.println("increase="+ increase);//remove
    	double newData = data + increase;
        //System.out.println("newData=" + newData);//remove
    	//Rounding new data to 2 decimals and return the rounded data
    	return (BigDecimal.valueOf(newData)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    }
    //The method calculates the new average according to the last sum, new data and the number of data.
    public AverageInfo newAverage(double sum,double data,double num) {
    	//Create an AverageInfo object to keep the new average's info which is the method's output.
    	AverageInfo aInfo = new AverageInfo();
    	
    	//Calculate the new sum which is the last sum + new data
    	double rawSum = sum + data;
    	//System.out.println("rawSum="+rawSum);//remove
    	//rounding the new sum to 2 decimals and set it in the AverageInfo object
    	aInfo.sum = (BigDecimal.valueOf(rawSum)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    	//System.out.println("Sum="+aInfo.sum);//remove
    	
    	//Calculate the average by dividing new sum by the number of data
    	double rawAve = aInfo.sum/num;
    	//System.out.println("rawAve="+rawAve);//remove
    	//Rounding the new average to 2 decimals and set it in the AverageInfo object
    	aInfo.average= (BigDecimal.valueOf(rawAve)).setScale(2,RoundingMode.HALF_UP).doubleValue();
    	
    	//return the AverageInfo object consisting of the new sum and the new average
    	return aInfo;
    }
    //The method generates new data of all fields which are BID, BID_AVG_INTRADAY, ASK, ASK_AVG_INTRADAY 
    public void GenerateAllFields() {
    	//Choose the increase value at random
    	//nextDouble() return double value between 0.0 and 1.0 e.g. 0.7853612220729844
    	double increase = rand.nextDouble();
    	//Increase the number of BID/ASK data to include new data
    	++numData;
    	
    	//Generate new BID and BID_AVG_INTRADAY according to the last BID, random increase and number of data
    	lastBid = newData(lastBid,increase);
    	AverageInfo bidAveInfo = newAverage(sumBid,lastBid,numData);
    	bidAvgIntraDay = bidAveInfo.average;
    	sumBid = bidAveInfo.sum;
    	
    	//Generate new ASK and ASK_AVG_INTRADAY according to the last ASK, random increase and number of data
    	//In the real world, ASK is higher than BID. Hence, increase for ASK should higher than BID
    	lastAsk = newData(lastAsk,increase+0.01);
    	AverageInfo askAveInfo = newAverage(sumAsk,lastAsk,numData);
    	askAvgIntraDay = askAveInfo.average;
    	sumAsk = askAveInfo.sum;
    }
    //The utility sub class to keep average's info   
    class AverageInfo {
    	//The sum of all values
    	double sum;
    	//The average
    	double average;
    }
}
