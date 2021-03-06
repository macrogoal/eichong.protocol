package com.usrlayer.cache;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ormcore.model.RateInfo;
import com.ormcore.model.TblBespoke;
import com.ormcore.model.TblElectricPileGun;
import com.usrlayer.config.GameBaseConfig;
import com.usrlayer.config.Global;
import com.usrlayer.constant.BaseConstant;
import com.usrlayer.constant.EpConstant;
import com.usrlayer.constant.EpConstantErrorCode;
import com.usrlayer.net.client.EpGateNetConnect;
import com.usrlayer.protocol.UtilProtocol;
import com.usrlayer.service.CacheService;
import com.usrlayer.service.EpBespokeService;
import com.usrlayer.service.EpChargeService;
import com.usrlayer.service.EpGateService;
import com.usrlayer.service.EpGunService;
import com.usrlayer.service.UserService;
import com.usrlayer.utils.DateUtil;

public class EpGunCache {
	
	private static final Logger logger = LoggerFactory.getLogger(EpGunCache.class + BaseConstant.SPLIT + GameBaseConfig.layerName);
	
	private int concentratorId;

	private int pkEpId; 
	
	private String  epCode;
	
	private int pkEpGunId;
	
	private int epGunNo;
	
	int startEnteryNum; //查询电桩业务起始位置

	int currentType;
	
	private int status;
	
	
	
	private BespCache bespCache;
	
	private ChargeCache chargeCache;
	
	//RealChargeInfo realChargeInfo;
	
	private boolean isNeedFronzeAmt;
	
	private int  curUserId;//
	private String curUserAccount;
	
	private String identyCode;// 识别码
	
	private long createIdentyCodeTime;//生成识别码的时间
	
	private long lastUDTime;//更新到数据库的信息
	
	private long lastUPTime;//手机更新时间
	
	private int cardOrigin;
	
	private long lastSendToMonitorTime; //记录上一次发送给监控系统的时间
	
	
	
	public long getLastSendToMonitorTime() {
		return lastSendToMonitorTime;
	}
	public void setLastSendToMonitorTime(long lastSendToMonitorTime) {
		this.lastSendToMonitorTime = lastSendToMonitorTime;
	}

	public EpGunCache(){
		
			
		lastUDTime =0;//更新到数据库的信息
		
		lastUPTime =0;//手机更新时间
		currentType =0;
		curUserAccount="";
		
		lastSendToMonitorTime = 0;//送实时监控的时间
		
		startEnteryNum=0;
		
		identyCode="";
		createIdentyCodeTime = 0;
		
	}
	
	
	public long getCreateIdentyCodeTime() {
		return createIdentyCodeTime;
	}
	public void setCreateIdentyCodeTime(long createIdentyCodeTime) {
		this.createIdentyCodeTime = createIdentyCodeTime;
	}
	
	public String getIdentyCode() {
		return identyCode;
	}
	
	public void setIdentyCode(String identyCode) {
		this.identyCode = identyCode;
	}
	
	
	public int getPkEpGunId() {
		return pkEpGunId;
	}

	public void setPkEpGunId(int pkEpGunId) {
		this.pkEpGunId = pkEpGunId;
	}

	public int getConcentratorId() {
		return concentratorId;
	}

	public void setConcentratorId(int concentratorId) {
		this.concentratorId = concentratorId;
	}

	public int getPkEpId() {
		return pkEpId;
	}
	
	@SuppressWarnings("unused")
	private void setUserInfo(int userId,String userAccount)
	{
		this.curUserId = userId;
		this.curUserAccount = userAccount;
	}
	public String getCurUserAccount() {
		return curUserAccount;
	}
	
	public int getCurChargeUserId()
	{
		if(curUserId!=0)
			return curUserId;
		else
		{
			if(this.chargeCache!=null)
			{
				return chargeCache.getUserId();
			}
			return curUserId;
		}
	}
	public int getCurBespokeUserId()
	{
		if(curUserId!=0)
			return curUserId;
		else
		{
			if(this.bespCache!=null)
			{
				return bespCache.getAccountId();
			}
			return curUserId;
		}
	}
	public int getCurUserId() {
		return curUserId;
	}
	

	
	public int getStartEnteryNum() {
		return startEnteryNum;
	}
	public void setStartEnteryNum(int startEnteryNum) {
		this.startEnteryNum = startEnteryNum;
	}
	

	

