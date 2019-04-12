package justitone;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.sampled.LineUnavailableException;

import justitone.jidi.JidiSequence;
import justitone.midi.Midi;
import justitone.parser.Reader;

public class Test {
    public static void main(String[] args) throws LineUnavailableException, InvalidMidiDataException, IOException {
        Reader reader = new Reader();

        Song song = reader.parse("120: ^:/243 [:243 :244 :245 :246 :247 :248 :249 :250 :251 :252 :253 :254 :255 :256]");
        
        JidiSequence seq = new JidiSequence(song, 768);
        
        Midi midi = new Midi();
        
        Sequence midiSeq = midi.jidiToMidi(seq, 2f);
        
        File f = new File("midifile.mid");
        MidiSystem.write(midiSeq, 1, f);
    }
}
