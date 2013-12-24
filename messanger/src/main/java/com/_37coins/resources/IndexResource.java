package com._37coins.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
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
	final private ServletContext servletContext;
	final private HttpServletRequest httpReq;
	
	@Inject public IndexResource(ServletRequest request,
			MessageFactory htmlFactory, ServletContext servletContext) {
		this.httpReq = (HttpServletRequest)request;
		this.htmlFactory = htmlFactory;
		this.servletContext = servletContext;
	}

	@GET
	public Response index(@HeaderParam("Accept-Language") String lng){
		Map<String,String> data = new HashMap<>();
		data.put("resPath", MessagingServletConfig.resPath);
		data.put("basePath", MessagingServletConfig.basePath);
		data.put("srvcPath", MessagingServletConfig.srvcPath);
		data.put("captchaPubKey", MessagingServletConfig.captchaPubKey);
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
	
	@GET
	@Path("deploy")
	public Response deploy(){
		File jsp = new File(servletContext.getRealPath(httpReq.getServletPath()));
		File dir = jsp.getParentFile();
		File warFile = new File (dir.toString() +  "/ROOT/pwm.war");
		boolean success = warFile.renameTo (new File (dir, warFile.getName ()));
		if (!success) {
			return Response.ok("{\"status\":\"not found. already deployed?\"}", MediaType.APPLICATION_JSON_TYPE).build();
		}else{
			return Response.ok("{\"status\":\"deploying!\"}", MediaType.APPLICATION_JSON_TYPE).build();
		}
	}
	
	/*
	 * ###################### TEST SCOPE
	 */
	
	@GET
	@Path("envayaTest.html")
	public Response envayaTest(){
		Map<String,String> data = new HashMap<>();
		data.put("lng", "en-US");
		DataSet ds = new DataSet()
			.setService("envayaTest.html")
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
	@Path("res/scripts/templates/{name}.htm")
	public Response proxy(@PathParam("name") String name) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader("/Users/johann/37coins/webui/app/scripts/templates/"+name+".htm"));
        try{
	        StringBuilder sb = new StringBuilder();
	        String inputLine;
	        while ((inputLine = br.readLine()) != null)
	        	sb.append(inputLine);
	        return Response.ok(sb.toString(), MediaType.TEXT_HTML_TYPE).build();
        }finally{
        	br.close();
        }
	}
	
}