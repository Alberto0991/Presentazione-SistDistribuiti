/*
 * NetViewer
 *
 * Ring Network
 * All The Way (LeLann 1977)
 *
 * **A counter travels with the message**, so when a node receives its ID back,
 * it reads the counter to get the ring size. This means FIFO is not necessary.
 * ie. message may cross.
 * The nodes themselves count the messages they have seen. This way they will know
 * they have not seen all ids if their count is not equal to the counter in the
 * message. (even if they have received their own id back.
 * Note: No notification stage.
 */

class RingNodeAllTheWay extends Node {

  int minimum; // the smallest ID seen so far
  int ringsize, numMessagesSeenByNode;
  boolean ringsizeKnown;

	RingNodeAllTheWay(Integer ID) {
		super(ID);
	}

	/* Send a message; put it on the link in direction dir.
   * Doesn't use the send() in Node because we need a 3rd parameter.
	 */
	protected void send(String msg, int dir, int numNodesSeenByMessage) {
		((Link)links.get(dir)).receive(msg, dir, numNodesSeenByMessage);
	}

	/* Receive a message.
	 * Dispatch to correct method depending on state.
	 */
	public synchronized void receive(String msg, int dir, int numNodesSeenByMessage) {
		NetViewer.out.println("Node "+id+" received message "+msg+" on the "+((dir==0)?("RIGHT"):("LEFT"))+".");
		switch (state) {
			case ASLEEP: asleep(msg, dir, numNodesSeenByMessage);
					 break;
			case AWAKE: awake(msg, dir, numNodesSeenByMessage);
					    break;
		}
	}

	/* Process message received while state = ASLEEP.
	 *   dir is the direction from which the message arrived.
	 */
	private void asleep(String msg, int dir, int numNodesSeenByMessage) {
		initialize();
		send(msg, Math.abs(dir-1), numNodesSeenByMessage+1); // blindly forward ID
		numMessagesSeenByNode++;
		int msgInt = Integer.parseInt(msg);
		minimum = msgInt<minimum?msgInt:minimum;
		NetViewer.out.println("Node "+id+" has been woken up, forwarded "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT"))+". min: "+minimum);
	}

	/* Process message received while state = AWAKE.
	 *   dir is the direction from which the message arrived.
	 */
	private void awake(String msg, int dir, int numNodesSeenByMessage) {
		numMessagesSeenByNode++;
		int msgInt = Integer.parseInt(msg);
		if (msgInt != id)
		{
			send(msg, Math.abs(dir-1), numNodesSeenByMessage+1);
			minimum = msgInt<minimum?msgInt:minimum;
			//numMessagesSeenByNode++;
			if (ringsizeKnown)
				checkIfDone();
			NetViewer.out.println("Node "+id+" forwarded "+msg+" to the "+((Math.abs(dir-1)==0)?("RIGHT"):("LEFT"))+". min: "+minimum);
            }
		else /* receive my own ID back */
		{
			ringsize = numNodesSeenByMessage;
			ringsizeKnown = true;
			checkIfDone();
		}
	}

  /* Initialization sequence.
   */
  protected void initialize() {
    become(AWAKE);
    minimum = id;
    ringsizeKnown = false;
    numMessagesSeenByNode = 0;
    int numNodesSeenByMessage = 1;
    send(idString, RIGHT, numNodesSeenByMessage);
    NetViewer.out.println("Node "+id+" initialized. ID sent to the right.");
  }

  private void checkIfDone() {
    if (numMessagesSeenByNode == ringsize) {
      if (minimum == id)
      {
        become(LEADER);
        NetViewer.out.println("**** LEADER FOUND **** Node "+id+".");
      }
      else
      {
        become(FOLLOWER);
        NetViewer.out.println("Node "+id+" became FOLLOWER.");
      }
    }
    else {
			NetViewer.out.println("Node "+id+" not finished yet.");
    }
  }

  /*------------- CASES (best/worst/average) -------------*/

	/* Arrange ids into average case configuration.
	 */
	public static boolean average()  {
		return false; // no change --> all cases the same
	}

	/* Arrange ids into best case configuration.
	 */
	public static boolean best()  {
		return false; // no change --> all cases the same
	}

	/* Arrange ids into worst case configuration.
	 */
	public static boolean worst()  {
		return false; // no change --> all cases the same
	}

}
