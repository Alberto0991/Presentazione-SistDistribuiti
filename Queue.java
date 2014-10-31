
public class Queue {
	
	private String msg;
	private Link link;
	private int stage;
	private int value;
	
	/*
	 * Constructor
	 */
	public Queue(String msg, Link linkMsgArrivedOn, int stage, int value){
		this.msg = msg;
		link = linkMsgArrivedOn;
		this.stage = stage;
		this.value = value;
	}
	
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public Link getLink() {
		return link;
	}
	public void setLink(Link link) {
		this.link = link;
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
