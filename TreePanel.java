/*
 * NetViewer
 * Class: TreePanel
 */


import java.awt.Graphics;      import java.awt.event.ComponentListener;
import java.awt.Color;         import java.awt.event.ComponentEvent;
import java.awt.Rectangle;     import java.awt.event.MouseEvent;
import java.awt.Point;         import java.awt.event.MouseListener;
import java.awt.geom.Line2D;   import java.awt.event.MouseMotionListener;
import java.util.Vector;       import javax.swing.SwingUtilities;
import java.awt.Dimension;     import java.util.Enumeration;

class TreePanel extends DrawingPanel implements MouseListener, MouseMotionListener, ComponentListener {

	static final private int MARGIN_TOP = 3;
	private final double heightOfALevel = 3*Node.DIAMETER;
	private int BUFFER = Node.DIAMETER + Node.RADIUS;

  TreePanel(NetworkPanel parent) {
		super(parent);
    addMouseListener(this);
    addMouseMotionListener(this);
    addComponentListener(this);
	}

  public void paintComponent(Graphics g)
  {
		super.paintComponent(g);
  }

	/* Resize component.
	 * - Reset size of panel (so the scrollpane can adjust)
	 * - Centre tree on panel
	 */
  public void componentResized(ComponentEvent e) {
		resizeBackgroundImage();
		if (drawingAreaIsBlank) {
			repaint(); // background
			return; // nothing to resize
		}
		resizePanel();
		centreTree();
		repaint();
	}

	public void centreTree() {
		getMaxMin();
		TreeNode root = ((TreeNode)parent.networkManager.getNodes().get(0)).getRoot();
		int displacement;
		if (maxX - minX < parent.treeScrollPane.getViewport().getWidth()) // tree is less wide than the view --> centre it
			displacement = -(minX - (parent.treeScrollPane.getViewport().getWidth()/2-(maxX-minX)/2));
		else // tree is wider than viewport --> move it flush to the left
			displacement = -minX;
		root.move(displacement);
	}

	public TreeNode drawRoot() {
		double XCoord = getSize().width/2-Node.RADIUS;
		drawingAreaIsBlank = false;
		TreeNode node = (TreeNode)parent.networkManager.newNode((int)Math.round(5*Math.random())); // generate a random first id between 0 and 5
		node.setCoords(XCoord, MARGIN_TOP);
		repaint();
		return node;
	}

	/* Clicking over a node adds a child to it.
	 */
  public void mouseClicked(MouseEvent e) {
    if (parent.networkManager.isRunning())
    	return; // disallow creation of new nodes while running an algorithm. Also disallows context menu (delete/rename)
		if (parent.clickedOnNode(e)) {
			if (SwingUtilities.isLeftMouseButton(e)) // left click on a node
			{
				TreeNode newNode = addChild((TreeNode)parent.getNodeClickedOn()); // create a new child
				if (newNode != null)
				{
					resizePanel();
					Rectangle rect = new Rectangle(newNode.coords.x, newNode.coords.y, Node.DIAMETER, Node.DIAMETER);
					scrollRectToVisible(rect);
					repaint();
				}
			}
			else if (SwingUtilities.isRightMouseButton(e)) // right click on a node
			{
				parent.getPopupMenu().show(this, e.getX(), e.getY()); // show context menu
			}
		}

	}

