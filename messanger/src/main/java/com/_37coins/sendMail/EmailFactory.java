package com._37coins.sendMail;

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
import com._37coins.workflow.pojo.Response;

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
	public static final String TEXT_FOLDER = "text/";
	public static final String HTML_FOLDER = "html/";

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
			rb = ResourceBundle.
					getBundle(rsp.getService(),rsp.getLocale(),loader);
			rsp.setResBundle(new ResourceBundleModel(rb, new BeansWrapper()));
		}
	}

	public String constructHtml(Response rsp, SendAction sendAction)
			throws IOException, TemplateException {
		prepare(rsp);
		return processTemplate(rsp, sendAction, HTML_FOLDER);
	}

	public String constructTxt(Response rsp, SendAction sendAction)
			throws IOException, TemplateException {
		prepare(rsp);
		return processTemplate(rsp, sendAction, TEXT_FOLDER);
	}

	public String constructSubject(Response rsp, SendAction sendAction) throws IOException, TemplateException {
		prepare(rsp);
		String subjectPrefix= sendAction.getTemplateId(rsp.getAction().getText());
		Template template = new Template("name", rb.getString(subjectPrefix+"Subject"),new Configuration()); 
		Writer out = new StringWriter(); 
		template.process(rsp, out); 
		return out.toString();
	}

	public String processTemplate(Response rsp,
			SendAction sendAction, String folder) throws IOException,
			TemplateException {

		// make email template
		Template template = cfg.getTemplate(folder + sendAction
				.getTemplateId(rsp.getAction().getText()) + ((folder.contains("html"))?".html":".txt"));

		Writer stringWriter = null;

		// create html mail part
		stringWriter = new StringWriter();
		template.process(rsp, stringWriter);

		stringWriter.flush();
		return stringWriter.toString();
	}

}
