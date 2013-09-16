package com._37coins.persistence.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.validation.constraints.NotNull;

import org.restnucleus.dao.Model;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class Gateway extends Model {
	private static final long serialVersionUID = -1031604697212697657L;

	@Persistent
	@NotNull
	private Account owner;
	
	@Persistent
	@NotNull
	private double fee;
	
	@Persistent
	@Index
	private String address;
	
	@Persistent
	@Index
	private Integer countryCode;

	public Account getOwner() {
		return owner;
	}

	public Gateway setOwner(Account owner) {
		this.owner = owner;
		return this;
	}

	public BigDecimal getFee() {
		return new BigDecimal(fee).setScale(8,RoundingMode.HALF_UP);
	}

	public Gateway setFee(BigDecimal fee) {
		this.fee = fee.doubleValue();
		return this;
	}

	public String getAddress() {
		return address;
	}

	public Gateway setAddress(String address) {
		this.address = address;
		return this;
	}
	
	public Integer getCountryCode() {
		return countryCode;
	}

	public Gateway setCountryCode(Integer countryCode) {
		this.countryCode = countryCode;
		return this;
	}

	@Override
	public void update(Model newInstance) {
		Gateway n = (Gateway) newInstance;
		if (null != n.getAddress())this.setAddress(n.getAddress());
		if (null != n.getFee())this.setFee(n.getFee());
		if (null != n.getCountryCode())this.setCountryCode(n.getCountryCode());
	}

}
