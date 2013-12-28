package com._37coins;

import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

public class WebSessionManager extends DefaultWebSessionManager {
	
   @Override
   public boolean isSessionIdCookieEnabled() {
       return false;
   }

}
