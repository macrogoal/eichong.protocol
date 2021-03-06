package com.epcentre.service;


import io.netty.channel.Channel;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.ElectricPileCache;
import com.epcentre.cache.EpCommClient;
import com.epcentre.cache.EpConcentratorCache;
import com.epcentre.cache.EqVersionCache;
import com.epcentre.cache.INetObject;
import com.epcentre.config.GameConfig;
import com.epcentre.constant.EpConstantErrorCode;
import com.epcentre.dao.DB;
import com.epcentre.model.TblBomList;
import com.epcentre.model.TblConcentrator;
import com.epcentre.model.TblElectricPile;
import com.epcentre.model.TblEquipmentVersion;
import com.epcentre.model.TblTypeSpan;
import com.epcentre.protocol.EpEncodeProtocol;
import com.epcentre.protocol.EqVersionInfo;
import com.epcentre.protocol.Iec104Constant;
import com.epcentre.protocol.WmIce104Util;
import com.epcentre.sender.EpMessageSender;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.FileUtils;

public class EqVersionService {

	private static final Logger logger = LoggerFactory.getLogger(EqVersionService.class);
	
	//升级版本缓存
	public static Map<Integer, Map<String,BomListInfo>> updateHexFileMap = new ConcurrentHashMap<Integer,Map<String,BomListInfo>>();
		
	private static long lastFetchEpHexDown=0;

	
	public static  void AddBomList(Integer key,Map<String,BomListInfo> bomList)
	{
		if(key !=null && bomList!=null)
		{
			updateHexFileMap.put(key, bomList);
			logger.debug("updateHexFileMapsize:{},bomListsize:{}",updateHexFileMap.size(),bomList.size());
		}
		else
		{
			logger.error("AddBomList fail,because of,key:{},bom:{}",key,bomList);
		}
	}
	public static  Map<String,BomListInfo>  getBomList(Integer key)
	{
		if(key !=null )
		{
			return updateHexFileMap.get(key);
		}
		else
		{
			logger.error("getBomList fail,because of,key:null");
			return null;
		}
	}
	public static void removeBomList(Integer key)
	{
		updateHexFileMap.remove(key);
	}
	public static void removeAllBomList()
	{
		updateHexFileMap.clear();
	}

		
	public static void init() 
	{
		if (lastFetchEpHexDown == 0) {
			
			getTypeSpanAndBomFromDB();

			lastFetchEpHexDown = DateUtil.getCurrentSeconds();
		}
	}
	//程序初始化时、定时升级时读取升级版本信息（tbl_bomlist和tbl_typespan）
	public static void getTypeSpanAndBomFromDB()
	{
		//EqVersionService.removeAllTypeSpan();
		EqVersionService.removeAllBomList();
		// 取得全部升级版本
		List<TblTypeSpan> tblTypeSpanList = DB.typeSpanDao.getAll();
		if(tblTypeSpanList !=null && tblTypeSpanList.size()>=1)
        {
			int size = tblTypeSpanList.size();
			for (int i = 0; i < size; i++) {
				TblTypeSpan typeSpan = tblTypeSpanList.get(i);

				// 取得全部升级版本
				List<TblBomList> tblBomList = DB.bomListDao.getAllByTypeSpanId(typeSpan.getTypeSpanId());
				
				if(tblBomList !=null && tblBomList.size()>=1)
		        {
					Map<String, BomListInfo> bomMap = new ConcurrentHashMap<String, BomListInfo>();
				    for (int j = 0; j < tblBomList.size(); j++) {
					TblBomList tblBom = tblBomList.get(j);
					BomListInfo bomInfo = convertBomInfo(tblBom);
					if(bomInfo.splitHardwareVersion()>0 ||bomInfo.splitSoftVersion()>0)
					{
						logger.error("getTypeSpanAndBomFromDB: bom error,hardwareVersion:{},softVersion:{}",
								bomInfo.getHardwareVersion(),bomInfo.getSoftVersion());
					     continue;
				     }
					
					 bomMap.put(bomInfo.getHardwareNumber() + bomInfo.getHardwareVersion(),bomInfo);
				     EqVersionService.AddBomList(typeSpan.getTypeSpanId(), bomMap);
		           }
			   }
			}
		}
	}
	public static BomListInfo convertBomInfo(TblBomList tblBom)
	{
		BomListInfo bomInfo =  new BomListInfo();
		bomInfo.setHardwareNumber(tblBom.getHardwareNumber());
		bomInfo.setHardwareVersion(tblBom.getHardwareVersion());
		bomInfo.setForceUpdate(tblBom.getForceUpdate());
		bomInfo.setSoftNumber(tblBom.getSoftNumber());
		bomInfo.setSoftVersion(tblBom.getSoftVersion());
		bomInfo.setTypeSpan(tblBom.getTypeSpan());
		bomInfo.setTypeSpanId(tblBom.getTypeSpanId());
		bomInfo.setFileMd5(tblBom.getFileMd5());
		
		return bomInfo;
	}
	public static void setBomListToEp(ElectricPileCache epCache,int typeSpanId)
	{
		
		   Map<String,BomListInfo> bomMap = getBomList(typeSpanId);
		   if(bomMap !=null)
		   {
			   logger.debug("bomMapsize:{},typeSpanId:{}",bomMap.size(),typeSpanId);
		     ////将bomlist复制到桩对象bomlist中
			   epCache.getVersionCache().setBomMap(bomMap);
		   }
	 	
	}
	public static void setBomListToStation(EpConcentratorCache stationCache,int typeSpanId)
	{
		
		   Map<String,BomListInfo> bomMap = getBomList(typeSpanId);
		   if(bomMap !=null)
		   {
		     ////将bomlist复制到桩对象bomlist中
			   stationCache.getVersionCache().setBomMap(bomMap);
		   }
	 	
	}
	
