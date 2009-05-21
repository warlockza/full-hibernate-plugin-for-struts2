package com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.interceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

@SuppressWarnings("serial")
public class HibernateValidatorInterceptor extends AbstractInterceptor{

	static Logger log = Logger.getLogger(HibernateValidatorInterceptor.class);
	
	private String excludeMethods = "";
	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		String[] excludeMethodsA = excludeMethods.split(",");
		for (String method : excludeMethodsA) {
			if (invocation.getProxy().getMethod().equals(method))
				return invocation.invoke();
		}
		Object action = invocation.getAction();
		if (!(action instanceof ActionSupport)) {
			log.debug("Hibernate Validation Allowed only in Actions that 'ISA' ActionSupport");
			return invocation.invoke();
		}
		ActionSupport actionAs = (ActionSupport) action;
		log.debug("Hibernate Validation in "+actionAs.getClass());
		
		Locale clientLocale = actionAs.getLocale();
		
		ResourceBundle clientResourceBundle = ResourceBundle.getBundle("org.hibernate.validator.resources.DefaultValidatorMessages", clientLocale, this.getClass().getClassLoader());
		InputStream stream = getClass().getResourceAsStream("/ValidatorMessages.properties");
		ClassValidator actionValidator = null;
		if (stream!=null) {
			PluginValidatorMessages validatorMessages = new PluginValidatorMessages(stream);
			validatorMessages.setParent(clientResourceBundle);
			actionValidator = new ClassValidator(action.getClass(),validatorMessages);
		} else {
			actionValidator = new ClassValidator(action.getClass(),clientResourceBundle);
		}
//		ClassValidator actionValidator = new ClassValidator(action.getClass(),clientResourceBundle);
		//TODO: usar as mensagens do ValidatorMessages.properties como indica a documentação do Hibernate Validator
		//TODO: usar as mensagens da Action caso as chaves estejam lá. Do jeito que está até pega a msg da chave, mas não usa os parametros de validação das anotações do Hib. Validator
		// PARECE QUE devo procurar uma class que "concatene" 2 ResourceBundle
	/*	ClassValidator actionValidator = null;
		try {
			ResourceBundle customResourceBundle = ResourceBundle.getBundle("ValidatorMessages", clientLocale, this.getClass().getClassLoader());
			DefaultMessageInterpolatorAggregator aggerator = new DefaultMessageInterpolatorAggregator();
			aggerator.initialize(customResourceBundle, clientResourceBundle);
			actionValidator = new ClassValidator(action.getClass(),aggerator);
		} catch (MissingResourceException e) {
			actionValidator = new ClassValidator(action.getClass(),clientResourceBundle);
		}*/
		
		// take all errors but discard when the field do not came from the request
		// Only the first validation error by field is used.
		InvalidValue[] invalidValues = actionValidator.getInvalidValues(action);
		List<String> invalidFieldNames = new ArrayList<String>();
		List<InvalidValue> invalidValuesFromRequest = new ArrayList<InvalidValue>();
		Map parameters = ActionContext.getContext().getParameters();
		for (InvalidValue invalidValue : invalidValues) {
			String fieldFullName = invalidValue.getPropertyPath();
			if (invalidFieldNames.contains(fieldFullName))
				continue;
			if (parameters.containsKey(fieldFullName)) {
				invalidValuesFromRequest.add(invalidValue);
				invalidFieldNames.add(fieldFullName);
			}
		}
		invalidFieldNames.clear();
		invalidFieldNames=null;
		invalidValues=null;
		actionValidator=null;
		if (invalidValuesFromRequest.isEmpty()) {
			log.debug("Hibernate Validation found no erros.");
			return invocation.invoke();			
		} else {
			for (InvalidValue invalidValue : invalidValuesFromRequest) {
				StringBuilder sbMessage = new StringBuilder(actionAs.getText(invalidValue.getPropertyPath(),""));
				if (sbMessage.length()>0)
					sbMessage.append(" - ");
				sbMessage.append(actionAs.getText(invalidValue.getMessage()));
				actionAs.addFieldError(invalidValue.getPropertyPath(), sbMessage.toString());
			}
			log.debug("Hibernate Validation found "+actionAs.getFieldErrors().size()+" validation Erros.");
			return actionAs.input();
		}

	}

	public String getExcludeMethods() {
		return excludeMethods;
	}

	public void setExcludeMethods(String excludeMethods) {
		this.excludeMethods = excludeMethods;
	}

}

// TODO: Ainda não usada
class PluginValidatorMessages extends PropertyResourceBundle {

	public PluginValidatorMessages(InputStream stream) throws IOException {
		super(stream);
	}

	public void setParent(ResourceBundle defaultResourceBundle) {
		super.setParent(defaultResourceBundle);
	}
	
}