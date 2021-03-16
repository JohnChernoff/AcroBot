package org.chernovia.net.games.parlour.acro.server;

import java.util.StringTokenizer;
import java.util.Vector;

import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.net.zugserv.ZugServ;
import org.chernovia.lib.netgames.db.FieldData;
import org.chernovia.lib.netgames.db.GameBase;
import org.chernovia.lib.netgames.db.GameData;

public class AcroBase extends GameBase {
	//public static int MAXHAND=15, MAXTITLE=6;

	public static Vector<FieldData> initFields() {
		Vector<FieldData> V = new Vector<FieldData>();
		V.add(new FieldData("STR","Handle"));
		V.add(new FieldData("INT","Acros"));
		V.add(new FieldData("INT","Games"));
		V.add(new FieldData("INT","Points"));
		V.add(new FieldData("INT","Wins"));
		V.add(new FieldData("INT","Rating"));
		return V;
	}

	public static StringTokenizer newPlayer(String P) {
		return new StringTokenizer(P + " 0 0 0 0 1500");
	}

	public static String statLine(GameData D) {
		if (D==null) return "No such player.";
		else return GameData.statHead() + CR + D.playStr();
	}

	public static void updateStats(AcroGame G, AcroPlayer winner) {
		int ratingavg = 0, rd = 0;
		int games, wins, rating, points, acros;
		GameData D = null;
		Vector<AcroPlayer> players = G.getPlayers();
		//calc average rating of board
		for (AcroPlayer p : players) {
			D = GameBase.getStats(p.getName(),newPlayer(p.getName()));
			if (!winner.getName().equals(
					D.getField("Handle"))) {
				ratingavg += D.getIntField("rating");
			}
		}
		if (players.size() > 1) ratingavg /= (players.size()-1);
		//edit stats
		for (AcroPlayer p : players) {
			D = GameBase.getStats(p.getName(),newPlayer(p.getName()));
			games = D.getIntField("games");
			acros = D.getIntField("acros");
			points = D.getIntField("points");
			wins = D.getIntField("wins");
			rating = D.getIntField("rating");
			D.setField("games",games + 1);
			D.setField("acros",acros); // + p.sumacros()); TODO: what's sumacros?!
			D.setField("points",points + p.score);
			rd = (ratingavg-rating);
			if (rd > 180) rd = 180; else if (rd < -180) rd = -180;
			if (p == winner) { //winner
				D.setField("wins",wins + 1);
				D.setField("rating",rating + (16 + (rd/12)));
				G.getServ().broadcast(ZugServ.MSG_SERV,D.getHandle() +
				" wins his/her " +	MiscUtil.getSuffix(wins+1) +
				" Acrophobia game!");
			}
			else { //loser
				D.setField("rating",rating - ((16 - (rd/12))/(players.size()-1))); //Hopefully > 0!
			}
			GameBase.editStats(D);
		}
	}
}