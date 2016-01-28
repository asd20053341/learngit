package com.bsk.update1;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bsk.update1.R;
import com.bsk.update.model.AppInfo;
import com.bsk.update.model.AppInfoListResp;
import com.bsk.update.util.CommonUtils;
import com.bsk.update.util.FileUtils;
import com.bsk.update.util.HttpUtils;
import com.bsk.update.util.LogUtils;
import com.bsk.update.util.MD5FileUtil;
import com.bsk.update.view.InstallPackage;
import com.google.gson.Gson;

public class OtaFragment extends Fragment {

	private View rootView;

	private Button localBtn;
	private Button checkBtn;

	private TextView versionTv;
	private TextView tipTv;
	private TextView titleTv;
	private TextView descTv;
	private TextView downloadPrecent;

	private ProgressBar downloadProgress;

	private String email;
	private String password;

	private String urlPath;
	private Handler mHandler;
	private JSONObject dataResponse;

	private String scode;
	private AppInfoListResp appInfoListResp;

	private boolean isSCode;

	private AppInfo appInfo; // update.zip包
	private DownloadManager dowanloadmanager = null;
	private DownloadChangeObserver downloadObserver;
	private long lastDownloadId;

	private Handler dlHandler;
	public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");

	private String firmVersion; // 固件版本
	private String filePath; // update.zip文件路径

	private boolean isDownload; // 是否可下载
	private boolean isDownloading; // 是否下载中

	private Resources res;
	private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = OtaFragment.this.getActivity();
		res = this.getResources();

		// 是否可下载
		isDownload = false;
		// 获取固件版本，这里要引入第三方jar包:layoutlib.jar
		firmVersion = SystemProperties.get("ro.product.firmware", "");

		// 获取update.zip这个文件的路径
		// Constants.SD_PATH为SD卡路径
		// Environment.DIRECTORY_DOWNLOADS:下载的标准目录,其实就是一个文件夹：Download
		// Constants.APP_UPDATE_ZIP = "update.zip"
		filePath = Constants.SD_PATH + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + Constants.APP_UPDATE_ZIP;
		Log.v("aaa", Environment.DIRECTORY_DOWNLOADS);
		// log输出：固件版本
		Log.v("aaa", "firm version=" + firmVersion);

		// String 类型
		email = "aaaad";
		// String 类型
		password = "test";

