package com.usrgate.net.client;

import io.netty.channel.Channel;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.usrgate.constant.EpGateConstant;
import com.usrgate.net.codec.EpGateDecoder;
import com.usrgate.service.CacheService;
import com.usrgate.service.EpGateService;

public class EpGateMessageHandler {
	/**
	 * 接受EpGate数据并处理
	 * @param channel
	 * @param message
	 * @throws IOException 
	 */
     public static void handleMessage(Channel channel, EpGateMessage message) {
		
    	 byte[] msg = message.getBytes();
    	
    	 
    	 try {
    	     processFrame(channel,message.getCmd(),msg);
    	 }catch (IOException e) {
				e.printStackTrace();
		}
	}

	public static void processFrame(Channel channel,int cmd,byte[] msg)
			throws IOException {
		
		if(!EpGateService.isValidCmd(cmd))
			return ;

		ByteBuffer byteBuffer = ByteBuffer.wrap(msg);

		// 指令数累加
		CacheService.setEpInstructStat(channel, cmd);
		
		switch (cmd) 
		{
		case EpGateConstant.EP_LOGIN:
		{
			EpGateDecoder.decodeLogin(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_ACK:
		{
			EpGateDecoder.decodeAck(channel, byteBuffer);
		}
		break;
		
		case EpGateConstant.EP_HEART://103	心跳
		{
			EpGateDecoder.decodeHeart(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_ONLINE://202	电桩在线
		{
			EpGateDecoder.decodeEpOnline(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_PHONE_ONLINE://203	手机在线
		{
			EpGateDecoder.decodePhoneOnline(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_PHONE_CONNECT_INIT://1001	手机连接初始化(带部分充电逻辑)
		{
			EpGateDecoder.decodePhoneConnect(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_CHARGE://1002	充电
		{
			EpGateDecoder.decodeCharge(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_CHARGE_EVENT://1003	充电事件
		{
			EpGateDecoder.decodeChargeEvent(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_STOP_CHARGE://1004	停止充电
		{
			EpGateDecoder.decodeStopCharge(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_CHARGE_REAL://1005	充电实时数据
		{
			EpGateDecoder.decodeChargeReal(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_CONSUME_RECORD://1006	消费记录
		{
			EpGateDecoder.decodeConsumeRecord(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_RE_INSERT_GUN://1100	重新插枪提示
		{
			EpGateDecoder.decodeReInsertGun(channel, byteBuffer);
		}
		break;
		case EpGateConstant.EP_YXYC:	//部分遥信遥测值
		{
			EpGateDecoder.decodeYxyc(channel, byteBuffer);
		}
		break;
		
		default:
		break;
		}

	}
}

