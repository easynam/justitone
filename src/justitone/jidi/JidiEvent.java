package justitone.jidi;

public abstract class JidiEvent {
    int tick;
    
    public static class NoteOn extends JidiEvent {
        @Override
        public String toString() {
            return "NoteOn [tick=" + tick + "]";
        }

        NoteOn(int pos) {
            this.tick = pos;
        }
    }
    
    public static class NoteOff extends JidiEvent {
        @Override
        public String toString() {
            return "NoteOff [tick=" + tick + "]";
        }

        NoteOff(int pos) {
            this.tick = pos;
        }
    }
    
    public static class Pitch extends JidiEvent {
        @Override
        public String toString() {
            return "Pitch [freq=" + freq + ", tick=" + tick + "]";
        }

        float freq;
        
        Pitch(int pos, float freq) {
            this.tick = pos;
            this.freq = freq;
        }
    }
}
