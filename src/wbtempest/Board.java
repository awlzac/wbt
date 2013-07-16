package wbtempest;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

import java.util.*;

/**
 * Main object, handles board, layout, update loop, and repainting.
 * @author ugliest
 *
 */
public class Board extends JPanel implements ActionListener {

	private static final long serialVersionUID = -1467405293079888602L;
    private static Random r = new Random(new java.util.Date().getTime());
    static int B_WIDTH = 800;
    static int B_HEIGHT = 600;
    static int LEVEL_DEPTH = 600;
    static int START_LIVES = 3;
	private static double ZSTRETCH = 125; // lower = more stretched on z axis
	private static int SPEED_LEV_ADVANCE = 7;  // speed at which we to the next board after clearing a level
	private static int GAME_OVER_BOARDSPEED = 40;  // speed at which the game recedes when player loses
	private static int DEATH_PAUSE_TICKS = 80;  // ticks to pause on crawler death

	private Timer timer;
    private Crawler crawler;
    private ArrayList<Ex> exes;
    private ArrayList<Missile> exmissiles;
    private ArrayList<Spike> spikes;
    private boolean clearboard=false;
    private int levelnum = 1;
    Level levelinfo;
    private boolean pause = false;
    private boolean levelcleared = false;
    private boolean levelprep = false;  // not dealt with by initLevel() - have to set it explicitly to handle level advance visual
    private boolean gameover = false;
    private int lives;
    private int score;
    private int boardpov;
    private int crawlerzoffset;
    private int dptLeft;
    private boolean crawlerSpiked;
    public Board() {
        addKeyListener(new TAdapter());
        setFocusable(true);
        setBackground(Color.BLACK);
        setDoubleBuffered(true);

        setSize(B_WIDTH, B_HEIGHT);
        timer = new Timer(15, this);
        timer.start();
        startGame();
    }
    
    private void startGame()
    {
    	lives=START_LIVES;
    	gameover=false;
    	score = 0;
    	levelnum = 1;
        exes = new ArrayList<Ex>();
        exmissiles = new ArrayList<Missile>();
        spikes = new ArrayList<Spike>();
    	initLevel();
    }
    
    /**
     * initialize a level for play
     */
    private void initLevel(){
    	levelinfo = new Level (levelnum, B_WIDTH, B_HEIGHT);
        crawler = new Crawler(levelinfo);
        exes.clear();
        for (int i=0; i<levelinfo.getNumExes(); i++ ) {
            exes.add(new Ex(r.nextInt(levelinfo.getColumns().size()), 
            		levelinfo.getColumns().size(), 
       				levelinfo.exesCanMove(),
       				levelinfo.isContinuous() ));
        }
        spikes.clear();
        for (int i=0; i<levelinfo.getNumSpikes(); i++) {
        	spikes.add(new Spike(r.nextInt(levelinfo.getColumns().size())));
        }
        
        //also do whatever we need when rplaying a level
        replayLevel();
    }

    /**
     * Reattempt current level.  retains state of level after player death.
     */
    public void replayLevel() {
        levelcleared = false;
        clearboard = false;
        boardpov = crawlerzoffset = 0;
        dptLeft = 0;
        crawlerSpiked = false;
        exmissiles.clear();
        if (exes.size() == 0) {
        	// need at least one ex
            exes.add(new Ex(r.nextInt(levelinfo.getColumns().size()), 
            		levelinfo.getColumns().size(), 
       				levelinfo.exesCanMove(),
       				levelinfo.isContinuous() ));
            exes.get(0).resetZ(LEVEL_DEPTH *5/4);
        }
        else
          for (Ex ex : exes )
            ex.resetZ(r.nextInt(LEVEL_DEPTH*2 + LEVEL_DEPTH *levelinfo.getNumExes()/5) + LEVEL_DEPTH*5/4);
    }
    
    public void addNotify() {
        super.addNotify();
    }