	//电桩或集中器连接初始化时读取数据库tbl_equipmentversion表
	public static void getEquipmentVersionFromDB(ElectricPileCache epCache,int stationAddr,int type)
	{
		if(type==1)//电桩
		{
		   if(epCache ==null)
		   {
			 return;
		   }
		   TblEquipmentVersion equitment = new TblEquipmentVersion();
		   equitment.setProductID(epCache.getPkEpId());
		   equitment.setProductType(type);
		   List<TblEquipmentVersion> equitMentVerList = DB.equipmentVersionDao.findEqVersion(equitment);
		   if(equitMentVerList != null && equitMentVerList.size()>=1)
		   {
			
			for(int i=0;i<equitMentVerList.size();i++)
			{
				EqVersionInfo  verinfo= new EqVersionInfo();
			    equitment = equitMentVerList.get(i);
			    
			    verinfo.setPk_EquipmentVersion(equitment.getPkEquipmentVersion());
			    verinfo.setEpCode(epCache.getCode());
			    verinfo.setType(type);
			    verinfo.setHardwareNumber(equitment.getHardwareNumber());
			    verinfo.setHardwareVersion(equitment.getHardwareVersion());
			    verinfo.setSoftNumber(equitment.getFirmwareNumber());
			    verinfo.setSoftVersion(equitment.getFirmwareVersion());
			    epCache.getVersionCache().addEpVersion(verinfo.getHardwareNumber()+verinfo.getHardwareVersion(), verinfo);
			}
			//设置bomList到设备
			setBomListToEp(epCache,epCache.getTypeSpanId());
		  }
	   }  
	   if(type ==2) //集中器
	   {
			EpConcentratorCache stationCache = EpConcentratorService.getConCentrator(stationAddr);
			if(stationCache ==null)
			{
				return;
			}
			TblEquipmentVersion equitment = new TblEquipmentVersion();
			equitment.setProductID(stationCache.getPkId());
			equitment.setProductType(type);
			List<TblEquipmentVersion> equitMentVerList = DB.equipmentVersionDao.findEqVersion(equitment);
			if(equitMentVerList != null && equitMentVerList.size()>=1)
			{
				EqVersionInfo  verinfo= new EqVersionInfo();
				for(int i=0;i<equitMentVerList.size();i++)
				{
				    equitment = equitMentVerList.get(i);
				    verinfo.setPk_EquipmentVersion(equitment.getPkEquipmentVersion());
				    verinfo.setStationAddr(stationAddr);
				    verinfo.setType(type);
				    verinfo.setHardwareNumber(equitment.getHardwareNumber());
				    verinfo.setHardwareVersion(equitment.getHardwareVersion());
				    verinfo.setSoftNumber(equitment.getFirmwareNumber());
				    verinfo.setSoftVersion(equitment.getFirmwareVersion());
				}
				stationCache.getVersionCache().addEpVersion(verinfo.getHardwareNumber()+verinfo.getHardwareVersion(), verinfo);
				//设置bomList到站
				setBomListToStation(stationCache,stationCache.getTypeSpanId());
			}
		}
	}
	public static void checkEpModifyTypeSpan()
	{
		List<TblElectricPile> epList = DB.epClientDao.getLastUpdate();
		int size =epList.size();
		for(int i=0;i<size;i++)
		{
			TblElectricPile epInfo=epList.get(i);
			ElectricPileCache epCache = EpService.getMapEpCache().get(epInfo.getEpCode());
			epCache.setTypeSpanId(epInfo.getEpTypeSpanId());
		}
		
		
	}
	public static void onTimerUpdateHexFile()
	{
		getTypeSpanAndBomFromDB();
		 Iterator iter = updateHexFileMap.entrySet().iterator();
	 	while (iter.hasNext()) {
           Map.Entry entry = (Map.Entry) iter.next();
 			
 			int typeSpanId= (int)entry.getKey();
	 	
		    //查询所有电桩
		    queryAllEpVerion(typeSpanId);
		    //查询所有集中器
		    queryAllStationVerion(typeSpanId);
	 	}
	}
	 public  static void saveEqVersiontoDB(EqVersionInfo verinfo,int pkId,int type,EqVersionCache eqVerCache)
	 {
		 if(verinfo == null || eqVerCache == null)
		 {
			 return;
		 }
		 String key = verinfo.getHardwareNumber()+verinfo.getHardwareVersion();
		 TblEquipmentVersion equipment = new TblEquipmentVersion();
		 equipment.setProductID(pkId);
		 equipment.setProductType(type);
		 equipment.setFirmwareNumber(verinfo.getSoftNumber());
		 equipment.setFirmwareVersion(verinfo.getSoftVersion());
		 equipment.setHardwareNumber(verinfo.getHardwareNumber());
		 equipment.setHardwareVersion(verinfo.getHardwareVersion());
		 equipment.setUpdatedate(DateUtil.currentDate());
		 EqVersionInfo ver = eqVerCache.getEpVersion(key);
		 if(ver !=null &&(ver.getSoftVersion().compareTo(verinfo.getSoftVersion())!=0 ||
					ver.getSoftNumber().compareTo(verinfo.getSoftNumber())!=0))
		 {
			 DB.equipmentVersionDao.updateEqVersion(equipment);
			 logger.info("handleVersionAck updateEqVersion pkId:{},type:{}",pkId,type);
		 }
		 else if(ver == null)
		 {
			 DB.equipmentVersionDao.insertEqVersion(equipment);
			 verinfo.setPk_EquipmentVersion(equipment.getPkEquipmentVersion());
			 logger.info("handleVersionAck insertEqVersion pkId:{},type:{}",pkId,type);
				
		 }
		 eqVerCache.addEpVersion(key,verinfo);
	 }

