package com.googlecode.s2hibernate.struts2.plugin.test;

import org.hibernate.Session;

import com.googlecode.s2hibernate.struts2.plugin.annotations.SessionTarget;

public class SubSubClasseAnotada {

	@SessionTarget
	Session sessionSubSubClasse;

	public Session getSessionSubSubClasse() {
		return sessionSubSubClasse;
	}

	public void setSessionSubSubClasse(Session sessionSubSubClasse) {
		this.sessionSubSubClasse = sessionSubSubClasse;
	}
}
