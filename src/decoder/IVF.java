package decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class IVF {

	private int headerLength;
	private int width,height;
	private int framerate;
	private int numFrames;
	private int timescale;
	private long curFrameSize;
	private FileInputStream in;
	private String name;
	
	public IVF(File file){
		try {
			
			in = new FileInputStream(file);
			name = file.getName();
			readFileHeader();


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public int getHeaderLength() {
		return headerLength;
	}


	public int getWidth() {
		return width;
	}


	public int getHeight() {
		return height;
	}


	public int getFramerate() {
		return framerate;
	}


	public int getNumFrames() {
		return numFrames;
	}


	public int getTimescale() {
		return timescale;
	}


	public long getCurFrameSize() {
		return curFrameSize;
	}


	public String getName() {
		return name;
	}

	
	private void readFileHeader() throws IOException{
		char[] signature = new char[4];
		int version;
		char[] codec = new char[4];
		
		
		

		signature[0] = (char) (in.read()& 0xFF);
		signature[1] = (char) (in.read()& 0xFF);
		signature[2] = (char) (in.read()& 0xFF);
		signature[3] = (char) (in.read()& 0xFF);

		version = in.read() & 0xFF;
		version |= (in.read()&0xFF) << 8;

		headerLength = in.read() & 0xFF;
		headerLength |= (in.read()& 0xFF) << 8;

		codec[0] = (char) (in.read()& 0xFF); 
		codec[1] = (char) (in.read()& 0xFF); 
		codec[2] = (char) (in.read()& 0xFF); 
		codec[3] = (char) (in.read()& 0xFF); 

		width = in.read() & 0xFF;
		width |= (in.read()& 0xFF) << 8;             

		height = in.read() & 0xFF;    
		height |= (in.read()& 0xFF) << 8;             

		framerate = in.read() & 0xFF;    
		framerate |= (in.read()& 0xFF) << 8;             
		framerate |= (in.read()& 0xFF) << 16;
		framerate |= (in.read()& 0xFF) << 24;

		timescale =  (in.read()&0xFF);
		timescale |= (in.read()&0xFF) << 8;
		timescale |= (in.read()&0xFF) << 16;
		timescale |= (in.read()&0xFF) << 24;

		numFrames =  (in.read()&0xFF);
		numFrames |= (in.read()&0xFF) << 8;
		numFrames |= (in.read()&0xFF) << 16;
		numFrames |= (in.read()&0xFF) << 24;
		
		in.read(); // unused byte
		in.read(); // unused byte
		in.read(); // unused byte
		in.read(); // unused byte

	}
	
	public long readFrameHeader() throws IOException {
		
		
		if(in.available()>0) {
			System.out.println("IVF FRAME HEADER: ");
			curFrameSize = (in.read()&0xFF) | (in.read()&0xFF) << 8 | (in.read()&0xFF) << 16 | (in.read()&0xFF) << 24;
			long timestamp = 0;
			for (int i = 0; i < 8; i++)
				timestamp |= (in.read()&0xFF) << (i << 3);
			
			System.out.println("Framesize: " + curFrameSize + " bytes");
			System.out.println("Timestamp: " + timestamp);
			
			
			return curFrameSize;
		}else return -1;
	}
}
