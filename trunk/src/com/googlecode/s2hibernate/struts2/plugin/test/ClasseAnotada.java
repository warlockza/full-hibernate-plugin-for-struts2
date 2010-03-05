package com.googlecode.s2hibernate.struts2.plugin.test;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.googlecode.s2hibernate.struts2.plugin.annotations.TransactionTarget;

public class ClasseAnotada {

	SubClasseComSessao subClasseComSessao = new SubClasseComSessao();
			
//	@SessionTarget
	private Session sessaoClasse;
	
	@TransactionTarget
	private Transaction transacaoClasse;

	public SubClasseComSessao getSubClasseComSessao() {
		return subClasseComSessao;
	}

	public void setSubClasseComSessao(SubClasseComSessao subClasseComSessao) {
		this.subClasseComSessao = subClasseComSessao;
	}
	
	public void teste() {
		System.out.println(subClasseComSessao.getSessaoSubClasse());
	}

	public Session getSessaoClasse() {
		return sessaoClasse;
	}

	public void setSessaoClasse(Session sessaoClasse) {
		this.sessaoClasse = sessaoClasse;
	}

	public Transaction getTransacaoClasse() {
		return transacaoClasse;
	}

	public void setTransacaoClasse(Transaction transacaoClasse) {
		this.transacaoClasse = transacaoClasse;
	}


}
