package com.bsk.update1;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.bsk.update1.R;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends FragmentActivity {

	// 系统升级按钮
	private Button otaBtn;
	// 应用升级按钮
	private Button appBtn;
	// 系统升级按钮下面的一个三角形图标
	private ImageView lTab;
	// 应用升级按钮下面的一个三角形图标
	private ImageView rTab;

	private Fragment otaFragment;
	private Fragment appFragment;

	private Resources res;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		res = this.getResources();
		
		lTab = (ImageView) findViewById(R.id.ltab_iv);
		rTab = (ImageView) findViewById(R.id.rtab_iv);
		otaBtn = (Button) findViewById(R.id.ota_btn);
		appBtn = (Button) findViewById(R.id.app_btn);

		// 获取了颜色好像没啥反映
		otaBtn.setTextColor(res.getColor(R.color.white));
		appBtn.setTextColor(res.getColor(R.color.gray));
		
		otaFragment = new OtaFragment();
		appFragment = new AppFragment();

		// 创建一个事物并提交
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fm.beginTransaction();
		fragmentTransaction.replace(R.id.container, otaFragment);
		fragmentTransaction.commit();

	}

	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_ota, container, false);
			return rootView;
		}
	}

	@SuppressWarnings("deprecation")
	public void selectFrag(View view) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fm.beginTransaction();
		
		if (view == findViewById(R.id.ota_btn)) {
			lTab.setVisibility(View.VISIBLE);
			rTab.setVisibility(View.INVISIBLE);
			otaBtn.setTextColor(res.getColor(R.color.white));
			appBtn.setTextColor(res.getColor(R.color.gray));
			fragmentTransaction.replace(R.id.container, otaFragment);
			// fragmentTransaction.hide(appFragment);
			// fragmentTransaction.show(otaFragment);
		} else {
			lTab.setVisibility(View.INVISIBLE);
			rTab.setVisibility(View.VISIBLE);
			otaBtn.setTextColor(res.getColor(R.color.gray));
			appBtn.setTextColor(res.getColor(R.color.white));
			fragmentTransaction.replace(R.id.container, appFragment);
			// fragmentTransaction.hide(otaFragment);
			// fragmentTransaction.show(appFragment);
		}
		fragmentTransaction.commit();
	}
}
