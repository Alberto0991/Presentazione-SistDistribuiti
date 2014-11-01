import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

public class CompleteGraphNodeElection extends Node {

	private InternalState state;
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
		stage =0;
		close = false;
		queue = new ArrayList<Queue>();
		state = new AsleepState();
	}

	private abstract class InternalState {
		
		public abstract void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value);
		public abstract boolean isFinalState();
	}
	

	private class AsleepState extends InternalState{
		
		public AsleepState(){
			become(ASLEEP);
		}

		@Override
		public void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value) {
			// Receiving Capture Message
			send("Accept", linkMsgArrivedOn, stage, value);
			NetViewer.out.println("Node " + id + " received Capture from "
					+ getNeighbourId(linkMsgArrivedOn) + " and sent Accept");
			CompleteGraphNodeElection.this.stage = 1;
			owner = linkMsgArrivedOn;
			ownerStage = stage + 1;
			state = new CapturedState();
			NetViewer.out.println("Node " + id + " become CAPTURED");
			
		}

		@Override
		public boolean isFinalState() {
			return false;
		}
		
	}
	
	private class CandidateState extends InternalState {

		CandidateState() {
			stage =1;
			become(CANDIDATE);
		}
		

		@Override
		public void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value) {

			int currentStage = CompleteGraphNodeElection.this.stage;
			int currentValue = CompleteGraphNodeElection.this.value;

			switch (msg) {

			case "Capture":
//				System.out.println("I am candidate and i am being captured: my stage is:" +currentStage+ "attack stage: "+stage);
				if ((stage < currentStage)
						|| (stage == currentStage && value > currentValue)) {
					send("Reject", linkMsgArrivedOn, currentStage, 0);
					NetViewer.out.println("Node " + id
							+ " received Capture from "
							+ getNeighbourId(linkMsgArrivedOn)
							+ " and sent Reject");
				} else {
					send("Accept", linkMsgArrivedOn, stage, value);
					NetViewer.out.println("Node " + id
							+ " received Capture from "
							+ getNeighbourId(linkMsgArrivedOn)
							+ " and sent Accept");
					owner = linkMsgArrivedOn;
					ownerStage = stage + 1;
					state = new CapturedState();
					NetViewer.out.println("Node " + id + " become CAPTURED");
				}
				break;

			case "Accept":
				NetViewer.out.println();
				CompleteGraphNodeElection.this.stage++;
//				System.out.println("Stage: "+CompleteGraphNodeElection.this.stage);
//				System.out.println("ids: "+ids.size());
				if (CompleteGraphNodeElection.this.stage >= (1 + ids.size() / 2)) {
					Enumeration allLinks = links.elements();
					while (allLinks.hasMoreElements())
						send("Terminate", (Link) allLinks.nextElement(), 0, 0);

					NetViewer.out
							.println("Node "
									+ id
									+ " received Accept from "
									+ getNeighbourId(linkMsgArrivedOn)
									+ ", terminated and sent terminate in all directions.");
					state = new LeaderState();
					NetViewer.out.println("Node " + id + " become LEADER");
				} else {
					if (others.hasMoreElements()) {
						Link next = (Link) others.nextElement();
						send("Capture", next, CompleteGraphNodeElection.this.stage, currentValue);
						NetViewer.out.println("Node " + id
								+ " received Accept from "
								+ getNeighbourId(linkMsgArrivedOn)
								+ " and sent capture to "
								+ getNeighbourId(next));
					}
				}
				break;

			case "Reject":
				state = new PassiveState();
				NetViewer.out.println("Node " + id + " received Reject from "
						+ getNeighbourId(linkMsgArrivedOn)
						+ " and become PASSIVE");
				break;

			case "Terminate":
				state = new FollowerState();
				NetViewer.out.println("Node " + id
						+ " received Terminate from "
						+ getNeighbourId(linkMsgArrivedOn)
						+ " and become FOLLOWER");
				break;

			case "Warning":
				if ((stage < currentStage)
						|| (stage == currentStage && value > currentValue)) {
					send("No", linkMsgArrivedOn, currentStage, 0);
					NetViewer.out
							.println("Node " + id + " received Warning from "
									+ getNeighbourId(linkMsgArrivedOn)
									+ " and sent No");
				} else {
					send("Yes", linkMsgArrivedOn, stage, 0);
					NetViewer.out.println("Node " + id
							+ " received Warning from "
							+ getNeighbourId(linkMsgArrivedOn)
							+ " and sent Yes");
					state = new PassiveState();
					NetViewer.out.println("Node " + id + " become PASSIVE");
				}
				break;

			}
		}


		@Override
		public boolean isFinalState() {
			return false;
		}

	}

	private class CapturedState extends InternalState {
		
		public CapturedState(){
			become(CAPTURED);
		}

		@Override
		public void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value) {
			if ((close == false) || (close == true && linkMsgArrivedOn == owner)) {
				_process(msg, linkMsgArrivedOn, stage, value);

				/*
				 * Se close=true vuol dire che il nodo deve mettere in coda tutti i
				 * messaggi che non arrivano da owner. Quado questo arriva rimetter�
				 * nuovamente close=true
				 */

				Iterator<Queue> it = queue.iterator();
				while (it.hasNext() && close == false)// se ci sono elementi nella
														// coda e li possiamo
														// gestire
				{
					Queue q = it.next();
					NetViewer.out.println("Node " + id
							+ " processed queue message from "
							+ getNeighbourId(linkMsgArrivedOn));
					_process(q.getMsg(), q.getLink(), q.getStage(), q.getValue());
					it.remove();
				}
			} else {
				// salvo i messaggi nella coda
				queue.add(new Queue(msg, linkMsgArrivedOn, stage, value));
				NetViewer.out
						.println("Node " + id + " received message from "
								+ getNeighbourId(linkMsgArrivedOn)
								+ " and put it on queue");

			}			
		}

		private void _process(String msg, Link linkMsgArrivedOn, int stage,
				int value) {
			switch (msg) {

			case "Capture":
				
//				System.out.println("I am CAPTURED : my stage is:"+CompleteGraphNodeElection.this.stage+" and i am being attacked by stage: "+stage);
				if (stage < ownerStage) {
					send("Reject", linkMsgArrivedOn, ownerStage, 0);
					NetViewer.out
							.println("Node " + id + " received Capture from "
									+ getNeighbourId(linkMsgArrivedOn)
									+ " and sent Reject");
				} else {
					attack = linkMsgArrivedOn;
					send("Warning", owner, stage, value);
					NetViewer.out.println("Node " + id + " received Capture from "
							+ getNeighbourId(linkMsgArrivedOn)
							+ " and sent Warning to " + getNeighbourId(owner)
							+ "(owner)");
					close = true;// close N(x)-{owner}
				}
				break;

			case "No":
				close = false;// open N(x)
				send("Reject", attack, stage, 0);
				NetViewer.out.println("Node " + id + " received No from "
						+ getNeighbourId(linkMsgArrivedOn) + " and sent Reject to "
						+ attack + "(attack)");
				break;

			case "Yes":
				ownerStage = stage + 1;
				owner = attack;
				close = false;// open N(x)
				send("Accept", attack, stage, value);
				NetViewer.out.println("Node " + id + " received Yes from "
						+ getNeighbourId(linkMsgArrivedOn) + " and sent Accept to "
						+ attack + "(attack)");

			case "Warning":
				if (stage < ownerStage) {
					send("No", linkMsgArrivedOn, ownerStage, 0);
					NetViewer.out.println("Node " + id + " received Warning from "
							+ getNeighbourId(linkMsgArrivedOn) + " and sent No");
				} else {
					send("Yes", linkMsgArrivedOn, stage, 0);
					NetViewer.out.println("Node " + id + " received Warning from "
							+ getNeighbourId(linkMsgArrivedOn) + " and sent Yes");
				}
				break;

			case "Terminate":
				state = new FollowerState();
				NetViewer.out
						.println("Node " + id + " received Terminate from "
								+ getNeighbourId(linkMsgArrivedOn)
								+ " and become FOLLOWER");
				break;
			}

		}

		@Override
		public boolean isFinalState() {
			return false;
		}
		
	}
		
	private class PassiveState extends InternalState{
		
		public PassiveState() {
			become(PASSIVE);
		}

		@Override
		public void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value) {
			
			int currentStage = CompleteGraphNodeElection.this.stage;
			int currentValue = CompleteGraphNodeElection.this.value;
			switch (msg) {

			case "Capture":
				
//				System.out.println("I am Passive with stage: "+currentStage+"and i am aatacked by stage: "+stage);
				if ((stage < currentStage)
						|| (stage == currentStage && value > currentValue)) {
					send("Reject", linkMsgArrivedOn, currentStage, 0);
					NetViewer.out
							.println("Node " + id + " received Capture from "
									+ getNeighbourId(linkMsgArrivedOn)
									+ " and sent Reject");
				} else {
					send("Accept", linkMsgArrivedOn, stage, value);
					NetViewer.out
							.println("Node " + id + " received Capture from "
									+ getNeighbourId(linkMsgArrivedOn)
									+ " and sent Accept");
					owner = linkMsgArrivedOn;
					ownerStage = stage + 1;
					state = new CapturedState();
					NetViewer.out.println("Node " + id + " become CAPTURED");
				}
				break;

			case "Warning":
				if ((stage < currentStage)
						|| (stage == currentStage && value > currentValue)) {
					send("No", linkMsgArrivedOn, currentStage, 0);
					NetViewer.out.println("Node " + id + " received Warning from "
							+ getNeighbourId(linkMsgArrivedOn) + " and sent No");
				} else {
					send("Yes", linkMsgArrivedOn, stage, 0);
					NetViewer.out.println("Node " + id + " received Warning from "
							+ getNeighbourId(linkMsgArrivedOn) + " and sent Yes");
				}
				break;

			case "Terminate":
				state = new FollowerState();
				NetViewer.out
						.println("Node " + id + " received Terminate from "
								+ getNeighbourId(linkMsgArrivedOn)
								+ " and become FOLLOWER");
				break;

			}
			
		}

		@Override
		public boolean isFinalState() {
			return false;
		}}
	
	private class LeaderState extends InternalState{
		
		public LeaderState(){
			become(LEADER);
		}

		@Override
		public void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value) {
			//DO NOTHING!
		}

		@Override
		public boolean isFinalState() {
			return true;
		}}
	
	private class FollowerState extends InternalState{
		
		public FollowerState() {
			become(FOLLOWER);
		}

		@Override
		public void processMessage(String msg, Link linkMsgArrivedOn,
				int stage, int value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isFinalState() {
			//DO NOTHING!
			return true;
		}}
	
	protected void send(String msg, Link link, int stage, int value) {
		link.receive(msg, this, stage, value);
	}

	public synchronized void receive(String msg, Link link, int stage, int value) {
		
		state.processMessage(msg, link, stage, value);
	}


	protected void initialize() {
		value = id;
		others = links.elements();
		state = new CandidateState();
		if (others.hasMoreElements()) {
			Link next = (Link) others.nextElement();
			send("Capture", next, stage, value);
			NetViewer.out.println("Node " + id
					+ " initialized. Sent Capture to " + getNeighbourId(next));
		}
		
		NetViewer.out.println("Node " + id + " become CANDIDATE");
	}



	

	private int getNeighbourId(Link link) {
		int potentialId = link.getNode(0).id;
		if (potentialId != id) {
			return potentialId;
		}
		return link.getNode(1).id;
	}

}
