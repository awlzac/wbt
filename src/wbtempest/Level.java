package wbtempest;

import java.util.ArrayList;
import java.util.List;

public class Level {
	private static int BASE_EX_FIRE_BPS = 11;
	
	private int levnum;
	private int b_width;
	private int b_height;
	private int cx;
	private int cy;
	private int exesct;
	private float spikespct;
	private List<Column> columns;
	private boolean continuous = false;
	private boolean exesCanMove = false;
	
	public Level(int levnum, int b_width, int b_height){
		this.levnum = levnum;
		this.b_height = b_height;
		this.b_width = b_width;
		cx = b_width/2;
		cy = b_height * 19/40;
		
		boolean firsttime = true;
		int oldx=0, oldy=0;
		exesct = 6 + levnum*2; 
		exesCanMove = (levnum != 1);
		if (levnum < 3)
			spikespct = 0;
		else if (levnum < 6)
			spikespct = (float) 0.3;
		else if (levnum < 6)
			spikespct = (float) 0.6;
		else spikespct = (float) 0.8;
		int numscreens = 2;
		int screennum = (levnum -2) % numscreens;
		if (screennum <0)
		  screennum = 0;

		switch (screennum) {
		case 0:
				// circle
				continuous = true;
				columns = new ArrayList<Column>();
				int ncols = 16;
				float rad_dist = (float) (3.1415927 * 2);
				float step = rad_dist/(ncols);
				int radius = 250;
				for (float rads=0; rads < rad_dist+step/2; rads+=step)
				{
					int x = cx - (int)(Math.sin(rads) * radius * .85);
					int y = cy - (int)(Math.cos(rads) * radius);
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
			case 1: // straight line
				ncols = 16;
				columns = new ArrayList<Column>();
				int y = b_height * 6/7;
				for (int x = b_width *1/(ncols+2); x < b_width * (1+ncols)/(ncols+2); x+= b_width/(ncols+2)){
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
		}
	}
	
	public List<Column> getColumns(){
		return columns;
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
		return (int) spikespct * columns.size();
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
