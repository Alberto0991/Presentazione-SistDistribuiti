import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

public class CompleteGraphNodeElection extends Node{

	private int stage;
	private int value;
	private Link owner;
	private Link attack;
	private int ownerStage;
	private Enumeration others;
	private boolean close;
	private ArrayList<Queue> queue;
	
	CompleteGraphNodeElection(Integer ID) {
		super(ID);
		close = false;
		queue = new ArrayList<Queue>();
	}
	
	protected void send(String msg, Link link, int stage, int value){
		link.receive(msg, this, stage, value);
	}
	
	public synchronized void receive(String msg, Link link, int stage, int value) {
		switch (state) {
			case ASLEEP: 
				asleep(msg, link, stage, value);
				break;
			case CANDIDATE:  
				candidate(msg, link, stage, value);
				break;
			case PASSIVE:  
				passive(msg, link, stage, value);
				break;
			case CAPTURED:
				captured(msg, link, stage, value);
				break;
		}
	}
	
	public boolean isFinished() {
		return (state == LEADER || state == FOLLOWER);
	}
	
	protected void initialize() {
		stage=1;
		value=id;
		others = links.elements();
		if (others.hasMoreElements()){
			Link next = (Link)others.nextElement();
			send ("Capture", next, stage, value);
			NetViewer.out.println("Node "+id+" initialized. Sent Capture to " + getNeighbourId(next));
		}
		become(CANDIDATE);
		NetViewer.out.println("Node "+id+" become CANDIDATE");
	}
	
