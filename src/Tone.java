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

public class Tone {
	// stack over flow for buffer refresher
	// Mary had a little lamb
	// ant -Dsong=Mary.txt run
	private static List<BellNote> song = new ArrayList<BellNote>();
	private int NUM_MEMBERS = 18; // number of possible notes
	Thread[] members = new Thread[NUM_MEMBERS];
	// private final BlockMutex m = new BlockMutex();

	public static void main(String[] args) throws Exception {
		song = loadSong(args[0]);
		final AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
		Tone t = new Tone(af);
		t.playSong(song);
	}

	private static List<BellNote> loadSong(String filename) {
		List<BellNote> notes = new ArrayList<>();
		final File file = new File(filename);
		BufferedReader br = null;
		if (file.exists()) {
			try {
				br = new BufferedReader(new FileReader(filename));
				String line = br.readLine();

				while (line != null) {
					String[] items = line.split(" ");
					BellNote thismove = new BellNote(parseNote(items[0]), parseInt(items[1]));
					notes.add(thismove);
					line = br.readLine();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return notes;
	}

	private static NoteLength parseInt(String num) {
		if (num == null) {
			return null;
		}

		switch (num.toUpperCase().trim()) {
		case "1":
			return NoteLength.WHOLE;

		case "2":
			return NoteLength.HALF;

		case "4":
			return NoteLength.QUARTER;

		case "8":
			return NoteLength.EIGTH;

		default:
			return null;
		}
	}

	private static Note parseNote(String symbol) {
		// If you give me garbage, I'll give it back
		if (symbol == null) {
			return null;
		} else {
			return Note.valueOf(symbol.toUpperCase().trim());
		}
	}

	private final AudioFormat af;

	Tone(AudioFormat af) {
		this.af = af;
	}

	void playSong(List<BellNote> song) throws LineUnavailableException {
		try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
			line.open();
			line.start();

			Member[] Members = new Member[Note.values().length];
			for (Note n : Note.values()) {
				Members[n.ordinal()] = new Member(n);
			}
			for (BellNote bn : song) {
				// playNote(line, bn);
				Member m = Members[bn.note.ordinal()];
				m.giveTurn(line, bn);

			}
			for (Member m : Members) {
				m.stopMember();
			}
			line.drain();
		}

	}

	public static void playNote(SourceDataLine line, BellNote bn) {
		final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
		final int length = Note.SAMPLE_RATE * ms / 1000;
		line.write(bn.note.sample(), 0, length);
		line.write(Note.REST.sample(), 0, 50);
	}

}

class BellNote {
	final Note note;
	final NoteLength length;

	BellNote(Note note, NoteLength length) {
		this.note = note;
		this.length = length;
	}
}

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

	public byte[] sample() {
		return sinSample;
	}
}