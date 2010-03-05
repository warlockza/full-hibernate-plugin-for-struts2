package com.googlecode.s2hibernate.struts2.plugin.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

import org.apache.log4j.lf5.util.Resource;
import org.hibernate.HibernateException;

import com.googlecode.s2hibernate.lang.SessionInjectionException;

public class TestClass {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String fileProperties = "/teste.properties".substring(1,"/teste.properties".length());
		Resource resource = new Resource(fileProperties);
		if (resource.getURL()!=null) {
			String fullpath = resource.getURL().getPath().replace("%20", " ");
			FileInputStream fis = new FileInputStream(fullpath);
			FileReader reader = new FileReader(fullpath);
			Properties properties = new Properties();
			properties.load(fis);
			System.out.println();
		}
	}

}
