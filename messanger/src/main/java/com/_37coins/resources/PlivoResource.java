package com._37coins.resources;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com._37coins.plivo.GetDigits;
import com._37coins.plivo.Response;
import com._37coins.plivo.Speak;

@Path(PlivoResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
public class PlivoResource {
	public final static String PATH = "/plivo";
	
	
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response getXml(){
		Response rv = null;
		try {
			rv = new Response()
				.setGetDigits(new GetDigits()
					.setAction(new URL("http://api.37coin.com/restplivo"))
					.setMethod("GET")
					.setSpeak(new Speak()
						.setText("test")));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(Response.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	 
			// output pretty printed
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	 
			jaxbMarshaller.marshal(rv, System.out);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return rv;
	}
	
}
