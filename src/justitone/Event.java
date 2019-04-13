package justitone;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.math3.fraction.BigFraction;

public abstract class Event {
    public TokenPos tokenPos;
    
    public Event withTokenPos(TokenPos pos) {
        this.tokenPos = pos;
        return this;
    }
    
    public BigFraction length() {
        return BigFraction.ZERO;
    }
    
    public SubSequence chop(BigFraction toLength) {
        return new Event.Bar(toLength.divide(length()), BigFraction.ONE, new Sequence(this));
    }
    
    public static abstract class SubSequence extends Event {
        BigFraction ratio;
        
        public BigFraction ratio() {
            return ratio;
        }
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

        public Note() {
            this(BigFraction.ONE, BigFraction.ONE);
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
        
        public Rest() {
            this(BigFraction.ONE);
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

        public Hold() {
            this(BigFraction.ONE);
        }
        
        BigFraction length;

        @Override
        public BigFraction length() {
            return length;
        }
        
    }
    
    public static class Tuple extends SubSequence {
        public Tuple(Sequence sequence) {
            this.length = BigFraction.ONE;
            this.ratio = BigFraction.ONE;
            this.sequence = sequence;
        }
        
        public Tuple(BigFraction length, BigFraction ratio, Sequence sequence) {
            this.length = length;
            this.ratio = ratio;
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

        @Override
        public SubSequence chop(BigFraction toLength) {
            BigFraction newTupleLength = toLength.divide(length);
            toLength = toLength.divide(eventLength());
            BigFraction total = BigFraction.ZERO;
            
            Sequence seq = new Sequence();
            
            for (Event e : sequence.events) {
                BigFraction start = total;
                total = total.add(e.length());
                
                if (total.compareTo(toLength) >= 0) {
                    seq.addEvent(e.chop(toLength.subtract(start)));

                    break;
                }
                
                seq.addEvent(e);
            }
            
            return new Tuple(newTupleLength, ratio, seq);
        }
    }
    
    public static class Bar extends SubSequence {
        public Bar(Sequence sequence) {
            this.eventLength = BigFraction.ONE;
            this.ratio = BigFraction.ONE;
            this.sequence = sequence;
        }
        
        public Bar(BigFraction eventLength, BigFraction ratio, Sequence sequence) {
            this.eventLength = eventLength;
            this.ratio = ratio;
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
        
        @Override
        public SubSequence chop(BigFraction toLength) {
            toLength = toLength.divide(eventLength());
            
            BigFraction total = BigFraction.ZERO;
            
            Sequence seq = new Sequence();
            
            for (Event e : sequence.events) {
                BigFraction start = total;
                total = total.add(e.length());
                
                if (total.compareTo(toLength) >= 0) {
                    seq.addEvent(e.chop(toLength.subtract(start)));
                    
                    break;
                }
                
                seq.addEvent(e);
            }
            
            return new Bar(eventLength, ratio, seq);
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
        
        @Override
        public SubSequence chop(BigFraction toLength) {
            List<SubSequence> seqs = sequences.stream()
                                              .map(s -> s.chop(toLength))
                                              .collect(Collectors.toList());
            
            return new Bar(new Sequence(new Poly(seqs)));
        }
    }
    
    public static class Modulation extends Event {
        public Modulation(BigFraction ratio) {
            this.ratio = ratio;
        }

        public BigFraction ratio;
    }
}
