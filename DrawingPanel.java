/*
 * NetViewer
 * Class: DrawingPanel
 *
 * All drawing panels subclass this class (RingPanel, GridPanel, etc.)
 * General operations, such as drawing a node and determining
 * whether the drawing area is blank, are done here.
 */

import java.util.Vector;			 import java.util.Enumeration;
import java.awt.Graphics;			 import java.awt.image.MemoryImageSource;
import java.awt.Color;         import javax.swing.JPanel;
import java.awt.Dimension;		 import java.awt.geom.AffineTransform;
import java.awt.Image;         import java.awt.event.ComponentEvent;
import java.awt.Point;         import java.awt.event.ComponentListener;
import java.awt.Graphics2D;    import java.awt.image.ImageObserver;


class DrawingPanel extends JPanel implements ComponentListener {

  protected NetworkPanel parent;
	protected boolean drawingAreaIsBlank, isDirty, nodesFinished, messagesFinished;
	protected int maxX, maxY, minX;
	protected Image image; // for background effect
	protected Graphics g;
	protected int numNodes;

  DrawingPanel(NetworkPanel parent) {
    this.parent = parent;
		drawingAreaIsBlank = true;
		isDirty = false;
		addComponentListener(this);
  }

	public boolean isBlank() {
		return drawingAreaIsBlank;
	}

	public void setIsBlank(boolean tf) {
		drawingAreaIsBlank = tf;
		if (this instanceof TreePanel || this instanceof ArbitraryPanel) { // set size of drawing area to 0 if it's the tree or arbitrary panel
			setPreferredSize(new Dimension(0,0));
			revalidate(); // let the scroll pane know to update itself
		}
	}

	/* Returns true if the algorithm has terminated naturally
	 * and the drawing area is showing its final result.
	 */
	public boolean isDirty() {
		return isDirty;
	}

	/* Set the isDirty flag.
	 * This indicates the network is ready to run
	 */
	public void setIsDirty(boolean trueORfalse) {
		isDirty = trueORfalse;
	}

	/* Draw all nodes in the network.
	 * Set colour depending on state.
	 */
	public void drawNodes() {
		Node node;
		int state, denominator = 3;
		Enumeration nodes = parent.networkManager.getNodes().elements();
		while (nodes.hasMoreElements())
		{
			node = (Node)nodes.nextElement();
			state = node.state;
			// Draw large red background circle around node if it is finished
			if (state == Node.FOLLOWER || state == Node.LEADER)
			{
				g.setColor(Node.COLOUR_TERMINATED);
				g.fillOval(node.coords.x-2, node.coords.y-2, Node.DIAMETER+4, Node.DIAMETER+4 );
			}
			// draw node
			Color nodeColour = (Color)Node.coloursMap.get(new Integer(state));
			g.setColor(nodeColour);
			g.fillOval(node.coords.x, node.coords.y, Node.DIAMETER, Node.DIAMETER);
			// draw node ID
			String buffer = String.valueOf(node.id);
			g.setColor(Node.ID_COLOUR);
			if (state == Node.PASSIVE || state == Node.FOLLOWER) g.setColor(Node.ID_COLOUR_ON_BLACK);
			denominator = 3;
			if (buffer.length() > 1) denominator = 6;
			g.drawString(buffer, node.coords.x+Node.DIAMETER/denominator, node.coords.y+Node.DIAMETER*3/4);
			if (node instanceof RingNodeFranklinStages) // draw message queues if it's the Franklin algorithm
				drawMessageQueues(node);
			if (nodesFinished && !node.isFinished())
				nodesFinished = false;
		}
	}

