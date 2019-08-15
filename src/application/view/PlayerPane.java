package application.view;

import java.io.File;
import java.io.IOException;

import application.MashidoPlayerMain;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

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
	
	private DirView parent;
	private File file;
	private int index;
	
	private double length;				//in seconds
	private Duration currentTime=Duration.ZERO;
	
	private boolean isPlaying=false;
	private Media media;
	private MediaPlayer player;
	
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
		
		media=new Media(file.toURI().toString());
		player=new MediaPlayer(media);
		
		player.setOnStopped(new Runnable() {
			public void run() {
				setPlay(false);
			}
		});
		player.currentTimeProperty().addListener(new ChangeListener<Duration>() {
			@Override
			public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {
				updateTime(newValue);
			}
		});
		player.setOnReady(new Runnable() {
			public void run() {
				length=media.getDuration().toSeconds();
				fileLength.setText(getTimeString((int)(length)));
				if(currentTime!=Duration.ZERO) 
					seekTime(currentTime);
			}
		});
		
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
		playedIndex.setText(getTimeString((int)(length*newProgress)));
	}
	@FXML
	private void progressMouseReleased(MouseEvent e) {
		double newProgress=e.getX()/progressBar.getWidth();
		if(newProgress<0||newProgress>1)return;
		progressBar.setProgress(newProgress);
		player.seek(media.getDuration().multiply(newProgress));
		updateProgressBar=true;
	}
	
	public void seekTime(Duration seekTime) {
		currentTime=seekTime;
		playedIndex.setText(getTimeString((int)(seekTime.toSeconds())));
		player.seek(seekTime);
	}
	public Duration getCurrentTime() {
		return currentTime;
	}
	
	void setPlay(boolean play) {
		if(isPlaying==play)return;
		if(play) {
			player.play();
		} else {
			player.pause();
		}
		isPlaying=play;
		playButton.setSelected(isPlaying);
	}
	
	@FXML
	void togglePlay() {
		setPlay(!isPlaying);
	}
	
	public boolean isPlaying() {
		return isPlaying;
	}
	
	private void updateTime(Duration time) {
		currentTime=time;
		if(updateProgressBar) {
			Platform.runLater(new Runnable() {
				public void run() {
					progressBar.setProgress(time.toSeconds()/length);
					playedIndex.setText(getTimeString((int)(time.toSeconds())));
				}
			});
		}
	}
	
	public File getFile() {
		return file;
	}
	public Duration playTime() {
		return player.getCurrentTime();
	}
	
	@FXML
	private void stop(){
		if(isPlaying) {
			setPlay(false);
		}
		parent.stop(file, index);
	}
}