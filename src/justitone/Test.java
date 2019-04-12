package justitone;

import javax.sound.sampled.LineUnavailableException;

import justitone.jidi.JidiSequence;
import justitone.parser.Reader;

public class Test {
    public static void main(String[] args) throws LineUnavailableException {
        Reader reader = new Reader();

        Song song = reader.parse("120: :/1 :1 :/5 :2/5\n");
        
        new JidiSequence(song);
    }
}
