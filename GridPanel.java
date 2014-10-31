/*
 * NetViewer
 * Class: GridPanel
 */

import java.util.Vector;    import java.util.Enumeration;
import java.awt.Dimension;  import java.awt.event.ComponentEvent;
import java.awt.Graphics;   import java.awt.event.ComponentListener;
import java.awt.Color;

class GridPanel extends DrawingPanel implements ComponentListener {

  private int rows, cols;

  GridPanel(NetworkPanel parent) {
		super(parent);
    this.addComponentListener(this);
  }

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
  }

	/* Resize component. Recalculate coordinates of nodes and links.
	 */
  public void componentResized(ComponentEvent e) {
		resizeBackgroundImage();
		if (drawingAreaIsBlank) {
			repaint(); // background
			return; // nothing to resize
		}
    Dimension size = getSize();
    rows = parent.networkManager.getNumRows();
    cols = parent.networkManager.getNumColumns();
    double dif_x = (3*size.width)/(4*(cols-1));
    double dif_y = (3*size.height)/(4*(rows-1));
    double downLinkLength = dif_y - Node.DIAMETER;
    double acrossLinkLength = dif_x - Node.DIAMETER;
    double x = size.width/8-Node.RADIUS/2;
    double y = size.height/8-Node.RADIUS/2;
    Link link;
    Node node;
    double next_x, next_y, this_x, this_y, startLink_x, startLink_y, endLink_x, endLink_y;
		// set node and link coordinates in preparation for drawing
		Enumeration links = parent.networkManager.getLinks().elements();
		Enumeration nodes = parent.networkManager.getNodes().elements();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				this_x = x + j * dif_x;
				this_y = y + i * dif_y;
				((Node)nodes.nextElement()).setCoords(this_x,this_y);
				if (i < rows-1) // there is a down link
				{
					link = (Link)links.nextElement();
					startLink_x = this_x+Node.RADIUS;
					startLink_y = this_y+Node.DIAMETER;
					endLink_y = startLink_y + downLinkLength;
					link.setStartCoords(startLink_x, startLink_y);
					link.setEndCoords(startLink_x, endLink_y);
				}
				if (j < cols-1) // there is an across link
				{
					link = (Link)links.nextElement();
					startLink_x = this_x+Node.DIAMETER;
					startLink_y = this_y+Node.RADIUS;
					endLink_x = startLink_x + acrossLinkLength;
					link.setStartCoords(startLink_x, startLink_y);
					link.setEndCoords(endLink_x, startLink_y);
				}
			}
		}
		repaint();
  }

  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

}
