package com._37coins.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restnucleus.dao.GenericRepository;
import org.restnucleus.dao.RNQuery;

import com._37coins.persistence.dto.MailAddress;
import com._37coins.persistence.dto.SendJournal;
import com._37coins.pojo.ServiceEntry;
import com._37coins.pojo.ServiceList;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;

@Api(value = PreferenceResource.PATH, description = "Resource to manage email subscription")
@Path(PreferenceResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PreferenceResource {
	public static final String PATH = "/preference";
	public final static String HTML_RESPONSE_DONE = "<html><head><title>Confirmation</title></head><body>Your email subscription settings have been changed.</body></html>";

	@Inject
	protected GenericRepository dao;
	@Inject
	protected RNQuery query;
	@Inject
	protected List<ServiceEntry> categories;

	@GET
	public Map<String, String> getPreferences(
			@QueryParam("service") String serviceName,
			@QueryParam("address") String addressString) {
		// query db
		SendJournal journal = dao.queryEntity(query, SendJournal.class);
		if (!journal.getDestination().getAddress().equalsIgnoreCase(addressString)){
			throw new WebApplicationException(addressString+" does not match db.",
					Response.Status.BAD_REQUEST);
		}
		// get a list of all categories
		ServiceList sl = new ServiceList(serviceName, categories);
		// filter by users categories
		List<String> categoryFilter = journal.getDestination()
				.getActiveCategoriesWithoutService();
		Map<String, String> rv = sl.getCategories(categoryFilter);
		// TODO: translate
		// TODO: return user's language
		return rv;
	}

	@PUT
	public Representation setPreferences(List<String> newCategories,
			@QueryParam("address") String addressString,
			@QueryParam("service") String serviceName,
			@QueryParam("hash") String hash) {
		MailAddress address = dao.queryEntity(
				new RNQuery().addFilter("hash", hash),SendJournal.class)
				.getDestination();
		if (!address.getAddress().equalsIgnoreCase(addressString)){
			throw new WebApplicationException(addressString+" does not match db.",
					Response.Status.BAD_REQUEST);
		}
		//update categories
		address.updateActiveCategories(newCategories,serviceName);
		return new StringRepresentation(HTML_RESPONSE_DONE,
				org.restlet.data.MediaType.TEXT_HTML);
	}

}
