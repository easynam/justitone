package justitone;

import org.apache.commons.math3.fraction.BigFraction;

public abstract class Event {
    public BigFraction length() {
        return BigFraction.ZERO;
    }
    
    public static class Note extends Event {
        public Note(BigFraction length, BigFraction ratio) {
            this.ratio = ratio;
            this.length = length;
        }

        public BigFraction ratio;
        BigFraction length;
        
        public BigFraction length() {
            return length;
        }
    }
    
    public static class Rest extends Event {
        public Rest(BigFraction length) {
            this.length = length;
        }

        BigFraction length;
        
        public BigFraction length() {
            return length;
        }
        
    }

    public static class Hold extends Event {
        public Hold(BigFraction length) {
            this.length = length;
        }

        BigFraction length;
        
        public BigFraction length() {
            return length;
        }
        
    }
    
    public static class Tuple extends Event {
        public Tuple(BigFraction length, Sequence track) {
            this.length = length;
            this.track = track;
        }

        BigFraction length;
        Sequence track;
        
        public BigFraction length() {
            return length;
        }
        
    }
    
    public static class Bar extends Event {
        public Bar(BigFraction eventLength, Sequence track) {
            this.eventLength = eventLength;
            this.track = track;
        }

        BigFraction eventLength;
        Sequence track;
        
        public BigFraction length() {
            return track.events.stream()
                               .map(e -> e.length())
                               .reduce(BigFraction::add)
                               .orElse(BigFraction.ZERO);
        }
    }
    
    public static class Modulation extends Event {
        public Modulation(BigFraction ratio) {
            this.ratio = ratio;
        }

        BigFraction ratio;
    }
}
