package application.view;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import application.MashidoPlayerMain;
import application.libs.JumpableAudioInputStream;
import application.libs.JumpableAudioPlayer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class PlayerPane extends AnchorPane{
	
	@FXML
	private Label fileName;
	@FXML
	private Label playedIndex;
	@FXML
	private Label fileLength;
	@FXML
	private Button stopButton;
	@FXML
	private ToggleButton playButton;
	@FXML
	private ProgressBar progressBar;
	
	private boolean updateProgressBar=true;
	
	private double frameRate;
	private long fileFrames;
	
	private DirView parent;
	private File file;
	private int index;
	
	private JumpableAudioInputStream stream;
	private JumpableAudioPlayer player;
	
	public static PlayerPane get(File file, DirView parent, int index) {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/PlayerPane.fxml"));
			Pane view = loader.load();
			PlayerPane controller=loader.getController();
			
			if(!controller.constructor(file,parent,index))return null;
			controller.getChildren().add(view);
			
			AnchorPane.setTopAnchor(view, 0d);
			AnchorPane.setBottomAnchor(view, 0d);
			AnchorPane.setLeftAnchor(view, 0d);
			AnchorPane.setRightAnchor(view, 0d);
			
			return controller;
		} catch(IOException ex) {
			MashidoPlayerMain.handleFailedToLoadException(ex);
			return null;											//Should't occur handle method has System.exit(-1);
		}
	}
	private boolean constructor(File file, DirView parent, int index) {
		this.parent=parent;
		this.index=index;
		this.file=file;
		fileName.setText(file.getName());
		
		try {
			stream=new JumpableAudioInputStream(file);
			player=new JumpableAudioPlayer(stream);
			player.setCurrentTimeConsumer((long l)->timeParser(l));
		} catch (LineUnavailableException e) {
			MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to play", "Audio out data line is unavaliable.").show();
			e.printStackTrace();
			return false;
		} catch (UnsupportedAudioFileException | IOException e) {
			MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to load", "Selected file is not valid audio file").show();
			e.printStackTrace();
			return false;
		}
		
		fileFrames=stream.getFrameLength();
		frameRate=stream.getFormat().getFrameRate();
		
		fileLength.setText(getTimeString((int)(stream.getFrameLength()/stream.getFormat().getFrameRate())));
		
		return true;
	}
	
	public static String getTimeString(int seconds) {
		StringBuffer ret=new StringBuffer();
		
		if(seconds>3600) {
			ret.append(seconds/3600).append(':');
			seconds%=3600;
		}
		ret.append(String.format("%02d", seconds/60)).append(":");
		seconds%=60;
		ret.append(String.format("%02d", seconds));
		
		return ret.toString();
	}
	
	@FXML
	private void progressMousePressed(MouseEvent e) {
		updateProgressBar=false;
		progressMouseDraged(e);
	}
	@FXML
	private void progressMouseDraged(MouseEvent e) {
		double newProgress=e.getX()/progressBar.getWidth();
		if(newProgress<0||newProgress>1)return;
		progressBar.setProgress(newProgress);
		playedIndex.setText(getTimeString((int)(newProgress*fileFrames/frameRate)));
	}
	@FXML
	private void progressMouseReleased(MouseEvent e) {
		double newProgress=e.getX()/progressBar.getWidth();
		if(newProgress<0||newProgress>1)return;
		progressBar.setProgress(newProgress);
		try {
			stream.jumpFrame((long)(newProgress*fileFrames));
		} catch (IOException e1) {
			MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to jump", "Error ocured during seeking sselected audio position");
			e1.printStackTrace();
		}
		updateProgressBar=true;
	}
	
	public void setFrame(long frameIndex) throws IOException {
		stream.jumpFrame(frameIndex);
	}
	
	void setPlay(boolean play) {
		if(play!=isPlaying())togglePlay();
	}
	
	@FXML
	void togglePlay() {
		player.toggleAudio();
		playButton.setSelected(player.isPlaying());
	}
	
	public boolean isPlaying() {
		return player.isPlaying();
	}
	
	private void timeParser(long time) {
		if(time==-1) {				//End of file
			playButton.setSelected(false);
			return;
		}
		if(time==-2) {
			MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to play", "Can't open audio data line");
			return;
		}
		
		if(updateProgressBar) {
			Platform.runLater(new Runnable() {
				public void run() {
					progressBar.setProgress((double)time/fileFrames);
					playedIndex.setText(getTimeString((int)(time/frameRate)));
				}
			});
		}
	}
	
	public File getFile() {
		return file;
	}
	public long getActualFrame() {
		return stream.getActualFrame();
	}
	
	@FXML
	private void stop(){
		if(player.isPlaying()) {
			togglePlay();
		}
		parent.stop(file, index);
	}
}