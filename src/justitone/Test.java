package justitone;

import javax.sound.sampled.LineUnavailableException;

import justitone.jidi.JidiSequence;
import justitone.parser.Reader;

public class Test {
    public static void main(String[] args) throws LineUnavailableException {
        Reader reader = new Reader();

        Sequence channel = reader.parse("120: 1/8{>0 >0 ^:5/4 [>0 >30 >90]} >0");

        new JidiSequence(channel);
    }
}
