package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

import justitone.Event;
import justitone.Sequence;
import justitone.Song;

//java intonation digital interface
public class JidiSequence {
    public List<JidiTrack> tracks;
    private List<JidiTrack> inUse;

    public final int ppm = 480 * 4;
    public final int bpm;

    public JidiSequence(Song song) {
        bpm = song.bpm;
        
        tracks = new ArrayList<>();
        inUse = new ArrayList<>();

        JidiTrack track = allocateTrack();
        inUse.add(track);

        loadSequence(new State(), BigFraction.ZERO, false, song.sequence, track);
    }

    public JidiTrack allocateTrack() {
        if (inUse.size() == tracks.size()) {
            JidiTrack track = new JidiTrack(tracks.size());
            tracks.add(track);
            return track;
        }
        else {
            return tracks.stream().filter(t -> !inUse.contains(t)).findFirst().get();
        }
    }
    
    public List<JidiTrack> allocateTracks(int count) {
        List<JidiTrack> tracks = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            tracks.add(allocateTrack());
        }
        
        return tracks;
    }

    public void loadSequence(State state, BigFraction currentPos, boolean noteOn, Sequence sequence, JidiTrack track) {
        for (Event e : sequence.contents()) {
            int tick = currentPos.multiply(ppm).intValue();

            if (e instanceof Event.Note) {
                noteOn = true;

                float freq = ((Event.Note) e).ratio.multiply(state.freqMultiplier).floatValue() * 440f;

                track.add(new JidiEvent.NoteOn(tick));
                track.add(new JidiEvent.Pitch(tick, freq));
            }
            else if (e instanceof Event.Rest) {
                if (noteOn) {
                    track.add(new JidiEvent.NoteOff(tick));
                }

                noteOn = false;

            }
            else if (e instanceof Event.Modulation) {
                state = state.multiplyFreq(((Event.Modulation) e).ratio);
            }
            else if (e instanceof Event.SubSequence) {
                Event.SubSequence sub = (Event.SubSequence) e;
                loadSequence(state.multiplyLength(sub.eventLength()), currentPos, noteOn, sub.sequence(), track);
            }
            else if (e instanceof Event.Poly) {
                Event.Poly poly = (Event.Poly) e;
                loadPoly(state, currentPos, noteOn, poly.sequences, track);
            }

            currentPos = currentPos.add(e.length().multiply(state.lengthMultiplier));
        }
    }
    
    public void loadPoly(State state, BigFraction currentPos, boolean noteOn, List<Event.SubSequence> subs, JidiTrack track) {
        loadSequence(state, currentPos, noteOn, subs.get(0).sequence(), track);
        
        if (subs.size() > 1) {
            List<JidiTrack> allocated = allocateTracks(subs.size() - 1);
            
            inUse.addAll(allocated);
            
            for (int i = 1; i < subs.size(); i++) {
                Event.SubSequence sub = subs.get(i);
                loadSequence(state.multiplyLength(sub.eventLength()), currentPos, noteOn, sub.sequence(), allocated.get(i - 1));
            }
            
            inUse.removeAll(allocated);
        }
    }
    
    private class State {
        final BigFraction lengthMultiplier;
        final BigFraction freqMultiplier;
        
        public State(BigFraction lengthMultiplier, BigFraction freqMultiplier) {
            this.lengthMultiplier = lengthMultiplier;
            this.freqMultiplier = freqMultiplier;
        }
        
        public State() {
            this(BigFraction.ONE, BigFraction.ONE);
        }
        
        public State multiplyLength(BigFraction multiplier) {
            return new State(lengthMultiplier.multiply(multiplier), freqMultiplier);
        }
        
        public State multiplyFreq(BigFraction multiplier) {
            return new State(lengthMultiplier, freqMultiplier.multiply(multiplier));
        }
    }
}
