/*
 * NetViewer
 *
 * Network: Ring
 * Algorithm: Franklin Stages
 *
 * Description: A node keeps broadcasting its ID in both directions until
 * it sees a smaller ID. It then becomes passive and forwards the messages
 * it receives. The leader is the node that receives its own ID from both
 * directions. The leader sends a notification message the left that goes
 * around the ring to inform the other nodes that a leader has been found.
 *
 */

import java.util.Vector;
import java.util.Enumeration;
import java.util.Collections;
import java.awt.Point;

class RingNodeFranklinStages extends Node {

	public Vector[] messagesWaiting;
	public Point queueCoordsRight; // drawing coordinates
	public Point queueCoordsLeft; // drawing coordinates

	// Basic constructor
	RingNodeFranklinStages(Integer ID) {
		super(ID);
		messagesWaiting = new Vector[2];
		messagesWaiting[LEFT] = new Vector();
		messagesWaiting[RIGHT] = new Vector();
	}

	/* Receive a message.
	 * Dispatch to correct method depending on state.
	 */
	public synchronized void receive(String msg, int dir) {
		NetViewer.out.println("Node "+id+" received message "+msg+" on the "+((dir==0)?("RIGHT"):("LEFT"))+".");
		switch (state) {
			case ASLEEP: asleep(msg, dir);
					 break;
			case CANDIDATE: candidate(msg, dir);
					    break;
			case PASSIVE: passive(msg, dir);
					  break;
                  // All other states (LEADER and FOLLOWER) have no action associated with them
		}
	}

	/* Process message received while state = ASLEEP.
	 *   dir is the direction from which the message arrived.
	 */
	private void asleep(String msg, int dir) {
		initialize();
		messagesWaiting[dir].add(msg); // enqueue message and wait for a message to arrive on the opposite link
		NetViewer.out.println("Node "+id+" enqueued "+msg+" on the "+((dir==0)?("RIGHT"):("LEFT"))+": "+messagesWaiting[dir].toString());
	}

	/* Process message received while state = CANDIDATE.
	 *   dir is the direction from which the message arrived.
	 */
	private void candidate(String msg, int dir) {
		int oppositeDir = Math.abs(dir-1);
		if (messagesWaiting[oppositeDir].isEmpty()) { // enqueue message and wait for a message in the opposite direction
			int msgInt = Integer.parseInt(msg);
			messagesWaiting[dir].add(msg);
			NetViewer.out.println("Node "+id+" added "+msg+" to the queue on the "+((dir==0)?("RIGHT"):("LEFT"))+": "+messagesWaiting[dir].toString());
		}
		else {
			String msga = (String)messagesWaiting[oppositeDir].remove(0);
			int msg_a = Integer.parseInt(msga);
			int msg_b = Integer.parseInt(msg);
			if (msg_a == id && msg_b == id) {
				become(LEADER);
				send("notification", LEFT);
				NetViewer.out.println("**** LEADER FOUND **** Node "+id+". Notification sent LEFT.");
			}
			else if (msg_a < id || msg_b < id) {
				become(PASSIVE);
				// Any messages that were waiting must be forwarded in the order they arrived
				Enumeration e = messagesWaiting[oppositeDir].elements();
				while (e.hasMoreElements()) {
					send((String)e.nextElement(), dir);
				}
				messagesWaiting[oppositeDir].removeAllElements();
				NetViewer.out.println("Node "+id+" just became passive and forwarded any remaining messages in its queue.");
			}
			else { // survived this stage; enter next stage
				send(idString, LEFT);
				send(idString, RIGHT);
				NetViewer.out.println("Node "+id+" just entered a new stage and sent its ID left & right.");
			}
		}
}

	/* Process message received while state = PASSIVE.
	 *   dir is the direction from which the message arrived.
	 */
	private void passive(String msg, int dir) {
		if (msg == "notification")
			become(FOLLOWER);
		send(msg, Math.abs(dir-1));
		NetViewer.out.println("Passive node "+id+" just forwarded message "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT")));
	}

  /* Initialization sequence.
   */
	protected void initialize() {
		become(CANDIDATE);
		send(idString, LEFT);
		send(idString, RIGHT);
		NetViewer.out.println("Node "+id+" initialized; sent ID left & right.");
	}

	/*-------- Methods used for drawing message queues --------*/

	public Vector getMessagesWaiting(int whichSideOfNode /*LEFT or RIGHT*/) {
		return messagesWaiting[whichSideOfNode];
	}

	/* Set the coordinates of the message queue on the left. Used for drawing.
   	 */
	public void setQueueCoordsLeft(Point coords) {
		this.queueCoordsLeft = coords;
	}

	/* Set the coordinates of the message queue on the left. Used for drawing.
   	 */
	public void setQueueCoordsLeft(double x, double y) {
		this.queueCoordsLeft = new Point((int)x, (int)y);
	}

	/* Get the coordinates of the message queue on the left. Used for drawing.
   	 */
	public Point getQueueCoordsLeft() {
		return queueCoordsLeft;
	}

	/* Set the coordinates of the message queue on the right. Used for drawing.
   	 */
	public void setQueueCoordsRight(Point coords) {
		this.queueCoordsRight = coords;
	}

	/* Set the coordinates of the message queue on the right. Used for drawing.
   	 */
	public void setQueueCoordsRight(double x, double y) {
		this.queueCoordsRight = new Point((int)x, (int)y);
	}

	/* Get the coordinates of the message queue on the right. Used for drawing.
   	 */
	public Point getQueueCoordsRight() {
		return queueCoordsRight;
	}

	/*------------- CASES (best/worst/average) -------------*/

	/* Arrange ids into average case configuration (randomize).
	 */
	public static boolean average()  {
		Collections.shuffle(ids); // randomize
		return true;
	}

	/* Arrange ids into best case configuration (increasing around ring).
	 */
	public static boolean best()  {
		Collections.sort(ids); // increasing around ring
		NetViewer.setSynchronous(true); // makes it easier to follow animation
		NetViewer.setInstantWakeUp(true); // makes it easier to follow animation
		return true;
	}

	/* Arrange ids into worst case configuration.
	 * Complicated. You'll really have to study the code to figure it out!
	 */
	public static boolean worst()  {
		int numNodes = ids.size();
		Vector idsCopy = (Vector)ids.clone();
		ids.clear();
		Collections.sort(idsCopy); // sort into increasing order
		Enumeration idsList = idsCopy.elements();
		ids.add((Integer)idsList.nextElement());
		ids.add((Integer)idsList.nextElement()); // networks must contain at least 2 nodes, so it's ok to do this
		int factor;
		while (ids.size() < numNodes) {
			factor = ids.size();
			for (int i=1; i<2*factor; i=i+2) { // add into the odd spaces
				ids.add(i, (Integer)idsList.nextElement());
				if (ids.size() == numNodes) break;
			}
		}
		NetViewer.setSynchronous(true); // makes it easier to follow animation
		NetViewer.setInstantWakeUp(true); // makes it easier to follow animation
		return true;
	}
}
