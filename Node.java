/*
 * NetViewer
 * Class: Node
 *
 * Abstract because you can only create a node through the subclass
 * of Node that implements the desired algorithm.
 */

import java.awt.Color;
import java.awt.Point;
import java.util.Vector;
import java.util.HashMap;
import java.util.Collections;

abstract class Node extends Thread {

	// Instance variables
	protected int id,	state, wakeUpPosition;
	protected String idString; // For sending messages (messages are strings)
	protected long wakeUpDelay; // The amount of time the node sleeps before waking up
	protected Point coords; // drawing coordinates
	protected Vector links; // all links attached to this node

	// Static variables (apply to all nodes)
	static protected Vector ids = new Vector(); // ids of all nodes

	// Constants
	static final int RIGHT = 0; // Sense of direction - use if required by the algorithm
	static final int LEFT = 1;  // Sense of direction - use if required by the algorithm
	static final int RADIUS = 10; // Radius of a node, used for drawing
	static final int DIAMETER = 20; // Diameter of a node, used for drawing

	// States (constants)
	static final int ASLEEP = 0;
	static final int AWAKE = 1;
	static final int CANDIDATE = 2;
	static final int PASSIVE = 3;
	static final int LEADER = 4;
	static final int FOLLOWER = 5;
	static final int CAPTURED = 6;

	static final int INITIATOR = 7;
	static final int IDLE = 8;
	static final int ACTIVE = 9;
	static final int DONE = 10;

	/* Add new states here if needed
		...
	*/

	// Colours, for representing a node's state visually
	static final Color COLOUR_ASLEEP = Color.yellow;
	static final Color COLOUR_AWAKE = Color.green;
	static final Color COLOUR_CANDIDATE = Color.green;
	static final Color COLOUR_PASSIVE = Color.black;
	static final Color COLOUR_FOLLOWER = Color.black; // the center of a follower will be black, like a passive node, but it whill be outlined in red to indicate it has finished
	static final Color COLOUR_LEADER = Color.red;
	static final Color COLOUR_TERMINATED = Color.red; // used to outline nodes when they have finished
	static final Color COLOUR_CAPTURED = Color.white;
	
	static final Color COLOUR_INITIATOR = Color.cyan;
	static final Color COLOUR_IDLE = Color.yellow;
	static final Color COLOUR_ACTIVE = Color.green;
	static final Color COLOUR_DONE = Color.gray;
	
	// Mapping of states to colours
	static final HashMap coloursMap = new HashMap();
	static {
		coloursMap.put(new Integer(ASLEEP), COLOUR_ASLEEP);
		coloursMap.put(new Integer(CANDIDATE), COLOUR_CANDIDATE);
		coloursMap.put(new Integer(AWAKE), COLOUR_AWAKE);
		coloursMap.put(new Integer(PASSIVE), COLOUR_PASSIVE);
		coloursMap.put(new Integer(LEADER), COLOUR_LEADER);
		coloursMap.put(new Integer(FOLLOWER), COLOUR_FOLLOWER);
		coloursMap.put(new Integer(CAPTURED), COLOUR_CAPTURED);
		
		coloursMap.put(new Integer(INITIATOR), COLOUR_INITIATOR);
		coloursMap.put(new Integer(IDLE), COLOUR_IDLE);
		coloursMap.put(new Integer(ACTIVE), COLOUR_ACTIVE);
		coloursMap.put(new Integer(DONE), COLOUR_DONE);
	}

	public static Color MESSAGE_QUEUE_COLOUR = Color.black; // for drawing
	public static Color ID_COLOUR = Color.black; // for drawing
	public static Color ID_COLOUR_ON_BLACK = Color.white; // for drawing

	// Constructor
	Node(Integer ID) {
		state = ASLEEP;
		id = ID.intValue();
		idString = ID.toString();
		links = new Vector();
		if (NetViewer.isInstantWakeUp())
		{
			setWakeUpDelay(1); // instant
			setWakeUpPosition(1); // instant
		}
		else // not instant wake up
		{
			setWakeUpDelay(); // random
			setWakeUpPosition(); // random
		}
		NetViewer.out.println("New node constructed with id "+id);
	}

	public synchronized void run() {
		try {
			long timeLeft = wakeUpDelay;
			while (timeLeft > 0) {
				wait(100);
				timeLeft -= 100;
				if (NetViewer.isAborted()) {
					timeLeft = 0;
					break;
			  }
				while (NetViewer.isPaused()) // Wait for user to resume algorithm.
					wait(100); // Avoids busy waiting, which freezes the screen
			}
			if (!NetViewer.isAborted())
				initialize();
		} catch (InterruptedException e) {
			// A sleeping node has been interrupted.
			// A message is printed from within Link when it sends a message to a sleeping node. The print order makes more sense that way.
		}
	}

