package application.view;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import application.MashidoPlayerMain;
import application.interfaces.Finishable;
import application.interfaces.Saveable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

public class MainView extends AnchorPane implements Saveable{
	@FXML
	private TabPane tabPane;
	
	private static MainView instance=null;
	
	/**Creates new MainView instance in Singleton manner.
	 * @return Instance of MainView
	 */
	public static MainView get() {
		if(instance!=null)return instance;
		try {
			FXMLLoader loader=new FXMLLoader(MashidoPlayerMain.class.getResource("/application/view/MainView.fxml"));
			instance=new MainView();
			loader.setController(instance);
			Pane view = loader.load();
//			instance=loader.getController();
			
			instance.getChildren().add(view);
			
			AnchorPane.setTopAnchor(view, 0d);
			AnchorPane.setBottomAnchor(view, 0d);
			AnchorPane.setLeftAnchor(view, 0d);
			AnchorPane.setRightAnchor(view, 0d);
			
			return instance;
		} catch(IOException ex) {
			MashidoPlayerMain.handleFailedToLoadException(ex);
			return null;											//Shouldn't occur handle method has System.exit(-1);
		}
	}
	private MainView() {}
	
	/**Opens sound file and returns resoult tab. If {@code file} points to file open it's parent directory and automaticly plays it. 
	 * @param file File to open.
	 * @return Tab containing opened file.
	 */
	public static Tab openFile(File file) {
		if(file.exists()) {
			if(file.isDirectory()) {
				return addTab(new DirView(file));
			} else {
				Tab tab=addTab(new DirView(file.getParentFile()));
				if(tab instanceof DirView) {
					((DirView)tab).play(file);
				}
				return tab;
			}
		}
		return null;
	}
	
	/**Tries to add tab to overaping TabPane.
	 * If tab with same as given one exist focus that one and return's it.
	 * else ads given tab to pane select it and returns it.
	 * @param tab
	 * @return
	 */
	public static Tab addTab(Tab tab) {
		if(instance==null)throw new IllegalStateException("First initialize MainView by using get() method.");
		String id=tab.getId();
		for(Tab actualTab:instance.tabPane.getTabs()) {
			if(actualTab.getId().equals(id)) {					//Tab already in pane
				instance.tabPane.getSelectionModel().select(actualTab);
				return actualTab;
			}
		}
		if(tab instanceof Finishable)((Finishable) tab).finishLoading();
		instance.tabPane.getTabs().add(tab);
		instance.tabPane.getSelectionModel().select(tab);
		
		return tab;
	}
	
	@FXML
	private void open() {
		FileChooser chooser=new FileChooser();
		chooser.setTitle("Select file");
		chooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("All files", "*.*"),
				new FileChooser.ExtensionFilter("WAV", "*.wav"),
				new FileChooser.ExtensionFilter("MP3",  "*.mp3"),
				new FileChooser.ExtensionFilter("MP4",  "*.mp4", "*mp4a"),
				new FileChooser.ExtensionFilter("AIFF","*.aif", "*.aiff")
				);
		File file=chooser.showOpenDialog(MashidoPlayerMain.getWindow());
		if(file!=null)
			openFile(file);
	}
	
	@Override
	public void saveState(Document document, Element el) {
		for(Tab tab:tabPane.getTabs()) {
			Element tabNode=document.createElement("tab");
			tabNode.setAttribute("classname", tab.getClass().getName());
			el.appendChild(tabNode);
			if(tab instanceof Saveable)
				((Saveable) tab).saveState(document, tabNode);
		}
	}
	@Override
	public void loadState(Element el) {
		NodeList tabs=el.getChildNodes();
		for(int i=0;i<tabs.getLength();i++) {
			Node n=tabs.item(i);
			if (n.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				Element e=(Element)n;
				
				String className=e.getAttribute("classname");
				if(className.equals("")) {
					MashidoPlayerMain.handleFailedToLoadDataFile();
					return;
				}
				try {
					Object tab=Class.forName(className).getDeclaredConstructor().newInstance();
					if(tab instanceof Saveable) {
						((Saveable) tab).loadState(e);
					}
					if(tab instanceof Tab)
						MainView.addTab((Tab) tab);
					else {
						MashidoPlayerMain.handleFailedToLoadDataFile();
						return;
					}
					
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e1) {
					e1.printStackTrace();
					MashidoPlayerMain.handleFailedToLoadDataFile();
				}
			}
		}
	}
}