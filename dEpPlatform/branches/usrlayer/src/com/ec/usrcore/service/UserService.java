package com.ec.usrcore.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ec.constants.ErrorCodeConstants;
import com.ec.constants.UserConstants;
import com.ec.usrcore.cache.UserCache;
import com.ec.usrcore.cache.UserRealInfo;
import com.ec.utils.LogUtil;
import com.ormcore.dao.DB;
import com.ormcore.model.TblUserInfo;

public class UserService {
	
	private static final Logger logger = LoggerFactory.getLogger(LogUtil.getLogName(UserService.class.getName()));
	
	public static UserCache getUserCache(String account)
	{
		UserCache userInfo = CacheService.getMapEpUserInfo().get(account);
		if(userInfo!=null)
		{
			return userInfo;
		}
		UserRealInfo userRealInfo =  findUserRealInfo(account);
		if(userRealInfo==null)
			return null;
		
		return CacheService.convertToCache(userRealInfo);
	}
	public static UserCache getUserCache(int accountId)
	{
		UserCache userInfo = CacheService.getMapEpUserInfo().get(accountId);
		if(userInfo!=null)
		{
			return userInfo;
		}
		UserRealInfo userRealInfo =  findUserRealInfo(accountId);
		if(userRealInfo==null)
			return null;
		
		return CacheService.convertToCache(userRealInfo);
	}
	
	public static BigDecimal getRemainBalance(int accountId)
	{	
		UserRealInfo u = findUserRealInfo(accountId);
		//假设数据
		if(null != u)
		{
			return u.getMoney();
		}
		return new BigDecimal(0.0);
	}
    
	public static UserRealInfo convertUsrRealInfo(TblUserInfo tblUserInfo)
	{
		try
		{
		if(tblUserInfo == null)
			return null;
		
		UserRealInfo u= new UserRealInfo();
		
		int usrId = tblUserInfo.getId();
		String usrAccount = tblUserInfo.getPhone();
		
		u.setName(tblUserInfo.getName());
		u.setAccount(usrAccount);
		u.setId(usrId);
		u.setPassword(tblUserInfo.getPassword());
		u.setStatus(tblUserInfo.getStatus());
		u.setLevel(tblUserInfo.getLevel());
		
		u.setDeviceid(tblUserInfo.getDeviceid());
		u.setInvitePhone(tblUserInfo.getInvitePhone());
		u.setMoney(tblUserInfo.getBalance());
	
		logger.info("convertUsrRealInfo,u.getId():{},u.getLevel():{}",u.getId(),u.getLevel());
		
		
		return u;
		}
		catch(Exception e)
		{
			logger.error("convertUsrRealInfo, e.getMessage():{}",e.getMessage());;
			return null;
		}
	}
    
    public static UserRealInfo findUserRealInfo(int userId)
    {
    	UserRealInfo userInfo=null;
    	
    	List<TblUserInfo> userInfoList=DB.userInfoDao.findUserInfoById(userId);
		if(null != userInfoList && userInfoList.size()!=1)
		{
			return userInfo;
		}
		
		userInfo = convertUsrRealInfo(userInfoList.get(0));
		CacheService.addUserInfo(userId, userInfo);
		
		return userInfo;
    }
    public static UserRealInfo findUserRealInfo(String userAccount)
    {
    	UserRealInfo userInfo=null;
    	
    	List<TblUserInfo> userInfoList = DB.userInfoDao.findUserInfoByPhone(userAccount);
		if(null != userInfoList && userInfoList.size()!=1)
		{
			return userInfo;
		}
		
		TblUserInfo dbUser = userInfoList.get(0);
		
		return convertUsrRealInfo(dbUser);
    }
    public static int findUserId(int orgNo, String userAccount)
    {
		int accountId = 0;

		if (orgNo == UserConstants.ORG_I_CHARGE) {
			List<TblUserInfo> userInfoList = DB.userInfoDao.findUserInfoById(Integer.valueOf(userAccount));
			if (null != userInfoList && userInfoList.size() == 1) {
				accountId = userInfoList.get(0).getId();
			}
		} else {
			accountId = DB.userBusinessDao.findUserInfoByOrgNo(orgNo);
		}
		
		return accountId;
    }
    
    public static int checkUserRealInfo(int userId)
    {
    	List<TblUserInfo> userInfoList=DB.userInfoDao.findUserInfoById(userId);
		if(null != userInfoList && userInfoList.size()!=1)
		{
			return ErrorCodeConstants.INVALID_ACCOUNT;
		}
		
		TblUserInfo dbUser = userInfoList.get(0);
		if (dbUser.getStatus() != 1) {
			return ErrorCodeConstants.INVALID_ACCOUNT_STATUS;
		}
		
		return 0;
    }
    public static UserCache getMemUser(int usrId,String usrAccount)
    {
    	UserCache u=null;
    	if(usrId>0)
    		u= getUserCache(usrId);
    	if(u==null)
    		u= getUserCache(usrAccount);
    	
    	if(u!=null)
    	{
    		if(u.getId()!=usrId || !u.getAccount().equals(usrAccount))
    		{
    			logger.error("getMemUser usrId,usrAccount",
    					new Object[]{u.getId(),u.getAccount(),usrId,usrAccount});
    		}
    	}
    	return u;
    	
    }
	public static int canCharge(int usrId)
    {
    	UserRealInfo  realU = CacheService.getUserInfo(usrId);
		if(null == realU)
		{
			return ErrorCodeConstants.INVALID_ACCOUNT;
		}
		int errorCode = realU.canCharge();
		if(errorCode>0)
			return errorCode;
		
		return 0;
    }
    
}