	/* Add a link leading from this node.
	 * Used during network construction.
	 */
	protected void addLink(Link link) {
		links.add(link);
	}

	/* Remove a link attached to this node.
	 * Used during network construction (arbitrary and tree).
	 */
	protected void removeLink(Link link) {
		links.remove(link);
	}

	/* Set a particular link leading from this node.
	 * Used during ring network construction to specify right or left.
	 */
	public void setLink(int whichLink, Link link) {
		links.add(whichLink, link);
	}

	public Vector getLinks() {
		return links;
	}

	/* Get a link leading from this node.
	 */
	public Link getLink(int whichLink) {
		return (Link)links.get(whichLink);
	}

	/* Send a message; put it on the link in direction dir.
	 * Used in the ring.
	 */
	 protected void send(String msg, int dir) {
		((Link)links.get(dir)).receive(msg, dir);
	}

	/* Send a message; put it on the given link.
	 * Used in networks other than the ring.
	 */
	protected void send(String msg, Link link) {
		link.receive(msg, this);
	}

	/* The receive() and initialize() functions are overridden in
	 * all subclasses. They exist at this level so that we can simply
	 * call receive() on any node object of any subclass without having
	 * to cast to the exact node type.
	 */
	protected synchronized void receive(String msg, int dir) {} // to be overridden
	protected synchronized void receive(String msg, Link link) {} // to be overridden
	protected synchronized void receive(String msg, int dir, int numNodesSeenByMessage) {} // to be overridden
	protected synchronized void receive(String msg, Link link, int data1, int data2) {} // to be overridden
	protected void initialize() {} // overriden for each algorithm

	/* become() changes the state
	 *   Note: (Could have been called setState(), but "become" conforms
	 *   better to the terminology used in distributed network pseudocode.
	 */
	public void become(int state) {
		this.state = state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public void setID(int ID) {
		id = ID;
		idString = Integer.toString(ID);
	}

	public void setID(String ID) {
		idString = ID;
		id = Integer.parseInt(ID);
	}

	protected Point getCentre() {
		return new Point(coords.x + RADIUS, coords.y + RADIUS);
	}

  public boolean isFinished() {
    return (state == LEADER || state == FOLLOWER);
  }

  /* Set the delay before the node wakes up.
   */
  public void setWakeUpDelay(long time) {
    wakeUpDelay = time;
  }

  /* Set a new random wakup delay for the node.
   */
  public void setWakeUpDelay() {
		do {
			wakeUpDelay = Math.round(Math.random()*10000); // random sleep time before waking up and starting the algorithm
		} while (wakeUpDelay == 0);
  }

  /* Set the # of clock ticks before the node wakes up.
   * Used in synchronous networks only.
   */
  public int getWakeUpPosition() {
    return wakeUpPosition;
  }

  /* Set the # of clock ticks before the node wakes up.
   * Used in synchronous networks only. 1 is the first clock tick, at time **0**.
	 * Note: more than one node can wake up on the same clock tick.
   */
  public void setWakeUpPosition() {
		wakeUpPosition = (int)Math.round(ids.size()*Math.random()+1); // random factor from 1 to n+1
  }

  /* Set the # of clock ticks before the node wakes up to the given value.
   * Used in synchronous networks only.
   * Note: more than one node can wake up on the same clock tick
   */
  public void setWakeUpPosition(int pos) {
		wakeUpPosition = pos;
  }

  /* Set a node's coordinates. Used for drawing.
   */
  public void setCoords(double x, double y) {
    this.coords = new Point((int)x, (int)y);
  }

	/* Shuffe links.
	 * Can be used to offset any sense of direction that could be
	 * inferred from the way the network was created.
	 */
  public void shuffleLinks() {
		Collections.shuffle(links);
  }

	public String toString() {
		return idString;
	}

	/*****************************************************
	  Static functions that apply to all node instances.
		Used mainly in network creation.
	*****************************************************/

	/* Only switches the id in the ids Vector; id not stored into a node.
	 * Used when the user manually changes an id (called from NetworkPanel).
	 */
	public static void switchId(Integer oldId, Integer newId) {
		int index = ids.indexOf(oldId);
		ids.remove(oldId);
		ids.add(index, newId);
	}

	public static Vector ids() {
		return ids;
	}

}
