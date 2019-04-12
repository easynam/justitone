package justitone.parser;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.math3.fraction.BigFraction;

import justitone.Event;
import justitone.Event.SubSequence;
import justitone.Sequence;
import justitone.Song;
import justitone.antlr.JIBaseVisitor;
import justitone.antlr.JILexer;
import justitone.antlr.JIParser;

public class Reader {
    final SongVisitor songVisitor = new SongVisitor();
    final SequenceVisitor sequenceVisitor = new SequenceVisitor();
    final PolySequenceVisitor polySequenceVisitor = new PolySequenceVisitor();
    final SequenceItemVisitor sequenceItemVisitor = new SequenceItemVisitor();
    final EventVisitor eventVisitor = new EventVisitor();
    final FractionVisitor fractionVisitor = new FractionVisitor();
    final PitchVisitor pitchVisitor = new PitchVisitor();
    final IntegerVisitor integerVisitor = new IntegerVisitor();

    public Song parse(String source) {
        CharStream charStream = CharStreams.fromString(source);
        JILexer lexer = new JILexer(charStream);
        TokenStream tokens = new CommonTokenStream(lexer);
        JIParser parser = new JIParser(tokens);
        
        Song traverseResult = songVisitor.visit(parser.song());

        return traverseResult;
    }

    class SongVisitor extends JIBaseVisitor<Song> {
        @Override
        public Song visitSong(JIParser.SongContext ctx) {
            int tempo = ctx.tempo.accept(integerVisitor);
            Sequence seq = ctx.sequence().accept(sequenceVisitor);
            
            return new Song(seq, tempo);
        }
    }
    
    class SequenceVisitor extends JIBaseVisitor<Sequence> {
        @Override
        public Sequence visitSequence(JIParser.SequenceContext ctx) {
            Sequence seq = new Sequence();
            
            ctx.sequenceItem().stream()
                              .map(event -> event.accept(sequenceItemVisitor))
                              .forEach(f -> f.accept(seq));

            return seq;
        }
    }

    class PolySequenceVisitor extends JIBaseVisitor<List<Sequence>> {
        @Override
        public List<Sequence> visitPolySequence(JIParser.PolySequenceContext ctx) {
            List<Sequence> sequences = ctx.sequence().stream()
                                           .map(event -> event.accept(sequenceVisitor))
                                           .collect(Collectors.toList());
    
            return sequences;
        }
    }
    
    class SequenceItemVisitor extends JIBaseVisitor<Consumer<Sequence>> {
        public Consumer<Sequence> visitEventRepeat(JIParser.EventRepeatContext ctx) {
            int repeats = ctx.repeats == null ? 1 : ctx.repeats.accept(integerVisitor);

            Event event = ctx.event().accept(eventVisitor);

            
            return (s -> {
                for (int i = 0; i < repeats; i++) {
                    s.addEvent(event);
                }
            });
        }
        
        public Consumer<Sequence> visitJump(JIParser.JumpContext ctx) {
            BigFraction length = ctx.lengthMultiplier == null ? BigFraction.ONE : ctx.lengthMultiplier.accept(fractionVisitor);
            int repeats = ctx.times == null ? 1 : ctx.times.accept(integerVisitor);

            return (s -> {
                for (int i = 0; i < repeats; i++) {
                    Sequence bar = new Sequence(s);
                    
                    s.addEvent(new Event.Bar(length, bar));
                    
                    s = bar;
                }
            });
        }
    }

    class EventVisitor extends JIBaseVisitor<Event> {
        @Override
        public Event visitEventNote(JIParser.EventNoteContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);
            BigFraction ratio = ctx.pitch().accept(pitchVisitor);
            
            return new Event.Note(length, ratio);
        }

        @Override
        public Event visitEventRest(JIParser.EventRestContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);

            return new Event.Rest(length);
        }

        @Override
        public Event visitEventHold(JIParser.EventHoldContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);

            return new Event.Hold(length);
        }

        @Override
        public Event visitEventTuple(JIParser.EventTupleContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);
            List<Sequence> sequences = ctx.polySequence().accept(polySequenceVisitor);
            
            List<SubSequence> tuples = sequences.stream()
                                                .map(s -> new Event.Tuple(length, s))
                                                .collect(Collectors.toList());
            
            return new Event.Poly(tuples);
        }

        @Override
        public Event visitEventBar(JIParser.EventBarContext ctx) {
            BigFraction length = ctx.lengthMultiplier == null ? BigFraction.ONE : ctx.lengthMultiplier.accept(fractionVisitor);
            List<Sequence> sequences = ctx.polySequence().accept(polySequenceVisitor);

            List<SubSequence> bars = sequences.stream()
                                              .map(s -> new Event.Bar(length, s))
                                              .collect(Collectors.toList());
            
            return new Event.Poly(bars);
        }

        @Override
        public Event visitEventModulation(JIParser.EventModulationContext ctx) {
            BigFraction ratio = ctx.pitch().accept(pitchVisitor);

            return new Event.Modulation(ratio);
        }
    }
    
    class PitchVisitor extends JIBaseVisitor<BigFraction> {
        @Override
        public BigFraction visitPitchRatio(JIParser.PitchRatioContext ctx) {
            return ctx.ratio.accept(fractionVisitor);
        }

        @Override
        public BigFraction visitPitchAngle(JIParser.PitchAngleContext ctx) {
            int angle = ctx.angle().accept(integerVisitor);

            if (angle > 0)
                return new BigFraction(180, 180 - angle);
            return new BigFraction(180 + angle, 180);
        }

        @Override
        public BigFraction visitPitchMultiple(JIParser.PitchMultipleContext ctx) {
            return ctx.pitch().stream().map(p -> p.accept(pitchVisitor)).reduce(BigFraction::multiply).get();
        }

        @Override
        public BigFraction visitPitchPower(JIParser.PitchPowerContext ctx) {
            BigFraction pitch = ctx.pitch().accept(pitchVisitor);
            return pitch.pow(ctx.PLUS().size() + 1);
        }
    }

    class FractionVisitor extends JIBaseVisitor<BigFraction> {
        @Override
        public BigFraction visitFraction(JIParser.FractionContext ctx) {
            int numerator = integerVisitor.visit(ctx.integer(0));
            int denominator = 1;

            if (null != ctx.integer(1)) {
                denominator = integerVisitor.visit(ctx.integer(1));
            }

            return new BigFraction(numerator, denominator);
        }
    }

    class IntegerVisitor extends JIBaseVisitor<Integer> {
        @Override
        public Integer visitSigned(JIParser.SignedContext ctx) {
            return Integer.parseInt(ctx.getText());
        }

        @Override
        public Integer visitInteger(JIParser.IntegerContext ctx) {
            return Integer.parseInt(ctx.getText());
        }
    }
}