	public void setPkEpId(int pkEpId) {
		this.pkEpId = pkEpId;
	}

	public String getEpCode() {
		return epCode;
	}

	public void setEpCode(String epCode) {
		this.epCode = epCode;
	}

	public int getEpGunNo() {
		return epGunNo;
	}

	public void setEpGunNo(int epGunNo) {
		this.epGunNo = epGunNo;
	}

	public int getStatus() {
		return status;
	}

	public BespCache getBespCache() {
		
		 
		 BespCache retBespCache= null;
		 retBespCache = this.bespCache;
         
		return retBespCache;
	}
	
	public int getCardOrigin() {
		return cardOrigin;
	}
	public void setCardOrigin(int cardOrigin) {
		this.cardOrigin = cardOrigin;
	}
	public void setBespCache(BespCache bespCache) {
		
        this.bespCache = bespCache;
	}

	public ChargeCache getChargeCache() {
		 
		ChargeCache retChargeCache= null;
		retChargeCache = this.chargeCache;
         
		return retChargeCache;
	}

	public void setChargeCache(ChargeCache chargeCache) {
		 
		
		this.chargeCache = chargeCache;
	}

	

	public boolean isNeedFronzeAmt() {
		return isNeedFronzeAmt;
	}

	public void setNeedFronzeAmt(boolean isNeedFronzeAmt) {
		this.isNeedFronzeAmt = isNeedFronzeAmt;
	}
	public int checkSingleYx(int value)
	{
		int ret=0;
		if(value!=0 && value!=1)
		{
			ret = -1;
		}
		return ret;
	}
	
	public ChargingInfo getCharingInfo()
	{
		
		return null;
	}

	private void modifyStatus(int status,boolean modifyDb)
	{
		logger.debug("modifyStatus,this.status:{},status:{}",this.status,status);
		this.status = status;
		
		if(modifyDb)
		{
			EpGunService.updateGunState(this.getPkEpGunId(), status);
		}
	}
	
	/**
	 * 只负责清楚桩上的用户信息,用户自带的枪和状态信息由事件函数去处理
	 */
	public void cleanUserInfo()
	{
		this.curUserId=0;
		this.curUserAccount="";
	}
	
	public void cleanBespokeInfo()
	{
		if(bespCache!=null)
		{
			String userAccount = bespCache.getAccount();
			int userId = bespCache.getAccountId();
			if(userAccount.length()>0)
			{
				UserCache u1= UserService.getUserCache(userAccount);
				if(u1!=null && u1.getUseGunStaus()== 3)
				{
					u1.clean();
					CacheService.putUserCache(u1);
				}
			}
			if(userId>0)
			{
				UserCache u2= UserService.getUserCache(userId);
				if(u2!=null && u2.getUseGunStaus()== 3)
				{
					u2.clean();
					CacheService.putUserCache(u2);
				}
			}
		}
 		 setBespCache(null);
	}
	public void cleanChargeInfo()
	{
		if(chargeCache!=null)
		{
			String userAccount = chargeCache.getAccount();
			int userId = chargeCache.getUserId();
			if(userAccount.length()>0)
			{
				UserCache u1= UserService.getUserCache(userAccount);
				if(u1!=null )
				{
					u1.clean();
					CacheService.putUserCache(u1);
				}
			}
			if(userId>0)
			{
				UserCache u2= UserService.getUserCache(userId);
				if(u2!=null )
				{
					u2.clean();
					CacheService.putUserCache(u2);
				}
			}
		}
	}
		
