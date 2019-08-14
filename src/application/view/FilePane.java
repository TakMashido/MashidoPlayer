package application.view;

import java.io.File;
import java.io.IOException;

import application.MashidoPlayerMain;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

public class FilePane extends AnchorPane{
	@FXML
	private Label fileName;
	
	private File file;
	private DirView parent;
	private int index;
	
	public static FilePane get(File file, DirView parent, int index) {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/FilePane.fxml"));
			Pane view = loader.load();
			FilePane controller=loader.getController();
			
			controller.constructor(file,parent,index);
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
	private void constructor(File file, DirView parent, int index) {
		this.parent=parent;
		this.index=index;
		this.file=file;
		fileName.setText(file.getName());
	}
	
	public File getFile() {
		return file;
	}
	
	@FXML
	private void play() {
		parent.play(file, index);
	}
}