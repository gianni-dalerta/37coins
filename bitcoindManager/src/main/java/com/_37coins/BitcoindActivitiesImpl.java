package com._37coins;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com._37coins.activities.BitcoindActivities;
import com._37coins.bcJsonRpc.BitcoindInterface;
import com._37coins.bcJsonRpc.pojo.Transaction;
import com.google.inject.Inject;

public class BitcoindActivitiesImpl implements BitcoindActivities {

	@Inject
	BitcoindInterface client;

	@Override
	public String sendTransaction(BigDecimal amount, BigDecimal fee, Long fromId, String toId, String toAddress){
		if (null!=toId){
			boolean rv = client.move(fromId.toString(), toId, amount);
			if (!rv){
				throw new RuntimeException("move failed."); 
			}
		}else{
			String rv = client.sendfrom(fromId.toString(), toAddress, amount);
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
		// TODO Auto-generated method stub
		return null;
	}

}
