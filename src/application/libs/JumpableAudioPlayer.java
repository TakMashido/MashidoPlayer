package application.libs;

import java.io.IOException;
import java.util.function.LongConsumer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class JumpableAudioPlayer {
	private JumpableAudioInputStream stream;
	private SourceDataLine audioOut;
	private AudioFormat audioFormat;
	private Thread playerThread;
	
	private LongConsumer consumer;
	
	private boolean isPlaying=false;
	
	public JumpableAudioPlayer(JumpableAudioInputStream stream) throws LineUnavailableException {
		this.stream=stream;
		audioFormat=stream.getFormat();
		initializeAudioLine();
	}
	private void initializeAudioLine() throws LineUnavailableException {			//Tries to get first line from fist mixer. Hope it's windows default audio output
		Mixer.Info[] miksers=AudioSystem.getMixerInfo();
		
		Mixer mixer;
		Line.Info[] lines;
		for(Mixer.Info mixerInfo:miksers) {
			mixer=AudioSystem.getMixer(mixerInfo);
			lines=mixer.getSourceLineInfo();
			if(lines.length>0) {
				for(Line.Info line:lines) {
					if(line.toString().contains("SourceDataLine")) {
						audioOut=(SourceDataLine) mixer.getLine(line);
						return;
					}
				}
			}
		}
	}
	
	/**Set LongConsumer to follow pllay progress.
	 * Consumer will recive current played frame, and 
	 * -1 if stream with audio become empty,
	 * -2 if failed to open sound output line
	 * @param consumer
	 */
	public void setCurrentTimeConsumer(LongConsumer consumer) {
		this.consumer=consumer;
	}
	
	public boolean isPlaying() {
		return isPlaying;
	}
	public void toggleAudio() {
		isPlaying^=true;
		if(isPlaying) {
			playAudio();
		} else {
			stopPlayingAudio();
		}
	}
	public void playAudio() {
		playerThread=new Thread(new PlayRunnable());
		playerThread.setDaemon(true);
		playerThread.start();
	}
	private void stopPlayingAudio(){
		isPlaying=false;
		try {
			playerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public JumpableAudioInputStream getStream() {
		return stream;
	}
	
	private class PlayRunnable implements Runnable{
		public void run() {
			int size=audioOut.getBufferSize()/8;
			if(size%audioFormat.getFrameSize()!=0)size-=size%audioFormat.getFrameSize();
			byte[] buffer=new byte[size];
			int readen;
			try {
				audioOut.open(audioFormat);
			} catch (LineUnavailableException ex) {
				ex.printStackTrace();
				isPlaying=false;
				consumer.accept(-2l);
			}
			audioOut.start();
			try {
				while(isPlaying&&((readen=stream.read(buffer))>0)) {
					audioOut.write(buffer, 0, readen);
					if(consumer!=null)
						consumer.accept(stream.getActualFrame());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			audioOut.drain();
			audioOut.stop();
			audioOut.close();
			consumer.accept(-1l);
			isPlaying=false;
		}
	}
}