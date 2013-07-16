package wbtempest;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Ex {

    static int HEIGHT = 20;
    static int SCOREVAL=150;
    private static int EXHEIGHT_H = HEIGHT/2; // half the height of an Ex
    private static Random rnd = new Random(new java.util.Date().getTime());
    
    // possible states of an ex
	private static enum State {STRAIGHT, JUMPRIGHT1, JUMPLEFT1, JUMPRIGHT2, JUMPLEFT2, LANDRIGHT1, LANDRIGHT2, LANDLEFT1, LANDLEFT2};

	private static int JUMPINTERVAL = 40;  // ticks
	private static int JUMPINTERVAL_INITIAL = 2;  // ticks
	private static double SPEED = 1.4;
    private int col;
    private int ncols;
    private double z = Board.LEVEL_DEPTH;
    private boolean visible;
    private int jumptimer = 0;
    private State prefdir = State.JUMPRIGHT1;  // initial predisp to jump right
    private State s = prefdir;
    private boolean canMove=false;
    private boolean levContinuous;
    private boolean spawning = true;
 
    /**
     * Initialize an Ex
     * @param col - initial column of ex
     * @param ncols - nulber of cols the current level contains
     * @param canMove - can the ex move col to col?
     * @param levelContinuous - is the level continuous?
     */
    public Ex(int col, int ncols, boolean canMove, boolean levelContinuous) {
        this.col = col;
        this.ncols = ncols;
        this.canMove = canMove;
        this.levContinuous = levelContinuous;
        visible = true;
    }
    
    public void resetZ(int zdepth) {
    	z = zdepth;
    }

    public void move(int xbound, int crawlerCol) {
        if (z > 0) 
            z -= SPEED;
        if (z > Board.LEVEL_DEPTH)
        	z -= SPEED/2; // go faster when theyre just spinning aimlessly before hitting the board.
        
        switch (s) {
        case STRAIGHT:
        	if (canMove || z <= 0 || z > Board.LEVEL_DEPTH){
        		// if exes are allowed to move column to column, or if we're already at 
        		// the front of the board, move every now and then.
        		jumptimer--;
        		if (jumptimer <= 0) {
        				if (z < Board.LEVEL_DEPTH && spawning)
        				{ // haven't chosen a direction yet
        					spawning = false;
        					prefdir = State.values()[rnd.nextInt(2)+1]; // right or left at random, note depends on order of values in state enum, which is tacky
        				}
        				if (z <= 0 && (rnd.nextInt(4) == 1)) {
        					// at top, randomly reorient and make sure we're evilly headed toward the player
        					if (levContinuous) {
        						int diff = crawlerCol - getColumn();
        						boolean wrap = Math.abs(diff) > ncols/2;
        						if ((diff > 0 && wrap) // ex is low and we need to wrap
        								|| (diff < 0 && !wrap)) // ex is high and we don't
        							prefdir = State.JUMPLEFT1;
        						else
        							prefdir = State.JUMPRIGHT1;
        					}
        					else{
        						if (crawlerCol > getColumn())
        							prefdir = State.JUMPRIGHT1;
        						else
        							prefdir = State.JUMPLEFT1;
        					}
        				}
    					s = prefdir;
        		}
        	}
        	break;
        case JUMPRIGHT2:
        	s = State.LANDLEFT1;
        	col++;
        	if (col >= ncols){
        		if (levContinuous){
                  	col %= ncols;
        		}
        		else {
        			col--;
        			prefdir = State.JUMPLEFT1;
        		}
        	}
        		
        	break;

        case JUMPLEFT2:
        	s = State.LANDRIGHT1;
        	col--;
           	if (col < 0){
        		if (levContinuous)
            		col = ncols + col;
        		else {
        			col++;
        			prefdir = State.JUMPRIGHT1;
        		}
           	}	
        	break;
        	
        case JUMPRIGHT1:
        	s = State.JUMPRIGHT2;
        	break;

        case JUMPLEFT1:
        	s = State.JUMPLEFT2;
        	break;

        case LANDRIGHT1:
        	s = State.LANDRIGHT2;
        	break;

        case LANDLEFT1:
        	s = State.LANDLEFT2;
        	break;

        case LANDRIGHT2:
        case LANDLEFT2:
        	if (z < Board.LEVEL_DEPTH) {
            	jumptimer = JUMPINTERVAL;
        	}
        	else
        	{
        		jumptimer = JUMPINTERVAL_INITIAL;
        	}
        	s = State.STRAIGHT;
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public int getColumn() {
//    	System.out.println("Ex.GetColumn returning "+col);
        return col;
    }
    
    public int getZ(){
//    	System.out.println("Ex.GetZ "+z);
    	return (int)z;
    }
    
    public List<int[]> getDeathCoords(Level lev){
    	// return death explosion image coords
    	int[][] coords = new int[15][3];
    	Column c = lev.getColumns().get(col);
    	int[] p1 = c.getFrontPoint1();
    	int[] p2 = c.getFrontPoint2();
    	coords[0][0] = p1[0] + (p2[0]-p1[0])/5;
    	coords[0][1] = p1[1] + (p2[0]-p1[0])/5;
    	coords[0][2] = (int) z;
    	coords[1][0] = p2[0] - (p2[0]-p1[0])/2;  // center point
    	coords[1][1] = p2[1] - (p2[1]-p1[1])/2;
    	coords[1][2] = (int) z;
    	coords[2][0] = p1[0] + (p2[0]-p1[0])/3;
    	coords[2][1] = p1[1] + (p2[1]-p1[1])/3;
    	coords[2][2] = (int) (z + EXHEIGHT_H*2);
    	coords[3] = coords[1];
    	coords[4][0] = p2[0] - (p2[0]-p1[0])/2;
    	coords[4][1] = p2[1] - (p2[1]-p1[1])/2;
    	coords[4][2] = (int) (z + EXHEIGHT_H*4);
    	coords[5] = coords[1];
    	coords[6][0] = p2[0] - (p2[0]-p1[0])/3;
    	coords[6][1] = p2[1] - (p2[1]-p1[1])/3;
    	coords[6][2] = (int) (z+EXHEIGHT_H*2);
    	coords[7] = coords[1];
    	coords[8][0] = p2[0] - (p2[0]-p1[0])/5;
    	coords[8][1] = p2[1] - (p2[1]-p1[1])/5;
    	coords[8][2] = (int) z;
    	coords[9] = coords[1];
    	coords[10][0] = p2[0] - (p2[0]-p1[0])/3;
    	coords[10][1] = p2[1] - (p2[1]-p1[1])/3;
    	coords[10][2] = (int) (z-EXHEIGHT_H*2);
    	coords[11] = coords[1];
    	coords[12][0] = p2[0] - (p2[0]-p1[0])/2;
    	coords[12][1] = p2[1] - (p2[1]-p1[1])/2;
    	coords[12][2] = (int) (z - EXHEIGHT_H*4);
    	coords[13] = coords[1];
    	coords[14][0] = p1[0] + (p2[0]-p1[0])/3;
    	coords[14][1] = p1[1] + (p2[1]-p1[1])/3;
    	coords[14][2] = (int) (z - EXHEIGHT_H*2);
    	return Arrays.asList(coords);
    }

    /**
     * If the screen needs to freeze (eg player death) the ex can ve reset to 
     * its normal state via this method.
     */
    public void resetState() {
    	s = State.STRAIGHT;
    }
    
    /**
     * return coords for drawing this ex, based on its state.
     * 
     * @param lev the current level; column information is needed.
     * @return
     */
	public List<int[]> getCoords(Level lev){
		int[][] coords = new int[7][3];
		Column c = lev.getColumns().get(col);
		int[] p1 = c.getFrontPoint1();
		int[] p2 = c.getFrontPoint2();

		switch (s)
		{
			case STRAIGHT:
				coords[0][0] = p1[0];
				coords[0][1] = p1[1];
				coords[0][2] = (int) (z-EXHEIGHT_H);
				coords[1][0] = p2[0];
				coords[1][1] = p2[1];
				coords[1][2] = (int) (z+EXHEIGHT_H);
				coords[2][0] = p2[0] - (p2[0]-p1[0])/3;
				coords[2][1] = p2[1] - (p2[1]-p1[1])/3;
				coords[2][2] = (int) z;
				coords[3][0] = p2[0];
				coords[3][1] = p2[1];
				coords[3][2] = (int) (z-EXHEIGHT_H);
				coords[4][0] = p1[0];
				coords[4][1] = p1[1];
				coords[4][2] = (int) (z+EXHEIGHT_H);
				coords[5][0] = p1[0] + (p2[0]-p1[0])/3;
				coords[5][1] = p1[1] + (p2[1]-p1[1])/3;
				coords[5][2] = (int) z;
				coords[6][0] = p1[0];
				coords[6][1] = p1[1];
				coords[6][2] = (int) (z-EXHEIGHT_H);
				break;

			case JUMPRIGHT1:
			case LANDRIGHT2:
				coords[0][0] = p1[0] + (p2[0]-p1[0])/4;
				coords[0][1] = p1[1] + (p2[1]-p1[1])/4;
				coords[0][2] = (int) (z+EXHEIGHT_H*2);
				coords[1][0] = p2[0] + (p2[0]-p1[0])/4;
				coords[1][1] = p2[1] + (p2[1]-p1[1])/4;
				coords[1][2] = (int) (z+EXHEIGHT_H*2);
				coords[2][0] = p2[0] - (p2[0]-p1[0])/11;
				coords[2][1] = p2[1] - (p2[1]-p1[1])/11;
				coords[2][2] = (int) (z+EXHEIGHT_H*1.8);
				coords[3][0] = p2[0];
				coords[3][1] = p2[1];
				coords[3][2] = (int) z;
				coords[4][0] = (int) (p1[0] + (p2[0]-p1[0])/2);
				coords[4][1] = (int) (p1[1] + (p2[1]-p1[1])/2);
				coords[4][2] = (int) (z+ EXHEIGHT_H*5);
				coords[5][0] = (int) (p2[0] - (p2[0]-p1[0])/2.5);
				coords[5][1] = (int) (p2[1] - (p2[1]-p1[1])/2.5);
				coords[5][2] = (int) (z+EXHEIGHT_H *2.6);
				coords[6] = coords[0];
				break;

			case JUMPLEFT1:
			case LANDLEFT2:
				coords[0][0] = p1[0];
				coords[0][1] = p1[1];
				coords[0][2] = (int) z;
				coords[1][0] = (int) (p1[0] + (p2[0]-p1[0])/2);
				coords[1][1] = (int) (p1[1] + (p2[1]-p1[1])/2);
				coords[1][2] = (int) (z+ EXHEIGHT_H*5);
				coords[2][0] = (int) (p1[0] + (p2[0]-p1[0])/2.5);
				coords[2][1] = (int) (p1[1] + (p2[1]-p1[1])/2.5);
				coords[2][2] = (int) (z+EXHEIGHT_H *2.6);
				coords[3][0] = p2[0] - (p2[0]-p1[0])/4;
				coords[3][1] = p2[1] - (p2[1]-p1[1])/4;
				coords[3][2] = (int) (z+EXHEIGHT_H*2);
				coords[4][0] = p1[0] - (p2[0]-p1[0])/4;
				coords[4][1] = p1[1] - (p2[1]-p1[1])/4;
				coords[4][2] = (int) (z+EXHEIGHT_H*2);
				coords[5][0] = p1[0] + (p2[0]-p1[0])/11;
				coords[5][1] = p1[1] + (p2[1]-p1[1])/11;
				coords[5][2] = (int) (z+EXHEIGHT_H*1.8);
				coords[6] = coords[0];
				break;

			case JUMPLEFT2:
			case LANDLEFT1:
				coords[0][0] = p1[0];
				coords[0][1] = p1[1];
				coords[0][2] = (int) z;
				coords[1][0] = (int) (p1[0] + (p2[0]-p1[0])/4.5);
				coords[1][1] = (int) (p1[1] + (p2[1]-p1[1])/4.5);
				coords[1][2] = (int) (z+ EXHEIGHT_H*8);
				coords[2][0] = (int) (p1[0] + (p2[0]-p1[0])/4.5);
				coords[2][1] = (int) (p1[1] + (p2[1]-p1[1])/4.5);
				coords[2][2] = (int) (z+EXHEIGHT_H *4);
				coords[3][0] = p2[0] - (p2[0]-p1[0])/2;
				coords[3][1] = p2[1] - (p2[1]-p1[1])/2;
				coords[3][2] = (int) (z+EXHEIGHT_H*4);
				coords[4][0] = (int) (p1[0] - (p2[0]-p1[0])/3.5);
				coords[4][1] = (int) (p1[1] - (p2[1]-p1[1])/3.5);
				coords[4][2] = (int) (z+EXHEIGHT_H*1.8);
				coords[5][0] = p1[0];// - (p2[0]-p1[0])/15;
				coords[5][1] = p1[1];// - (p2[1]-p1[1])/15;
				coords[5][2] = (int) (z+EXHEIGHT_H*1.8);
				coords[6] = coords[0];
				break;

			case JUMPRIGHT2:
			case LANDRIGHT1:
				coords[0][0] = p2[0] - (p2[0]-p1[0])/2;
				coords[0][1] = p2[1] - (p2[1]-p1[1])/2;
				coords[0][2] = (int) (z+EXHEIGHT_H*4);
				coords[1][0] = (int) (p2[0] + (p2[0]-p1[0])/3.5);
				coords[1][1] = (int) (p2[1] + (p2[1]-p1[1])/3.5);
				coords[1][2] = (int) (z+EXHEIGHT_H*1.8);
				coords[2][0] = p2[0];// - (p2[0]-p1[0])/15;
				coords[2][1] = p2[1];// - (p2[1]-p1[1])/15;
				coords[2][2] = (int) (z+EXHEIGHT_H*1.8);
				coords[3][0] = p2[0];
				coords[3][1] = p2[1];
				coords[3][2] = (int) z;
				coords[4][0] = (int) (p2[0] - (p2[0]-p1[0])/4.5);
				coords[4][1] = (int) (p2[1] - (p2[1]-p1[1])/4.5);
				coords[4][2] = (int) (z+ EXHEIGHT_H*8);
				coords[5][0] = (int) (p2[0] - (p2[0]-p1[0])/4.5);
				coords[5][1] = (int) (p2[1] - (p2[1]-p1[1])/4.5);
				coords[5][2] = (int) (z+EXHEIGHT_H *4);
				coords[6] = coords[0];
				break;
		}
		
		return Arrays.asList(coords);
	}
}

