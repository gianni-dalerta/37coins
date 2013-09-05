package com._37coins.resources;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.Account;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;


@Api(value = HealthCheckResource.PATH, description = "a resource to test database access.")
@Path(HealthCheckResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckResource {
	public final static String PATH = "/healthcheck";
	
	@Inject protected GenericRepository dao;

	@GET
	@ApiOperation(value = "check health.", notes = "this will also execute database access.")
    @ApiErrors(value = { @ApiError(code = 500, reason = "Internal Server Error.")})
	public Map<String,String> healthcheck(){
		dao.count(new RNQuery(), Account.class);
		Map<String,String> rv = new HashMap<>(1);
		rv.put("status", "ok!");
		return rv;
	}
	
}
