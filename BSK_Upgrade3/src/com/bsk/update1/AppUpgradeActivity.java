package com.bsk.update1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.R.integer;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.bsk.update.adapter.AppUpdateListAdapter;
import com.bsk.update.model.AppInfo;
import com.bsk.update.model.AppInfoListResp;
import com.bsk.update.model.UpdateModel;
import com.bsk.update.util.FileUtils;
import com.bsk.update.util.HttpUtils;
import com.bsk.update.util.LogUtils;
import com.bsk.update.util.MD5FileUtil;
import com.bsk.update1.OtaOnlineActivity.DownloadChangeObserver;
import com.bsk.update1.R;
import com.google.gson.Gson;


public class AppUpgradeActivity extends Activity {
	
	private Button checkBtn;
	private Button upgradeBtn;

	private ListView mListView;
	private List<UpdateModel> dataList;
	private AppUpdateListAdapter adapter;
	
	private String[] appPkgs;
	
	
	private DownloadManager dowanloadmanager = null;  
    private DownloadChangeObserver downloadObserver;
    
    private Handler mHandler;
	private JSONObject dataResponse;
    
    private String urlPath;
    private String scode;
	private AppInfoListResp appInfoListResp;
	
    private long lastDownloadId;
    private Handler dlHandler;
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");
	private Resources res;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_upgrade);
		
		
		//配置需要升级的应用包名
		appPkgs = new String[] {Constants.APP_BSK_GALLERY, Constants.APP_BSK_MUSIC, Constants.APP_BSK_CALENDAR};
		
		checkBtn = (Button)this.findViewById(R.id.check_app_btn);
		upgradeBtn = (Button)this.findViewById(R.id.all_upgrade_btn);
		
		mListView = (ListView)this.findViewById(R.id.app_list);
		
		dataList = new ArrayList<UpdateModel>();
		
		initAppData();
		
		
		SharedPreferences settings = getSharedPreferences(Constants.UPDATE_PREFS,0);
        scode = settings.getString("Scode", "");
		
		
		final Runnable getAppInfoRunnable = new Runnable() {
			@Override
			public void run() {

						urlPath = Constants.HOST + Constants.API_APP_UPGRADE
								+ "?scode=" + scode;
						//		+ "&app1=update.zip,,";
						
						for(int i=0;i<dataList.size();i++) {
							urlPath += "&app" + (i+1) + "=" + dataList.get(i).getPkgName() + ",," ;
						}
						
						
						LogUtils.e("path= " + urlPath);
						
						Message msg = mHandler.obtainMessage();
						
						String jsonString = HttpUtils.doGet(urlPath);
						LogUtils.e("app info jsonString= " + jsonString);
						if(!TextUtils.isEmpty(jsonString)) {
							try {
								
								Gson gson = new Gson();
								appInfoListResp = gson.fromJson(jsonString, AppInfoListResp.class);
								
								int result = appInfoListResp.getRet();
								LogUtils.e("result=" + result);
								
								msg.what = result;
								Bundle bundle = new Bundle();
								
								if(result!=0) {
									String msgStr = dataResponse.getString("msg");
									bundle.putString("msg", msgStr);
								}
								
								msg.setData(bundle);
								msg.sendToTarget();
								
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
				
			}
		};
		
		mHandler = new Handler() {
        	public void handleMessage(Message msg) {
        		super.handleMessage(msg);
        		LogUtils.e("msg.what= " + msg.what);
        		Bundle bundle = null;
        		bundle = msg.getData();
        		
        		if(msg.what==0) {	//获取AppInfo成功		
						
						List<AppInfo> appInfos = appInfoListResp.getApps();
						if(appInfos!=null && appInfos.size()>0) {
							for(int i=0;i<appInfos.size();i++) {
								
								AppInfo appInfo = appInfos.get(i);
								
								for(int j=0;j<dataList.size();j++) {
									if(appInfo.getPkgname().equals(dataList.get(j).getPkgName())) {
										
										dataList.get(j).setDownloading(false);
										dataList.get(j).setUpgraded(false);
										
										dataList.get(j).setPkgUrl(appInfo.getPkgurl());
										dataList.get(j).setNewCode(Integer.parseInt(appInfo.getPkgver()));
										dataList.get(j).setAppMd5(appInfo.getPkgmd5());
										
										dataList.get(j).setFileSize(Long.parseLong(appInfo.getPkgsize()));
										
										if(!dataList.get(j).isDownloading()) {	//未下载中的应用更新状态

											String updateTip = "";
											if(dataList.get(j).getNewCode() > dataList.get(j).getVersionCode()) {
												updateTip = res.getString(R.string.find_new_version);
											//	dataList.get(j).setDownloading(true);
											}else {
												updateTip = res.getString(R.string.is_new_version);
												dataList.get(j).setDownloading(false);
											}
											dataList.get(j).setUpdateTip(updateTip);
										}
									}
								}
							}
							
							adapter.notifyDataSetChanged();
							
						}
						
				}else if(msg.what==2) {
					int position = bundle.getInt("pos");
					
					LogUtils.e("2---download star,pos=" + position);
					
					startDownload(position);
					
					
				}else {
        			String msgStr = bundle.getString("msg");
    				Toast.makeText(AppUpgradeActivity.this, msgStr, Toast.LENGTH_LONG).show();
        		}
        		
        	}
        };
		
		dlHandler = new Handler() {
        	public void handleMessage(Message msg) {
        		super.handleMessage(msg);
        		
        		switch (msg.what) {
                case 0:
                    int status = (Integer)msg.obj;
                    
                    if (isDownloading(status)) { //下载状态显示进度条，隐藏下载按钮
                    
                    //    downloadProgress.setVisibility(View.VISIBLE);
                    //    downloadProgress.setMax(0);
                    //    downloadProgress.setProgress(0);
                    //    downloadButton.setVisibility(View.GONE);
                    //    downloadSize.setVisibility(View.VISIBLE);
                    //    downloadPrecent.setVisibility(View.VISIBLE);
                    //    downloadCancel.setVisibility(View.VISIBLE);
                    //    downloadTip.setText(res.getString(R.string.download_tip));

                    	LogUtils.e("notify!! arg2=" + msg.arg2);
                    	adapter.notifyDataSetChanged();
                       
                    	if (msg.arg2 < 0) {	//文件总大小<0
                    //        downloadProgress.setIndeterminate(true);
                    //        downloadPrecent.setText("0%");
                    //        downloadSize.setText("0M/0M");
                            
                        } else { //下载中 ，显示大小，
                    //        downloadProgress.setIndeterminate(false);
                    //        downloadProgress.setMax(msg.arg2);
                    //        downloadProgress.setProgress(msg.arg1);
                    //        downloadPrecent.setText(FileUtils.getNotiPercent(msg.arg1, msg.arg2));  //设置百分比
                    //        downloadSize.setText(FileUtils.getAppSize(msg.arg1) + "/" + FileUtils.getAppSize(msg.arg2)); //设置文件大小比。
                    //        downloadTip.setText(res.getString(R.string.downloading));
                        	
                    //    	int downloadId = msg.arg1;
                        	
                        }
                        
                    } else {	//不是下载中状态，就隐藏进度条等，显示下载按钮并改变文字
                    	
                    	//清除下载状态
                    	LogUtils.e("msg.arg1(dId)=" + msg.arg1);
                    	int pos = 0;
                    	for(int i=0;i<dataList.size();i++) {
                    		if(dataList.get(i).getDownloadId() == msg.arg1) {
                    			dataList.get(i).setDownloadId(0);
                    			dataList.get(i).setDownloading(false);
                    			pos = i;
                    		}
                    	}

                        if (status == DownloadManager.STATUS_FAILED) {
                        	LogUtils.e("下载失败");
                     //       downloadButton.setText("下载失败");
                     //   	downloadTip.setText(res.getString(R.string.download_fail));
                        	
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        	LogUtils.e("下载成功!");
                     //       downloadButton.setText("下载成功");
                     //   	downloadTip.setText(res.getString(R.string.download_success));
                    //    	upgradeBtn.setEnabled(true);
                        	
                        	
                        	//验证MD5并安装
                        	LogUtils.e("md5=" + dataList.get(pos).getAppMd5() );
                        	
                        	if(!dataList.get(pos).isUpgraded()) {
                        		dataList.get(pos).setUpgraded(true);
                        		String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) 
                    					+ File.separator 
                    					+ dataList.get(pos).getFileName();
                        		File file = new File(filePath);
                        		try {
									String fileMd5 = MD5FileUtil.getFileMD5String(file);
									
									if(fileMd5.equals(dataList.get(pos).getAppMd5())) {
										LogUtils.e("install " + dataList.get(pos).getFileName());
										
										//silence install
										
									}else {
										Toast.makeText(AppUpgradeActivity.this, " md5 check fail!", Toast.LENGTH_SHORT).show();
									}
									
								} catch (IOException e) {
									e.printStackTrace();
								}
                        		
                        	}
                        	
                        	
                        	
                        } else {
                     //       downloadButton.setText("下载");
                     //   	downloadTip.setText(res.getString(R.string.download_tip));
                        }
                        
                        
                    }
                    break;
        		}
        	}
        };
        
        
        checkBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
				
				//需要处理没有scode情况
				if(!TextUtils.isEmpty(scode)) {
					new Thread(getAppInfoRunnable).start();
				}
			}
		});
        
        upgradeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
				for(int i=0;i<dataList.size();i++) {
					startDownload(i);
				}
				
			}
		});
        
        adapter = new AppUpdateListAdapter(this, dataList , mHandler);
		mListView.setAdapter(adapter);
		
		dowanloadmanager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);  
		registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));   
	}
	
	private void startDownload(int position) {
		 
		String pkgUrl = dataList.get(position).getPkgUrl();
		Uri uri = Uri.parse(dataList.get(position).getPkgUrl());
		int index = pkgUrl.lastIndexOf('/');
		String fileName = pkgUrl.substring(index);
	    LogUtils.e(pkgUrl + ",filename=" + fileName);
	    dataList.get(position).setFileName(fileName);
	    
	    
		
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir();
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) 
        					+ File.separator 
        					+ fileName;
        File file = new File(filePath);
        if(file.exists() && file.isFile()) {
        	file.delete();
        }
	        
	  
       lastDownloadId = dowanloadmanager.enqueue(new DownloadManager.Request(uri)  
	                								.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)  
	                								.setAllowedOverRoaming(false)  
	                								.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
	                								.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName));  
       LogUtils.e("lastDownloadId=" + lastDownloadId);
	        
	    downloadObserver = new DownloadChangeObserver(null);      
	    getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);
	    
	    dataList.get(position).setDownloadId(lastDownloadId);
	    dataList.get(position).setDownloading(true);
	        
	}
	
	private void queryDownloadStatus() {  
		
		for(int i=0;i<dataList.size();i++) {
			if(dataList.get(i).isDownloading()) {
				LogUtils.e("downloading id = " + dataList.get(i).getDownloadId());
				long downloadId = dataList.get(i).getDownloadId();
				
				DownloadManager.Query query = new DownloadManager.Query();
				query.setFilterById(downloadId);
				
				
				Cursor c = dowanloadmanager.query(query);
				if (c != null && c.moveToFirst()) {
					
					int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));		//下载状态
					
					int reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);
					
					int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);				//下载文件名字
					int fileSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);	//下载文件大小
					int bytesDLIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);	//已经下载大小
					
					String title = c.getString(titleIdx);
				//	int fileSize = c.getInt(fileSizeIdx);	//文件总大小
					int bytesDL = c.getInt(bytesDLIdx);		//文件已下载大小

					// Translate the pause reason to friendly text.
					
					
					int pos = 0;
					for(int j=0;j<dataList.size();j++) {
                		if(dataList.get(j).getDownloadId()==downloadId) {
                			dataList.get(j).setDlSize(bytesDL);
                			pos = j;
                		}
                	}

					// Display the status
					int reason = c.getInt(reasonIdx);
					StringBuilder sb = new StringBuilder();
					sb.append(title).append("\n");
					sb.append("Downloaded ").append(bytesDL).append(" / ").append(dataList.get(pos).getFileSize());
					LogUtils.e(sb.toString());
					
					switch (status) {
					case DownloadManager.STATUS_PAUSED:
						LogUtils.e("STATUS_PAUSED");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int)downloadId, (int)dataList.get(pos).getFileSize(), DownloadManager.STATUS_PAUSED));
						
					case DownloadManager.STATUS_PENDING:
						LogUtils.e("STATUS_PENDING");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int)downloadId, (int)dataList.get(pos).getFileSize(), DownloadManager.STATUS_PENDING));
						
					case DownloadManager.STATUS_RUNNING:
						// 正在下载，不做任何事情
						LogUtils.e("STATUS_RUNNING");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int)downloadId, (int)dataList.get(pos).getFileSize(), DownloadManager.STATUS_RUNNING));
						break;
						
					case DownloadManager.STATUS_SUCCESSFUL:
						// 完成
						LogUtils.e("下载完成");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int)downloadId, (int)dataList.get(pos).getFileSize(), DownloadManager.STATUS_SUCCESSFUL));
						
						break;
						
					case DownloadManager.STATUS_FAILED:
						// 清除已下载的内容，重新下载
						LogUtils.e("STATUS_FAILED");
						dowanloadmanager.remove(lastDownloadId);
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int)downloadId, (int)dataList.get(pos).getFileSize(), DownloadManager.STATUS_FAILED));
						break;
					}
					
					
					c.close();
				}  
			}
		}
   	
		
   } 
	
	public static boolean isDownloading(int downloadManagerStatus) {
        return downloadManagerStatus == DownloadManager.STATUS_RUNNING
                || downloadManagerStatus == DownloadManager.STATUS_PAUSED
                || downloadManagerStatus == DownloadManager.STATUS_PENDING;
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if(receiver!=null) {
				unregisterReceiver(receiver);
			}
			if(downloadObserver!=null) {
				getContentResolver().unregisterContentObserver(downloadObserver);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {     
        @Override     
        public void onReceive(Context context, Intent intent) {     
            //这里可以取得下载的id，这样就可以知道哪个文件下载完成了。适用与多个下载任务的监听    
            Log.e("tag", "DownloadManager.EXTRA_DOWNLOAD_ID=" + intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));    
            Log.e("", "downlaod onReceive************************************************************** ");
            queryDownloadStatus();   
            
            
        }     
    };
    
    class DownloadChangeObserver extends ContentObserver {  
    	  
        public DownloadChangeObserver(Handler handler) {  
            super(handler);  
        }  
  
        @Override  
        public void onChange(boolean selfChange) {  
        	Log.e("", "downlaod on change ------------------------------------------------------- ");
            queryDownloadStatus();     
        }  
  
    }

	public void initAppData() {
		
		List<PackageInfo> packages = AppUpgradeActivity.this.getPackageManager().getInstalledPackages(0);
		
		for(int i=0; i < appPkgs.length; i++) {
			String pkgName = appPkgs[i];
			for(int j=0;j<packages.size();j++) {
				PackageInfo packageInfo = packages.get(j);
				if(pkgName.equals(packageInfo.packageName)) {
					LogUtils.e("----------" + packageInfo.packageName);
					LogUtils.e("----------------" + packageInfo.applicationInfo.loadLabel(AppUpgradeActivity.this.getPackageManager()).toString());
					LogUtils.e("----------------" + packageInfo.versionName);
					LogUtils.e("----------------" + packageInfo.versionCode);
					
					UpdateModel model = new UpdateModel();
					model.setAppName(packageInfo.applicationInfo.loadLabel(AppUpgradeActivity.this.getPackageManager()).toString());
					model.setAppIcon(packageInfo.applicationInfo.loadIcon(AppUpgradeActivity.this.getPackageManager()));
					model.setPkgName(packageInfo.packageName);
					model.setVersionName(packageInfo.versionName);
					model.setVersionCode(packageInfo.versionCode);
					
					
					dataList.add(model);
				}
			}
		}
		
		LogUtils.e("dataList.size=" + dataList.size());

	}
	
    
}