	/* Find the extent of the network to the left, right, and bottom.
	 * The network manager goes through the central collection of nodes and
	 * determines the leftmost, rightmost and lower coordinates of the network.
	 */
	protected void getMaxMin() {
		if (parent.networkManager.getNumNodes() == 0) return;
		Node firstNode = ((Node)parent.networkManager.getNodes().get(0)); // for initial values, use location of first node in main collection (do not use 0 or width of view)
		minX = firstNode.coords.x;
		maxX = firstNode.coords.x + Node.DIAMETER;
		maxY = firstNode.coords.y + Node.DIAMETER;
		Enumeration allNodes = parent.networkManager.getNodes().elements();
		Node node;
		while (allNodes.hasMoreElements()) {
			node = (Node)allNodes.nextElement();
			if (node.coords.x+Node.DIAMETER > maxX)
				maxX = node.coords.x+Node.DIAMETER;
			if (node.coords.y+Node.DIAMETER > maxY)
				maxY = node.coords.y+Node.DIAMETER;
			if (node.coords.x < minX)
				minX = node.coords.x;
		}
	}

  /* Helper function to determine the points on the circumference of
   * the two nodes the link joins, so that the link can be draw from these.
 	 * Links are not anchored at node centres because messages would disappear
   * under them before they actually finish.
   */
  protected Point[] findPointsOnCircumference(Point centre1, Point centre2) {
		double rise = centre2.y - centre1.y;
		double run = centre2.x - centre1.x;
		double theta = Math.atan(rise/run);
		double littleX = Node.RADIUS*Math.cos(theta);
		double littleY = Node.RADIUS*Math.sin(theta);
		if (run < 0) {
			littleX *= -1;
			littleY *= -1;
		}
		Point p1 = new Point((int)(centre1.x+littleX), (int)(centre1.y+littleY));
		Point p2 = new Point((int)(centre2.x-littleX), (int)(centre2.y-littleY));
		return new Point[] {p1,p2};
  }

	/* Make an image that fades through different colours.
	 * Used to create a nice background canvas for the network.
	 */
  protected void resizeBackgroundImage() {
		int w = getSize().width;
		int h = getSize().height;
		if (w * h <= 0) return; // window is too small to display drawing panel

		// Sky effect - fade from blue (top) to white (bottom)
		int colour, blue = 255;
		float red = 153, green = 204, redGap = 255-red, greenGap = 255-green, incrementR = redGap/h, incrementG = greenGap/h;
		int[] pix = new int[w * h];
		int index = 0;
		for (int x = 0; x < w; x++) {
			pix[index++] = Color.black.getRGB(); // one strip of black at the top, like a border
		}
		for (int y = 1; y < h; y++) {
			red += incrementR;
			green += incrementG;
			colour = (255 << 24) | ((int)red << 16) | ((int)green << 8) | blue; // form 32-bit colour value. (255 << 24) places 1s in the signed byte (highest byte)
			for (int x = 0; x < w; x++) {
				pix[index++] = colour;
			}
		}
		image = createImage(new MemoryImageSource(w, h, pix, 0, w));
	}

