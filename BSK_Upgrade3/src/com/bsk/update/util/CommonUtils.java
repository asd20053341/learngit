package com.bsk.update.util;

import android.content.Context;
import android.net.ConnectivityManager;

public class CommonUtils {
	private static Context mContext;
	
	/**
	 * ̬获取当前网络状态
	 */
	public static boolean getNetWorkStatus(Context context) {
		boolean netSataus = false;
		ConnectivityManager cwjManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		cwjManager.getActiveNetworkInfo();
		
		if (cwjManager.getActiveNetworkInfo() != null) {
			netSataus = cwjManager.getActiveNetworkInfo().isAvailable();
		}
		return netSataus;
	}
	
	
	/** 
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素) 
     */  
    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }
}
