package com._37coins.resources;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Account;
import com._37coins.persistence.dto.Gateway;
import com._37coins.persistence.dto.MsgAddress;

@Path(GatewayResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class GatewayResource {
	public final static String PATH = "/api/gateway";
	public final static String HTML_RESPONSE_DONE = "<html><head><title>Confirmation</title></head><body>The gateway has been registered successfully.</body></html>";
	
	final private GenericRepository dao;
	
	@Inject public GatewayResource(ServletRequest request) {
		HttpServletRequest httpReq = (HttpServletRequest)request;
		dao = (GenericRepository)httpReq.getAttribute("gr");
	}
	
	@GET
	@RolesAllowed({"gateway"})
	public Response get(@Context SecurityContext context){
		if (null!=context.getUserPrincipal()){
			String username = context.getUserPrincipal().getName();
			return Response.ok("<html><body>username: "+username+"</body></html>", MediaType.TEXT_HTML_TYPE).build();
		}else{
			return Response.ok("<html><body>unautheticated</body></html>", MediaType.TEXT_HTML_TYPE).build();
		}
	}
	
	@POST
	@Path("/login")
	@RolesAllowed({"gateway","admin"})
	public List<String> login(@Context SecurityContext context){
		List<String> roles = new ArrayList<>();
		if (null!=context.getUserPrincipal()){
			roles.add("gateway");
			roles.add("admin");
			Iterator<String> i = roles.iterator();
			while (i.hasNext()){
				String role = i.next();
				if (!context.isUserInRole(role)){
					i.remove();
				}
			}
		}
		return roles;
	}

	@POST
	public Response register(@FormParam("ownerAddress") String ownerAddress, 
			@FormParam("address") String address, 
			@FormParam("coutryCode") String countryCode,
			@FormParam("fee") String fee,
			@FormParam("password") String password){
		RNQuery query = new RNQuery().addFilter("address", ownerAddress);
		MsgAddress ma = dao.queryEntity(query, MsgAddress.class,false);
		if (null==ma){
			ma = new MsgAddress()
				.setAddress(query.getFilter("address"))
				.setOwner(new Account());
			dao.add(ma);
		}
		RNQuery gqQuery = new RNQuery().addFilter("address", address);
		Gateway gw = dao.queryEntity(gqQuery, Gateway.class, false);
		if (gw==null){
			gw = new Gateway()
				.setAddress(address)
				.setCountryCode((null!=countryCode)?Integer.parseInt(countryCode):null)
				.setFee(new BigDecimal(fee).setScale(8,RoundingMode.HALF_UP))
				.setPassword(password)
				.setOwner(ma.getOwner());
			dao.add(gw);
			return Response.ok(HTML_RESPONSE_DONE, MediaType.TEXT_HTML_TYPE).build();
		}else{
			throw new WebApplicationException("gateway exists already",
					javax.ws.rs.core.Response.Status.CONFLICT);
		}
	}
	
}