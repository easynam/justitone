package justitone;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.math3.fraction.BigFraction;

public abstract class Event {
    public BigFraction length() {
        return BigFraction.ZERO;
    }
    
    public static abstract class SubSequence extends Event {
        public Sequence sequence() {
            throw new NotImplementedException();
        }
        public BigFraction eventLength() {
            throw new NotImplementedException();
        }
    }
    
    public static class Note extends Event {
        public Note(BigFraction length, BigFraction ratio) {
            this.ratio = ratio;
            this.length = length;
        }

        public BigFraction ratio;
        BigFraction length;

        @Override
        public BigFraction length() {
            return length;
        }
    }
    
    public static class Rest extends Event {
        public Rest(BigFraction length) {
            this.length = length;
        }

        BigFraction length;

        @Override
        public BigFraction length() {
            return length;
        }
        
    }

    public static class Hold extends Event {
        public Hold(BigFraction length) {
            this.length = length;
        }

        BigFraction length;

        @Override
        public BigFraction length() {
            return length;
        }
        
    }
    
    public static class Tuple extends SubSequence {
        public Tuple(BigFraction length, Sequence sequence) {
            this.length = length;
            this.sequence = sequence;
        }

        BigFraction length;
        Sequence sequence;

        @Override
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
    
    public static class Bar extends SubSequence {
        public Bar(BigFraction eventLength, Sequence sequence) {
            this.eventLength = eventLength;
            this.sequence = sequence;
        }

        BigFraction eventLength;
        Sequence sequence;

        @Override
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
    
    public static class Poly extends Event {
        public Poly(List<SubSequence> sequences) {
            this.sequences = sequences;
        }

        public List<SubSequence> sequences;
        
        public BigFraction length() {
            return sequences.stream().map(Event::length).max(Comparable::compareTo).get();
        }
    }
    
    public static class Modulation extends Event {
        public Modulation(BigFraction ratio) {
            this.ratio = ratio;
        }

        public BigFraction ratio;
    }
    
    public static class Jump extends Event {
        public Jump(int times) {
            this.times = times;
        }

        public int times;
    }
}
