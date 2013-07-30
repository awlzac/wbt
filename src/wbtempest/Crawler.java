package wbtempest;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

public class Crawler {

	private static final int C_POSES = 3;
	private static final double SPEED = 1.3;
    static final int CHEIGHT = 10;
    private static final int CHEIGHT_H = CHEIGHT/2; // half height
    private static final int CHEIGHT_HP = (int) (CHEIGHT * 0.6);  // slightly more than half
    private static final int MAX_MISSILES = 7;
    private double vpos;
    private double pos;
    private int width;
    private int height;
    private boolean visible;
    private Image image;
    private ArrayList<Missile> missiles;
    private Level lev;
    private int pos_max;
    private boolean fireFlag = false;


    public Crawler(Level lev) {
        missiles = new ArrayList();
        visible = true;
        this.lev = lev;
        this.pos_max = lev.getColumns().size() * C_POSES -1;
    }

    public void move(int crawleroffset) {
        pos += vpos;
        if (lev.isContinuous()){
        	pos %= pos_max;
        	if (pos < 0)
        		pos = pos_max + pos;
        }
        else{
        	if (pos > pos_max)
        		pos = pos_max;
        	else if (pos < 0)
        		pos = 0;
        }
        
        if (fireFlag)
            fire(crawleroffset);
        
    }

    /** 
     * Returns the column number where the crawler currently is.
     * @return
     */
    public int getColumn(){
    	return (int)pos / C_POSES;
    }
    
    /**
     * Returns the coordinates to draw the crawler at its current position/pose.
     * 
     * Like everything in this game, the crawler is drawn based on a line
     * connecting a list of points. a few fixed positions are used.
     * 
     * @return
     */
    public List<int[]> getCoords() {
        int colnum = getColumn();
        int pose = (int)pos % C_POSES;
        int[][] coords=new int[9][3]; 
        
        Column column = lev.getColumns().get(colnum);
        int[] pt1 = column.getFrontPoint1();
        int[] pt2 = column.getFrontPoint2();
        switch (pose)
        {
        	case 0:{
                coords[0][0] = pt1[0] +(pt2[0] - pt1[0])/3;
                coords[0][1] = pt1[1] +(pt2[1] - pt1[1])/3;
                coords[0][2] = CHEIGHT_H;
                coords[2][0] = pt1[0] +(pt2[0] - pt1[0])/4;
                coords[2][1] = pt1[1] +(pt2[1] - pt1[1])/4;
                coords[2][2] = -CHEIGHT;
                coords[4][0] = pt2[0] -(pt2[0] - pt1[0])/4;
                coords[4][1] = pt2[1] -(pt2[1] - pt1[1])/4;
                coords[4][2] = CHEIGHT_HP;
                coords[6][0] = pt1[0] +(pt2[0] - pt1[0])/4;
                coords[6][1] = pt1[1] +(pt2[1] - pt1[1])/4;
                coords[6][2] = -CHEIGHT_H;
                break;
        	}
        	case 1: {
                coords[0][0] = pt1[0] +(pt2[0] - pt1[0])/3;
                coords[0][1] = pt1[1] +(pt2[1] - pt1[1])/3;
                coords[0][2] = CHEIGHT_H;
                coords[2][0] = pt1[0] +(pt2[0] - pt1[0])/2;
                coords[2][1] = pt1[1] +(pt2[1] - pt1[1])/2;
                coords[2][2] = -CHEIGHT;
                coords[4][0] = pt2[0] -(pt2[0] - pt1[0])/3;
                coords[4][1] = pt2[1] -(pt2[1] - pt1[1])/3;
                coords[4][2] = CHEIGHT_H;
                coords[6][0] = pt1[0] +(pt2[0] - pt1[0])/2;
                coords[6][1] = pt1[1] +(pt2[1] - pt1[1])/2;
                coords[6][2] = -CHEIGHT_H;
                break;
        	}
        	case 2: {
                coords[0][0] = pt1[0] +(pt2[0] - pt1[0])/4;
                coords[0][1] = pt1[1] +(pt2[1] - pt1[1])/4;
                coords[0][2] = CHEIGHT_HP;
                coords[2][0] = pt1[0] +(pt2[0] - pt1[0])*3/4;
                coords[2][1] = pt1[1] +(pt2[1] - pt1[1])*3/4;
                coords[2][2] = -CHEIGHT;
                coords[4][0] = pt2[0] -(pt2[0] - pt1[0])/3;
                coords[4][1] = pt2[1] -(pt2[1] - pt1[1])/3;
                coords[4][2] = CHEIGHT_H;
                coords[6][0] = pt1[0] +(pt2[0] - pt1[0])*3/4;
                coords[6][1] = pt1[1] +(pt2[1] - pt1[1])*2/3;
                coords[6][2] = -CHEIGHT_H;
                break;
        	}
        }
        coords[1][0] = pt1[0];
        coords[1][1] = pt1[1];
        coords[1][2]=0;
        coords[3][0] = pt2[0];
        coords[3][1] = pt2[1];
        coords[3][2] = 0;
        coords[5][0] = pt2[0] -(pt2[0] - pt1[0])/6;
        coords[5][1] = pt2[1] -(pt2[1] - pt1[1])/6;
        coords[5][2] = 0;
        coords[7][0] = pt1[0] +(pt2[0] - pt1[0])/6;
        coords[7][1] = pt1[1] +(pt2[1] - pt1[1])/6;
        coords[7][2] = 0;
        coords[8] = coords[0];
        return Arrays.asList(coords);
    }

    public Image getImage() {
        return image;
    }

    public ArrayList<Missile> getMissiles() {
        return missiles;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    private void accelRight(){
    	vpos = SPEED;
    }

    private void accelLeft(){
    	vpos = -SPEED;
    }

    public void keyPressed(KeyEvent e, int crawleroffset) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_SPACE) {
        	fireFlag = true;
        	fire(crawleroffset);
        }

        if (key == KeyEvent.VK_LEFT) {
        	accelLeft();
        }

        if (key == KeyEvent.VK_RIGHT) {
        	accelRight();
        }
    }

    /**
     * fire a missile.
     * @param zoffset - z pos of crawler
     */
    public void fire(int zoffset) {
    	if (missiles.size() < MAX_MISSILES) {
    		if (missiles.size() == 0 
    				|| missiles.get(missiles.size()-1).getZPos() > Missile.HEIGHT*3) {
        		missiles.add(new Missile(this.getColumn(), Missile.HEIGHT/3+zoffset, true));
    		}
    	}
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
            vpos = 0;
        }
        else if (key == KeyEvent.VK_RIGHT) {
            vpos = 0;
        }
        else if (key == KeyEvent.VK_SPACE) {
        	fireFlag = false;
        }

    }
}

