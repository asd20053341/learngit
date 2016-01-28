package com.bsk.update.model;

import java.io.Serializable;

public class AppInfo implements Serializable {

	private static final long serialVersionUID = -4409074704113270895L;
	
	private String appname;		//应用名称
	private String pkgname;		//应用包名
	private String pkgver;	//应用版本Code
	private String pkgurl;		//下载地址
	private String pkgmd5;		//文件MD5
	private String pkgsize;
	private String remarks;
	public String getAppname() {
		return appname;
	}
	public void setAppname(String appname) {
		this.appname = appname;
	}
	public String getPkgname() {
		return pkgname;
	}
	public void setPkgname(String pkgname) {
		this.pkgname = pkgname;
	}
	public String getPkgver() {
		return pkgver;
	}
	public void setPkgver(String pkgver) {
		this.pkgver = pkgver;
	}
	public String getPkgurl() {
		return pkgurl;
	}
	public void setPkgurl(String pkgurl) {
		this.pkgurl = pkgurl;
	}
	public String getPkgmd5() {
		return pkgmd5;
	}
	public void setPkgmd5(String pkgmd5) {
		this.pkgmd5 = pkgmd5;
	}
	public String getPkgsize() {
		return pkgsize;
	}
	public void setPkgsize(String pkgsize) {
		this.pkgsize = pkgsize;
	}
	public String getRemarks() {
		return remarks;
	}
	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

}