	/* Add a child to the given node.
	 * Lay out tree so no nodes overlap.
	 * Public so the network manager can call it when generating trees automatically.
	 */
	public TreeNode addChild(TreeNode parentNode) {
		int maxChildren;
		if (NetViewer.maxChildrenField.getText().equals("")) // the user has deleted what was in the text field; use default maximum
			maxChildren = TreeNode.DEFAULT_MAX_CHILDREN;
		else
			maxChildren = Integer.parseInt(NetViewer.maxChildrenField.getText());
		if (parentNode.numChildren() >= maxChildren)
			return null; // cannot exceed the maximum allowed # of children
		TreeNode newNode;
		Link newLink;
		newNode = (TreeNode)parent.networkManager.newNode(); // a distinct id will be generated for it
		newNode.setCoords(parentNode.coords.x, parentNode.coords.y+heightOfALevel); // dummy x coord will be fixed later
		newLink = parent.networkManager.newLink(new Point(parentNode.coords.x+Node.RADIUS,parentNode.coords.y+Node.DIAMETER), new Point(parentNode.coords.x+Node.RADIUS,parentNode.coords.y+(int)heightOfALevel)); // correct coords will be set for it later (these will stay if it is an only child)
		newLink.setNode(Node.LEFT, parentNode);
		newLink.setNode(Node.RIGHT, newNode);
		newNode.setBackwardsLink(newLink);
		newNode.addLink(newLink);
		parentNode.addLink(newLink);
		repositionChildren(newNode);
		fixTreeLayout(newNode); // fix overlapping subtrees
		fixTree(); // in case more conflicts were created by fixing the initial one
		return newNode;
	}

	private void repositionChildren(TreeNode newNode) {
		if (newNode.getSiblings() == null) // only child
			return; // coordinates already correct. Child is centred under its parent.
		int widthAllChildren = 4*Node.DIAMETER + 3*Node.RADIUS; // enough space for 4 nodes
		TreeNode tempNode;
		int numChildren = newNode.getParent().numChildren();
		Vector siblingsLV = newNode.siblings(Node.LEFT);
		Vector siblingsRV = newNode.siblings(Node.RIGHT);
		if (NetworkManager.even(numChildren))
		{
			Enumeration siblings = newNode.getSiblings().elements();
			while (siblings.hasMoreElements()) { // if there is a node right in the centre (other than the new node) it must move left, so add it to the siblings on the left
				tempNode = (TreeNode)siblings.nextElement();
				if (tempNode.centredUnderParent() && tempNode != newNode) {
					siblingsLV.add(tempNode);
					break;
				}
			}
			siblingsRV.add(newNode);
		}
		Enumeration siblingsL = siblingsLV.elements();
		while (siblingsL.hasMoreElements()) {
			tempNode = (TreeNode)siblingsL.nextElement();
			tempNode.move(-3*Node.DIAMETER/4);
		}
		Enumeration siblingsR = siblingsRV.elements();
		while (siblingsR.hasMoreElements()) {
			tempNode = (TreeNode)siblingsR.nextElement();
			tempNode.move(3*Node.DIAMETER/4);
		}
	}

