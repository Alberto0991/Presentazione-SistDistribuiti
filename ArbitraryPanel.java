/*
 * NetViewer
 * Class: ArbitraryPanel
 */

import java.awt.Dimension;     import java.awt.event.MouseEvent;
import java.awt.Graphics;      import java.awt.event.MouseListener;
import java.awt.Color;         import java.awt.event.MouseMotionListener;
import java.awt.Point;         import javax.swing.SwingUtilities;
import java.util.Vector;       import java.awt.event.ComponentListener;
import java.util.Enumeration;  import java.awt.event.ComponentEvent;

class ArbitraryPanel extends DrawingPanel implements MouseListener, MouseMotionListener, ComponentListener {

	private Node originalNode;
	private double offsetX, offsetY; // used when dragging a node to a new position
	private Point nodeCentre; // for starting point of link leading from this node
	private Link linkBeingDrawn;

	ArbitraryPanel(NetworkPanel parent) {
		super(parent);
		addMouseListener(this);
		addMouseMotionListener(this);
    addComponentListener(this);
		linkBeingDrawn = null;
	}

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
  }

  public void mouseClicked(MouseEvent e) {
    if (parent.networkManager.isRunning()) return; // disallow creation of new nodes or links while an algorithm is executing - also disallows context menu (delete/rename)
    if (parent.clickedOnNode(e)) {
			if (SwingUtilities.isLeftMouseButton(e)) { // left click
				if (linkBeingDrawn == null) { // start drawing a link
					nodeCentre = new Point(parent.getNodeClickedOn().coords.x+Node.RADIUS, parent.getNodeClickedOn().coords.y+Node.RADIUS); // for starting point of link leading from this node
					linkBeingDrawn = parent.networkManager.newLink(nodeCentre, nodeCentre); // start & end coords are initially the same; will grow as user moves mouse
					((Node)parent.getNodeClickedOn()).addLink(linkBeingDrawn);
					linkBeingDrawn.setNode(Node.LEFT, parent.getNodeClickedOn());
					originalNode = parent.getNodeClickedOn();
				}
				else { // link already being drawn
					if (originalNode.id == parent.getNodeClickedOn().id || linkAlreadyExistsBetween(originalNode, parent.getNodeClickedOn())) { // remove link from original node
						parent.networkManager.removeLink(linkBeingDrawn);
						((Node)originalNode).removeLink(linkBeingDrawn);
						linkBeingDrawn = null;
						originalNode = null;
						repaint();
					}
					else { // complete the link; set end coords
						Point centre1 = new Point(linkBeingDrawn.getStartCoords().x, linkBeingDrawn.getStartCoords().y);
						Point centre2 = new Point(parent.getNodeClickedOn().coords.x+Node.RADIUS, parent.getNodeClickedOn().coords.y+Node.RADIUS);
						Point[] points = findPointsOnCircumference(centre1, centre2);
						linkBeingDrawn.setStartCoords(points[0].x, points[0].y);
						linkBeingDrawn.setEndCoords(points[1].x, points[1].y);
						linkBeingDrawn.setNode(Node.RIGHT, parent.getNodeClickedOn());
						((Node)parent.getNodeClickedOn()).addLink(linkBeingDrawn);
						linkBeingDrawn = null;
						originalNode = null;
						repaint();
					}
				}
			}
      else if (SwingUtilities.isRightMouseButton(e)) { // and clicked on a node
	  		parent.getPopupMenu().show(this, e.getX(), e.getY());
			}
    } // clicked on a node
    else if (SwingUtilities.isLeftMouseButton(e)) { // left click
			if (linkBeingDrawn != null) { // clicked in the open for end of link; no good; delete link
				parent.networkManager.removeLink(linkBeingDrawn);
				((Node)originalNode).removeLink(linkBeingDrawn);
				linkBeingDrawn = null;
				originalNode = null;
				repaint();
			}
			else { // create a new node where the mouse was clicked
				if (!parent.networkManager.getNetworkType().equals("Arbitrary"))
					parent.networkManager.initializeNetwork();
				Node node = parent.networkManager.newNode(); // a distinct id will be generated for it
				node.setCoords(e.getPoint().x-Node.RADIUS, e.getPoint().y-Node.RADIUS);
				drawingAreaIsBlank = false;
				NetViewer.topologyLabel.setText("Arbitrary");
				NetViewer.algorithmLabel.setText(parent.networkManager.getAlgorithm());
				resizePanel(); // Resize panel to the size of the network with the new node. (So scrollbars appear when necessary)
				repaint();
			}
    }
  }

  public void mousePressed(MouseEvent e) {
		if (parent.clickedOnNode(e)) {
			offsetX = e.getX() - parent.getNodeClickedOn().coords.x; // for dragging
			offsetY = e.getY() - parent.getNodeClickedOn().coords.y; // for dragging
		}
  }

  // Moving a node
  public void mouseDragged(MouseEvent e) {
		if (drawingAreaIsBlank) {
			return;
		}
    if (linkBeingDrawn != null && parent.getNodeClickedOn() == null) { // clicked and dragged in the open while drawing a link; no good; delete link
			parent.networkManager.removeLink(linkBeingDrawn);
			((Node)originalNode).removeLink(linkBeingDrawn);
			linkBeingDrawn = null;
    }
    else if (parent.getNodeClickedOn() != null) {
  		if (linkBeingDrawn != null) { // clicked and dragged over a node while drawing a link - save end coords of link
    		if (originalNode.id == parent.getNodeClickedOn().id || linkAlreadyExistsBetween(originalNode, parent.getNodeClickedOn())) { // remove link from original node
      		parent.networkManager.removeLink(linkBeingDrawn);
      		((Node)originalNode).removeLink(linkBeingDrawn);
    		}
    		else { // to link to end node if not already done
					Vector links;
					links = ((Node)parent.getNodeClickedOn()).getLinks();
					if (!links.contains(linkBeingDrawn)) {
        		((Node)parent.getNodeClickedOn()).addLink(linkBeingDrawn);
	  				linkBeingDrawn.setNode(Node.RIGHT, parent.getNodeClickedOn());
					}
	    	}
	    	linkBeingDrawn = null;
	    	originalNode = null;
	  	}
		  // move the node and all its links
		  int x = e.getX();
		  int y = e.getY();
		  // keep within drawing area
		  if (x-offsetX < 0) x = (int)offsetX;
		  if (y-offsetY < 0) y = (int)offsetY;
		  Dimension dim = getSize();
		  if (x+Node.DIAMETER-offsetX > dim.getWidth())  x = (int)(dim.getWidth()  - Node.DIAMETER + offsetX);
		  if (y+Node.DIAMETER-offsetY > dim.getHeight()) y = (int)(dim.getHeight() - Node.DIAMETER + offsetY);
		  Point newLocation = new Point((int)(x-offsetX), (int)(y-offsetY));
		  Point centre2 = new Point(parent.getNodeClickedOn().coords.x+Node.RADIUS, parent.getNodeClickedOn().coords.y+Node.RADIUS);
		  Vector links = ((Node)parent.getNodeClickedOn()).getLinks();
		  Link link;
		  nodeCentre = new Point(parent.getNodeClickedOn().coords.x+Node.RADIUS, parent.getNodeClickedOn().coords.y+Node.RADIUS); // for starting point of link leading from this node
		  Node otherNode;
		  for (int i = 0; i < links.size(); i++) { // reset link coords
				link = (Link)links.get(i);
				if (parent.getNodeClickedOn() == link.getNode(Node.LEFT)) // if start of link is node clicked on
					otherNode = link.getNode(Node.RIGHT);
				else
					otherNode = link.getNode(Node.LEFT);
				Point centre1 = new Point(otherNode.coords.x+Node.RADIUS, otherNode.coords.y+Node.RADIUS);
				Point[] points = findPointsOnCircumference(centre1, centre2);
				if (parent.getNodeClickedOn() == link.getNode(Node.LEFT)) { // if start of link is attached to the node clicked on
					link.setStartCoords(points[1].x, points[1].y);
					link.setEndCoords(points[0].x, points[0].y);
				}
				else {
					link.setStartCoords(points[0].x, points[0].y);
					link.setEndCoords(points[1].x, points[1].y);
				}
			} // for each link
			parent.getNodeClickedOn().setCoords(newLocation.x, newLocation.y);
			resizePanel(); // resize in case user dragged a node farther right or down than any existing node.
    }
    repaint();
  }

  /* Drawing a link. The user has clicked over a node and is moving
   * the mouse to another node to anchor the link.
   */
  public void mouseMoved(MouseEvent e) {
		if (linkBeingDrawn != null) {
			linkBeingDrawn.setEndCoords(e.getPoint().x,e.getPoint().y);
			repaint();
		}
  }

  // Helper function
  public boolean linkAlreadyExistsBetween(Node n1, Node n2) {
		Link link;
		int dir;
		for (int i=0; i<((Node)n1).getLinks().size(); i++) {
			link = n1.getLink(i);
			if (link.getNode(Node.LEFT) == n1)
				dir = Node.LEFT;
			else	dir = Node.RIGHT;
			if (link.getNode(Math.abs(dir-1)) == n2)
				return true;
		}
		return false;
  }

	/* Resize the panel to the size of the network.
	 * Makes scrollbars appear when necessary.
	 */
	protected void resizePanel() {
		if (drawingAreaIsBlank) { // may have become blank after deleting the last node manually, so panel must still be shrunk
			setPreferredSize(new Dimension(0,0)); // put back to small size so scrollbars disappear
			revalidate();
			return;
		}
		getMaxMin(); // get the extent of the tree to the left, right, bottom
		setPreferredSize(new Dimension(maxX, maxY));
		revalidate();
	}

  public void mouseEntered(MouseEvent e) {} // when mouse enters panel
  public void mouseExited(MouseEvent e) {} // when mouse exits panel
  public void mouseReleased(MouseEvent e) {}

  /* Check if a node is too close to any others in the network.
   * Used when generating an arbitrary network automatically
   * so that nodes do not overlap. If nodes were allowed to
   * overlap, some links might be hidden.
   */
  public boolean tooCloseToOtherNodes(Node nodeToCheck) {
		Enumeration allNodes = parent.networkManager.getNodes().elements();
		Node node = null;
		Point centre1, centre2;
		while (allNodes.hasMoreElements()) {
			node = (Node)allNodes.nextElement();
			if (node != nodeToCheck) {
				centre1 = node.getCentre();
				centre2 = nodeToCheck.getCentre();
				if (centre1.distance(centre2) < Node.DIAMETER + Node.RADIUS)
					return true;
			}
		}
		return false;
	}

	/* Resize component. Resize background image.
	 */
  public void componentResized(ComponentEvent e) {
		resizeBackgroundImage();
		repaint();
	}

  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

}

