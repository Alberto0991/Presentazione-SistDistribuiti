import java.awt.Color;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class ArbitraryNodeShout extends Node {
	
	private boolean root;
	private Vector treeNeighbors;
	private Integer counter;
	private Link parent;
	
	ArbitraryNodeShout(Integer ID)
	{
		super(ID);
		treeNeighbors = new Vector();
		counter = 0;
		if(ID == (int)ids.firstElement()) {
			become(INITIATOR);
			root = true;
			setWakeUpDelay(1); // instant wakeup
			setWakeUpPosition(1); // instant wakeup		
		}
		else {
			become(IDLE);
			root = false;
		}
	}	
	
	public boolean isFinished() {
		return (state == DONE);
	}
	
	public synchronized void receive(String msg, Link link) {
		switch (state) {
			case IDLE: 
				idle(msg, link);
				break;
			case ACTIVE:  
				active(msg, link);
				break;
		}
	}
	
	protected void initialize() {
		if (state == INITIATOR)
		{
			become(ACTIVE);
			
			Link link;
			Enumeration allLinks = links.elements();
			while (allLinks.hasMoreElements()) {
				link = (Link)allLinks.nextElement();
				send("Q", link);
			}
			NetViewer.out.println("Node "+id+" is the initiator and sent a request Q to all his neighbours");
		}
		
	}
	
	private void idle(String msg, Link linkMsgArrivedOn)
	{
		parent = linkMsgArrivedOn;
		treeNeighbors.add(linkMsgArrivedOn);
		send("Yes", linkMsgArrivedOn);
		NetViewer.out.println("Node "+id+" has become son of " + getNeighbourId(linkMsgArrivedOn) + " and replied Yes to him");
		counter++;
		if(counter == links.size()) {
			become(DONE);
		}
		else{
			Link link;
			Enumeration allLinks = links.elements();
			while (allLinks.hasMoreElements()) {
				link = (Link)allLinks.nextElement();
				if (link != linkMsgArrivedOn)
					send("Q", link);
			}
			become(ACTIVE);
			NetViewer.out.println("Node "+id+" has sent a Q to all his neighbors except his parent " + getNeighbourId(linkMsgArrivedOn));
		}
	}
	
	private void active(String msg, Link linkMsgArrivedOn)
	{
		switch (msg) {
			case "Q":
				send("No", linkMsgArrivedOn);
				NetViewer.out.println("Node "+id+" received a Q  from " + getNeighbourId(linkMsgArrivedOn) +" and answered No");
				break;
			case "Yes":
				treeNeighbors.add(linkMsgArrivedOn);
				NetViewer.out.println("Node "+id+" received Yes from " + getNeighbourId(linkMsgArrivedOn) + " and became his parent");
				counter++;
				if(counter == links.size()) {
					become(DONE);
					NetViewer.out.println("Node "+id+" is done");
				}
				break;
			case "No":
				NetViewer.out.println("Node "+id+" received a No from " + getNeighbourId(linkMsgArrivedOn));
				counter++;
				if(counter == links.size()) {
					become(DONE);
					NetViewer.out.println("Node "+id+" is done");
				}
				linkMsgArrivedOn.setColor(Color.cyan);
				break;
		}
	}	
	
	private int getNeighbourId(Link link) {
		int potentialId = link.getNode(0).id;
		if (potentialId != id) {
			return potentialId;
		}
		return link.getNode(1).id;
	}
}