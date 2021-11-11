import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * This is our main class, from which we read in a file which includes the notes
 * needed to play a specific song.
 */
public class Tone {
	private static List<BellNote> song = new ArrayList<BellNote>();
	private int NUM_MEMBERS = 18; // number of possible notes
	Thread[] members = new Thread[NUM_MEMBERS];
	private static int linecount;
	private static String fileName;
	private static boolean isReady;
	// private final BlockMutex m = new BlockMutex();

	/**
	 * This is where we load in the file, making a quick check to make sure it's
	 * valid. We call loadSong() and playSong().
	 */
	public static void main(String[] args) throws Exception {
		fileName = args[0];
		File myFile = new File(fileName);
		if (!myFile.exists()) { // Does the file exist?
			System.err.println(fileName + " does not exist.");
			System.exit(0);
		} else { // Load and play the song
			song = loadSong(fileName);
			final AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
			Tone t = new Tone(af);
			t.playSong(song);
		}
	}

	/**
	 * Here we read in the lines of the file and make various checks to ensure the
	 * file is in the right format. We store the elements of each line as a list of
	 * Bellnotes.
	 */
	private static List<BellNote> loadSong(String filename) {
		List<BellNote> notes = new ArrayList<>();
		final File file = new File(filename);
		BufferedReader br = null;
		try { //let's read the file
			br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			linecount = 0;
			while (line != null) {
				linecount++;
				String[] items = line.split(" "); //split on whitespace
				try {
					BellNote thismove = new BellNote(parseNote(items[0]), parseInt(items[1]));
					notes.add(thismove);
					line = br.readLine();
				} catch (ArrayIndexOutOfBoundsException e) {
					// If there are missing elements in a line, throw an error and terminate.
					isReady = false;
					System.err.println("Missing note length value at line " + linecount + " of the file " + fileName
							+ ". Please use this format: Note[space]Length. i.e. E4 2");
					System.exit(0);
				}
				// If they have something after the note and the length, we will remind them
				// that it's being ignored.
				try {
					String testline = items[2];
					System.out.println("Everything after and including the element '" + testline + "' on line "
							+ linecount + "  was ignored. Please use this space for comments and notes.");
				} catch (ArrayIndexOutOfBoundsException e) {
					// Nothing needs to be done
				}
			}
			if (linecount == 0) {
				System.err.println("Unable to read lines. " + fileName + " is probably empty.");
				System.exit(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close(); //Close our Buffered Reader like a good little boy scout
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		isReady = true;
		return notes;
	}

	/**
	 * A function for converting a string from our file into a value of the enum
	 * NoteLength.
	 */
	private static NoteLength parseInt(String n) {
		switch (n.toUpperCase().trim()) {
		case "1":
			return NoteLength.WHOLE;

		case "2":
			return NoteLength.HALF;

		case "4":
			return NoteLength.QUARTER;

		case "8":
			return NoteLength.EIGTH;

		default: //if its not one of the above, that's a problem
			isReady = false;
			System.err.println("Incorrect Note Length syntax at line " + linecount + " of the file " + fileName + ". '"
					+ n + "' is not a valid length.");
			System.exit(0);
			return null;
		}
	}

	/**
	 * A function for converting a string from our file into a value of the enum
	 * Note.
	 */
	private static Note parseNote(String n) {
		Note mynote = null;
		try {
			mynote = Note.valueOf(n.toUpperCase().trim());
		} catch (IllegalArgumentException e) {
			isReady = false;
			if (n.isEmpty()) {
				System.err.println("Line " + linecount + " of the file " + fileName
						+ " is empty. Please don't have empty lines between notes.");
			} else {
				System.err.println("Incorrect Note syntax at line " + linecount + " of the file " + fileName
						+ ". The token '" + n + "' is unrecognized.");
			}
			System.exit(0);
		}

		return mynote;
	}

	private final AudioFormat af;

	Tone(AudioFormat af) {
		this.af = af;
	}

	/**
	 * In this function we create our member threads, calling from the Member class.
	 * Next, we give them each a Source Data line and a BellNote, and then stop them
	 * after all the notes have been played.
	 */
	void playSong(List<BellNote> song) throws LineUnavailableException {
		try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
			if (isReady) {
				line.open();
				line.start();
				//Create Member threads
				Member[] Members = new Member[Note.values().length];
				for (Note n : Note.values()) {
					Members[n.ordinal()] = new Member(n);
				}
				System.out.println("Playing " + fileName); //Begin playing notes.
				for (BellNote bn : song) {
					// playNote(line, bn);
					Member m = Members[bn.note.ordinal()];
					m.giveTurn(line, bn);

				}
				for (Member m : Members) { // Stop our members now that everything is done.
					m.stopMember();
				}
				line.drain();
			}

		}

	}

	/**
	 * This function plays a note of a song, and takes a Source Data Line and a
	 * BellNote as arguments. It's able to write audio bytes to the line to play our
	 * note.
	 */
	public static void playNote(SourceDataLine line, BellNote bn) {
		final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
		final int length = Note.SAMPLE_RATE * ms / 1000;
		line.write(bn.note.sample(), 0, length);
		line.write(Note.REST.sample(), 0, 50);
	}

}

/**
 * This class packs the note pitch and length into one datatype.
 */
class BellNote {
	final Note note;
	final NoteLength length;

	BellNote(Note note, NoteLength length) {
		this.note = note;
		this.length = length;
	}
}

/**
 * This enum is where all the lengths are stored and determines the time scale
 * for each.
 */
enum NoteLength {
	WHOLE(1.0f), HALF(0.5f), QUARTER(0.25f), EIGTH(0.125f);

	private final int timeMs;

	private NoteLength(float length) {
		timeMs = (int) (length * Note.MEASURE_LENGTH_SEC * 1000);
	}

	public int timeMs() {
		return timeMs;
	}
}

/**
 * This enumerator contains all the notes that can be played as well as the
 * complicated calculations needed in order to get accurate pitches most
 * commonly used in music theory
 */
enum Note {
	// REST Must be the first 'Note'
	REST, A4, A4S, B4, C4, C4S, D4, D4S, E4, F4, F4S, G4, G4S, A5, A5S, B5, C5, C5S, D5;

	public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
	public static final int MEASURE_LENGTH_SEC = 1;

	// Circumference of a circle divided by # of samples
	private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

	private final double FREQUENCY_A_HZ = 440.0d;
	private final double MAX_VOLUME = 127.0d;

	private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

	private Note() {
		int n = this.ordinal();
		if (n > 0) {
			// Calculate the frequency!
			final double halfStepUpFromA = n - 1;
			final double exp = halfStepUpFromA / 12.0d;
			final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

			// Create sinusoidal data sample for the desired frequency
			final double sinStep = freq * step_alpha;
			for (int i = 0; i < sinSample.length; i++) {
				sinSample[i] = (byte) (Math.sin(i * sinStep) * MAX_VOLUME);
			}
		}
	}

	/**
	 * Returns a sine wave
	 */
	public byte[] sample() {
		return sinSample;
	}
}