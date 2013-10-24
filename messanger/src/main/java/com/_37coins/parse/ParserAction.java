package com._37coins.parse;

import com._37coins.workflow.pojo.DataSet;

public interface ParserAction {
	
	public void handleWithdrawal(DataSet data);
	
	public void handleDeposit(DataSet data);
	
	public void handleResponse(DataSet data);
	
	public void handleConfirm(DataSet data);

}
