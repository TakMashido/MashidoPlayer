package application.view;

import java.io.File;
import java.io.IOException;

import application.MashidoPlayerMain;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class DirPane extends AnchorPane{
	@FXML
	private Label dirName;
	
	private File file;
	
	public static DirPane get(File file) {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/DirPane.fxml"));
			Pane view = loader.load();
			DirPane controller=loader.getController();
			
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
		
		dirName.setText(file.getName());
	}
	
	public File getFile() {
		return file;
	}
	
	@FXML
	private void open() {
		MainView.openFile(file);
	}
}