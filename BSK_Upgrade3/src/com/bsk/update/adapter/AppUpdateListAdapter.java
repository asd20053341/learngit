package com.bsk.update.adapter;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bsk.update.model.UpdateModel;
import com.bsk.update.util.FileUtils;
import com.bsk.update.util.LogUtils;
import com.bsk.update1.R;

public class AppUpdateListAdapter extends BaseAdapter {

	private final String TAG = "AppUpdateListAdapter";

	private LayoutInflater mInflater = null;
	private Context context;
	private List<UpdateModel> dataList;
	
	private Handler handler;
	
	private int position;	

	public AppUpdateListAdapter(Context context, List<UpdateModel> dataList , Handler handler) {
		
		this.context = context;
		this.mInflater = LayoutInflater.from(this.context);
		this.handler = handler;

		this.dataList = dataList;
		
	}
	
	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int getCount() {
		return dataList.size();
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public View getView(final int position, View view, ViewGroup parent) {

		final ViewHolder holder;
		if (view == null) {

			view = this.mInflater.inflate(R.layout.list_item_app, null);
			holder = new ViewHolder();
			holder.mLogoIv = (ImageView) view.findViewById(R.id.logo_iv);
		//	holder.mNameTv = (TextView) view.findViewById(R.id.name_tv);
			holder.mVersionTv = (TextView) view.findViewById(R.id.version_tv);
			holder.mTipTv = (TextView) view.findViewById(R.id.tip_tv);
		//	holder.mSizeTv = (TextView) view.findViewById(R.id.dlsize_tv);
			holder.mPercentTv = (TextView) view.findViewById(R.id.precent_tv);
			
			holder.mProgressBar = (ProgressBar)view.findViewById(R.id.download_progress);
			holder.mUpdateBtn = (Button) view.findViewById(R.id.update_btn);

			view.setTag(holder);
		} else {

			holder = (ViewHolder) view.getTag();

		}
		
		
		holder.mLogoIv.setImageDrawable(dataList.get(position).getAppIcon()); 
		
	//	holder.mNameTv.setText( dataList.get(position).getAppName());
		holder.mVersionTv.setText(dataList.get(position).getAppName() + "  (" + dataList.get(position).getVersionName() +")");
	//	holder.mSizeTv.setText( );
		holder.mPercentTv.setText( FileUtils.getAppSize(dataList.get(position).getDlSize()) + " / " + FileUtils.getAppSize(dataList.get(position).getFileSize()) +
				"  (" + FileUtils.getNotiPercent(dataList.get(position).getDlSize(), dataList.get(position).getFileSize() ) + ")");
		
		if(dataList.get(position).isDownloading()) {
			holder.mTipTv.setVisibility(View.INVISIBLE);
			holder.mProgressBar.setVisibility(View.VISIBLE);
			holder.mPercentTv.setVisibility(View.VISIBLE);
		}

		if(dataList.get(position).getDlSize() > 0) {
			holder.mProgressBar.setIndeterminate(false);
			holder.mProgressBar.setMax((int)dataList.get(position).getFileSize());
			holder.mProgressBar.setProgress((int)dataList.get(position).getDlSize());
		}
		
		
		if(dataList.get(position).getNewCode() > dataList.get(position).getVersionCode()) {
		//	holder.mTipTv.setTextColor(context.getResources().getColor(R.color.red));
			holder.mUpdateBtn.setEnabled(true);
		}else {
		//	holder.mTipTv.setTextColor(context.getResources().getColor(R.color.green));
			holder.mUpdateBtn.setEnabled(false);
		}
		
	//	if() isUpgraded bg enable
		if(dataList.get(position).isUpgraded()) {
			holder.mUpdateBtn.setEnabled(false);
		//	holder.mUpdateBtn.setBackground(r);
		}else {
			holder.mUpdateBtn.setEnabled(true);
		//	holder.mUpdateBtn.setBackground(r);
		}
		
		holder.mTipTv.setText( dataList.get(position).getUpdateTip());

		holder.mUpdateBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LogUtils.e("opt btn click!=" + position);
				
				if(!dataList.get(position).isDownloading()) {
					LogUtils.e("Download="  );
					Message msg = handler.obtainMessage();
					msg.what = 2;
					Bundle bundle = new Bundle();
					
					bundle.putInt("pos", position);
					msg.setData(bundle);
					
					msg.sendToTarget();
					
				}else if(dataList.get(position).isUpgraded()) {
					LogUtils.e("Upgraded"  );
					Message msg = handler.obtainMessage();
					msg.what = 3;
					Bundle bundle = new Bundle();
					
					bundle.putInt("pos", position);
					msg.setData(bundle);
					
					msg.sendToTarget();
				}
				
			}
		});

		return view;
	}

	public final class ViewHolder {
		public ImageView mLogoIv;
		public TextView mNameTv;
		public TextView mVersionTv;
		public TextView mTipTv;
		public TextView mSizeTv;
		public TextView mPercentTv;
		public ProgressBar mProgressBar;
		public Button mUpdateBtn;
	}
	
	
}