    /**
     * This is how we achieve the 3D effect.  The z-axis is assumed to
     * point "into" the screen, away from the player.  interpolation is
     * done toward this center to simulate a z-axis -- but it can't be
     * done linearly, or depth perception is ruined.  this function 
     * creates a "z factor", based on the
     * z position, which asymptotically approaches but never hits the
     * board center (which represents z-infinity). this z-factor can then 
     * be used by the caller as a percentage of the way to the center.
     * 
     * @param z the inpassed z position
     * @return
     */
    private static double getZFact(int z) {
      return 1.0-(ZSTRETCH/(z+ZSTRETCH) );
    }

    // the downside to the curve used to represent the z-axis, is that
    // it goes to infinity quickly for negative Z values.  to get around this,
    // a line is used to continue the slope manageably for negative z.
    private static double ZFACT_TAIL_SLOPE = 2*(getZFact(1)-getZFact(0)) / (1-0);

    /**
     * given a point in (x,y,z) space, return the real (x,y) coords needed to 
     * display the point.
     * 
     * @param x
     * @param y
     * @param z
     * @return int array holding x and y coords.
     */
    private int[] renderFromZ(int x, int y, int z){
    	double zfact = getZFact(z);
    	if (z<0)
    		zfact = z * ZFACT_TAIL_SLOPE;
    	int eff_x = x + (int)(zfact * (levelinfo.getZPull_X()-x));
    	int eff_y = y + (int)(zfact * (levelinfo.getZPull_Y()-y));
    	int[] effcoords = {eff_x, eff_y};
    	return effcoords;
    }

    /**
     * draw the inpassed object; inpassed coords are 3D.
     * @param g2d
     * @param color
     * @param coords
     */
    private void drawObject(Graphics2D g2d, Color color, List<int[]> coords){
    	drawObject(g2d, color, coords, 0);
    }
    
    /**
     * Draw the object inpassed, which is assumed to be a set of coordinates
     * of dots to connect.  inpassed coords are 3d.
     * 
     * The goal here is to emulate the vector graphics style of tempest, where 
     * everything is a combination of drawn lines.
     *  
     * @param g2d - the graphics control object
     * @param color - which color to use to render the object.
     * @param coords
     * @param zoffset
     */
    private void drawObject(Graphics2D g2d, Color color, List<int[]> coords, int zoffset){
    	int oldx = 0, oldy=0;
        g2d.setColor(color);
    	for (int i=0; i<coords.size(); i++)
    	{
    		int x=coords.get(i)[0];
    		int y=coords.get(i)[1];
    		int z=coords.get(i)[2];
    		int[] eff_coords = renderFromZ(x, y, z-boardpov+zoffset);
    		
    		if (i > 0) {
    			g2d.drawLine(oldx, oldy, eff_coords[0], eff_coords[1]);
    		}
    		oldx=eff_coords[0];
    		oldy=eff_coords[1];
    	}
    }
    
    /**
     * Draw the board, based on the coordinates of the front of the 
     * current level.  depth axis is generated.
     * 
     * @param g2d
     * @param colCoords
     */
    private void drawBoard(Graphics2D g2d, List<int[]> colCoords, int playerCol){
    	int oldx = 0, oldy=0, oldbackx=0, oldbacky=0;
    	int z=LEVEL_DEPTH;
        g2d.setColor(Color.BLUE);
    	for (int i=0; i<colCoords.size(); i++)
    	{
    		int[] ftCoords = renderFromZ(colCoords.get(i)[0], colCoords.get(i)[1], 0-boardpov);
    		int x=ftCoords[0];
    		int y=ftCoords[1];
    		int[] backCoords = renderFromZ(colCoords.get(i)[0], colCoords.get(i)[1], z-boardpov);
    		int backx = backCoords[0];
    		int backy = backCoords[1];
    		if (i > 0) {
    			g2d.drawLine(oldx, oldy, x, y);
  			    g2d.drawLine(oldbackx, oldbacky, backx, backy);
    			if (i == playerCol || i == playerCol+1){
    		        g2d.setColor(Color.YELLOW);
    			}
    			if (i < colCoords.size()-1 || i==playerCol+1 || !levelinfo.isContinuous())
        			g2d.drawLine(x,  y, backx, backy);
    			if (i == playerCol || i == playerCol + 1){
        	        g2d.setColor(Color.BLUE);
    			}
    		}
    		else {
    			if (i == playerCol || i == playerCol+1){
    		        g2d.setColor(Color.YELLOW);
    			}
    			g2d.drawLine(x,  y, backx, backy);
    			if (i == playerCol || i == playerCol + 1){
        	        g2d.setColor(Color.BLUE);
    			}
    		}
    		oldx=x;
    		oldy=y;
    		oldbackx=backx;
    		oldbacky=backy;
    	}
    }

