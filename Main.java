import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class Main {
	static int RightMicIndex = 0;
	static int LeftMicIndex = 0;
	static int Threshold = 500;
	
	public static void main(String[] args) {
		AudioFormat format = new AudioFormat(96000.0f, 16, 1, true, false);
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); 

		// print all audio devices
		// you will need to manually select the correct microphone
		Mixer.Info[] infos = AudioSystem.getMixerInfo();
		// System.out.println(infos);
		for(int i = 0; i < infos.length; i++){
			Mixer mixer = AudioSystem.getMixer(infos[i]);
			Line.Info[] lines = mixer.getTargetLineInfo();
			for(int j = 0; j < lines.length; j++){
				System.out.println(i + ": " + lines[j]);
				if ((lines[j]+ "").equals("interface TargetDataLine supporting 4 audio formats, and buffers of at least 32 bytes")){
					if (RightMicIndex != i || LeftMicIndex != i){
						if(RightMicIndex == 0){
							RightMicIndex = i;
						}else{
							LeftMicIndex = i;
						}
					}
				}
			}
		}
		RightMicIndex = 3;
		LeftMicIndex = 4;

		if(args.length > 0){
			RightMicIndex = Integer.parseInt(args[0]);
			LeftMicIndex = 0;
		}

		System.out.println();
		System.out.println("RightMicIndex: " + RightMicIndex + "\t" + "LeftMicIndex: " + LeftMicIndex);

		// if (!AudioSystem.isLineSupported(AudioSystem.getMixer(infos[6]).getTargetLineInfo()[1])) {
			// System.out.print(info);
		//} 
		try {
			int ticks = 300;

			TargetDataLine line1 = getLine(format, infos[RightMicIndex]);
			TargetDataLine line2 = getLine(format, infos[LeftMicIndex]);

			line1.open(format);
			line1.start();

			line2.open(format);
			line2.start();

			System.out.println("Initializing... Please don't make any sound.");

			for(int i = 0; i < ticks; i++){
				int max1 = MicReader.readBlock(line1, format);
				int max2 = MicReader.readBlock(line2, format);
			}

			System.out.println("Doing threshold now.");

			int sum1 = 0;
			int sum2 = 0;
			int allmax1 = 0;
			int allmax2 = 0;
			for(int i = 0; i < ticks; i++){
				int max1 = MicReader.readBlock(line1, format);
				int max2 = MicReader.readBlock(line2, format);

				sum1 += max1;
				sum2 += max2;

				allmax1 = Math.max(max1, allmax1);
				allmax2 = Math.max(max2, allmax2);
			}

			System.out.println("avg1: " + ((float)sum1 / ticks));
			System.out.println("avg2: " + ((float)sum2 / ticks));
			System.out.println("max1: " + allmax1);
			System.out.println("max2: " + allmax2);

			float threshold1 = 4f * allmax1;
			float threshold2 = 4f * allmax2;

			float[] directionBuffer = new float[150];
			int directionBufferIndex = 0;
			int lastDirection = 0;

			while(true){
				float max1 = MicReader.readBlock(line1, format) - threshold1;
				// float max2 = MicReader.readBlock(line2, format) - threshold2;
				float max2 = 0;
				if(LeftMicIndex > 0){
					max2 = MicReader.readBlock(line2, format) - threshold2;
				}

				float difference = max1 - max2;

				String outVal = "";
				if(max1	> 0 && max1 > max2){
					// System.out.println("1");
					outVal = "1";
					directionBuffer[directionBufferIndex] = 1;
				} else if(max2 > 0 && max2 > max1){
					// System.out.println("-1");
					outVal = "-1";
					directionBuffer[directionBufferIndex] = -1;
				} else{
					// System.out.println("0");
					outVal = "0";
					directionBuffer[directionBufferIndex] = 0;
				}

				directionBufferIndex = (directionBufferIndex + 1) % directionBuffer.length;

				float directionAverage = 0;
				for(int i = 0; i < directionBuffer.length; i++){
					directionAverage += directionBuffer[i];
				}
				directionAverage /= directionBuffer.length;

				// System.out.println(max1 + "\t" + max2 + "\t" + directionAverage);
				if(directionAverage > 0.2 && lastDirection != 1){
					System.out.println("1");
					lastDirection = 1;
				} else if(directionAverage < -0.2 && lastDirection != -1){
					System.out.println("-1");
					lastDirection = -1;
				}

				// System.out.println(max1 + "\t" + max2 + "\t" + outVal);



				// if (max1 >= 5000 && max2 >= 5000){
				// 	System.out.println(0);
				// }else if (max1 >= 5000){
				// 	System.out.println(-1);
				// }else if (max2 >= 5000){
				// 	System.out.println(1);
				// }else{
				// 	System.out.println(0);
				// }
				// if (difference > Threshold){
				// 	System.out.println(1);
				// }else if (difference < -1 * Threshold){
				// 	System.out.println(-1);
				// }else{
				// 	System.out.println(0);
				// }
			}
		} catch (LineUnavailableException ex) {
			System.out.println(ex);
		}
	}
	
	static TargetDataLine getLine(AudioFormat format, Mixer.Info info) throws LineUnavailableException{
		TargetDataLine line = AudioSystem.getTargetDataLine(format, info);
		return line;
	}
}
