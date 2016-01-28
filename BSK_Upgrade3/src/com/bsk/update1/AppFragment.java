package com.bsk.update1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

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
import android.renderscript.RenderScript.RSErrorHandler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.bsk.update1.R;
import com.bsk.update.adapter.AppUpdateListAdapter;
import com.bsk.update.model.AppInfo;
import com.bsk.update.model.AppInfoListResp;
import com.bsk.update.model.UpdateModel;
import com.bsk.update.util.FileUtils;
import com.bsk.update.util.HttpUtils;
import com.bsk.update.util.LogUtils;
import com.bsk.update.util.MD5FileUtil;
import com.google.gson.Gson;

public class AppFragment extends Fragment {

	private View rootView;

	private Button checkAllBtn;
	private Button updateAllBtn;

	private ListView mListView;
	// 一个应用的升级信息
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

	private String email;
	private String password;

	private Resources res;
	private Context mContext;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mContext = AppFragment.this.getActivity();
		res = this.getResources();

		email = "aaaad"; // !
		password = "test"; // !

		// 配置需要升级的应用包名，appPkgs是String[]类型。
		// 云相册应用，音乐应用，日历应用，升级应用
		appPkgs = new String[] { Constants.APP_BSK_GALLERY, Constants.APP_BSK_MUSIC, Constants.APP_BSK_CALENDAR,
				Constants.APP_BSK_UPDATE };
		// List<UpdateModel>类型，一个应用的升级信息
		if (dataList == null) {
			dataList = new ArrayList<UpdateModel>();
			LogUtils.e("oncreate datalist.size=" + dataList.size());
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// 加载视图
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.fragment_app, null);
		}

		// 已存在的话，先删掉
		ViewGroup parent = (ViewGroup) rootView.getParent();
		if (parent != null) {
			parent.removeView(rootView);
		}

		checkAllBtn = (Button) rootView.findViewById(R.id.check_all_btn);
		updateAllBtn = (Button) rootView.findViewById(R.id.update_all_btn);
		mListView = (ListView) rootView.findViewById(R.id.app_list);

		// 返回json数据，获取ret的值，如果为O，就传Scode的值
		final Runnable getSCodeRunnable = new Runnable() {
			@Override
			public void run() {
				urlPath = Constants.HOST + Constants.API_ACTIVATE + "?email=" + email + "&password=" + password
						+ "&client_type=device" + "&login_type=bind_device" + "&lang=en";

				LogUtils.e("path= " + urlPath);

				String jsonString = HttpUtils.doGet(urlPath);
				LogUtils.e("jsonString= " + jsonString);
				if (!TextUtils.isEmpty(jsonString)) {
					try {
						dataResponse = new JSONObject(jsonString);
						String result = dataResponse.getString("ret");
						if (!TextUtils.isEmpty(result)) {
							if (result.equals("0")) {
								String scode = dataResponse.getString("scode");
								SharedPreferences settings = mContext.getSharedPreferences(Constants.UPDATE_PREFS, 0);
								SharedPreferences.Editor editor = settings.edit();
								editor.putString("Scode", scode);
								editor.commit();
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

			}
		};

		// 返回json数据，获取ret的值，如果非O，就传msg的值
		final Runnable getAppInfoRunnable = new Runnable() {
			@Override
			public void run() {

				urlPath = Constants.HOST + Constants.API_APP_UPGRADE + "?scode=" + scode;
				// + "&app1=update.zip";

				// dataList.size()为4，遍历增加path，注意，这里是“urlPath +”，有个+号（就是不同的包名）
				for (int i = 0; i < dataList.size(); i++) {
					urlPath += "&app" + (i + 1) + "=" + dataList.get(i).getPkgName() + ",,";
				}
				
				LogUtils.e("path= " + urlPath);

				//用mHandler对象来new消息，那就是会执行mHandler对象的线程
				Message msg = mHandler.obtainMessage();

				String jsonString = HttpUtils.doGet(urlPath);
				LogUtils.e("app info jsonString= " + jsonString);
				if (!TextUtils.isEmpty(jsonString)) {
					try {
						Gson gson = new Gson();
						appInfoListResp = gson.fromJson(jsonString, AppInfoListResp.class);

						int result = appInfoListResp.getRet();
						LogUtils.e("result=" + result);
						//获取ret的值
						msg.what = result;
						Bundle bundle = new Bundle();

						if (result != 0) {
							String msgStr = appInfoListResp.getMsg();
							bundle.putString("msg", msgStr);
						}

						msg.setData(bundle);
						//发送并传msg的值
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
				//就是获取msg的值
				bundle = msg.getData();

				if (msg.what == 0) { // 获取AppInfo成功
					//getApps()=return apps,返回的是APP信息列表；appInfoListResp：AppInfoListResp类，就是ret，msg，apps
					List<AppInfo> appInfos = appInfoListResp.getApps();
					//遍历APP列表
					if (appInfos != null && appInfos.size() > 0) {			
						for (int i = 0; i < appInfos.size(); i++) {
							AppInfo appInfo = appInfos.get(i);

							//dataList:List<UpdateModel>类，就是很详细的应用信息
							for (int j = 0; j < dataList.size(); j++) {
								if (appInfo.getPkgname().equals(dataList.get(j).getPkgName())) {
									
									dataList.get(j).setDownloading(false); // 未下载标志
									dataList.get(j).setUpgraded(false); // 未升级标志
									//从服务器获取每一个应用的信息并显示
									dataList.get(j).setPkgUrl(appInfo.getPkgurl());
									dataList.get(j).setNewCode(Integer.parseInt(appInfo.getPkgver()));
									dataList.get(j).setAppMd5(appInfo.getPkgmd5());
									dataList.get(j).setVersionNameNew(appInfo.getRemarks()); // remarks作为新版本号

									dataList.get(j).setFileSize(Long.parseLong(appInfo.getPkgsize()));

									if (!dataList.get(j).isDownloading()) { // 未下载中的应用更新状态

										String updateTip = "";
										if (dataList.get(j).getNewCode() > dataList.get(j).getVersionCode()) {
											updateTip = "  " + res.getString(R.string.download_check_new) + " "
													+ dataList.get(j).getVersionNameNew();
											// dataList.get(j).setDownloading(true);
										} else {
											updateTip = res.getString(R.string.menu_has_not_newnewsion);
											dataList.get(j).setDownloading(false);
										}
										dataList.get(j).setUpdateTip(updateTip);
									}

									// 如果版本一样，从dataList中删除
									if (dataList.get(j).getNewCode() <= dataList.get(j).getVersionCode()) {
										dataList.remove(j);
										j--;
									}

								}
							}
						}

						//如果要更新的APK大于1个
						if (dataList.size() > 0) {
							updateAllBtn.setTextColor(res.getColor(R.color.black));
							updateAllBtn.setEnabled(true);
						} else {
							updateAllBtn.setTextColor(res.getColor(R.color.black));
							updateAllBtn.setEnabled(false);
							Toast.makeText(mContext, res.getString(R.string.all_app_is_new), Toast.LENGTH_SHORT).show();
						}

						//刷新
						adapter.notifyDataSetChanged();
						mListView.setVisibility(View.VISIBLE);
					}

				} else if (msg.what == 2) {
					int position = bundle.getInt("pos");

					LogUtils.e("2---download star,pos=" + position);

					if (!dataList.get(position).isDownloading()) { // 未下载中应用开始下载
						startDownload(position);
					}

				} else if (msg.what == 3) {
					/*
					 * int position = bundle.getInt("pos"); LogUtils.e(
					 * "2---download star,pos=" + position);
					 * if(!dataList.get(position).isDownloading()) {
					 * //未下载中应用开始下载 startDownload(position); }
					 */
					Toast.makeText(mContext, res.getString(R.string.update_new_version_finished), Toast.LENGTH_LONG)
							.show();
				} else {
					scode = "";
					SharedPreferences settings = mContext.getSharedPreferences(Constants.UPDATE_PREFS, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("Scode", scode);
					editor.commit();
					String msgStr = bundle.getString("msg");
					Toast.makeText(mContext, msgStr, Toast.LENGTH_LONG).show();

					dataList.clear();
					adapter.notifyDataSetChanged();
				}

			}
		};

		dlHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				switch (msg.what) {
				case 0:
					int status = (Integer) msg.obj;

					if (isDownloading(status)) { // 下载状态显示进度条，隐藏下载按钮

						// downloadProgress.setVisibility(View.VISIBLE);
						// downloadProgress.setMax(0);
						// downloadProgress.setProgress(0);
						// downloadButton.setVisibility(View.GONE);
						// downloadSize.setVisibility(View.VISIBLE);
						// downloadPrecent.setVisibility(View.VISIBLE);
						// downloadCancel.setVisibility(View.VISIBLE);
						// downloadTip.setText(res.getString(R.string.download_tip));

						LogUtils.e("notify!! arg2=" + msg.arg2);
						adapter.notifyDataSetChanged();

						if (msg.arg2 < 0) { // 文件总大小<0
							// downloadProgress.setIndeterminate(true);
							// downloadPrecent.setText("0%");
							// downloadSize.setText("0M/0M");

						} else { // 下载中 ，显示大小，
							// downloadProgress.setIndeterminate(false);
							// downloadProgress.setMax(msg.arg2);
							// downloadProgress.setProgress(msg.arg1);
							// downloadPrecent.setText(FileUtils.getNotiPercent(msg.arg1,
							// msg.arg2)); //设置百分比
							// downloadSize.setText(FileUtils.getAppSize(msg.arg1)
							// + "/" + FileUtils.getAppSize(msg.arg2));
							// //设置文件大小比。
							// downloadTip.setText(res.getString(R.string.downloading));

							// int downloadId = msg.arg1;

						}

					} else { // 不是下载中状态，就隐藏进度条等，显示下载按钮并改变文字

						// 清除下载状态
						LogUtils.e("msg.arg1(dId)=" + msg.arg1);
						/*
						 * int pos = 0; for(int i=0;i<dataList.size();i++) {
						 * if(dataList.get(i).getDownloadId() == msg.arg1) {
						 * dataList.get(i).setDownloadId(0);
						 * dataList.get(i).setDownloading(false); pos = i; } }
						 */

						if (status == DownloadManager.STATUS_FAILED) {
							LogUtils.e("下载失败");
							// downloadButton.setText("下载失败");
							// downloadTip.setText(res.getString(R.string.download_fail));

						} else if (status == DownloadManager.STATUS_SUCCESSFUL) {
							LogUtils.e("下载成功!");
							// downloadButton.setText("下载成功");
							// downloadTip.setText(res.getString(R.string.download_success));
							// upgradeBtn.setEnabled(true);

						} else {
							// downloadButton.setText("下载");
							// downloadTip.setText(res.getString(R.string.download_tip));
						}

					}
					break;
				}
			}
		};

		dowanloadmanager = (DownloadManager) mContext.getSystemService(android.content.Context.DOWNLOAD_SERVICE);
		mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		//检查更新按钮
		checkAllBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				SharedPreferences settings = mContext.getSharedPreferences(Constants.UPDATE_PREFS, 0);
				scode = settings.getString("Scode", "");
				LogUtils.e("SharedPreferences scode=" + scode);
				if (!TextUtils.isEmpty(scode)) {

					dataList.clear();

					mListView.setVisibility(View.INVISIBLE);
					adapter = new AppUpdateListAdapter(mContext, dataList, mHandler);
					mListView.setAdapter(adapter);

					initAppData();

					new Thread(getAppInfoRunnable).start();
				} else {
					new Thread(getSCodeRunnable).start();
				}

			}
		});
		
		//全部更新按钮
		updateAllBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//dataList:List<UpdateModel>类，就是很详细的应用信息
				for (int i = 0; i < dataList.size(); i++) {
					if (!dataList.get(i).isDownloading()) { // 未下载中应用开始下载
						startDownload(i);
					}
				}
			}
		});

		return rootView;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		try {
			if (receiver != null) {
				mContext.unregisterReceiver(receiver);
			}
			if (downloadObserver != null) {
				mContext.getContentResolver().unregisterContentObserver(downloadObserver);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static boolean isDownloading(int downloadManagerStatus) {
		return downloadManagerStatus == DownloadManager.STATUS_RUNNING
				|| downloadManagerStatus == DownloadManager.STATUS_PAUSED
				|| downloadManagerStatus == DownloadManager.STATUS_PENDING;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// 这里可以取得下载的id，这样就可以知道哪个文件下载完成了。适用与多个下载任务的监听
			long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
			Log.e("", "downlaod onReceive************************************************************** ");
			Log.e("tag", "==============finish==================DownloadManager.EXTRA_DOWNLOAD_ID=" + downloadId);
			queryDownloadStatus();

			for (int i = 0; i < dataList.size(); i++) {
				LogUtils.e("data downloadId=" + dataList.get(i).getDownloadId() + ",downId=" + downloadId);

				if (dataList.get(i).getDownloadId() == downloadId) {
					String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
							+ dataList.get(i).getFileName();
					LogUtils.e("########################################install filename=" + filePath);

					// 验证MD5并安装
					LogUtils.e("md5=" + dataList.get(i).getAppMd5());

					if (!dataList.get(i).isUpgraded()) {

						File file = new File(filePath);
						try {
							String fileMd5 = MD5FileUtil.getFileMD5String(file);
							LogUtils.e("file md5=" + fileMd5);
							if (fileMd5.equals(dataList.get(i).getAppMd5())) {
								Toast.makeText(mContext, "\"" + dataList.get(i).getAppName() + "\""
										+ res.getString(R.string.app_updating), Toast.LENGTH_SHORT).show();
								// silence install
								String upgradeResult = FileUtils.silenceInstall(filePath);
								LogUtils.e("upgradeResult=" + upgradeResult);

								if (upgradeResult.indexOf("Success") >= 0) {
									Toast.makeText(mContext,
											"\"" + dataList.get(i).getAppName() + "\""
													+ res.getString(R.string.app_update_success),
											Toast.LENGTH_SHORT).show();
									dataList.get(i).setVersionName(dataList.get(i).getVersionNameNew());
									adapter.notifyDataSetChanged();
									dataList.get(i).setUpgraded(true);
								} else {
									Toast.makeText(mContext,
											"\"" + dataList.get(i).getAppName() + "\""
													+ res.getString(R.string.app_update_fail),
											Toast.LENGTH_SHORT).show();
								}

							} else {
								Toast.makeText(mContext, res.getString(R.string.file_validate_fail), Toast.LENGTH_SHORT)
										.show();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}

					}

				}
			}

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

		List<PackageInfo> packages = mContext.getPackageManager().getInstalledPackages(0);

		for (int i = 0; i < appPkgs.length; i++) {
			String pkgName = appPkgs[i];
			for (int j = 0; j < packages.size(); j++) {
				PackageInfo packageInfo = packages.get(j);
				if (pkgName.equals(packageInfo.packageName)) {
					LogUtils.e("----------" + packageInfo.packageName);
					LogUtils.e("----------------"
							+ packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
					LogUtils.e("----------------" + packageInfo.versionName);
					LogUtils.e("----------------" + packageInfo.versionCode);

					UpdateModel model = new UpdateModel();
					model.setAppName(packageInfo.applicationInfo.loadLabel(mContext.getPackageManager()).toString());
					model.setAppIcon(packageInfo.applicationInfo.loadIcon(mContext.getPackageManager()));
					model.setPkgName(packageInfo.packageName);
					model.setVersionName("V" + packageInfo.versionName);
					model.setVersionCode(packageInfo.versionCode);

					dataList.add(model);
				}
			}
		}

		LogUtils.e("dataList.size=" + dataList.size());

	}

	private void startDownload(int position) {
		LogUtils.e("position=" + position);
		LogUtils.e("dataList.size=" + dataList.size());

		String pkgUrl = dataList.get(position).getPkgUrl();
		Uri uri = Uri.parse(dataList.get(position).getPkgUrl());
		int index = pkgUrl.lastIndexOf('/');
		String fileName = pkgUrl.substring(index);
		LogUtils.e(pkgUrl + ",filename=" + fileName);
		dataList.get(position).setFileName(fileName);

		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir();
		String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
				+ File.separator + fileName;
		File file = new File(filePath);
		if (file.exists() && file.isFile()) {
			file.delete();
		}

		lastDownloadId = dowanloadmanager
				.enqueue(new DownloadManager.Request(uri)
						.setAllowedNetworkTypes(
								DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)
						.setAllowedOverRoaming(false)
						.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
						.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName));
		LogUtils.e("lastDownloadId=" + lastDownloadId);

		downloadObserver = new DownloadChangeObserver(null);
		mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);

		dataList.get(position).setDownloadId(lastDownloadId);
		dataList.get(position).setDownloading(true); // 下载中标志

	}

	private void queryDownloadStatus() {

		for (int i = 0; i < dataList.size(); i++) {
			if (dataList.get(i).isDownloading()) {
				LogUtils.e("downloading id = " + dataList.get(i).getDownloadId());
				long downloadId = dataList.get(i).getDownloadId();

				DownloadManager.Query query = new DownloadManager.Query();
				query.setFilterById(downloadId);

				Cursor c = dowanloadmanager.query(query);
				if (c != null && c.moveToFirst()) {

					int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)); // 下载状态

					int reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);

					int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE); // 下载文件名字
					int fileSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES); // 下载文件大小
					int bytesDLIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR); // 已经下载大小

					String title = c.getString(titleIdx);
					// int fileSize = c.getInt(fileSizeIdx); //文件总大小
					int bytesDL = c.getInt(bytesDLIdx); // 文件已下载大小

					// Translate the pause reason to friendly text.

					int pos = 0;
					for (int j = 0; j < dataList.size(); j++) {
						if (dataList.get(j).getDownloadId() == downloadId) {
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
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int) downloadId,
								(int) dataList.get(pos).getFileSize(), DownloadManager.STATUS_PAUSED));

					case DownloadManager.STATUS_PENDING:
						LogUtils.e("STATUS_PENDING");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int) downloadId,
								(int) dataList.get(pos).getFileSize(), DownloadManager.STATUS_PENDING));

					case DownloadManager.STATUS_RUNNING:
						// 正在下载，不做任何事情
						LogUtils.e("STATUS_RUNNING");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int) downloadId,
								(int) dataList.get(pos).getFileSize(), DownloadManager.STATUS_RUNNING));
						break;

					case DownloadManager.STATUS_SUCCESSFUL:
						// 完成
						LogUtils.e("下载完成");
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int) downloadId,
								(int) dataList.get(pos).getFileSize(), DownloadManager.STATUS_SUCCESSFUL));

						break;

					case DownloadManager.STATUS_FAILED:
						// 清除已下载的内容，重新下载
						LogUtils.e("STATUS_FAILED");
						dowanloadmanager.remove(lastDownloadId);
						dlHandler.sendMessage(dlHandler.obtainMessage(0, (int) downloadId,
								(int) dataList.get(pos).getFileSize(), DownloadManager.STATUS_FAILED));
						break;
					}

					c.close();
				}
			}
		}
	}

}
