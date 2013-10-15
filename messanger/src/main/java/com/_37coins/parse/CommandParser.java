package com._37coins.parse;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.tuple.Pair;

import com._37coins.workflow.pojo.DataSet.Action;

public class CommandParser {
	public static final String RB_NAME = "37coins";
	public static final List<Action> reqCmdList = Arrays.asList(
			Action.BALANCE,
			Action.DEPOSIT_REQ,
			Action.HELP, 
			Action.TRANSACTION, 
			Action.WITHDRAWAL_CONF, 
			Action.WITHDRAWAL_REQ, 
			Action.WITHDRAWAL_REQ_OTHER);
	
	private Map<String, Pair<Action,Locale>> wordMap = new HashMap<>();

	public CommandParser() {
		this(null);
	}

	public CommandParser(ServletContext sc) {
		// figure out class loader
		File[] files= null;
		ClassLoader loader = null;
		if(null!=sc && !sc.getServerInfo().contains("jetty")){
			Set<String> paths = sc.getResourcePaths("/WEB-INF/classes/");
			files = new File[paths.size()];
			int i = 0;
			for (String path : paths){
				files[i] = new File(path);
				i++;
			}
			loader = CommandParser.class.getClassLoader();
		}else{
			try{
				URL bundle = (null != sc) ? sc.getResource("/WEB-INF/classes/"+RB_NAME+"_en.properties")
						: ClassLoader.getSystemClassLoader().getResource(RB_NAME+"_en.properties");
				File root = new File(bundle.getFile()).getParentFile();
				files = root.listFiles();
				URL[] urls = {root.toURI().toURL()};
				loader = new URLClassLoader(urls);
			}catch(Exception e){}
		}
		// find all available locales
		List<Locale> locales = new ArrayList<>();
		for (File file : files) {
			if (file.getName().matches(".*"+RB_NAME+"_..\\.properties")) {
				int l = file.getName().length();
				String localeString = file.getName().substring(l - 13, l - 11);
				String[] locs = localeString.split("[-_]");
				switch(locs.length){
			        case 2: locales.add(new Locale(locs[0], locs[1])); break;
			        case 3: locales.add(new Locale(locs[0], locs[1], locs[2])); break;
			        default: locales.add(new Locale(locs[0])); break;
			    }
			}
		}
		// create a map of command words to actions and locales
		for (Locale locale : locales) {
			try {
				ResourceBundle rb = ResourceBundle.getBundle(RB_NAME, locale,loader);
				for (Action a : reqCmdList) {
					String cmdList = rb.getString(a.getText() + "Cmd");
					for (String cmd : cmdList.split(",")) {
						wordMap.put(cmd, Pair.of(a, locale));
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private Action replaceCommand(String cmd) {
		for (String pos : wordMap.keySet()) {
			if (cmd.equalsIgnoreCase(pos)) {
				return wordMap.get(pos).getLeft();
			}
		}
		return null;
	}
	
	private Locale readLanguage(String cmd) {
		for (String pos : wordMap.keySet()) {
			if (cmd.equalsIgnoreCase(pos)) {
				return wordMap.get(pos).getRight();
			}
		}
		return null;
	}
	
	public Action processCommand(String msg) {
		msg = msg.trim().replaceAll(" +", " ");
		String[] ca = msg.split(" ");
		return replaceCommand(ca[0]);
	}
	
	public Locale guessLocale(String msg) {
		msg = msg.trim().replaceAll(" +", " ");
		String[] ca = msg.split(" ");
		return readLanguage(ca[0]);
	}

	
}