    /**
     * Main refresh routine.
     */
    public void paint(Graphics g) {
    	Graphics2D g2d = (Graphics2D)g;
    	if (pause)
    	{
    		Font small = new Font("Helvetica", Font.BOLD, 14);
    		FontMetrics metr = this.getFontMetrics(small);
    		g.setColor(Color.white);
    		g.setFont(small);
    		String pausestr = "PAUSED";
    		g.drawString(pausestr, (getWidth() - metr.stringWidth(pausestr)) / 2,
    				getHeight() / 2);
    		return;
    	}
    	
    	super.paint(g);

    	if (!gameover) {
    		// draw level board
    		drawBoard(g2d, levelinfo.getBoardFrontCoords(), crawler.getColumn());

    		// draw crawler
    		if (crawler.isVisible()){
    			Color c = Color.YELLOW;
    			if (crawlerSpiked)
    				c = new Color(r.nextInt(255),r.nextInt(255),r.nextInt(255));
    			drawObject(g2d, c, crawler.getCoords(), crawlerzoffset);
    		}

    		// draw crawler's missiles
    		for (Missile m : crawler.getMissiles()) {
    			if (m.isVisible())
    				drawObject(g2d, Color.YELLOW, m.getCoords(levelinfo));
    		}

    		// draw exes
    		for (Ex ex : exes) {
    			if (ex.isVisible())
    				drawObject(g2d, Color.RED, ex.getCoords(levelinfo), crawlerzoffset);
    			else {
    				// not visible but still in list means just killed
    				drawObject(g2d, Color.WHITE, ex.getDeathCoords(levelinfo)); 
    			}
    		}

    		// draw ex missiles
    		for (Missile exm : exmissiles) {
    			if (exm.isVisible())
    				drawObject(g2d, Color.GRAY, exm.getCoords(levelinfo));
    		}

    		// draw spikes and spinnythings
    		for (Spike s : spikes) {
    			if (s.isVisible()) {
    				List<int[]> spikeCoords = s.getCoords(levelinfo);
    				drawObject(g2d, Color.GREEN, spikeCoords);
    				spikeCoords.set(0, spikeCoords.get(1)); // add white dot at end
    				drawObject(g2d, Color.WHITE, spikeCoords);
    				if (s.isSpinnerVisible()) {
        				List<int[]> spinCoords = s.getSpinnerCoords(levelinfo);
        				drawObject(g2d, Color.GREEN, spinCoords);
    				}
    			}
    		}

    		// other crudethings?  vims, for extra lives?
    	}

    	g2d.setColor(Color.WHITE);
		g2d.drawString("SCORE: " + score, 5, 15);
		g2d.drawString("LIVES: " + lives, 5, 30);
		g2d.drawString("LEVEL: " + levelnum, 605, 15);

		if (gameover) {
    		String gmovr_msg = "Game Over";
    		String restart_msg = "Press Space to Restart";
    		Font small = new Font("Helvetica", Font.BOLD, 14);
    		FontMetrics metr = this.getFontMetrics(small);

    		g.setColor(Color.white);
    		g.setFont(small);
    		g.drawString(gmovr_msg, (getWidth() - metr.stringWidth(gmovr_msg)) / 2,
    				getHeight() / 2);
    		g.drawString(restart_msg, (getWidth() - metr.stringWidth(restart_msg)) / 2,
    				getHeight() * 3/4);
    	}

    	Toolkit.getDefaultToolkit().sync();
    	g.dispose();
    }