	public int startChargeAction(String epCode,int epGunNo,int userId,
			String bespNo,short chargeStyle,int frozenAmt,int payMode,int orgNo, int fromSource, String actionIdentity)
	{
		if(payMode==1)
		{
			BigDecimal bdRemainAmt = UserService.getRemainBalance(userId);
			
			//100倍后转为整数
			bdRemainAmt = bdRemainAmt.multiply(Global.DecTime2);
			int nRemainAmt= UtilProtocol.BigDecimal2ToInt(bdRemainAmt);
			BigDecimal bdFrozenAmt = UtilProtocol.intToBigDecimal2(frozenAmt);
			//充电冻结金额
			logger.info("charge epCode:{},epGunNo:{},userId:{},bdRemainAmt:{},bdFrozenAmt:{}",new Object[]{epCode,epGunNo,userId,bdRemainAmt.doubleValue(),bdFrozenAmt.doubleValue()});
			
			//冻结金额
			if(nRemainAmt<0 || frozenAmt<=0 || nRemainAmt<frozenAmt)
			{
				return EpConstantErrorCode.EPE_NO_ENOUGH_MONEY;
			}
		}

		EpGateNetConnect commClient = CacheService.getEpGate(epCode);
		commClient.setLastSendTime(DateUtil.getCurrentSeconds());
		EpGateService.sendCharge(commClient.getChannel(), epCode, epGunNo, userId, frozenAmt, chargeStyle, 1);
	
		return 0;
	}
	
