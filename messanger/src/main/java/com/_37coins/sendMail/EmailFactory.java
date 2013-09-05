package com._37coins.sendMail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import com._37coins.pojo.SendAction;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.ResourceBundleModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * the email factory creates text artifacts from templates and data
 * 
 * @author Johann Barbie
 * 
 */
public class EmailFactory {
	// where to find the templates when running in web container
	public static final String RESOURCE_PATH = "/WEB-INF/templates/";
	// where to find the templates when outside web container
	public static final String LOCAL_RESOURCE_PATH = "src/main/webapp/WEB-INF/templates/";
	public static final String CT_TEXT_HTML = "text/html";
	public static final String CT_PLAIN_TEXT = "text/plain";
	public static final String TEXT_ENDING = ".txt";
	public static final String HTML_ENDING = ".html";

	private final Configuration cfg;
	private final ServletContext servletContext;
	private ResourceBundle rb;

	public EmailFactory() {
		this(null);
	}

	public EmailFactory(ServletContext servletContext) {
		cfg = new Configuration();
		this.servletContext = servletContext;
		if (servletContext == null) {
			try {
				cfg.setDirectoryForTemplateLoading(new File(LOCAL_RESOURCE_PATH));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			cfg.setServletContextForTemplateLoading(servletContext,
					RESOURCE_PATH);
		}
	}

	private void prepare(Map<String, Object> data) throws MalformedURLException {
		if (null == data.get("msg")) {
			ClassLoader loader = null;
			if (null==servletContext){
				File file = new File(LOCAL_RESOURCE_PATH+"../classes");
				URL[] urls = {file.toURI().toURL()};
				loader = new URLClassLoader(urls);
			}else{
				loader = EmailFactory.class.getClassLoader();
			}
			if (data.get("locale") instanceof String){
				data.put("locale", new Locale((String)data.get("locale")));
			}
			rb = ResourceBundle.
					getBundle((String)data.get("service"),(Locale) data.get("locale"),loader);
			data.put("msg", new ResourceBundleModel(rb, new BeansWrapper()));
		}
	}

	public String constructHtml(Map<String, Object> data, SendAction sendAction)
			throws IOException, TemplateException {
		prepare(data);
		return processTemplate(data, sendAction, HTML_ENDING);
	}

	public String constructTxt(Map<String, Object> data, SendAction sendAction)
			throws IOException, TemplateException {
		prepare(data);
		return processTemplate(data, sendAction, TEXT_ENDING);
	}

	public String constructSubject(Map<String, Object> data, SendAction sendAction) throws IOException, TemplateException {
		prepare(data);
		String subjectPrefix= sendAction.getTemplateId((String)data.get("action"));
		Template template = new Template("name", rb.getString(subjectPrefix+"Subject"),new Configuration()); 
		Writer out = new StringWriter(); 
		template.process(data, out); 
		return out.toString();
	}

	public String processTemplate(Map<String, Object> data,
			SendAction sendAction, String fileEnding) throws IOException,
			TemplateException {

		// make email template
		Template template = cfg.getTemplate(sendAction
				.getTemplateId((String) data.get("action")) + fileEnding);

		Writer stringWriter = null;

		// create html mail part
		stringWriter = new StringWriter();
		template.process(data, stringWriter);

		stringWriter.flush();
		return stringWriter.toString();
	}

}
