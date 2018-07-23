///*|-----------------------------------------------------------------------------
// *|            This source code is provided under the Apache 2.0 license      --
// *|  and is provided AS IS with no warranty or guarantee of fit for purpose.  --
// *|                See the project's LICENSE.md for details.                  --
// *|           Copyright Thomson Reuters 2017. All rights reserved.            --
///*|-----------------------------------------------------------------------------

package com.thomsonreuters.ema.examples.training.iprovider.example__custom__fields;

import com.thomsonreuters.ema.access.EmaFactory;
import com.thomsonreuters.ema.access.FieldList;
import com.thomsonreuters.ema.access.GenericMsg;
import com.thomsonreuters.ema.access.Msg;
import com.thomsonreuters.ema.access.OmmException;
import com.thomsonreuters.ema.access.OmmProvider;
import com.thomsonreuters.ema.access.OmmProviderClient;
import com.thomsonreuters.ema.access.OmmProviderEvent;
import com.thomsonreuters.ema.access.OmmReal;
import com.thomsonreuters.ema.access.OmmState;
import com.thomsonreuters.ema.access.PostMsg;
import com.thomsonreuters.ema.access.RefreshMsg;
import com.thomsonreuters.ema.access.ReqMsg;
import com.thomsonreuters.ema.access.StatusMsg;
import com.thomsonreuters.ema.access.UpdateMsg;
import com.thomsonreuters.ema.access.DataType.DataTypes;
import com.thomsonreuters.ema.rdm.DataDictionary;
import com.thomsonreuters.ema.rdm.EmaRdm;

class AppClient implements OmmProviderClient
{
	private DataDictionary dataDictionary = EmaFactory.createDataDictionary();
	private boolean fldDictComplete = false;
	private boolean enumTypeComplete = false;
	private boolean dumpDictionary = false;
	public long itemHandle = 0;
	public long loginHandle = 0;
	
	//the source of canned data for the provider
	public ItemInfo data;
	
	//To set the value for a REAL field, numeric value and decimal are required.
	//This provider application publishes 2 decimals so just find out the numeric value
	//The method is to convert double e.g. BID to numeric value(long type) e.g. 32.45 -> 3245, 32.4 -> 3240
	public long convertDouble2NumericValue(double dVal) {
		String[] sval=Double.toString(dVal).split("\\.");
		//sval[0] is numeric value before decimal, sval[1] is number after decimal
		StringBuffer realStr = new StringBuffer(sval[0]+sval[1]);
		//Some cases sval[1] is only 1 decimal e.g. 32.4 , pad zero at the end to have 2 decimals
		if(sval[1].length()==1)
			realStr.append("0");
		//Return the numeric value converted from String
		return Long.parseLong(realStr.toString());
	}
	AppClient(String[] args)
	{
		int idx = 0;
		
		while ( idx < args.length )
		{
			if ( args[idx].compareTo("-dumpDictionary") == 0 )
			{
				dumpDictionary = true;
			}
			
			++idx;
		}
		//Initialize the ItemInfo object to create all fields with initial values
		data = new ItemInfo();
	}

	public void onReqMsg(ReqMsg reqMsg, OmmProviderEvent providerEvent)
	{
		switch(reqMsg.domainType())
		{
		case EmaRdm.MMT_LOGIN:
			processLoginRequest(reqMsg,providerEvent);
			break;
		case EmaRdm.MMT_MARKET_PRICE:
			processMarketPriceRequest(reqMsg,providerEvent);
			break;
		default:
			processInvalidItemRequest(reqMsg,providerEvent);
			break;
		}
	}
	
	public void onRefreshMsg(RefreshMsg refreshMsg, OmmProviderEvent event) 
	{
		System.out.println("Received Refresh. Item Handle: " + event.handle() + " Closure: " + event.closure());

		System.out.println("Item Name: " + (refreshMsg.hasName() ? refreshMsg.name() : "<not set>"));
		System.out.println("Service Name: " + (refreshMsg.hasServiceName() ? refreshMsg.serviceName() : "<not set>"));

		System.out.println("Item State: " + refreshMsg.state());
		
		decode(refreshMsg, refreshMsg.complete());
		
		System.out.println();
	}
	
