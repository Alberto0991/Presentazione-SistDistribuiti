/*
 * NetViewer
 * Class: Link
 */

import java.util.Vector;
import java.util.Enumeration;
import java.awt.Point;
import java.awt.Color;

class Link {

	static final int RIGHT = 0, LEFT = 1;
	static private long unitOfTime; // for synchronous networks, unit of time is the same for all links

	private long speed;
	private static int speedSeed;
	private Node[] nodesArray;
	private String type; // type of network the link is part of
	private Vector activeMessages; // the messages currently travelling along the link (in both directions)
	protected Point startCoords, endCoords; // drawing coordinates
	private Vector queue; // queue to hold messages that arrive out of order and must wait because it's FIFO
	private Object control; // to control flow through the alertMsgFinished() function for FIFO networks so that messages arrive in the same order as they were sent
	private Message[] syncMessage;

	//public static Color LINK_COLOUR = Color.black; // for drawing
	public static Color LinkDefaultColor = Color.black;
	private Color link_color;
	public void setColor (Color color){
		link_color = color;
	}
	public Color getColor (){
		return link_color;
	}
	
	Link(Node n1, Node n2) {
		activeMessages = new Vector();
		nodesArray = new Node[2];
		nodesArray[LEFT] = n1; // Set the node at the left end of the link
		nodesArray[RIGHT] = n2; // Set the node at the right end of the link
		if (NetViewer.isSynchronous())
			speed = unitOfTime; // all links same speed
		else
			speed = getNewSpeed();
		queue = new Vector();	control = new Object();
		syncMessage = new Message[2];
		
		setColor(LinkDefaultColor);
	}

	// Constructor used in tree and arbitrary networks.
	// Manual drawing means a link may be created before its final endpoints are known
	// (user draws the link with the mouse)
	Link() {
		activeMessages = new Vector();
		nodesArray = new Node[2];
		if (NetViewer.isSynchronous())
			speed = unitOfTime; // all links same speed
		else
			speed = getNewSpeed();
		queue = new Vector();	control = new Object();
		syncMessage = new Message[2];
	}

	/* Receive a message.
	 *   Spawn a thread that waits a certain amount of time (to simulate the message
   * travelling along the link) and then sends a notification when it is finished.
	 *   dir is the direction the message came from
	 */
	public synchronized void receive(String msg, int dir) {
		type = "basic ring";
		Message m = new Message(msg, dir, this);
		activeMessages.add(m); // add before starting the thread, in case the thread dies really fast and tries to remove itself from the queue before being added.
		if (!NetViewer.isSynchronous())
			m.start();
	}

	/* Receive a message with a counter (for ring algorithm "all the way").
	 *   Spawn a thread that waits a certain amount of time (to simulate the message
   * travelling along the link) and then  sends a notification when it is finished.
	 *   dir is the direction the message came from
	 * The method is synchronized to regulate access to the active messages vector
	 *  - To avoid two message threads being created and added to the queue in one order
   *    but started in another order. In other words, it makes the adding and starting
	 *    of message threads happen atomically (as one statement).
	 */
	public synchronized void receive(String msg, int dir, int numNodesSeenByMessage) {
		type = "basic ring";
		Message m = new Message(msg, dir, this, numNodesSeenByMessage);
		activeMessages.add(m);
		if (!NetViewer.isSynchronous())
			m.start();
	}

	/* Receive a message from a particular node (used in the grid).
	 *   Spawn a thread that waits a certain amount of time (to simulate the message
   * travelling along the link) and then sends a notification when it is finished.
	 *   node is the node the message came from (use it to find the direction to send in)
	 */
	public synchronized void receive(String msg, Node node) {
		type = "general";
		int dir;
		if (nodesArray[LEFT] == node)
			dir = RIGHT;
		else	dir = LEFT; // direction the message must travel
		Message m = new Message(msg, dir, this);
		activeMessages.add(m);
		if (!NetViewer.isSynchronous())
			m.start();
	}
	
	public synchronized void receive(String msg, Node node, int data1, int data2) {
		type = "general";
		int dir;
		if (nodesArray[LEFT] == node)
			dir = RIGHT;
		else	dir = LEFT; // direction the message must travel
		Message m = new Message(msg, dir, this, data1, data2);
		activeMessages.add(m);
		if (!NetViewer.isSynchronous())
			m.start();
	}

