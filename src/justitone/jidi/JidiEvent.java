package justitone.jidi;

public abstract class JidiEvent {
    int tick;
    
    public static class NoteOn extends JidiEvent {
        NoteOn(int pos) {
            this.tick = pos;
        }
    }
    
    public static class NoteOff extends JidiEvent {
        NoteOff(int pos) {
            this.tick = pos;
        }
    }
    
    public static class Pitch extends JidiEvent {
        float freq;
        
        Pitch(int pos, float freq) {
            this.tick = pos;
            this.freq = freq;
        }
    }
}
