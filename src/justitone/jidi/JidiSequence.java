package justitone.jidi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.math3.fraction.BigFraction;

import justitone.Event;
import justitone.Sequence;
import justitone.Song;

//java intonation digital interface
public class JidiSequence {
    public List<JidiTrack> tracks;
    private Map<JidiTrack, Periods> used;

    public final int ppm;
    public final int bpm;

    public JidiSequence(Song song, int ppq) {
        bpm = song.bpm;
        ppm = ppq*4;
        
        tracks = new ArrayList<>();
        used = new HashMap<>();

        JidiTrack track = allocateTrack(BigFraction.ZERO, song.sequence.length());

        loadSequence(new State(), BigFraction.ZERO, false, song.sequence, track);
    }

    public JidiTrack allocateTrack(BigFraction start, BigFraction end) {
        Optional<JidiTrack> maybeTrack = tracks.stream()
                                               .filter(t -> used.get(t).canAllocate(start, end))
                                               .findFirst();
        if (maybeTrack.isPresent()) {
            JidiTrack track = maybeTrack.get();
            
            used.get(track).allocate(start, end);
            
            return track;
        }
        else {
            JidiTrack track = new JidiTrack(tracks.size());
            Periods periods = new Periods();
            periods.allocate(start, end);
            tracks.add(track);
            used.put(track, periods);
            return track;
        }
    }
    
    public void loadSequence(State state, BigFraction currentPos, boolean noteOn, Sequence sequence, JidiTrack track) {
        Event last = null;
        
        for (Event e : sequence.contents()) {
            last = e;
            
            int tick = currentPos.multiply(ppm).intValue();

            if (e instanceof Event.Note) {
                noteOn = true;

                float freq = ((Event.Note) e).ratio.multiply(state.freqMultiplier).floatValue() * 440f;

                track.add(new JidiEvent.NoteOn(tick, e.tokenPos));
                track.add(new JidiEvent.Pitch(tick, freq, e.tokenPos));
            }
            else if (e instanceof Event.Rest) {
                if (noteOn) {
                    track.add(new JidiEvent.NoteOff(tick, e.tokenPos));
                }
                else {
                    track.add(new JidiEvent.Empty(tick, e.tokenPos));
                }

                noteOn = false;
            }
            else if (e instanceof Event.Hold) {
                track.add(new JidiEvent.Empty(tick, e.tokenPos));

                noteOn = false;
            }
            else if (e instanceof Event.Modulation) {
                state = state.multiplyFreq(((Event.Modulation) e).ratio);
            }
            else if (e instanceof Event.SubSequence) {
                Event.SubSequence sub = (Event.SubSequence) e;
                loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio()), 
                             currentPos, noteOn, sub.sequence(), track);
            }
            else if (e instanceof Event.Poly) {
                Event.Poly poly = (Event.Poly) e;
                loadPoly(state, currentPos, noteOn, poly.sequences, track);
            }

            currentPos = currentPos.add(e.length().multiply(state.lengthMultiplier));
        }
        
        if(noteOn) {
            track.add(new JidiEvent.NoteOff(currentPos.multiply(ppm).intValue(), last.tokenPos));
        }
    }
    
    public void loadPoly(State state, BigFraction currentPos, boolean noteOn, List<Event.SubSequence> subs, JidiTrack track) {
        Event.SubSequence sub = subs.get(0);
        
        loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio()), 
                     currentPos, noteOn, subs.get(0).sequence(), track);
        
        if (subs.size() > 1) {
            for (int i = 1; i < subs.size(); i++) {
                sub = subs.get(i);
                
                BigFraction end = currentPos.add(sub.length().multiply(state.lengthMultiplier));
                
                loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio()), 
                             currentPos, noteOn, sub.sequence(), allocateTrack(currentPos, end));
            }
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
