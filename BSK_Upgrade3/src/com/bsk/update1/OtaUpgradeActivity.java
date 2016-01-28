package com.bsk.update1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bsk.update.view.InstallPackage;
import com.bsk.update1.R;

public class OtaUpgradeActivity extends Activity {
	
	private TextView versionTv;
	private Button localBtn;
	private Button onlineBtn;
	
	private Resources res;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ota_upgrade);
		
		res = this.getResources();
		
		versionTv = (TextView)this.findViewById(R.id.version_tv);
		localBtn = (Button)this.findViewById(R.id.local_btn);
		onlineBtn = (Button)this.findViewById(R.id.online_btn);
		
		String verion = Build.VERSION.RELEASE;
		versionTv.setText(res.getString(R.string.current_version) + " " + verion);
		
		localBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				
				Intent intent = new Intent(OtaUpgradeActivity.this, FileSelector.class);
                intent.putExtra(FileSelector.ROOT, "/mnt");
                startActivityForResult(intent, 0);
				
			}
		});
		
		onlineBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(OtaUpgradeActivity.this, OtaOnlineActivity.class);
                startActivity(intent);
			}
		});

	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
	        Bundle bundle = data.getExtras();
	        String file = bundle.getString("file");
	        if (file != null) {
	            final Dialog dlg = new Dialog(this, android.R.style.Theme_Holo_Light_Dialog);
	            dlg.setTitle(R.string.confirm_update);
	            LayoutInflater inflater = LayoutInflater.from(this);
	            InstallPackage dlgView = (InstallPackage) inflater.inflate(R.layout.install_ota, null, false);
	            dlgView.setPackagePath(file);
	            dlg.setContentView(dlgView);
	            dlg.findViewById(R.id.confirm_cancel).setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                    dlg.dismiss();
	                }
	            });
	            dlg.show();
	        }
        }
    }


}
