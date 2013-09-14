package com._37coins.persistence.dto;

import java.util.Locale;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.validation.constraints.NotNull;

import org.restnucleus.dao.Model;

import com._37coins.workflow.pojo.MessageAddress.MsgType;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class MsgAddress extends Model {
	private static final long serialVersionUID = 953812543451239327L;
	
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
	
	public Gateway getGateway() {
		return gateway;
	}

	public MsgAddress setGateway(Gateway gateway) {
		this.gateway = gateway;
		return this;
	}

	public void update(Model newInstance) {
		MsgAddress n = (MsgAddress) newInstance;
		if (null != n.getAddress())this.setAddress(n.getAddress());
		if (null != n.getLocale())this.setLocale(n.getLocale());
		if (null != n.getType())this.setType(n.getType());
		if (null != n.getGateway())this.setGateway(n.getGateway());
	}

}