	private boolean fixTreeLayout(TreeNode newNode) {
		boolean noConflicts = true;
		if (newNode.isRoot() /*|| newNode.getParent().isRoot()*/)
			return true; // there will be no layout conflicts at the first level
		TreeNode tempNode, tempSibling;
		int level = newNode.level();
		Vector newNodeAndSiblingsV = newNode.getSiblings();
		newNodeAndSiblingsV.add(newNode);
		Vector allNodesV = parent.networkManager.getNodes();
		Vector nodesToTestAgainstV = new Vector();
		Enumeration allNodesE = allNodesV.elements();
		while (allNodesE.hasMoreElements()) {
			tempNode = (TreeNode)allNodesE.nextElement();
			if (tempNode.level() != level)
			{
				// do nothing; not interested in nodes at other levels
			}
			else
			{
				if (!newNodeAndSiblingsV.contains(tempNode))
					nodesToTestAgainstV.add(tempNode);
			}
		}
		// Go through each sibling and test if it conflicts with other
		// nodes at the same level (not including its own siblings).
		int overlap = 0;
		TreeNode parentSib, parentTest, nodeToMove;
		Enumeration newNodeAndSiblingsE = newNodeAndSiblingsV.elements();
		while (newNodeAndSiblingsE.hasMoreElements()) {
			tempSibling = (TreeNode)newNodeAndSiblingsE.nextElement();
			Enumeration nodesToTestAgainstE = nodesToTestAgainstV.elements();
			while (nodesToTestAgainstE.hasMoreElements()) {
				tempNode = (TreeNode)nodesToTestAgainstE.nextElement();
				parentSib = tempSibling.getParent();
				parentTest = tempNode.getParent();

				if (nodesOverlap(tempSibling, tempNode)) { // there is a conflict
					noConflicts = false;
					while (!parentSib.getSiblings().contains(parentTest)) { // move up the tree until ancestors of conflicting nodes are siblings
						parentSib = parentSib.getParent();
						parentTest = parentTest.getParent();
					}
					overlap = overlap(tempSibling, tempNode);
					if (parentSib.centredUnderParent()) {
						if (parentTest.leftOfParent())
							BUFFER *= -1;
						moveNodes(parentTest, overlap+BUFFER); // move all nodes on the left/right of the node (they may be in its path)
						parentTest.move(overlap+BUFFER); // move original parent (or ancestor) of conflicting node
					}
					else if (parentTest.centredUnderParent()) {
						overlap *= -1;
						if (parentSib.leftOfParent())
							BUFFER *= -1;
						moveNodes(parentSib, overlap+BUFFER); // move all nodes on the left/right of the node (they may be in its path)
						parentSib.move(overlap+BUFFER); // move original parent (or ancestor) of conflicting node
					}
					else if (parentSib.leftOfParent() && parentTest.leftOfParent()) {
						BUFFER *= -1;
						if (overlap > 0) {
							overlap *= -1;
							if (linksCross(tempSibling, tempNode)) {
								moveNodes(parentSib, overlap+BUFFER);
								parentSib.move(overlap+BUFFER);
							}
							else { // nodesTooClose
								moveNodes(parentTest, BUFFER-overlap);
								parentTest.move(BUFFER-overlap);
							}
						}
						else { // overlap negative
							if (linksCross(tempSibling, tempNode)) {
								moveNodes(parentTest, overlap+BUFFER);
								parentTest.move(overlap+BUFFER);
							}
							else { // nodesTooClose
								moveNodes(parentSib, BUFFER-overlap);
								parentSib.move(BUFFER-overlap);
							}
						}
					}
					else if (parentSib.rightOfParent() && parentTest.rightOfParent()) {
						if (overlap > 0) {
							if (linksCross(tempSibling, tempNode)) {
								moveNodes(parentTest, overlap+BUFFER);
								parentTest.move(overlap+BUFFER);
							}
							else { // nodesTooClose
								moveNodes(parentSib, BUFFER-overlap);
								parentSib.move(BUFFER-overlap);
							}
						}
						else { // overlap negative
							overlap *= -1; // make positive (moving right)
							if (linksCross(tempSibling, tempNode)) {
								moveNodes(parentSib, overlap+BUFFER);
								parentSib.move(overlap+BUFFER);
							}
							else { // nodesTooClose
								moveNodes(parentTest, BUFFER-overlap);
								parentTest.move(BUFFER-overlap);
							}
						}
					}
					else { // move both nodes
						if (linksCross(tempSibling, tempNode))
						{
							if (parentSib.leftOfParent()) {
								BUFFER *= -1;
							}
							if (!NetworkManager.even(overlap)) // make it even to avoid perptual conflicts
							{
								if (overlap < 0)
									overlap--;
								else
									overlap++;
							}
							moveNodes(parentSib, (-1*overlap + BUFFER)/2); // move siblings of parents (or ancestors)
							moveNodes(parentTest, (overlap + -1*BUFFER)/2);
							parentSib.move((-1*overlap + BUFFER)/2); // move original parents (or ancestors) of conflicting nodes
							parentTest.move((overlap + -1*BUFFER)/2);
						}
						else // nodesTooClose
						{
							int x = Math.abs(BUFFER)-Math.abs(overlap);
							if (!NetworkManager.even(x)) // make it even to avoid perptual conflicts
								x++;
							if (overlap < 0) { // sib is to the left of test and must move further left
								x *= -1;
							}
							moveNodes(parentTest, -1*x/2);
							moveNodes(parentSib, x/2);
							parentSib.move(x/2);
							parentTest.move(-1*x/2);
						}
					}
					BUFFER = Node.DIAMETER + Node.RADIUS; // reset for next time
				}
			}
		}
		return noConflicts;
	}

