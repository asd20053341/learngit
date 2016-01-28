package com.bsk.update.view;

import android.content.Context;
import android.os.Handler;
import android.os.RecoverySystem;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsk.update.util.OtaUpgradeUtils;
import com.bsk.update1.R;

public class InstallPackage extends LinearLayout implements OtaUpgradeUtils.ProgressListener {

	private ProgressBar mProgressBar;
	private OtaUpgradeUtils mUpdateUtils;
	private LinearLayout mOutputField;
	private LayoutInflater mInflater;
	private String mPackagePath;
	private Handler mHandler = new Handler();
	private Button mDismiss;

	public InstallPackage(Context context, AttributeSet attrs) {
		super(context, attrs);
		// mUpdateUtils=OtaUpgradeUtils
		// mInflater=com.android.internal.policy.impl.PhoneLayoutInflater
		mUpdateUtils = new OtaUpgradeUtils(context);
		mInflater = LayoutInflater.from(context);
		requestFocus();
	}

	// path=/mnt/sdcard/astar_y3-ota-20150923.zip
	public void setPackagePath(String path) {
		mPackagePath = path;
	}

	// new InstallPackage时，执行这个方法， 当View中所有的子控件均被映射成xml后触发
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// 获取进度条
		mProgressBar = (ProgressBar) findViewById(R.id.verify_progress);
		// 获取LinearLayout
		mOutputField = (LinearLayout) findViewById(R.id.output_field);
		// 获取textView
		final TextView tv = (TextView) mInflater.inflate(R.layout.medium_text, null);

		// This action can not be interrupted when you are updating, please
		// backup you data.
		// 这个动作不能被打断,当你更新,请备份你的数据。
		tv.setText(R.string.install_ota_output_confirm);
		// 设置字体离控件的距离
		tv.setPadding(10, 8, 2, 2);
		tv.setTextSize(20);
		// 把TextView tv加载在LinearLayout mOutputField
		mOutputField.addView(tv);
		// 动画，就是tv.setText内容的显示效果
		Animation animation = new AlphaAnimation(0.0f, 1.0f);
		// Animation animation = new AlphaAnimation(1.0f,5.0f);
		// 动画持续时间，毫秒
		animation.setDuration(600);
		// 控件的动画显示顺序
		LayoutAnimationController controller = new LayoutAnimationController(animation);
		// 把动画设置给LinearLayout mOutputField
		mOutputField.setLayoutAnimation(controller);
		// 取消按钮
		mDismiss = (Button) findViewById(R.id.confirm_cancel);

		// 升级按钮
		findViewById(R.id.confirm_update).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.v("aaa", "onClick");
				// 移除视图,因为视图已经存在要先移除才可以再添加
				mOutputField.removeAllViews();
				// 文字：开始升级...过程当中请勿断电
				tv.setText(R.string.install_ota_output_start);
				// 加载到LinearLayout mOutputField
				mOutputField.addView(tv);

