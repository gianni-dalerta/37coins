package com._37coins.persistence.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Serialized;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.RandomStringUtils;
import org.restnucleus.dao.Model;

import com._37coins.pojo.ServiceList;
import com._37coins.workflow.pojo.MessageAddress.MsgType;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class MsgAddress extends Model {
	private static final long serialVersionUID = 953812543451239327L;
	
	public static MsgAddress prepareNewMail(String address, ServiceList service) {
		MsgAddress rv = new MsgAddress().setAddress(address);
		for (String category : service.getDefaultCategories()) {
			rv.addActiveCategory(category, service.getName());
		}
		return rv;
	}
	
	@Persistent
	@NotNull
	private Account owner;
	
	@Persistent
	@NotNull
	private Gateway gateway;
	
	@Persistent
	private Locale locale;
	
	@Persistent
	@Index
	private String address;
	
	@Persistent
	private MsgType type;
	
	
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
	
	
	//***************************
	//A bit of biz logic
	
	@JsonIgnore
	public MsgAddress addActiveCategory(String category, String serviceName){
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
	public void updateActiveCategories(List<String> newCategories, String serviceName){
		List<String> categories = new ArrayList<>(newCategories.size());
		for (String cat: newCategories){
			categories.add(serviceName + "::" + cat);
		}
		updateActiveCategories(categories);
	}
	
	@JsonIgnore
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
	public MsgAddress addActiveCategory(String category){
		if (null == activeCategories){
			activeCategories = new ArrayList<String>();
		}
		if (null!=category && category.length()>0){
			activeCategories.add(category);
		}
		return this;
	}
	
	//****************************
	//GETTERS & SETTERS
	

	public Account getOwner() {
		return owner;
	}

	public MsgAddress setOwner(Account owner) {
		this.owner = owner;
		return this;
	}

	public Locale getLocale() {
		return locale;
	}

	public MsgAddress setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public MsgAddress setAddress(String address) {
		this.address = address;
		return this;
	}

	public MsgType getType() {
		return type;
	}

	public MsgAddress setType(MsgType msgType) {
		this.type = msgType;
		return this;
	}
	
	public List<String> getActiveCategories() {
		return activeCategories;
	}

	public MsgAddress setActiveCategories(List<String> activeCategories) {
		this.activeCategories = activeCategories;
		return this;
	}
	
	public Gateway getGateway() {
		return gateway;
	}

	public MsgAddress setGateway(Gateway gateway) {
		this.gateway = gateway;
		return this;
	}
	
	public List<String> getSecret() {
		return secret;
	}

	public MsgAddress setSecret(List<String> secret) {
		this.secret = secret;
		return this;
	}

	public void update(Model newInstance) {
		MsgAddress n = (MsgAddress) newInstance;
		if (null != n.getAddress())this.setAddress(n.getAddress());
		if (null != n.getLocale())this.setLocale(n.getLocale());
		if (null != n.getActiveCategories())this.setActiveCategories(n.getActiveCategories());
		if (null != n.getType())this.setType(n.getType());
		if (null != n.getGateway())this.setGateway(n.getGateway());
	}

}
