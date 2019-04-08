package justitone;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Playback {
	static final int fs = 44100;

	public static void play(Track channel) throws LineUnavailableException {
		final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
		SourceDataLine line = AudioSystem.getSourceDataLine(af);

		line.open(af, fs);
		line.start();
		
		for (Note note : channel.notes) {
			play (line, note);
		}
		
		line.drain();
		line.close();
	}

	private static void play(SourceDataLine line, Note Note) {
		line.write(Note.sin(), 0, Note.samples*2);
	}
}
