package application.interfaces;

/**Use to not finish long lasting loading of element if not necessary.*/
public interface Finishable {
	/**Finish loading of the class.*/
	public void finishLoading();
}