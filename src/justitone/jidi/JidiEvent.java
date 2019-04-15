package justitone.jidi;

import justitone.TokenPos;

public abstract class JidiEvent {
    public long tick;
    public TokenPos tokenPos;
    
    public static class NoteOn extends JidiEvent {
        @Override
        public String toString() {
            return "NoteOn [freq=" + freq + ", tick=" + tick + "]";
        }

        public float freq;

        NoteOn(int tick, float freq, TokenPos pos) {
            this.tick = tick;
            this.freq = freq;
            this.tokenPos = pos;
        }
    }
    
    public static class NoteOff extends JidiEvent {
        @Override
        public String toString() {
            return "NoteOff [tick=" + tick + "]";
        }

        NoteOff(int tick, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
        }
    }
    
    public static class Instrument extends JidiEvent {
        public int instrument;
        
        Instrument(int tick, int instrument, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
            this.instrument = instrument;
        }
    }
    
    public static class Empty extends JidiEvent {
        Empty(int tick, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
        }
    }
}
