package com.bsk.update.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	
	
	static final DecimalFormat DOUBLE_DECIMAL_FORMAT = new DecimalFormat("0.##");

    public static final int    MB_2_BYTE             = 1024 * 1024;
    public static final int    KB_2_BYTE             = 1024;
    
    
    /**
     * @param size
     * @return
     */
    //获取文件大小
    public static CharSequence getAppSize(long size) {
        if (size <= 0) {
            return "0MB";
        }

        if (size >= MB_2_BYTE) {
            return new StringBuilder(16).append(DOUBLE_DECIMAL_FORMAT.format((double)size / MB_2_BYTE)).append("MB");
        } else if (size >= KB_2_BYTE) {
            return new StringBuilder(16).append(DOUBLE_DECIMAL_FORMAT.format((double)size / KB_2_BYTE)).append("KB");
        } else {
            return size + "B";
        }
    }

    /**
     * @param progress
     * @param max
     * @return
     */
    public static String getNotiPercent(long progress, long max) {
        int rate = 0;
        if (progress <= 0 || max <= 0) {
            rate = 0;
        } else if (progress > max) {
            rate = 100;
        } else {
            rate = (int)((double)progress / max * 100);
        }
        return new StringBuilder(16).append(rate).append("%").toString();
    }
	
	/**
	 * �?��是否存在SDCard
	 * @return
	 */
	public static boolean hasSdcard(){
		String state = Environment.getExternalStorageState();
		if(state.equals(Environment.MEDIA_MOUNTED)){
			return true;
		}else{
			return false;
		}
	}
	
	
	
	/**
	 * 获取系统SD卡路�?
	 * @return
	 */
	public static String getSDCardPath() {
		return Environment.getExternalStorageDirectory().getPath();
	}
	
	/**
	 * 在SD卡上创建文件�?
	 * @param path
	 */
	public static void createDir(String path) {
        File file = new File(path);
        if(!file.exists()) {
        	file.mkdir();
        }
	}
	
	public static boolean inputstreamToFile(InputStream is, String path){
    	try {
    		File file = new File(path);
    		OutputStream os = new FileOutputStream(file);
    		int bytesRead = 0;
    		byte[] buffer = new byte[8192];
    		while ((bytesRead = is.read(buffer, 0, 8192)) != -1) {
				os.write(buffer, 0, bytesRead);
				
    		}	
			os.close();
			is.close();
		    
		    return true;
		    
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return false;  
    }
	
	public static boolean copyFile(File src, File dst) {
        try {
            if (!dst.exists()) {
                dst.createNewFile();
            }
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            int length = -1;
            byte[] buf = new byte[1024];
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
            out.flush();
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
	
	/**
	 * 保存Bitmap为图片文�?
	 * @param mBitmap
	 * @param filePath
	 * @param format
	 * @param quality
	 * @throws IOException
	 */
	public static void saveBitmap(Bitmap mBitmap, String filePath, CompressFormat format, int quality) throws IOException {  
	    	
		File file = new File(filePath);
	    file.createNewFile();  
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mBitmap.compress(format, quality, fOut);
		try {
			fOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 安装apk
	 * @param path
	 * @return
	 */
	public static String silenceInstall(String path) {
		String[] args = { "pm", "install", "-r", path };  
		String result = "";  
		ProcessBuilder processBuilder = new ProcessBuilder(args);  
		Process process = null;  
		InputStream errIs = null;  
		InputStream inIs = null;  
		try {  
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();  
		    int read = -1;  
		    process = processBuilder.start();  
		    errIs = process.getErrorStream();  
		    while ((read = errIs.read()) != -1) {  
		        baos.write(read);  
		    }  
		    baos.write('\n');  
		    inIs = process.getInputStream();  
		    while ((read = inIs.read()) != -1) {  
		        baos.write(read);  
		    }  
		    byte[] data = baos.toByteArray();  
		    result = new String(data);  
		} catch (IOException e) {  
		    e.printStackTrace();  
		} catch (Exception e) {  
		    e.printStackTrace();  
		} finally {  
		    try {  
		        if (errIs != null) {  
		            errIs.close();  
		        }  
		        if (inIs != null) {  
		            inIs.close();  
		        }  
		    } catch (IOException e) {  
		        e.printStackTrace();  
		    }  
		    if (process != null) {  
		        process.destroy();  
		    }  
		}  
		Log.e("", "result=" + result);
		return result;
	}
}
