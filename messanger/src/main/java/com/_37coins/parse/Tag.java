package com._37coins.parse;

import java.util.Locale;

import com._37coins.workflow.pojo.Request.ReqAction;

public class Tag {
	
	public Tag(ReqAction cmd, Locale lng){
		this.cmd = cmd;
		this.lng = lng;
	}
	
	private ReqAction cmd;
	private Locale lng;
	
	public ReqAction getAction(){
		return cmd;
	}
	
	public Locale getLocale(){
		return lng;
	}

}
