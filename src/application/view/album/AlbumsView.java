package application.view.album;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import application.MashidoPlayerMain;
import application.interfaces.Finishable;
import application.view.MainView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class AlbumsView extends Tab implements Finishable{
	@FXML
	private VBox albumsPane;
	@FXML
	private ScrollPane scrollPane;
	
	private List<Node> childs;
	
	private static final File albumsDir=new File("albums/");
	
	private static AlbumsView instance=null;
	
	private static boolean deleteState=false;
	
	public static AlbumsView get() {
		if(instance==null) {
			instance=new AlbumsView();
		}
		return instance;
	}
	
	private AlbumsView() {}
	private void loadFXML() {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/album/AlbumsView.fxml"));
			loader.setController(this);
			Pane view = loader.load();
			
			setContent(view);
			
			childs=albumsPane.getChildren();
			
			AnchorPane.setTopAnchor(view, 0d);
			AnchorPane.setBottomAnchor(view, 0d);
			AnchorPane.setLeftAnchor(view, 0d);
			AnchorPane.setRightAnchor(view, 0d);
		} catch(IOException ex) {
			MashidoPlayerMain.handleFailedToLoadException(ex);
		}
	}
	
	@FXML
	private void initialize() {
		setId("albums");
		scrollPane.setFitToHeight(true);
	}
	
	private boolean finished=false;
	@Override
	public void finishLoading() {
		if(!finished) {
			finished=true;
			loadFXML();
			
			this.setText("Albums");
			
			File[] files=albumsDir.listFiles();
			for(File f:files) {
				if(f.isFile()) {
					if(f.getName().endsWith(".xml")) {
						AlbumPane album=AlbumPane.get(f);
						if(album!=null)
							childs.add(album);
					}
				}
			}
		}
	}
	
	public void albumRenamed(File lastFile, File newFile) {
		if(finished) {
			for(int i=0;i<childs.size();i++) {
				Node child=childs.get(i);
				if(child instanceof AlbumPane) {
					AlbumPane pane=(AlbumPane)child;
					if(pane.getFile().getAbsolutePath().equals(lastFile.getAbsolutePath())) {				//pane.getFile().equals(lastFile); do not work, lastFile is absolute path and pane.getFile() not.
						pane.setFile(newFile);
						return;
					}
				}
			}
		}
	}
	
	public static boolean inDeleteState() {
		return deleteState;
	}
	
	@FXML
	private void add() {
		File newAlbum=new File(albumsDir,"New album.xml");
		if(newAlbum.exists()) {
			for(int i=0;;i++) {
				newAlbum=new File(albumsDir,"New album "+i+".xml");
				if(!newAlbum.exists())break;
			}
		}
		
		try {
			newAlbum.createNewFile();
			
			InputStream in=MashidoPlayerMain.class.getResourceAsStream("/application/files/DefaultAlbum.xml");
			
			if(in==null) {
				MashidoPlayerMain.handleFailedToLoadException(new IOException("File /application/files/DefaultAlbum.xml is missing"),false);
				return;
			}
			
			OutputStream out=new FileOutputStream(newAlbum);
			
			byte[] buf=new byte[1024];
			int toCopy;
			while((toCopy=in.read(buf))>0) {
				out.write(buf, 0, toCopy);
			}
			
			in.close();
			out.close();
			
			AlbumPane album=AlbumPane.get(newAlbum);
			if(album!=null)
				childs.add(album);
			MainView.addTab(new AlbumView(newAlbum));
		} catch (IOException e) {
			MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to create album", "Error occured during creating album file");
			e.printStackTrace();
		}
		
		System.err.println("Open new tab with new album after creating it");
	}
	@FXML
	private void delete() {
		if(deleteState) {
			int toDelete=0;
			for(Node child:childs) {
				if(child instanceof AlbumPane&&((AlbumPane) child).markedToDelete()) {
					toDelete++;
					if(toDelete>=2)break;
				}
			}
			
			if(toDelete!=0) {
				String lbum=toDelete>1?"lbums":"lbum";
				Alert alert=MashidoPlayerMain.getAlert(AlertType.CONFIRMATION, "Delete", "A"+lbum+" delete", "Delete selected a"+lbum+"?");
				
				alert.getButtonTypes().clear();
				alert.getButtonTypes().addAll(ButtonType.YES,ButtonType.NO,ButtonType.CANCEL);
				alert.showAndWait().ifPresent(res->{
					if(res==ButtonType.CANCEL) {
						return;
					}
					if(res==ButtonType.YES){
						for(int i=0;i<childs.size();i++) {
							Node child=childs.get(i);
							if(child instanceof AlbumPane&&((AlbumPane) child).markedToDelete()) {
								childs.remove(i--);
								((AlbumPane) child).getFile().delete();
							}
						}
					}
				});
			}
		}
		deleteState^=true;
		for(Node child:childs) {
			if(child instanceof AlbumPane) {
				((AlbumPane) child).setDeleteState(deleteState);
			}
		}
	}
}