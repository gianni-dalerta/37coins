package com._37coins.persistence.dto;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.restnucleus.dao.Model;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class Account extends Model {
	private static final long serialVersionUID = -792538125194459327L;
	public static final int PIN_MAX_WRONG = 3;
	
	// the name
	@Persistent
	private String firstName;

	// the name
	@Persistent
	private String lastName;
	
	// the language of the user
	@Persistent
	private String language;
	
	@Persistent
	private Integer pin;
	
	@Persistent
	private Integer pinWrongCount = 0;

	@Persistent(mappedBy="owner")
	private Set<MsgAddress> msgAddresses;
	
	
	public Set<MsgAddress> getMsgAddresses() {
		return msgAddresses;
	}
	
	public Account addMsgAddress(String address){
		MsgAddress ma = new MsgAddress().setAddress(address);
		if (null == this.msgAddresses){
			this.msgAddresses = new HashSet<>();
		}
		this.msgAddresses.add(ma);
		return this;
	}

	public Account setMsgAddresses(Set<MsgAddress> msgAddresses) {
		this.msgAddresses = msgAddresses;
		return this;
	}
	
	public MsgAddress getFirstMsgAddress(){
		if (null == msgAddresses)
			return null;
		Iterator<MsgAddress> i = msgAddresses.iterator();
		return (i.hasNext())?i.next():null;
	}
	
	public String getFirstName() {
		return firstName;
	}

	public Account setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public Account setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getLanguage() {
		return language;
	}

	public Account setLanguage(String language) {
		this.language = language;
		return this;
	}

	public Integer getPin() {
		return pin;
	}

	public Account setPin(Integer pin) {
		this.pin = pin;
		return this;
	}
	
	public Integer getPinWrongCount() {
		return pinWrongCount;
	}

	public Account setPinWrongCount(Integer pinWrongCount) {
		this.pinWrongCount = pinWrongCount;
		return this;
	}

	public void update(Model newInstance) {
		Account n = (Account) newInstance;
		if (null != n.getMsgAddresses())this.setMsgAddresses(n.getMsgAddresses());
		if (null != n.getFirstName())this.setFirstName(n.getFirstName());
		if (null != n.getLastName())this.setLastName(n.getLastName());
		if (null != n.getLanguage())this.setLanguage(n.getLanguage());
		if (null != n.getPin())this.setPin(n.getPin());
	}

}
