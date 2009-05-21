package com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator;


import org.apache.log4j.Logger;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;

import com.googlecode.s2hibernate.struts2.plugin.s2hibernatevalidator.exception.HibernatePluginValidationException;
import com.opensymphony.xwork2.ActionSupport;

/**
 * <p>
 * Validador abstrato para uso do framework Hibernate Validator no Struts 2 <br/>
 * <p>
 * Os métodos de suas classes concretas que devem ser <i>public void</i> e possuir o mesmo nome do mapeamento. <br/>
 * <i>Exemplo: 
 * 	<br/>Mapeamento <b>salvarUsuario.action</b> </i> <br/>
 *  Método de validação na classe: <code> public void salvarUsuario() </code>
 * </p>
 *  
 * <p>
 * Abstract Validator for Hibernate Validator Framework in Struts 2 Actions.
 * <p>
 * The validation methods in concret classes may be <i>public void</i> and have the same name of the mapping. <br/>
 * <i>Example: 
 * 	<br/>Mapping <b>saveUser.action</b> </i> <br/>
 *  Validation method: <code> public void saveUser() </code>
 * </p>
 * 
 * @author José Yoshiriro - jyoshiriro@gmail.com
 *
 * @param <A> 
 * Tipo da Action <br/>
 * Action Type
 */
public abstract class Struts2ActionHibernateValidator<A extends ActionSupport> {
	
	static Logger log = Logger.getLogger(Struts2ActionHibernateValidator.class);
	
	protected A action;

	/**
	 * Validar todos os campos da Action. <br/>
	 * Validate all Action fields.
	 * @throws Exception 
	 */
	protected void validate() {
		validate(action);
	}
	
	/**
	 * Validar um único campo da Action. <br/>
	 * Validate one Action field.
	 * @param campos
	 * @throws Exception 
	 */
	protected void validate(String fieldName)  {
		validate(action, fieldName);
	}
	
	/**
	 * Validar varios campos da Action. <br/>
	 * Validate some Action fields.
	 * @param fieldNames
	 * @throws Exception 
	 */
	protected void validate(String... fieldNames)  {
		validate(action, fieldNames);
	}

	/**
	 * Validar um POJO da Action. <br/>
	 * Validate a POJO (Action field) as all.
	 * @param entity
	 * @throws InvalidStateException
	 */
	@SuppressWarnings("unchecked")
	protected void validate(Object entity) throws InvalidStateException {
		ClassValidator classValidator = new ClassValidator(entity.getClass());
		classValidator.assertValid(entity);
	}

	
	/**
	 * Validar um ou mais campos de um POJO de entidade da Action. <br/>
	 * Validate one or more fields by Entity POJO (Action field).
	 * @param entity
	 * @param fieldNames
	 * @throws HibernatePluginValidationException
	 */
	@SuppressWarnings("unchecked")
	protected void validate(Object entity, String... fieldNames) throws HibernatePluginValidationException  {
		ClassValidator classValidator = new ClassValidator(entity.getClass());
		if (!classValidator.hasValidationRules()) {
			System.err.println(entity.getClass().getName()+" has no Hibernate Validation annotation");
		}
		for (String fieldName : fieldNames) {
			String trueFieldName = fieldName;
			
			if (!entity.getClass().equals(action.getClass())) 
				trueFieldName = fieldName.substring(fieldName.lastIndexOf(".")+1, fieldName.length());
			
			InvalidValue[] ivs = classValidator.getInvalidValues(entity,trueFieldName);
			if (ivs.length>0) {
//				throw new HibernatePluginValidationException(ivs, fieldName);
				throw new RuntimeException("okoko");
			}			
		}
	}

	public A getAction() {
		return action;
	}

	public void setAction(A action) {
		this.action = action;
	}
	
}
