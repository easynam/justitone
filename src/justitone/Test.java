package justitone;

import javax.sound.sampled.LineUnavailableException;

import justitone.jidi.JidiSequence;
import justitone.parser.Reader;

public class Test {
    public static void main(String[] args) throws LineUnavailableException {
        Reader reader = new Reader();

        Sequence channel = reader.parse("120: 1/8[>0 >90 >0 >0 >0 >0 >0]");


        new JidiSequence(channel);
    }
}
