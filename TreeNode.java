/*
 * NetViewer
 *
 * Tree Node
 *
 * ** This is an intermediate level between Node and the specific tree algorithms (TreeNodeWakeUp, etc.)
 * 	The class is still abstract (can only be instantiated through a subclass)
 *
 */

import java.util.Vector;
import java.util.Enumeration;

abstract class TreeNode extends Node {

	static final int DEFAULT_MAX_CHILDREN = 7;
	private Link backwardsLink;

	TreeNode(Integer ID) {
		super(ID);
	}

	protected boolean isRoot() {
		return backwardsLink == null; // has no parent
	}

	protected boolean isLeaf() {
		return numChildren() == 0; // has no children
	}

	protected TreeNode getParent() {
		if (isRoot())
			return null; // root has no parent
		if (backwardsLink.getNode(RIGHT) != this)
			return (TreeNode)backwardsLink.getNode(RIGHT);
		else
			return (TreeNode)backwardsLink.getNode(LEFT);
	}

	/* Get the level of this node within the tree. (Recursive)
	 * Level 0 is the root.
	 */
	protected int level() {
		if (isRoot()) // base case
			return 0;
		else
			return 1+getParent().level();
	}

	/* Get the heigt of the entire tree
	 */
	protected int treeHeight() {
		return getRoot().heightOfSubtree();
	}

	/* Get the heigt of this node's subtree.
	 * Uses depth-first traversal
	 */
	protected int heightOfSubtree() {
		// to do (need traversal)
		return 0;
	}

	/* Get the root node. (Recursive)
	 */
	protected TreeNode getRoot() {
		if (isRoot())
			return this;
		else
			return getParent().getRoot();
	}

	/* Add a link to the parent.
	 * Used in algorithms where messages must be sent "upwards", towards the root
	 */
	protected void setBackwardsLink(Link link) {
		backwardsLink = link;
	}

	protected Link getBackwardsLink() {
		return backwardsLink;
	}

	protected Vector getChildren() {
		Vector children = new Vector(numChildren());
		Link link;
		Enumeration allLinks = links.elements();
		while (allLinks.hasMoreElements()) {
			link = (Link)allLinks.nextElement();
			if (link != backwardsLink) {
				if (link.getNode(RIGHT) != this)
					children.add(link.getNode(RIGHT));
				else
					children.add(link.getNode(LEFT));
			}
		}
		return children;
	}


	/* Return the number of (direct) children this node has.
	 */
	protected int numChildren() {
		if (isRoot())
			return links.size();
		else
			return links.size()-1;
	}

	/********* METHODS FOR ENSURING TREE IS DRAWN WITHOUT OVERLAPPING NODES *********/

	protected Vector getSiblings() {
		if (isRoot())
			return null; // root has no siblings
		Vector siblings = getParent().getChildren();
		Enumeration siblings_ = siblings.elements();
		TreeNode tempNode;
		while (siblings_.hasMoreElements()) {
			tempNode = (TreeNode)siblings_.nextElement();
			if (tempNode == this)
				siblings.remove(tempNode);
		}
		return siblings;
	}

	/* Return the siblings on the right/left of this node, as drawn on the network panel.
	 */
	protected Vector siblings(int SIDE) {
		if (isRoot())
			return null; // root has no siblings
		Vector siblings = new Vector(numChildren()); // will contain the final set of siblings on the left/right of this node
		TreeNode tempNode;
		Enumeration siblingsE = getSiblings().elements();
		while (siblingsE.hasMoreElements()) {
			tempNode = (TreeNode)siblingsE.nextElement();
			if (SIDE == Node.LEFT)
			{
				if (tempNode.coords.x < coords.x)
					siblings.add(tempNode);
			}
			else
			{
				if (tempNode.coords.x > coords.x)
					siblings.add(tempNode);
			}
		}
		return siblings; // on the specified side of the tree
	}

	/* Get sibling farthest left/right as drawn on the network panel.
	 */
	protected TreeNode getSiblingFarthest(int LEFT_OR_RIGHT) {
		Vector sibsRL = siblings(LEFT_OR_RIGHT);
		Enumeration siblingsE = sibsRL.elements();
		TreeNode farthestNode = this;
		TreeNode tempNode;
		while (siblingsE.hasMoreElements()) {
			tempNode = (TreeNode)siblingsE.nextElement();
			if (Math.abs(tempNode.coords.x - coords.x) > Math.abs(farthestNode.coords.x - coords.x))
				farthestNode = tempNode;
		}
		return farthestNode;
	}


	/* Return the x coordinate defining the center of the tree.
	 */
	protected int center() {
		return getRoot().coords.x+Node.RADIUS;
	}

	/* Move a node horizontally.
	 * If distance is negative, the node moves left. Otherwise right.
	 * Recursively move all children so the subtree is moved as well.
	 */
	protected void move(int distance) {
		setCoords(coords.x + distance, coords.y);
		// move end coords of backwards link (if not root)
		if (!isRoot())
			backwardsLink.setEndCoords(backwardsLink.getEndCoords().x + distance, backwardsLink.getEndCoords().y);
		// move start coords of each child link
		Link tempLink;
		Enumeration allLinks = links.elements();
		while (allLinks.hasMoreElements()) {
			tempLink = (Link)allLinks.nextElement();
			if (tempLink != backwardsLink) {
				tempLink.setStartCoords(tempLink.getStartCoords().x+distance, tempLink.getStartCoords().y);
			}
		}
		// recursively move children
		Enumeration children = getChildren().elements();
		while (children.hasMoreElements()) {
			((TreeNode)children.nextElement()).move(distance);
		}
	}

	protected boolean leftOfParent() {
		if (isRoot()) return false;
		return coords.x < getParent().coords.x;
	}

	protected boolean rightOfParent() {
		if (isRoot()) return false;
		return coords.x > getParent().coords.x;
	}

	protected boolean centredUnderParent() {
		if (isRoot()) return false;
		return coords.x == getParent().coords.x;
	}
}
