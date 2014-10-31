/*
 * NetViewer
 *
 * Network Manager: Performs tasks for the network
 * 	- Network creation & initialization
 *  - Changing algorithm
 *  - Changing case (best/worst/average)
 *  - Resetting network to re-run an algorithm
 *  - Making network synchronous when requested
 */

import java.awt.Point;         import java.awt.Dimension;
import java.lang.Class;        import java.util.StringTokenizer;
import java.util.Vector;       
import javax.swing.JOptionPane;
import javax.swing.JComboBox;  
import java.util.Enumeration;

import javax.swing.JPanel;     
import java.awt.event.ActionListener;

import javax.swing.Timer;      
import java.awt.event.ActionEvent;

class NetworkManager implements ActionListener {

	private int numNodes, numLinks, rows, cols, previousNumNodes, previousNumLinks, previousNumRows, previousNumCols, mirrors, tick;
	private Vector nodesVector, linksVector, previousNodesVector, previousLinksVector, previousIdsUsed, chordLengths;
	private String algorithm, networkType, previousAlgorithm, previousNetworkType;
	private boolean isRunning, isNewNetwork;
	private Class[] integerClass; // helper during dynamic object creation
	protected Timer syncTimer;	// Timer for synchronous networks
	protected long lastTick, a; // last timer tick, for synchronous networks

