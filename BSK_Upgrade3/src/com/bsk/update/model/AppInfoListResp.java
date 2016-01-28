package com.bsk.update.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppInfoListResp implements Serializable {

	private static final long serialVersionUID = -450939674860428580L;

	protected int ret;
	protected String msg;
	
	private List<AppInfo> apps = new ArrayList<AppInfo>();

	public int getRet() {
		return ret;
	}

	public void setRet(int ret) {
		this.ret = ret;
	}
	
 	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public List<AppInfo> getApps() {
		return apps;
	}

	public void setApps(List<AppInfo> apps) {
		this.apps = apps;
	}

}
