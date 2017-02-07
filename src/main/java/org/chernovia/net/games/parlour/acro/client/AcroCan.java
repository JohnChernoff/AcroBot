package org.chernovia.net.games.parlour.acro.client;

import java.awt.*;

public class AcroCan extends Canvas {
	
	private Image[] letters;
	private String acro = "acrophobia";
	Color color;
	
	public AcroCan(int width, int height,Image[] imgs, Color c) {
		color = c;
		setSize(width,height);
		letters = imgs;
	}
	
	public void paint(Graphics g) {
		int x=0; int w = getWidth(); int h = getHeight();
		g.setColor(color); g.fillRect(0, 0, w, h);
		for (int i=0;i<acro.length();i++) {
			Image img = letters[acro.charAt(i)-97];
			g.drawImage(img,x,0,img.getWidth(null),h,null); 
			x += img.getWidth(null);
		}
	}
	
	public void setAcro(String a) {	acro = a.toLowerCase(); repaint(); }
}
