package wbtempest;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the details of the level being played.
 * 
 * @author ugliest
 *
 */
public class Level {
	private static int BASE_EX_FIRE_BPS = 35;  // bps chance per tick that we fire an ex at player
	
	private int levnum;
	private int exesct;
	private float spikespct;
	private List<Column> columns;
	private boolean continuous = false;
	private boolean exesCanMove = false;
	private int zpull_x;  // z pull is the point that the z-axis leads to for this level
	private int zpull_y;
	private int numscreens = 6;
	
	public Level(int levnum, int b_width, int b_height){
		int ncols;
		int colsPerSide;
		this.levnum = levnum;
		
		int cx = b_width/2;  // center for drawing; not same as where z-axis goes to.
		int cy = b_height * 31/60;

		zpull_x = b_width/2;  // where z-axis goes; default z pull to be just low of center.
		zpull_y = b_height *35/60;
		
		boolean firsttime = true;
		int x, y, oldx=0, oldy=0;
		exesct = 5 + levnum*2; 
		exesCanMove = (levnum != 1);
		if (levnum < 4)
			spikespct = (float)0;
		else if (levnum < 6)
			spikespct = (float) 0.5;
		else if (levnum < 9)
			spikespct = (float) 0.75;
		else spikespct = (float) 1;
		float rad_dist;
		float step;
		int radius = 250; // consistent-ish radius for levels that have one
		columns = new ArrayList<Column>();

		// if we run out of screens....cycle
		int screennum = (levnum-1) % numscreens;
		//screennum=5;

		switch (screennum) {
		case 0:	// circle
				ncols = 16;
				continuous = true;
				rad_dist = (float) (3.1415927 * 2);
				step = rad_dist/(ncols);
				for (float rads=0; rads < rad_dist+step/2; rads+=step)
				{
					x = cx - (int)(Math.sin(rads) * radius * .95);
					y = cy - (int)(Math.cos(rads) * radius);
					if (firsttime){
						firsttime = false;
					}
					else {
						Column col = new Column(oldx, oldy, x, y);
						columns.add(col);
					}
					oldx = x;
					oldy = y;
				}
				break;
				
			case 1: // square
				continuous = true;
				ncols = 16;
				colsPerSide = ncols/4;
				// left
				for (x = cx - radius, y = cy-radius; y < cy+radius; y+=(radius*2/colsPerSide)){
					if (firsttime){
						firsttime = false;
					}
					else {
						Column col = new Column(oldx, oldy, x, y);
						columns.add(col);
					}
					oldx = x;
					oldy = y;
				}
				// bottom
				for (x = cx - radius, y = cy+radius; x < cx+radius; x+=(radius*2/colsPerSide)){
					Column col = new Column(oldx, oldy, x, y);
					columns.add(col);
					oldx = x;
					oldy = y;
				}
				// right
				for (x = cx + radius, y = cy+radius; y > cy-radius; y-=(radius*2/colsPerSide)){
					Column col = new Column(oldx, oldy, x, y);
					columns.add(col);
					oldx = x;
					oldy = y;
				}
				// top
				for (x = cx + radius, y = cy-radius; x >= cx-radius; x-=(radius*2/colsPerSide)){
					Column col = new Column(oldx, oldy, x, y);
					columns.add(col);
					oldx = x;
					oldy = y;
				}
				break;
				
			case 2: // triangle
				continuous = true;
				radius = 320;
				ncols = 15;
				colsPerSide = ncols/3;
				cy = b_height*3/5;
				// left
				for (x = cx, y = cy-radius; y < cy+radius*3/4; y+=(radius*3/2/colsPerSide),x-=radius*2/3/colsPerSide){
					if (firsttime){
						firsttime = false;
					}
					else {
						Column col = new Column(oldx, oldy, x, y);
						columns.add(col);
					}
					oldx = x;
					oldy = y;
				}
				// bottom
				firsttime = true;
				int targx = cx + (cx-oldx);
				for (x = oldx, y = oldy; x < targx; x+=(radius*4/3/colsPerSide)){
					if (firsttime){
						firsttime = false;
					}
					else {
						Column col = new Column(oldx, oldy, x, y);
						columns.add(col);
					}
					oldx = x;
					oldy = y;
				}
				// right
				for (; y >= cy-radius; y-=(radius*3/2/colsPerSide),x-=radius*2/3/colsPerSide+1){
					Column col = new Column(oldx, oldy, x, y);
					columns.add(col);
					oldx = x;
					oldy = y;
				}
				break;
				
			case 3: // straight, angled V
				ncols = 16;
				zpull_x = b_width/2; 
				zpull_y = b_height /4;
				for (x = cx/2, y=cy/3; x < cx; x+= cx/2/(ncols/2), y+=(cy*3/2)/(ncols/2)){
					if (firsttime){
						firsttime = false;
					}
					else {
						Column col = new Column(oldx, oldy, x, y);
						columns.add(col);
					}
					oldx = x;
					oldy = y;
				}
				for (; x <= cx*3/2; x+= cx/2/(ncols/2), y-=(cy*3/2)/(ncols/2)){
					Column col = new Column(oldx, oldy, x, y);
					columns.add(col);
					oldx = x;
					oldy = y;
				}
				break;

			case 4: // straight line
				ncols = 14;
				zpull_x = b_width/2; 
				zpull_y = b_height /4;
				y = b_height * 5/7;
				for (x = b_width *1/(ncols+2); x < b_width * (1+ncols)/(ncols+2); x+= b_width/(ncols+2)){
					if (firsttime){
						firsttime = false;
					}
					else {
						Column col = new Column(oldx, oldy, x, y);
						columns.add(col);
					}
					oldx = x;
					oldy = y;

				}
				break;

			case 5: // jagged V
				zpull_x = b_width/2; 
				zpull_y = b_height /5;
				int ycolwidth = 80;
				int xcolwidth = ycolwidth *4/5;
				int ystart;
				x = oldx = cx - (int)(xcolwidth*3.5);
				y = oldy = ystart = cy - ycolwidth;
				y+=ycolwidth;
				columns.add(new Column(oldx, oldy, x, y));
				oldy = y;
				while (y < ystart + ycolwidth*4){
					x+=xcolwidth;
					columns.add(new Column(oldx, oldy, x, y));
					oldx=x;
					y+=ycolwidth;
					columns.add(new Column(oldx, oldy, x, y));
					oldy=y;
				}
				while (y > ystart){
					x+=xcolwidth;
					columns.add(new Column(oldx, oldy, x, y));
					oldx=x;
					y-=ycolwidth;
					columns.add(new Column(oldx, oldy, x, y));
					oldy=y;
				}
				
				
		}
	}
	
	public List<Column> getColumns(){
		return columns;
	}
	
	public Color getLevelColor(){
		if (levnum > numscreens*2)
			return Color.RED;
		return Color.BLUE;
	}
	
	public int getZPull_X() {
		return zpull_x;
	}

	public int getZPull_Y() {
		return zpull_y;
	}
	
	public boolean isContinuous(){
		return continuous;
	}
	
	public boolean exesCanMove(){
		return exesCanMove;
	}
	
	public int getNumExes(){
		return this.exesct;
	}
	
	public int getExFireBPS(){
		return BASE_EX_FIRE_BPS + levnum/2;
	}
	
	public int getNumSpikes(){
		return (int) (spikespct * columns.size());
	}
	
	public List<int[]> getBoardFrontCoords(){
		List<int[]> coordList = new ArrayList<int[]>();
		for (int i=0; i<columns.size(); i++){
			Column col = columns.get(i);
			if (i==0)
				coordList.add(col.getFrontPoint1());
			coordList.add(col.getFrontPoint2());
		}
		return coordList;
	}

}
