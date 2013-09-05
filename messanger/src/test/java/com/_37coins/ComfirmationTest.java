package com._37coins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.junit.Assert;
import org.junit.Test;

import com._37coins.persistence.dto.MailAddress;
import com._37coins.pojo.SendAction;
import com._37coins.pojo.ServiceEntry;
import com._37coins.pojo.ServiceList;

public class ComfirmationTest {
	public static ServiceList s = null;
	static{
		List<ServiceEntry> serviceList = ServiceList.initialize(null);
		s = new ServiceList("37coins", serviceList);
	}
	
	@Test
	public void testPrepare(){
		MailAddress ma = MailAddress.prepareNewMail("test@37coins.com", s);
		Assert.assertTrue(ma.getActiveCategories().contains("37coins::newsletter"));
	}
	
	@Test
	public void testEditCategories(){
		MailAddress ma = MailAddress.prepareNewMail("test@37coins.com", s);
		ma.addNewCategories(Arrays.asList("newsletter2"), "37coins");
		ma.processSecret(ma.getSecret().get(0));
		Assert.assertTrue(ma.getActiveCategories().contains("37coins::newsletter2"));
		Assert.assertEquals(1, ma.getActiveCategories().size());
	}
	@Test
	public void test2Services(){
		MailAddress ma = MailAddress.prepareNewMail("test@37coins.com", s);
		ma.addNewCategories(Arrays.asList("newsletter"), "korbit");
		Assert.assertFalse(ma.getActiveCategories().contains("korbit::newsletter"));
		ma.processSecret(ma.getSecret().get(0));
		Assert.assertTrue(ma.getActiveCategories().contains("37coins::newsletter"));
		Assert.assertTrue(ma.getActiveCategories().contains("korbit::newsletter"));
	}
	
	@Test
	public void testDeleteAll(){
		MailAddress ma = MailAddress.prepareNewMail("test@37coins.com", s);
		ma.addNewCategories(new ArrayList<String>(), "37coins");
		ma.processSecret(ma.getSecret().get(0));
		Assert.assertEquals(0, ma.getActiveCategories().size());
	}
	
	@Test
	public void testSendAction(){
		SendAction sa = new SendAction(null);
		Assert.assertEquals(sa.getCategory("create"), "events");
	}

}
