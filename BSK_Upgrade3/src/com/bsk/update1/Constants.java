package com.bsk.update1;

import java.io.File;

import com.bsk.update.util.FileUtils;

public class Constants {
	
	public static boolean isDebug = true;		//是否输出Log
	
	public final static  String UPDATE_PREFS = "update_prefs";
	
	
	public final static  String SD_PATH = FileUtils.getSDCardPath();
	
	
	public static String HOST = "http://120.24.231.164";
	public static String API_ACTIVATE = "/api/dev/login/login1";
	public static String API_APP_UPGRADE = "/api/dev/heartbeat/hb";
	
	
	public static String APP_UPDATE_ZIP = "update.zip";					//OTA升级包
	
	public static String APP_BSK_LAUNCHER = "com.bsk.launcher";		//Launcher应用
	public static String APP_BSK_GALLERY = "com.bsktek.gallery";		//云相册应用
	public static String APP_BSK_MUSIC = "com.bsktek.music";			//音乐应用
	public static String APP_BSK_CALENDAR = "com.bsktek.calendar";		//日历应用
	public static String APP_BSK_UPDATE = "com.bsk.update";			//升级应用
	
	
	public static final String VISIT_SERVER_ERROR = "{\"ret\":\"1\",\"msg\":\"访问服务器错误!\"}";
}
