package com._37coins;

import java.util.Locale;
import java.util.Map;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.activities.CoreActivities;
import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.MsgAddress;

public class CoreActivitiesImpl implements CoreActivities {
	public static Logger log = LoggerFactory.getLogger(CoreActivitiesImpl.class);
	
	@Override
	public Map<String, Object> findAccountByMsgAddress(Map<String, Object> data) {
		GenericRepository dao = new GenericRepository();
		RNQuery q = new RNQuery().addFilter("address", (String)data.get("msgAddress"));
		MsgAddress ma = dao.queryEntity(q, MsgAddress.class,false);
		if (null==ma){
			dao.closePersistenceManager();
			throw new RuntimeException("account not found");
		}
		data.put("account", ma.getOwner().getId().toString());
		dao.closePersistenceManager();
		return data;
	}
	
	@Override
	public Map<String, Object> createDbAccount(Map<String, Object> data) {
		GenericRepository dao = new GenericRepository();
		MsgAddress ma = new MsgAddress()
			.setAddress((String)data.get("msgAddress"))
			.setLocale(((Locale)data.get("locale")).toString())
			.setType((String)data.get("source"))
			.setOwner(new Account());
		dao.add(ma);
		dao.flush();
		data.put("account", ma.getOwner().getId().toString());
		dao.closePersistenceManager();
		return data;
	}
	
	@Override
	public Map<String, Object> readAccount(Map<String, Object> data) {
		GenericRepository dao = new GenericRepository();
		Account account = dao.getObjectById(Long.parseLong((String)data.get("account")), Account.class);
		data.put("msgAddress", account.getFirstMsgAddress().getAddress());
		data.put("source", account.getFirstMsgAddress().getType());
		data.put("locale", new Locale(account.getFirstMsgAddress().getLocale()));
		dao.closePersistenceManager();
		return data;
	}

	@Override
	public Map<String, Object> findReceiverAccount(Map<String, Object> data) {
		String address = (String)data.get("receiverPhone");
		address = (address==null)?(String)data.get("receiverEmail"):address;
		if (address!=null){
			GenericRepository dao = new GenericRepository();
			RNQuery q = new RNQuery().addFilter("address", address);
			MsgAddress ma = dao.queryEntity(q, MsgAddress.class,false);
			if (null==ma){
				dao.closePersistenceManager();
				throw new RuntimeException("account not found");
			}
			data.put("receiverAccount", ma.getOwner().getId().toString());
			dao.closePersistenceManager();
		}else{
			throw new RuntimeException("Parameter missing");
		}
		return data;
	}

}
