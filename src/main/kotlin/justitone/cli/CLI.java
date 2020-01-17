package justitone.cli;

import justitone.Song;
import justitone.jidi.JidiSequence;
import justitone.midi.Midi;
import justitone.parser.Reader;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class CLI {
    public static void main(String args[]) throws InvalidMidiDataException, IOException {
        if (args.length < 1) System.exit(0);

        String fileName = args[0];

        Scanner in = new Scanner(System.in);

        StringBuilder s = new StringBuilder();

        while (in.hasNext()) {
            s.append(in.nextLine());
            s.append("\n");
        }

        String source = s.toString();

        in.close();

        Reader reader = new Reader();

        Song song = reader.parse(source);

//        if (song.getE().getEvents().isEmpty()) System.exit(1);

        JidiSequence seq = new JidiSequence(song, 768);

        Midi midi = new Midi();

        Sequence midiSeq = midi.jidiToMidi(seq, 2f);

        File f = new File(fileName);
        MidiSystem.write(midiSeq, 1, f);
    }
}
