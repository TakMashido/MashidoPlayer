package application.interfaces;

import application.view.PlayerPane;

/**
 * Interface for components having {@link PlayerPane} as child
 * @author TakMashido
 */
public interface PlayerHolder {
	/**Invoced when PlayerPane request it's close
	 * @param pane layerPane requesting close.
	 */
	public void stop(PlayerPane pane);
}