	private void asleep(String msg, Link linkMsgArrivedOn, int stage, int value) {
		//Receiving Capture Message
		send ("Accept", linkMsgArrivedOn, stage, value);
		NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + " and sent Accept");
		this.stage=1;
		owner=linkMsgArrivedOn;
		ownerStage=stage+1;
		become(CAPTURED);
		NetViewer.out.println("Node "+id+" become CAPTURED");
	}
	
	private void candidate(String msg, Link linkMsgArrivedOn, int stage, int value) {
		switch (msg) {
		
		case "Capture":
			if( (stage<this.stage) || (stage==this.stage && value>this.value)){
				send ("Reject", linkMsgArrivedOn, this.stage, 0);
				NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + 
						" and sent Reject");
			}
			else{
				send ("Accept", linkMsgArrivedOn, stage, value);
				NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + 
						" and sent Accept");
				owner=linkMsgArrivedOn;
				ownerStage=stage+1;
				become(CAPTURED);
				NetViewer.out.println("Node "+id+" become CAPTURED");
			}
			break;
			
		case "Accept":
			NetViewer.out.println();
			this.stage=this.stage+1;
			if ( this.stage>=(1+ids.size()/2) ){
				Enumeration allLinks = links.elements();
				while (allLinks.hasMoreElements())
					send ("Terminate", (Link)allLinks.nextElement(), 0, 0);
				
				NetViewer.out.println("Node "+id+" received Accept from " + getNeighbourId(linkMsgArrivedOn) + 
						", terminated and sent terminate in all directions.");
				become(LEADER);
				NetViewer.out.println("Node "+id+" become LEADER");
			}else{
				if (others.hasMoreElements()){
					Link next = (Link)others.nextElement();
					send ("Capture", next, this.stage, this.value);
					NetViewer.out.println("Node "+id+" received Accept from " + getNeighbourId(linkMsgArrivedOn) +" and sent capture to " + getNeighbourId(next));
				}
			}
			break;
			
		case "Reject":
			become(PASSIVE);
			NetViewer.out.println("Node "+id+" received Reject from " + getNeighbourId(linkMsgArrivedOn)
					+ " and become PASSIVE");
			break;
		
		case "Terminate":
			become(FOLLOWER);
			NetViewer.out.println("Node "+id+" received Terminate from " + getNeighbourId(linkMsgArrivedOn)
					+ " and become FOLLOWER");
			break;
			
		case "Warning":
			if( (stage<this.stage) || (stage==this.stage && value>this.value)){
				send ("No", linkMsgArrivedOn, this.stage, 0);
				NetViewer.out.println("Node "+id+" received Warning from " + getNeighbourId(linkMsgArrivedOn)
					+ " and sent No");
			}
			else{
				send ("Yes", linkMsgArrivedOn, stage, 0);
				NetViewer.out.println("Node "+id+" received Warning from " + getNeighbourId(linkMsgArrivedOn)
						+ " and sent Yes");
				become(PASSIVE);
				NetViewer.out.println("Node "+id+" become PASSIVE");
			}
			break;

		}			
	}
	
	private void passive(String msg, Link linkMsgArrivedOn, int stage, int value) {
		switch (msg) {

		case "Capture":
			if( (stage<this.stage) || (stage==this.stage && value>this.value)){
				send ("Reject", linkMsgArrivedOn, this.stage, 0);
				NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + 
						" and sent Reject");
			}
			else{
				send ("Accept", linkMsgArrivedOn, stage, value);
				NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + 
						" and sent Accept");
				owner=linkMsgArrivedOn;
				ownerStage=stage+1;
				become(CAPTURED);
				NetViewer.out.println("Node "+id+" become CAPTURED");
			}
			break;

		case "Warning":
			if( (stage<this.stage) || (stage==this.stage && value>this.value)){
				send ("No", linkMsgArrivedOn, this.stage, 0);
				NetViewer.out.println("Node "+id+" received Warning from " + getNeighbourId(linkMsgArrivedOn)
						+ " and sent No");
			}
			else{
				send ("Yes", linkMsgArrivedOn, stage, 0);
				NetViewer.out.println("Node "+id+" received Warning from " + getNeighbourId(linkMsgArrivedOn)
						+ " and sent Yes");
			}
			break;

		case "Terminate":
			become(FOLLOWER);
			NetViewer.out.println("Node "+id+" received Terminate from " + getNeighbourId(linkMsgArrivedOn)
					+ " and become FOLLOWER");
			break;
			
		}
	}
	
	private void captured(String msg, Link linkMsgArrivedOn, int stage, int value) {
		if( (close==false) || (close==true && linkMsgArrivedOn==owner)){
			captured_(msg, linkMsgArrivedOn, stage, value);
				
			/*
			 * Se close=true vuol dire che il nodo deve mettere in coda tutti i messaggi che non 
			 * arrivano da owner. Quado questo arriva rimetterà nuovamente close=true
			 */

			Iterator<Queue> it = queue.iterator();
			while(it.hasNext() && close==false)//se ci sono elementi nella coda e li possiamo gestire
			{
				Queue q = it.next();
				NetViewer.out.println("Node "+id+" processed queue message from " +
						getNeighbourId(linkMsgArrivedOn));
				captured_(q.getMsg(), q.getLink(), q.getStage(), q.getValue());
				it.remove();
			}			
		}
		else{
			//salvo i messaggi nella coda
			queue.add(new Queue(msg, linkMsgArrivedOn, stage, value));
			NetViewer.out.println("Node "+id+" received message from " + getNeighbourId(linkMsgArrivedOn)
					+ " and put it on queue");

		}
	}

	private void captured_(String msg, Link linkMsgArrivedOn, int stage, int value){
		switch(msg){

		case "Capture":
			if(stage<ownerStage){
				send ("Reject", linkMsgArrivedOn, ownerStage, 0);
				NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + 
						" and sent Reject");
			}
			else{
				attack=linkMsgArrivedOn;
				send ("Warning", owner, stage, value);
				NetViewer.out.println("Node "+id+" received Capture from " + getNeighbourId(linkMsgArrivedOn) + 
						" and sent Warning to " + getNeighbourId(owner) + "(owner)");
				close=true;//close N(x)-{owner}
			}
			break;

		case "No":
			close=false;//open N(x)
			send("Reject", attack, stage, 0);
			NetViewer.out.println("Node "+id+" received No from " + getNeighbourId(linkMsgArrivedOn) + 
					" and sent Reject to "+attack+"(attack)");			
			break;

		case "Yes":
			ownerStage=stage+1;
			owner=attack;
			close=false;//open N(x)
			send ("Accept", attack, stage, value);
			NetViewer.out.println("Node "+id+" received Yes from " + getNeighbourId(linkMsgArrivedOn) + 
					" and sent Accept to "+attack+"(attack)");			

		case "Warning":
			if(stage<ownerStage){
				send ("No", linkMsgArrivedOn, ownerStage, 0);
				NetViewer.out.println("Node "+id+" received Warning from " + getNeighbourId(linkMsgArrivedOn)
						+ " and sent No");
			}
			else{
				send ("Yes", linkMsgArrivedOn, stage, 0);
				NetViewer.out.println("Node "+id+" received Warning from " + getNeighbourId(linkMsgArrivedOn)
						+ " and sent Yes");
			}
			break;

		case "Terminate":
			become(FOLLOWER);
			NetViewer.out.println("Node "+id+" received Terminate from " + getNeighbourId(linkMsgArrivedOn)
					+ " and become FOLLOWER");
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
