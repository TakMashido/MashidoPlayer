package application.view;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import application.MashidoPlayerMain;
import application.interfaces.Finishable;
import application.interfaces.Saveable;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class DirView extends Tab implements Finishable,Saveable{
	@FXML
	private Label dirName;
	@FXML
	private VBox filesPane;
	
	private List<Node> childs;
	
	private File file;
	
	public DirView() {}					//For further use with loadState(org.w3c.dom.Node);
	public DirView(File file) {
		constructor(file);
	}
	private void loadFXML() {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/DirView.fxml"));
			loader.setController(this);
			Pane view = loader.load();
			
			setContent(view);
			
			AnchorPane.setTopAnchor(view, 0d);
			AnchorPane.setBottomAnchor(view, 0d);
			AnchorPane.setLeftAnchor(view, 0d);
			AnchorPane.setRightAnchor(view, 0d);
		} catch(IOException ex) {
			MashidoPlayerMain.handleFailedToLoadException(ex);
		}
	}
	private void constructor(File file) {
		if(!file.exists()|!file.isDirectory()) throw new RuntimeException("Dir which you try to open doesn't exist");
		
		this.file=file;
		setId("dir:"+file.getAbsolutePath());
	}
	
	@FXML
	private void initialize() {
		childs=filesPane.getChildren();
		this.setOnCloseRequest(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				for(Node child:childs) {
					if(child instanceof PlayerPane) {
						if(((PlayerPane)child).isPlaying()) {
							Alert alert=MashidoPlayerMain.getAlert(AlertType.CONFIRMATION, "Warning", "Tab contain unstopped audio", "Do you want to stop now?");
							alert.getButtonTypes().clear();
							alert.getButtonTypes().addAll(ButtonType.YES,ButtonType.NO);
							alert.showAndWait().ifPresent(res->{
								if(res==ButtonType.YES){
									for(Node child2:childs) {
										if(child2 instanceof PlayerPane) {
											PlayerPane player=(PlayerPane)child2;
											if(player.isPlaying()) {
												player.togglePlay();
											}
										}
									}
								} else if(res==ButtonType.NO){
									event.consume();
								} else assert false:"Unknown button clicked";
							});;
						}
					}
				}				
			}
		});
	}
	
	public void play(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof FilePane) {
				if(((FilePane)node).getFile().equals(file)) {
					play(file,i);
					return;
				}
			} else if(node instanceof PlayerPane) {
				PlayerPane player=(PlayerPane)node;
				if(!player.isPlaying())player.togglePlay();
				return;
			}
		}
	}
	public void play(File file, int index) {
		PlayerPane pane=openPlayerPane(file,index);
		
		pane.togglePlay();
	}
	public PlayerPane openPlayerPane(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof FilePane) {
				if(((FilePane)node).getFile().equals(file)) {
					return openPlayerPane(file,i);
				}
			} else if(node instanceof PlayerPane) {
				PlayerPane player=(PlayerPane)node;
				if(!player.isPlaying())player.togglePlay();
				return player;
			}
		}
		return null;
	}
	public PlayerPane openPlayerPane(File file,int index) {
		PlayerPane pane=PlayerPane.get(file, this, index);;
		childs.set(index, pane);
		return pane;
	}
	public void stop(File file, int index) {
		childs.set(index,FilePane.get(file, this, index));
	}

	private boolean finished=false;
	@Override
	public void finishLoading() {
		if(!finished) {
			finished=true;
			loadFXML();
			
			String tabName=file.getName();
			dirName.setText(tabName);
			this.setText(tabName);
			
			Queue<File> fileQueue=new LinkedList<>();
			
			File[] files=file.listFiles();
			for(File f:files) {
				if(f.isFile()) {
					if(MashidoPlayerMain.isSupportedSoundFile(f)) {
						fileQueue.add(f);
					}
				} else if(f.isDirectory()) {
					childs.add(DirPane.get(f));
				}
			}
			
			int index=childs.size();
			while(!fileQueue.isEmpty()) {
				childs.add(FilePane.get(fileQueue.poll(), this, index++));
			}
		}
	}
	
	@Override
	public void loadState(Element e) {
		constructor(new File(e.getAttribute("dirName")));
		finishLoading();
		NodeList opened=e.getElementsByTagName("opened");
		for(int i=0;i<opened.getLength();i++) {
			org.w3c.dom.Node item=opened.item(i);
			if (item.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				Element el = (Element) item;
				
				String str=el.getAttribute("name");
				if(str.isEmpty()) MashidoPlayerMain.handleFailedToLoadDataFile();
				File playiedFile=new File(str);
				if(!playiedFile.isFile()||!playiedFile.getParentFile().equals(file))MashidoPlayerMain.handleFailedToLoadDataFile();
				if(!playiedFile.exists())continue;
				
				str=el.getAttribute("time");
				double time=0;
				try {
					if(str.length()!=0)time=Double.parseDouble(str);
				} catch(NumberFormatException ex) {
					ex.printStackTrace();
				}
				
				PlayerPane player=openPlayerPane(playiedFile);
				player.seekTime(new Duration(time));
				
				str=el.getAttribute("play");
				if(str.length()!=0 && Boolean.parseBoolean(str)) {
					player.setPlay(true);
				}
				
				str=el.getAttribute("volume");
				double volume=1;
				try {
					if(str.length()!=0)volume=Double.parseDouble(str);
				} catch(NumberFormatException ex) {
					ex.printStackTrace();
				}
				player.setVolume(volume);
			}
		}
	}
	@Override
	public void saveState(Document document, Element node) {
		node.setAttribute("dirName", file.getAbsolutePath());
		for(Node child:childs) {
			if(child instanceof PlayerPane) {
				PlayerPane player=(PlayerPane)child;
				Element opened=document.createElement("opened");
				opened.setAttribute("name", player.getFile().getAbsolutePath());
				opened.setAttribute("time", Double.toString(player.getCurrentTime().toMillis()));
				opened.setAttribute("play", Boolean.toString(player.isPlaying()));
				opened.setAttribute("volume", Double.toString(player.getVolume()));
				node.appendChild(opened);
			}
		}
	}
}