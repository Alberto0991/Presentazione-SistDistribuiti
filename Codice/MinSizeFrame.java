import java.awt.Frame;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;

public class MinSizeFrame extends Frame implements ComponentListener {

	static final int MIN_WIDTH = 300;
	static final int MIN_HEIGHT = 400;

	public MinSizeFrame(String title) {
		super(title);
		addComponentListener(this);
	}

	public void componentResized(ComponentEvent e) {

	 int width = getWidth();
	 int height = getHeight();
	 boolean resize = false;

	 // check if either width or height is below minimum
		if (width < MIN_WIDTH)
		{
			resize = true;
			width = MIN_WIDTH;
		}
		if (height < MIN_HEIGHT)
		{
			resize = true;
			height = MIN_HEIGHT;
		}
		if (resize)
		{
			setSize(width, height);
		}
	}

	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentHidden(ComponentEvent e) {}

}
