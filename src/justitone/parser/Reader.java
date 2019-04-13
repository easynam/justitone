package justitone.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.math3.fraction.BigFraction;

import justitone.Event;
import justitone.Event.SubSequence;
import justitone.Sequence;
import justitone.Song;
import justitone.TokenPos;
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
    
    public static TokenPos tokenPos(ParserRuleContext ctx) {
        return new TokenPos(ctx.start.getStartIndex(), ctx.stop.getStopIndex());
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
                    System.out.println("adding " + event);
                    s.addEvent(event);
                }
            });
        }
        
        public Consumer<Sequence> visitJump(JIParser.JumpContext ctx) {
            BigFraction length = ctx.lengthMultiplier == null ? BigFraction.ONE : ctx.lengthMultiplier.accept(fractionVisitor);
            BigFraction ratio = ctx.pitch() == null ? BigFraction.ONE : ctx.pitch().accept(pitchVisitor);
            int repeats = ctx.times == null ? 1 : ctx.times.accept(integerVisitor);

            return (s -> {
                for (int i = 0; i < repeats; i++) {
                    Sequence bar = new Sequence(s);
                    
                    s.addEvent(new Event.Bar(length, ratio, bar));
                    
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
            
            return new Event.Note(length, ratio).withTokenPos(tokenPos(ctx));
        }

        @Override
        public Event visitEventRest(JIParser.EventRestContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);

            return new Event.Rest(length).withTokenPos(tokenPos(ctx));
        }

        @Override
        public Event visitEventHold(JIParser.EventHoldContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);

            return new Event.Hold(length).withTokenPos(tokenPos(ctx));
        }

        @Override
        public Event visitEventTuple(JIParser.EventTupleContext ctx) {
            BigFraction length = ctx.length == null ? BigFraction.ONE : ctx.length.accept(fractionVisitor);
            BigFraction ratio = ctx.pitch() == null ? BigFraction.ONE : ctx.pitch().accept(pitchVisitor);
            List<Sequence> sequences = ctx.polySequence().accept(polySequenceVisitor);
            
            List<SubSequence> tuples = sequences.stream()
                                                .map(s -> new Event.Tuple(length, ratio, s))
                                                .collect(Collectors.toList());
            
            return new Event.Poly(ratio, tuples);
        }

        @Override
        public Event visitEventFill(JIParser.EventFillContext ctx) {
            //this should go somewhere else at this point
            BigFraction length = ctx.lengthMultiplier == null ? BigFraction.ONE : ctx.lengthMultiplier.accept(fractionVisitor);
            BigFraction ratio = ctx.pitch() == null ? BigFraction.ONE : ctx.pitch().accept(pitchVisitor);

            Sequence start = ctx.start == null ? new Sequence() : ctx.start.accept(sequenceVisitor);
            Sequence loop = ctx.loop.accept(sequenceVisitor);
            
            if (loop.length().equals(BigFraction.ZERO)) {
                throw new RuntimeException("Empty loop in fill at "+ ctx.loop.start.getStartIndex());
            }
            
            if (start.length().compareTo(length) >= 0) {
                return new Event.Bar(start).chop(length);
            }
            
            BigFraction total = start.length();
            Sequence seq = new Sequence();
            
            System.out.println("CREATED SEQ------"+seq);

            seq.addEvent(new Event.Bar(start));

            while(true) {
                BigFraction prev = total;
                total = total.add(loop.length());

                if (total.compareTo(length) >= 0) {
                    BigFraction toLength = length.subtract(prev);

                    seq.addEvent(new Event.Bar(loop).chop(toLength));
                    
                    break;
                }

                seq.addEvent(new Event.Bar(loop));
            }
            
            return new Event.Bar(BigFraction.ONE, ratio, seq);
        }

        @Override
        public Event visitEventBar(JIParser.EventBarContext ctx) {
            BigFraction length = ctx.lengthMultiplier == null ? BigFraction.ONE : ctx.lengthMultiplier.accept(fractionVisitor);
            BigFraction ratio = ctx.pitch() == null ? BigFraction.ONE : ctx.pitch().accept(pitchVisitor);
            List<Sequence> sequences = ctx.polySequence().accept(polySequenceVisitor);

            List<SubSequence> bars = sequences.stream()
                                              .map(s -> new Event.Bar(length, ratio, s))
                                              .collect(Collectors.toList());
            
            return new Event.Poly(ratio, bars);
        }

        @Override
        public Event visitEventModGroup(JIParser.EventModGroupContext ctx) {
            BigFraction length = ctx.lengthMultiplier == null ? BigFraction.ONE : ctx.lengthMultiplier.accept(fractionVisitor);
            BigFraction ratio = ctx.pitch() == null ? BigFraction.ONE : ctx.pitch().accept(pitchVisitor);
            List<Sequence> sequences = ctx.polySequence().accept(polySequenceVisitor);

            List<SubSequence> bars = sequences.stream()
                                              .map(s -> s.events.stream()
                                                                .flatMap(e -> Stream.concat(Stream.of(e), Stream.of(new Event.Modulation(e.ratio()))))
                                                                .collect(Collectors.toList()))
                                              .map(s -> new Event.Bar(length, ratio, new Sequence(s)))
                                              .collect(Collectors.toList());
            
            return new Event.Poly(ratio, bars);
        }
        
        @Override
        public Event visitEventChord(JIParser.EventChordContext ctx) {
            Event event = ctx.event() == null ? new Event.Note() : ctx.event().accept(eventVisitor);
            BigFraction ratio = BigFraction.ONE;
            
            List<BigFraction> pitches = ctx.pitch().stream()
                                                   .map(p -> p.accept(pitchVisitor))
                                                   .collect(Collectors.toList());
            
            List<SubSequence> events = new ArrayList<>();
            
            for (BigFraction p : pitches) {
              ratio = ratio.multiply(p);
              events.add(new Event.Bar(BigFraction.ONE, ratio, new Sequence(event)));
          }
            
            return new Event.Poly(event.ratio(), events);
        }

        @Override
        public Event visitEventModulation(JIParser.EventModulationContext ctx) {
            BigFraction ratio = ctx.pitch().accept(pitchVisitor);

            return new Event.Modulation(ratio).withTokenPos(tokenPos(ctx));
        }
    }
    
    class PitchVisitor extends JIBaseVisitor<BigFraction> {
        BigFraction invert(BigFraction ratio, boolean shouldInvert) {
            if (shouldInvert) {
                return ratio.reciprocal();
            }
            else return ratio;
        }
        
        @Override
        public BigFraction visitPitchRatio(JIParser.PitchRatioContext ctx) {
            boolean invert = ctx.MINUS() != null;
            
            return invert(ctx.ratio.accept(fractionVisitor), invert);
        }
        
        @Override
        public BigFraction visitPitchSuperparticular(JIParser.PitchSuperparticularContext ctx) {
            int numerator = ctx.integer().accept(integerVisitor);
            boolean invert = ctx.MINUS() != null;
            
            if(numerator <= 1) {
                throw new RuntimeException("Superparticular numerator cant be less than 2");
            }
            
            return invert(new BigFraction(numerator, numerator - 1), invert);
        }


        @Override
        public BigFraction visitPitchAngle(JIParser.PitchAngleContext ctx) {
            int angle = ctx.integer().accept(integerVisitor);
            boolean invert = ctx.MINUS() != null;

            return invert(new BigFraction(180, 180 - angle), invert);
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
            int numerator = ctx.num == null ? 1 : ctx.num.accept(integerVisitor);
            int denominator = ctx.den == null ? 1 : ctx.den.accept(integerVisitor);

            return new BigFraction(numerator, denominator);
        }
    }

    class IntegerVisitor extends JIBaseVisitor<Integer> {
        @Override
        public Integer visitInteger(JIParser.IntegerContext ctx) {
            return Integer.parseInt(ctx.getText());
        }
    }
}
