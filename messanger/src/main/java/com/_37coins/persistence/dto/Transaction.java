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

import org.apache.commons.lang3.RandomStringUtils;
import org.restnucleus.dao.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@PersistenceCapable
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class Transaction extends Model {
	private static final long serialVersionUID = 2463534722502497789L;
	
	public enum State {
		//REQUESTS
		STARTED("started"),
		CONFIRMED("confirmed"),
		COMPLETED("completed");

		private String text;

		State(String text) {
			this.text = text;
		}

		@JsonValue
		final String value() {
			return this.text;
		}

		public String getText() {
			return this.text;
		}

		@JsonCreator
		public static State fromString(String text) {
			if (text != null) {
				for (State b : State.values()) {
					if (text.equalsIgnoreCase(b.text)) {
						return b;
					}
				}
			}
			return null;
		}
	}
	
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
	
	@Persistent
	private State state;
	
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

	public State getState() {
		return state;
	}

	public Transaction setState(State state) {
		this.state = state;
		return this;
	}

	@Override
	public void update(Model newInstance) {
		// TODO Auto-generated method stub
	}
	
}
