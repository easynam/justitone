package justitone;

import javax.sound.sampled.LineUnavailableException;

import justitone.parser.Reader;

public class Main {

	public static void main(String[] args) throws LineUnavailableException {
		Reader reader = new Reader();
		
		Track channel = reader.parse("c1: [:1 - _ :2 - _ :3 _]");
		
		for (Note note : channel.notes) {
			System.out.println(note);
		}
		
		Playback.play(channel);
	}

}
