package com._37coins;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com._37coins.activities.BitcoindActivities;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com._37coins.bcJsonRpc.pojo.Transaction.Category;
import com.google.inject.Inject;

public class BitcoindActivitiesImpl implements BitcoindActivities {

	@Inject
	BitcoindInterface client;

	@Override
	public String sendTransaction(BigDecimal amount, BigDecimal fee, String fromCn, String toCn, String toAddress, String id, String message){
		BigDecimal balance = client.getbalance(fromCn).setScale(8,RoundingMode.FLOOR);
		if (balance.compareTo(amount.add(fee).setScale(8))<0){
			throw new RuntimeException("insufficient funds.");
		}
		if (null!=toCn){
			boolean rv = client.move(
					fromCn, 
					toCn, 
					amount, 
					1L, 
					id + ((null!=message)?"::"+message:"::_")+((null!=toAddress)?"::"+toAddress:""));
			if (!rv){
				throw new RuntimeException("move failed."); 
			}
		}else{
			String rv = null;
				rv = client.sendfrom(fromCn, toAddress, amount, 1L, id + ((null!=message)?"::"+message:null),toAddress);
			return rv;
		}
		return null;
	}

	@Override
	public BigDecimal getAccountBalance(String cn) {
		BigDecimal rv = client.getbalance(cn, 0);
		rv = rv.setScale(8, RoundingMode.DOWN);
		return rv;
	}
	
	@Override
	public String getNewAddress(String cn) {
		return client.getaccountaddress(cn);
	}

	@Override
	public Long getAccount(String bcAddress) {
		String rv = client.getaccount(bcAddress);
		return Long.parseLong(rv);
	}
	
	@Override
	public BigDecimal getTransactionVolume(String cn, int hours) {
		//get last transactions
		List<Transaction> list = client.listtransactions(cn, 1000, 0);
		
		BigDecimal total = BigDecimal.ZERO;
		for (Transaction tx :list){
			if ((tx.getCategory()==Category.SEND || 
					(tx.getCategory()==Category.MOVE && tx.getAmount().compareTo(BigDecimal.ZERO)<0))
				&& tx.getTime()*1000 > System.currentTimeMillis()-((long)hours)*3600000L){
				//only take outgoing transactions into account
				total = total.add(tx.getAmount().abs());
			}
		}
		return total;
	}

	@Override
	public List<Transaction> getAccountTransactions(String cn) {
		//get last transactions
		List<Transaction> list = client.listtransactions(cn, 12, 0);
		//group them by workflowId
		Map<String, List<Transaction>> sets = new HashMap<>();
		for (Transaction t: list){
			String workflowId = (t.getComment()!=null&&t.getComment().contains("::"))?t.getComment().split("::")[0]:null;
			//capture all moven that follow scheme into sets, ignore other moves
			if (t.getCategory()==Category.MOVE){
				if (workflowId!=null ){
					if (sets.get(workflowId)==null){
						sets.put(workflowId, new ArrayList<Transaction>());
					}
					sets.get(workflowId).add(t);
				}
			//capture all send that follow scheme into sets
			}else if (workflowId!=null && t.getCategory()==Category.SEND){
				if (sets.get(workflowId)==null){
					sets.put(workflowId, new ArrayList<Transaction>());
				}
				sets.get(workflowId).add(t);
			//capture all the rest into single entries
			}else{
				sets.put(t.getTxid(), Arrays.asList(t));
			}
		}
		//sum up each set into one transaction
		List<Transaction> rv = new ArrayList<>();
		for (Entry<String,List<Transaction>> e: sets.entrySet()){
			BigDecimal total = BigDecimal.ZERO;
			String comment=null;
			Long time = null;
			String to =null;
			for (Transaction tx :e.getValue()){
				time = tx.getTime();
				//add up 37coins fees
				total = total.add(tx.getAmount());
				//add possible blockchain fee on send
				if (tx.getFee()!=null && tx.getCategory()==Category.SEND){
					total = total.add(tx.getFee());
				}
				//read in comments and receiver
				if (tx.getComment().split("::").length>1){
					String c = tx.getComment().split("::")[1];
					if (!c.equalsIgnoreCase("_")){
						comment = c;
					}
				}
				if(tx.getCategory()==Category.RECEIVE || tx.getCategory()==Category.SEND){
					to = tx.getTxid();
				}
				if (tx.getComment().split("::").length>3){
					String[] addr = tx.getComment().split("::");
					to = (total.compareTo(BigDecimal.ZERO)<0)?addr[2]:addr[3];
				}
				if (tx.getTo()!=null){
					to = tx.getTo();
				}
			}
			rv.add(new Transaction()
				.setAmount(total)
				.setComment(comment)
				.setTime(time*1000L)
				.setTo(to));
		}
		return rv;
	}

}
