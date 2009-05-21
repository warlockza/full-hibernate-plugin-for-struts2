package com.googlecode.s2hibernate.struts2.plugin.s2hibernatesession;

import com.googlecode.s2hibernate.struts2.plugin.util.Constants;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.InvalidAttributeValueException;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.validator.InvalidStateException;

import com.googlecode.s2hibernate.struts2.plugin.util.HibernateSessionFactory;
import com.googlecode.s2hibernate.struts2.plugin.util.HibernateSessionPluginUtils;
import com.googlecode.s2hibernate.struts2.plugin.util.SessionInfo;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;


/**
 * Interceptor for Hibernate Session and Transaction Injection
 *
 * <br/><br/>
 * 
 * Interceptor para injeção da Sessão Hibernate e da Transação
 *
 * @author Jose Yoshiriro - jyoshiriro@gmail.com
 *
 */

public class HibernateSessionInterceptor extends AbstractInterceptor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2222692750520221708L;

	static Logger log = Logger.getLogger(HibernateSessionInterceptor.class);
	
	private String sessionTarget;
	
	private String transactionTarget;
	
	private String customSessionFactoryClass;
	
	private String getSessionMethod;
	
	private String staticGetSessionMethod = "true";
	
	private String closeSessionAfterInvoke = "true";

	private String configurationFiles = HibernateSessionFactory.DEFAULT_HIBERATE_CONFIGFILE;	
	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		
		Object action = invocation.getAction();
		
		if (action.getClass().equals(ActionSupport.class))
			return invocation.invoke();
		
		if (sessionTarget==null || sessionTarget.equals("")) {
			throw new ConfigurationException("No target setted for Hibernate Session object. Use the *"+Constants.HIBERNATEPLUGIN_SESSIONTARGET+"* property.");
		}
		if (transactionTarget==null || transactionTarget.equals("")) {
			throw new ConfigurationException("No target setted for Hibernate Transaction object. Use the *"+Constants.HIBERNATEPLUGIN_TRANSACTIONTARGET+"* property.");
		}
		
		HibernateSessionTransactionInfo sessionInfo = new HibernateSessionTransactionInfo();
		HibernateSessionTransactionInfo transactionInfo = new HibernateSessionTransactionInfo();
		
		List<Field> fields = HibernateSessionPluginUtils.getFieldsFromAction(invocation.getAction());
		
		StringBuilder sbErrorMessage = new StringBuilder("Error! Please, check your JDBC/JDNI Configurations and Database Server avaliability. \n ");
		
		try {
			sessionInfo = putHibernateSessionInStack(fields, invocation);
		} catch (Exception e) {
			sbErrorMessage.append("Could not open or put a Hibernate Session in ValueStack: ");
			sbErrorMessage.append(e.getMessage());
			String message = sbErrorMessage.toString();
			System.err.println(message);
			e.printStackTrace();
			throw new SessionException(message);
		}
		
		try {
			transactionInfo = putHibernateTransactionInStack(fields, sessionInfo.getSessionObject(), invocation);
		} catch (Exception e) {
			sbErrorMessage.append("Could not open or put a Hibernate Transaction in ValueStack: ");
			sbErrorMessage.append(e.getMessage());
			String message = sbErrorMessage.toString();
			System.err.println(message);
			e.printStackTrace();
			throw new TransactionException(message);
		}
		
		String returnName = invocation.invoke();

		Session hibernateSession = sessionInfo.getSessionObject();
		Transaction hibernateTransation = transactionInfo.getTransactionObject();
		
		//TODO: Test if the transaction has any Queue. This method gone to all methods, except for the *merge* method
		/*Boolean sessionWithQueue = ((SessionImpl)hibernateSession).getActionQueue().hasAnyQueuedActions();
		
		if (sessionWithQueue)
			hibernateTransation.begin();
			*/

		if ((hibernateTransation.isActive()) && (!hibernateTransation.wasRolledBack())) {
			try {
				hibernateSession.flush();
				hibernateTransation.commit();
			} 
			catch (InvalidStateException e) {
				// Erros de Validação Hibernate em caso de uso de Event Listeners
				hibernateSession.close();
				InvalidStateException ise = (InvalidStateException)e;
//				HibernateValidator4Struts2.prepareValidationErrorMessages(ise, (ActionSupport)invocation.getAction(),ServletActionContext.getRequest().getParameterMap(), invocation.getStack());
				if (action instanceof ActionSupport)
					return ((ActionSupport)action).input();
				else
					return ActionSupport.INPUT;
			}
			catch (HibernateException e) {
				sbErrorMessage.append("Could not commit the Hibernate Transaction: ");
				sbErrorMessage.append(e.getMessage());
				String message = sbErrorMessage.toString();
				System.err.println(message);
				e.printStackTrace();
				throw new HibernateException(message);
			}
		}

		String hibernateSessionTrueTarget = sessionInfo.getSessionTarget();

		
		if (isCloseSessionAfterInvoke()) {
			hibernateSession.close();
			hibernateSession=null;
			invocation.getStack().setValue(hibernateSessionTrueTarget, null);
		} else {
			//TODO: verificar se o Hibernate Manager está Habilitado antes de por as sessões no contexto de aplicação para economizar memória!
			Set<SessionInfo> hibernateSessions = (Set<SessionInfo>) ActionContext.getContext().getApplication().get("struts2HibernatePlugin_Sessions");
			if (hibernateSessions==null)
				hibernateSessions = new LinkedHashSet<SessionInfo>();
			SessionInfo info = new SessionInfo(hibernateSession,new Date(),ServletActionContext.getRequest().getSession());
			hibernateSessions.add(info);
			ActionContext.getContext().getApplication().put("struts2HibernatePlugin_Sessions", hibernateSessions);
		}
		
		return returnName ;
	}

	private HibernateSessionTransactionInfo putHibernateTransactionInStack(List<Field> fields,
			Session hibernateSession, ActionInvocation invocation) throws Exception {

		Boolean targetFound = false;
		String trueTarget = "";
		Transaction transaction = null;
		for (Field field:fields) {
			String[] targetAsArray = transactionTarget.replace(".", ",").split(",");
			
			if (!Pattern.matches(HibernateSessionPluginUtils.wildcardToRegex(targetAsArray[0]), field.getName())) 
				continue;
			
			targetAsArray[0] = field.getName();
			trueTarget = Arrays.toString(targetAsArray).replace("[", "").replace("]", "").replace(", ",".");

			if (transaction==null)
				//TODO: Look for the last TODO item. When finish this, change this line to *getTransaction()* method.
				transaction = hibernateSession.beginTransaction();
			
			targetFound = true;
			invocation.getStack().setValue(trueTarget, transaction);
		}
		if (!targetFound) {
			throw new Exception("Target \""+transactionTarget+"\" not found in Value Stack ("+invocation.getAction().getClass().getName()+") for Hibernate Session.");
		}
		HibernateSessionTransactionInfo transactionInfo = new HibernateSessionTransactionInfo();
		transactionInfo.setTransactionObject(transaction);
		transactionInfo.setTransactionTarget(trueTarget);
		return transactionInfo;
	}

	private HibernateSessionTransactionInfo putHibernateSessionInStack(List<Field> fields, ActionInvocation invocation) throws Exception {

		Boolean targetFound = false;
		String trueTarget = "";
		Session hibernateSession = null;
		for (Field field:fields) {
			String[] targetAsArray = sessionTarget.replace(".", ",").split(",");
			
			if (!Pattern.matches(HibernateSessionPluginUtils.wildcardToRegex(targetAsArray[0]), field.getName())) 
				continue;
			
			targetAsArray[0] = field.getName();
			trueTarget = Arrays.toString(targetAsArray).replace("[", "").replace("]", "").replace(", ",".");

			if (hibernateSession==null) {
				// Using the PLUGIN Session Factory Class
				if ( (customSessionFactoryClass==null) || (customSessionFactoryClass.equalsIgnoreCase("plugin")) ) {
					if (HibernateSessionFactory.getSessionFactory()==null) {
						HibernateSessionFactory.setConfigFiles(configurationFiles);
						HibernateSessionFactory.rebuildSessionFactory();
					}
					hibernateSession = HibernateSessionFactory.getSession();
				} 
				else {
				// Using a custom Session Factory Class
					Object sessionFactory = null;
					if (isStaticGetSessionMethod())
						sessionFactory = Class.forName(customSessionFactoryClass, false, this.getClass().getClassLoader());
					else
						sessionFactory = Class.forName(customSessionFactoryClass).newInstance();				
					Method method = Class.forName(customSessionFactoryClass).getDeclaredMethod(getSessionMethod, null);
					hibernateSession = (Session) method.invoke(sessionFactory, null);
				}
			}
			targetFound = true;
			invocation.getStack().setValue(trueTarget, hibernateSession);
		}
		if (!targetFound) {
			throw new NoSuchFieldException("Target \""+sessionTarget+"\" not found in Value Stack ("+invocation.getAction().getClass().getName()+") for Hibernate Session.");
		}
		HibernateSessionTransactionInfo sessionInfo = new HibernateSessionTransactionInfo();
		sessionInfo.setSessionObject(hibernateSession);
		sessionInfo.setSessionTarget(trueTarget);
		return sessionInfo;
	}

	public String getSessionTarget() {
		return sessionTarget;
	}

	@Inject(value=Constants.HIBERNATEPLUGIN_SESSIONTARGET, required=false)
	public void setSessionTarget(String sessionTarget) {
		this.sessionTarget = sessionTarget;
	}

	public String getCustomSessionFactoryClass() {
		return customSessionFactoryClass;
	}
	
	@Inject(value=Constants.HIBERNATEPLUGIN_CUSTOMSESSIONFACTORYCLASS, required=false)
	public void setCustomSessionFactoryClass(String customSessionFactoryClass) {
		this.customSessionFactoryClass = customSessionFactoryClass;
	}

	public String getGetSessionMethod() {
		return getSessionMethod;
	}

	@Inject(value=Constants.HIBERNATEPLUGIN_GETSESSIONMETHOD, required=false)
	public void setGetSessionMethod(String getSessionMethod) {
		this.getSessionMethod = getSessionMethod;
	}

	public String getCloseSessionAfterInvoke() {
		return closeSessionAfterInvoke;
	}
	
	public Boolean isCloseSessionAfterInvoke() {
		Boolean resultado = new Boolean(closeSessionAfterInvoke); 
		return resultado;
	}

	@Inject(value=Constants.HIBERNATEPLUGIN_CLOSESESSIONAFTERINVOKE,required=false)
	public void setCloseSessionAfterInvoke(String closeSessionAfterInvoke) {
		this.closeSessionAfterInvoke = closeSessionAfterInvoke;
	}

	public String getStaticGetSessionMethod() {
		return staticGetSessionMethod;
	}
	
	public Boolean isStaticGetSessionMethod() {
		Boolean resultado = new Boolean(staticGetSessionMethod); 
		return resultado;
	}

	@Inject(value=Constants.HIBERNATEPLUGIN_STATICGETSESSIONMETHOD,required=false)
	public void setStaticGetSessionMethod(String staticGetSessionMethod) {
		this.staticGetSessionMethod = staticGetSessionMethod;
	}
	
	
	public String[] getConfigurationFiles() {
		if (configurationFiles==null)
			configurationFiles="";
		return configurationFiles.split(",");
	}
	
	@Inject(value=Constants.HIBERNATEPLUGIN_CONFIGURATIONFILES, required=false)
	public void setConfigurationFiles(String configurationFiles) {
		this.configurationFiles = configurationFiles;
	}

	@Inject(value=Constants.HIBERNATEPLUGIN_TRANSACTIONTARGET, required=false)
	public void setTransactionTarget(String transactionTarget) {
		this.transactionTarget = transactionTarget;
	}

	@Inject(value=Constants.HIBERNATEPLUGIN_CONFIGURATIONTYPE, required=false)
	public void setConfigurationType(String configurationType) throws InvalidAttributeValueException {
		HibernateSessionFactory.setConfigurationType(configurationType);
	}

}

class HibernateSessionTransactionInfo {
	Session sessionObject;
	String sessionTarget;

	Transaction transactionObject;
	String transactionTarget;
	
	public Session getSessionObject() {
		return sessionObject;
	}
	public void setSessionObject(Session sessionObject) {
		this.sessionObject = sessionObject;
	}
	public String getSessionTarget() {
		return sessionTarget;
	}
	public void setSessionTarget(String sessionTarget) {
		this.sessionTarget = sessionTarget;
	}
	public Transaction getTransactionObject() {
		return transactionObject;
	}
	public void setTransactionObject(Transaction transactionObject) {
		this.transactionObject = transactionObject;
	}
	public String getTransactionTarget() {
		return transactionTarget;
	}
	public void setTransactionTarget(String transactionTarget) {
		this.transactionTarget = transactionTarget;
	}
}