package wbtempest;

import java.util.Arrays;
import java.util.List;

/**
 * A missile.  If being fired down into the level, it represents a player missile;
 * if fired up (at the player), it represents an enemy missile.
 * 
 * @author ugliest
 *
 */
public class Missile {
	private static int BASE_SPEED = 8;
	static int HEIGHT = 8;
	private static int HEIGHT_H = HEIGHT/2;
	private int colnum;
	private int zpos;
	private boolean visible = true;
	private int speed;
	
	public Missile(int colnum, int zpos, boolean down){
		this.colnum = colnum;
		this.zpos = zpos;
    	if (down)
    		speed = BASE_SPEED;
    	else
    		speed = -BASE_SPEED/2;
	}
	
	public int getZPos(){
		return zpos;
	}
	
	public int getColumn(){
		return colnum;
	}
	
	public void move(int maxz){
		zpos+=speed;
		if ((zpos > maxz) || (zpos < 0))
			visible = false;
	}
	
	public boolean isVisible(){
		return visible;
	}
	
	/**
	 * return the points that make up the onscreen missile.
	 * 
	 * @param lev
	 * @return
	 */
	public List<int[]> getCoords(Level lev){
		int[][] coords = new int[5][3];
		Column c = lev.getColumns().get(colnum);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();
		coords[0][0] = p1[0]+(p2[0] - p1[0])*2/5;
		coords[0][1] = p1[1]+(p2[1] - p1[1])*2/5;
		coords[0][2] = zpos-HEIGHT_H;
		coords[1][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[1][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[1][2] = zpos-HEIGHT;
		coords[2][0] = p1[0]+(p2[0] - p1[0])*3/5;
		coords[2][1] = p1[1]+(p2[1] - p1[1])*3/5;
		coords[2][2] = zpos-HEIGHT_H;
		coords[3][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[3][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[3][2] = zpos;
		coords[4] = coords[0];
		
		return Arrays.asList(coords);
	}

	/**
	 * coordinates for the second layer to be drawn; idea is to allow board to draw in 
	 * a different color.
	 * @param lev
	 * @return
	 */
	public List<int[]> getLayerCoords(Level lev){
		int[][] coords = new int[5][3];
		Column c = lev.getColumns().get(colnum);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();
		coords[0][0] = p1[0]+(p2[0] - p1[0])*9/20;
		coords[0][1] = p1[1]+(p2[1] - p1[1])*9/20;
		coords[0][2] = zpos-HEIGHT_H;
		coords[1][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[1][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[1][2] = zpos-HEIGHT*3/5;
		coords[2][0] = p1[0]+(p2[0] - p1[0])*11/20;
		coords[2][1] = p1[1]+(p2[1] - p1[1])*11/20;
		coords[2][2] = zpos-HEIGHT_H;
		coords[3][0] = p1[0]+(p2[0] - p1[0])/2;
		coords[3][1] = p1[1]+(p2[1] - p1[1])/2;
		coords[3][2] = zpos-HEIGHT*2/5;
		coords[4] = coords[0];
		
		return Arrays.asList(coords);
	}

	public void setVisible(boolean b) {
		this.visible = b;
	}
}
