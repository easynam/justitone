package justitone.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import justitone.jidi.JidiEvent;
import justitone.jidi.JidiSequence;
import justitone.jidi.JidiTrack;

public class Midi {
    public Sequence jidiToMidi(JidiSequence jidi, float pitchBendRange) throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, jidi.ppm/4);
        
        Track tempoTrack = seq.createTrack();
        
        {
            MetaMessage message = new MetaMessage();
            message.setMessage(0x51, encodeTempo(jidi.bpm), 3);
            tempoTrack.add(new MidiEvent(message, 0));
        }
        {
            ShortMessage message = new ShortMessage();
            message.setMessage(0xB0, 126, 0);
            tempoTrack.add(new MidiEvent(message, 0));
        }

        int channel = 0;
        
        for (JidiTrack jidiTrack : jidi.tracks) {
            addTrack(seq, jidiTrack, pitchBendRange, channel++);
        }

        return seq;
    }
    
    private float note(float freq) {
        return (float) (69 + 12 * Math.log(freq/440)/Math.log(2));
    }
    
    private int midiNote(float note) {
        return Math.round(note);
    }
    
    private byte[] midiPitchBend(float note, float range) {
        final float translated = note - midiNote(note);
        
        final float clamped = Math.min(Math.max(translated/range, -1), 1);
        final int asInt = (int) (clamped * 8192);
        
        return encode(asInt);
    }

    public byte[] encode(int num) {
        final int translated = num + 8192;
            
        byte[] encoded = new byte[2];
            
        encoded[0] = (byte) (translated & 0x7F);
        encoded[1] = (byte) ((translated & 0x3F80) >> 7);
            
        return encoded;
    }
    
    public byte[] encodeTempo(int bpm) {
        int micros = (int) 60000000f/bpm;
        
        byte[] encoded = new byte[3];
            
        encoded[2] = (byte) (micros & 0xFF);
        encoded[1] = (byte) ((micros & 0xFF00) >> 8);
        encoded[0] = (byte) ((micros & 0xFF0000) >> 16);
            
        return encoded;
    }
    
    private Track addTrack(Sequence seq, JidiTrack jidiTrack, float pitchBendRange, int channel) throws InvalidMidiDataException {
        Track track = seq.createTrack();
        
        ShortMessage message = new ShortMessage();
        message.setMessage(ShortMessage.PROGRAM_CHANGE | channel, 80, 0);
        track.add(new MidiEvent(message, 0));
        
        float freq = 440;
        
        boolean noteOn = false;
        
        for (JidiEvent e : jidiTrack.events) {
            if (e instanceof JidiEvent.Pitch) {
                if (noteOn) {
                    writeNoteOff(track, freq, e.tick, channel);
                }
                
                JidiEvent.Pitch pitch = (JidiEvent.Pitch) e;
                
                freq = pitch.freq;
                writeNoteOn(track, freq, pitch.tick, pitchBendRange, channel);
                
                noteOn = true;
            }
            else if (e instanceof JidiEvent.NoteOff) {
                writeNoteOff(track, freq, e.tick, channel);
                
                noteOn = false;
            }
        }
        
        return track;
    }
    
    private void writeNoteOn(Track track, float freq, long tick, float pitchBendRange, int channel) throws InvalidMidiDataException {
        float note = note(freq);
        
        ShortMessage message = new ShortMessage();
        message.setMessage(ShortMessage.NOTE_ON | channel, midiNote(note) & 0xff, 0x60);
        track.add(new MidiEvent(message, tick));
        
        byte[] pitchBend = midiPitchBend(note, pitchBendRange);
        
        message = new ShortMessage();
        message.setMessage(ShortMessage.PITCH_BEND | channel, pitchBend[0], pitchBend[1]);
        track.add(new MidiEvent(message, tick));
    }
    
    private void writeNoteOff(Track track, float freq, long tick, int channel) throws InvalidMidiDataException {
        float note = note(freq);
        
        ShortMessage message = new ShortMessage();
        message.setMessage(ShortMessage.NOTE_OFF | channel, midiNote(note), 0x40);
        track.add(new MidiEvent(message, tick));
    }
    
}
