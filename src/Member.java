import javax.sound.sampled.SourceDataLine;

public class Member implements Runnable {
	// private static final int NUM_TURNS = 5;
	private SourceDataLine myLine;
	private BellNote myNote;

	public static void main(String[] args) {
		// Create all the Members, and give each a turn

	}

	private final Note myJob;
	private final Thread t;
	private volatile boolean running;
	private boolean myTurn;

	Member(Note n) {
		this.myJob = n;
		t = new Thread(this, n.name());
		t.start();
	}

	public void stopMember() {
		running = false;
		myTurn = true;
		synchronized(this) {
			notify();
		}
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void giveTurn(SourceDataLine line, BellNote note) {
		synchronized (this) {
			if (myTurn) {
				throw new IllegalStateException(
						"Attempt to give a turn to a Member who's hasn't completed the current turn");
			}
			myTurn = true;
			myNote = note;
			myLine = line;
			running = true;
			notify();
			while (myTurn) {
				try {
					wait();
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	public void run() {
		synchronized (this) {
			do {
				// Wait for my turn
				while (!myTurn) {
					try {
						wait();
					} catch (InterruptedException ignored) {
					}
				}

				// My turn!
				if(running)
				{
					doTurn();

					// Done, complete turn and wakeup the waiting process
					myTurn = false;
					notify();
				}
				
			} while (running);
		}
	}

	private void doTurn() {
		System.out.println("Member[" + myJob.name() + "] taking turn");
		Tone.playNote(myLine, myNote);
	}
}