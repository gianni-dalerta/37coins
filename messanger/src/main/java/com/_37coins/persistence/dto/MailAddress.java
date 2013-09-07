package com._37coins.persistence.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Serialized;

import org.apache.commons.lang.RandomStringUtils;
import org.restnucleus.dao.Model;

import com._37coins.pojo.ServiceList;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class MailAddress extends Model {
	private static final long serialVersionUID = 953852143451239327L;
	
	public static MailAddress prepareNewMail(String address, ServiceList service) {
		MailAddress rv = new MailAddress().setAddress(address);
		for (String category : service.getDefaultCategories()) {
			rv.addActiveCategory(category, service.getName());
		}
		return rv;
	}
	
	//ISO 639 language code
	@Persistent
	private String language;
	
	@Index
	@Persistent
	private String address;
	
	@Persistent
	private List<String> secret;
	
	//this is a string like: service::category
	//to reduce db schema granularity 
	@Persistent
	private List<String> activeCategories;
	
	// {secret, [service:category, service:category]}
	@Persistent
	@Serialized
	private Map<String,List<String>> newCategories;
	
	@JsonIgnore
	public void processSecret(String secret){
		if (null == secret || null == newCategories || null == this.secret){
			//throw exception
		}
		boolean found = false;
		for (String existing: this.secret){
			if (existing.equalsIgnoreCase(secret)){
				this.secret.remove(existing);
				found = true;
				break;
			}
		}
		if (!found){
			//throw exception
		}else{
			List<String> newCategories = this.newCategories.get(secret);
			this.newCategories.remove(secret);
			updateActiveCategories(newCategories);
		}
	}
	
	public void updateActiveCategories(List<String> newCategories, String serviceName){
		List<String> categories = new ArrayList<>(newCategories.size());
		for (String cat: newCategories){
			categories.add(serviceName + "::" + cat);
		}
		updateActiveCategories(categories);
	}
	
	/**
	 * 
	 * @param newCategories a list of new categories including service, like [service::category]
	 */
	public void updateActiveCategories(List<String> newCategories){
		if (null==newCategories || newCategories.size() < 1){
			return;
		}
		//remove all activeCategories with same service
		String service = newCategories.get(0).split("::")[0];
		Iterator<String> caterogyIterator = activeCategories.iterator();
		while (caterogyIterator.hasNext()){
			String category = caterogyIterator.next();
			if (category.split("::")[0].equalsIgnoreCase(service)){
				caterogyIterator.remove();
			}
		}
		//add categories
		for (String category: newCategories){
			if (category.split("::").length>1){
				this.addActiveCategory(category);
			}
		}		
	}
	
	@JsonIgnore
	public List<String> getActiveCategoriesWithoutService(){
		if (null == activeCategories){
			return null;
		}
		List<String> rv = new ArrayList<>();
		for (String concat: activeCategories){
			rv.add(concat.split("::")[1]);
		}
		return rv;
	}
	
	@JsonIgnore
	public MailAddress setActiveCategories(List<String> activeCategories, String serviceName) {
		activeCategories = new ArrayList<>();
		for (String category: activeCategories){
			this.addActiveCategory(category, serviceName);
		}
		return this;
	}
	
	@JsonIgnore
	public MailAddress addActiveCategory(String category){
		if (null == activeCategories){
			activeCategories = new ArrayList<String>();
		}
		if (null!=category && category.length()>0){
			activeCategories.add(category);
		}
		return this;
	}
	
	@JsonIgnore
	public MailAddress addActiveCategory(String category, String serviceName){
		if (null == activeCategories){
			activeCategories = new ArrayList<>();
		}
		activeCategories.add(serviceName+"::"+category);
		return this;
	}
	
	@JsonIgnore
	public String addNewCategories(List<String> newCategories, String serviceName){
		//create secret
		if (null == this.secret){
			this.secret = new ArrayList<>();
		}
		String secret = RandomStringUtils.random(8,true,true);
		this.secret.add(secret);
		if (null == this.newCategories){
			this.newCategories = new HashMap<>();
		}
		List<String> categories = new ArrayList<>();
		for (String newCategory: newCategories){
			categories.add(serviceName+"::"+newCategory);
		}
		if (categories.size() == 0){
			categories.add(serviceName+"::");
		}
		this.newCategories.put(secret,categories); 
		return secret;
	}
	
	@JsonIgnore
	public boolean containsCategory(String category){
		boolean contains = false;
		for (String existing: getActiveCategoriesWithoutService()){
			if (existing.equalsIgnoreCase(category)){
				contains = true;
				break;
			}
		}
		return contains;
	}
	
	@JsonIgnore
	public MailAddress addSecret(String secret) {
		if (null == this.secret){
			this.secret = new ArrayList<>();
		}
		this.secret.add(secret);
		return this;
	}
	
	//****default getters and setters

	public String getLanguage() {
		return language;
	}

	public MailAddress setLanguage(String language) {
		this.language = language;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public MailAddress setAddress(String address) {
		this.address = address;
		return this;
	}

	public List<String> getSecret() {
		return secret;
	}

	public MailAddress setSecret(List<String> secret) {
		this.secret = secret;
		return this;
	}
	
	public List<String> getActiveCategories() {
		return activeCategories;
	}

	public MailAddress setActiveCategories(List<String> activeCategories) {
		this.activeCategories = activeCategories;
		return this;
	}

	public Map<String,List<String>> getNewCategories() {
		return newCategories;
	}

	public MailAddress setNewCategories(Map<String,List<String>> newCategories) {
		this.newCategories = newCategories;
		return this;
	}
	
	public void update(Model newInstance) {
		MailAddress n = (MailAddress) newInstance;
		if (null != n.getAddress())this.setAddress(n.getAddress());
		if (null != n.getLanguage())this.setLanguage(n.getLanguage());
		if (null != n.getSecret())this.setSecret(n.getSecret());
		if (null != n.getActiveCategories())this.setActiveCategories(n.getActiveCategories());
		if (null != n.getNewCategories())this.setNewCategories(n.getNewCategories());
	}

}