	NetworkManager() {
    algorithm = "";
    networkType = "";
		isRunning = false;
		nodesVector = new Vector();
		linksVector = new Vector();
		// Timer used for synchronization
		syncTimer = new Timer(0, this);
		syncTimer.setInitialDelay(0);
		syncTimer.setCoalesce(true);
		try {
			integerClass = new Class[] {Class.forName("java.lang.Integer")};
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void createRingNetwork(int numNodes_, String algorithm_, String whichCase) {
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		networkType = "Ring";
		numNodes = numNodes_;
		numLinks = numNodes_;
		algorithm = algorithm_;

		createNodes(whichCase);

		// Create links
		for (int i=1; i<=numNodes; i++) {
			if (i != numNodes)
				linksVector.add(i-1, new Link((Node)nodesVector.get(i-1), (Node)nodesVector.get(i)));
			else
				linksVector.add(i-1, new Link((Node)nodesVector.get(i-1), (Node)nodesVector.get(0)));
		}

		// Store links into the nodes at either end
		for (int i=0; i<numNodes; i++) {
			int leftIndex = i-1;
			int rightIndex = i;
			if (i==0)	leftIndex = numNodes-1;
			((Node)nodesVector.get(i)).setLink(Node.RIGHT, (Link)linksVector.get(rightIndex));
			((Node)nodesVector.get(i)).setLink(Node.LEFT, (Link)linksVector.get(leftIndex));
		}

		isNewNetwork = true; // forces resize upon next drawing, to recalculate network dimensions
	}

	public void createCompleteGraphNetwork(int numNodes_, String algorithm_, String whichCase) {
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		networkType = "Complete Graph";
		numNodes = numNodes_;
		numLinks = (numNodes*(numNodes-1))/2;
		algorithm = algorithm_;

		createNodes(whichCase);

		// Create links
		Node targetNode, anchorNode;
		Link newLink;
		for (int i=0; i<numNodes-1; i++) {
			anchorNode = (Node)nodesVector.get(i);
			for (int j=i+1; j<numNodes; j++) {
				targetNode = (Node)nodesVector.get(j);
				newLink = new Link(anchorNode, targetNode);
				anchorNode.addLink(newLink);
				targetNode.addLink(newLink);
				linksVector.add(newLink);
			}
		}

		isNewNetwork = true; // forces resize upon next drawing, to recalculate network dimensions
	}

	public void createChordalRingNetwork(int numNodes_, String algorithm_, String whichCase, String chords) {
		// Read chord lengths into a vector
		chordLengths = new Vector();
		chordLengths.add(new Integer(1)); // outer ring always present
		StringTokenizer st = new StringTokenizer(chords,",");
		while (st.hasMoreTokens())
			chordLengths.add(new Integer(st.nextToken()));
		// Error checking --> chord lengths must be between 1 and the number of nodes, separated by commas.
		int testValue;
		for (int i = 0; i < chordLengths.size(); i++) {
			testValue = ((Integer)chordLengths.get(i)).intValue();
			if (testValue > numNodes_ || testValue <= 0) { // popup window with error message
				JOptionPane.showMessageDialog(NetViewer.getPopupLocation(),"Chord lengths must be between 1 and the number\n of nodes, separated by commas.", "Input Error", JOptionPane.WARNING_MESSAGE);
				NetViewer.chordsField.requestFocus();
				NetViewer.getDrawingPanel().repaint();
				return;
			}
		}
		// proceed; chord lengths are correct
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		networkType = "Chordal Ring";
		numNodes = numNodes_;
		algorithm = algorithm_;

		// remove any "duplicates". ex: n=6 chords = 2,4 both give the same result. We only need one - either 2 or 4.
		mirrors = 0;
		if (even(numNodes) && chordLengths.contains(new Integer(numNodes/2)))
			mirrors = numNodes/2;
		for (int i = 0; i < chordLengths.size()-1; i++) {
			for (int j = i+1; j < chordLengths.size(); j++) {
				int a = ((Integer)chordLengths.get(i)).intValue();
				int b = ((Integer)chordLengths.get(j)).intValue();
				if (a == numNodes-b || a == b) {
					chordLengths.remove(j);
					j--; // so it continues without missing any (everything has shifted left)
				}
			}
		}

		createNodes(whichCase);

		// Create links
		Node targetNode, anchorNode;
		Link newLink;
		int size = chordLengths.size();
		int index;
		for (int i=0; i<numNodes; i++) {
			anchorNode = (Node)nodesVector.get(i);
			for (int j=0; j<size; j++) {
				if (mirrors > 0 && (i >= numNodes/2) && ((Integer)chordLengths.get(j)).intValue() == numNodes/2)
				{
					// do nothing --> when chord length is half the num nodes, the 2nd 1/2 of the ring is already in place when the first 1/2 is complete
				}
				else {
					index = i+((Integer)chordLengths.get(j)).intValue();
					if (index >= numNodes)
						index = index-numNodes;
					targetNode = (Node)nodesVector.get(index);
					newLink = new Link(anchorNode, targetNode);
					anchorNode.addLink(newLink);
					targetNode.addLink(newLink);
					linksVector.add(newLink);
				}
			}
		}
		numLinks = linksVector.size(); // also given by numNodes*chordLengths.size()-mirrors;
		isNewNetwork = true; // forces resize upon next drawing, to recalculate network dimensions
	}

	public void createGridNetwork(int rows_, int cols_, String algorithm_, String whichCase) {
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		networkType = "Grid";
		algorithm = algorithm_;
		rows = rows_;
		cols = cols_;
		numNodes = rows*cols;
		numLinks = 2*numNodes - rows - cols;

		createNodes(whichCase);

		// construct links and save into nodes
		Link newLink;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (i < rows-1) // create down link
				{
					newLink = new Link(getNode(i,j), getNode(i+1,j));
					linksVector.add(newLink);
					getNode(i,j).addLink(newLink);
					getNode(i+1,j).addLink(newLink);
        }
      	if (j < cols-1) // create across link
      	{
					newLink = new Link(getNode(i,j), getNode(i,j+1));
					linksVector.add(newLink);
					getNode(i,j).addLink(newLink);
					getNode(i,j+1).addLink(newLink);
       	}
    	} //for
   	} //for

		isNewNetwork = true; // forces resize upon next drawing, to recalculate network dimensions
	}

