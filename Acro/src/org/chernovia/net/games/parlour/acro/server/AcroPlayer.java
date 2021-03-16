package org.chernovia.net.games.parlour.acro.server;

import java.util.Vector;
import org.chernovia.lib.net.zugserv.Connection;
import com.google.gson.JsonObject;

public class AcroPlayer implements Comparable<AcroPlayer> {

	class AcroRec {
		AcroGame.Acro acro; AcroPlayer vote; 
		public AcroRec(AcroGame.Acro a, AcroPlayer v) { acro=a; vote=v; }
	}
	Connection conn; 
	Vector<AcroRec> record;
	int acros, vote, score;
	AcroGame game;

	public AcroPlayer(AcroGame g, Connection c) {
		game = g; conn = c; acros = 0; score=0; vote = -1;
		record = new Vector<AcroRec>();
	}

	public String getName() { return conn.getHandle(); } //TODO: remove this potentially confusing redundancy

	public void save(AcroGame.Acro a, AcroPlayer v) {
		record.add(new AcroRec(a,v));
		if (game.REVEAL && vote >= 0) { //TODO: no clue what I was trying to do here
			//conn.getServ().tch(game.getChan(), getName() + " voted for: " + game.getPlayer(vote).getName(),false,false);
		}
		if (acros < game.getMaxRound()) acros++;
	}
	
	public JsonObject toJson() {
		JsonObject p = new JsonObject();
		p.addProperty("name",conn.getHandle());
		p.addProperty("score",score);
		return p;
	}

	@Override
	public int compareTo(AcroPlayer p) {
		return this.score - p.score;
	}
}
