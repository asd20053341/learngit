package com.bsk.update.util;

import com.bsk.update1.Constants;

import android.util.Log;

public class LogUtils {

	public  static void i(String msg) {
		if(Constants.isDebug) {
			Log.i("", msg);
		}
	}
	
	public  static void e(String msg) {
		if(Constants.isDebug) {
			Log.e("", msg);
		}
	}
}