	public int stopChargeAction(int userId,EpGateNetConnect commClient)
	{
		commClient.setLastSendTime(DateUtil.getCurrentSeconds());
		EpGateService.sendStopCharge(commClient.getChannel(), epCode, epGunNo, userId);
		
		return 0;
	}
	
	
	public int startBespokeAction(UserCache userInfo,RateInfo rateInfo,int redo,int secBuyOutTime,String bespNo,
			int payMode,int orgNo,int cmdFromSource,String cmdIdentily)
	{	
		//1.充电桩未连接不能充电
		/*EpCommClient commClient = (EpCommClient)getEpNetObject();
		if(commClient==null || commClient.isComm()==false) {
			
			return EpConstantErrorCode.EP_UNCONNECTED;//
		}
		
		if( redo == 1 )
		{
			// 11.这个枪没有预约不能续约
			if(status != EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
			{
				
				return EpConstantErrorCode.BESP_NO_NOT_EXIST;
			}
			if (this.bespCache.getAccountId() != userInfo.getId()) {
				return EpConstantErrorCode.NOT_SELF_REDO_BESP;//
			}
		}
		if(redo == 0)
		{
			if(status ==EpConstant.EP_GUN_STATUS_EP_OPER)//用户使用状态，允许使用用户预约
			{
				if(this.curUserId!=0 && this.curUserId != userInfo.getId())
				{
					return EpConstantErrorCode.CAN_NOT_BESP_IN_BESP_COOLING;
				}
				return EpConstantErrorCode.USED_GUN;//
			}
			if(status == EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED)
				return EpConstantErrorCode.EPE_OTHER_BESP;//
			
			if(this.status == EpConstant.EP_GUN_STATUS_SETTING)
			 {
				return EpConstantErrorCode.EPE_IN_EP_OPER;
			 }
			if(this.status == EpConstant.EP_GUN_STATUS_SELECT_CHARGE_MODE)
			 {
				return EpConstantErrorCode.EPE_IN_EP_OPER;
			 }
			
		}
		if(status == EpConstant.EP_GUN_STATUS_IDLE )
		{
			status = EpConstant.EP_GUN_STATUS_EP_OPER;
		}
				
		if (redo == 1) {
			long bespTotalTime = this.bespCache.getEndTime() + secBuyOutTime
					- bespCache.getStartTime();
			if (bespTotalTime > (6 * 3600)) {
				return EpConstantErrorCode.BESP_TO_MAX_TIME;//
			}
		}
		

		BigDecimal fronzingAmt = RateService.calcBespAmt(rateInfo.getBespokeRate(),secBuyOutTime / 60);

		int userId= userInfo.getId();
		BigDecimal userRemainAmt= UserService.getRemainBalance(userId);
		logger.info("before startBespokeAction userId:{},payMode:{},remainAmt:{}",
				new Object[]{userId,payMode,userRemainAmt.doubleValue()});
		
		setUserInfo(userInfo.getId(), userInfo.getAccount());

		// 12.钱不够不能充电
		if (payMode == EpConstant.P_M_FIRST && userRemainAmt.compareTo(fronzingAmt)<0) {
			logger.error("startBespoke fail ,not enough! fronzing amt:{},userRemainAmt:{},curUserAccount{},bespNo{},epCode{},epGunNo{}"
					,new Object[]{fronzingAmt.doubleValue(),userRemainAmt,this.curUserAccount,bespNo,this.epCode,this.epGunNo});
			
			return EpConstantErrorCode.EPE_NO_ENOUGH_MONEY;//
		}
		if (redo ==1 && this.bespCache.getPayMode() == EpConstant.P_M_FIRST && userRemainAmt.compareTo(fronzingAmt)<0) {
			logger.error("startBespoke fail ,not enough! fronzing amt:{},userRemainAmt:{},curUserAccount{},bespNo{},epCode{},epGunNo{}"
					,new Object[]{fronzingAmt.doubleValue(),userRemainAmt,this.curUserAccount,bespNo,this.epCode,this.epGunNo});
			
			return EpConstantErrorCode.EPE_NO_ENOUGH_MONEY;//
		}
		

		byte bcdqno = (byte) epGunNo;
		byte bredo = (byte) redo;
		byte[] start_time = WmIce104Util.getP56Time2a();

		// todo:20150803
		String CardNo = new String("1234567891234567");// 充电卡号
		String CarCardNo = new String("1234567891234567");// 车牌号
		
		//String bespNoMd5 = Md5Encoder.encodeByMD5(bespNo);

		java.util.Date dt = new Date();
		long bespSt = dt.getTime() / 1000;

		if (redo == 0) {
			BespCache bespCacheObj = new BespCache();
			//this.curAction = EventConstant.EVENT_BESPOKE;
			//this.curActionOccurTime = DateUtil.getCurrentSeconds();
			this.curUserAccount = userInfo.getAccount();
			bespCacheObj.setAccount(userInfo.getAccount());
			bespCacheObj.setBespNo(bespNo);

			//bespCacheObj.setBespNoMd5(bespNoMd5);
			
			long et = bespSt + (long) (secBuyOutTime);
			
			bespCacheObj.setStartTime(bespSt);
			bespCacheObj.setEndTime(et);
			bespCacheObj.setRealEndTime(et);

			bespCacheObj.setAccountId(userInfo.getId());
			bespCacheObj.setRate(rateInfo.getBespokeRate());
			bespCacheObj.setStatus(EpConstant.BESPOKE_STATUS_CONFIRM);
			
			bespCacheObj.setPayMode(payMode);
			UserOrigin userOrigin = new UserOrigin(orgNo,cmdFromSource,cmdIdentily);
			
			bespCacheObj.setUserOrigin(userOrigin);
		
			this.bespCache = bespCacheObj;
			
		}
		bespCache.setBuyTimes(secBuyOutTime / 60);
		
		bespCache.setRedo((short)redo);
	
		if(ImitateEpService.IsStart())
			ImitateEpService.ImitateBespoke(this.epCode, this.epGunNo, redo, bespNo);
		
		else
		{
			byte[] bcdAccountNo = WmIce104Util.str2Bcd(userInfo.getAccount());
		
			byte[] orderdata = EpEncodeProtocol.do_bespoke(
					WmIce104Util.str2Bcd(epCode), bespNo, bcdqno, bredo,
					bcdAccountNo, WmIce104Util.str2Bcd(CardNo), start_time,
					(short)(secBuyOutTime/60), StringUtil.repeat("0", 16).getBytes());
	
			byte[] cmdTimes = WmIce104Util.timeToByte();
			
			String messagekey = String.format("%03d%s", Iec104Constant.C_BESPOKE,bespCache.getBespNo());
			
			EpMessageSender.sendRepeatMessage(commClient, messagekey, 0, 0,Iec104Constant.C_BESPOKE, orderdata,cmdTimes,commClient.getCommVersion());
			logger.info("startBespoke fronzing userId,{},amt:{},bredo:{},curUserAccount{},bespNo{},epCode{},epGunNo{}"
					,new Object[]{userId,fronzingAmt.doubleValue(),bredo,this.curUserAccount,bespNo,this.epCode,this.epGunNo});
		   
		}*/					
		return 0;
		
	}
	public int stopBespokeAction(int source,String srcIdentity ,String bespno)
	{
		//this.curAction = EventConstant.EVENT_CANNEL_BESPOKE;
		//this.curActionOccurTime = DateUtil.getCurrentSeconds();
		
		/*if(ImitateEpService.IsStart())
			ImitateEpService.ImitateCancelBespoke(srcIdentity, source, bespno);
		
		else
		{
			byte[] sendMsg = EpEncodeProtocol.do_cancel_bespoke(epCode, this.epGunNo,bespno);
			
			byte[] cmdTimes = WmIce104Util.timeToByte();
			EpCommClient commClient = (EpCommClient)this.epNetObject;
			EpMessageSender.sendMessage(commClient, 0, 0, Iec104Constant.C_CANCEL_BESPOKE, sendMsg,cmdTimes,commClient.getCommVersion());
			logger.info("stopBespoke curUserAccount:{},bespNo:{},epCode:{},epGunNo:{}"
					,new Object[]{this.bespCache.getAccount(),bespno,this.epCode,this.epGunNo});
		  
		}*/
	
		return 0;
	}
	
