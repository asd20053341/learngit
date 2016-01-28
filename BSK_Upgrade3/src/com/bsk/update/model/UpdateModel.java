package com.bsk.update.model;

import android.graphics.drawable.Drawable;


public class UpdateModel {
	
	private Drawable appIcon;
	private String appName;			//应用名称
	private String pkgName;			//应用包名
	private String versionName;		//应用版本名称
	private String versionNameNew;		//应用新版本名称
	private int versionCode;		//应用版本code
	private int newCode;			//应用最新版本code
	private String appMd5;			//应用apk的MD5
	private String pkgUrl;			//应用下载地址
	private String updateTip;
	private long dlSize;
	private long fileSize;
	private String fileName;		//升级包文件名
	private boolean isDownloading;	//是否在下载中
	private boolean isUpgrade;		//是否可升级
	private boolean isUpgraded;	//是否已升级
	private long downloadId;		//下载中的downloadId
	
	public Drawable getAppIcon() {
		return appIcon;
	}
	public void setAppIcon(Drawable appIcon) {
		this.appIcon = appIcon;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getPkgName() {
		return pkgName;
	}
	public void setPkgName(String pkgName) {
		this.pkgName = pkgName;
	}
	public String getVersionName() {
		return versionName;
	}
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	public String getVersionNameNew() {
		return versionNameNew;
	}
	public void setVersionNameNew(String versionNameNew) {
		this.versionNameNew = versionNameNew;
	}
	public int getVersionCode() {
		return versionCode;
	}
	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}
	public String getAppMd5() {
		return appMd5;
	}
	public void setAppMd5(String appMd5) {
		this.appMd5 = appMd5;
	}
	public int getNewCode() {
		return newCode;
	}
	public void setNewCode(int newCode) {
		this.newCode = newCode;
	}
	public String getUpdateTip() {
		return updateTip;
	}
	public void setUpdateTip(String updateTip) {
		this.updateTip = updateTip;
	}
	public String getPkgUrl() {
		return pkgUrl;
	}
	public void setPkgUrl(String pkgUrl) {
		this.pkgUrl = pkgUrl;
	}
	public long getDlSize() {
		return dlSize;
	}
	public void setDlSize(long dlSize) {
		this.dlSize = dlSize;
	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public boolean isDownloading() {
		return isDownloading;
	}
	public void setDownloading(boolean isDownloading) {
		this.isDownloading = isDownloading;
	}
	public boolean isUpgrade() {
		return isUpgrade;
	}
	public void setUpgrade(boolean isUpgrade) {
		this.isUpgrade = isUpgrade;
	}
	public boolean isUpgraded() {
		return isUpgraded;
	}
	public void setUpgraded(boolean isUpgraded) {
		this.isUpgraded = isUpgraded;
	}
	public long getDownloadId() {
		return downloadId;
	}
	public void setDownloadId(long downloadId) {
		this.downloadId = downloadId;
	}
	
}
