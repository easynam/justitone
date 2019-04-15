package justitone.jidi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        track.events.stream().filter(e -> e instanceof JidiEvent.Instrument).collect(Collectors.toList());
        track.add(new JidiEvent.Instrument(currentPos.multiply(ppm).intValue(), state.instrument));
        
        for (Event e : sequence.contents()) {
            long tick = currentPos.multiply(ppm).longValue();

            if (e instanceof Event.Note) {
                track.add(new JidiEvent.Token(tick, e.tokens));
                
                if (noteOn) {
                    track.add(new JidiEvent.NoteOff(tick));
                }

                float freq = ((Event.Note) e).ratio().multiply(state.freqMultiplier).floatValue() * 440f;

                track.add(new JidiEvent.Pitch(tick, freq));
                track.add(new JidiEvent.NoteOn(tick));
                
                noteOn = true;
            }
            else if (e instanceof Event.Rest) {
                track.add(new JidiEvent.Token(tick, e.tokens));
                
                if (noteOn) {
                    track.add(new JidiEvent.NoteOff(tick));
                }

                noteOn = false;
            }
            else if (e instanceof Event.Hold) {
                track.add(new JidiEvent.Token(tick, e.tokens));
            }
            else if (e instanceof Event.Modulation) {
                state = state.multiplyFreq(((Event.Modulation) e).ratio());
            }
            else if (e instanceof Event.Instrument) {
                if (noteOn) {
                    track.add(new JidiEvent.NoteOff(tick));
                }
                
                Event.Instrument i = (Event.Instrument) e;
                track.add(new JidiEvent.Instrument(tick, i.instrument));
                
                state = state.changeInstrument(((Event.Instrument) e).instrument);
                
                noteOn = false;
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

        long tick = currentPos.multiply(ppm).longValue();
        
        track.add(new JidiEvent.Token(tick, Collections.emptyList()));
        if(noteOn) {
            track.add(new JidiEvent.NoteOff(currentPos.multiply(ppm).intValue()));
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
                             currentPos, false, sub.sequence(), allocateTrack(currentPos, end));
            }
        }
    }
    
    private class State {
        BigFraction lengthMultiplier;
        BigFraction freqMultiplier;
        int instrument;
        
        public State(BigFraction lengthMultiplier, BigFraction freqMultiplier, int instrument) {
            this.lengthMultiplier = lengthMultiplier;
            this.freqMultiplier = freqMultiplier;
            this.instrument = instrument;
        }
        
        public State(State state) {
            this(state.lengthMultiplier, state.freqMultiplier, state.instrument);
        }
        
        public State() {
            this(BigFraction.ONE, BigFraction.ONE, 0);
        }
        
        public State multiplyLength(BigFraction multiplier) {
            State state = new State(this);
            state.lengthMultiplier = lengthMultiplier.multiply(multiplier);
            return state;
        }
        
        public State multiplyFreq(BigFraction multiplier) {
            State state = new State(this);
            state.freqMultiplier = freqMultiplier.multiply(multiplier);
            return state;
        }
        
        public State changeInstrument(int instrument) {
            State state = new State(this);
            state.instrument = instrument;
            return state;
        }
    }
}