	public void onStatusMsg(StatusMsg statusMsg, OmmProviderEvent event) 
	{
		System.out.println("Received Status. Item Handle: " + event.handle() + " Closure: " + event.closure());

		System.out.println("Item Name: " + (statusMsg.hasName() ? statusMsg.name() : "<not set>"));
		System.out.println("Service Name: " + (statusMsg.hasServiceName() ? statusMsg.serviceName() : "<not set>"));

		if (statusMsg.hasState())
			System.out.println("Item State: " + statusMsg.state());

		System.out.println();
	}
	
	public void onGenericMsg(GenericMsg genericMsg, OmmProviderEvent providerEvent) {}
	public void onPostMsg(PostMsg postMsg, OmmProviderEvent providerEvent) {}
	public void onReissue(ReqMsg reqMsg, OmmProviderEvent providerEvent) {}
	public void onClose(ReqMsg reqMsg, OmmProviderEvent providerEvent) {}
	public void onAllMsg(Msg msg, OmmProviderEvent providerEvent) {}
	
	void processLoginRequest(ReqMsg reqMsg, OmmProviderEvent event)
	{
		event.provider().submit(EmaFactory.createRefreshMsg().domainType(EmaRdm.MMT_LOGIN).name(reqMsg.name()).nameType(EmaRdm.USER_NAME).
				complete(true).solicited(true).state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Login accepted").
				attrib( EmaFactory.createElementList() ), event.handle());
		
		loginHandle = event.handle();
	}
	
