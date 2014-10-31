/*
 * NetViewer
 * Class: NetworkPanel
 */

import java.util.Vector;        import java.awt.event.ActionListener;
import java.util.Enumeration;   import java.awt.event.ActionEvent;
import java.awt.BorderLayout;   import java.awt.event.MouseEvent;
import java.awt.Point;          import java.awt.event.MouseListener;
import javax.swing.JPanel;      import javax.swing.SwingUtilities;
import javax.swing.JMenuItem;   import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;  import javax.swing.JScrollPane;
import java.awt.Rectangle;

class NetworkPanel extends JPanel implements MouseListener {

	public NetworkManager networkManager; // public so we can access it directly from the drawing panels
	private RingPanel ringPanel;
	private GridPanel gridPanel;
	private TreePanel treePanel;
	private ArbitraryPanel arbitraryPanel;
	private CompleteGraphPanel completeGraphPanel;
	private ChordalRingPanel chordalRingPanel;
	private TorusPanel torusPanel;
	private JPopupMenu popupMenu;
	private JMenuItem delete, rename;
	private DrawingPanel lastDirtyCanvas;
	private Node nodeClickedOn;
	public JScrollPane treeScrollPane, arbScrollPane;

	NetworkPanel(NetworkManager networkManager_) {
		this.networkManager = networkManager_;
		setLayout(new BorderLayout());
		this.addMouseListener(this);
		ringPanel = new RingPanel(this);
		gridPanel = new GridPanel(this);
		treePanel = new TreePanel(this);
		treeScrollPane = new JScrollPane(treePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // it is possible that the tree grows past the borders of the network panel, so we need scrollbars.
		treeScrollPane.setBorder(null);
		arbitraryPanel = new ArbitraryPanel(this);
		arbScrollPane = new JScrollPane(arbitraryPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // it is possible that the tree grows past the borders of the network panel, so we need scrollbars.
		arbScrollPane.setBorder(null);
		completeGraphPanel = new CompleteGraphPanel(this);
		chordalRingPanel = new ChordalRingPanel(this);
		torusPanel = new TorusPanel(this);
		lastDirtyCanvas = ringPanel;
		popupMenu = new JPopupMenu();
		delete = new JMenuItem("Delete");
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (networkManager.getNetworkType().equals("Arbitrary")) { // delete arbitrary node
					Link link;
					Node node;
					Vector links;
					links = ((Node)nodeClickedOn).getLinks();
					while (links.size() > 0) // Remove node's links
					{
						link = (Link)links.get(0);
						((Node)link.getNode(Node.LEFT)).removeLink(link);
						((Node)link.getNode(Node.RIGHT)).removeLink(link);
						networkManager.removeLink(link);
						link = null;
					}
					networkManager.removeNode(nodeClickedOn);
					if (networkManager.getNumNodes() == 0)
						getDrawingArea().setIsBlank(true);
					((ArbitraryPanel)getDrawingArea()).resizePanel();
				}
				else { // Delete tree node
					TreeNode node = (TreeNode)nodeClickedOn;
					TreeNode parent = null;
					if (!node.isRoot())
					{
						Enumeration siblingsToMove = node.siblings(Node.LEFT).elements();
						while (siblingsToMove.hasMoreElements()) // move siblings on left to the right to fill the gap where the deleted node was
							((TreeNode)siblingsToMove.nextElement()).move(3*Node.RADIUS/2);
						siblingsToMove = node.siblings(Node.RIGHT).elements();
						while (siblingsToMove.hasMoreElements()) // move siblings on right to the left to fill the gap where the deleted node was
							((TreeNode)siblingsToMove.nextElement()).move(-3*Node.RADIUS/2);
						parent = node.getParent();
					}
					removeTreeNode(node);
					if (!node.isRoot())
					{
						if (parent.numChildren() == 1) // if we are left with an only child, position it centrally under its parent
						{
							TreeNode onlyChild = (TreeNode)parent.getChildren().get(0);
							onlyChild.move(parent.coords.x-onlyChild.coords.x);
						}
					}
					TreePanel canvas = (TreePanel)getDrawingArea();
					if (networkManager.getNumNodes() == 0)
						canvas.setIsBlank(true);
					else {
						canvas.fixTree(); // in case conflicts were created when this node was deleted (subtrees may have collided)
						canvas.centreTree();
					}
					canvas.resizePanel(); // in case we can reduce the size now that a node has been deleted
				}
				repaint();
      }
    });
    rename = new JMenuItem("Rename");
    rename.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
			String message = "Change node ID.";
			boolean finished = false;
			String s, newIDstring;
			Integer newID;
			do {
			  s = JOptionPane.showInputDialog(null, message, "Data Input", JOptionPane.PLAIN_MESSAGE);
			  if (s == null)
					finished = true;
			  else {						// Validation
					newIDstring = s.trim();
					if (newIDstring.equals(""))
						finished = true;
					else { // proceed
				  	try {
							newID = new Integer(newIDstring);
							if (Node.ids().contains(newID) && !newIDstring.equals(nodeClickedOn.toString()))
								message = "That ID is already in use. Please choose another.";
						  else {
								if (!newIDstring.equals(nodeClickedOn.toString())) {
									Node.switchId(new Integer(nodeClickedOn.id), newID);
									nodeClickedOn.setID(newID.intValue());
								}
								finished = true;
						   }
						} catch (NumberFormatException ex) {
							message = "The ID must be a number and cannot contain internal spaces.";
				    }
			    } // else
			  } // else
			} while (!finished);
			getComponentAt(0,0).repaint();
      }
    });
    popupMenu.add(rename);
  } // constructor

	/* Switch in the panel that knows how to draw the
	 * desired network type.
	 * switchingTo: the network type (topology) to switch to
	 */
	public void switchTo(String switchingTo) {
		if (getComponentAt(0,0) != null) {
			if (!getDrawingArea().isBlank())
				lastDirtyCanvas = getDrawingArea();
			remove(getComponentAt(0,0)); // remove drawing panel or scroll pane containing a drawing panel
		}
    if (switchingTo.equals("Ring")) {
      add(ringPanel, BorderLayout.CENTER);
			popupMenu.remove(delete);
    }
    else if (switchingTo.equals("Grid")) {
      add(gridPanel, BorderLayout.CENTER);
      popupMenu.remove(delete);
    }
    else if (switchingTo.equals("Tree")) {
      add(treeScrollPane, BorderLayout.CENTER);
      popupMenu.add(delete);
    }
    else if (switchingTo.equals("Arbitrary")) {
      add(arbScrollPane, BorderLayout.CENTER);
      popupMenu.add(delete);
    }
    else if (switchingTo.equals("Complete Graph")) {
      add(completeGraphPanel, BorderLayout.CENTER);
      popupMenu.remove(delete);
    }
    else if (switchingTo.equals("Chordal Ring")) {
      add(chordalRingPanel, BorderLayout.CENTER);
      popupMenu.remove(delete);
    }
    else if (switchingTo.equals("Torus")) {
      add(torusPanel, BorderLayout.CENTER);
      popupMenu.remove(delete);
    }
    validate();
    repaint();	// shows blank if no network created (paints background only)
	}

  public void mouseClicked(MouseEvent e) {
		if (networkManager.isRunning()) return; // disallow context menu while executing an algorithm
		if (SwingUtilities.isRightMouseButton(e)) { // right click
			if (clickedOnNode(e))
				popupMenu.show(this, e.getX(), e.getY());
		}
  } // mouseClicked

  public void mousePressed(MouseEvent e) {}
  public void mouseDragged(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {} // when mouse enters panel
  public void mouseExited(MouseEvent e) {} // when mouse exits panel
  public void mouseMoved(MouseEvent e) {}

  /* Helper function to determine if the user clicked on a node
   * Saves the node clicked on in a private class variable
   */
  public boolean clickedOnNode(MouseEvent e) {
		if (networkManager.getNumNodes() == 0) { // no nodes; cannot possibly click on a node
			nodeClickedOn = null;
			return false;
		}
		Node node = null;
		for (int i = networkManager.getNumNodes()-1; i >= 0; i--) { // go backwards so nodes on top will be affected first (dragged, links attached, ...)
			node = networkManager.getNode(i);
			Rectangle nodeArea = new Rectangle(node.coords.x, node.coords.y, Node.DIAMETER, Node.DIAMETER);
	  	if (nodeArea.contains(e.getPoint()))
			{
				nodeClickedOn = node;
				return true;
	  	}
		}
		nodeClickedOn = null; // clears node clicked on when user clicks in open
		return false;
  }

  public JPopupMenu getPopupMenu() {
		return popupMenu;
  }

  public Node getNodeClickedOn() {
		return nodeClickedOn;
  }

  public DrawingPanel getDrawingArea() {
		try {
			return (DrawingPanel)getComponentAt(0,0);
		}
		catch (ClassCastException e) { // for the tree, the scrollpane is the first element
			JScrollPane jscp = (JScrollPane)getComponentAt(0,0);
			return (DrawingPanel)jscp.getViewport().getView();
		}
  }

  public DrawingPanel getLastDirtyCanvas() {
			return lastDirtyCanvas;
  }

  /* Remove a tree node. (Recursive)
   * Remove the node and all its children
   * Used when deleting a node in the tree network.
   */
  private void removeTreeNode(TreeNode node) {
		if (node.isLeaf()) // remove self and backwards link
		{
			if (!node.isRoot())
			{
				node.getParent().removeLink(node.getBackwardsLink()); // remove backwards link from parent node
				networkManager.removeLink(node.getBackwardsLink()); // remove backwards link from central collection
			}
			networkManager.removeNode(node); // remove self
		}
		else // not a leaf; remove children and self
		{
			Enumeration children = node.getChildren().elements();
			while (children.hasMoreElements()) // remove children
				removeTreeNode((TreeNode)children.nextElement());
			if (!node.isRoot())
			{
				node.getParent().removeLink(node.getBackwardsLink()); // remove backwards link from parent node
				networkManager.removeLink(node.getBackwardsLink()); // remove backwards link from central collection
			}
			networkManager.removeNode(node); // remove self
		}
		if (networkManager.getNumNodes() == 0)
			getDrawingArea().setIsBlank(true);
	}


}

