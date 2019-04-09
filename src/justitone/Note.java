package justitone;

import java.nio.ByteBuffer;

import org.apache.commons.math3.fraction.BigFraction;
public class Note {
    static final int fs = 44100;
    
	BigFraction offset;
	BigFraction length;

    public Note(BigFraction offset, BigFraction length) {
    	this.offset = offset;
    	this.length = length;
    }
    
    public byte[] sin() {
    	float freq = offset.floatValue()*440f;
    	
    	int samples = samples();
    	
    	ByteBuffer buffer = ByteBuffer.allocate(samples * 2);
    	
    	if (freq == 0) return buffer.array();
    	
        for (int i = 0; i < samples; i++) {
            double period = (double)fs / freq;
            double angle = 2.0 * Math.PI * i / period;
            buffer.putShort((short) (Math.sin(angle) * Short.MAX_VALUE * 0.25f));
        }
        
        return buffer.array();
    }
    
    public int samples() {
    	return (int) (length.doubleValue() * fs);
    }

	@Override
	public String toString() {
		return "Note [offset=" + offset + ", length=" + length + "]";
	}
}
