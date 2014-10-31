/*
 * NetViewer
 *
 * Ring Network
 * Alternating Steps Algorithm (Peterson)
 *
 * 1. Each node wakes up and sends its ID to the right.
 * 2. Each node compares the ID it received from the left with its own ID.
 * 3. Become passive if the received ID is smaller.
 * 4. Otherwise (survived) send own ID the the *left*.
 * Repeat, alternating the direction at each stage.
 *
 */

class RingNodeAlternatingSteps extends Node {

	RingNodeAlternatingSteps(Integer ID) {
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
		become(CANDIDATE);
		send(idString, RIGHT);
		candidate(msg, dir);
		NetViewer.out.println("Node "+id+" has been woken up. Sent ID to the right.");
	}

	/* Process message received while state = CANDIDATE.
	 *   dir is the direction from which the message arrived.
	 */
	private void candidate(String msg, int dir) {
		int msgInt = Integer.parseInt(msg);
		if (msgInt < id) // If the msg is smaller than my own ID, become passive
		{
			become(PASSIVE);
			NetViewer.out.println("Node "+id+" has become passive because it received "+msg+".");
		}
		else if (id < msgInt) // If my id is smaller, send it back in the direction the message came from
		{
			send(idString, dir); // and remain a candidate
			NetViewer.out.println("Node "+id+" has defeated "+msg+" and sent its ID back to the "+((dir==0)?("RIGHT"):("LEFT"))+".");
		}
		else // have received my own ID --> LEADER
		{
			become(LEADER);
			send("notification", LEFT);
			NetViewer.out.println("**** LEADER FOUND **** Node "+id+". Notification sent LEFT.");
		}
	}

	/* Process message received while state = PASSIVE.
	 *   dir is the direction from which the message arrived.
	 */
	private void passive(String msg, int dir) {
		if (msg == "notification")
			become(FOLLOWER);
		send(msg, Math.abs(dir-1));
		NetViewer.out.println("Passive node "+id+" just forwarded message "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT"))+".");
	}

  /* Initialization sequence.
   */
	protected void initialize() {
		become(CANDIDATE);
		send(idString, RIGHT);
		NetViewer.out.println("Node "+id+" just woke up and sent its ID to the right.");
	}

	/*------------- CASES (best/worst/average) -------------*/

	/* Arrange ids into average case configuration.
	 */
	public static boolean average()  {
		return false; // not implemented
	}

	/* Arrange ids into best case configuration.
	 */
	public static boolean best()  {
		return false; // not implemented
	}

	/* Arrange ids into worst case configuration.
	 */
	public static boolean worst()  {
		return false; // not implemented
	}
}
