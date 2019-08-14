package application.libs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class JumpableAudioInputStream extends InputStream{
	private File source;
	private long actualIndex;					//In bytes
	private boolean closed=false;
	
	private AudioInputStream input;
	private AudioFormat format;
	private int frameSize;
	
	public JumpableAudioInputStream(File source) throws UnsupportedAudioFileException, IOException {
		this.source=source;
		
		loadAudio();
	}
	private void loadAudio() throws UnsupportedAudioFileException, IOException {
		input=AudioSystem.getAudioInputStream(source);
		format=input.getFormat();
		frameSize=format.getFrameSize();
		actualIndex=0;
	}
	
	public long getActualFrame() {
		return actualIndex/frameSize;
	}
	
	public synchronized void jumpFrame(long n) throws IOException {
		if(closed)throw new IOException("Stream already closed");
		n*=frameSize;
		if(n>actualIndex) {
			skip(n-actualIndex);
		} else {
			input.close();
			
			try {
				loadAudio();
			} catch (UnsupportedAudioFileException e) {				//Shouldn't occur, file was opened before
				System.err.println("wtf");
				assert false:"Somehow file wchich was readed correctly before now can't be readen";
				e.printStackTrace();
			}
			
			skip(n);
		}
	}
	public synchronized long skipFrame(long n) throws IOException {
		if(closed)throw new IOException("Stream already closed");
		n*=frameSize;
		long ret=input.skip(n);
		actualIndex+=ret;
		return ret;
	}
	
	public synchronized int read() throws IOException {
		if(closed)throw new IOException("Stream already closed");
		actualIndex++;
		return input.read();
	}
	public synchronized int read(byte[] b) throws IOException {
		if(closed)throw new IOException("Stream already closed");
		actualIndex+=b.length;
        return input.read(b,0,b.length);
    }
	public synchronized int read(byte[] b, int off, int len) throws IOException {
		if(closed)throw new IOException("Stream already closed");
		actualIndex+=len;
        return input.read(b,off,len);
    }
	
	/*Orginal audio input stream methods*/
	public AudioFormat getFormat() {
		return format;
	}
	public long getFrameLength() {
		return input.getFrameLength();
	}
	public int available() throws IOException{
		return input.available();
	}
	public synchronized void close() throws IOException {
		closed=true;
		input.close();
	}
}