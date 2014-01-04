package com._37coins;

import java.io.Serializable;

import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;

public class WebSessionManager extends DefaultWebSessionManager {
	
   @Override
   public boolean isSessionIdCookieEnabled() {
       return false;
   }

   @Override
   public Serializable getSessionId(SessionKey key) {
	   return key.getSessionId();
   }
}
