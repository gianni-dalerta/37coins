package com._37coins.envaya;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import com._37coins.pojo.SendAction;
import com._37coins.sendMail.EmailFactory;
import com._37coins.workflow.pojo.Response;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class MessageFactory {
	
	// where to find the templates when running in web container
	public static final String RESOURCE_PATH = "/WEB-INF/templates/";
	// where to find the templates when outside web container
	public static final String LOCAL_RESOURCE_PATH = "src/main/webapp/WEB-INF/templates/";
	public static final String TEXT_ENDING = ".txt";

	private final Configuration cfg;
	private final ServletContext servletContext;
	private ResourceBundle rb;

	public MessageFactory() {
		this(null);
	}

	public MessageFactory(ServletContext servletContext) {
		cfg = new Configuration();
		this.servletContext = servletContext;
		if (servletContext == null) {
			try {
				cfg.setDirectoryForTemplateLoading(new File(LOCAL_RESOURCE_PATH+"sms/"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			cfg.setServletContextForTemplateLoading(servletContext,
					RESOURCE_PATH+"text/");
		}
	}

	private void prepare(Response rsp) throws MalformedURLException {
		if (null == rsp.getResBundle()) {
			ClassLoader loader = null;
			if (null==servletContext){
				File file = new File(LOCAL_RESOURCE_PATH+"../classes");
				URL[] urls = {file.toURI().toURL()};
				loader = new URLClassLoader(urls);
			}else{
				loader = EmailFactory.class.getClassLoader();
			}
			rb = ResourceBundle.getBundle(rsp.getService(),rsp.getLocale(),loader);
			rsp.setResBundle(new ResourceBundleModel(rb, new BeansWrapper()));
		}
	}
	
	public String construct(Response rsp,
			SendAction sendAction) throws IOException,
			TemplateException {
		
		prepare(rsp);

		Template template = cfg.getTemplate(sendAction
				.getTemplateId(rsp.getAction().getText()) + TEXT_ENDING);

		Writer stringWriter = null;

		stringWriter = new StringWriter();
		template.process(rsp, stringWriter);

		stringWriter.flush();
		return stringWriter.toString();
	}

}
