package justitone.jidi;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fraction.BigFraction;

import justitone.Event;
import justitone.Sequence;

//java intonation digital interface
public class JidiSequence {
    public List<JidiTrack> tracks;
    private List<JidiTrack> inUse;

    final int ppm = 480 * 4;

    public JidiSequence(Sequence sequence) {
        tracks = new ArrayList<>();
        inUse = new ArrayList<>();

        JidiTrack track = allocateTrack();
        inUse.add(track);

        loadSequence(new State(), BigFraction.ZERO, false, sequence, track);
    }

    public JidiTrack allocateTrack() {
        if (inUse.size() == tracks.size()) {
            JidiTrack track = new JidiTrack();
            tracks.add(track);
            return track;
        }
        else {
            return tracks.stream().filter(t -> !inUse.contains(t)).findFirst().get();
        }
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

            currentPos = currentPos.add(e.length().multiply(state.lengthMultiplier));
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
