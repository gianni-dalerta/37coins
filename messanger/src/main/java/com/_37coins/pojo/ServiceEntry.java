package com._37coins.pojo;

public class ServiceEntry {
	
	private String serviceName;
	private String categoryName;
	private String categoryDescription;
	private boolean isDefault;
	
	public String getServiceName() {
		return serviceName;
	}
	public ServiceEntry setServiceName(String serviceName) {
		this.serviceName = serviceName;
		return this;
	}
	public String getCategoryName() {
		return categoryName;
	}
	public ServiceEntry setCategoryName(String categoryName) {
		this.categoryName = categoryName;
		return this;
	}
	public String getCategoryDescription() {
		return categoryDescription;
	}
	public ServiceEntry setCategoryDescription(String categoryDescription) {
		this.categoryDescription = categoryDescription;
		return this;
	}
	public boolean isDefault() {
		return isDefault;
	}
	public ServiceEntry setDefault(boolean isDefault) {
		this.isDefault = isDefault;
		return this;
	}

}
