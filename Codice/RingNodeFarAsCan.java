/*
 * NetViewer
 *
 * Network: Ring
 * Algorithm: As Far As It Can (Chang & Roberts)
 *
 * Description:
 *
 */

import java.util.Collections;

class RingNodeFarAsCan extends Node {

	RingNodeFarAsCan(Integer ID) {
		super(ID);
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
		}
	}

	/* Process message received while state = ASLEEP.
	 *   dir is the direction from which the message arrived.
	 */
	private void asleep(String msg, int dir) {
	  initialize();
		int msgInt = Integer.parseInt(msg);
		if (id < msgInt) {
			// do nothing; remain candidate
			// We don't need to resend id; a node only ever sends its ID once in this algorithm.
			NetViewer.out.println("Node "+id+" has been woken up, remained candidate, defeated "+msg+".");
		}
		else /* msg < id */ {
			become(PASSIVE);
			send(msg, Math.abs(dir-1)); // send in opposite direction
			NetViewer.out.println("Node "+id+" has been woken up, become passive, and sent msg "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT"))+".");
		}
	}

	/* Process message received while state = CANDIDATE.
	 *   dir is the direction from which the message arrived.
	 */
	private void candidate(String msg, int dir) {
		int msgInt = Integer.parseInt(msg);
		if (id < msgInt) {
			// do nothing; remain candidate
			NetViewer.out.println("Node "+id+" remains candidate, defeats "+msg);
		}
		else if (msgInt < id) {
			become(PASSIVE);
			send(msg, Math.abs(dir-1)); // send in opposite direction
			NetViewer.out.println("Node "+id+" defeated. Became passive and forwarded "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT"))+".");
		}
		else { /* received id */
			become(LEADER);
			send("notification", Math.abs(dir-1));
			NetViewer.out.println("**** LEADER FOUND **** Node "+id+". Sent notification.");
		}
	}

	/* Process message received while state = PASSIVE.
	 *   dir is the direction from which the message arrived.
	 */
	private void passive(String msg, int dir) {
		if (msg == "notification")
			become(FOLLOWER);
		send(msg, Math.abs(dir-1)); // send in opposite direction
		NetViewer.out.println("Passive node "+id+" forwarded message "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT"))+".");
	}

  /* Initialization sequence.
   */
  protected void initialize() {
    become(CANDIDATE);
    send(idString, RIGHT);
    NetViewer.out.println("Node "+id+" initialized. ID sent to the right.");
  }

  /*------------- CASES (best/worst/average) -------------*/

	/* Arrange ids into average case configuration (randomize).
	 */
	public static boolean average()  {
		Collections.shuffle(ids); // randomize
		return true;
	}

	/* Arrange ids into best case configuration (decreasing around ring).
	 */
	public static boolean best()  {
		Collections.sort(ids);
		Collections.reverse(ids); // decreasing around ring
		NetViewer.setSynchronous(true); // makes it easier to follow animation
		NetViewer.setInstantWakeUp(true); // makes it easier to follow animation
		return true;
	}

	/* Arrange ids into worst case configuration (increasing around ring).
	 */
	public static boolean worst()  {
		Collections.sort(ids); // increasing around ring
		NetViewer.setSynchronous(true); // makes it easier to follow animation
		NetViewer.setInstantWakeUp(true); // makes it easier to follow animation
		return true;
	}
}