	public void createTorusNetwork(int rows_, int cols_, String algorithm_, String whichCase) {
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		networkType = "Torus";
		algorithm = algorithm_;
		rows = rows_;
		cols = cols_;
		numNodes = rows*cols;
		numLinks = 2*numNodes;

		createNodes(whichCase);

		// construct links and save into nodes
		TorusLink newLink;
		int m,n;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
					m = i; n = j;
					if (i == rows-1) m = -1;
					newLink = new TorusLink(getNode(i,j), getNode(m+1,n));
					if (m == -1)
						newLink.setIsWrapAround(true);
					else
						newLink.setIsWrapAround(false);
					linksVector.add(newLink);
					getNode(i,j).addLink(newLink);
					getNode(m+1,n).addLink(newLink);
					m = i;
					if (j == cols-1) n = -1;
					newLink = new TorusLink(getNode(i,j), getNode(m,n+1));
					if (n == -1)
						newLink.setIsWrapAround(true);
					else
						newLink.setIsWrapAround(false);
					linksVector.add(newLink);
					getNode(i,j).addLink(newLink);
					getNode(m,n+1).addLink(newLink);
    	} //for
   	} //for

		isNewNetwork = true; // forces resize upon next drawing, to recalculate network dimensions
	}

	/* Create a tree with the given number of nodes.
	 * Randomly assign children to nodes until n is reached.
	 * Only used when the user selects automatic tree creation.
	 * Otherwise, the tree is created manually by clicking the mouse.
	 */
	public void createTree(int numNodes_) {
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		TreePanel drawingPanel = (TreePanel)NetViewer.getNetworkPanel().getDrawingArea();
		drawingPanel.drawRoot(); // create the root
		int random; TreeNode node;
		while (numNodes < numNodes_) { // add nodes until n is reached
			// pick an existing node
			random = (int)Math.round((numNodes-1)*Math.random()); // generate a random index between 0 and 5
			node = (TreeNode)nodesVector.get(random);
			// add a child
			drawingPanel.addChild(node);
		}
		drawingPanel.resizePanel();
		drawingPanel.centreTree();
		drawingPanel.repaint();
	}

	/* Create an arbitrary network with the given number of nodes.
	 * Assign random locations to nodes until n is reached. Do not
	 * let nodes overlap.
	 * Only used when the user selects automatic network creation.
	 * Otherwise, the network is created manually by clicking the mouse.
	 */
	public void createArbitraryNetwork(int numNodes_) {
		clear(); // data structures that store links, nodes, ids
		NetViewer.getNetworkPanel().getLastDirtyCanvas().setIsBlank(true);
		Message.resetTotalMessages(); // back to 0
		ArbitraryPanel drawingPanel = (ArbitraryPanel)NetViewer.getNetworkPanel().getDrawingArea();
		drawingPanel.setPreferredSize(NetViewer.getNetworkPanel().arbScrollPane.getViewportBorderBounds().getSize()); // set the size to the size of the current view
		int x,y,z,i,m; Node newNode, newNeighbour; Link link;
		while (numNodes < numNodes_) { // add nodes until n is reached
			newNode = newNode(); // a distinct id will be generated for it
			x = (int)Math.round((drawingPanel.getPreferredSize().width-Node.DIAMETER)*Math.random()); // generate a random index between 0 and the width of the drawing panel
			y = (int)Math.round((drawingPanel.getPreferredSize().height-Node.DIAMETER)*Math.random()); // generate a random index between 0 and the height of the drawing panel
			newNode.setCoords(x,y);
			if (drawingPanel.tooCloseToOtherNodes(newNode))
				removeNode(newNode);
			else { // add some links to other existing nodes
				if (numNodes == 1)
					z = 0;
				else
					z = (int)Math.round((numNodes-2)*Math.random()+1); // # of neighbours this node will have, from 1 to n-1
				Vector neighbours = new Vector(z);
				i = 0;
				while (i < z) { // haven't reached full # neighbours yet
					m = (int)Math.round((numNodes-1)*Math.random()); // generate a random index between 0 and n to get the next neighbour out of the nodes vector
					newNeighbour = (Node)nodesVector.get(m);
					if (!neighbours.contains(newNeighbour) && newNeighbour != newNode) {
						newLink(newNode, newNeighbour);
						neighbours.add(newNeighbour);
						i++;
					}
				}
			}
		}
		drawingPanel.setIsBlank(false);
		drawingPanel.resizePanel(); // Resize panel to the size of the network with the new node. (So scrollbars appear when necessary)
		drawingPanel.repaint();
	}

	public void initializeNetwork() {
		clear(); // clear data structures that store links, nodes, ids
		DrawingPanel lastDirtyCanvas = NetViewer.getNetworkPanel().getLastDirtyCanvas();
		lastDirtyCanvas.setIsBlank(true);
		if (lastDirtyCanvas instanceof TreePanel || lastDirtyCanvas instanceof ArbitraryPanel) {
			lastDirtyCanvas.setPreferredSize(new Dimension(0,0)); // put back to small size so scrollbars disappear
			lastDirtyCanvas.revalidate();
		}
	  Message.resetTotalMessages(); // back to 0
	  networkType = NetViewer.getNetworkType(); //(String)((JComboBox)NetViewer.getToolBar().getComponentAtIndex(0)).getSelectedItem();
	  algorithm   = NetViewer.getAlgorithm(); //(String)((JComboBox)((JPanel)NetViewer.getToolBar().getComponentAtIndex(2)).getComponent(0)).getSelectedItem();
	  isNewNetwork = true;
	}

	/* Create a new link at the given location.
	 */
	public Link newLink(Point startCoords, Point endCoords) {
		Link newLink = new Link();
		newLink.setStartCoords(startCoords.x, startCoords.y);
		newLink.setEndCoords(endCoords.x, endCoords.y);
		linksVector.add(newLink);
		numLinks++;
		return newLink;
	}

	/* Create a new link between n1 and n2.
	 */
	public Link newLink(Node n1, Node n2) {
		Link newLink = new Link(n1, n2);
		newLink.setStartCoords(n1.getCentre().x, n1.getCentre().y);
		newLink.setEndCoords(n2.getCentre().x, n2.getCentre().y);
		n1.addLink(newLink);
		n2.addLink(newLink);
		linksVector.add(newLink);
		numLinks++;
		return newLink;
	}

	public void removeLink(Link link) {
		linksVector.remove(link);
		link = null;
		numLinks--;
	}

	public void removeNode(Node node) {
		nodesVector.remove(node);
		Node.ids().remove(new Integer(node.id));
		node = null;
		numNodes--;
	}

  /* Start nodes executing
	 */
	public void startAlgorithm() {
		if (numNodes <= 0) return; // empty network; cannot run algorithm
	  Message.resetTotalMessages(); // back to 0

		// Double check message queues are clear (should already be done)
		Link link;
		Enumeration links = linksVector.elements();
		while (links.hasMoreElements())
		{
			link = (Link)links.nextElement();
			link.getActiveMessages().clear();
			link.setSyncMessage(Link.LEFT, null);
			link.setSyncMessage(Link.RIGHT, null);
			link.getQueue().clear();
		}
  	NetViewer.startAnimation(); // starts the timer, so network starts redrawing
		Enumeration nodes = nodesVector.elements();
		if (NetViewer.isSynchronous()) { // do not start nodes if synchronous; only start the syncTimer
			syncTimer.setDelay((int)Link.getUnitOfTime());
			syncTimer.setInitialDelay(0);
			tick = 0;
			syncTimer.start();
		}
		else {
			while (nodes.hasMoreElements())
				((Node)nodes.nextElement()).start();
		}
		previousNetworkType = networkType;
		previousAlgorithm = algorithm;
	  previousNumNodes = numNodes;
	  previousNumLinks = numLinks;
	  previousNumRows = rows;
	  previousNumCols = cols;
	  previousNodesVector = nodesVector;
	  previousLinksVector = linksVector;
	  previousIdsUsed = Node.ids();
	  isRunning = true;
		NetViewer.out.println("Algorithm started.");
  }

	public Integer getNewID() {
		Integer newID;
		if (numNodes == 0) // generate a random first id between 0 and 5 (so that it's not always 0)
			newID = new Integer((int)Math.round(5*Math.random()));
		else {
			do
				newID = new Integer((int)Math.round((2*numNodes)*Math.random()));
			while (Node.ids().contains(newID)); // id must be distinct
		}
		Node.ids().add(newID);
		return newID;
	}

	// Get a node
  public Node getNode(int index) {
		return (Node)nodesVector.get(index);
  }

	// Get a link
  public Link getLink(int index) {
  	return (Link)linksVector.get(index);
  }

	// Get grid node (i,j)
  public Node getNode(int i, int j) {
    return (Node)nodesVector.get(i*cols + j);
  }

	// Get grid link (i,j,k)
  public Link getLink(int i, int j, int k) {
		if (i == rows-1 && j == cols-1) // no links on bottom right corner
			return null;
		if (i == rows-1 && k == 0) // no down links on last row
			return null;
		if (i == rows-1 && k == 1) // across link on last row
			return (Link)linksVector.get(numLinks - cols + j + 1);
		return (Link)linksVector.get(2*i*cols + 2*j - i+k+1);
  }

	public int getNumNodes() {
    return numNodes;
  }

	public int getNumLinks() {
    return numLinks;
  }

	public Vector getLinks() {
		return linksVector;
	}

	public Vector getNodes() {
		return nodesVector;
	}

  public String getAlgorithm() {
    return algorithm;
  }

  public String getNetworkType() {
    return networkType;
  }

	public void setIsRunning(boolean tf) {
		isRunning = tf;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public int getNumRows() {
		return rows;
	}

	public int getNumColumns() {
		return cols;
	}

	public void clear() {
		nodesVector.clear();
		linksVector.clear();
		Node.ids().clear();
		numNodes = 0;
		numLinks = 0;
	}

	/* changeCase(): Rearrange node ids into the correct order for best/worst/average case.
	 * Occurs when the user selets best/worst/average case from a drop down menu.
	 */
	public void changeCase(String whichCase) {
		try {
			// arrange node ids into the correct order for best/worst/average case
			boolean changed = ((Boolean)Class.forName(getNodeType()).getDeclaredMethod(whichCase, new Class[] {}).invoke(new Object(), new Object[] {})).booleanValue();
			if (!changed)
				return;
			// save new ids into nodes
			Integer id;
			Enumeration idsList = Node.ids().elements();
			Enumeration nodes = nodesVector.elements();
			while (nodes.hasMoreElements()) {
				id = (Integer)idsList.nextElement();
				((Node)nodes.nextElement()).setID(id.intValue());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/* changeAlgorithm(): Switch all nodes to a different type.
	 *  Occurs when the user selets a different algorithm in the drop down menu.
	 *  Transparent to the user.
	 */
	public void changeAlgorithm(String newAlgorithm) {
		NetViewer.out.println("Changed algorithm to "+newAlgorithm+" from "+algorithm);
		algorithm = newAlgorithm;
		Node originalNode, newNode;
		Link link;
		Vector links;
		Vector nodes = (Vector)nodesVector.clone();
		nodesVector.clear();
		for (int i = 0; i < nodes.size(); i++) {
			originalNode = (Node)nodes.get(i);
			newNode = newNode(new Integer(originalNode.id));
			newNode.setCoords(originalNode.coords.x, originalNode.coords.y);
			newNode.setWakeUpDelay(originalNode.wakeUpDelay);
		  newNode.setWakeUpPosition(originalNode.getWakeUpPosition());
			// hook up old links to new nodes
			links = originalNode.getLinks();
			for (int j = 0; j < links.size(); j++) {
				link = (Link)links.get(j);
				if (originalNode == link.getNode(Node.LEFT))
					link.setNode(Node.LEFT, newNode);
				else
					link.setNode(Node.RIGHT, newNode);
				newNode.addLink(link);
			}
			nodesVector.add(newNode);
			originalNode = null;
		}
		isNewNetwork = true; // so that when painting, resize will be called to make sure all size calculations are correct (important when switching to FranklinStages, because queue coords must be calculated (cannot be copied from previous network)
		NetViewer.getNetworkPanel().getDrawingArea().setIsDirty(false);
		resetLinks();
	}

	/* Reset all nodes and links ready for re-running.
   * New nodes must be constructed because the old ones are threads that have died and CANNOT be restarted.
   * Exception: Nodes in a synchronous network are never started, so they can simply be put back to sleep.
	 */	
	public void resetNodesAndLinks() {
		resetNodes();
		resetLinks();
	}
	
	private void resetNodes() {
	  if (numNodes == 0) return; // nothing to reset
	  Message.resetTotalMessages(); // back to 0
		Node originalNode, newNode;
		Link link;
		Vector links;
		Vector nodes = (Vector)nodesVector.clone();
		nodesVector.clear();
		for (int i = 0; i < nodes.size(); i++) {
		  originalNode = (Node)nodes.get(i);
			newNode = newNode(new Integer(originalNode.id));
		  newNode.setCoords(originalNode.coords.x, originalNode.coords.y);
		  newNode.setWakeUpDelay(originalNode.wakeUpDelay);
		  newNode.setWakeUpPosition(originalNode.wakeUpPosition); // in case instant wake up is selected now, and we later (before running) choose synchronous (wake up position would be 0 without this)
		  nodesVector.add(newNode);
		  if (algorithm.equals("Franklin Stages")) { // set queue coords
				((RingNodeFranklinStages)newNode).setQueueCoordsLeft(((RingNodeFranklinStages)originalNode).getQueueCoordsLeft());
				((RingNodeFranklinStages)newNode).setQueueCoordsRight(((RingNodeFranklinStages)originalNode).getQueueCoordsRight());
			}
			if (networkType.equals("Tree"))
			{
				if (!((TreeNode)originalNode).isRoot()) {
					link = ((TreeNode)originalNode).getBackwardsLink();
					if (originalNode == link.getNode(Node.LEFT))
					  link.setNode(Node.LEFT, newNode);
					else
					  link.setNode(Node.RIGHT, newNode);
					((TreeNode)newNode).setBackwardsLink(link);
				}
			}
		  links = originalNode.getLinks();
		  for (int j = 0; j < links.size(); j++) { // save old links into new nodes and vice versa
				link = (Link)links.get(j);
				if (originalNode == link.getNode(Node.LEFT))
				  link.setNode(Node.LEFT, newNode);
				else
				  link.setNode(Node.RIGHT, newNode);
				newNode.addLink(link);
			} // for
		} // for
	  NetViewer.out.println("Reset");
	}
	
	private void resetLinks() {
		Enumeration allLinks = linksVector.elements();
		while (allLinks.hasMoreElements()) {
			((Link)allLinks.nextElement()).setColor(Link.LinkDefaultColor);
		}
	}

	/* Wait for active threads to finish (nodes and messages).
	 * Used when Animation is stopped (aborted), so should not have too long to wait.
	 */
  public void waitForThreadsToFinish() throws InterruptedException {
		// wait for nodes to terminate (ones that haven't woken up)
		if (!NetViewer.isSynchronous())
		{
			for (int i=0; i<numNodes; i++)
				((Node)nodesVector.get(i)).join();
		}
		// wait for active messages to terminate
		Link link; Message m; Enumeration messages;
		for (int i=0; i<numLinks; i++)
		{
			link = getLink(i);
			if (NetViewer.isSynchronous()) {
				link.getActiveMessages().clear();
				messages = link.getSyncMessages().elements();
				link.setSyncMessage(Link.LEFT, null);
				link.setSyncMessage(Link.RIGHT, null);
			}
			else
				messages = link.getActiveMessages().elements();
			while (messages.hasMoreElements()) {
				m = (Message)messages.nextElement();
				m.join(); // wait for thread to end
			}
		}
	}

	/* Create a new node of the given type, with the given id.
	 * Construction is dynamic. Code can handle any type of node.
	 */
	private Node newNode(Integer id_) {
		Object[] id = new Object[] {id_};
		String nodeType = getNodeType(); // based on network type and algorithm
		try {
			return (Node)Class.forName(nodeType).getDeclaredConstructor(integerClass).newInstance(id);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/* Helper function. Create a new node of the given type. Id must be generated.
	 */
	public Node newNode() {
		Node newNode = newNode(getNewID()); // generate random id
		nodesVector.add(newNode);
		numNodes++;
		return newNode;
	}

	/* Helper function. Create a new node with the given id.
	 */
	public Node newNode(int id) {
		Integer newId = new Integer(id);
		Node newNode = newNode(newId);
		nodesVector.add(newNode);
		Node.ids().add(newId);
		numNodes++;
		return newNode;
	}

	/* Check if graph is connected
	 * For arbitrary network
	 */
  private boolean graphIsConnected() {
		// to implement
		return true;
  }

	/* Determine the type of node corresponding to
	 * the current network type and algorithm.
	 */
  public String getNodeType() {
		return noSpaces(networkType)+"Node"+noSpaces(algorithm);
	}

	public boolean isNewNetwork() {
		return isNewNetwork;
	}

	public void setIsNewNetwork(boolean tf) {
		isNewNetwork = tf;
	}

	public Vector getChordLengths() {
		return chordLengths;
	}

	public boolean mirrors() {
		return (mirrors > 0);
	}

	/* Create nodes for a network.
	 * Based on # of nodes, algorithm, networkType, and case (all previously set).
	 */
	private void createNodes(String whichCase) {
		int top = numNodes;
		numNodes = 0; // will increment as nodes are created
		for (int i=0; i < top; i++) {
			newNode(); // id will be generated
		}
		if (networkType.equals("Ring") && !whichCase.equals("average"))
			changeCase(whichCase);
	}

	/* Make wake-up delay 1 ms for all nodes (almost instant).
	 * This can be useful when studying an algorithm's behaviour.
	 */
	public void instantWakeUp() {
		Enumeration nodes = nodesVector.elements();
		Node node;
		while (nodes.hasMoreElements()) {
			node = (Node)nodes.nextElement();
			node.setWakeUpDelay(1);
			node.setWakeUpPosition(1); // in case system is synchronous
		}
	}

	/* Make wake-up delay random for all nodes (almost instant).
	 * Needed when the instant wake-up option is turned off.
	 */
	public void staggerWakeUp() {
		Enumeration nodes = nodesVector.elements();
		while (nodes.hasMoreElements())
			((Node)nodes.nextElement()).setWakeUpDelay(); // get a new random wake-up delay
	}

	public void setAllLinksSameSpeed() {
		Enumeration links = linksVector.elements();
		while (links.hasMoreElements())
			((Link)links.nextElement()).setSpeed(Link.getUnitOfTime()); // set all link speeds equal to the unit of time
	}

	/* Set a random wakeup order for nodes in a synchronous network.
	 */
	public void setWakeUpOrder() {
		Enumeration nodes = nodesVector.elements();
		while (nodes.hasMoreElements())
			((Node)nodes.nextElement()).setWakeUpPosition(); // random
	}

	/* Set random speeds for a non-synchronous network.
	 */
	public void setRandomSpeeds() {
		Enumeration links = linksVector.elements();
		while (links.hasMoreElements())
			((Link)links.nextElement()).setNewSpeed(); // get a new link speed
	}

	private boolean allNodesFinished() {
		Enumeration nodes = nodesVector.elements();
		Node node;
		while (nodes.hasMoreElements()) {
			node = (Node)nodes.nextElement();
			if (!node.isFinished()) {
				return false;
			}
		}
		return true;
	}

  /* Called every time the timer ticks when the network is synchronous.
   * Represents one unit of time in a synchronous network.
   */
  public void actionPerformed(ActionEvent e) {
		lastTick = System.currentTimeMillis();
		tick++;
		NetViewer.updateClockTick(tick); // display on results panel
		if (NetViewer.beepSync)
			java.awt.Toolkit.getDefaultToolkit().beep();
		NetViewer.out.println("TICK "+tick);

		if (allNodesFinished())
		{
			try {
				waitForThreadsToFinish(); // some messages may still be travelling
			} catch (InterruptedException ex) {}
			syncTimer.stop();
			return;
		}

		// Get all messages finishing on this clock tick
		Vector mgsToDeliver = new Vector();
		Enumeration links = linksVector.elements();
		while (links.hasMoreElements())
			mgsToDeliver.addAll(((Link)links.nextElement()).getActiveMessages());

		// Deliver finishing messages to their destination nodes and let the calculations take place to produce new messages.
		// Note: Cannot be done in the above loop; must be done after getting all messages. Otherwise, you may deliver a message that will spawn a new message, which you may deliver straight away on the next iteration!
		Message m;  Link link;
		Enumeration messages = mgsToDeliver.elements();
		while (messages.hasMoreElements())
		{
			m = (Message)messages.nextElement();
			link = m.getCallingLink();
			if(m.getStage() != -1)
				link.send(m.getMessage(), m.getDirection(), m.getStage(), m.getValue());
			if (m.getNumNodesSeenByMessage() == -1)
				link.send(m.getMessage(), m.getDirection());
			else // ring algorithm "All The Way"
				link.send(m.getMessage(), m.getDirection(), m.getNumNodesSeenByMessage());
			link.getActiveMessages().remove(m);
		}

		// Wake up any nodes that should wake up on this clock tick and have not already been interrupted.
		Enumeration nodes = nodesVector.elements();
		Node node;
		while (nodes.hasMoreElements()) {
			node = (Node)nodes.nextElement();
			if (node.state == Node.ASLEEP && node.getWakeUpPosition() == tick) {
				node.initialize();
			}
		}

		// Make two new messages for each link (one for each direction) containing the text of the messages on the link separated by commas.
		String stringMsg;
		links = linksVector.elements();
		while (links.hasMoreElements())
		{
			link = (Link)links.nextElement();
			for (int i=0; i<2; i++) { // once for right, once for left
				messages = link.getActiveMessages(i).elements(); // get all msgs travelling left/right
				stringMsg = "";
				while (messages.hasMoreElements())
				{
					m = (Message)messages.nextElement();
					if (!stringMsg.equals(""))
							 stringMsg += ", ";
					stringMsg += m.toString(); // add each message's text to the string to make one long message
				}
				if (!stringMsg.equals(""))
				{
					m = new Message(stringMsg, i, link, true); // create a single message containing the text of all messages travelling in this direction on this link
					link.setSyncMessage(i, m);
					m.start();
				}
				else {
					link.setSyncMessage(i, null);
				}
			}
		}
  }

	/*-------- UTILITY FUNCTIONS --------*/

	/* Answer true if n is even, else false.
	 */
	static boolean even(int n) {
		return (Math.IEEEremainder((double)n, 2) == 0);
	}

	/* Remove any spaces in a string.
	 */
  public String noSpaces(String s) {
		StringBuffer stringNoSpaces = new StringBuffer();
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens())
			stringNoSpaces.append(st.nextToken());
		return stringNoSpaces.toString();
	}

} // class

//////////// Reference ////////////

	/* make last link a bottleneck so the last node doesn't
	   interrupt the first and wake it up too soon
	*/
//	((Link)linksVector.get(numNodes-1)).setSpeed(1000*numNodes);
