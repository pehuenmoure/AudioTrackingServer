import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
// import javax.comm.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

import java.nio.charset.StandardCharsets;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {
	static int RightMicIndex = 0;
	static int LeftMicIndex = 0;
	static int Threshold = 500;
	static int TIME_OUT = 2000;
	private static final int DATA_RATE = 9600;

	public static class SerialTest implements SerialPortEventListener {
		private BufferedReader input;

		public SerialTest(SerialPort serialPort){
			try {
				input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			} catch	(Exception e){
				System.out.println(e);
			}
		}

		/**
		 * Handle an event on the serial port. Read the data and print it.
		 */
		public synchronized void serialEvent(SerialPortEvent oEvent) {
			if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				try {
					String inputLine=input.readLine();
					System.out.println(inputLine);
				} catch (Exception e) {
					System.err.println(e.toString());
				}
			}
		}
	}

	static class MyHandler implements HttpHandler {
		OutputStream output;

		public MyHandler(OutputStream output){
			this.output = output;
		}

        @Override
        public void handle(HttpExchange t) throws IOException {
        	Map<String, String> params = queryToMap(t.getRequestURI().getQuery()); 
			System.out.println("param cmd=" + params.get("cmd"));
			writeToSerial(output, params.get("cmd") + "\n");

            String response = "success";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        public Map<String, String> queryToMap(String query){
		    Map<String, String> result = new HashMap<String, String>();
		    for (String param : query.split("&")) {
		        String pair[] = param.split("=");
		        if (pair.length>1) {
		            result.put(pair[0], pair[1]);
		        }else{
		            result.put(pair[0], "");
		        }
		    }
		    return result;
		}
    }

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}

	public static void writeToSerial(OutputStream output, String toWrite){
		try {
			byte[] b = toWrite.getBytes(StandardCharsets.US_ASCII);
			output.write(b);
			output.flush();
		} catch(Exception e){
			System.out.println(e);
		}
	}
	
	public static void main(String[] args) {

		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		String portName = "/dev/tty.usbmodem14111";

		SerialPort serialPort;
		OutputStream output = null;

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			System.out.println(currPortId.getName());
			// System.out.println(currPortId.getName().equals(portName));
			if(currPortId.getName().equals(portName)){
				System.out.println("found port");
				try{
					// open serial port, and use class name for the appName.
					serialPort = (SerialPort) currPortId.open("AudioTracker",
							TIME_OUT);

					Runtime.getRuntime().addShutdownHook(new Thread() {
					    public void run() { 
					    	System.out.println("Closing");
					    	serialPort.close();
					    }
					});

					// set port parameters
					serialPort.setSerialPortParams(DATA_RATE,
							SerialPort.DATABITS_8,
							SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);

					// add event listeners
					// SerialTest listener = new Main.SerialTest(serialPort);
					// serialPort.addEventListener(listener);
					// serialPort.notifyOnDataAvailable(true);

					output = serialPort.getOutputStream();

					try {
						HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
				        server.createContext("/test", new MyHandler(output));
				        server.setExecutor(null); // creates a default executor
				        server.start();
				    } catch(Exception e){
				    	System.out.println(e);
				    }

					writeToSerial(output, "p150\n");
					writeToSerial(output, "t045\n");
				} catch(Exception e){
					System.out.println(e);
				}
				
				break;
			}
		}


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

			float threshold1 = 800f;//4f * allmax1;
			float threshold2 = 800f;//4f * allmax2;

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
					if(output != null){
						System.out.println("Write: 55");
						writeToSerial(output, "p045\n");
					}
					lastDirection = 1;
				} else if(directionAverage < -0.2 && lastDirection != -1){
					System.out.println("-1");
					if(output != null){
						System.out.println("Write: 115");
						writeToSerial(output, "p135\n");
					}
					lastDirection = -1;
				}
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
