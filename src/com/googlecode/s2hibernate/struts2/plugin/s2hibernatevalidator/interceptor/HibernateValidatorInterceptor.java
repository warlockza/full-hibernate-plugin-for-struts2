package com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;

import com.googlecode.s2hibernate.struts2.plugin.interceptors.GenericInterceptor;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

@SuppressWarnings("serial")
public class HibernateValidatorInterceptor extends GenericInterceptor{

	
	private String excludeMethods = "";
	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		String[] excludeMethodsA = excludeMethods.split(",");
		for (String method : excludeMethodsA) {
			if (invocation.getProxy().getMethod().equals(method))
				return invocation.invoke();
		}
		Object action = invocation.getAction();
		Method method = action.getClass().getDeclaredMethod(invocation.getProxy().getMethod(), null);
		
		if (method.isAnnotationPresent(SkipValidation.class))
			return invocation.invoke();
		
		if (!(action instanceof ActionSupport)) {
			log.warn("Full Hibernate Plugin Validation Allowed only in Actions that 'ISA' ActionSupport");
			return invocation.invoke();
		}
		ActionSupport actionAs = (ActionSupport) action;
		log.debug("Full Hibernate Plugin Validation in "+actionAs.getClass());
		
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
			log.debug("Full Hibernate Plugin Validation found no erros.");
			actionAs.validate();
			if (actionAs.hasActionErrors() || actionAs.hasFieldErrors()) {
				log.debug("Full Hibernate Plugin found custom validation errors: "+actionAs.getFieldErrors()+" "+actionAs.getActionErrors());
				return actionAs.input();
			}
			else {
				return invocation.invoke();
			}
		} else {
			for (InvalidValue invalidValue : invalidValuesFromRequest) {
				StringBuilder sbMessage = new StringBuilder(actionAs.getText(invalidValue.getPropertyPath(),""));
				if (sbMessage.length()>0)
					sbMessage.append(" - ");
				sbMessage.append(actionAs.getText(invalidValue.getMessage()));
				actionAs.addFieldError(invalidValue.getPropertyPath(), sbMessage.toString());
			}
			log.debug("Full Hibernate Plugin Validation found "+actionAs.getFieldErrors().size()+" validation Errors.");
			actionAs.validate();
			if (action instanceof Preparable) {
				Method methodPrepare = Preparable.class.getDeclaredMethod("prepare");
				methodPrepare.invoke(action);
			}
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

class PluginValidatorMessages extends PropertyResourceBundle {

	public PluginValidatorMessages(InputStream stream) throws IOException {
		super(stream);
	}

	public void setParent(ResourceBundle defaultResourceBundle) {
		super.setParent(defaultResourceBundle);
	}
	
}