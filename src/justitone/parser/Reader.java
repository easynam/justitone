package justitone.parser;

import java.util.function.Consumer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.math3.fraction.Fraction;

import justitone.Track;
import justitone.antlr.JIBaseVisitor;
import justitone.antlr.JILexer;
import justitone.antlr.JIParser;

public class Reader {
	final SequenceVisitor sequenceVisitor = new SequenceVisitor();
	final EventVisitor eventVisitor = new EventVisitor();
	final FractionVisitor fractionVisitor = new FractionVisitor();
	final IntegerVisitor integerVisitor = new IntegerVisitor();
	
	public Track parse(String someLangSourceCode) {
		CharStream charStream = CharStreams.fromString(someLangSourceCode);
		JILexer lexer = new JILexer(charStream);
		TokenStream tokens = new CommonTokenStream(lexer);
		JIParser parser = new JIParser(tokens);

		Track traverseResult = sequenceVisitor.visit(parser.sequence());

		return traverseResult;
	}

	class SequenceVisitor extends JIBaseVisitor<Track> {
		@Override
		public Track visitSequence(JIParser.SequenceContext ctx) {

			Track track = new Track();
			
			ctx.event().stream()
					   .map(event -> event.accept(eventVisitor))
					   .forEach(f -> f.accept(track));
			
			return track;
		}
	}

	class EventVisitor extends JIBaseVisitor<Consumer<Track>> {
		@Override
    	public Consumer<Track> visitNote(JIParser.NoteContext ctx) {
			Fraction length = ctx.length.accept(fractionVisitor);
			Fraction ratio = ctx.ratio.accept(fractionVisitor);
			
    		return (t -> t.addNote(ratio, length));
    	}
		
		@Override
    	public Consumer<Track> visitModulation(JIParser.ModulationContext ctx) {
			Fraction ratio = ctx.ratio.accept(fractionVisitor);
			
    		return (t -> t.changeRoot(ratio));
    	}
	}
	
	class FractionVisitor extends JIBaseVisitor<Fraction> {
		@Override
    	public Fraction visitFraction(JIParser.FractionContext ctx) {
			int numerator = integerVisitor.visit(ctx.integer(0));
			int denominator = 1;
			
			if (null != ctx.integer(1)) {
				denominator = integerVisitor.visit(ctx.integer(1));
			}
			
			return new Fraction(numerator, denominator);
    	}
	}
	
	class IntegerVisitor extends JIBaseVisitor<Integer> {
		@Override
    	public Integer visitInteger(JIParser.IntegerContext ctx) {
			return Integer.parseInt(ctx.getText());
    	}
	}
}