	/* Send a message.
	 * - Deliver the message to the node at the opposite end of the link.
	 */
	protected void send(String msg, int dir) {
		Node node = nodesArray[dir];
		if (node.state == Node.ASLEEP) // Wake up the node if it is ASLEEP
		{
			NetViewer.out.println("Node "+node.id+" got interrupted.");
			node.interrupt();
		}
		if (type.equals("basic ring"))
			node.receive(msg, Math.abs(dir-1));
		else
			node.receive(msg, this);
	}
	
	/* Send a message
	 * - for complete network
	 */
	protected void send(String msg, int dir, int stage, int value) {
		Node node = nodesArray[dir];
		if (node.state == Node.ASLEEP) // Wake up the node if it is ASLEEP
		{
			NetViewer.out.println("Node "+node.id+" got interrupted.");
			node.interrupt();
		}
		node.receive(msg, this, stage, value);
	}

	/* Send a message to a node for the All the Way ring algorithm.
	 * - Deliver the message to the node at the opposite end of the link.
	 * - Include counter (for ring algorithm "all the way")
	 */
	protected void send(String msg, int dir, int numNodesSeenByMessage) {
		if (nodesArray[dir].state==Node.ASLEEP) { // Wake up the node if it is ASLEEP
			NetViewer.out.println("Node "+nodesArray[dir].id+" got interrupted.");
			nodesArray[dir].interrupt();
		}
		nodesArray[dir].receive(msg, Math.abs(dir-1), numNodesSeenByMessage);
	}

	/* Receive an alert indicating a message has finished travelling across the link
	 * Deliver the message to the opposite end of the link.
	 */
	public void alertMsgFinished(Message m) {
		if (NetViewer.isSynchronous())
			return; // central clock in NetworkManager handles it
		if (NetViewer.isFIFO()) { // send msg if it is the next in line to go; otherwise queue msg
			synchronized (control) {
				String id = m.toString();
				if (arrivedTooSoon(m)) {
					queue.add(m);
				}
				else {
					while (m != null) { // while there are still messages to send
						if(m.getStage() != -1)
							send(m.getMessage(), m.getDirection(), m.getStage(), m.getValue());
						else if (m.getNumNodesSeenByMessage() == -1)
							send(m.getMessage(), m.getDirection());
						else // ring algorithm "All The Way"
							send(m.getMessage(), m.getDirection(), m.getNumNodesSeenByMessage());
						activeMessages.remove(m);
						m = checkQueue(m.getDirection()); // get the next msg in the queue that can go now
					}
				}
			}
		}
		else { // not FIFO; send messages in the order they arrive
			if(m.getStage() != -1)
				send(m.getMessage(), m.getDirection(), m.getStage(), m.getValue());
			else if (m.getNumNodesSeenByMessage() == -1)
				send(m.getMessage(), m.getDirection());
			else // ring algorithm "All The Way"
				send(m.getMessage(), m.getDirection(), m.getNumNodesSeenByMessage());
			activeMessages.remove(m);
		}

	}

	/* Check if the given message arrived too
	 * soon, ie. out of order. If there is an active message with the same
	 * direction nearer to the beginning of the queue, return true.
	 */
	private boolean arrivedTooSoon(Message m) {
		Enumeration msgs = activeMessages.elements();
		Message msg;
		while (msgs.hasMoreElements()) {
			msg = (Message)msgs.nextElement();
			if (msg.getDirection() == m.getDirection())
				if (activeMessages.indexOf(m) > activeMessages.indexOf(msg))
					return true;
		}
		return false;
	}

	/* Check the queue for messages that arrived too
	 * early but can now be sent. Checks if queue contains the next
	 * in line in the activeMessages vector.
	 */
	private Message checkQueue(int direction) {
		if (queue.isEmpty()) return null;
		Enumeration msgs = activeMessages.elements();
		Message msg = null;
		while (msgs.hasMoreElements()) {
			msg = (Message)msgs.nextElement();
			if (msg.getDirection() == direction) // get the first active message with the correct direction
				break;
		}
		if (queue.remove(msg)) {
			return msg;
		}
		else return null;
	}

	// Remove a message that has been forced to terminate from the active messge queue.
	// Used from Message when simulation has been aborted while the message is still alive.
	public void removeMessage(Message m) {
		activeMessages.remove(m);
	}

