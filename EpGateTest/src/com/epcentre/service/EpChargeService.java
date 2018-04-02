package com.epcentre.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.cache.ChargeCache;
import com.epcentre.cache.ChargeCarInfo;
import com.epcentre.cache.ChargePowerModInfo;
import com.epcentre.cache.ElectricPileCache;
import com.epcentre.cache.EpCommClient;
import com.epcentre.cache.EpGunCache;
import com.epcentre.cache.UserCache;
import com.epcentre.cache.UserOrigin;
import com.epcentre.cache.UserRealInfo;
import com.epcentre.config.GameConfig;
import com.epcentre.config.Global;
import com.epcentre.constant.ChargeStatus;
import com.epcentre.constant.EpConstant;
import com.epcentre.constant.EpConstantErrorCode;
import com.epcentre.constant.EpProtocolConstant;
import com.epcentre.constant.EventConstant;
import com.epcentre.dao.DB;
import com.epcentre.model.RateInfo;
import com.epcentre.model.TblChargingOrder;
import com.epcentre.model.TblChargingfaultrecord;
import com.epcentre.model.TblChargingrecord;
import com.epcentre.model.TblElectricPileGun;
import com.epcentre.model.TblPowerModule;
import com.epcentre.model.TblVehicleBattery;
import com.epcentre.protocol.ChargeCmdResp;
import com.epcentre.protocol.ChargeEvent;
import com.epcentre.protocol.EpEncodeProtocol;
import com.epcentre.protocol.Iec104Constant;
import com.epcentre.protocol.NoCardConsumeRecord;
import com.epcentre.protocol.UtilProtocol;
import com.epcentre.sender.EpMessageSender;
import com.epcentre.test.ImitateEpService;
import com.epcentre.utils.DateUtil;
import com.epcentre.utils.StringUtil;

public class EpChargeService {
	
	private static final Logger logger = LoggerFactory.getLogger(EpChargeService.class);
	
	private static final String ChargeYearDateFmt = "yyyy-MM-dd HH:mm:ss";
	private static final String ChargeDayShortDateFmt = "MM-dd HH:mm";
	
	public static boolean isValidAddress(int address,int type)
	{
		//logger.debug("into isValidAddress address:{},type:{}",address,type);
		boolean ret= true;
		switch(type)
		{
		case 1:
		{
			if(address>=13 && address <=16)//保留
			{
				ret= false;
			}
			else if(address> 21)//保留
			{
				ret= false;
			}
		}
		break;
		case 3:
		{
			if(address>=5 && address <=8)//保留
			{
				ret= false;
			}
			else if(address> 16)//保留
			{
				ret= false;
			}
		}
		break;
		case 11:
		{
			if(address>8 && address <=16)//保留
			{
				ret= false;
			}
			else if(address>=37 && address <=40)//保留
			{
				ret = false;
			}
			else if(address>=50 && address <=128)//保留
			{
				ret = false;
			}
			
			
		}
		break;
		case 132:
		{
			if(address>=5 && address <=8)//保留
			{
				ret= false;
			}
			else if(address>=11)//保留
			{
				 ret= false;
			}
		}
		break;
		default:
			ret= false;
			break;
		}
		
		//logger.debug("isValidAddress ret:{}",ret);
		return ret;
		
	}
	
	
	public static ChargeCache convertFromDb(TblChargingrecord tblChargeRecord)
	{
		if(tblChargeRecord ==null)
			return null;
		ChargeCache chargeCache = new ChargeCache();
	
		chargeCache.setSt(tblChargeRecord.getChreStartdate().getTime()/1000);
		
		chargeCache.setStatus(tblChargeRecord.getStatus());
		
		
		chargeCache.setAccount(tblChargeRecord.getUserPhone());
		chargeCache.setBespNo(tblChargeRecord.getChreBeginshowsnumber());
		chargeCache.setChargeSerialNo(tblChargeRecord.getChreTransactionnumber());
		
		chargeCache.setChOrCode(tblChargeRecord.getChreCode());
		
		chargeCache.setUserId(tblChargeRecord.getUserId());
		chargeCache.setPkUserCard(tblChargeRecord.getPkUserCard());
		if(chargeCache.getPkUserCard() !=0)
		{
			chargeCache.setStartChargeStyle((short)EpConstant.CHARGE_TYPE_CARD);
		}
		else
		{
			chargeCache.setStartChargeStyle((short)EpConstant.CHARGE_TYPE_QRCODE);
		}
		
		
		BigDecimal value= tblChargeRecord.getFrozenAmt().multiply(Global.DecTime2);
		chargeCache.setFronzeAmt(value.intValue());
		chargeCache.setPayMode(tblChargeRecord.getPayMode());
		
		UserOrigin userOrigin = new UserOrigin(tblChargeRecord.getUserOrgNo(),1,"");
		chargeCache.setUserOrigin(userOrigin);
		
		RateInfo rateInfo = new RateInfo();
		rateInfo.setJ_Rate(tblChargeRecord.getJPrice());
		rateInfo.setF_Rate(tblChargeRecord.getFPrice());
		rateInfo.setP_Rate(tblChargeRecord.getPPrice());
		rateInfo.setG_Rate(tblChargeRecord.getGPrice());
		rateInfo.setG_Rate(tblChargeRecord.getGPrice());
		rateInfo.setQuantumDate(tblChargeRecord.getQuantumDate());
		
		chargeCache.setRateInfo(rateInfo);
		
		return chargeCache;
	}
	
	public static ChargeCache GetUnFinishChargeCache(String epCode,int epGunNo)
	{
		TblChargingrecord tblQueryChargeRecord= new TblChargingrecord();
		tblQueryChargeRecord.setChreUsingmachinecode(epCode);
		
		tblQueryChargeRecord.setChreChargingnumber(epGunNo);
		
		List<TblChargingrecord> chargeList = DB.chargingrecordDao.getUnFinishedCharge(tblQueryChargeRecord);
		logger.debug("chargeList count:{}",chargeList.size());
		TblChargingrecord tblChargeRecord=null;
		if (chargeList != null && chargeList.size() > 0) {
			tblChargeRecord = chargeList.get(0);
		}
		
		if(tblChargeRecord==null)
			return null;
		
		return convertFromDb(tblChargeRecord);
	}
	
	public static int getChargeOrderStatus(String serialNo)
	{
		String ret = DB.chargeOrderDao.getOrderStatus(serialNo);
		return (ret==null)?-1:Integer.parseInt(ret);
	}
	
	public static ChargeCache GetChargeCacheFromDb(String serialNo)
	{
		TblChargingrecord tblChargeRecord=null;
		String chreTransactionnumber = serialNo;
		List<TblChargingrecord> chargeList = DB.chargingrecordDao.getByTranNumber(chreTransactionnumber);
		logger.debug("chargeList count:{}",chargeList.size());
		if (chargeList != null && chargeList.size() > 0) {
			tblChargeRecord = chargeList.get(0);
		}
		
		if(tblChargeRecord==null)
			return null;
		
		return convertFromDb(tblChargeRecord);
	}
	
