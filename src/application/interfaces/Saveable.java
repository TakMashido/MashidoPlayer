package application.interfaces;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface Saveable {
	public void loadState(Element el);
	public void saveState(Document document, Element el);
}