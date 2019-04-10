package justitone.parser;

import java.util.function.Consumer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.math3.fraction.BigFraction;

import justitone.Track;
import justitone.antlr.JIBaseVisitor;
import justitone.antlr.JILexer;
import justitone.antlr.JIParser;

public class Reader {
	final SequenceVisitor sequenceVisitor = new SequenceVisitor();
	final EventVisitor eventVisitor = new EventVisitor();
	final FractionVisitor fractionVisitor = new FractionVisitor();
	final PitchVisitor pitchVisitor = new PitchVisitor();
	final IntegerVisitor integerVisitor = new IntegerVisitor();
	
	public Track parse(String source) {
		CharStream charStream = CharStreams.fromString(source);
		JILexer lexer = new JILexer(charStream);
		TokenStream tokens = new CommonTokenStream(lexer);
		JIParser parser = new JIParser(tokens);

		Track traverseResult = sequenceVisitor.visit(parser.sequence());

		return traverseResult;
	}

	class SequenceVisitor extends JIBaseVisitor<Track> {
		@Override
		public Track visitSequence(JIParser.SequenceContext ctx) {
			int tempo = ctx.tempo.accept(integerVisitor);
			
			Track track = new Track(tempo);
			
			ctx.eventRepeat().stream()
					         .map(event -> event.accept(eventVisitor))
					         .forEach(f -> f.accept(track));
			
			return track;
		}
	}

	class EventVisitor extends JIBaseVisitor<Consumer<Track>> {
		public Consumer<Track> visitEventRepeat(JIParser.EventRepeatContext ctx) {
			int repeats = ctx.repeats == null? 1 : ctx.repeats.accept(integerVisitor);
			
			Consumer<Track> event = ctx.event().accept(eventVisitor);
			
			return (t -> {
				for (int i = 0; i < repeats; i++) {
					event.accept(t);
				}
			});
		}
		
		@Override
    	public Consumer<Track> visitEventNote(JIParser.EventNoteContext ctx) {
			BigFraction length = ctx.length == null? BigFraction.ONE : ctx.length.accept(fractionVisitor);
			BigFraction ratio = ctx.pitch().accept(pitchVisitor);
			
    		return (t -> t.addNote(ratio, length));
    	}
		
		@Override
    	public Consumer<Track> visitEventRest(JIParser.EventRestContext ctx) {
			BigFraction length = ctx.length == null? BigFraction.ONE : ctx.length.accept(fractionVisitor);

    		return (t -> t.addNote(BigFraction.ZERO, length));
    	}

		@Override
    	public Consumer<Track> visitEventHold(JIParser.EventHoldContext ctx) {
			BigFraction length = ctx.length == null? BigFraction.ONE : ctx.length.accept(fractionVisitor);

    		return (t -> t.holdNote(length));
    	}
		
		@Override
    	public Consumer<Track> visitEventTuple(JIParser.EventTupleContext ctx) {
			BigFraction length = ctx.length == null? BigFraction.ONE : ctx.length.accept(fractionVisitor);
			
    	    return (track -> {
    			Track tuple = new Track(0);

    			ctx.eventRepeat().stream()
    					         .map(event -> event.accept(eventVisitor))
    					         .forEach(f -> f.accept(tuple));
    			
    			track.addTuple(tuple, length);
    	    });
    	}
		
		@Override
    	public Consumer<Track> visitEventModulation(JIParser.EventModulationContext ctx) {
			BigFraction ratio = ctx.pitch().accept(pitchVisitor);
			
    		return (t -> t.changeRoot(ratio));
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
			
			if(angle > 0)
			  return new BigFraction(180, 180-angle);
			return new BigFraction(180+angle, 180);
    	}
		
		@Override
    	public BigFraction visitPitchMultiple(JIParser.PitchMultipleContext ctx) {
			return ctx.pitch().stream().map(p -> p.accept(pitchVisitor)).reduce(BigFraction::multiply).get();
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
