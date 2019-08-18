package application;
	
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
import org.xml.sax.SAXException;

import application.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class MashidoPlayerMain extends Application {
	private static Stage primaryStage;
	
	private static MainView mainView;
	
	private static String loadedCss;
	
	@Override
	public void start(Stage stage) throws FileNotFoundException {
		primaryStage=stage;
		
		loadedCss=getClass().getResource("view/css/DarkTheme.css").toExternalForm();
		
		mainView=MainView.get();
		mainView.getStylesheets().add(loadedCss);
		
		stage.setTitle("Mashido Player");
		
		Scene scene = new Scene(mainView,400,300);
		primaryStage.setScene(scene);
		primaryStage.show();
		
		for(String str:getParameters().getRaw()) {
			MainView.openFile(new File(str));
		}
		
		loadDataFile();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				saveDataToFile();
			}
		});
		
//		MainView.openFile(new File("C:\\Users\\Przemek\\Desktop\\speech datasets\\spoken wikipedia\\wav\\to_label\\1"));
//		MainView.openFile(new File("C:\\Users\\Przemek\\Desktop\\speech datasets\\spoken wikipedia\\wav\\to_label\\2"));
	}
	private void saveDataToFile() {
		try {
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			
			Element mainNode=doc.createElement("MashidoPlayer");
			doc.appendChild(mainNode);
			
			Element tabsNode=doc.createElement("openedTabs");
			mainNode.appendChild(tabsNode);
			mainView.saveState(doc, tabsNode);
			
			TransformerFactory transformerFactory=TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 4);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,"yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			//transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			//transformer.transform(new DOMSource(doc), new StreamResult(Config.mainFile));
			
			StringWriter string=new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(string));
			Scanner output=new Scanner(string.toString());
			//var fileOut=new PrintStream(new FileOutputStream(Config.mainFile));
			OutputStreamWriter fileOut=new OutputStreamWriter(new FileOutputStream(new File("data.xml")),StandardCharsets.UTF_8);
			while(output.hasNextLine()) {
				String line=output.nextLine();
				if(!line.trim().isEmpty()) {
					fileOut.write(line);
					fileOut.write("\r\n");
				}
			}
			output.close();
			fileOut.close();
			
			doc.normalize();
		} catch (ParserConfigurationException | TransformerException | IOException e) {
			e.printStackTrace();
			handleFailedToSaveDataFile();
		}
	}
	private void loadDataFile() {
		try {
			File file=new File("data.xml");
			if(!file.exists()) {
				file.createNewFile();
				
				InputStream in=MashidoPlayerMain.class.getResourceAsStream("/application/files/DefaultFile.xml");
				OutputStream out=new FileOutputStream(file);
				
				byte[] buf=new byte[1024];
				int toCopy;
				while((toCopy=in.read(buf))>0) {
					out.write(buf, 0, toCopy);
				}
				
				in.close();
				out.close();
			}
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
			
			Element mainNode=(Element)doc.getElementsByTagName("MashidoPlayer").item(0);
			if(mainNode==null) {
				handleFailedToLoadDataFile();
				return;
			}
			
			Element tabs=(Element)mainNode.getElementsByTagName("openedTabs").item(0);
			if(tabs!=null)
				mainView.loadState(tabs);
			
		} catch(IllegalArgumentException | SecurityException | IOException | SAXException | ParserConfigurationException ex) {
			ex.printStackTrace();
			handleFailedToLoadDataFile();
		}
	}
	/**Shows alert informing user about corruption in datafile, clear it, reset all loaded data to default and loads default data file. 
	 */
	public static void handleFailedToLoadDataFile(){
		assert false:"Error during loading";
		getAlert(AlertType.ERROR,"Critical error","There is corruption in data file.","Please reload the app").showAndWait();
		new File("data.xml").delete();
		System.exit(-2);
	}
	public static void handleFailedToSaveDataFile() {
		assert false:"Error during saving";
		getAlert(AlertType.ERROR,"Error","Error ocured during saving app state.","Actual state(opened dirs, loaded files) will be lost.").showAndWait();
	}
	
	public static void handleFailedToLoadException(IOException ex) {
		ex.printStackTrace();
		
		getAlert(Alert.AlertType.ERROR,"Critical error","Failed to load window composition","One of files in jar file is missing or corrupted").showAndWait();
		
		System.exit(-1);
	}
	
	public static Alert getAlert(AlertType type, String title, String header, String content) {
		Alert Return=new Alert(type);
		Return.getDialogPane().getStylesheets().add(loadedCss);
		
		Return.setTitle(title);
		if(header!=null)Return.setHeaderText(header);
		if(content!=null)Return.setContentText(content);
		
		return Return;
	}
	
	public static boolean isSupportedSoundFile(File file) {
		String name=file.getName();
		name=name.substring(name.lastIndexOf('.')+1);
		
		switch(name) {
		case "wav":
		case "mp3":
		case "mp4":
		case "mp4a":
		case "aif":
		case "aiff":
		case "aifc":
			return true;
		}
		return false;
	}
	
	public static Stage getWindow() {
		return primaryStage;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		launch(args);
	}
}