	private boolean nodesOverlap(TreeNode n1, TreeNode n2) {
		if (linksCross(n1, n2))
		{
			return true;
		}
		else // lines don't intersect. Check if nodes are too close.
		{
			if (nodesTooClose(n1, n2))
			{
				return true;
			}
		}
		return false;
	}

	private int overlap(TreeNode n1, TreeNode n2) {
		return n1.coords.x - n2.coords.x;
	}

	private boolean linksCross(TreeNode n1, TreeNode n2) {
		TreeNode n1Parent = n1.getParent();
		TreeNode n2Parent = n2.getParent();
		if (Line2D.linesIntersect(n1Parent.coords.x, n1Parent.coords.y, n1.coords.x, n1.coords.y, n2Parent.coords.x, n2Parent.coords.y, n2.coords.x, n2.coords.y))
			return true;
		else
			return false;
	}

	private boolean nodesTooClose(TreeNode n1, TreeNode n2) {
		if (Math.abs(n1.coords.x - n2.coords.x) < Math.abs(BUFFER)) {
			return true;
		}
		return false;
	}

	private void moveNodes(TreeNode node, int distance) {
		Vector siblings = node.getSiblings(); // error: should be all nodes at this level, not necessarily just siblings!
		if (siblings == null) return; // nothing to move
		TreeNode tempNode = null;
		for (int i=0; i<siblings.size(); i++)
		{
			tempNode = (TreeNode)siblings.get(i);
			if (distance < 0) { // moving left
				if (tempNode.coords.x < node.coords.x) {
					tempNode.move(distance);
				}
			}
			else { // moving right
				if (tempNode.coords.x > node.coords.x) {
					tempNode.move(distance);
				}
			}
		}
	}

	public void fixTree() {
		boolean conflictsExist = false;
		Enumeration allNodes;
		do { // check whole tree for conflicts until all are eliminated (loop because fixing one conflict may cause another elsewhere in the tree)
			conflictsExist = false;
			repaint();
			allNodes = parent.networkManager.getNodes().elements();
			while (allNodes.hasMoreElements()) {
				if (fixTreeLayout((TreeNode)allNodes.nextElement()) == false)
					conflictsExist = true;
			}
		} while (conflictsExist);
	}

	/* Resize the panel to the size of the network.
	 * Makes scrollbars appear when necessary.
	 */
	protected void resizePanel() {
		if (drawingAreaIsBlank) { // may have become blank after deleting the root manually, so panel must still be shrunk
			setPreferredSize(new Dimension(0,0)); // put back to small size so scrollbars disappear
			revalidate();
			return;
		}
		getMaxMin(); // get the extent of the tree to the left, right, bottom
		if (minX < 0) {
			((TreeNode)parent.networkManager.getNodes().get(0)).getRoot().move(-minX); // move whole tree right
			maxX += -minX;
		}
		if (minX > 0 && maxX >= parent.treeScrollPane.getViewport().getWidth()) // tree has extended outside the view on the right
		{
			TreeNode root = ((TreeNode)parent.networkManager.getNodes().get(0)).getRoot();
			int displacement;
			if (maxX - minX < parent.treeScrollPane.getViewport().getWidth()) // tree is less wide than the view --> centre it
				displacement = -(minX - (parent.treeScrollPane.getViewport().getWidth()/2-(maxX-minX)/2));
			else // tree is wider than viewport; move it flush to the left
				displacement = -minX;
			root.move(displacement);
			maxX += displacement;
		}
		setPreferredSize(new Dimension(maxX, maxY));
		revalidate();
	}

  // Moving a node and all its children (and links)
  public void mouseDragged(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

}

