package application.view.album;

import java.io.File;
import java.io.IOException;

import application.MashidoPlayerMain;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class AlbumFilePane extends AnchorPane{
	private Pane view;
	
	@FXML
	private Label fileName;
	
	private File file;
	private AlbumView parent;
	@FXML
	private Button actionButton;
	
	private static final PseudoClass deleteState=PseudoClass.getPseudoClass("delete-state");
	private static final PseudoClass toDelete=PseudoClass.getPseudoClass("to-delete");
	private boolean markedToDelete=false;
	private boolean inDeleteState=false;
	
	public static AlbumFilePane get(File file, AlbumView parent) {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/album/AlbumFilePane.fxml"));
			Pane view = loader.load();
			AlbumFilePane controller=loader.getController();
			controller.view=view;
			
			controller.constructor(file,parent);
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
	private void constructor(File file, AlbumView parent) {
		this.parent=parent;
		this.file=file;
		fileName.setText(file.getName());
	}
	
	public File getFile() {
		return file;
	}
	@FXML
	private void action() {
		if(inDeleteState) {
			deleteButtonPressed();
		} else {
			play();
		}
	}
	private void play() {
		parent.play(file);
	}
	private void deleteButtonPressed() {
		markedToDelete^=true;
		view.pseudoClassStateChanged(toDelete, markedToDelete);
		actionButton.pseudoClassStateChanged(toDelete, markedToDelete);
	}
	
	void setDeleteState(boolean state) {
		if(inDeleteState==state)return;
		
		view.pseudoClassStateChanged(deleteState, state);
		actionButton.pseudoClassStateChanged(deleteState, state);
		
		if(state) {
			actionButton.setText("Delete");
		} else {
			if(markedToDelete) {
				view.pseudoClassStateChanged(toDelete, false);
				actionButton.pseudoClassStateChanged(toDelete, false);
			}
			actionButton.setText("Play");
			markedToDelete=false;
		}
		inDeleteState=state;
	}
	boolean markedToDelete() {
		return markedToDelete;
	}
}