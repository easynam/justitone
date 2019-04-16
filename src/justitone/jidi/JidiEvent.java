package justitone.jidi;

import java.util.List;

import justitone.TokenPos;

public abstract class JidiEvent {
    public long tick;
    
    public static class NoteOn extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": NoteOn";
        }

        NoteOn(long tick) {
            this.tick = tick;
        }
    }
    
    public static class NoteOff extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": NoteOff";
        }

        NoteOff(long tick) {
            this.tick = tick;
        }
    }
    
    public static class Pitch extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": Pitch [freq=" + freq + "]";
        }

        public float freq;

        Pitch(long tick, float freq) {
            this.tick = tick;
            this.freq = freq;
        }
    }
    
    public static class Instrument extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": Instrument [instrument=" + instrument + "]";
        }
        
        public int instrument;
        
        Instrument(long tick, int instrument) {
            this.tick = tick;
            this.instrument = instrument;
        }
    }
    
    public static class Token extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": Tokens";
        }
        
        public List<TokenPos> tokens;
        
        Token(long tick, List<TokenPos> tokens) {
            this.tick = tick;
            this.tokens = tokens;
        }
        
        public int start() {
            return tokens.stream().mapToInt(t -> t.start).min().orElse(0);
        }
        
        public int stop() {
            return tokens.stream().mapToInt(t -> t.stop).max().orElse(0);
        }
    }
}
