package com._37coins;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.inject.servlet.GuiceFilter;

public class EmbeddedJetty {

    private Server server;
    
    public String setInitParam(ServletHolder holder){
    	holder.setInitParameter("javax.ws.rs.Application", "org.restnucleus.RestNucleusApplication");
    	return "src/main/webapp";
    }

    public void start() throws Exception {

        server = new Server(8087);

        WebAppContext bb = new WebAppContext();
        bb.setServer(server);

        bb.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));

        ServletHolder holder = bb.addServlet(ServletContainer.class, "/*");

        bb.addServlet(holder, "/*");
        bb.setContextPath("/");
        bb.setWar(setInitParam(holder));

        server.setHandler(bb);
        
        System.out.println(">>> STARTING EMBEDDED JETTY SERVER");
        server.start();
    }
    
    public void stop() throws Exception{
        server.stop();
    }
    
    public URI getBaseUri(){
        try {
			return new URI("http://localhost:8087");
		} catch (URISyntaxException e) {
			return null;
		}
    }
    
}