package com.epcentre.server;

import io.netty.channel.Channel;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epcentre.service.AppApiService;


/**
 * 接受（App后台服务器）的数据并处理
 * @author 
 * 2015-3-17 下午4:49:26
 */
public class InnerApiMessageHandler {

	private static final Logger logger = LoggerFactory.getLogger(InnerApiMessageHandler.class);
	
	/**
	 *  转发App后台服务器发送的消息给电桩逻辑服务器
	 * @author 
	 * 2014-12-1
	 * @param channel
	 * @param message
	 */
	public static void handleMessage(Channel channel, ApiMessage message){
		logger.debug("Api handleMessage enter\n");
		short protocolNum = message.getProtocolNum();
		int[] userIds = message.getUserIds();
		byte[] bytes = message.getBytes();
	
		
		int type =protocolNum/100;
		if(type == 103)
		{
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		
				 switch (protocolNum) {
		         case 10301:
		             //app后台服务器--预约充电
		        	 AppApiService.bespoke(channel, byteBuffer);
		             break;
		         case 10302:
		             //app后台服务器--取消预约
		        	 AppApiService.cancelBespoke(channel, byteBuffer);
		             break;
		         case 10303:
		             //app后台服务器--开始充电
		        	// AppApiService.startElectricize(channel, byteBuffer);
		             break;
		         case 10304:
		             //app后台服务器--结束充电
		        	 AppApiService.stopElectricize(channel, byteBuffer);
		             break;
		         case 10305:
		         	//app后台服务器--私有电桩运营时间
		        	 AppApiService.stopElectricize(channel, byteBuffer);
		        	 break;
		         case 10306:
			         	//app后台服务器--下发费率
		        	 AppApiService.RateCmd(channel, byteBuffer);
		        	 break;
		         case 10307://app后台服务器--二维码
			         	
		        	 AppApiService.updateQRCode(channel, byteBuffer);
		        	 break;
		         case 10308://app后台服务器--改变电桩IP
			         	
		        	 AppApiService.changeEpGate(channel, byteBuffer);
		             break;
		         case 10309://降地锁
			         	
		        	 AppApiService.dropCarPlaceLock(channel, byteBuffer);
		        	 	break;
		         case 10310://闪LED灯
			         	
		        	 AppApiService.flashLed(channel, byteBuffer);
			             break;
		         case 10311://新版充电
			         	
		        	 AppApiService.startElectricize2(channel, byteBuffer);
			             break;
		         case 10312://远程升级
			         	
		        	 AppApiService.updateHexFile(channel, byteBuffer);
			             break;
		         case 10313://集中器配置
		        	 AppApiService.concentratorConfig(channel, byteBuffer);
			             break;
			      default:
			    	  logger.error("Api handleMessage protocolNum:{} is invalid",protocolNum);
			    	     break;
		     }
		}
		
	}
	
	
	
}
