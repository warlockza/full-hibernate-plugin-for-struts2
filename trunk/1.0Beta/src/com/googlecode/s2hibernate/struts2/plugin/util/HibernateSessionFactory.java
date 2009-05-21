package com.googlecode.s2hibernate.struts2.plugin.util;

import java.util.List;

import javax.management.InvalidAttributeValueException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;


/**
 * Simple Hibernate Core Session Factory for Web Applications.
 * @author José Yoshiriro - jyoshiriro@gmail.com
 *
 */
public class HibernateSessionFactory {

    /**
     * '/hibernate.cfg.xml' file
     */
    public static final String DEFAULT_HIBERATE_CONFIGFILE = "/hibernate.cfg.xml";
    
    public static final String XML_TYPE = "xml";
    public static final String ANNOTATION_TYPE = "annotation";
    
    private static String configurationType = ANNOTATION_TYPE;

	private static Configuration xmlConfiguration = null;    
	private static AnnotationConfiguration annotationConfiguration = null;
	
    private static SessionFactory sessionFactory;
    private static String configFiles=DEFAULT_HIBERATE_CONFIGFILE;

    private HibernateSessionFactory() {
    }
	
    
    /**
     * Returns a new Hibernate Session
     * @return A new Hibernate Session
     * @throws Exception
     */
    public static Session getSession() throws Exception {
        Session session = null;
		if (sessionFactory == null) {
			rebuildSessionFactory();
		}
		session = sessionFactory.openSession();

        return session;
    }

	/**
	 * Reload the session factory configurations according the configuration files
	 * @throws Exception
	 */
	public static void rebuildSessionFactory() throws Exception {
		try {
			if (isAnnotationConfiguration())
				annotationConfiguration = new AnnotationConfiguration();
			else
				xmlConfiguration = new Configuration();
			String[] files = configFiles.split(",");
			if (files.length==0)
				throw new Exception("No configuration file for Hibernate");
			for (String file:files) {
				if (isAnnotationConfiguration())
					annotationConfiguration.configure(file);
				else
					xmlConfiguration.configure(file);
			}
			if (sessionFactory!=null) {
				if (sessionFactory.isClosed()) {
					sessionFactory.close();
				}
				sessionFactory=null;
			}
			if (isAnnotationConfiguration())
				sessionFactory = annotationConfiguration.buildSessionFactory();
			else
				sessionFactory = xmlConfiguration.buildSessionFactory();
			
			Session testSession = sessionFactory.openSession();
			List testList = testSession.createSQLQuery("select 1").list();
			testSession.close();
				
		} catch (Exception e) {
			sessionFactory=null;
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * In Web Applications with Hibernate, when the context is reload, it's necessary to destroy the session factory to evict problems with connection pools
	 */
	public static void destroyFactory() {
		if (sessionFactory!=null)
			sessionFactory.close();
		configFiles="";
		sessionFactory = null;
	}

	public static void setConfigFiles(String configFiles) {
		if ((configFiles!=null) && (!configFiles.equals(""))) {
			HibernateSessionFactory.configFiles = configFiles;			
		}
		sessionFactory = null;
	}


	public static String getConfigFiles() {
		return configFiles;
	}


	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}


	public static String getConfigurationType() {
		return configurationType;
	}


	/**
	 * Set the configuration type
	 * @param configurationType use values <b>xml</b> or <b>annotation</b><br/>
	 * You can use <b>HibernateSessionFactory.ANNOTATION_TYPE</b> or <b>HibernateSessionFactory.XML_TYPE</b> instead
	 * @throws InvalidAttributeValueException 
	 * 
	 */
	public static void setConfigurationType(String configurationType) throws InvalidAttributeValueException {
		if ((configurationType==null) || (configurationType.equals("")) )
			configurationType=XML_TYPE;
		if ( 
			// prevent annotationS
			(!configurationType.toLowerCase().startsWith(ANNOTATION_TYPE))
			&&
			(!configurationType.equalsIgnoreCase(XML_TYPE))
		) {
			String message = "Invalid value for Hibernate Core Session Factory: \""+configurationType+"\". Valid configuration types for configurationType are \"xml\" and \"annotation\". The default is \"xml\"";
			message+=". Check your \""+Constants.HIBERNATEPLUGIN_CONFIGURATIONTYPE+"\" configuration property";
			System.err.println(message);
			throw new InvalidAttributeValueException(message);
		}
		HibernateSessionFactory.configurationType = configurationType;
	}

	private static boolean isAnnotationConfiguration() {
		if (configurationType==null)
			configurationType=ANNOTATION_TYPE;
		return configurationType.equalsIgnoreCase(ANNOTATION_TYPE);
	}
}