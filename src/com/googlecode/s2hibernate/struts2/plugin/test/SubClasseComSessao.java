package com.googlecode.s2hibernate.struts2.plugin.test;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.googlecode.s2hibernate.struts2.plugin.annotations.TransactionTarget;

public class SubClasseComSessao {

//	@SessionTarget
	Session sessaoSubClasse;
	
	@TransactionTarget
	private Transaction transacaoSubClasse;
	
	private SubSubClasseAnotada subsubclasseanotada;

	public Session getSessaoSubClasse() {
		return sessaoSubClasse;
	}

	public void setSessaoSubClasse(Session sessaoSubClasse) {
		this.sessaoSubClasse = sessaoSubClasse;
	}
	
	public void teste() {
		System.out.println(sessaoSubClasse);
	}

	public Transaction getTransacaoSubClasse() {
		return transacaoSubClasse;
	}

	public void setTransacaoSubClasse(Transaction transacaoSubClasse) {
		this.transacaoSubClasse = transacaoSubClasse;
	}

	public SubSubClasseAnotada getSubsubclasseanotada() {
		return subsubclasseanotada;
	}

	public void setSubsubclasseanotada(SubSubClasseAnotada subsubclasseanotada) {
		this.subsubclasseanotada = subsubclasseanotada;
	}

	
}