	/**
	 * api开始充电
	 * @param epCode
	 * @param epGunNo
	 * @param accountId
	 * @param account
	 * @param bespNo
	 * @param ermFlag
	 * @param appClientIp
	 * @param frozenAmt
	 * @param source,但来自于爱充的用户需要收费，来自于其他合作伙伴有可能不冻结钱.只记录充电和消费记录
	 * @return
	 */
	public static int apiStartElectric(String epCode,int epGunNo,int accountId,String account,String bespNo,
			short startChargeStyle,int pkUserCard,
			int frozenAmt,int payMode,
			int orgNo,int fromSource,String actionIdentity,byte [] cmdTimes)
	{
		//1.电桩编号长度不对，不能充电
		if(epCode.length() !=16)
		{
			return EpConstantErrorCode.INVALID_EP_CODE;
		}
		//2.用户长度不能充电
		int lenOfAccount=account.length();
		if(lenOfAccount != EpProtocolConstant.LEN_ACCOUNT &&  lenOfAccount != EpProtocolConstant.LEN_BIG_ACCOUNT)
		{
			logger.info("apiStartElectric,account:{}",account);
			return EpConstantErrorCode.INVALID_ACCOUNT;
		}
		//检查电桩
		ElectricPileCache epCache =  EpService.getEpByCode(epCode);
		if(epCache == null)
		{
			logger.info("dont find ElectricPileCache,epCode:{}",epCode);
			return EpConstantErrorCode.EP_UNCONNECTED;
		}
		int error = EpService.getEpStatusFromDb(epCode);
		if(error > 0)
		{
			return EpConstantErrorCode.EP_UNCONNECTED;
		}
		if(epCache.getState()==EpConstant.EP_STATUS_OFFLINE)
		{
			return EpConstantErrorCode.EP_PRIVATE;
		}
		else if(epCache.getState()<EpConstant.EP_STATUS_OFFLINE)
		{
			return EpConstantErrorCode.EP_NOT_ONLINE;
		}
		if(epCache.getDeleteFlag() !=0)
		{
			return EpConstantErrorCode.EP_NOT_ONLINE;
		}
				
		if(epGunNo<1|| epGunNo> epCache.getGunNum())
		{
			return EpConstantErrorCode.INVALID_EP_GUN_NO;//
		}
		
		int rateInfoId = epCache.getRateid();
		
		RateInfo rateInfo= RateService.getRateById(rateInfoId);
		if(rateInfo == null)
		{
			return EpConstantErrorCode.INVALID_RATE;
		}
		
		
		UserCache memUserInfo= UserService.getUserCache(account);
		String epChargeGun = epCode + epGunNo;
		if(orgNo == 1000)
		{
			//自己的用户需要校验用户的状态,因为用户可能被冻结
			UserRealInfo userRealInfo = UserService.findUserRealInfo(accountId);
			
			if(userRealInfo==null)
			{
				logger.info("apiStartElectric,accountId:{}",accountId);
				return EpConstantErrorCode.INVALID_ACCOUNT;
			}
			
			
			if(userRealInfo.getStatus() != 1)
			{
				logger.info("apiStartElectric,dbUser.getStatus():{}",userRealInfo.getStatus());
				
				return EpConstantErrorCode.INVALID_ACCOUNT;
			}
			
			
			logger.info("user:{}, gun:{}",account,memUserInfo.getUseGun()+":"+epChargeGun);
			
			if(memUserInfo.getUseGun().length()>0 && memUserInfo.getUseGun().compareTo(epChargeGun)!=0)
			{
				if(memUserInfo.getUseGunStaus() == 6  )
				{
					return EpConstantErrorCode.EPE_REPEAT_CHARGE;
				}
				if(memUserInfo.getUseGunStaus() ==3 )
				{
					return EpConstantErrorCode.CANNOT_OTHER_OPER;
				}
			}
		}
		
		
		EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		
		if( epGunCache.getStatus() ==  EpConstant.EP_GUN_STATUS_SETTING)
		{
			return EpConstantErrorCode.EPE_IN_EP_OPER;//
		}
		if(epGunCache.getStatus() == EpConstant.EP_GUN_STATUS_EP_OPER && startChargeStyle == EpConstant.CHARGE_TYPE_QRCODE)
		{
			logger.debug("epGunCache.getStatus():{},startChargeStyle:{}",epGunCache.getStatus(),startChargeStyle);
			return EpConstantErrorCode.EPE_IN_EP_OPER;//
		}
		if( epGunCache.getStatus() ==  EpConstant.EP_GUN_STATUS_EP_UPGRADE)
		{
			return EpConstantErrorCode.EP_UPDATE;//
		}
		if( epGunCache.getStatus()>30)
		{
			return EpConstantErrorCode.EPE_GUN_FAULT;//
		}
		
		int errorCode = epGunCache.startChargeAction(memUserInfo, rateInfo, bespNo, startChargeStyle, frozenAmt,
				payMode, orgNo,fromSource,actionIdentity,cmdTimes);
		logger.info("apiStartElectric,errorCode:{}",errorCode);
		if(errorCode>0)
		{
			return errorCode;
		}
		memUserInfo.setUseGun(epChargeGun);
		epGunCache.getChargeCache().setPkUserCard(pkUserCard);
		
		return 0;
	}
	public static int apiStopElectric(String epCode,int epGunNo,int userId,int source,String apiClientIp)
	{
		if(epCode.length() !=16)
		{
			return EpConstantErrorCode.INVALID_EP_CODE;//
		}
		
		//检查电桩
		ElectricPileCache epCache =  EpService.getEpByCode(epCode);
		if(epCache == null)
		{
			logger.error("dont find ElectricPileCache:{}",epCode);
			return EpConstantErrorCode.EP_UNCONNECTED;
		}
		
		
		if(epGunNo<1|| epGunNo> epCache.getGunNum())
		{
			return EpConstantErrorCode.INVALID_EP_GUN_NO;//
		}
		
		EpGunCache epGunCache= EpGunService.getEpGunCache(epCode, epGunNo);
		//桩断线，不能结束充电
 		if(epGunCache ==null )
		{
			
			return EpConstantErrorCode.EP_UNCONNECTED;//
		}
		
		return epGunCache.stopChargeAction(userId,source,apiClientIp);
	}

	public static void handEpStartChargeResp(EpCommClient epCommClient,ChargeCmdResp chargeCmdResp,byte []cmdTimes )
	{
		logger.info("handEpStartChargeResp ChargeCmdResp:"+chargeCmdResp+"\n");
	
		EpGunCache epGunCache = EpGunService.getEpGunCache(chargeCmdResp.getEpCode(),chargeCmdResp.getEpGunNo());
		
		if(epGunCache!=null)
		{
			epGunCache.onEpStartCharge(chargeCmdResp);
		}
	}
	/**
	 * 
	 * @param chargeEvent
	 * @return 4:参数错误
	 */
	private static int checkRespChargeEventParams(ChargeEvent chargeEvent)
	{
		if(chargeEvent==null)
		{
			logger.info("checkRespChargeEventParams chargeEvent==null");
			return 4;
		}
		String epCode = chargeEvent.getEpCode();
		if(epCode==null || epCode.length()!=16)
		{
			logger.info("checkRespChargeEventParams invalid epCode:{}",epCode);
			return 4;
		}
		int epGunNo = chargeEvent.getEpGunNo();
		
		if(chargeEvent.getSuccessFlag()!=0&&  chargeEvent.getSuccessFlag()!=1)
		{
			logger.info("checkRespChargeEventParams invalid successFlag:{}",chargeEvent.getSuccessFlag());
			return 4;
		}
		
        String serialNo = chargeEvent.getSerialNo();		
		if(serialNo==null || serialNo.length()!=32)
		{
			logger.info("checkRespChargeEventParams not find serialno:{}",serialNo);
			return 5;
		}
		String zeroSerialNo= StringUtil.repeat("0", 32);
		if( serialNo.compareTo(zeroSerialNo)==0)
		{
			 logger.error("checkRespChargeEventParams invalid SerialNo:{}",serialNo);
			 return 6;
		 }

		EpGunCache epGunCache = EpGunService.getEpGunCache(epCode,epGunNo);
		if(epGunCache == null)
		{
			logger.info("checkRespChargeEventParams not find epGunCache,epCode:{},epGunNo:{}",epCode,epGunNo);
			return 7;
		}
		return 0;
		
	}
	public static  int  handleStartElectricizeEventV3(EpCommClient epCommClient,ChargeEvent chargeEvent,byte []cdmTimes) 
	{
		logger.info("onEpStartChargeEvent,epChargeEvent:{}",chargeEvent);
		
		String retEpCode= null;
		int retEpGunNo = 0;
		String serialNo = null;
		
		//1.检查充电时间参数
		int retCode=checkRespChargeEventParams(chargeEvent);
		if(retCode==0)
		{
			 retEpCode= chargeEvent.getEpCode();
			 retEpGunNo = chargeEvent.getEpGunNo();
			 serialNo = chargeEvent.getSerialNo();
			
			EpGunCache epGunCache = EpGunService.getEpGunCache(retEpCode,retEpGunNo);
		
			retCode = epGunCache.handleStartChargeEvent(chargeEvent);
		}
		else
		{
			logger.error("onEpStartChargeEvent,retCode:{}",retCode);
			
			if(chargeEvent.getEpCode() !=null && chargeEvent.getEpCode().length()==16)
			{
				retEpCode= chargeEvent.getEpCode();
			}
			else
			{
				retEpCode = StringUtil.repeat("0", 16);
			}
			if(retCode==5)
			{
				serialNo = StringUtil.repeat("0", 32);
			}
			else
			{
				serialNo = chargeEvent.getSerialNo();
			}
			
		}
		
		if(!ImitateEpService.IsStart() )
		{
			byte[] confirmdata = EpEncodeProtocol.do_charge_event_confirm(chargeEvent.getEpCode(),chargeEvent.getEpGunNo(),chargeEvent.getSerialNo(),retCode);
				
			if(confirmdata==null)
			{
				logger.error("do_confirm exception");
				return retCode;
			}
			EpMessageSender.sendMessage(epCommClient, 0,0,(byte)Iec104Constant.C_CHARGE_EVENT_CONFIRM,confirmdata,cdmTimes,epCommClient.getCommVersion());
			
			
		}
		return retCode;
		
	}
	public static void updateBeginRecordToDb(int chargeUserId,int chorType,
			String chargeAccount,int pkUserCard,int userOrigin,int pkEpId,String epCode,
			int epGunNo,int chargingmethod,String bespokeNo,String chOrCode, int frozenAmt,long st,
			String chargeSerialNo,int startMeterNum,RateInfo rateInfo,int status)
	{
		TblChargingrecord record = new TblChargingrecord();
		
		BigDecimal bdMeterNum = UtilProtocol.intToBigDecimal3(startMeterNum);
		// 开始充电表低示数
		String beginShowsNumber = String.valueOf(bdMeterNum);
		record.setChreBeginshowsnumber(beginShowsNumber);
		
		// 充电开始时间
		Date startDate = DateUtil.toDate(st * 1000);
		record.setChreStartdate(startDate);
		record.setChreEnddate(startDate);
		record.setChreUsingmachinecode(epCode);
		record.setChreChargingnumber(epGunNo);
		record.setChreReservationnumber(bespokeNo);
		record.setChreResttime(0);
		record.setChreTransactionnumber(chargeSerialNo);
	    record.setUserId(chargeUserId);
	    record.setUserPhone(chargeAccount);
		record.setChreElectricpileid(pkEpId);
	
		record.setChreChargingmethod(chargingmethod);
		
		record.setChreEndshowsnumber("0");
		record.setChreCode(chOrCode);
		record.setStatus(status);
		record.setJPrice(rateInfo.getJ_Rate());
		record.setFPrice(rateInfo.getF_Rate());
		record.setPPrice(rateInfo.getP_Rate());
		record.setGPrice(rateInfo.getG_Rate());
		record.setQuantumDate(rateInfo.getQuantumDate());
		record.setPkUserCard(pkUserCard);
		record.setUserOrigin(userOrigin);
		BigDecimal bdFrozenAmt = UtilProtocol.intToBigDecimal2(frozenAmt);
		
		record.setFrozenAmt(bdFrozenAmt);
		// 新增开始充电记录
		DB.chargingrecordDao.updateBeginRecord(record);
	}
	
