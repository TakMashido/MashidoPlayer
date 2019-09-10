package application.view.album;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import application.MashidoPlayerMain;
import application.interfaces.Finishable;
import application.interfaces.PlayerHolder;
import application.interfaces.Saveable;
import application.view.PlayerPane;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class AlbumView extends Tab implements Finishable,Saveable,PlayerHolder{
	@FXML
	private TextField albumName;
	@FXML
	private VBox filesPane;
	@FXML
	private ScrollPane scrollPane;
	
	private List<Node> childs;
	private List<Element> elements=new ArrayList<>();
	private Element parentElement;
	private Document doc;
	private boolean albumChanged=false;
	
	private File file;
	
	private boolean deleteState=false;
	
	public AlbumView() {}						//For further use with loadaState(Element e)
 	public AlbumView(File file) {
		constructor(file);
	}
	private void loadFXML() {
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/album/AlbumView.fxml"));
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
		if(!file.exists()|!file.isFile()) throw new RuntimeException("Dir which you try to open (/"+file.getName()+"\")doesn't exist");
		
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
		
		scrollPane.setFitToHeight(true);
	}
	
	private int getFileIndex(File file) {
		for(int i=0;i<childs.size();i++) {
			Node child=childs.get(i);
			if(child instanceof AlbumFilePane) {
				if(((AlbumFilePane) child).getFile().equals(file)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public void play(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof AlbumFilePane) {
				if(((AlbumFilePane)node).getFile().equals(file)) {
					play(file,i);
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
	public void play(File file, int index) {
		PlayerPane pane=openPlayerPane(file,index);
		
		pane.togglePlay();
	}
	public PlayerPane openPlayerPane(File file) {
		for(int i=0;i<childs.size();i++) {
			Node node=childs.get(i);
			if(node instanceof AlbumFilePane) {
				if(((AlbumFilePane)node).getFile().equals(file)) {
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
	private PlayerPane openPlayerPane(File file,int index) {
		PlayerPane pane=PlayerPane.get(file, this);
		childs.set(index, pane);
		return pane;
	}
	public void stop(PlayerPane pane) {
		int index=getFileIndex(pane.getFile());
		if(index!=-1)
			childs.set(index,AlbumFilePane.get(pane.getFile(), this));
	}
	
	private boolean finished=false;
	@Override
	public void finishLoading() {
		if(!finished) {
			finished=true;
			loadFXML();
			
			Document data;
			try {
				data = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			} catch (SAXException | IOException | ParserConfigurationException e) {
				MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Failed to load album data file", "");
				e.printStackTrace();
				return;
			}
			doc=data;
			
			int errors=0;
			parentElement=(Element)((Element) (data.getElementsByTagName("MashidoPlayer").item(0))).getElementsByTagName("album").item(0);
			NodeList files=parentElement.getElementsByTagName("file");
			for(int i=0;i<files.getLength();i++) {
				Element fileElement=(Element)files.item(0);
				File file=new File(fileElement.getAttribute("name"));
				if(MashidoPlayerMain.isSupportedSoundFile(file)) {
					childs.add(AlbumFilePane.get(file,this));
					elements.add(fileElement);
				} else {
					parentElement.removeChild(fileElement);
					errors++;
				}
			}
			if(errors>0) {
				MashidoPlayerMain.getAlert(AlertType.ERROR, "Error", "Program was unable to load "+errors+" file"+(errors>1?"s.":"."), "File"+(errors>1?"s":"")+" may becomed corruped, moved, or deleted").show();;
				albumChanged=true;
				save();
			}
			
			String tabName=file.getName().substring(0,file.getName().length()-4);
			albumName.setText(tabName);
			this.setText(tabName);
		}
	}
	
	@Override
	public void loadState(Element e) {
		constructor(new File(e.getAttribute("albumFile")));
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
		node.setAttribute("albumFile", file.getAbsolutePath());
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
	
	@FXML
	private void add() {
		File file=MashidoPlayerMain.chooseFile();
		int index=getFileIndex(file);
		if(index==-1) {
			childs.add(AlbumFilePane.get(file, this));
			Element newElement=doc.createElement("file");
			newElement.setAttribute("name", file.getAbsolutePath());
			parentElement.appendChild(newElement);
			elements.add(newElement);
			albumChanged=true;
			save();
		}
	}
	@FXML
	private void delete() {
		if(deleteState) {
			int toDelete=0;
			for(Node child:childs) {
				if(child instanceof AlbumFilePane&&((AlbumFilePane) child).markedToDelete()) {
					toDelete++;
					if(toDelete>=2)break;
				}
			}
			
			if(toDelete!=0) {
				String ile=toDelete>1?"iles":"ile";
				Alert alert=MashidoPlayerMain.getAlert(AlertType.CONFIRMATION, "Delete", "F"+ile+" delete", "Delete selected f"+ile+" from album?");
				
				alert.getButtonTypes().clear();
				alert.getButtonTypes().addAll(ButtonType.YES,ButtonType.NO,ButtonType.CANCEL);
				alert.showAndWait().ifPresent(res->{
					if(res==ButtonType.CANCEL) {
						return;
					}
					if(res==ButtonType.YES){
						for(int i=0;i<childs.size();i++) {
							Node child=childs.get(i);
							if(child instanceof AlbumFilePane&&((AlbumFilePane) child).markedToDelete()) {
								parentElement.removeChild(elements.remove(i));
								childs.remove(i--);
								albumChanged=true;
							}
						}
						save();
					}
				});
			}
		}
		deleteState^=true;
		for(Node child:childs) {
			if(child instanceof AlbumFilePane) {
				((AlbumFilePane) child).setDeleteState(deleteState);
			}
		}
	}
	private volatile long lastTyped=-1;
	@FXML
	private void nameChanged() {
		if(lastTyped==-1) {
			lastTyped=System.currentTimeMillis();
			Thread renameThread=new Thread() {
				private static final long waitTime=2000;
				@Override
				public void run() {
					try {
						long lastCheck=System.currentTimeMillis();
						do{
							try {
								Thread.sleep(lastTyped-lastCheck+waitTime);
							} catch (InterruptedException e) {break;}
							lastCheck=System.currentTimeMillis();
							System.out.println(lastTyped-lastCheck);
						} while(lastCheck-lastTyped<waitTime);
						
						if(albumName.getText()+".xml"==file.getName())return;				//File name not changed
						
						File renamed=new File(file.getParent(),albumName.getText()+".xml");
						if(renamed.exists()) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									MashidoPlayerMain.getAlert(AlertType.INFORMATION, "Name error", "album with choosen name already exist", "Please choose diffrend name").show();
									albumName.setText(file.getName().substring(0, file.getName().length()-4));
								}
							});
							return;
						}
						
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								AlbumsView.get().albumRenamed(file, renamed);
								file.renameTo(renamed);
								file=renamed;
								MashidoPlayerMain.getAlert(AlertType.INFORMATION, "Success", "Name changed", "Album name successfully changed").show();;
								setText(albumName.getText());
							}
						});
					} finally {
						lastTyped=-1;
					}
				}
			};									//Do not set deamon true: keep thread if app get's closed to not lose new name
			renameThread.start();
		} else {
			lastTyped=System.currentTimeMillis();
		}
	}
	
	private void save() {							//Find better way then directly invocing after adding/deleteing file/s
		if(albumChanged) {
			try {
				TransformerFactory transformerFactory=TransformerFactory.newInstance();
				transformerFactory.setAttribute("indent-number", 4);
				Transformer transformer;
				transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,"yes");
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
				
				StringWriter string=new StringWriter();
				transformer.transform(new DOMSource(doc), new StreamResult(string));
				Scanner output=new Scanner(string.toString());
				OutputStreamWriter fileOut=new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8);
				while(output.hasNextLine()) {
					String line=output.nextLine();
					if(!line.trim().isEmpty()) {
						fileOut.write(line);
						fileOut.write("\r\n");
					}
				}
				output.close();
				fileOut.close();
			} catch (TransformerException | IOException e) {
				MashidoPlayerMain.getAlert(AlertType.ERROR, "Eror", "Failed to save", "Failed to save album changes");
				e.printStackTrace();
			}
			
			albumChanged=false;
		}
	}
}