	public UserCache getChargeUser(int userIdInCache,String userAccountInCache,int chargeStyle,String account)
	{
		int userId =  getCurChargeUserId();
		
		UserCache u = null;
		if(userIdInCache==0)
		{
			
			if(chargeStyle == 1)
			{
				u= UserService.getUserCache(account);
			}
			else
			{
				u=UserService.getCardUser(account);
			}
		}
		else
		{
			u= UserService.getUserCache(userId);
			if(u == null)
			{
			//	logger.info("SSSSSSSSSSSSSSSSSSSSSSSss,curUserId:{},curUserAccount:{}",curUserId,curUserAccount);
				
				u= UserService.getUserCache(userAccountInCache);
			}
		}
		
		return u;
	}

	public boolean init(ElectricPileCache epCache,int epGunNo)
	{
		String epCode = epCache.getCode();
		int currentType = epCache.getCurrentType();
	
		if(currentType!=EpConstant.EP_DC_TYPE && currentType!= EpConstant.EP_AC_TYPE)
		{
			logger.error("init error!invalid current type:{}",currentType);
			return false;
		}
		
		this.currentType = currentType;
		TblElectricPileGun tblEpGun= EpGunService.getDbEpGun(pkEpId,epGunNo);
		if(tblEpGun==null)
		{
			logger.error("init error!did not find gun,pkEpId:{},epGunNo:{}",pkEpId,epGunNo);
			return false;
		}
		
		this.chargeCache = null;
		this.bespCache = null;
	
		this.setPkEpGunId(tblEpGun.getPkEpGunId());
		
		this.concentratorId = epCache.getConcentratorId();
		this.identyCode = tblEpGun.getQr_codes();
		this.createIdentyCodeTime = tblEpGun.getQrdate()-EpConstant.IDENTYCODE_INVALID_TIME2;
		
		
		
		int epGunStatusInDb= tblEpGun.getEpState();
		//以数据库最后枪头状态为准
		this.modifyStatus(epGunStatusInDb, false);

		//2.取最新的预约中的预约记录
		BespCache tmpBespCache=null;
		TblBespoke besp = EpBespokeService.getUnStopBespokeFromDb(this.pkEpId, this.pkEpGunId);
		if (besp != null) {
			tmpBespCache = EpBespokeService.makeBespokeCache(besp);
			
			// 检查是否过期,如果过期.那么结算
			long diff  = EpBespokeService.expireTime(tmpBespCache);
		
			if (diff > 0) {
				//结算
				Date now = new Date();
				tmpBespCache.setRealEndTime(now.getTime()/1000);
				if(tmpBespCache.getRealEndTime() > tmpBespCache.getEndTime())
				{
					tmpBespCache.setRealEndTime(tmpBespCache.getEndTime());
				}
				
				//BigDecimal realBespAmt = EpBespokeService.statBespoke(tmpBespCache);
				//EpBespokeService.endBespoke(epCode, realBespAmt, tmpBespCache, now);
			
				tmpBespCache=null;
			} else {
				tmpBespCache.setStatus(EpConstant.BESPOKE_STATUS_LOCK);
			}
		}
		if(tmpBespCache!=null)
		{
			
			String chargeAccount = tmpBespCache.getAccount();
			//装载未完成充电用户
			UserCache userCache = UserService.getUserCache(chargeAccount);
			
			if(userCache!=null)
			{
				userCache.setUseGun(epCode + epGunNo);
				userCache.setUseGunStaus(EpConstant.EP_GUN_STATUS_BESPOKE_LOCKED);
			}
			this.bespCache = tmpBespCache;
			
		}
		logger.error("init 1");
		//3.取最新的未完成的充电记录
		ChargeCache tmpChargeCache=EpChargeService.GetUnFinishChargeCache(epCode, epGunNo);
		if(tmpChargeCache!=null)
		{
			logger.debug("tmpChargeCache.getStatus():{}",tmpChargeCache.getStatus());
			logger.error("init 2");
			String chargeAccount = tmpChargeCache.getAccount();
			//装载未完成充电用户
			UserCache userCache = UserService.getUserCache(chargeAccount);
			
			logger.error("init 4");
			if(userCache!=null)
			{
				logger.error("init 5");
				if(tmpChargeCache.getPkUserCard()>0)
				{
					logger.error("init 6");
					ChargeCardCache cardCache=UserService.getChargeCardCache(tmpChargeCache.getPkUserCard());
					userCache.setCard(cardCache);
				}
				userCache.setUseGun(epCode + epGunNo);
				userCache.setUseGunStaus(EpConstant.EP_GUN_STATUS_CHARGE);
			}
			tmpChargeCache.getUserOrigin().setCmdChIdentity(userCache.getAccount());
			this.chargeCache = tmpChargeCache;
		}
		
		logger.error("init 7");
		if(chargeCache!=null)
		{
			logger.error("init 8");
			this.setUserInfo(chargeCache.getUserId(), chargeCache.getAccount());
		}
		else
		{
			logger.error("init 9");
			if(bespCache!=null)
			{
				logger.error("init 10");
				
				this.setUserInfo(bespCache.getAccountId(),  bespCache.getAccount());
				
			}
		}
		
	
		String msg = MessageFormat.format("gun init status:{0},epcode:{1},gunno:{2}",
    		   status,this.epCode,this.epGunNo);
		logger.info(msg);
		
		return true;
	}


	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		
        sb.append("EpGunCache\n");
        sb.append("集中器pkId = ").append(this.getConcentratorId()).append("\n");
        sb.append("电桩pkId = ").append(this.getPkEpId()).append("\n");
        sb.append("电桩编号 = ").append(this.getEpCode()).append("\n");
        
