package com._37coins.pojo;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ServiceList {
	public static final String CATEGORIES_FILE_NAME = "/WEB-INF/categories.json";
	public static final String LOCAL_RESOURCE_PATH = "src/main/webapp"+CATEGORIES_FILE_NAME;
	public static List<ServiceEntry> initialize(ServletContext sc){
		List<ServiceEntry> serviceList = null;
		if (null == sc){
			try {
				serviceList = new ObjectMapper().readValue(
						new FileReader(LOCAL_RESOURCE_PATH),
						new TypeReference<List<ServiceEntry>>() {});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try {
				serviceList = new ObjectMapper().readValue(
						sc.getResourceAsStream(CATEGORIES_FILE_NAME),
						new TypeReference<List<ServiceEntry>>() {});
			} catch (IOException e1) {
				e1.printStackTrace();
			}			
		}
		return serviceList;
	}
	
	private String name;
	private List<ServiceEntry> list;
	

	public ServiceList(String name, List<ServiceEntry> list) {
		this.name = name;
		this.list = list;
	}

	public String getName() {
		return this.name;
	}

	public Map<String, String> getCategories() {
		return getCategories(null);
	}

	public Map<String, String> getCategories(List<String> filter) {
		Map<String, String> rv = new HashMap<>();
		for (ServiceEntry se : this.list) {
			if (se.getServiceName().equals(name)) {
				String category = se.getCategoryName();
				if (null == filter
						|| (null != filter && filter.contains(category))) {
					rv.put(category, se.getCategoryDescription());
				}
			}
		}
		return rv;
	}

	public List<String> getDefaultCategories() {
		List<String> rv = new ArrayList<>();
		for (ServiceEntry se : this.list) {
			if (se.getServiceName().equals(name) && se.isDefault()) {
				rv.add(se.getCategoryName());
			}
		}
		return rv;
	}
}
