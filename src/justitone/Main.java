package justitone;

import javax.sound.sampled.LineUnavailableException;

import justitone.parser.Reader;

public class Main {

	public static void main(String[] args) throws LineUnavailableException {
		Reader reader = new Reader();
		
		Track channel = reader.parse("c1> 1/8:1 1/8:4/3 1/4:3/2 1/4:9/5 1/8:8/5 1/8:7/5 1/4:6/5 1/4:8/7 1/4:1 ^5/4 1/8:1 1/8:4/3 1/4:3/2 1/4:9/5 1/8:8/5 1/8:7/5 1/4:6/5 1/4:8/7 1/4:1");
		
		for (Note note : channel.notes) {
			System.out.println(note);
		}
		
		Playback.play(channel);
	}

}