    /**
     * Timer driven update function, handles position updating for
     * everything on board.  
     * Driven by this.timer.
     * 
     */
    public void actionPerformed(ActionEvent e) {
    	boolean playerDead = (clearboard && !levelcleared) || crawlerSpiked;

    	if (pause)
    		return; // if we're on pause, don't do anything.

    	// if player died, they don't get to move crawler
    	if (!playerDead)
    		crawler.move(crawlerzoffset);

    	if (clearboard)	{ 
    		// if we're clearing the board, updating reduces to the boardclear animations
    		if (crawlerSpiked) {
    			dptLeft -=1;
    			if (dptLeft <= 0)
    				if (lives > 0)
    					replayLevel();
    				else
    					crawlerSpiked = false; // damage has been done; other case will now handle game over.
    		}
    		else if (levelcleared)
    		{   // player passed level.  
    			// pull board out towards screen until player leaves far end of board.
    			boardpov += SPEED_LEV_ADVANCE;
    			if (crawlerzoffset < LEVEL_DEPTH)
    				crawlerzoffset+=SPEED_LEV_ADVANCE; 

    			if (boardpov > LEVEL_DEPTH * 3/2)
    			{
    				levelnum++;
    				initLevel();
    				boardpov = -LEVEL_DEPTH * 2;
    				levelprep=true;
    			}
    		}
    		else if (lives > 0)
    		{   // player died but we can continue - suck crawler down and restart level
    			crawlerzoffset += SPEED_LEV_ADVANCE*2;
    			if (crawlerzoffset > LEVEL_DEPTH)
    				replayLevel();
    		}
    		else
    		{ // player died and game is over.  advance everything along z away from player.
    			if (boardpov > -LEVEL_DEPTH *7)
    				boardpov -= GAME_OVER_BOARDSPEED;
    			else
    				gameover=true;
    		}
    	}

    	if (lives > 0)
    	{
    		if (levelprep)
    		{   // just cleared a level and we're prepping the new one
    			// advance POV onto board, and then allow normal play
    			boardpov += SPEED_LEV_ADVANCE*3/2;
    			if (boardpov >= 0)
    			{
    				boardpov = 0;
    				levelprep = false;
    			}
    		}

    		// update player missile positions
    		ArrayList<Missile> ms = crawler.getMissiles();
    		for (int i = 0; i < ms.size(); i++) {
    			Missile m = (Missile) ms.get(i);
    			if (m.isVisible()) 
    				m.move(LEVEL_DEPTH);
    			else
    				ms.remove(i);
    		}

    		if (!playerDead)
    		{   // if the player is alive, the exes can shoot
    			for (int i = 0; i < exes.size(); i++) {
    				Ex ex = (Ex) exes.get(i);
    				if (ex.isVisible()) 
    				{
    					ex.move(B_WIDTH, crawler.getColumn());
    					if ((ex.getZ() < LEVEL_DEPTH) 
    							&& (r.nextInt(10000) < levelinfo.getExFireBPS()))
    					{ // this ex fires a missile
    						exmissiles.add(new Missile(ex.getColumn(), ex.getZ(), false));
    					}
    				}
    				else 
    					exes.remove(i);
    			}
    			
    			for (Spike s : spikes) {
    				if (s.isVisible()) {
    					if (s.isSpinnerVisible()) {
    						s.move();
    					}
    				}
    			}

    		}
    		

    		// update ex missiles
    		for (int i = 0; i < exmissiles.size(); i++) {
    			Missile exm = (Missile) exmissiles.get(i);
    			if (exm.isVisible()) 
    				exm.move(LEVEL_DEPTH);
    			else {
    				exmissiles.remove(i);
    			}
    		}

    		if (!playerDead)
    			checkCollisions();

    		// did player clear level?
    		if (exes.size() <= 0 && !crawlerSpiked)
    		{
    			levelcleared = true;
    			clearboard = true;
    		}
    	}
    	repaint();  
    }
    
