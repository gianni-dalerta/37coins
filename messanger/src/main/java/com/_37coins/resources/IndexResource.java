package com._37coins.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com._37coins.MessageFactory;
import com._37coins.MessagingServletConfig;
import com._37coins.workflow.pojo.DataSet;

import freemarker.template.TemplateException;

@Path(IndexResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class IndexResource {
	public final static String PATH = "/";

	final private MessageFactory htmlFactory;
	
	@Inject public IndexResource(ServletRequest request,
			MessageFactory htmlFactory) {
		this.htmlFactory = htmlFactory;
	}

	@GET
	public Response index(@HeaderParam("Accept-Language") String lng){
		Map<String,String> data = new HashMap<>();
		data.put("resPath", MessagingServletConfig.resPath);
		data.put("basePath", MessagingServletConfig.basePath);
		data.put("lng", (lng!=null)?lng.split(",")[0]:"en-US");
		DataSet ds = new DataSet()
			.setService("index.html")
			.setPayload(data);
		String rsp;
		try {
			rsp = htmlFactory.processTemplate(ds, null);
		} catch (IOException | TemplateException e) {
			throw new WebApplicationException("template not loaded",
					javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
		return Response.ok(rsp, MediaType.TEXT_HTML_TYPE).build();
	}
	
	@GET
	@Path("index.html")
	public Response fullindex(@HeaderParam("Accept-Language") String lng){
		return index(lng);
	}
	
	@GET
	@Path("res/scripts/templates/{name}.htm")
	public Response proxy(@PathParam("name") String name) throws IOException{
		URL oracle = new URL(MessagingServletConfig.resPath+"scripts/templates/"+name+".htm");
        BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
        try{
	        StringBuilder sb = new StringBuilder();
	        String inputLine;
	        while ((inputLine = in.readLine()) != null)
	        	sb.append(inputLine);
	        return Response.ok(sb.toString(), MediaType.TEXT_HTML_TYPE).build();
        }finally{
        	in.close();
        }
	}
	
	@GET
	@Path("res/locales/{language}/{namespace}.json")
	public Response lngProky(
			@PathParam("language") String language,
			@PathParam("namespace") String namespace) throws IOException{
		String rsp;
		try {
			rsp = htmlFactory.constructJson(
					new DataSet().setLocale(new Locale(language)), 
					namespace+".json");
		} catch (IOException | TemplateException e) {
			e.printStackTrace();
			throw new WebApplicationException("template not loaded",
					javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
		return Response.ok(rsp, MediaType.APPLICATION_JSON_TYPE).build();
	}
	
}