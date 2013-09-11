package com._37coins.persistence.dto;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.validation.constraints.NotNull;

import org.restnucleus.dao.Model;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class MsgAddress extends Model {
	private static final long serialVersionUID = 953812543451239327L;
	
	@Persistent
	@Index
	@NotNull
	private Account owner;
	
	@Persistent
	private String locale;
	
	@Persistent
	private String address;
	
	@Persistent
	private String type;
	

	public Account getOwner() {
		return owner;
	}

	public MsgAddress setOwner(Account owner) {
		this.owner = owner;
		return this;
	}

	public String getLocale() {
		return locale;
	}

	public MsgAddress setLocale(String locale) {
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

	public String getType() {
		return type;
	}

	public MsgAddress setType(String type) {
		this.type = type;
		return this;
	}
	
	public void update(Model newInstance) {
		MsgAddress n = (MsgAddress) newInstance;
		if (null != n.getAddress())this.setAddress(n.getAddress());
		if (null != n.getLocale())this.setLocale(n.getLocale());
		if (null != n.getType())this.setType(n.getType());
	}

}
