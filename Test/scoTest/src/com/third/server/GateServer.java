package com.third.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netCore.server.impl.AbstractGameServer;
import com.third.config.GameConfig;
import com.third.service.ThirdConfigService;

public class GateServer extends AbstractGameServer{
	private static final Logger logger = LoggerFactory.getLogger(GateServer.class.getName());
	
	private static GateServer gameServer;
	
	private static Object lock = new Object();
	
	public GateServer(){
		//super();
	
		//创建netty服务器
		/*ServerConfigs serverConfigs = CoreConfig.serverConfigs;
		if (serverConfigs != null) {
			
			ServerConfig gateSvrCfg = serverConfigs.get("third-server");
			//第三方服务
			if (gateSvrCfg !=null) {
				
				ByteToMessageDecoder decoder = new EpGateDecoder();
				@SuppressWarnings("rawtypes")
				MessageToByteEncoder encoder = new EpGateEncoder();
			
				EpGateServer nettyServer = new EpGateServer(gateSvrCfg, decoder, encoder,0,0);
				nettyServerList.add(nettyServer);
			}else {
				String errMsg = "【Third服务器】缺少【第三方接口】访问配置...服务器强行退出！";
				logger.error(errMsg);
				throw new RuntimeException(errMsg);
			}
		}*/
	}
	
	/**
	 * 创建服务端服务器
	 * @author 
	 * 2014-11-28
	 * @return
	 */
	public static GateServer getInstance(){
		synchronized(lock){
			if(gameServer==null){
				gameServer = new GateServer();
			}
		}
		return gameServer;
	}
	
	public void init(){
		super.init();
		new GameConfig();//初始化服务器基础配置
		ThirdConfigService.initThirdConfigs();
		ThirdConfigService.writeMeter("3301021010000005", 14, 1.1);
		//new GameContext();//初始化spring,加载数据库全局数据
		
		logger.info("初始化服务成功...");
	}
	
	@Override
	public void start() {
		logger.info("start");
	
		super.start();
		
	}

	@Override
	public void stop() {
		
		//1、停止 netty服务器、停止 netty客户端、关闭线程池、关闭任务池
		super.stop();
	}
	
	@Override
	public void startTimerServer() {

		SCOService.startThirdPushTimeout(10);
		
		super.startTimerServer();
		
		logger.info("所有定时任务启动成功!");
	}
}