//TODO: investigate possible mixup of acro order in updateAcro()
//also: add timer for countdown...

package org.chernovia.net.games.parlour.acro.server;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.*;

public class AcroBox extends JFrame {
	private static final long serialVersionUID = 1L;

	public class AcroPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private BufferedImage img = null;
		public AcroPanel() {}
		public Graphics getContext() {
			if (img == null) { 
				img = new BufferedImage(getWidth(),getHeight(),
				BufferedImage.TYPE_INT_RGB);
			}
			return img.getGraphics();
		}
		public BufferedImage getImg() { return img; }
		public void setImg(BufferedImage newImg) { img = newImg; repaint(); }
		public void paint(Graphics g) { g.drawImage(img, 0,  0,  null); }
	}
	
	int width = 1066, height = 600;
	//should these be JTables?
	AcroPanel scorePan, acroPan, highPan, helpPan; 
	
	public AcroBox() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(width,height); //this.setType(Window.Type.UTILITY);
		scorePan = new AcroPanel(); scorePan.setSize(width/3,height/2); 
		acroPan = new AcroPanel(); acroPan.setSize((int)(width/1.5),height/2); 
		highPan = new AcroPanel(); highPan.setSize(width/2,(height/2) - 25); 
		helpPan = new AcroPanel(); helpPan.setSize(width/2,(height/2) -25); 
		add(scorePan); scorePan.setLocation(0, 0);
		add(acroPan); acroPan.setLocation(width/3, 0);
		add(highPan); highPan.setLocation(0, height/2);
		add(helpPan); helpPan.setLocation(width/2, height/2);
		add(new Label()); //Why??		
		BufferedImage img = getScaledImage(
		this.getClass().getResource("img/acrohelp.png"),
		helpPan.getWidth(),helpPan.getHeight());
		//helpPan.getContext().drawImage(img,-4,-4,null);
		helpPan.setImg(img);
	}
	
	public BufferedImage getScaledImage(URL url,int x, int y) {
		System.out.println("URL: " + url.getFile());
		Image img = null;
		try { img = ImageIO.read(url).getScaledInstance(x,y, Image.SCALE_SMOOTH);	}
		catch (Exception e) { e.printStackTrace(); return null; }
		BufferedImage buffImg = new BufferedImage(x,y,BufferedImage.TYPE_INT_ARGB);
		Graphics g = buffImg.createGraphics();
		g.drawImage(img,0,0,null); g.dispose();
		return buffImg;
	}
		
	public void updateScores(Vector<AcroPlayer> players) {
		String[] playlist = new String[players.size()];
		int i=0; for (AcroPlayer p : players) playlist[i++] = p.conn.getHandle() + ": " + p.score;
		makeList(scorePan,playlist);
	}
	
	public void updateAcros(Vector<AcroGame.Acro> acrolist) {
		String[] acros = new String[acrolist.size()];
		int i=0; for (AcroGame.Acro acro : acrolist) acros[i++] = (i) + "." + acro.acro;
		makeList(acroPan,acros);
	}
	
	//TODO: measure Fonts more intelligently
	private void makeList(AcroPanel pan, String[] list) {
		if (list.length < 1) return;
		Graphics g = pan.getContext();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, pan.getWidth(), pan.getHeight());
		g.setColor(Color.WHITE); 
		g.draw3DRect(1, 1, pan.getWidth()-2, pan.getHeight()-2,true);
		int ySpace = pan.getHeight() / list.length;
		int y = 20 - list.length;
		g.setFont(new Font(Font.MONOSPACED,Font.BOLD,(int)(y/1.5)));
		for (int i = 0; i < list.length; i++) {
			g.setColor(rndColor());
			g.drawString(list[i], 8, y);
			y += ySpace;
		}
		repaint();
	}
	
	public void updateHiScores(StringTokenizer scores) {
		int top = scores.countTokens(); //should be 10
		Graphics g = highPan.getContext();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, highPan.getWidth(), highPan.getHeight());
		g.setColor(Color.WHITE); 
		g.draw3DRect(1, 1, highPan.getWidth()-2, highPan.getHeight()-2,true);
		int ySpace = highPan.getHeight() / top;
		int y = 16;
		g.setFont(new Font(Font.MONOSPACED,Font.BOLD,12));
		for (int i = 0; i < top; i++) {
			g.setColor(rndColor());
			g.drawString(scores.nextToken(), 8, y);
			y += ySpace;
		}
		repaint();
	}
	
	public void updateAcro(String acro, String topic) {
		Graphics g = acroPan.getContext();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, acroPan.getWidth(), acroPan.getHeight());
		g.setColor(Color.WHITE); 
		g.draw3DRect(1, 1, acroPan.getWidth()-2, acroPan.getHeight()-2,true);
		if (!topic.equals(AcroGame.NO_TOPIC)) {
			g.setFont(new Font(Font.MONOSPACED,Font.BOLD,24));
			g.drawString("Topic: " + topic, 8,24);
		}
		g.setFont(new Font(Font.MONOSPACED,Font.BOLD,36));
		g.drawString("Acro: " + acro, acroPan.getWidth()/6, acroPan.getHeight()/2);
		repaint();
	}
	
	private Color rndColor() {
		int r = (int)(Math.random() * 128) + 128;
		int g = (int)(Math.random() * 128) + 128;
		int b = (int)(Math.random() * 128) + 128;
		return new Color(r,g,b);
	}

	/*
	public static void main(String[] args) {
		AcroBox box = new AcroBox();
		box.setVisible(true);
		String[] acros = new String[10];
		for (int i=0;i<acros.length;i++) acros[i] = "yausfgugfuigf aduywtgf aa duygw";
		acros[acros.length-1] = "last acro";
		//box.updateAcros(24, acros);
		//box.updateAcro("EFUGHE");
		//box.updateHiScores(acros);
		box.updateHelp("res/acro/acrohelp.png");
	}*/

}
