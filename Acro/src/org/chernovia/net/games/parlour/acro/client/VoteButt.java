package org.chernovia.net.games.parlour.acro.client;

import java.awt.Button;

public class VoteButt extends Button {
	private static final long serialVersionUID = 1L;
	String acro, author;
	int number;
	
	public VoteButt(int n, String a, String p) {
		acro = a; author = p; number = n; 
		setLabel(n + ". " + a);
	}
	
}