	public static void updateFailRecordToDb(String serialNo,int status)
	{
		TblChargingrecord record = new TblChargingrecord();
	
		record.setChreTransactionnumber(serialNo);
		record.setStatus(status);
		// 新增开始充电记录
		DB.chargingrecordDao.updateFailChargeRecord(record);
	}
	
	public static void insertChargeRecordToDb(int chargeUserId,int chorType,
			String chargeAccount,int pkUserCard,int userOrigin,int pkEpId,String epCode,
			int epGunNo,int chargingmethod,String bespokeNo,String chOrCode, int frozenAmt,int payMode,int userOrgNo,
			ChargeEvent chargeEvent
			, RateInfo rateInfo,int status)
	{
		TblChargingrecord record = new TblChargingrecord();
		
		BigDecimal bdMeterNum = UtilProtocol.intToBigDecimal3(chargeEvent.getStartMeterNum());
		// 开始充电表低示数
		String beginShowsNumber = String.valueOf(bdMeterNum);
		record.setChreBeginshowsnumber(beginShowsNumber);
		
		// 充电开始时间
		Date startDate = DateUtil.toDate(chargeEvent.getStartChargeTime() * 1000);
		record.setChreStartdate(startDate);
		record.setChreEnddate(startDate);
		record.setChreUsingmachinecode(chargeEvent.getEpCode());
		record.setChreChargingnumber(chargeEvent.getEpGunNo());
		record.setChreReservationnumber(chargeEvent.getBespokeNo());
			

		record.setChreResttime(chargeEvent.getRemainTime());
		

		
		record.setChreTransactionnumber(chargeEvent.getSerialNo());
        record.setUserId(chargeUserId);
	    record.setUserPhone(chargeAccount);
		record.setChreElectricpileid(pkEpId);
		
		record.setChreChargingmethod(chargingmethod);
		
		record.setChreEndshowsnumber("0");
		record.setChreCode(chOrCode);
	
		
		record.setStatus(status);
		record.setJPrice(rateInfo.getJ_Rate());
		record.setFPrice(rateInfo.getF_Rate());
		record.setPPrice(rateInfo.getP_Rate());
		record.setGPrice(rateInfo.getG_Rate());
		record.setQuantumDate(rateInfo.getQuantumDate());
		record.setPkUserCard(pkUserCard);
		logger.debug("userOrigin:{}",userOrigin);
		record.setUserOrigin(0);
		
		BigDecimal bdFrozenAmt = UtilProtocol.intToBigDecimal2(frozenAmt);
		record.setFrozenAmt(bdFrozenAmt);
		record.setUserOrgNo(userOrgNo);
		record.setPayMode(payMode);
		// 新增开始充电记录
		DB.chargingrecordDao.insertBeginRecord(record);
		
	}
	public static void insertChargeOrderToDb(int chargeUserId,int chorType,
			int pkUserCard,int userOrigin,int pkEpId,String epCode,
			int epGunNo,int chargingmethod,String bespokeNo,String chOrCode, int frozenAmt,
			int payMode,int userOrgNo,long st,String chargeSerialNo
			, RateInfo rateInfo)
	{
		//订单编号，赞借用流水号
		Date date = new Date();
		
		Date dtStart = new Date(st *1000);
		// 计算总电量
		String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");
		
		BigDecimal  bdZero = new BigDecimal(0.0);
		
		BigDecimal  chargeAmt = new BigDecimal(0.0);
		BigDecimal  serviceAmt =new BigDecimal(0.0);
		// 充电消费订单
		 TblChargingOrder order = new TblChargingOrder();
		 order.setChorCode(chOrCode);
		
		order.setChorAppointmencode(bespokeNo);
		
		 order.setChorPilenumber(epCode);
		 order.setChorUserid(""+chargeUserId);
		
		 order.setChorType(chorType);
		 //order.setChorType(0);
		 
		 order.setChorMoeny(chargeAmt.add(serviceAmt) + "");
		 order.setChOr_tipPower(bdZero);
		 order.setChOr_peakPower(bdZero);
		 order.setChOr_usualPower(bdZero);
		 order.setChOr_valleyPower(bdZero);
		 order.setChorQuantityelectricity(bdZero );
		 order.setChorTimequantum(beginTime + " - ");
		 order.setChorMuzzle(epGunNo);
		 order.setChorChargingstatus(EpConstant.ORDER_STATUS_WAIT + "");
		 order.setChorTranstype("1");
		
		 order.setChorCreatedate(new Date());
		 order.setChorUpdatedate(new Date());
		 //order.setChorUsername(userInfo.getUsinFacticityname() == null ? "":userInfo.getUsinFacticityname());
		 order.setChorUsername("");
		 order.setChorTransactionnumber(chargeSerialNo);
		 order.setChorChargemoney(chargeAmt);
		 order.setChorServicemoney(serviceAmt);
		 order.setChorOrdertype(0);
		 beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);
		
