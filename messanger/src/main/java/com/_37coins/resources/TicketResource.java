package com._37coins.resources;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com._37coins.MessagingServletConfig;

@Path(TicketResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class TicketResource {
	public final static String PATH = "/ticket";
	public static Logger log = LoggerFactory.getLogger(TicketResource.class);
		
	private final Cache cache;
		
	private final HttpServletRequest httpReq;

	@Inject
	public TicketResource(Cache cache,
				ServletRequest request){
			this.cache = cache;
			httpReq = (HttpServletRequest)request;
	}
	
	/**
	 * a ticket is a token to execute critical or expensive code, like sending email.
	 * a ticket will be given out a few times free, then limited by turing tests.
	 */
	@POST
	public Pair<String,String> getTicket(){
		Element e = cache.get(getRemoteAddress());
		if (e!=null){
			if (e.getHitCount()>3){
				//TODO: implement turing test
				throw new WebApplicationException("to many requests", Response.Status.BAD_REQUEST);
			}
		}else{
			cache.put(new Element(getRemoteAddress(),getRemoteAddress()));
		}
		String ticket = RandomStringUtils.random(14, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
		cache.put(new Element("ticket"+ticket,ticket));
		return Pair.of("ticket",ticket);
	}
	
	/**
	 * recaptcha
	 */
	@POST
	@Path("/captcha")
	public Pair<String,String> recaptcha(@FormParam("chal") String challenge,
			@FormParam("resp") String response){
        ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
        reCaptcha.setPrivateKey(MessagingServletConfig.captchaSecKey);
        ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(getRemoteAddress(), challenge, response);
        if (reCaptchaResponse.isValid()) {
        	cache.remove(getRemoteAddress());
    		String ticket = RandomStringUtils.random(14, "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789");
    		cache.put(new Element("ticket"+ticket,ticket));
    		return Pair.of("ticket",ticket);
        } else {
          	throw new WebApplicationException("error", Response.Status.BAD_REQUEST);
        }
	}
	
	
	private String getRemoteAddress(){
		String ip = httpReq.getRemoteAddr();
		//TODO: actually we need to check the proxy header too
		return ip;
	}
	
	
	@GET
	public Pair<String,String> validateToken(@QueryParam("ticket") String ticket){
		Element e = cache.get("reset"+ticket);
		if (null!=e){
			return Pair.of("status", "active");
		}else{
			return Pair.of("status", "inactive");
		}
	}

}