	/* Draw links and messages. Can be used in any network topology.
	 */
	protected void drawLinksAndMessages() {
		double msgSpeed, timeLeft, msg_x, msg_y, theta, length, rotator;
		Link link; Message msg;
		Enumeration messages, links = parent.networkManager.getLinks().elements();
		while (links.hasMoreElements())
		{
			link = (Link)links.nextElement();
			// draw this link
			//g.setColor(Link.LinkDefaultColor);
			g.setColor(link.getColor());
			if (link instanceof TorusLink)
				((TorusPanel)this).draw((TorusLink)link);
			else
				g.drawLine(link.getStartCoords().x,link.getStartCoords().y,link.getEndCoords().x,link.getEndCoords().y); // draw link

			// draw messages on this link
			if (NetViewer.isSynchronous())
				messages = link.getSyncMessages().elements();
			else
				messages = link.getActiveMessages().elements();
      //for (int i = 0; i < messages.size(); i++) // draw each message on the link
      while (messages.hasMoreElements()) // draw each message on the link
      {
       	//msg = (Message)messages.get(i);
       	msg = (Message)messages.nextElement();
        timeLeft = msg.getTimeLeft();
        if (timeLeft > 0.0) {
					messagesFinished = false; // animation has not finished because there is at least one message on a link
					if (link instanceof TorusLink && ((TorusLink)link).isWrapAround())
						((TorusPanel)this).drawMsgOnWrapAroundLink((TorusLink)link, msg);
					else { // normal link; draw message in normal way
						msgSpeed = msg.getSpeed();
						theta = link.theta();
						length = link.length();
						if (link.getEndCoords().x < link.getStartCoords().x)
							theta += Math.PI; // for quadrants where x < 0
						if (msg.getDirection() == Link.RIGHT) {
							msg_x = (1-timeLeft/msgSpeed)* Math.cos(theta) * length + link.getStartCoords().x;
							msg_y = (1-timeLeft/msgSpeed)* Math.sin(theta) * length + link.getStartCoords().y;
							rotator = 3*Math.PI/4; // 135 degrees
						}
						else {
							msg_x = link.getEndCoords().x - (1-timeLeft/msgSpeed)* Math.cos(theta) * length;
							msg_y = link.getEndCoords().y - (1-timeLeft/msgSpeed)* Math.sin(theta) * length;
							rotator = -Math.PI/4; // 45 degrees
						}

						// draw the message as an arrow
						g.setColor(Message.MESSAGE_COLOUR);
						((Graphics2D)g).rotate(theta+rotator, msg_x, msg_y); // theta gets g in line with the link, and the rotator places g at 45 degrees to the link
						g.fillRect((int)msg_x, (int)msg_y, 8, 2);
						g.fillRect((int)msg_x, (int)msg_y, 2, 8);
						((Graphics2D)g).rotate(-(rotator+theta), msg_x, msg_y); // back in line with link

						// draw the message string beside the message
						g.setColor(Message.MESSAGE_STRING_COLOUR);
						g.drawString(msg.getMessage(),(int)msg_x+10, (int)msg_y+10);
					}
        }
      } // end while -> draw messages
    } // end while -> draw links and messages
	} // end drawLinksAndMessages

	/* Draw message queues at each of the node's links.
	 */
	private void drawMessageQueues(Node node) {
		g.setColor(Node.MESSAGE_QUEUE_COLOUR);
		Vector v = ((RingNodeFranklinStages)node).getMessagesWaiting(Node.LEFT);
		String queue;
		if (!v.isEmpty()) {
			queue = v.toString();
			g.drawString(queue, ((RingNodeFranklinStages)node).getQueueCoordsLeft().x, ((RingNodeFranklinStages)node).getQueueCoordsLeft().y);
		}
		v = ((RingNodeFranklinStages)node).getMessagesWaiting(Node.RIGHT);
		if (!v.isEmpty()) {
			queue = v.toString();
			g.drawString(queue, ((RingNodeFranklinStages)node).getQueueCoordsRight().x, ((RingNodeFranklinStages)node).getQueueCoordsRight().y);
		}
	}

  public void paintComponent(Graphics g) {

		// check if there is a network to draw
		if (!parent.networkManager.getNetworkType().equals(NetViewer.getNetworkType()) || parent.networkManager.getNumNodes() == 0)
		{
			drawingAreaIsBlank = true;
			if (NetViewer.coolBackground)
				g.drawImage(image, 0, 0, this); // background colour effect
			else
				super.paintComponent(g); // paint normal grey background
			return;
		}

		// proceed - network exists
		drawingAreaIsBlank = false;
		nodesFinished = true; messagesFinished = true; // will be set to false if any nodes or messages are still active
		this.g = g; // save graphics object so superclass can access it

		// initialize coordinates if network is being drawn for the first time
		if (parent.networkManager.isNewNetwork()) {
			componentResized(new ComponentEvent(this,0)); // set coordinates of nodes and links
			parent.networkManager.setIsNewNetwork(false);
			return; // will be repainted from componentResized()
		}

		// draw background
		if (NetViewer.coolBackground)
			g.drawImage(image, 0, 0, this); // background colour effect
		else
			super.paintComponent(g); // paint normal grey background

		// draw the network
		drawLinksAndMessages();
		drawNodes(); // draw nodes last, on top of links and messages

		// check if algorithm is finished and stop the animation
		if (nodesFinished && messagesFinished && NetViewer.getTimer().isRunning()) {
      NetViewer.stopAnimation();
			isDirty = true;
    }
	} // end paint component