		 order.setChargeBegintime(beginTime);
		 order.setChargeEndtime(beginTime);
		 order.setPkUserCard(pkUserCard);
		 order.setUserOrigin(0);
		 order.setUserOrgNo(userOrgNo);
		 //order.setPayMode(payMode);
		 // 新增充电消费订单
		 DB.chargeOrderDao.insertChargeOrder(order);
	}
	public static String getMeterNum(int nMeterNum)
	{
		BigDecimal bdMeterNum = UtilProtocol.intToBigDecimal3(nMeterNum);

		String strMeterNum = String.valueOf(bdMeterNum);
		return strMeterNum;
	}
	
	public static void insertChargeWithConsumeRecord(int chargeUserId,int chorType,
			String chargeAccount,int pkUserCard,int userOrigin,int pkEpId,String epCode,int epGunNo,int chargingmethod,
			String bespokeNo,String chOrCode,int payMode,int userOrgNo, NoCardConsumeRecord consumeRecord
			, RateInfo rateInfo)
	{
		// 尖时段用电度数
		BigDecimal tipPower = UtilProtocol.intToBigDecimal3(consumeRecord.getjDl());
		// 峰时段用电度数
		BigDecimal peakPower = UtilProtocol.intToBigDecimal3(consumeRecord.getfDl());
		// 平时段用电度数
		BigDecimal usualPower = UtilProtocol.intToBigDecimal3(consumeRecord.getpDl());
		// 谷时段用电度数
		BigDecimal valleyPower = UtilProtocol.intToBigDecimal3(consumeRecord.getgDl()); 
		// 时间段
		
		BigDecimal totalPower = UtilProtocol.intToBigDecimal3(consumeRecord.getTotalDl());
		
		
		Date dtStart = new Date(consumeRecord.getStartTime()*1000);
		Date dtEnd = new Date(consumeRecord.getEndTime()*1000);
		// 计算总电量
		String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");
		String endTime = DateUtil.toDateFormat(dtEnd,"MM-dd HH:mm");
		
		
		BigDecimal  chargeAmt =  UtilProtocol.intToBigDecimal2(consumeRecord.getTotalChargeAmt());
		BigDecimal  serviceAmt = UtilProtocol.intToBigDecimal2(consumeRecord.getServiceAmt());
	
		// 充电消费订单
		 TblChargingOrder order = new TblChargingOrder();
		 order.setChorCode(chOrCode);
		
		order.setChorAppointmencode(bespokeNo);
		
		 order.setChorPilenumber(epCode);
		 order.setChorUserid(""+chargeUserId);
		
		 order.setChorType(chorType);
		 
		 order.setChorMoeny(chargeAmt.add(serviceAmt) + "");
		 order.setChOr_tipPower(tipPower);
		 order.setChOr_peakPower(peakPower);
		 order.setChOr_usualPower(usualPower);
		 order.setChOr_valleyPower(valleyPower);
		 order.setChorQuantityelectricity(totalPower );
		 order.setChorTimequantum(beginTime + " - " + endTime);
		 order.setChorMuzzle(epGunNo);
		 order.setChorChargingstatus(EpConstant.ORDER_STATUS_DONE + "");
		 order.setChorTranstype(""+consumeRecord.getTransType());
		
		 order.setChorCreatedate(new Date());
		 order.setChorUpdatedate(new Date());
		 //order.setChorUsername(userInfo.getUsinFacticityname() == null ? "":userInfo.getUsinFacticityname());
		 order.setChorUsername("");
		 order.setChorTransactionnumber(consumeRecord.getSerialNo());
		 order.setChorChargemoney(chargeAmt);
		 order.setChorServicemoney(serviceAmt);
		 order.setChorOrdertype(0);
		 beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);
		 endTime = DateUtil.toDateFormat(dtEnd,ChargeYearDateFmt);
		
		 order.setChargeBegintime(beginTime);
		 order.setChargeEndtime(endTime);
		 order.setPkUserCard(pkUserCard);
		 order.setUserOrgNo(userOrgNo);
		 //order.setPayMode(payMode);
		 // 新增充电消费订单
		 DB.chargeOrderDao.insertFullChargeOrder(order);
			 
	 
		
		TblChargingrecord record = new TblChargingrecord();
		// 开始充电表低示数
		String beginShowsNumber = getMeterNum(consumeRecord.getStartMeterNum());
		String endShowsnumber = getMeterNum(consumeRecord.getEndMeterNum());
		
		// 充电开始时间
		
        record.setUserId(chargeUserId);
	    record.setUserPhone(chargeAccount);
		record.setChreElectricpileid(pkEpId);
		record.setChreUsingmachinecode(epCode);

		record.setChreTransactionnumber(consumeRecord.getSerialNo());
		
		record.setChreStartdate(dtStart);
		record.setChreChargingnumber(epGunNo);
		record.setChreChargingmethod(chargingmethod);
		record.setChreReservationnumber("");
		
		record.setChreBeginshowsnumber(beginShowsNumber);
		record.setChreEndshowsnumber(endShowsnumber);
		
		record.setChreCode(chOrCode);
		
		record.setChreEnddate(dtEnd);
		record.setChreResttime(0);
		record.setStatus(1);
		record.setJPrice(rateInfo.getJ_Rate());
		record.setFPrice(rateInfo.getF_Rate());
		record.setPPrice(rateInfo.getP_Rate());
		record.setGPrice(rateInfo.getG_Rate());
		record.setQuantumDate(rateInfo.getQuantumDate());
		record.setPkUserCard(pkUserCard);
		record.setUserOrigin(userOrigin);
		record.setUserOrgNo(userOrgNo);
		record.setPayMode(payMode);
		// 新增开始充电记录
		DB.chargingrecordDao.insertFullChargeRecord(record);
		
	}
	
	
	public static void updateChargeToDb(int pkEpId,String epCode,int payMode,
			String chargeUserAccount,int userId,int epGunNo,RateInfo rateInfo,
			String chOrCode,NoCardConsumeRecord consumeRecord,boolean exceptionData)
	{
		// 尖时段用电度数
		BigDecimal tipPower = UtilProtocol.intToBigDecimal3(consumeRecord.getjDl());
		// 峰时段用电度数
		BigDecimal peakPower = UtilProtocol.intToBigDecimal3(consumeRecord.getfDl());
		// 平时段用电度数
		BigDecimal usualPower = UtilProtocol.intToBigDecimal3(consumeRecord.getpDl());
		// 谷时段用电度数
		BigDecimal valleyPower = UtilProtocol.intToBigDecimal3(consumeRecord.getgDl()); 
		// 时间段
		
		BigDecimal totalPower = UtilProtocol.intToBigDecimal3(consumeRecord.getTotalDl());
				
			
		Date dtStart = new Date(consumeRecord.getStartTime()*1000);
		Date dtEnd = new Date(consumeRecord.getEndTime()*1000);
		// 计算总电量
		String beginTime = DateUtil.toDateFormat(dtStart, "MM-dd HH:mm");
		String endTime = DateUtil.toDateFormat(dtEnd,"MM-dd HH:mm");
		
		BigDecimal  chargeAmt =  UtilProtocol.intToBigDecimal2(consumeRecord.getTotalChargeAmt());
		BigDecimal  serviceAmt = UtilProtocol.intToBigDecimal2(consumeRecord.getServiceAmt());
		BigDecimal  chargeCost = UtilProtocol.intToBigDecimal2(consumeRecord.getTotalAmt());
		// 充电消费订单
		 TblChargingOrder order = new TblChargingOrder();
		 order.setChorCode(chOrCode);
		
		order.setChorAppointmencode("000000000000");
		
		order.setChorPilenumber(epCode);
		 
		 
		 order.setChorMoeny(chargeCost + "");
		 order.setChorChargemoney(chargeAmt);
		 order.setChorServicemoney(serviceAmt);
		 
		 order.setChOr_tipPower(tipPower);
		 order.setChOr_peakPower(peakPower);
		 order.setChOr_usualPower(usualPower);
		 order.setChOr_valleyPower(valleyPower);
		 order.setChorQuantityelectricity(totalPower );
		 order.setChorTimequantum(beginTime + " - " + endTime);
		 order.setChorTransactionnumber(consumeRecord.getSerialNo());
		 order.setChorMuzzle(epGunNo);
		 
		 if(!exceptionData)
		 {
		 
			 if(payMode == EpConstant.P_M_FIRST)
			 {
				 order.setChorChargingstatus(EpConstant.ORDER_STATUS_SUCCESS + "");
			 }
			 else
			 {
				 order.setChorChargingstatus(EpConstant.ORDER_STATUS_DONE + "");
			 }
		 }
		 else
		 {
			 order.setChorChargingstatus(EpConstant.ORDER_STATUS_EXCEPTION+"");
		 }
		 
		 
		 order.setChorTranstype(consumeRecord.getTransType() + "");
		 String bespokeNo= StringUtil.repeat("0", 12);
		 order.setChorAppointmencode(bespokeNo);
		 
		 order.setChorUpdatedate(new Date());
		
		 order.setChorUsername("");
		 order.setChorTransactionnumber(consumeRecord.getSerialNo());
		
		 
		 beginTime = DateUtil.toDateFormat(dtStart, ChargeYearDateFmt);
		 endTime = DateUtil.toDateFormat(dtEnd,ChargeYearDateFmt);
		 order.setChargeBegintime(beginTime);
		 order.setChargeEndtime(endTime);
		 // 新增充电消费订单
		 DB.chargeOrderDao.updateOrder(order);
				 
		 // 根据交易流水号更新充电订单编号
		TblChargingrecord record = new TblChargingrecord();
		
		String beginShowsNumber = getMeterNum(consumeRecord.getStartMeterNum());
		String endShowsnumber = getMeterNum(consumeRecord.getEndMeterNum());
		
		// 开始充电表低示数
	
		// 充电开始时间
		record.setUserId(userId);
	    record.setUserPhone(chargeUserAccount);
	   
		record.setChreElectricpileid(pkEpId);
		record.setChreUsingmachinecode(epCode);

		record.setChreTransactionnumber(consumeRecord.getSerialNo());
		record.setChreBeginshowsnumber(beginShowsNumber);
		record.setChreEndshowsnumber(endShowsnumber);
		record.setChreStartdate(dtStart);
		record.setChreEnddate(dtEnd);
		record.setChreChargingnumber(epGunNo);
		record.setChreChargingmethod(1);
		record.setChreReservationnumber(bespokeNo);

		record.setChreResttime(0);
		
		record.setJPrice(rateInfo.getJ_Rate());
		record.setFPrice(rateInfo.getF_Rate());
		record.setPPrice(rateInfo.getP_Rate());
		record.setGPrice(rateInfo.getG_Rate());
		record.setQuantumDate(rateInfo.getQuantumDate());
		record.setChreCode(order.getChorCode());
		
		record.setStatus(1);
		
		// 新增开始充电记录
		DB.chargingrecordDao.insertEndRecord(record);
		
	}

	public static String getFaultDesc(int nStopRet)
	{
		String faultCause = "";
		switch(nStopRet)
		{
		case 2:
			faultCause = "用户强制结束";
			break;
		case 3:
			faultCause = "急停";
			break;
		case 4:
			faultCause = "连接线断掉";
			break;
		case 5:
			faultCause = "电表异常";
			break;
		case 6:
			faultCause = "过流停止";
			break;
		case 7:
			faultCause = "过压停止";
			break;
		case 8:
			faultCause = "防雷器故障";
			break;
		case 9:
			faultCause = "接触器故障";
			break;
		case 10:
			faultCause = "充电金额不足";
			break;
		case 11:
			faultCause = "漏电保护器";
			break;
		case 13:
			faultCause = "BMS通信异常故障";
			break;
		case 14:
			faultCause = "违规拔枪";
			break;
		case 15:
			faultCause = "电桩断电";
			break;
		default:
			faultCause = "电桩保护,nStopRet:"+nStopRet;
			break;
		}
		return faultCause;
	}
	public static String getStopChargeDesc(int nCause)
	{
		String faultCause = "";
		switch(nCause)
		{
		case 3:
			faultCause = "急停";
			break;
		case 4:
			faultCause = "连接线断掉";
			break;
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
		case 11:
		case 13:
			faultCause = "电桩保护(原因"+nCause+")";
			break;
		case 10:
			faultCause = "充电金额不足";
			break;
		case 12:
			faultCause = "自动充满";
			break;
		case 14:
			faultCause = "违规拔枪";
			break;
		case 15:
			faultCause = "电桩断电";
			break;
		case 16:
		case 17:
		case 18:
			faultCause = "电桩保护(原因"+nCause+")";
			break;
		case 19:
			faultCause = "车BMS主动停止";
			break;
		default:
			faultCause = "电桩保护(原因"+nCause+")";
			break;
		}
		return faultCause;
	}
	
	public static  int handleStopElectricizeEvent(EpCommClient epCommClient,String epCode,int epGunNo,String serialNo,long et,
			short nStopRet,int nDbValue,byte ChargeStye,byte offstates,byte successflag,byte[]time,byte []cmdTimes) throws IOException
	{
		//logger.info("handleStopElectricizeEvent epCode:"+epCode +",epGunNo:"+epGunNo+",nStopRet:"+nStopRet +"\n");
		
		
		short isException=0;
		int exceptionCode=0;
	
		//1.
		EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		if(epGunCache==null)
		{
			logger.info("handleStopElectricizeEvent epGunCache!epCode:{},epGunNo:{}",epCode,epGunNo);			
			return  1;
		}
		short nTimeOut =0;
		ChargeCache chargeCache = epGunCache.getChargeCache();
			//在线二维码停止充电超时
		
		if (chargeCache == null )
		{
			logger.info("handleStopElectricizeEvent not find ElectricCache,epCode:{},epGunNo:{}",
					epCode ,epGunNo );
			return  2;
		}
		if(chargeCache.getStatus() == ChargeStatus.CS_CHARGE_END)
		{
			logger.info("handleStopElectricizeEvent had been stop charge,epCode:{},epGunNo:{}",
					epCode ,epGunNo );
			return 3;
		}
		
		//桩停止充电不成功		
		if (successflag != 1)
		{
			logger.info("handleStopElectricizeEvent stop fail,successflag:"+successflag +"\n");
			return 3;
		}
		chargeCache.setEt(et);

		if (nStopRet > 12) {
				nStopRet = 1;
		}
		if (nStopRet == 1 || nStopRet == 2 || nStopRet == 3 || nStopRet == 10) {
			isException = 1;
		} 
		else {
			exceptionCode = nStopRet;
		}
		short nnStopRet = nStopRet;
		if (nStopRet > 11)// 充电桩正常退出
		{
			nnStopRet = nStopRet;
		}
		chargeCache.setStopCause(nStopRet);
		
		endChargeRecord(chargeCache.getChargeSerialNo(),et ,0);
        
		chargeCache.setStatus(ChargeStatus.CS_CHARGE_END);
		
		return 0;
	}
	public static void endChargeRecord(String serialNo,long et,int endMeterNum)
	{
		TblChargingrecord record = new TblChargingrecord();
		record.setChreTransactionnumber(serialNo);
		record.setStatus( ChargeStatus.CS_CHARGE_END);
		
		BigDecimal bdEndMeterNum = UtilProtocol.intToBigDecimal3(endMeterNum);
		String endShowsnumber = String.valueOf(bdEndMeterNum);
		
		record.setChreEndshowsnumber(endShowsnumber);
		Date endDate = DateUtil.toDate(et * 1000);
		
		
		record.setChreEnddate(endDate);
		
		// 更新充电记录 记录结束时间和用电度数
		DB.chargingrecordDao.insertEndRecord(record);
	}
	public static void insertFaultRecord(int stopCause,String epCode,int pkEpId,int epGunNo,String serialNo,Date faultDate)
	{
		try
		{
			if (stopCause>2 && stopCause != 10) 
			{// 故障原因
				String faultCause = ""+stopCause;//getFaultDesc(stopCause);
				
	            TblChargingfaultrecord faultrecord = new TblChargingfaultrecord();
				faultrecord.setCfreUsingmachinecode(epCode);
				faultrecord.setCfreElectricpileid(pkEpId);
				faultrecord.setCfreElectricpilename("");
				
				faultrecord.setcFRe_EphNo(epGunNo);
					
				
				faultrecord.setCfreChargingsarttime(faultDate);
				faultrecord.setCfreFaultcause(faultCause);
				faultrecord.setCfreTransactionnumber(serialNo);
				// 新增故障记录
				DB.chargingfaultrecordDao.insertFaultRecord(faultrecord);
			}
		}catch(Exception e)
		{
			logger.error("insertFaultRecord,exception stack trace:{}",e.getStackTrace());
		}
		
	}

	public static  void handleCardFronzeAmt(EpCommClient epCommClient,String epCode,int epGunNo,String innerCardNo,int fronzeAmt,byte []cmdTimes) throws IOException
	{
		int errorCode=0;
		EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		if(epGunCache == null)
		{
			errorCode= 2;
		}
		else
		{
			UserCache userCache =  UserService.getCardUser(innerCardNo);
			
			if(userCache != null)
			{
				int cardPayMode = userCache.getCard().getPayMode();
				//把卡付费模式转为充电付费模式
				int payMode = EpConstant.P_M_FIRST;
				if(cardPayMode == 12)
				{
					payMode = EpConstant.P_M_POSTPAID;
				}
				
				//BigDecimal bdFronzeAmt=(new BigDecimal(fronzeAmt)).multiply(Global.Dec2);
				int pkCardId = userCache.getCard().getId();
				logger.debug("handleCardFronzeAmt success!call apiStartElectric");
				errorCode = apiStartElectric(
						epCode,epGunNo,userCache.getId(),userCache.getAccount(),"",
						(short)EpConstant.CHARGE_TYPE_CARD,pkCardId,
						fronzeAmt,payMode,1000,2,userCache.getAccount(),cmdTimes);
				
				if(errorCode>0)
				{
					logger.info("handleCardFronzeAmt errorCode:{}",errorCode);
				}
			}
			else
			{
				errorCode = 3;
				
			}
		}
		if(errorCode>0)
		{
			switch(errorCode)
			{
			case 1002:
			{				
				errorCode =1;	
			}
			break;
			case EpConstantErrorCode.EP_UNCONNECTED:
			case EpConstantErrorCode.INVALID_EP_CODE:
			case EpConstantErrorCode.INVALID_EP_GUN_NO:
			case EpConstantErrorCode.INVALID_RATE:
			{
				errorCode =2;	
			}
			break;
			case 6800:
			case EpConstantErrorCode.CANNOT_OTHER_OPER:
			{
				errorCode =4;
			}
			break;
			case EpConstantErrorCode.INVALID_ACCOUNT:
			{
				errorCode =3;
			}
			break;
			default:
				break;
			}
			
			logger.info("handleCardFronzeAmt errorCode:{}",errorCode);
			
			byte[] data = EpEncodeProtocol.do_card_frozen_amt(epCode,epGunNo,innerCardNo,0,errorCode);
			EpMessageSender.sendMessage(epCommClient,0,0,Iec104Constant.M_CARD_FRONZE_AMT_RET,data,cmdTimes,epCommClient.getCommVersion());
			
		}
	}
	/**
	 * 处理北汽出行消费记录
	 * @param consumeRecord
	 * @param epGunCache
	 * @return   4：无效的交易流水号
	 * 			3:已经处理
				2:数据不存在
				1:处理成功
	 */
	public static int endBQConsumeRecord(NoCardConsumeRecord consumeRecord,EpGunCache  epGunCache )
	{
		int orderStatus = EpChargeService.getChargeOrderStatus(consumeRecord.getSerialNo());
		logger.debug("endCreditConsumeRecord serialNo,{},orderStatus:{}",consumeRecord.getSerialNo(),orderStatus);
		if(orderStatus==2|| orderStatus==3)//
		{
			logger.info("endBQConsumeRecord,serialNo:{},orderStatus:{}",consumeRecord.getSerialNo(),orderStatus);
			return 3;
		}
		
		String chOrCode = EpChargeService.makeChargeOrderNo(consumeRecord.getEpUserAccount());
		Map<String, Object> consumeRecordMap = new ConcurrentHashMap<String, Object>();
		
		consumeRecordMap.put("userId",0);
		consumeRecordMap.put("orderid",chOrCode);
		consumeRecordMap.put("st",consumeRecord.getStartTime());
		consumeRecordMap.put("et",consumeRecord.getEndTime());
		consumeRecordMap.put("totalMeterNum",consumeRecord.getTotalDl());
		consumeRecordMap.put("totalAmt",consumeRecord.getTotalChargeAmt());
		consumeRecordMap.put("serviceAmt",consumeRecord.getServiceAmt());
		consumeRecordMap.put("pkEpId",epGunCache.getPkEpId());
		consumeRecordMap.put("epCode",epGunCache.getEpCode());
		consumeRecordMap.put("chargStartEnergy",consumeRecord.getStartMeterNum());
		consumeRecordMap.put("chargEndEnergy",consumeRecord.getEndMeterNum());
		consumeRecordMap.put("account",consumeRecord.getEpUserAccount());
		consumeRecordMap.put("serialNo",consumeRecord.getSerialNo());

		
		epGunCache.setCardOrigin(0);
	
		epGunCache.handleEventExtra(EventConstant.EVENT_CONSUME_RECORD, consumeRecord.getUserOrgin(), 0, 0, null, consumeRecordMap);
		
		return 1;
		
		
	}
	/**
	 *处理大账户信用卡
	 * @param consumeRecord
	 * @return  4：无效的交易流水号
	 * 			3:已经处理
				2:数据不存在
				1:处理成功
	 */
	public static int endCreditConsumeRecord(NoCardConsumeRecord consumeRecord,EpGunCache  epGunCache )
	{
		String cardInNo= consumeRecord.getEpUserAccount();
		UserCache cardUser= UserService.getCardUser(cardInNo);
		
		int orderStatus = EpChargeService.getChargeOrderStatus(consumeRecord.getSerialNo());
		logger.debug("endCreditConsumeRecord serialNo,{},orderStatus:{}",consumeRecord.getSerialNo(),orderStatus);
		if(orderStatus==2|| orderStatus==3)//
			return 3;
		if(cardUser==null)
		{
			logger.info("endCreditConsumeRecord not find user info,account",cardInNo);
			return 4;
		}
		logger.debug("endCreditConsumeRecord cardUser:{}",cardUser);
		
		RateInfo rateInfo = RateService.getRateInfo(epGunCache.getEpCode());
		String chOrCode = makeChargeOrderNo(cardUser.getAccount());
		
		int chorType= EpChargeService.getOrType(cardUser.getLevel());
		
		int chargeTime = (int)((consumeRecord.getEndTime()-consumeRecord.getStartTime())/60);			
		EpChargeService.addChargeStat(epGunCache.getPkEpGunId(),consumeRecord.getTotalDl(),chargeTime,consumeRecord.getTotalAmt());

		
		insertChargeWithConsumeRecord(cardUser.getId(),chorType,cardUser.getAccount(),cardUser.getCard().getId(),0,epGunCache.getPkEpGunId(),
				epGunCache.getEpCode(),epGunCache.getEpGunNo(),EpConstant.CHARGE_TYPE_CARD,"",chOrCode,2,0,consumeRecord,rateInfo);
		
		return 1;
		
	}
	/**
	 *处理萍乡特殊的消费记录
	 * @param consumeRecord
	 * @return  4：无效的交易流水号
	 * 			3:已经处理
				2:数据不存在
				1:处理成功
	 */
	public static int endPXConsumeRecord(NoCardConsumeRecord consumeRecord,EpGunCache  epGunCache )
	{
		int orderStatus = EpChargeService.getChargeOrderStatus(consumeRecord.getSerialNo());
		logger.debug("serialNo,{},orderStatus:{}",consumeRecord.getSerialNo(),orderStatus);
		if(orderStatus==2|| orderStatus==3)//
			return 3;
		
		//String account="18017285847";
		String account= GameConfig.bigAccount1002;
		logger.debug("account:{}",account);
		
		
		UserCache cardUser= UserService.getUserCache(account);
		if(cardUser==null)
		{
			logger.info("endPXConsumeRecord not find user info,account",account);
			return 4;
		}
		logger.debug("cardUser:{}",cardUser);
		
			
		
		RateInfo rateInfo = RateService.getRateInfo(epGunCache.getEpCode());
		
		String chOrCode = makeChargeOrderNo("1002");
		
		
		int chorType= EpChargeService.getOrType(3);
		
			
		int chargeTime = (int)((consumeRecord.getEndTime()-consumeRecord.getStartTime())/60);
		logger.debug("handleConsumeRecord 4-1002-5:chargeTime{}",chargeTime);
		EpChargeService.addChargeStat(epGunCache.getPkEpGunId(),consumeRecord.getTotalDl(),chargeTime,consumeRecord.getTotalAmt());

		logger.debug("handleConsumeRecord 4-1002-6:");
		
		insertChargeWithConsumeRecord(cardUser.getId(),chorType,cardUser.getAccount(),0,0,epGunCache.getPkEpGunId(),
				epGunCache.getEpCode(),epGunCache.getEpGunNo(),EpConstant.CHARGE_TYPE_ACCOUNT,"",chOrCode,2,0,
				consumeRecord,rateInfo);
		
		return 1;
		
	}
	
	private static int checkConsumeRecord(NoCardConsumeRecord consumeRecord)
	{
		//1.检查流水号是否合法
	   if(consumeRecord==null)
	   {
		   logger.error("endChargeWithConsumeRecord invalid consumeRecord:{}",consumeRecord);
		   return 4;
	   }
	   
	   String epCode = consumeRecord.getEpCode();
		int epGunNo = consumeRecord.getEpGunNo();
		  
	   String consumeSerialNo = consumeRecord.getSerialNo();
	   String zeroSerialNo= StringUtil.repeat("0", 32);
	   
	   if(consumeSerialNo==null || consumeSerialNo.length()!=32)
	   {
		   logger.error("endChargeWithConsumeRecord invalid consumeSerialNo:{},consumeRecord:{}",consumeSerialNo,consumeRecord);
		   return 5;
	   }
	   if( consumeSerialNo.compareTo(zeroSerialNo)==0)
	   {
		   logger.error("endChargeWithConsumeRecord invalid consumeSerialNo:{},consumeRecord:{}",consumeSerialNo,consumeRecord);
		   return 6;
	   }
		   
		EpGunCache  epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		
		
		if(epGunCache == null)
		{
			logger.error("handleNoCardConsumeRecord do not find EpGunCache!epCode:{},epGunNo:{},consumeRecord:{}", new Object[]{epCode,epGunNo,consumeRecord});
			return 7;
		}
		
		
		return 0;
		
	}
	/**
	 * 消费记录电桩编号和枪口编号一定要准确，错了就发到别的地方
	 * @param channel
	 * @param consumeRecord
	 * @throws IOException
	 */
	public static  void handleConsumeRecord(EpCommClient epCommClient,NoCardConsumeRecord consumeRecord,byte []cmdTimes) throws IOException
	{
		logger.debug("handleNoCardConsumeRecord,consumeRecord:{}",consumeRecord);
		
		int retCode= checkConsumeRecord(consumeRecord);
		
		if(retCode != 0 && retCode!=8)
		{
			logger.error("handleConsumeRecord,retCode:{}",retCode);
			
			
			String retEpCode = StringUtil.repeat("0", 16);
			if(consumeRecord.getEpCode()!=null && consumeRecord.getEpCode().length()==16)
			{
				retEpCode = consumeRecord.getEpCode();
			}
			
			int retEpGunNo = consumeRecord.getEpGunNo();
				
			byte[] confirmdata=null;
			String serialNo = consumeRecord.getSerialNo();
			if(retCode==5)
			{
				serialNo = StringUtil.repeat("0", 32);
			}
			
			confirmdata = EpEncodeProtocol.do_consumerecord_confirm(retEpCode,retEpGunNo,serialNo,retCode);
			EpMessageSender.sendMessage(epCommClient,0,0,Iec104Constant.C_CONSUME_RECORD_CONFIRM, confirmdata,cmdTimes,epCommClient.getCommVersion());	
			
			return ;
		}
		
		
		String epCode = consumeRecord.getEpCode();
		int epGunNo = consumeRecord.getEpGunNo();
		
		EpGunCache  epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
		 
		if(consumeRecord.getAccountType()==3 )
		{
			if(consumeRecord.getUserOrgin() == 5)
			{
				retCode = endBQConsumeRecord(consumeRecord,epGunCache);
			}
			else
			{
				retCode =endCreditConsumeRecord(consumeRecord,epGunCache);
				
			}
		}
		//萍乡特殊的消息记录，即没有卡号，也没有账号
		else if(consumeRecord.getAccountType() == 4 && consumeRecord.getUserOrgin() == 1002  )
		{
			retCode = endPXConsumeRecord(consumeRecord,epGunCache);
		}
		else
		{
			//爱充正常用户充电记录
			retCode = epGunCache.endChargeWithConsumeRecord(consumeRecord);
		}
		
		logger.info("handleNoCardConsumeRecord retCode:{},epGunNo:{}", retCode,epCode+epGunNo);
	
		if(retCode>=0 && retCode <=3)
		{	
				
			byte[] confirmdata=null;
			
			confirmdata = EpEncodeProtocol.do_consumerecord_confirm(epCode,epGunNo,consumeRecord.getSerialNo(),retCode);
			EpMessageSender.sendMessage(epCommClient,0,0,Iec104Constant.C_CONSUME_RECORD_CONFIRM, confirmdata,cmdTimes,epCommClient.getCommVersion());
		}
		else
		{
			logger.error("handleNoCardConsumeRecord retCode:{},epGunNo:{}", retCode,epCode+epGunNo);
			
		}
	}
	
	
	public static  void handleHisConsumeRecord(EpCommClient epCommClient,NoCardConsumeRecord consumeRecord) throws IOException
	{
		logger.debug("handleHisConsumeRecord,consumeRecord:{}",consumeRecord);
	}
	
	/**
	 * 该函数用来测试强制移除充电业务
	 * @param epCode
	 * @param epGunNo
	 */
	static public String forceRemoveCharge(String epCode,int epGunNo)
	{
		if(epCode!=null && epCode.length() != EpProtocolConstant.LEN_CODE )
        {
			return "remove charge cache! code invalid! \n";
        }
    	ElectricPileCache epClient =  EpService.getEpByCode(epCode);
    	if(epClient==null)
    	{
    		return "remove charge cache! did not find ep"+ epCode+" \n";
    	}
        		
        int nGunNum=epClient.getGunNum();
            	
            	
    	if(epGunNo<1 ||epGunNo >nGunNum)
    	{
    		return "remove charge cache! epGunNo invalid"+epGunNo+" \n";
    	}
            	
		//String epGunNoKey = epCode +epGunNo;
		//int currentType = epClient.getCurrentType();
        //ChargeCache chargeCache=null;
        
        EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
        if(epGunCache !=null)
        {
        	if(epGunCache.getChargeCache()!=null)
        	{
        		long et = DateUtil.getCurrentSeconds();
        		endChargeRecord(epGunCache.getChargeCache().getChargeSerialNo(),et ,0);
        	}
        	
        	//TODO://如果处理数据库
        	epGunCache.cleanChargeInfo();
        	
    		BigDecimal bdZero = new BigDecimal(0.0);
    		EpGunService.updateChargeInfoToDbByEpCode(epClient.getCurrentType(),epCode,epGunNo,
    				bdZero,"",bdZero,0,0);
    		
        }
        return "";
	}

	/**
	 * 该函数用来测试强制移除充电业务
	 * @param epCode
	 * @param epGunNo
	 */
	static public String forceRemoveBespoke(String epCode,int epGunNo)
	{
		if(epCode!=null && epCode.length() != EpProtocolConstant.LEN_CODE )
        {
			return "remove bespoke cache! code invalid! \n";
        }
    	ElectricPileCache epClient =  EpService.getEpByCode(epCode);
    	if(epClient==null)
    	{
    		return "remove bespoke cache! did not find ep"+ epCode+" \n";
    	}
        		
        int nGunNum=epClient.getGunNum();
            	
            	
    	if(epGunNo<1 ||epGunNo >nGunNum)
    	{
    		return "remove bespoke cache! epGunNo invalid"+epGunNo+" \n";
    	}
            
        EpGunCache epGunCache = EpGunService.getEpGunCache(epCode, epGunNo);
        if(epGunCache !=null)
        {
        	epGunCache.cleanBespokeInfo();
        }
        return "";
	}
	public static String makeSerialNo()
	{
		Date now = new Date();
		Random random = new Random();
		
		return DateUtil.toDateFormat(now, Global.serialSecFormat) + "1"+ String.format("%03d",Math.abs(random.nextLong()%1000)); 
	}
	
	public static String makeChargeOrderNo(String account)
	{
		Date now = new Date();
		String chOrCode=""+ now.getTime()/1000;
		
		int len=0;
		if(account==null )
			len=0;
		else
		{
			len = account.length();
		}
		Random random = new Random();
		if(len>=11)
		{
			chOrCode += account.substring(0, 11);
		}
		else
		{
			chOrCode += account + StringUtil.repeat("9", 11-len);
		}
	
		return chOrCode;
	}
	
	public static void addChargeStat(int pkEpGunId,int chargeMeterNum,int chargeTime,int chargeAmt)
	{
		TblElectricPileGun info= new TblElectricPileGun();
		info.setPkEpGunId(pkEpGunId);
		
		info.setTotalChargeMeter(UtilProtocol.intToBigDecimal3(chargeMeterNum));
		info.setTotalChargeTime(chargeTime);
		info.setTotalChargeAmt(UtilProtocol.intToBigDecimal2(chargeAmt));
	    
		DB.epGunDao.addChargeStat(info);
	}
	
	public static int getOrType(int level)
	{
		int chorType=1;
		if(level==6)
			chorType=1;
		else if(level == 5)
			chorType=2;
		else
			chorType=3;
		
		return chorType;
	}
	
	private static TblPowerModule converPowerModule(ChargePowerModInfo info)
	{
		TblPowerModule powerModule = new TblPowerModule();
		
		
		powerModule.setPkPowerModuleid( info.getPkId());
		//1.输出电压
		powerModule.setOutput_voltage((new BigDecimal(info.getOutVol())).multiply(Global.Dec1));
		
		//2.输出电流
		powerModule.setOutput_current((new BigDecimal(info.getOutCurrent())).multiply(Global.Dec2));
		
		//3.A相电压
		powerModule.setA_voltage((new BigDecimal(info.getaVol())).multiply(Global.Dec1));
		
		//4.B相电压
		powerModule.setB_voltage((new BigDecimal(info.getbVol())).multiply(Global.Dec1));
		
		//5.C相电压
		powerModule.setC_voltage((new BigDecimal(info.getcVol())).multiply(Global.Dec1));
		
		//6.A相电流
		powerModule.setA_current((new BigDecimal(info.getaCurrent())).multiply(Global.Dec2));
		
		//7.B相电流
		powerModule.setB_current((new BigDecimal(info.getbCurrent())).multiply(Global.Dec2));
		
		//8.C相电流
		powerModule.setC_current((new BigDecimal(info.getcCurrent())).multiply(Global.Dec2));
		
		//9.模块温度
		powerModule.setTemperature((new BigDecimal(info.getTemp())).multiply(Global.Dec1));
		
		return powerModule;
	
	}
	
	public static void updatePowerInfoToDB(ChargePowerModInfo info){
		
		if(info==null )
		{
			logger.error("updatePowerInfoToDB error!ChargePowerModInfo:{}",info);
			return ;
		}
		
		TblPowerModule dbInfo = converPowerModule(info);
		
		dbInfo.setPkPowerModuleid(info.getPkId());

		DB.powerModuleDao.update(dbInfo);
		
	}
	public static void insertPowerInfoDB(ChargePowerModInfo info,String chargeSerialNo)
	{
		if(info==null || chargeSerialNo==null || chargeSerialNo.length()!=32)
		{
			logger.error("insertPowerInfoDB error!ChargePowerModInfo:{},chargeSerialNo",info,chargeSerialNo);
			return ;
		}
		
		TblPowerModule dbInfo = converPowerModule(info);
		dbInfo.setChargeSerialNo(chargeSerialNo);
		
		dbInfo.setPowerModuleName("电源模块_"+info.getModId());
		
	
		int pkId = DB.powerModuleDao.insert(dbInfo);
		
		info.setPkId(pkId);
		
	}
	
	public  static void savePowerModule( Map<Integer,ChargePowerModInfo> mapPowerModule,String chargeSerialNo)
	{
		logger.debug("savePowerModule");
		if( mapPowerModule.size()== 0) //没有数据需要更新
		{
			return;
		}
        Iterator iter = mapPowerModule.entrySet().iterator();
        
		while (iter.hasNext()) {
			
			Map.Entry entry = (Map.Entry) iter.next();
			
			ChargePowerModInfo powerModInfo = (ChargePowerModInfo)entry.getValue();
			if(powerModInfo==null || powerModInfo.getChange()!=1  )
				continue;
			
			if(powerModInfo.getPkId()==0)
				insertPowerInfoDB(powerModInfo,chargeSerialNo);
			else
			{
				updatePowerInfoToDB(powerModInfo);
			}
			
			powerModInfo.setChange(0);
		}
	}
	
	private static TblVehicleBattery convertChargeCarInfo(ChargeCarInfo info)
	{
		TblVehicleBattery battery = new TblVehicleBattery();
		
		
		battery.setBattery_manufacturers( info.getBatteryManufacturer());
		
		battery.setBattery_rated_capacity((int)info.getTotalBattryCapacity());
		battery.setBattery_type((int)info.getBattryType());
		battery.setCycle_count((int)info.getBattryChargeTimes());
		battery.setMax_current((new BigDecimal(info.getSignleAllowableHighCurrent())).multiply(Global.Dec2));
		battery.setMax_temperature((new BigDecimal(info.getAllowableHighTotalTemp())).multiply(Global.Dec1));
		battery.setSingle_max_vol((new BigDecimal(info.getSignleAllowableHighVol())).multiply(Global.Dec1));
		battery.setTotal_energy((new BigDecimal(info.getTotalBattryPower())).multiply(Global.Dec1));
		battery.setVin(info.getCarIdenty());
		battery.setTotal_rated_voltage((new BigDecimal(info.getAllowableHighTotalVol())).multiply(Global.Dec1));
		
		int year=info.getBattryMadeYear();
		int day = info.getBattryMadeDay();
		
		String dateString=String.format("%04d%02d%02d",year,day/100,day%100);		
		battery.setProduction_date(DateUtil.parse(dateString));
		
		
		return battery;
	}
	
	public static void updateChargeCarInfoToDB(ChargeCarInfo info){
		
		if(info==null )
		{
			logger.error("updatePowerInfoToDB error!ChargePowerModInfo:{}",info);
			return ;
		}
		
		TblVehicleBattery dbInfo = convertChargeCarInfo(info);
		
		dbInfo.setPk_VehicleBattery(info.getPkId());
		
		DB.vehicleBatteryDao.update(dbInfo);
		
	}
	public static void insertChargeCarInfoToDB(ChargeCarInfo info,String chargeSerialNo)
	{
		if(info==null || chargeSerialNo==null || chargeSerialNo.length()!=32)
		{
			logger.error("insertChargeCarInfoToDB error!ChargePowerModInfo:{},chargeSerialNo",info,chargeSerialNo);
			return ;
		}
		
		TblVehicleBattery dbInfo = convertChargeCarInfo(info);
		dbInfo.setChargeSerialNo(chargeSerialNo);
		
		int pkId =DB.vehicleBatteryDao.insert(dbInfo);
	
		info.setPkId(pkId);
		
	}
	
	public static void onChargeFail(int userId,ChargeCache chargeCache)
	{
		if(chargeCache.getStatus() == ChargeStatus.CS_CHARGE_FAIL)
		{
			logger.error("onChargeFail had handle,chargeCache:{}",chargeCache);
			return ;
		}
		chargeCache.setStatus(ChargeStatus.CS_CHARGE_FAIL);
		
		try{
		
			TblChargingrecord record = new TblChargingrecord();
			record.setChreTransactionnumber(chargeCache.getChargeSerialNo());
			record.setStatus(ChargeStatus.CS_CHARGE_FAIL);
			DB.chargingrecordDao.updateBeginRecordStatus(record);
		}
		catch(Exception e)
		{
			logger.error("updateBeginRecordStatus exception.userId:{},chargeCache:{},getStackTrace:{}",
					new Object[]{userId,chargeCache,e.getStackTrace()});
		}
		
		
		//2.退钱
		BigDecimal bdFronzeAmt = UtilProtocol.intToBigDecimal2(chargeCache.getFronzeAmt());
		logger.info("onChargeFail,return user:{},fronze amt:{}",userId,bdFronzeAmt);
		UserService.addAmt(userId, bdFronzeAmt);
	}
	
	public static void saveVehicleBatteryToDb(ChargeCarInfo info,String chargeSerialNo)
	{
		logger.debug("saveVehicleBatteryToDb");
		if(info==null)
		{
			return ;
		}
		if(info.getPkId()==0)//第一次保存
		{
			insertChargeCarInfoToDB(info,chargeSerialNo);
		}
		else//有数据更新
		{
			updateChargeCarInfoToDB(info);
		}
	}
	
}
	