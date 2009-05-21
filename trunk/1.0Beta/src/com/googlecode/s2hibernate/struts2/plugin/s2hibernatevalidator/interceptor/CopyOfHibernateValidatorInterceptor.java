package com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.interceptor;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;

import com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.HibernateValidator4Struts2;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

@SuppressWarnings("serial")
public class CopyOfHibernateValidatorInterceptor extends AbstractInterceptor{

	static Logger log = Logger.getLogger(HibernateValidatorInterceptor.class);
	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception {
		Object action = invocation.getAction();
		if (!(action instanceof ActionSupport)) {
			log.debug("Hibernate Validation Allowed only in Actions that 'ISA' ActionSupport");
			return invocation.invoke();
		}
		ActionSupport actionAs = (ActionSupport) action;
		HibernateValidator4Struts2.validate(actionAs, ServletActionContext.getRequest().getParameterMap());
		log.debug("Hibernate Validation in "+actionAs.getClass());
		if (actionAs.hasFieldErrors()) {
			log.debug("Hibernate Validation found "+actionAs.getFieldErrors().size()+" validation Erros.");
			return ActionSupport.INPUT;
		}
		else {
			log.debug("Hibernate Validation found no erros.");
			return invocation.invoke();
		}
		
	}

}
