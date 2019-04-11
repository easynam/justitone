package justitone;

import org.apache.commons.math3.fraction.BigFraction;

public abstract class Event {
    public BigFraction length() {
        return BigFraction.ZERO;
    }
    
    public static interface SubSequence {
        public Sequence sequence();
        public BigFraction eventLength();
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
    
    public static class Tuple extends Event implements SubSequence {
        public Tuple(BigFraction length, Sequence sequence) {
            this.length = length;
            this.sequence = sequence;
        }

        BigFraction length;
        Sequence sequence;
        
        public BigFraction length() {
            return length;
        }

        @Override
        public Sequence sequence() {
            return sequence;
        }

        @Override
        public BigFraction eventLength() {
            return length.divide(sequence.length());
        }
    }
    
    public static class Bar extends Event implements SubSequence {
        public Bar(BigFraction eventLength, Sequence sequence) {
            this.eventLength = eventLength;
            this.sequence = sequence;
        }

        BigFraction eventLength;
        Sequence sequence;
        
        public BigFraction length() {
            return sequence.length().multiply(eventLength);
        }

        @Override
        public Sequence sequence() {
            return sequence;
        }

        @Override
        public BigFraction eventLength() {
            return eventLength;
        }
    }
    
    public static class Modulation extends Event {
        public Modulation(BigFraction ratio) {
            this.ratio = ratio;
        }

        BigFraction ratio;
    }
}