	void processMarketPriceRequest(ReqMsg reqMsg, OmmProviderEvent event)
	{
		if ( itemHandle != 0 )
		{
			processInvalidItemRequest(reqMsg, event);
			return;
		}
		
		FieldList fieldList = EmaFactory.createFieldList();
		//Add the REAL fields and custom REAL fields(negative field id) to the fieldList of the Refresh Message
		//Create a REAL field by using FieldEntry.real(int fieldId, long mantissa, int magnitudeType) method
		//Use convertDouble2NumericValue(double dVal) to convert double generated from canned data(data variable) to long(mantissa parameter) 
		//magnitudeType parameter always is OmmReal.MagnitudeType.EXPONENT_NEG_2 which power of -2 or 2 decimals.
		fieldList.add(EmaFactory.createFieldEntry().real(22, convertDouble2NumericValue(data.getBID()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
		fieldList.add(EmaFactory.createFieldEntry().real(25, convertDouble2NumericValue(data.getASK()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
		fieldList.add(EmaFactory.createFieldEntry().real(-4001, convertDouble2NumericValue(data.getBidAvgIntraDay()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
		fieldList.add(EmaFactory.createFieldEntry().real(-4002, convertDouble2NumericValue(data.getAskAvgIntraDay()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
		
		
		event.provider().submit(EmaFactory.createRefreshMsg().serviceName(reqMsg.serviceName()).name(reqMsg.name()).
				state(OmmState.StreamState.OPEN, OmmState.DataState.OK, OmmState.StatusCode.NONE, "Refresh Completed").solicited(true).
				payload(fieldList).complete(true), event.handle());
		
		itemHandle = event.handle();
	}
	
	void processInvalidItemRequest(ReqMsg reqMsg, OmmProviderEvent event)
	{
		event.provider().submit(EmaFactory.createStatusMsg().name(reqMsg.name()).serviceName(reqMsg.serviceName()).
				domainType(reqMsg.domainType()).
				state(OmmState.StreamState.CLOSED, OmmState.DataState.SUSPECT, OmmState.StatusCode.NOT_FOUND, "Item not found"),
				event.handle());
	}
	
	void decode(Msg msg, boolean complete)
	{
		switch (msg.payload().dataType())
		{
		case DataTypes.SERIES:
			
			if ( msg.name().equals("RWFFld") )
			{
				dataDictionary.decodeFieldDictionary(msg.payload().series(), EmaRdm.DICTIONARY_NORMAL);
				
				if ( complete )
				{
					fldDictComplete = true;
				}
			}
			else if ( msg.name().equals("RWFEnum") )
			{
				dataDictionary.decodeEnumTypeDictionary(msg.payload().series(), EmaRdm.DICTIONARY_NORMAL);
				
				if ( complete )
				{
					enumTypeComplete = true;
				}
			}
		
			if ( fldDictComplete && enumTypeComplete )
			{
				System.out.println();
				System.out.println("\nDictionary download complete");
				System.out.println("Dictionary Id : " + dataDictionary.dictionaryId());
				System.out.println("Dictionary field version : " + dataDictionary.fieldVersion());
				System.out.println("Number of dictionary entries : " + dataDictionary.entries().size());
				
				if( dumpDictionary )
					System.out.println(dataDictionary);
			}
		
			break;
		}
	}
}

public class IProvider
{
	public static void main(String[] args)
	{
		OmmProvider provider = null;
		try
		{
			AppClient appClient = new AppClient(args);	
			FieldList fieldList = EmaFactory.createFieldList();
			UpdateMsg updateMsg = EmaFactory.createUpdateMsg();

			provider = EmaFactory.createOmmProvider(EmaFactory.createOmmIProviderConfig(), appClient );
			
			while( appClient.loginHandle == 0 ) Thread.sleep(1000);
			
			long rwfFld = provider.registerClient(EmaFactory.createReqMsg().name("RWFFld").filter(EmaRdm.DICTIONARY_NORMAL)
					.serviceName("DIRECT_FEED").domainType(EmaRdm.MMT_DICTIONARY), appClient);
			
			long rwfEnum = provider.registerClient(EmaFactory.createReqMsg().name("RWFEnum").filter(EmaRdm.DICTIONARY_NORMAL)
					.serviceName("DIRECT_FEED").domainType(EmaRdm.MMT_DICTIONARY), appClient);
			
			while ( appClient.itemHandle == 0 ) Thread.sleep(1000);
			
			for(int i = 0; i < 60; i++)
			{
				fieldList.clear();
				//Generate new data of all fields for an Update Message
				appClient.data.GenerateAllFields();
				//Add the REAL fields and custom REAL fields(negative field id) to the fieldList of the Update Message
				//Create a REAL field by using FieldEntry.real(int fieldId, long mantissa, int magnitudeType) method
				//Use convertDouble2NumericValue(double dVal) to convert double generated from canned data(data variable) to long(mantissa parameter) 
				//magnitudeType parameter always is OmmReal.MagnitudeType.EXPONENT_NEG_2 which power of -2 or 2 decimals.
				fieldList.add(EmaFactory.createFieldEntry().real(22, appClient.convertDouble2NumericValue(appClient.data.getBID()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
				fieldList.add(EmaFactory.createFieldEntry().real(25, appClient.convertDouble2NumericValue(appClient.data.getASK()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
				fieldList.add(EmaFactory.createFieldEntry().real(-4001, appClient.convertDouble2NumericValue(appClient.data.getBidAvgIntraDay()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
				fieldList.add(EmaFactory.createFieldEntry().real(-4002, appClient.convertDouble2NumericValue(appClient.data.getAskAvgIntraDay()), OmmReal.MagnitudeType.EXPONENT_NEG_2));
			
				provider.submit(updateMsg.clear().payload(fieldList), appClient.itemHandle );
				
				Thread.sleep(1000);
			}
		}
		catch (OmmException | InterruptedException excp)
		{
			System.out.println(excp.getMessage());
		} 
		finally 
		{
			if (provider != null) provider.uninitialize();
		}
	}
}