				new Thread(new Runnable() {
					@Override
					public void run() {
						// 这句话肯定是触发了OtaUpgradeUtils.ProgressListener里面的重写方法，所以执行到这句时，
						// 执行OtaUpgradeUtils.ProgressListener里面的重写方法
						mUpdateUtils.upgradeFromOta(mPackagePath, InstallPackage.this);

					}
				}).start();
				// 设置取消按钮不可点击
				mDismiss.setEnabled(false);

			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!mDismiss.isEnabled() && keyCode == KeyEvent.KEYCODE_BACK) {
			Log.v("aaa", "onKeyDown");
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}

	public void deleteSource(boolean b) {
		mUpdateUtils.deleteSource(b);
	}

	// 不断的调用此方法
	@Override
	public void onProgress(final int progress) {
		Log.v("zzzzb", "onProgress");
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				if (progress == 0) {
					TextView tv = (TextView) mInflater.inflate(R.layout.medium_text, null);
					// 正在校验升级包...
					tv.setText(R.string.install_ota_output_checking);
					tv.setPadding(10, 2, 2, 2);
					tv.setTextSize(20);
					mOutputField.addView(tv);
					Log.v("zzzzb", "正在校验升级包...");
				} else if (progress == 100) {
					TextView tv = (TextView) mInflater.inflate(R.layout.medium_text, null);
					// 升级包校验通过...
					tv.setText(R.string.install_ota_output_check_ok);
					tv.setPadding(10, 2, 2, 2);
					tv.setTextSize(20);
					mOutputField.addView(tv);
					Log.v("zzzzb", "升级包校验通过...");
				}
				// 因为onProgress会不断被调用，所以不断得设置progress为一半
				// 其实进度条是一直自动走，也就是说参数progress是系统传进来的，因为调用了OtaUpgradeUtils类中用了
				// RecoverySystem.verifyPackage方法，就会自动一直调用onProgress（final int
				// progress）方法，
				// 这里直接把进度设置为我们自己定义的界面显示出来，并每次都只显示一半，因为后面还有一半是复制升级包的进度显示。
				mProgressBar.setProgress(progress / 2);
				// System.out.println("progress=="+progress);
			}
		});
	}

	// 不断的调用次方法
	@Override
	public void onCopyProgress(final int progress) {
		Log.v("zzzzb", "onCopyProgress");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (progress == 0) {
					mOutputField.removeAllViews();
					TextView tv1 = (TextView) mInflater.inflate(R.layout.medium_text, null);
					// 正在复制文件...
					tv1.setText(R.string.install_ota_output_copying);
					tv1.setPadding(10, 2, 2, 2);
					tv1.setTextSize(20);
					mOutputField.addView(tv1);
					Log.v("zzzzb", "正在复制文件...");
				} else if (progress == 100) {
					TextView tv = (TextView) mInflater.inflate(R.layout.medium_text, null);
					// 复制完成...
					tv.setText(R.string.install_ota_output_copy_ok);
					tv.setPadding(6, 2, 2, 2);
					tv.setTextSize(14);
					mOutputField.addView(tv);
					Log.v("zzzzb", "复制完成....");
					tv = (TextView) mInflater.inflate(R.layout.medium_text, null);
					// 正在重启系统，开始升级...
					tv.setText(R.string.install_ota_output_restart);
					tv.setPadding(10, 2, 2, 2);
					tv.setTextSize(20);
					mOutputField.addView(tv);
					Log.v("zzzzb", "正在重启系统，开始升级...");
				}
				mProgressBar.setProgress(50 + progress / 2);
			}
		});
	}

	@Override
	public void onVerifyFailed(int errorCode, Object object) {
		Log.v("zzzzb", "onVerifyFailed");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				TextView tv = (TextView) mInflater.inflate(R.layout.medium_text, null);
				// 升级包校验失败，请重新检查您的升级包！
				Log.v("zzzzb", "校验失败");
				tv.setText(R.string.install_ota_output_check_error);
				tv.setTextColor(getResources().getColor(R.color.red));
				tv.setPadding(10, 2, 2, 2);
				tv.setTextSize(20);
				mOutputField.addView(tv);
				mDismiss.setEnabled(true);
			}
		});
	}

	@Override
	public void onCopyFailed(int errorCode, Object object) {
		Log.v("zzzzb", "onCopyFailed");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				TextView tv = (TextView) mInflater.inflate(R.layout.medium_text, null);
				// 复制失败，请检查您的磁盘空间！
				Log.v("zzzzb", "复制失败，请检查您的磁盘空间！");
				tv.setText(R.string.install_ota_output_copy_failed);
				tv.setTextColor(getResources().getColor(R.color.red));
				tv.setPadding(10, 2, 2, 2);
				tv.setTextSize(20);
				mOutputField.addView(tv);
				mDismiss.setEnabled(true);
			}
		});
	}
}
