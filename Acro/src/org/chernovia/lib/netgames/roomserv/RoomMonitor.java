package org.chernovia.lib.netgames.roomserv;

public class RoomMonitor extends Thread {
	private RoomManager mgr;
	private long UnstartedBoardTimeout = 120000;
	private int PollFreq = 60;
	private boolean RUNNING = false;

	public RoomMonitor(long t, RoomManager m) {
		UnstartedBoardTimeout = t * 1000; mgr = m;
		setName("BoardMon" + getName());
	}

	@Override
	public void run() {
		RUNNING = true;
		while (RUNNING) {
			try { sleep(PollFreq * 1000); }
			catch (InterruptedException ignore) {}
			long t = System.currentTimeMillis();
			for (int b=0;b<mgr.getRooms().size();b++) {
				Room R = mgr.getRooms().elementAt(b);
				if (!R.isAlive() &&
						t > (R.getTimeCreated() +
								UnstartedBoardTimeout)) {
					mgr.getServ().broadcast("Clearing " +
							R.getCreator() + "'s " +
							mgr.getRoomNoun());
					R.closeRoom(true);
				}
			}
		}
	}
}