	public Vector getActiveMessages() {
		return activeMessages;
	}

	public Vector getActiveMessages(int dir) {
		Vector answer = new Vector();
		Enumeration	messages = activeMessages.elements();
		Message msg;
		while (messages.hasMoreElements())
		{
			msg = (Message)messages.nextElement();
			if (msg.getDirection() == dir)
				answer.add(msg);
		}
		return answer;
	}

	public void setSyncMessage(int dir, Message m) {
		syncMessage[dir] = m;
	}

	/* Get the two messages for this link in a synchronous network.
	 * One message for each direction, containing all messages on the link
	 * separated by commas for better viewing.
	 */
	public Vector getSyncMessages() {
		Vector syncMsgs = new Vector();
		if (syncMessage[LEFT] != null) syncMsgs.add(syncMessage[LEFT]);
		if (syncMessage[RIGHT] != null) syncMsgs.add(syncMessage[RIGHT]);
		return syncMsgs;
	}

	public long getSpeed() {
		return speed;
	}

	public void setSpeed(long time) {
		speed = time;
	}

	/* Set the node at one end of the link (LEFT or RIGHT).
	 * Used during network construction.
	 */
	public void setNode(int whichNode, Node node) {
		nodesArray[whichNode] = node;
	}

	/* Get the node at one end of the link (LEFT or RIGHT).
	 * Used when re-constructing all nodes to re-run an algorithm.
	 */
	public Node getNode(int whichNode) {
		return nodesArray[whichNode];
	}

	private static void getNewSpeedSeed() {
		speedSeed = ((int)Math.round(Math.random()*10000)+2000)/2; // a message will take between 2 and 12 seconds to travel along the link
	}

	/* Generate a new speed.
	 * Used when the speed is adjusted using the speed slider.
	 * Can be used to set the speed of messages as well.
	 */
	public static long getNewSpeed() {
		getNewSpeedSeed();
		int sliderValue = NetViewer.getSpeed();
    if (sliderValue < 90) {
      return (long)((double)speedSeed / Math.sin(Math.toRadians(sliderValue)));
    }
    else {
      return (long)((double)speedSeed * Math.sin(Math.toRadians(sliderValue)));
    }
	}

	/* Set a new speed for the link.
	 */
	public void setNewSpeed() {
		speed = getNewSpeed();
	}

	/* Set a new unit of time for all link in a synchronous network.
	 */
	public static void setNewUnitOfTime() {
		unitOfTime = getNewSpeed();
	}

	public String toString() {
		return "Link between "+nodesArray[LEFT]+" and "+nodesArray[RIGHT];
	}

	/*------------- METHODS USED FOR DRAWING THE LINK -------------*/

	/* Set a link's start coordinates. Used for drawing.
   */
	public void setStartCoords(double x, double y) {
		startCoords = new Point((int)x, (int)y);
	}

	/* Get a link's start coordinates. Used for drawing.
   */
	public Point getStartCoords() {
		return startCoords;
	}

	/* Set a link's end coordinates. Used for drawing.
   */
	public void setEndCoords(double x, double y) {
		endCoords = new Point((int)x, (int)y);
	}

	/* Get a link's end coordinates. Used for drawing.
   */
	public Point getEndCoords() {
		return endCoords;
	}

	/* Set both start and end coordinates.
	 */
	public void setCoords(Point p1, Point p2) {
		startCoords = new Point(p1.x, p1.y);
		endCoords 	= new Point(p2.x, p2.y);
	}

	public double length() {
		return startCoords.distance(endCoords);
	}

	public double theta() {
		double rise = endCoords.y - startCoords.y;
		double run = endCoords.x - startCoords.x;
		return Math.atan(rise/run);
	}

  /* Get the unit of time (for a synchronous network).
   * This is the time between clock ticks, or the time it takes for
   * any message to travel along a link (all links same speed).
   */
  public static long getUnitOfTime() {
    return unitOfTime;
  }

  /* Set the unit of time (for a synchronous network).
   * This is the time between clock ticks, or the time it takes for
   * any message to travel along a link (all links same speed).
   */
  public static void setUnitOfTime(long t) {
    unitOfTime = t;
  }

  public Vector getQueue() {
    return queue;
  }

}