	/* Resize component. Recalculate coordinates of nodes and links.
	 */
	public void componentResized(ComponentEvent e) {
		// done differently in each subclass
	}

  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

	/* Position nodes in a circle based on the size of the panel.
	 */
	protected void positionNodesInACirlce() {
		Dimension size = getSize();
    double centre_x = size.width/2-Node.RADIUS; // shift whole drawing left by Node.RADIUS to make it look centered
    double centre_y = size.height/2-Node.RADIUS; // shift whole drawing up by Node.RADIUS to make it look centered
    double h;
    if (size.height <= size.width)
			h = size.height/2 - Node.DIAMETER; // radius of circle depends on the size of the panel and the diameter of a node
    else
			h = size.width/2 - Node.DIAMETER; // radius of circle depends on the size of the panel and the diameter of a node
    numNodes = parent.networkManager.getNumNodes();
    double linkThetaBase = 90.0 + 180.0/numNodes;
    double dTheta = 360.0/(double)numNodes;
    double thetaDeg, thetaRad, nextTheta, x, y, next_x, next_y;
    Node node;
		// set node positions
		for (int i = 0; i < numNodes; i++) {
			thetaDeg = 360.0-(double)i*dTheta;
			thetaRad = Math.toRadians(thetaDeg);
			nextTheta = Math.toRadians(360.0-(double)(i+1)*dTheta);
			x = (h*Math.cos(thetaRad)+centre_x);
			y = (h*Math.sin(thetaRad)+centre_y);
			next_x = h*Math.cos(nextTheta)+centre_x+(double)Node.RADIUS;
			next_y = h*Math.sin(nextTheta)+centre_y+(double)Node.RADIUS;
			node = parent.networkManager.getNode(i);
			node.setCoords(x,y); // SET

			// set message queue coords if it's the Franklin algorithm
			double msgQLThetaDeg, msgQRThetaDeg, msgQLThetaRad, msgQRThetaRad, msgQ_h, msgQL_x, msgQL_y, msgQR_x, msgQR_y;
			if (node instanceof RingNodeFranklinStages) {
				msgQLThetaDeg = thetaDeg+7.0;
				msgQRThetaDeg = thetaDeg-7.0;
				msgQLThetaRad = Math.toRadians(msgQLThetaDeg);
				msgQRThetaRad = Math.toRadians(msgQRThetaDeg);
				msgQ_h = h+8.0;
				msgQL_x = msgQ_h*Math.cos(msgQLThetaRad)+centre_x+Node.RADIUS;
				msgQL_y = msgQ_h*Math.sin(msgQLThetaRad)+centre_y+Node.RADIUS;
				msgQR_x = msgQ_h*Math.cos(msgQRThetaRad)+centre_x+Node.RADIUS;
				msgQR_y = msgQ_h*Math.sin(msgQRThetaRad)+centre_y+Node.RADIUS;
				((RingNodeFranklinStages)node).setQueueCoordsLeft(msgQL_x, msgQL_y); // SET
				((RingNodeFranklinStages)node).setQueueCoordsRight(msgQR_x, msgQR_y); // SET
			}
		} // end for  - store values for painting nodes

	}

	/* Set link coordinated based on the nodes they are attached to.
	 */
	protected void setLinkCoords() {
		Node n1, n2; Link link; Point centre1, centre2; Point[] points;
		Enumeration links = parent.networkManager.getLinks().elements();
		while (links.hasMoreElements())
		{
			link = (Link)links.nextElement();
			n1 = link.getNode(Link.LEFT);
			n2 = link.getNode(Link.RIGHT);
			centre1 = n1.getCentre();
			centre2 = n2.getCentre();
			points = findPointsOnCircumference(centre1, centre2);
			link.setStartCoords(points[0].x, points[0].y);
			link.setEndCoords(points[1].x, points[1].y);
		}
	}
}

