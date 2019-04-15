package justitone.jidi;

import justitone.TokenPos;

public abstract class JidiEvent {
    public long tick;
    public TokenPos tokenPos;
    
    public static class NoteOn extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": NoteOn";
        }

        NoteOn(int tick, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
        }
    }
    
    public static class NoteOff extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": NoteOff";
        }

        NoteOff(int tick, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
        }
    }
    
    public static class Pitch extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": Pitch [freq=" + freq + "]";
        }

        public float freq;

        Pitch(int tick, float freq, TokenPos pos) {
            this.tick = tick;
            this.freq = freq;
            this.tokenPos = pos;
        }
    }
    
    public static class Instrument extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": Instrument [instrument=" + instrument + "]";
        }
        
        public int instrument;
        
        Instrument(int tick, int instrument, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
            this.instrument = instrument;
        }
    }
    
    public static class Empty extends JidiEvent {
        @Override
        public String toString() {
            return tick + ": Empty";
        }
        
        Empty(int tick, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
        }
    }
}
