package application.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import application.MashidoPlayerMain;
import application.interfaces.Finishable;
import application.interfaces.PlayerHolder;
import application.interfaces.Saveable;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class DirView extends Tab implements Finishable,Saveable,PlayerHolder{
	@FXML
	private Label dirName;
	@FXML
	private VBox filesPane;
	@FXML
	private ScrollPane scrollPane;
	
	private List<Node> childs;
	
	private File file;
	
	private Thread watcherThread;
	
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
							});
						}
					}
				}				
				watcherThread.interrupt();
			}
		});
		
		scrollPane.setFitToHeight(true);
	}
	
	/**Return index of given file in childs list or -1 if not presents.
	 * @param file File to find.
	 * @return Read description.
	 */
	private int getFileIndex(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof FilePane) {
				if(((FilePane)node).getFile().equals(file)) return i;
			} else if(node instanceof PlayerPane) {
				if(((PlayerPane)node).getFile().equals(file)) return i;
			} else if(node instanceof DirPane) {
				if(((DirPane) node).getFile().equals(file)) return i;
			}
		}
		return -1;
	}
	/**Gets index of last dirPane in childs
	 * @return
	 */
	private int getLastDirIndex() {
		for(int i=0;i<childs.size();i++) {
			if(!(childs.get(i) instanceof DirPane)) {
				return i-1;
			}
		}
		return childs.size()-1;
	}
	public void play(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof FilePane) {
				if(((FilePane)node).getFile().equals(file)) {
					PlayerPane player=openPlayerPane(file,i);
					player.setPlay(true);
					return;
				}
			} else if(node instanceof PlayerPane) {
				if(((PlayerPane)node).getFile().equals(file)) {
					PlayerPane player=(PlayerPane)node;
					if(!player.isPlaying())player.togglePlay();
					return;
				}
			}
		}
	}
	public PlayerPane openPlayerPane(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof FilePane) {
				if(((FilePane)node).getFile().equals(file)) {
					PlayerPane player=PlayerPane.get(file, this);
					childs.set(i, player);
					return player;
				}
			} else if(node instanceof PlayerPane) {
				PlayerPane player=(PlayerPane)node;
				if(!player.isPlaying())player.togglePlay();
				return player;
			}
		}
		return null;
	}
	private PlayerPane openPlayerPane(File file, int index) {
		PlayerPane player=PlayerPane.get(file, this);
		childs.set(index, player);
		return player;
	}
	
	public void stop(PlayerPane pane) {
		childs.set(getFileIndex(pane.getFile()),FilePane.get(pane.getFile(), this));
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
			
			while(!fileQueue.isEmpty()) {
				childs.add(FilePane.get(fileQueue.poll(), this));
			}
			
			watcherThread=new Thread(new DirWatcher());
			watcherThread.setDaemon(true);
			watcherThread.start();
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
				if(!(playiedFile.exists()&&playiedFile.isFile()&&playiedFile.getParentFile().equals(file)))continue;
				
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
	
	public void dispose() {
		watcherThread.interrupt();
	}
	
	private class DirWatcher implements Runnable {
		@Override
		public void run() {
			Path path=file.toPath();
			try {
				WatchService watcher=FileSystems.getDefault().newWatchService();
				
				path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
				
				while(true) {
					WatchKey key=watcher.take();
					
					for(WatchEvent<?> event:key.pollEvents()) {
						Kind<?> kind=event.kind();
						if(kind==StandardWatchEventKinds.OVERFLOW) {
							MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to refresh files", "Directory change watcher becomed overflowed");
						} else if(kind==StandardWatchEventKinds.ENTRY_CREATE) {
							@SuppressWarnings("unchecked")
							File added=path.resolve(((WatchEvent<Path>)event).context()).toFile();
							Platform.runLater(new Runnable() {
								public void run() {
									if(added.isDirectory()) {
										childs.add(getLastDirIndex()+1,DirPane.get(added));
									}else if(added.isFile()) {
										if(MashidoPlayerMain.isSupportedSoundFile(added)) {
											Node node=FilePane.get(added, DirView.this);
											if(node!=null)
												childs.add(node);
										}
									}
								}
							});
							
						} else if(kind==StandardWatchEventKinds.ENTRY_DELETE) {
							@SuppressWarnings("unchecked")
							int index=getFileIndex(path.resolve(((WatchEvent<Path>)event).context()).toFile());
							if(index==-1)continue;
							Node child=childs.get(index);
							Platform.runLater(new Runnable() {
								public void run() {
									if(child instanceof PlayerPane)((PlayerPane) child).dispose();
									childs.remove(index);
								}
							});
						} else assert false:"Unrecognized event cought";
					}
					
					if(!key.reset()) {
										//Dir deleted. Somehow should also check if dir name modified and reset this dir then.
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (InterruptedException e) {}					//Loop inside try block
		}
	}
}