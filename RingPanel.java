/*
 * NetViewer
 * Class: RingPanel
 */

                            import java.awt.event.ComponentEvent;
import java.awt.Graphics;   import java.awt.event.ComponentListener;
import java.awt.Color;      import java.awt.Dimension;

class RingPanel extends DrawingPanel implements ComponentListener {

  RingPanel(NetworkPanel parent) {
		super(parent);
    this.addComponentListener(this);
  }

  public void paintComponent(Graphics g) {
		super.paintComponent(g);
  } // end paintComponent

	/* Resize component. Recalculate coordinates of nodes and links.
	 */
	public void componentResized(ComponentEvent e) {
		resizeBackgroundImage();
		if (drawingAreaIsBlank) {
			repaint(); // background
			return; // nothing to resize
		}
		positionNodesInACirlce();
		setLinkCoords();
		repaint();
	}

  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

}