	// 处理桩回复版本信息
	public static void handleVersionAck(EpCommClient epCommClient, String epCode,int stationAddr,Vector<EqVersionInfo> verInfos) {

		logger.debug("handleVersionAck epCode:{},stationAddr:{}",epCode,stationAddr);
		if(verInfos == null)
	    {
		    logger.error("handleVersionAck fail verInfos==null, epCode:{},stationAddr:{}",epCode,stationAddr);
		    return;
	    }
		EqVersionCache eqVerCache;
		int pkId;
		int type;
		if(stationAddr>0) //处理站
		{
			EpConcentratorCache stationCache = EpConcentratorService.getConCentrator(epCommClient.getChannel());
			if(stationCache ==null)
			{
				 logger.error("handleVersionAck fail stationCache ==null, stationAddr:{}",stationAddr);
				return;
			}
			eqVerCache = stationCache.getVersionCache();
		    pkId = stationCache.getPkId();
		    type=2;
		}
		else
		{
			//更新电桩表中产品型号,整个产品型号
		    ElectricPileCache epCache = EpService.getEpByCode(epCode);
		    if(epCache ==null)
		    {
			    logger.error("handleVersionAck fail epCache ==null, epCode:{}",epCode);
			    return;
		    }
		    
		    eqVerCache = epCache.getVersionCache();
		    pkId = epCache.getPkEpId();
		    type=1;
		}
		//比较数据库中硬件版本，如硬件已换，旧硬件删除
		eqVerCache.removeEpVersion(verInfos);//
		int size = verInfos.size();
		for(int i=0;i<size;i++)
		{
			EqVersionInfo info = verInfos.get(i);
			saveEqVersiontoDB(info,pkId,type,eqVerCache);			
		}
		forceUpdateHexFile(epCode,stationAddr);
	}	
    
  
    public static void handleUpdateAck(int stationAddr,String epCode,EqVersionInfo info) 
    {
    	if(info ==null)
		{
    		logger.error("handleUpdateAck info ==null epcode:{},stationAddr:{}",epCode,stationAddr);
           return;
		}
    	EqVersionCache eqVerCache;
		int pkId;
		int type;
		if(stationAddr>0)//站
    	{
    		EpConcentratorCache stationCache = EpConcentratorService.getConCentrator(stationAddr);
    		if(stationCache ==null)
    		{
    			logger.error("handleUpdateAck stationCache ==null epcode:{},stationAddr:{}",epCode,stationAddr);
    			return;
    		}
    		pkId = stationCache.getPkId();
    		eqVerCache=stationCache.getVersionCache();
    		type=2;
    	}
		else//电桩
    	{
    		ElectricPileCache epCache = EpService.getEpByCode(epCode);
    		if(epCache ==null)
    		{
    			logger.error("handleUpdateAck epCache ==null epcode:{},stationAddr:{}",epCode,stationAddr);
    			return;
    		}
    		pkId = epCache.getPkEpId();
    		eqVerCache=epCache.getVersionCache();
    		type=1;
    	}
		saveEqVersiontoDB(info,pkId,type,eqVerCache);	
    }
   