        sb.append("枪口pkId = ").append(this.pkEpGunId).append("\n");
        sb.append("枪口编号 = ").append(this.epGunNo).append("\n");
        sb.append("当前用户ID = ").append(curUserId).append("\n");
        sb.append("当前用户账号 = ").append(curUserAccount).append("\n");
        
        sb.append("识别码 = ").append(identyCode).append("\n");
        
        String sTime= DateUtil.StringYourDate(DateUtil.toDate(createIdentyCodeTime*1000));
        sb.append("识别码产生时间 = ").append(sTime).append("\n");
        
        sTime= DateUtil.StringYourDate(DateUtil.toDate(lastUDTime*1000));
        
        sb.append("数据库实时数据更新时间 = ").append(sTime).append("\n");
        
        sTime= DateUtil.StringYourDate(DateUtil.toDate(lastUPTime*1000));
        sb.append("手机充电信息更新时间  = ").append(sTime).append("\n");
        
        if(this.bespCache ==null)
        {
        	sb.append("无预约\n\r\n");
        }
        else
        {
        	sb.append(this.bespCache.toString() ).append("\n");
        }
        
        if(this.chargeCache ==null)
        {
        	sb.append("无充电\n\r\n");
        }
        else
        {
        	sb.append(this.chargeCache.toString() ).append("\n");
        }
        
      
        return sb.toString();
	}
	
}