		// Constants.UPDATE_PREFS = "update_prefs"
		// name为本组件的配置文件名( 自己定义，也就是一个文件名)，当这个文件不存在时，直接创建，如果已经存在，则直接使用，
		// mode为操作模式，默认的模式为0或MODE_PRIVATE，还可以使用MODE_WORLD_READABLE和MODE_WORLD_WRITEABLE
		// mode指定为MODE_PRIVATE，则该配置文件只能被自己的应用程序访问。
		// mode指定为MODE_WORLD_READABLE，则该配置文件除了自己访问外还可以被其它应该程序读取。
		// mode指定为MODE_WORLD_WRITEABLE，则该配置文件除了自己访问外还可以被其它应该程序读取和写入
		SharedPreferences settings = mContext.getSharedPreferences(Constants.UPDATE_PREFS, 0);
		// String 类型
		scode = settings.getString("Scode", "");
		// scode为空，没值，不知道有什么样用
		Log.v("aaa", "SharedPreferences Scode=" + scode);
	}

	// 加载视图，获取服务器信息，返回json数据
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// 一开始确实为空，就把fragment_ota这个布局给它
		if (rootView == null) {
			rootView = inflater.inflate(R.layout.fragment_ota, null);
		}

		// addView后，你要添加的view就一直存在添加view的容器中，不removeView不会有什么情况，
		// 是不过在你要想在addView上次的view之前必须先把removeView
		// 掉，否则会提示你view已存在异常
		ViewGroup parent = (ViewGroup) rootView.getParent();
		if (parent != null) {
			parent.removeView(rootView);
		}

		// localBtn：本地升级按钮
		localBtn = (Button) rootView.findViewById(R.id.local_btn);
		// checkBtn：在线检测按钮
		checkBtn = (Button) rootView.findViewById(R.id.check_btn);
		// 版本：V2.1.0.01-D150923
		versionTv = (TextView) rootView.findViewById(R.id.version_tv);

		// tipTv可视为gone
		tipTv = (TextView) rootView.findViewById(R.id.tip_tv);
		// titleTv="版本描述:"
		titleTv = (TextView) rootView.findViewById(R.id.version_title);
		// "版本描述:"下的一段文字
		descTv = (TextView) rootView.findViewById(R.id.version_desc);

		// downloadPrecent可视为gone
		downloadPrecent = (TextView) rootView.findViewById(R.id.download_precent);
		// downloadSize = (TextView)findViewById(R.id.download_size);

		// downloadProgress=进度条
		downloadProgress = (ProgressBar) rootView.findViewById(R.id.download_progress);

		// 设置TextView为可以上下滚动
		descTv.setMovementMethod(new ScrollingMovementMethod());
		String verion = Build.VERSION.RELEASE;
		// versionTv.setText(res.getString(R.string.current_version) + " " +
		// firmVersion);

		// 设置固件版本TextView
		versionTv.setText(firmVersion);

		// 返回json，通过0或1传数据,在线检测升级时调用
		final Runnable getSCodeRunnable = new Runnable() {

			@Override
			public void run() {
				isSCode = true;
				// HOST = "http://120.24.231.164";
				// API_ACTIVATE = "/api/dev/login/login1";
				// email = "aaaad";
				// password = "test";
				urlPath = Constants.HOST + Constants.API_ACTIVATE + "?email=" + email + "&password=" + password
						+ "&client_type=device" + "&login_type=bind_device" + "&lang=en";

				LogUtils.e("path= " + urlPath);
				// msg={ when=-2h42m29s237ms what=0
				// target=com.bsk.update.OtaFragment$6 }
				Message msg = mHandler.obtainMessage();

				// 调用自己定义的HttpUtils.doGet方法，并把urlPath传入，最后从服务器获取数据
				String jsonString = HttpUtils.doGet(urlPath);
				LogUtils.e("jsonString= " + jsonString);
				// 返回的数据如果不为空
				if (!TextUtils.isEmpty(jsonString)) {
					try {
						// 返回的是json数据，开始解析json
						// 比如：jsonString= {"ret":"1","msg":"访问服务器错误!"}
						dataResponse = new JSONObject(jsonString);
						String result = dataResponse.getString("ret");
						msg.what = Integer.parseInt(result);
						Bundle bundle = new Bundle();
						// result不为空就进入
						if (!TextUtils.isEmpty(result)) {
							// 估计是根据返回的json数据的值不同，来进行判断put什么值进去给Bundle
							if (result.equals("0")) {
								String scode = dataResponse.getString("scode");
								bundle.putString("scode", scode);
							} else {
								String msgStr = dataResponse.getString("msg");
								bundle.putString("msg", msgStr);
							}
						}

						msg.setData(bundle);
						// 发送
						msg.sendToTarget();

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

			}
		};

		// 返回json，通过不等于0传数据，在线检测升级时调用
		final Runnable getAppInfoRunnable = new Runnable() {
			@Override
			public void run() {
				isSCode = false;
				// HOST = "http://120.24.231.164"
				// API_APP_UPGRADE = "/api/dev/heartbeat/hb"
				// scode = settings.getString("Scode", "");
				// firmVersion = SystemProperties.get("ro.product.firmware",
				// "");
				urlPath = Constants.HOST + Constants.API_APP_UPGRADE + "?scode=" + scode + "&app1=update.zip,"
						+ firmVersion + ","; // firmVersion匹配reqver

				LogUtils.e("path= " + urlPath);

				Message msg = mHandler.obtainMessage();

				String jsonString = HttpUtils.doGet(urlPath);
				LogUtils.e("app info jsonString= " + jsonString);
				if (!TextUtils.isEmpty(jsonString)) {
					try {

						Gson gson = new Gson();
						// 从AppInfoListResp类里解析jsonString字符串
						appInfoListResp = gson.fromJson(jsonString, AppInfoListResp.class);
						// getRet()=return ret;
						int result = appInfoListResp.getRet();
						LogUtils.e("result=" + result);

						msg.what = result;
						Bundle bundle = new Bundle();
						// 获取的ret如果不等于0
						if (result != 0) {
							// getMsg=return msg;
							String msgStr = appInfoListResp.getMsg();
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

		// "本地升级"按钮，跳转到FileSelector类，传"/mnt"
		localBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// Toast.makeText(mContext, "Local!",
				// Toast.LENGTH_SHORT).show();

				Intent intent = new Intent(mContext, FileSelector.class);
				// 传"/mnt"值，通过FileSelector.ROOT=root
				intent.putExtra(FileSelector.ROOT, "/mnt");
				// 标识码为0
				startActivityForResult(intent, 0);

			}
		});

		checkBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// isNetwordAvailable=true
				boolean isNetwordAvailable = CommonUtils.getNetWorkStatus(mContext);
				if (isNetwordAvailable) {

					// isDownload是是否可下载，开始时isDownload为false
					if (isDownload) {
						// 如果update.zip包不为空
						if (appInfo != null) {
							// update.zip包路径
							File file = new File(filePath);
							// 如果update.zip包已存在的话，先把旧的删除
							if (file != null && file.exists()) {
								// 把update.zip包删除
								file.delete();
							}

							// 那个提示的TextView，设置隐藏
							tipTv.setVisibility(View.INVISIBLE);
							// 一个TextView，text为：100%
							downloadPrecent.setVisibility(View.VISIBLE);
							// 进度条
							downloadProgress.setVisibility(View.VISIBLE);
							// 设置在线检测按钮
							checkBtn.setText(res.getString(R.string.is_downloading));
							// 设置不可点击
							checkBtn.setEnabled(false);
							startDownload();
						}
					} else {
						if (!TextUtils.isEmpty(scode)) { // 如果有保存scode,访问服务器获取升级信息
							// 如果不为空就执行getAppInfoRunnable，通过不等于，传msg
							new Thread(getAppInfoRunnable).start();
						} else { // 如果为空，就通过0传scode或1传msg
							new Thread(getSCodeRunnable).start();
						}

					}
				} else {
					// 提示网络无效
					Toast.makeText(mContext, res.getString(R.string.network_invalid), Toast.LENGTH_LONG).show();
				}

			}
		});

		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				LogUtils.e("msg.what= " + msg.what);
				Bundle bundle = null;
				bundle = msg.getData();

				if (msg.what == 0) {

					if (isSCode) { // 获取SCode成功
						scode = bundle.getString("scode");
						LogUtils.e("scode=" + scode);

						// 保存scode
						SharedPreferences settings = mContext.getSharedPreferences(Constants.UPDATE_PREFS, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putString("Scode", scode);
						editor.commit();

						new Thread(getAppInfoRunnable).start();

					} else { // 获取AppInfo成功

						List<AppInfo> appInfos = appInfoListResp.getApps();
						if (appInfos != null && appInfos.size() > 0) {
							for (int i = 0; i < appInfos.size(); i++) {
								if (appInfos.get(i).getPkgname().equals(Constants.APP_UPDATE_ZIP)) { // 找到pkgname=update.zip
									appInfo = appInfos.get(i);

									isDownload = true;

									// checkBtn.setText(res.getString(R.string.update));
									tipTv.setVisibility(View.VISIBLE);
									String fileSize = FileUtils.getAppSize(Long.parseLong(appInfo.getPkgsize()))
											.toString();
									tipTv.setText("检测到新版本 " + appInfo.getPkgver() + " (" + fileSize + ")");
									titleTv.setVisibility(View.VISIBLE);
									descTv.setVisibility(View.VISIBLE);
									descTv.setText(appInfo.getRemarks());
								}
							}
						}

						if (appInfo == null) {
							tipTv.setVisibility(View.GONE);
							// tipTv.setText("已是最新版本" );
							Toast.makeText(mContext, "已是最新版本", Toast.LENGTH_SHORT).show();
						}
					}

				} else {
					scode = "";
					SharedPreferences settings = mContext.getSharedPreferences(Constants.UPDATE_PREFS, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("Scode", scode);
					editor.commit();
					String msgStr = bundle.getString("msg");
					Toast.makeText(mContext, msgStr, Toast.LENGTH_LONG).show();
				}

			}
		};

		dlHandler = new Handler() {
			public void handleMessage(Message msg) {
				super.handleMessage(msg);

				switch (msg.what) {
				case 0:
					// 得到下载状态，一组数据
					// dlHandler.obtainMessage(0, bytesDL, fileSize,
					// DownloadManager.STATUS_PAUSED)
					int status = (Integer) msg.obj;

					if (isDownloading(status)) { // 下载状态显示进度条，隐藏下载按钮

						// downloadProgress.setVisibility(View.VISIBLE);
						downloadProgress.setMax(0);
						downloadProgress.setProgress(0);
						// downloadButton.setVisibility(View.GONE);
						// downloadSize.setVisibility(View.VISIBLE);
						// downloadPrecent.setVisibility(View.VISIBLE);
						// downloadCancel.setVisibility(View.VISIBLE);
						// downloadTip.setText(res.getString(R.string.download_tip));

						if (msg.arg2 < 0) { // 文件总大小<0
							// 明确(false)就是滚动条的当前值自动在最小到最大值之间来回移动，形成这样一个动画效果，
							// 这个只是告诉别人“我正在工作”，但不能提示工作进度到哪个阶段。
							// 主要是在进行一些无法确定操作时间的任务时作为提示。
							// 而“明确”(true)就是根据你的进度可以设置现在的进度值。
							downloadProgress.setIndeterminate(true);
							downloadPrecent.setText("0M/0M (0%)");
							// downloadSize.setText("0M/0M");

						} else { // 下载中 ，显示大小，
							// 不确认大小，来回滚动
							downloadProgress.setIndeterminate(false);
							// 设置进度条最大值为：文件总大小
							downloadProgress.setMax(msg.arg2);
							// 设置进度条当前进度为：当前下载的文件大小
							downloadProgress.setProgress(msg.arg1);
							downloadPrecent
									.setText(FileUtils.getAppSize(msg.arg1) + "/" + FileUtils.getAppSize(msg.arg2)
											+ " (" + FileUtils.getNotiPercent(msg.arg1, msg.arg2) + ")"); // 设置百分比
							// downloadSize.setText(); //设置文件大小比。
							// downloadTip.setText(res.getString(R.string.downloading));
						}

					} else { // 不是下载中状态，就隐藏进度条等，显示下载按钮并改变文字

						// downloadProgress.setVisibility(View.GONE);
						// downloadProgress.setMax(0);
						// downloadProgress.setProgress(0);
						// downloadButton.setVisibility(View.VISIBLE);
						// downloadSize.setVisibility(View.GONE);
						// downloadPrecent.setVisibility(View.GONE);
						// downloadCancel.setVisibility(View.GONE);

						if (status == DownloadManager.STATUS_FAILED) {
							// downloadButton.setText("下载失败");
							// downloadTip.setText(res.getString(R.string.download_fail));
							downloadPrecent.setText(res.getString(R.string.download_fail));

						} else if (status == DownloadManager.STATUS_SUCCESSFUL) {
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

		return rootView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v("aaa", " onActivityResult");
		// data=Intent { (has extras) }
		if (data != null) {
			// bundle=Bundle[{file=/mnt/sdcard/astar_y3-ota-20150923.zip}]
			Bundle bundle = data.getExtras();
			// file=/mnt/sdcard/astar_y3-ota-20150923.zip
			String file = bundle.getString("file");
			if (file != null) {
				// 打开升级对话框
				// file=/mnt/sdcard/astar_y3-ota-20150923.zip
				showUpgradeDialog(file);
			}
		}
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

	// 升级对话框，filePath=/mnt/sdcard/astar_y3-ota-20150923.zip
	public void showUpgradeDialog(String filePath) {
		// new一个对话框，dlg=android.app.Dialog@41c26a90
		final Dialog dlg = new Dialog(mContext, android.R.style.Theme_Holo_Light_Dialog);
		// 设置标题,配置了中文跟英文，具体是怎么判断获取哪个，还不知道
		dlg.setTitle(R.string.confirm_update);
		// 初始化布局对象
		LayoutInflater inflater = LayoutInflater.from(mContext);
		// 加载布局，即要弹出的对话框，并给予自己定义的InstallPackage类对象
		// InstallPackage为自己定义的类
		InstallPackage dlgView = (InstallPackage) inflater.inflate(R.layout.install_ota, null, false);
		// 设置文件路径给InstallPackage类里面的setPackagePath方法
		dlgView.setPackagePath(filePath);
		// 设置布局
		dlg.setContentView(dlgView);
		// 点击了cannel按钮事件
		dlg.findViewById(R.id.confirm_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 把对话框关闭
				dlg.dismiss();
			}
		});
		// 获取到这个对话框，这是长宽，再设置回去
		Window dialogWindow = dlg.getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		// 因为布局文件里的进度条限制了宽度，所以这里怎么改，都没变化
		lp.width = 770; // 宽度
		lp.height = 535;
		// lp.x=200;
		// lp.y=150;
		dialogWindow.setAttributes(lp);
		// 触摸其他领域，不让对话框消失
		dlg.setCanceledOnTouchOutside(false);
		dlg.show();
	}

	public void startDownload() {

		// 获取下载服务
		dowanloadmanager = (DownloadManager) mContext.getSystemService(android.content.Context.DOWNLOAD_SERVICE);
		
		// Uri uri = Uri.parse("http://commonsware.com/misc/test.mp4");
		// 获取update.zip包的路径
		Uri uri = Uri.parse(appInfo.getPkgurl());
		
		// 在SD卡上放一个名为："Download"的目录
		Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir();

		Log.e("", "path1=" + Environment.DIRECTORY_DOWNLOADS);
		Log.e("", "path2=" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

		// setAllowedOverRoaming(false)移动网络下不能下载
		// setNotificationVisibility显示下载通知
		// setDestinationInExternalPublicDir把Constants.APP_UPDATE_ZIP文件给予Environment.DIRECTORY_DOWNLOADS
		// 最后返回一个下载id给lastDownloadId
		lastDownloadId = dowanloadmanager
				.enqueue(new DownloadManager.Request(uri)
						.setAllowedNetworkTypes(
								DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI)
						.setAllowedOverRoaming(false)
						.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
						.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, Constants.APP_UPDATE_ZIP));

		// 注册一个下载完成的广播
		mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

		// 下载变化观察者,监控数据库，即获取下载进度，自己定义的，继承ContentObserver
		downloadObserver = new DownloadChangeObserver(null);

		// 注册DownloadChangeObserver
		// CONTENT_URI="content://downloads/my_downloads"
		mContext.getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);

	}

	private void queryDownloadStatus() {

		DownloadManager.Query query = new DownloadManager.Query();
		// 获取下载ID
		query.setFilterById(lastDownloadId);

		// 查询对应的下载ID的数据
		Cursor c = dowanloadmanager.query(query);
		if (c != null && c.moveToFirst()) {

			int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)); // 下载状态

			int reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);// 下载暂停的原因

			int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE); // 下载文件名字
			int fileSizeIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES); // 下载文件大小
			int bytesDLIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR); // 已经下载大小

			String title = c.getString(titleIdx);
			// int fileSize = c.getInt(fileSizeIdx); //文件总大小
			int fileSize = Integer.parseInt(appInfo.getPkgsize());
			int bytesDL = c.getInt(bytesDLIdx); // 文件已下载大小

			// //下载暂停的原因
			int reason = c.getInt(reasonIdx);
			StringBuilder sb = new StringBuilder();
			sb.append(title).append("\n");
			sb.append("Downloaded ").append(bytesDL).append(" / ").append(fileSize);

			// Display the status
			LogUtils.e(sb.toString());

			switch (status) {
			// 状态为暂停的话
			case DownloadManager.STATUS_PAUSED:
				LogUtils.e("STATUS_PAUSED");
				dlHandler.sendMessage(dlHandler.obtainMessage(0, bytesDL, fileSize, DownloadManager.STATUS_PAUSED));
				// 状态为挂起的话
			case DownloadManager.STATUS_PENDING:
				LogUtils.e("STATUS_PENDING");
				dlHandler.sendMessage(dlHandler.obtainMessage(0, bytesDL, fileSize, DownloadManager.STATUS_PENDING));
				// 状态为运行的话
			case DownloadManager.STATUS_RUNNING:
				// 正在下载，不做任何事情
				LogUtils.e("STATUS_RUNNING");
				dlHandler.sendMessage(dlHandler.obtainMessage(0, bytesDL, fileSize, DownloadManager.STATUS_RUNNING));
				break;
			// 状态为成功的话
			case DownloadManager.STATUS_SUCCESSFUL:
				// 完成
				LogUtils.e("下载完成");
				dlHandler.sendMessage(dlHandler.obtainMessage(0, bytesDL, fileSize, DownloadManager.STATUS_SUCCESSFUL));

				break;
			// 状态为是失败的话
			case DownloadManager.STATUS_FAILED:
				// 清除已下载的内容，重新下载
				LogUtils.e("STATUS_FAILED");
				dowanloadmanager.remove(lastDownloadId);
				dlHandler.sendMessage(dlHandler.obtainMessage(0, bytesDL, fileSize, DownloadManager.STATUS_FAILED));
				break;
			}

			c.close();
		}
	}

	public void startUpgrade() {
		File file = new File(filePath);
		if (file != null) {

			LogUtils.e("app md5=" + appInfo.getPkgmd5());

			try {
				String md5 = MD5FileUtil.getFileMD5String(file);
				LogUtils.e("file md5=" + md5);

				if (md5.equals(appInfo.getPkgmd5())) {
					// Toast.makeText(OtaOnlineActivity.this, "升级包校验成功!",
					// Toast.LENGTH_LONG).show();
					LogUtils.e("md5 ==");

					showUpgradeDialog(filePath);

				} else {

					Toast.makeText(mContext, "升级包校验失败,请重新下载!", Toast.LENGTH_LONG).show();
					LogUtils.e("md5 !=");

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	// 返回下载状态，一个数字
	public boolean isDownloading(int downloadManagerStatus) {
		return downloadManagerStatus == DownloadManager.STATUS_RUNNING
				|| downloadManagerStatus == DownloadManager.STATUS_PAUSED
				|| downloadManagerStatus == DownloadManager.STATUS_PENDING;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// 这里可以取得下载的id，这样就可以知道哪个文件下载完成了。适用与多个下载任务的监听
			Log.e("tag",
					"DownloadManager.EXTRA_DOWNLOAD_ID=" + intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
			Log.e("", "downlaod onReceive************************************************************** ");
			queryDownloadStatus();
			// zzz checkBtn.setText("升级");
			// zzz checkBtn.setEnabled(true);
			startUpgrade();
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

}
