/*
 * NetViewer
 * Class: TorusLink extends Link
 *
 * Wraparound links are composed of two parts.
 * Two sets of coordinates must be saved, as opposed to one set in Link
 */

import java.util.Vector;
import java.awt.Point;

class TorusLink extends Link {

	private Point startCoords1, startCoords2, endCoords1, endCoords2;
	private boolean isWrapAround;

	TorusLink(Node n1, Node n2) {
		super(n1, n2);
	}

	public void setIsWrapAround(boolean tf) {
		isWrapAround = tf;
	}

	public boolean isWrapAround() {
		return isWrapAround;
	}

	/* METHODS USED FOR DRAWING THE LINK */

	/* Set start coordinates for part one of the torus wraparound link.
   */
	public void setStartCoordsA(double x, double y) {
		startCoords1 = new Point((int)x, (int)y);
	}

	/* Get start coordinates for part one of the torus wraparound link.
   */
	public Point getStartCoordsA() {
		return startCoords1;
	}

	/* Set end coordinates for part one of the torus wraparound link.
   */
	public void setEndCoordsA(double x, double y) {
		endCoords1 = new Point((int)x, (int)y);
	}

	/* Get end coordinates for part one of the torus wraparound link.
   */
	public Point getEndCoordsA() {
		return endCoords1;
	}

	/* Set start coordinates for part two of the torus wraparound link.
   */
	public void setStartCoordsB(double x, double y) {
		startCoords2 = new Point((int)x, (int)y);
	}

	/* Get start coordinates for part one of the torus wraparound link.
   */
	public Point getStartCoordsB() {
		return startCoords2;
	}

	/* Set end coordinates for part one of the torus wraparound link.
   */
	public void setEndCoordsB(double x, double y) {
		endCoords2 = new Point((int)x, (int)y);
	}

	/* Get end coordinates for part one of the torus wraparound link.
   */
	public Point getEndCoordsB() {
		return endCoords2;
	}

	/* A wraparound torus link has two parts of equal length.
   */
	public double lengthWrapAround() {
		return startCoords1.distance(endCoords1);
	}

}
