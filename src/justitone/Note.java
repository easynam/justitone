package justitone;

import java.nio.ByteBuffer;

import org.apache.commons.math3.fraction.Fraction;

public class Note {
    static final int fs = 44100;
    
	float freq;
    int samples;

    public Note(float freq, Fraction length) {
    	this.freq = freq;
    	this.samples = (int) (length.doubleValue() * fs);
    }
    
    public byte[] sin() {
    	ByteBuffer buffer = ByteBuffer.allocate(samples * 2);
    	
    	if (freq == 0) return buffer.array();
    	
        for (int i = 0; i < samples; i++) {
            double period = (double)fs / freq;
            double angle = 2.0 * Math.PI * i / period;
            buffer.putShort((short) (Math.sin(angle) * Short.MAX_VALUE * 0.25f));
        }
        
        return buffer.array();
    }

	@Override
	public String toString() {
		return "Note [freq=" + freq + ", samples=" + samples + "]";
	}
}