    private void playerDeath() {
    	lives--;
    	clearboard = true;
    }

    /**
     * check for relevant ingame collisions
     */
    public void checkCollisions() {
    	int cCol = crawler.getColumn();

    	if (clearboard && levelcleared && !crawlerSpiked) {
    		// check spike/player
    		for (Spike s : spikes) {
    			if (s.isVisible() && s.getColumn() == cCol && ((LEVEL_DEPTH-s.getLength()) < crawlerzoffset)) {
    				playerDeath();
    				crawlerSpiked = true;
    				dptLeft = DEATH_PAUSE_TICKS;
    				levelcleared = false;  
    				break;
    			}
    		}
    	}
    	else {
    		// check ex/player
        	for (Ex ex : exes) {
        		if ((ex.getColumn() == cCol) && (ex.getZ() < Ex.HEIGHT)) {
        			playerDeath();
        			ex.resetState();
        			break;
        		}
        	}

        	// check exes' missiles / player
    		for (Missile exm : exmissiles) {
    			if (exm.isVisible() 
    					&& (exm.getColumn() == crawler.getColumn()) && (exm.getZPos() < Crawler.CHEIGHT)) 
    			{
    				playerDeath();
    				exm.setVisible(false);
    				break;
    			}
    		}

    	}

    	// check player's missiles vs everything
    	int ncols = levelinfo.getColumns().size();
    	for (Missile m : crawler.getMissiles()) {
    		// vs exes:
    		for (Ex ex : exes) {
    			// check for normal missile/ex collision, also ex adjacent to crawler
    			if (m.isVisible() 
    					&& (m.getColumn() == ex.getColumn() && (Math.abs(m.getZPos() - ex.getZ())<ex.HEIGHT)) 
    					|| ((m.getColumn() == crawler.getColumn()) 
    							&& (ex.getZ() <= 0)
    							&& (m.getZPos() < crawler.CHEIGHT*2)
    							&& (((ex.getColumn() +1)%ncols == crawler.getColumn())
    									|| ((crawler.getColumn()+1)%ncols == ex.getColumn())))){
    				m.setVisible(false);
    				ex.setVisible(false);
    				score += Ex.SCOREVAL;
    				break;
    			}
    		}

    		// vs exmissiles:
    		if (m.isVisible()) {
    			for (Missile exm : exmissiles) {
    				if ((m.getColumn() == exm.getColumn())
    						&& (exm.getZPos() - m.getZPos() < Missile.HEIGHT)) {
    					exm.setVisible(false);
    					m.setVisible(false);
    				}
    			}
    		}

    		// vs spikes
    		if (m.isVisible()) {
    			for (Spike s : spikes) {
    				if (s.isVisible() 
    						&& m.getColumn() == s.getColumn() 
    						&& ((LEVEL_DEPTH - s.getLength()) < m.getZPos() - Missile.HEIGHT/2)) {
    					s.impact();
    					m.setVisible(false);
    					score += Spike.SPIKE_SCORE;
    					if (s.isSpinnerVisible() &&
    							Math.abs(s.getSpinnerZ() - m.getZPos()) < Missile.HEIGHT) {
    						s.setSpinnerVisible(false);
    						score += Spike.SPINNER_SCORE;
    					}
    				}
    			}
    		}
    	}
    }

    /**
     * Handle user keypresses.
     * @author ugliest
     *
     */
    private class TAdapter extends KeyAdapter {

        public void keyReleased(KeyEvent e) {
            crawler.keyReleased(e);
        }

        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_P) {
            	pause = !pause;
            	repaint();
            }
            else
                crawler.keyPressed(e, crawlerzoffset);
            
            if (gameover)
            {
                if (key == KeyEvent.VK_SPACE) {
                    startGame();
                }
        	}
        }
    }
}