     public synchronized static int queryAllEpVerion(int typeSpanId){
  
    	 int error =1;
    	 
    	 Iterator iter = EpService.getMapEpCache().entrySet().iterator();
 		
 		while (iter.hasNext()) {

 			Map.Entry entry = (Map.Entry) iter.next();
 			
 			ElectricPileCache epClient=(ElectricPileCache) entry.getValue();
		
	    	if(epClient == null )
		    {
	    		logger.error("queryAllEpVerion: epClient = null");
			   continue;
		    }
	    	
	    	if(typeSpanId!=epClient.getTypeSpanId())
	    	{
	    		logger.error("queryAllEpVerion: typeSpanId != epClient.getTypeSpanId(),typeSpanId:{},epTypeSpanId:{}",typeSpanId,epClient.getTypeSpanId());
	    		continue;
	    	}
	    	Map<String,BomListInfo> bomMap = getBomList(typeSpanId);
	    	////将bomlist设置到电桩上
	    	epClient.getVersionCache().setBomMap(bomMap);
	    	
		    INetObject commClient = epClient.getEpNetObject();
		    if(commClient==null || !commClient.isComm())
		    {
			   logger.error("queryEpVersion fail,epClient commClient is null!epCode:{}",epClient.getCode());
			   continue ;
			
		    }
		    queryVersion((EpCommClient)commClient,epClient.getCode(),0);
		    error =0;
 		}
 		return error;
	}
   //查询标志，queryFlag=1查询所有集中器，queryFlag=0查询指定产品类型集中器
     public synchronized static int queryAllStationVerion(int typeSpanId){
    	 
    	 int error =1;
    	
    	 Iterator iter = EpConcentratorService.getMapCh2Station().entrySet().iterator();
 		
 		while (iter.hasNext()) {

 			Map.Entry entry = (Map.Entry) iter.next();
 			
 			EpConcentratorCache stationClient=(EpConcentratorCache) entry.getValue();
		
	    	if(stationClient == null )
		    {
	    		logger.error("queryAllStationVerion fail1,stationClient= null");
			   continue;
		    }
	    	
	    	if(typeSpanId !=stationClient.getTypeSpanId())
	    	{
	    		logger.error("queryAllStationVerion fail2,stationAddr:{}",stationClient.getPkId());
	    		continue;
	    	}
	    	Map<String,BomListInfo> bomMap = getBomList(typeSpanId);
	    	
	    	//将bomlist设置到站
	    	stationClient.getVersionCache().setBomMap(bomMap);
		    Channel channel =(Channel)entry.getKey();
		    
		    if(channel==null )
		    {
			   logger.error("queryStationVersion fail,station commClient is null!stationAddr:{}",stationClient.getPkId());
			   continue ;
			
		    }
		    EpCommClient  commClient = EpCommClientService.getCommClient(""+stationClient.getPkId());
		    if(commClient == null)
		    {
		    	logger.error("queryStationVersion fail,station commClient is null!stationAddr:{}",stationClient.getPkId());
				continue ;
		    }
		    queryVersion(commClient,"0000000000000000",stationClient.getPkId());
		    error =0;
 		}
 		return error;
	}
     
