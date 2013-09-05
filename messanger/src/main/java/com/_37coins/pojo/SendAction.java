package com._37coins.pojo;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SendAction {
	public static final String FILE_NAME = "/WEB-INF/sendActions.json";
	public static final String LOCAL_RESOURCE_PATH = "src/main/webapp"+FILE_NAME;
	private JsonNode rootNode;
	
	public SendAction(ServletContext servletContext){
		ObjectMapper om = new ObjectMapper();
		if (servletContext == null) {
			try {
				this.rootNode = om.readTree(new FileReader(LOCAL_RESOURCE_PATH));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				this.rootNode = om.readTree(servletContext.getResourceAsStream(FILE_NAME));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String getListMemberValueByKeyName(String name, String member){
		String rv = null;
		Iterator<JsonNode> i = rootNode.iterator();
		while(i.hasNext()){
			JsonNode node = i.next();
			if (node.get("sendAction").asText().equalsIgnoreCase(name)){
				rv = node.get(member).asText();
				break;
			}
		}
		return rv;
	}
	
	public String getCategory(String name){
		return getListMemberValueByKeyName(name, "categoryName");
	}
	
	public String getTemplateId(String name){
		return getListMemberValueByKeyName(name, "templateId");
	}

}
