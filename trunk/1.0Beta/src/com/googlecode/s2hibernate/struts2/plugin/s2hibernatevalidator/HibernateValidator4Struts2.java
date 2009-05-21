package com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.exception.HibernatePluginValidationException;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * Validor class for Hibernate
 * @author José Yoshiriro - jyoshiriro@gmail.com
 *
 */
public class HibernateValidator4Struts2 {

	private static final String CONFIG_ERROR_MESSAGE = "hibernate_validator.error_config_validator";

	static Logger log = Logger.getLogger(HibernateValidator4Struts2.class);
	
	/** 
	 * <p>
	 * Validate the Action. <br/>
	 * Search a class at the same package of the Action, with the same name, and suffixed with <b>"Validator"</b>.<br/>
	 * <i>Example:</i> <br/>
	 * &bull; MyAction.java (Action Class) <br/>
	 * &bull; <b>MyActionValidator.java</b> (Validator Class) <br/>
	 * </p>
	 * <p>
	 * Valida a Action. <br/>
	 * Procura por uma classe no mesmo pacote da Action, com o mesmo nome, mas com o sufixo <b>"Validator"</b>.<br/>
	 * <i>Exemplo:</i> <br/>
	 * &bull; MyAction.java (Classe Action) <br/>
	 * &bull; <b>MyActionValidator.java</b> (Classe de Validação) <br/>
	 * </p>
	 * @param requestParameters 
	 * @param actionToValidate
	 */
	public synchronized static void validate(ActionSupport action, Map requestParameters) {
		
		Struts2ActionHibernateValidator validator = null;
		String classeValidador = action.getClass().getName()+"Validator";
		Method methodValidate = null;
		try {
			validator = (Struts2ActionHibernateValidator) Class.forName(classeValidador).newInstance();
		} catch (Exception e) {
			String messageNoValidator = getMessage("hibernate_validator.no_validation_class_found", action)+classeValidador;
			System.err.println(messageNoValidator);
			return;
		}
		try {
			methodValidate = Class.forName(classeValidador).getDeclaredMethod(ActionContext.getContext().getName());
		} catch (Exception e) {
			String messageNoMethod = getMessage("hibernate_validator.no_validation_method_found", action)+classeValidador+" public void "+ActionContext.getContext().getName()+"()";
			System.err.println(messageNoMethod); 
			return;
		} 
		if (validator!=null) {
			validator.setAction(action);
			try {
				methodValidate.invoke(validator);
			} catch (IllegalArgumentException e) {
				System.err.println(getMessage(CONFIG_ERROR_MESSAGE, action)+e.getMessage());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				System.err.println(getMessage(CONFIG_ERROR_MESSAGE, action)+e.getMessage());
				e.printStackTrace();
			} 
			catch (InvocationTargetException e) {
				Throwable erroOriginal = e.getTargetException(); 
				if ( erroOriginal instanceof HibernatePluginValidationException)
					addActionError((HibernatePluginValidationException) e.getTargetException(), action, requestParameters);
				else 
					if ( erroOriginal instanceof InvalidStateException)
						;
					else {
						System.err.println(getMessage(CONFIG_ERROR_MESSAGE, action)+e.getMessage());
						e.printStackTrace();
					}
			} 
		}
	}

	private static void addActionError(HibernatePluginValidationException erroValidacao, ActionSupport action, Map requestParameters) {
		InvalidValue[] invalidValues = erroValidacao.getInvalidValues();
		for (InvalidValue invalidValue:invalidValues) {
			
			Field[] actionFields = action.getClass().getDeclaredFields();
			// navegando nos campos da Action
			for (Field field: actionFields) {
				try {
					field.setAccessible(true);
					Object valor = field.get(action);
					if (valor!=null) {
						String nomeCampo = "";
						// o campo é da própria Action
						if (invalidValue.getBean().equals(action)) {
							nomeCampo = field.getName();
						}
						// o campo é de um POJO da Action
						else {
							if (invalidValue.getRootBean().equals(valor)) {
								nomeCampo = field.getName()+"."+invalidValue.getPropertyPath();
							}
						}
						// se um dos campos na action é o que contem o erro de validação atual, encerra-se o loop nos campos da action
						if (!nomeCampo.equals("")) {
							String mensagem = action.getText(nomeCampo)+" "+invalidValue.getMessage();
							action.addFieldError(nomeCampo, mensagem);
							System.err.println(getMessage("hibernate_validator.validation_error", action)+mensagem+"\n -> "+action.getClass().getName()+" - "+invalidValue.getBeanClass().getName()+" - "+invalidValue.getPropertyPath());
							break;
						}
					}
				} 
				catch (Exception e) {
					System.err.println(getMessage(CONFIG_ERROR_MESSAGE, action)+e.getMessage());
					e.printStackTrace();
				} 
			}
		}
	}
	
	private static String getMessage(String key, ActionSupport action) {
		String message="";
		if ( (action.getTexts()!=null) && (action.getTexts().getString(key)!=null) )
			message = action.getText(key);
		else {
			ResourceBundle resourceBundle = ResourceBundle.getBundle("com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.resources");
			message = resourceBundle.getString(key);
		}
		return message+": ";
	}

	public static void prepareValidationErrorMessages(InvalidStateException ise, ActionSupport action, Map parameterMap, ValueStack valueStack) {
		
		for (InvalidValue iv : ise.getInvalidValues()) {
			String entityClassName = action.getText(iv.getBean().getClass().getName());
			StringBuilder sb = new StringBuilder(entityClassName);
			sb.append(": ");
			sb.append(iv.getMessage());
			String finalMessage = sb.toString();
			action.addActionError(finalMessage);
			System.out.println("");
			/*Set<String> keys = parameterMap.keySet(); 
			for (String key : keys) {
				Object value = parameterMap.get(key);
				if (value.equals(iv.getValue())) {
					((ActionSupport)action).addFieldError(key, iv.getMessage());
					break;
				}
			}*/
//			System.out.println(iv.getMessage()+" - "+iv.getPropertyName()+" - "+iv.getPropertyPath()+" - "+iv.getBean()+" - "+iv.getRootBean());
		}
	}
	

}
