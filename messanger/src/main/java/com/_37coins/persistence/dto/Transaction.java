package com._37coins.persistence.dto;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;

import org.apache.commons.lang.RandomStringUtils;
import org.restnucleus.dao.Model;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class Transaction extends Model {
	private static final long serialVersionUID = 2463534722502497789L;
	
	static public String generateKey(){
		return RandomStringUtils.random(5, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
	}
	
	@Persistent
	@Unique
	@Index
	private String key;
	
	@Persistent
	@Column(jdbcType = "CLOB")
	private String taskToken;
	
	public String getKey() {
		return key;
	}

	public Transaction setKey(String key) {
		this.key = key;
		return this;
	}

	public String getTaskToken() {
		try {
			return URLDecoder.decode(taskToken, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Transaction setTaskToken(String taskToken) {
		try {
			this.taskToken = URLEncoder.encode(taskToken,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public void update(Model newInstance) {
		// TODO Auto-generated method stub
	}

}