    public static void forceUpdateHexFile(String epCode,int stationAddr)
 	{
    	 if(epCode.compareTo("0000000000000000")==0 &&(stationAddr==0))
    	 {
    		 logger.info("forceUpdateHexFile fail1,epCode:{},stationAddr:{}",epCode,stationAddr);
    		 return;
    	 }
    	 Map<String,BomListInfo> bomMap=null;
    	 EpConcentratorCache stationClient=null;
    	 ElectricPileCache epCache=null;
    	 //处理站
    	 if(stationAddr>0)
    	 {
    		 stationClient=EpConcentratorService.getConCentrator(stationAddr);	
 	    	 if(stationClient == null )
 		     {
 	    		logger.debug("forceUpdateHexFile fail2,stationClient=null");
 			    return;
 		     }
 	    	bomMap = stationClient.getVersionCache().getMapBomList();
    	 }
    	 //处理电桩
    	 else
    	 {
    		 epCache = EpService.getEpByCode(epCode);
    		 if(epCache ==null)
    		 {
    			 logger.debug("forceUpdateHexFile fail2,epCache=null");
    			 return;
    		 }
    		 bomMap = epCache.getVersionCache().getMapBomList();
    	 }
    	 if(bomMap == null)
    	 {
    		 logger.error("forceUpdateHexFile bomMap =null, epCode:{},stationAddr:{}",epCode,stationAddr);
    		 return;
    	 }
        //该电桩下所有的升级版本和电桩现有版本对照判断
 		Iterator iter = bomMap.entrySet().iterator();
 		
 		while (iter.hasNext()) {
 			
 			Map.Entry entry = (Map.Entry) iter.next();
 			
 			BomListInfo bom=(BomListInfo) entry.getValue();
    		//发送升级标志，1发送，0不发送
    		int sendFlag = 0;
    		String key = bom.getHardwareNumber()+bom.getHardwareVersion();
    		EqVersionInfo verinfo=null;
    		if(stationAddr>0)
       	    {
    			verinfo= stationClient.getVersionCache().getEpVersion(key);
       	    }
    		else
    		{
    			verinfo= epCache.getVersionCache().getEpVersion(key);
    		}
    		if(verinfo==null)
    		{//电桩上无该硬件
    			logger.error("forceUpdateHexFile fail,not find hardware from ep, bom hardwareNumber:"+bom.getHardwareNumber()
    					+",hardwareVersion:"+bom.getHardwareVersion()+",epCode:"+
    					epCode+",stationAddr:"+stationAddr+",key:"+key);
    			 continue;
    		}
    		else
    		{
     			String bomSoft = bom.getSoftNumber()+bom.getSoftVersion();
    			String epSoft = verinfo.getSoftNumber()+verinfo.getSoftVersion();
    			if(bomSoft.compareTo(epSoft)==0)//版本相同
    			{
    				logger.error("forceUpdateHexFile fail,softversion is same,softVersion:"+bomSoft+",hardwareNumber:"+
    			      bom.getHardwareNumber()+",hardwareVersion:"+bom.getHardwareVersion()+",epCode:"+
        					epCode+",stationAddr:"+stationAddr);
    					 continue;
    			}
    			if(bom.getForceUpdate()==1)//版本不同但强制升级
    			{
    				sendFlag=1;
    			}
    			else//判断版本高低，升级版本高就升
    			{
    				int bomSoftver = bom.getSoftM()*100000+bom.getSoftA()*1000+bom.getSoftC();
        			int epSoftver = verinfo.getSoftM()*100000+verinfo.getSoftA()*1000+verinfo.getSoftC();
                    if(bomSoftver>epSoftver)
                    {
                        sendFlag=1;  
                        logger.debug("forceUpdateHexFile,bomSoftver>epSoftver,bom.soft:{},epSoftver:{}",bomSoftver,epSoftver);
                        logger.debug("forceUpdateHexFile,bomSoftver>epSoftver,bom.getSofta:{},bom.getSoftC:{}",bom.getSoftA(),bom.getSoftC());
                        logger.debug("forceUpdateHexFile,bomSoftver>epSoftver,verinfo.getSoftM():{},verinfo.getSoftA():{}",verinfo.getSoftM(),verinfo.getSoftA());
                        logger.debug("forceUpdateHexFile,bomSoftver>epSoftver,verinfo.getSoftC:{}",verinfo.getSoftC());
                    }
                    else
                    {
                    	logger.error("forceUpdateHexFile fail,bomversion is Low,bom hardwareNumber:"+bom.getHardwareNumber()
            					+",hardwareVersion:"+bom.getHardwareVersion()+",epCode:"+
            					epCode+",stationAddr:"+stationAddr+",bom softVersion:"+bomSoftver
            					+",ep softVersion:"+epSoftver);
                    }
                    	
    			 }
    			
    		 }
    		 if(sendFlag ==1)
			 {
				 forceUpdateHexFile(epCode,stationAddr,bom.getHardwareNumber(),bom.getHardwareM(),bom.getHardwareA()); 
			 }
    	}

 		
 	}
 
    
    public static int forceUpdateHexFile(String epCode, int stationAddr,String hardwareNumber,int hardwareM,int hardwareA)
	{
    	EpCommClient commClient = EpCommClientService.getCommClient(epCode);
		if(commClient==null || !commClient.isComm())
		{
			commClient = EpCommClientService.getCommClient(""+stationAddr);
		}
		if(commClient==null || !commClient.isComm())
		{
			 logger.debug("forceUpdateHexFile fail,commClient==null || !commClient.isComm()");
			return 1;
		}
		
		try {
		
		 byte[] msg = EpEncodeProtocol.do_force_update_ephex((short)stationAddr,epCode, hardwareNumber,
				 hardwareM, hardwareA);
		
		if(msg ==null)
		{
			logger.error("forceUpdateEpHex fail msg ==null");
			return 1;
		}
	    byte[] cmdTimes = WmIce104Util.timeToByte();
		EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_FORCE_UPDATE_EP_HEX, msg,cmdTimes,commClient.getCommVersion());
		
		logger.info("forceUpdateHexFile send hardwareNumber:"+hardwareNumber
					+",hardwareVersion:"+hardwareM+"."+hardwareA+",epCode:"+
					epCode+",stationAddr:"+stationAddr);
		return 0;
		}
		catch (NumberFormatException e)
		{ 
			logger.error("forceUpdateEpHex fail exception");
			e.printStackTrace();
			return 1;
			}
		
	}
	  
	public static void handleEpHexFileSumaryReq(Channel channel, String epCode,short stationAddr,
			String hardwareNumber,int hardwareM,int hardwareA,int len,byte []cmdTimes)
	{
		logger.debug("into handleEpHexFileSumaryReq ");
	   EpCommClient commClient = EpCommClientService.getCommClientByChannel(channel);
	   if(commClient==null || !commClient.isComm())
	   {
		    logger.info("handleEpHexFileSumaryReq fail,commClient==null || commClient is close,epCode:{},stationAddr:{}",
					epCode,stationAddr);
			return;
	   }
		if (stationAddr ==0 && epCode.compareTo("0000000000000000")==0 )
    	{
			logger.info("handleEpHexFileSumaryReq fail did not find epCode:{},stationAddr:{}",
					epCode,stationAddr);
			 return ;
    	}
		
		int productid = 0;
		int type = 0;
		int typeSpanId=0;
		BomListInfo bom = null;
		Map<String,BomListInfo> bomMap = null;
		
		if(stationAddr>0)
		{
		    EpConcentratorCache stationCache = EpConcentratorService.getConCentrator(channel);
			if(stationCache==null)
			{
				logger.info("handleEpHexFileSumaryReq fail not find EpStationCache,stationAddr:{}",stationAddr);
				return ;
			}
			productid = stationCache.getPkId();
			type = 2;//集中器
			typeSpanId=stationCache.getTypeSpanId();
			bomMap = stationCache.getVersionCache().getMapBomList();
		 }
		 else
		 {
		    ElectricPileCache epCache = EpService.getEpByCode(epCode);
			if(epCache == null)
			{
				logger.info("handleEpHexFileSumaryReq fail not find epCache,epCode:{}",epCode);
				return ;
			}
			productid = epCache.getPkEpId();
			type=1; //电桩
			typeSpanId=epCache.getTypeSpanId();
			bomMap = epCache.getVersionCache().getMapBomList();
		 }
		String filename = null;
		String key = hardwareNumber+hardwareM+"."+hardwareA;
	
		if(bomMap ==null)
		{
			logger.info("handleEpHexFileSumaryReq fail not find bom,typeSpanId:{}",typeSpanId);
			return ;
		}
		
		bom = bomMap.get(key);
		if(bom==null)
		{
			logger.info("handleEpHexFileSumaryReq fail not find bom,key:{}",key);
			return ;
		}
		
        filename = GameConfig.epExePath;
			
			    String softNumber = bom.getSoftNumber();
				int softM = bom.getSoftM();
				int softA = bom.getSoftA();
				int softC=bom.getSoftC();
				String softVersion=bom.getSoftVersion();
				String Md5Value=bom.getFileMd5();

		filename = filename + softNumber + "-V"+ softVersion + ".bin";
			
			byte[] msg;
			int file_len = FileUtils.getFileSize(filename);
			
			if (file_len == 0) {
				msg = EpEncodeProtocol.do_ep_hex_file_sumary(epCode,
						stationAddr, hardwareNumber, hardwareM, hardwareA,
						softNumber, softM, softA, softC, 0, 0, (short) 0, 0,"");
				
				logger.info("handleEpHexFileSumaryReq---file not find filename:{},epcode:{}",filename,epCode);
				

			} else {
				byte []SectionData = FileUtils.getBinaryInfo(filename, 0, file_len);
				short sections = getEpHexFileSections(file_len, len);
				msg = EpEncodeProtocol.do_ep_hex_file_sumary(epCode,
						stationAddr, hardwareNumber, hardwareM, hardwareA,
						softNumber, softM, softA, softC, 1, file_len, sections,
						1,Md5Value);
				
				logger.info("handleEpHexFileSumaryReq---success filename:{},epcode:{} ",filename,epCode);
			}
			if (msg == null) {
				logger.info("do_ep_hex_file_sumary exception,filename:{},epcode:{}",
						filename,epCode);
				return;
			}

		EpMessageSender.sendMessage(commClient, 0, 0,Iec104Constant.C_EP_HEX_FILE_SUMARY, msg,cmdTimes,commClient.getCommVersion());

	}
	public static String  getHexFileNameFromBomList(int type,int typeSpanId,
			String hardwareNumber,int hardwareM,int hardwareA)
	{
		String filename = null;
		Map<String,BomListInfo> bomMap =  getBomList(typeSpanId);
		if(bomMap ==null)
		{
			logger.error("handleEpHexFileSumaryReq fail not find bom,typeSpanId:{}",typeSpanId);
			return null;
		}
		String key = hardwareNumber+hardwareM+"."+hardwareA;
		BomListInfo bom = bomMap.get(key);
		if(bom==null)
		{
			logger.error("handleEpHexFileSumaryReq fail not find bom,key:{}",key);
			return null;
		}
        filename = GameConfig.epExePath;
			
			    String softNumber = bom.getSoftNumber();
				int softM = bom.getSoftM();
				int softA = bom.getSoftA();
				int softC=bom.getSoftC();
				String softVersion=bom.getSoftVersion();
				String Md5Value=bom.getFileMd5();

		filename = filename + softNumber + "-V"+ softVersion + ".bin";
		return filename;
	}
	public static short getEpHexFileSections(int file_len,int len)
	{
		if(file_len<1 || len<1)
			return (short)0;
		short sections = (short) (file_len / len);
		int remain = file_len % len;
		if (remain > 0)
			sections += 1;
		
		return sections;
		
	}
	public static short calcFileDownSectionLen(int file_len,int SectionIndexReq,int len)
	{
		if(file_len<1)
			return 0;

		int sections = getEpHexFileSections(file_len,len);
		
		if (SectionIndexReq < 1 || SectionIndexReq > sections) {
			return 0;
		}

		if (SectionIndexReq == sections) {
			return (short) (file_len - (sections - 1)
					* len);
		}
		else
		{
			return (short)len;
		}
	}

	public static void handleEpHexFileDownReq(EpCommClient epCommClient, String epCode,short stationAddr,
			int SectionIndexReq,EqVersionInfo versionInfo,int sectionLeng,byte []cmdTimes) {
		
		if (stationAddr == 0 && epCode.compareTo("0000000000000000") == 0) {
			logger.error("handleEpHexFileDownReq did not find eq:epCode={},,stationAddr:",
					epCode,stationAddr);
			return;
		}
		int productid = 0;
		int type = 0;
		EpCommClient commClient = EpCommClientService.getCommClientByChannel(epCommClient.getChannel());
		if (commClient == null || !commClient.isComm())
		{
			logger.error("handleEpHexFileDownReq fail,commClient==null||commClient is close,channel:{}",
					epCommClient);
			return;
		}
		ElectricPileCache epCache = EpService.getEpByCode(epCode);
		if (epCache == null) {
			EpConcentratorCache stationCache = EpConcentratorService.getConCentrator(epCommClient.getChannel());
			if (stationCache == null) {
				logger.error("handleEpHexFileDownReq not find EpStationCache,stationAddr:{}",stationAddr);
				return;
			}
			productid = stationCache.getPkId();
			type = 2;// 集中器
		} else {
			productid = epCache.getPkEpId();
			type = 1; // 电桩
		}

		String filename = GameConfig.epExePath;
		
		String softCode = versionInfo.getSoftNumber();
		
		
		filename = filename + softCode+"-V"+ versionInfo.getSoftVersion() + ".bin";

		//filename
		int file_len = FileUtils.getFileSize(filename);

		short len = calcFileDownSectionLen(file_len,SectionIndexReq,sectionLeng);
		
		byte[] msg;
		byte[] SectionData=null;
		if(len<=0)
		{
			msg = EpEncodeProtocol.do_ep_hex_file_down(epCode,stationAddr,
					versionInfo.getSoftNumber(),
					versionInfo.getSoftM(),
					versionInfo.getSoftA(),
					(short)versionInfo.getSoftC(),
					(short)SectionIndexReq,(short)0, SectionData,0);
			logger.error("handleEpHexFileDownReq fail len<=0,softNumber:{},softVersion:{}",versionInfo.getSoftNumber(),versionInfo.getSoftVersion());
		}
		else
		{
			int offset = (SectionIndexReq - 1)
					* sectionLeng;
			
			 SectionData = FileUtils.getBinaryInfo(filename, offset, len);
			msg = EpEncodeProtocol.do_ep_hex_file_down(epCode,stationAddr,
					versionInfo.getSoftNumber(),
					versionInfo.getSoftM(),
					versionInfo.getSoftA(),
					(short)versionInfo.getSoftC(),
					(short)SectionIndexReq,  (short)len, SectionData,1);
		}
		if(msg ==null)
		{
			logger.error("do_ep_hex_file_down exception");
			return ;
		}
	
		EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_EP_HEX_FILE_SECTION, msg,cmdTimes,commClient.getCommVersion());
		

	}	
	 public static int queryVersion(EpCommClient commClient,String epCode,int stationAddr)
	    {
	    	if (stationAddr ==0 && epCode.compareTo("0000000000000000")==0 )
	    	{
				 return EpConstantErrorCode.INVALID_EP_CODE;
	    	}
	    	if(commClient == null)
	    		return EpConstantErrorCode.EP_UNCONNECTED;
	
			
			byte[] data= EpEncodeProtocol.do_eqversion_req(epCode, (short)stationAddr);
			byte[] cmdTimes = WmIce104Util.timeToByte();
			EpMessageSender.sendMessage(commClient,0,0,Iec104Constant.C_DEVICE_VERSION_REQ, data,cmdTimes,commClient.getCommVersion());
				
			
		    return 0;
	    }
	
	 public  static void deleteEqVersionFromDB(int id)
	 {
		 DB.equipmentVersionDao.deleteEqVersion(id);
	 }
	
     public  static void queryAllEpByTypeSpanID(int typeSpanId){
  	 
    	 List<TblElectricPile> epPileList=DB.epClientDao.findResultObjectBySpanId(typeSpanId);
    	 if(epPileList ==null ||epPileList.size()<1)
    	 {
    		 logger.error("queryAllEpByTypeSpanID not find ep from DB typeSpanId:{}",typeSpanId);
    	 }
    	 for(int i=0;i<epPileList.size();i++)
    	 {
    		 TblElectricPile ep= epPileList.get(i);
    		 ElectricPileCache epClient=EpService.getEpByCode(ep.getEpCode());
    		 if(epClient == null )
 		     {
 	    		logger.error("queryAllEpByTypeSpanID epClient = null");
 			   continue;
 		     }
    		 epClient.setTypeSpanId(typeSpanId);
    		 Map<String,BomListInfo> bomMap = getBomList(typeSpanId);
 	    	 ////将bomlist复制到桩对象bomlist中
 	    	 epClient.getVersionCache().setBomMap(bomMap);
 	    	
 		    INetObject commClient = epClient.getEpNetObject();
 		    if(commClient==null || !commClient.isComm())
 		    {
 			   logger.error("queryAllEpByTypeSpanID fail,epClient commClient is null!epCode:{}",epClient.getCode());
 			   continue ;
 			
 		    }
 		    queryVersion((EpCommClient)commClient,epClient.getCode(),0); 
    	 }
	}
     public  static void queryAllStaionByTypeSpanID(int typeSpanId)
     {
      	 
   	     List<TblConcentrator> centList=DB.concentratorDao.findResultObjectBySpanId(typeSpanId);
   	     if(centList ==null ||centList.size()<1)
    	 {
    		 logger.error("queryAllEpByTypeSpanID not find ep from DB typeSpanId:{}",typeSpanId);
    	 }
    	 for(int i=0;i<centList.size();i++)
    	 {
    		 TblConcentrator concentrator= centList.get(i);
    		 EpConcentratorCache stationClient=EpConcentratorService.getConCentrator(concentrator.getPkConcentratorID());
    		 if(stationClient == null )
 		     {
 	    		logger.error("queryAllEpByTypeSpanID epClient = null");
 			    continue;
 		     }
    		 stationClient.setTypeSpanId(typeSpanId);
    		 
    		 Map<String,BomListInfo> bomMap = getBomList(typeSpanId);
 	    	
 	    	 //将bomlist复制到站对象bomlist中
 	    	 stationClient.getVersionCache().setBomMap(bomMap);		    
 		     EpCommClient  commClient = EpCommClientService.getCommClient(""+stationClient.getPkId());
 		     if(commClient == null)
 		     {
 		    	 logger.error("queryStationVersion fail,station commClient is null!stationAddr:{}",stationClient.getPkId());
 				 continue ;
		     }
    	     queryVersion(commClient,"0000000000000000",stationClient.getPkId());
   	    }
     }
    	 
    
}
