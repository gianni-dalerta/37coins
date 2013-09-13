package com._37coins.persistence.dto;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;

import org.restnucleus.dao.Model;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class SendJournal extends Model {
	private static final long serialVersionUID = 853852143451932327L;
	
	@Unique
	@Persistent
	private String hash;
	
	@Persistent
	private MsgAddress destination;
	
	
	public String getHash() {
		return hash;
	}


	public SendJournal setHash(String hash) {
		this.hash = hash;
		return this;
	}


	public MsgAddress getDestination() {
		return destination;
	}


	public SendJournal setDestination(MsgAddress destination) {
		this.destination = destination;
		return this;
	}


	public void update(Model newInstance) {
		SendJournal n = (SendJournal) newInstance;
		if (null != n.getDestination())this.setDestination(n.getDestination());
	}

}
