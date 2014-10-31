/*
 * NetViewer
 * Class: TorusPanel
 */

import java.util.Vector;    import java.util.Enumeration;
import java.awt.Dimension;  import java.awt.event.ComponentEvent;
import java.awt.Graphics;   import java.awt.event.ComponentListener;
import java.awt.Graphics2D; import java.awt.Color;

class TorusPanel extends DrawingPanel implements ComponentListener {

  private int rows, cols;

  TorusPanel(NetworkPanel parent)
  {
		super(parent);
    addComponentListener(this);
  }

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
  }

	/* Resize component. Recalculate coordinates of nodes and links.
	 */
  public void componentResized(ComponentEvent e) {
		resizeBackgroundImage();
		if (drawingAreaIsBlank) {
			repaint(); // background
			return; // nothing to resize
		}
    Dimension size = getSize();
    rows = parent.networkManager.getNumRows();
    cols = parent.networkManager.getNumColumns();
    double dif_x = (3*size.width)/(4*(cols-1));
    double dif_y = (3*size.height)/(4*(rows-1));
    double downLinkLength = dif_y - Node.DIAMETER;
    double acrossLinkLength = dif_x - Node.DIAMETER;
    double x = size.width/8-Node.RADIUS/2;
    double y = size.height/8-Node.RADIUS/2;
    TorusLink link;
    Node tempNode;
    int tempLength;
    double next_x, next_y, this_x, this_y, startLink_x, startLink_y, endLink_x, endLink_y;
		// set node and link coordinates in preparation for drawing
		Enumeration links = parent.networkManager.getLinks().elements();
		Enumeration nodes = parent.networkManager.getNodes().elements();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				this_x = x + j * dif_x;
				this_y = y + i * dif_y;
				((Node)nodes.nextElement()).setCoords(this_x,this_y);
				link = (TorusLink)links.nextElement(); // down link
				if (link.isWrapAround()) {
					// link part 1
					tempLength = (int)(3*(size.height-this_y-Node.DIAMETER)/4);
					startLink_x = this_x+Node.RADIUS;
					startLink_y = this_y+Node.DIAMETER;
					endLink_y = startLink_y + tempLength;
					link.setStartCoordsA(startLink_x, startLink_y);
					link.setEndCoordsA(startLink_x, endLink_y);
					// link part 2
					tempNode = parent.networkManager.getNode(0,j);
					//tempLength = tempNode.coords.y/2;
					startLink_x = tempNode.coords.x+Node.RADIUS;
					startLink_y = tempNode.coords.y-tempLength;
					endLink_y = startLink_y + tempLength;
					link.setStartCoordsB(startLink_x, startLink_y);
					link.setEndCoordsB(startLink_x, endLink_y);
				}
				else { // normal down link
					startLink_x = this_x+Node.RADIUS;
					startLink_y = this_y+Node.DIAMETER;
					endLink_y = startLink_y + downLinkLength;
					link.setStartCoords(startLink_x, startLink_y);
					link.setEndCoords(startLink_x, endLink_y);
				}
				link = (TorusLink)links.nextElement(); // across link
				if (link.isWrapAround()) {
					// link part 1
					tempLength = (int)(3*(size.width-this_x-Node.DIAMETER)/4);
					startLink_x = this_x+Node.DIAMETER;
					startLink_y = this_y+Node.RADIUS;
					endLink_x = startLink_x + tempLength;
					link.setStartCoordsA(startLink_x, startLink_y);
					link.setEndCoordsA(endLink_x, startLink_y);
					// link part 2
					tempNode = parent.networkManager.getNode(i,0);
					startLink_x = tempNode.coords.x-tempLength;
					startLink_y = tempNode.coords.y+Node.RADIUS;
					endLink_x = tempNode.coords.x;
					link.setStartCoordsB(startLink_x, startLink_y);
					link.setEndCoordsB(endLink_x, startLink_y);
				}
				else { // normal across link
					startLink_x = this_x+Node.DIAMETER;
					startLink_y = this_y+Node.RADIUS;
					endLink_x = startLink_x + acrossLinkLength;
					link.setStartCoords(startLink_x, startLink_y);
					link.setEndCoords(endLink_x, startLink_y);
				}
			}
		}
		repaint();
  }

  public void componentMoved(ComponentEvent e) {}
  public void componentShown(ComponentEvent e) {}
  public void componentHidden(ComponentEvent e) {}

	/* Draw a link.
	 * May have to draw two parts if it's a wrap-around link.
	 */
	protected void draw(TorusLink link) {
		g.setColor(Color.black);
		if (link.isWrapAround())
		{
			g.drawLine(link.getStartCoordsA().x, link.getStartCoordsA().y, link.getEndCoordsA().x, link.getEndCoordsA().y); // 1st part of link
			g.drawLine(link.getStartCoordsB().x, link.getStartCoordsB().y, link.getEndCoordsB().x, link.getEndCoordsB().y); // 2nd part of link
		}
		else {
			g.drawLine(link.getStartCoords().x, link.getStartCoords().y, link.getEndCoords().x, link.getEndCoords().y);
		}
	}

	/* Draw a message on a wrap-around link.
	 * Figure out which half of the link it is on and how far along.
	 */
	protected void drawMsgOnWrapAroundLink(TorusLink link, Message msg) {
		double msg_x, msg_y, rotator;
		double msgSpeed = msg.getSpeed();
		double timeLeft = msg.getTimeLeft();
		double percentDone = 1.0-timeLeft/msgSpeed;
		if (link.getStartCoordsA().x == link.getEndCoordsA().x) // up/down link
		{
			if (msg.getDirection() == Link.RIGHT) { // down
				if (percentDone <= 0.5) // msg is on the 1st part of the wrap around link going down
					msg_y = link.getStartCoordsA().y + 2*percentDone*link.lengthWrapAround();
				else // msg is on the 2nd part of the wrap around link going down
					msg_y = link.getStartCoordsB().y + 2*(percentDone-0.5)*link.lengthWrapAround();
				rotator = 3*Math.PI/4; // 135 degrees
			}
			else { // up
				if (percentDone <= 0.5) // msg is on the 2nd part of the wrap around link going up
					msg_y = link.getEndCoordsB().y - 2*percentDone*link.lengthWrapAround();
				else // msg is on the 1st part of the wrap around link going up
					msg_y = link.getEndCoordsA().y - 2*(percentDone-0.5)*link.lengthWrapAround();
				rotator = -Math.PI/4; // 45 degrees
			}

			msg_x = link.getStartCoordsA().x;

			// draw the message as an arrow
			g.setColor(Message.MESSAGE_COLOUR);
			((Graphics2D)g).rotate(Math.PI/2+rotator, msg_x, msg_y); // theta gets g in line with the link, and the rotator places g at 45 degrees to the link
			g.fillRect((int)msg_x, (int)msg_y, 8, 2);
			g.fillRect((int)msg_x, (int)msg_y, 2, 8);
			((Graphics2D)g).rotate(-(rotator+Math.PI/2), msg_x, msg_y); // back to original position

			// draw the message string beside the message
			g.setColor(Message.MESSAGE_STRING_COLOUR);
			g.drawString(msg.getMessage(), (int)msg_x+10, (int)msg_y+10);
		}
		else // left/right link
		{
			if (msg.getDirection() == Link.RIGHT) {
				if (percentDone <= 0.5) // msg is on the 1st part of the wrap around link going right
					msg_x = link.getStartCoordsA().x + 2*percentDone*link.lengthWrapAround();
				else // msg is on the 2nd part of the wrap around link going right
					msg_x = link.getStartCoordsB().x + 2*(percentDone-0.5)*link.lengthWrapAround();
				rotator = 3*Math.PI/4; // 135 degrees
			}
			else { // left
				if (percentDone <= 0.5) // msg is on the 2nd part of the wrap around link going left
					msg_x = link.getEndCoordsB().x - 2*percentDone*link.lengthWrapAround();
				else // msg is on the 1st part of the wrap around link going left
					msg_x = link.getEndCoordsA().x - 2*(percentDone-0.5)*link.lengthWrapAround();
				rotator = -Math.PI/4; // 45 degrees
			}

			msg_y = link.getStartCoordsA().y;

			// draw the message as an arrow
			g.setColor(Message.MESSAGE_COLOUR);
			((Graphics2D)g).rotate(0+rotator, msg_x, msg_y); // theta gets g in line with the link, and the rotator places g at 45 degrees to the link
			g.fillRect((int)msg_x, (int)msg_y, 8, 2);
			g.fillRect((int)msg_x, (int)msg_y, 2, 8);
			((Graphics2D)g).rotate(-(rotator+0), msg_x, msg_y); // back to original position

			// draw the message string beside the message
			g.setColor(Message.MESSAGE_STRING_COLOUR);
			g.drawString(msg.getMessage(), (int)msg_x+10, link.getStartCoordsA().y+10);
		}
	}

}
