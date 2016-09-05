import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class MicReader {
	public static int readBlock(TargetDataLine line, AudioFormat format) throws LineUnavailableException{
		byte[] data = new byte[1024];
		short[] shorts = new short[data.length/2];

		// line.open(format);
		// line.start();

		line.read(data, 0, data.length);
		ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
			
		short max = 0;
		for(int i = 0; i < shorts.length; i++){
			if(shorts[i] > max)
				max = shorts[i];
		}
		
		// line.stop();
		return max;
    }
}
