package justitone.parser;

import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.math3.fraction.BigFraction;

import justitone.Event;
import justitone.Sequence;
import justitone.antlr.JIBaseVisitor;
import justitone.antlr.JILexer;
import justitone.antlr.JIParser;

public class Reader {
    final SequenceVisitor sequenceVisitor = new SequenceVisitor();
    final EventRepeatVisitor eventRepeatVisitor = new EventRepeatVisitor();
    final EventVisitor eventVisitor = new EventVisitor();
    final FractionVisitor fractionVisitor = new FractionVisitor();
    final PitchVisitor pitchVisitor = new PitchVisitor();
    final IntegerVisitor integerVisitor = new IntegerVisitor();

    public Sequence parse(String source) {
        CharStream charStream = CharStreams.fromString(source);
        JILexer lexer = new JILexer(charStream);
        TokenStream tokens = new CommonTokenStream(lexer);
        JIParser parser = new JIParser(tokens);

        Sequence traverseResult = sequenceVisitor.visit(parser.sequence());

        return traverseResult;
    }

    class SequenceVisitor extends JIBaseVisitor<Sequence> {
        @Override
        public Sequence visitSequence(JIParser.SequenceContext ctx) {
            int tempo = ctx.tempo.accept(integerVisitor);

            Sequence seq = new Sequence(tempo);
            
            ctx.eventRepeat().stream()
                             .flatMap(event -> event.accept(eventRepeatVisitor))
                             .forEach(e -> seq.addEvent(e));

            return seq;
        }
    }
    
    
    class EventRepeatVisitor extends JIBaseVisitor<Stream<Event>> {
        public Stream<Event> visitEventRepeat(JIParser.EventRepeatContext ctx) {
            int repeats = ctx.repeats == null ? 1 : ctx.repeats.accept(integerVisitor);

            Event event = ctx.event().accept(eventVisitor);

            Builder<Event> b = Stream.builder();
            
            for (int i = 0; i < repeats; i++) {
                b.accept(event);
            }
            
            return b.build();
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

            Sequence seq = new Sequence(0);
            
            ctx.eventRepeat().stream()
                             .map(event -> event.accept(eventVisitor))
                             .forEach(e -> seq.addEvent(e));
            
            
            return new Event.Tuple(length, seq);
        }

        @Override
        public Event visitEventBar(JIParser.EventBarContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);


            Sequence seq = new Sequence(0);
            
            ctx.eventRepeat().stream()
                             .map(event -> event.accept(eventVisitor))
                             .forEach(e -> seq.addEvent(e));
            
            return new Event.Bar(length, seq);
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
