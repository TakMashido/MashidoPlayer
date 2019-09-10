package application.view.album;

import java.io.File;
import java.io.IOException;

import application.MashidoPlayerMain;
import application.view.MainView;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class AlbumPane extends AnchorPane{
	private Pane view;
	
	@FXML
	private Label albumName;
	
	private File file;
	private String albumNameString;
	@FXML
	private Button actionButton;
	
	private static final PseudoClass deleteState=PseudoClass.getPseudoClass("delete-state");
	private static final PseudoClass toDelete=PseudoClass.getPseudoClass("to-delete");
	private boolean markedToDelete=false;
	private boolean inDeleteState=false;
	
	public static AlbumPane get(File file) {
		if(!file.getName().endsWith(".xml"))return null;
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/album/AlbumPane.fxml"));
			Pane view = loader.load();
			AlbumPane controller=loader.getController();
			controller.view=view;
			
			controller.constructor(file);
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
	private void constructor(File file) {
		this.file=file;
		
		albumNameString=file.getName().substring(0, file.getName().length()-4);
		
		albumName.setText(albumNameString);
	}
	
	public File getFile() {
		return file;
	}
	
	@FXML
	private void action() {
		if(inDeleteState) {
			deleteButtonPressed();
		} else {
			open();
		}
	}
	private void open() {
		MainView.addTab(new AlbumView(file));
	}
	private void deleteButtonPressed() {
		markedToDelete^=true;
		view.pseudoClassStateChanged(toDelete, markedToDelete);
		actionButton.pseudoClassStateChanged(toDelete, markedToDelete);
	}
	
	public String getName() {
		return albumNameString;
	}
	
	public void setDeleteState(boolean state) {
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
			actionButton.setText("Open");
			markedToDelete=false;
		}
		inDeleteState=state;
	}
	public boolean markedToDelete() {
		return markedToDelete;
	}
	
	public void setFile(File file) {
		constructor(file);
	}
}