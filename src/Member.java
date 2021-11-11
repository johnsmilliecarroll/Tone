import javax.sound.sampled.SourceDataLine;

/**
 * This class sets up Member threads which are tasked with each playing only one
 * unique note.
 */
public class Member implements Runnable {
	private SourceDataLine myLine;
	private BellNote myNote;
	private final Note myJob;
	private final Thread t;
	private volatile boolean running;
	private boolean myTurn;

	Member(Note n) {
		running = true;
		this.myJob = n;
		t = new Thread(this, n.name());
		t.start();
	}

	/**
	 * Stops the Member after the song is done playing, notifies it that it is time
	 * to stop waiting.
	 */
	public void stopMember() {
		running = false;
		synchronized (this) {
			notify(); //let me know to stop waiting
		}
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This gives the needed SourceDataLine and BellNote to the Member when a new
	 * note needs to be played, and allows them to stop waiting by giving them a
	 * turn.
	 */
	public void giveTurn(SourceDataLine line, BellNote mynote) {
		synchronized (this) {
			//A member doesn't need to be given a turn if its already their turn!
			if (myTurn) {
				throw new IllegalStateException(
						"Attempt to give a turn to a Member who's hasn't completed the current turn");
			}
			// A Member can only play their specific note!
			if (t.getName() != mynote.note.name()) {
				throw new IllegalStateException(
						"You can't give the note " + mynote.note.name() + " to Member " + t.getName());
			}
			myTurn = true;
			myNote = mynote;
			myLine = line;
			notify();
			while (myTurn) {
				try {
					wait(); //It's my turn, give me time to get something done.
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	/**
	 * This is the main run method of the thread, in which is waits until its turn,
	 * and when it's turn comes it calls the function doTurn() to accomplish it's
	 * specific task.
	 */
	public void run() {
		synchronized (this) {
			do {
				// Wait for my turn
				while (!myTurn && running) {
					try {
						wait();
					} catch (InterruptedException ignored) {
					}
				}
				if (running) {
					// My turn!
					doTurn();

					// Done, complete turn and wakeup the waiting process
					myTurn = false;
					notify();
				}

			} while (running);
		}
	}

	/**
	 * We call the playNote() function from Tone in order to actually play the note
	 * the member is given.
	 */
	private void doTurn() {
		System.out.println("Member[" + myJob.name() + "] taking turn");
		Tone.playNote(myLine, myNote); //Ringing my bell!
	}
}