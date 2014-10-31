/*
 * NetViewer
 * Class: ChordalRingPanel
 */

import java.util.Vector;    import java.util.Enumeration;
import java.awt.Dimension;  import java.awt.event.ComponentEvent;
import java.awt.Graphics;   import java.awt.event.ComponentListener;
import java.awt.Point;      import java.awt.Color;

class ChordalRingPanel extends DrawingPanel implements ComponentListener {

  ChordalRingPanel(NetworkPanel parent) {
		super(parent);
    addComponentListener(this);
  }

  public void paintComponent(Graphics g)
  {
		super.paintComponent(g);
  }

	/* Resize component. Recalculate coordinates of nodes and links.
	 */
  public void componentResized(ComponentEvent e)
  {
		resizeBackgroundImage();
		if (drawingAreaIsBlank) {
			repaint(); // background
			return; // nothing to resize
		}
		positionNodesInACirlce();
		setLinkCoords();
		repaint();
	} // resize

  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

}

