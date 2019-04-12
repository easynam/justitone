package justitone.jidi;

import justitone.TokenPos;

public abstract class JidiEvent {
    public long tick;
    public TokenPos tokenPos;
    
    public static class NoteOn extends JidiEvent {
        @Override
        public String toString() {
            return "NoteOn [tick=" + tick + "]";
        }

        NoteOn(int tick, TokenPos pos) {
            this.tick = tick;
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
    
    public static class Pitch extends JidiEvent {
        @Override
        public String toString() {
            return "Pitch [freq=" + freq + ", tick=" + tick + "]";
        }

        public float freq;
        
        Pitch(int tick, float freq, TokenPos pos) {
            this.tick = tick;
            this.freq = freq;
            this.tokenPos = pos;
        }
    }
    
    public static class Empty extends JidiEvent {
        Empty(int tick, TokenPos pos) {
            this.tick = tick;
            this.tokenPos = pos;
        }
    }
}
