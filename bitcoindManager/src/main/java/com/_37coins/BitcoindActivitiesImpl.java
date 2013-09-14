package com._37coins;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	public String sendTransaction(BigDecimal amount, BigDecimal fee, Long fromId, String toId, String toAddress, String id, String message){
		BigDecimal balance = client.getbalance(fromId.toString()).setScale(8,RoundingMode.FLOOR);
		if (balance.compareTo(amount.add(fee).setScale(8))<0){
			throw new RuntimeException("insufficient funds.");
		}
		if (null!=toId){
			boolean rv = client.move(
					fromId.toString(), 
					toId, 
					amount, 
					1L, 
					id + ((null!=message)?"::"+message:"::_")+((null!=toAddress)?"::"+toAddress:""));
			if (!rv){
				throw new RuntimeException("move failed."); 
			}
		}else{
			String rv = null;
				rv = client.sendfrom(fromId.toString(), toAddress, amount, 1L, id + ((null!=message)?"::"+message:null),toAddress);
			return rv;
		}
		return null;
	}

	@Override
	public BigDecimal getAccountBalance(Long accountId) {
		BigDecimal rv = client.getbalance(accountId.toString(), 0);
		rv = rv.setScale(8, RoundingMode.DOWN);
		return rv;
	}
	
	@Override
	public String getNewAddress(Long accountId) {
		return client.getaccountaddress(accountId.toString());
	}

	@Override
	public Long getAccount(String bcAddress) {
		String rv = client.getaccount(bcAddress);
		return Long.parseLong(rv);
	}

	@Override
	public List<Transaction> getAccountTransactions(Long accountId) {
		//get last transactions
		List<Transaction> list = client.listtransactions(accountId.toString(), 12, 0);
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
				if (tx.getComment().split("::").length>2){
					to = tx.getComment().split("::")[2];
				}
				if (tx.getTo()!=null){
					to = tx.getTo();
				}
			}
			rv.add(new Transaction()
				.setAmount(total)
				.setComment(comment)
				.setTime(time)
				.setTo(to));
		}
		Collections.sort(rv, new Comparator<Transaction>() {
		    public int compare(Transaction t1, Transaction t2) {
		        return (t2.getTime() != t1.getTime())?((t2.getTime() < t1.getTime())?-1:1):0;
		    }
		});
		return rv.subList(0, 3);
	}

}
