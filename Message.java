/*
 * NetViewer
 * Class: Message
 */

import java.awt.Color;

class Message extends Thread {

	private static int totalMessagesWithN, totalMessagesNoN; // total # of messages sent since the algorithm started executing
	private String msg; // the message on the link
	private int dir; // the direction the message is travelling on the link
	private Link callingLink; // the link the message is on
	private long timeLeft; // so we can query the message to find how far along the link it is
	private int numNodesSeenByMessage; // only for All The Way algorithm
	private long speed; // the length of time it takes for a message travel along the link
	private boolean isDummy = false;
	
	/*only for Election in complete network*/
	private int stage;
	private int value;

	public static Color MESSAGE_COLOUR = Color.blue; // for drawing
	public static Color MESSAGE_STRING_COLOUR = Color.black; // for drawing

	/* Constructor
	 */
	Message(String msg, int dir, Link callingLink) {
		this.msg = msg;
		this.dir = dir;
 		this.callingLink = callingLink;
		numNodesSeenByMessage = -1;
		stage = -1;
		value = -1;
		totalMessagesWithN++;
		if (!msg.equals("notification"))
			totalMessagesNoN++;
		if (!NetViewer.isFIFO()) // speed is in the messages
			speed = callingLink.getNewSpeed(); // generate a speed for the message based on the speed slider value
		else // speed is on the links
			speed = callingLink.getSpeed();
		timeLeft = speed;
	}

	/* Special constructor, only for All The Way algorithm.
	 */
	Message(String msg, int dir, Link callingLink, int numNodesSeenByMessage) {
		this.msg = msg;
		this.dir = dir;
		this.callingLink = callingLink;
		this.numNodesSeenByMessage = numNodesSeenByMessage;
		totalMessagesWithN++;
		if (!msg.equals("notification"))
			totalMessagesNoN++;
		if (!NetViewer.isFIFO()) // speed is in the messages
			speed = callingLink.getNewSpeed(); // generate a speed for the message based on the speed slider value
		else // speed is on the links
			speed = callingLink.getSpeed();
		timeLeft = speed;
	}

	/* Special constructor, for Election in complete network for example .
	 */
	Message(String msg, int dir, Link callingLink, int stage, int value) {
		this.msg = msg;
		this.dir = dir;
		this.callingLink = callingLink;
		this.stage = stage;
		this.value = value;
		totalMessagesWithN++;
		if (!msg.equals("notification"))
			totalMessagesNoN++;
		if (!NetViewer.isFIFO()) // speed is in the messages
			speed = callingLink.getNewSpeed(); // generate a speed for the message based on the speed slider value
		else // speed is on the links
			speed = callingLink.getSpeed();
		timeLeft = speed;
	}

	/* Constructor for a dummy message, used in synchronous networks.
	 * Does not increment total message count.
	 */
	Message(String msg, int dir, Link callingLink, boolean dummy) {
		this.msg = msg;
		this.dir = dir;
 		this.callingLink = callingLink;
		speed = callingLink.getSpeed(); // FIFO because synchronous
		timeLeft = speed;
		isDummy = true;
	}

	public synchronized void run() {
		try {
			while (timeLeft > 0) {
				wait(79);
				timeLeft -= 79;
				if (NetViewer.isAborted()) {
					timeLeft = 0;
					break;
				}
				while (NetViewer.isPaused())
				{
					wait(100); // Do nothing. Wait for user to resume algorithm.
				}		     // This avoids busy waiting, which freezes the screen.
			}
			if (!NetViewer.isAborted())
				callingLink.alertMsgFinished(this);
			else
				callingLink.removeMessage(this);
		} catch (InterruptedException e){
			NetViewer.out.println("A Message got interrupted. This should never happen!");
		}
	}

  public String getMessage() {
    return msg;
  }

  public int getDirection() {
    return dir;
  }

  public Link getCallingLink() {
   return callingLink;
  }

  public void setTimeLeft(long timeLeft) {
    this.timeLeft = timeLeft;
  }

  public long getTimeLeft() {
    return timeLeft;
  }

  public long getSpeed() {
    return speed;
  }

  public void setSpeed(long newSpeed) {
    this.speed = speed;
  }

  /* Return true if this message a dummy.
   * Used in synchronous networks to show all messages on a link in one nice string.
   */
  public boolean isDummy() {
		return isDummy;
	}

	/* Set new speed and time left for this message.
	 * Used when the link this message is on changes speed.
	 */
  public void setNewInfo(long newTimeLeft, long newSpeed) {
    this.timeLeft = newTimeLeft;
    this.speed = newSpeed;
  }

  public static int getTotalMessagesWithN() {
    return totalMessagesWithN;
  }

  public static int getTotalMessagesNoN() {
    return totalMessagesNoN;
  }

  public static void resetTotalMessages() {
    totalMessagesWithN = 0;
    totalMessagesNoN = 0;
  }

  /* For ring algorithm "All The Way"
   */
  public int getNumNodesSeenByMessage() {
    return numNodesSeenByMessage;
  }

  public String toString() {
	  return msg;
  }
  
  public int getStage() {
	  return stage;
  }

  public void setStage(int stage) {
	  this.stage = stage;
  }
  
  public int getValue() {
	  return value;
  }

  public void setValue(int value) {
	  this.value = value;
  }

}