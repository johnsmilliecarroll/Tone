
public class BlockMutex {

	private volatile boolean occupied;

	BlockMutex() {
		occupied = false;
	}

	/**
	 * This is where the wait() function for the tread is housed. If occupied is
	 * true, threads need to wait until they're able to occupy the mutex.
	 */

	synchronized public void acquire() {
		while (occupied) { // other threads can't occupy
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		occupied = true; // a thread has occupied the mutex
	}

	/**
	 * Sets occupied to false and lets all the threads know that the mutex is freed
	 * up for one of them to occupy it.
	 */

	synchronized public void release() {
		occupied = false; // mutex is free for a thread to occupy
		notifyAll(); // let the threads know
	}
}
