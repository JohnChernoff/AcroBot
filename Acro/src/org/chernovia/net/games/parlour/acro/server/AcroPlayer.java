package org.chernovia.net.games.parlour.acro.server;

import org.chernovia.lib.netgames.roomserv.Connection;

public class AcroPlayer {

	class AcroRec {
		String acro; int vote; int votes;
		public AcroRec(String a, int v, int p) {
			acro=a; vote=v; votes=p;
		}
	}
	Connection conn; AcroRec record[];
	int acros, vote, score;
	AcroGame game;

	public AcroPlayer(AcroGame g, Connection c) {
		game = g; conn = c; acros = 0; score=0; vote = -1;
		record = new AcroRec[game.getMaxRound()];
	}

	public String getName() { return conn.getHandle(); }

	public void save(String a, int v, int p) {
		record[acros] = new AcroRec(a,v,p);
		if (game.REVEAL && vote>=0) {
			conn.getServ().tch(game.getChan(),
			getName() + " voted for: " + game.getPlayer(vote).getName(),
			false,false);
		}
		if (acros < game.getMaxRound()) acros++;
	}

	public int sumacros() {
		int a = 0; for (int i=0;i<acros;i++)
			if (!record[i].acro.equals("")) a++;
		return a;
	}
}
