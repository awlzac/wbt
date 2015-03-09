package wbtempest;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

import java.util.*;

/**
 * Main object, handles board, layout, update loop, and repainting.
 * 
 * Game is initiated by instantiating one of these.
 * 
 * @author ugliest
 *
 */
public class Board extends JPanel implements ActionListener {

	private static final long serialVersionUID = -1467405293079888602L;
    private static Random r = new Random(new java.util.Date().getTime());
    static int B_WIDTH = 800;
    static int B_HEIGHT = 680;
    static int LEVEL_DEPTH = 600;
    static int START_LIVES = 3;
	private static double ZSTRETCH = 125; // lower = more stretched on z axis
	private static int SPEED_LEV_ADVANCE = 7;  // speed at which we to the next board after clearing a level
	private static int GAME_OVER_BOARDSPEED = 40;  // speed at which the game recedes when player loses
	private static int DEATH_PAUSE_TICKS = 70;  // ticks to pause on crawler death
	private static int SUPERZAPPER_TICKS = 30; // how long does a superzap last?
	private static int NUM_STARS = 50; // number of stars when entering a level

	private Timer timer;
    private Crawler crawler;
    private ArrayList<Ex> exes;
    private ArrayList<Missile> enemymissiles;
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
    private int hiscore=0;
    private int boardpov;
    private int crawlerzoffset;
    private int dptLeft;   // death pause ticks remaining; used to provide a pause when player dies.
    private int superzapperTicksLeft = 0;  // if this is > 0, we're currently in a superzap
    private boolean crawlerSpiked;
    private List<List<int[]>> stars;

	Font stdfnt, bigfnt;

    public Board() {
    	addKeyListener(new TAdapter());
    	setFocusable(true);
    	setBackground(Color.BLACK);
    	setDoubleBuffered(true);

    	try {
        	InputStream fntStr = this.getClass().getClassLoader().getResourceAsStream("lt.ttf");
    		GraphicsEnvironment ge = 
    				GraphicsEnvironment.getLocalGraphicsEnvironment();
    		ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, fntStr));
    		fntStr.close();
        	stdfnt = new Font("Lowtech", Font.BOLD, 20);
        	bigfnt = new Font("Lowtech", Font.BOLD, 50);
    	} catch (Exception e) {
    		// just use helvetica
        	stdfnt = new Font("Helvetica", Font.BOLD, 20);
        	stdfnt = new Font("Helvetica", Font.BOLD, 50);
    	}

        exes = new ArrayList<Ex>();
        enemymissiles = new ArrayList<Missile>();
        spikes = new ArrayList<Spike>();
        stars = new ArrayList<List<int[]>>();
        for (int i=0; i< NUM_STARS; i++){
        	// this is awkward, but we'd like to use the existing plotting method, which expects a 
        	// List for each thing to be drawn -- in this case, a point.
        	int[] starcoords = new int[3];
        	starcoords[0] = r.nextInt(B_WIDTH);
        	starcoords[1] = r.nextInt(B_HEIGHT);
        	starcoords[2] = -r.nextInt(LEVEL_DEPTH);
        	List<int[]> starcoordlist = new ArrayList<int[]>();
        	starcoordlist.add(starcoords);
        	starcoordlist.add(starcoords);
        	stars.add(starcoordlist);
        }

        setSize(B_WIDTH, B_HEIGHT);

        FileReader f = null;
        try { // this is the sort of shit that made me question java.
        	f = new FileReader("wbt.hi");
        	BufferedReader br = new BufferedReader(f);
        	hiscore = Integer.parseInt(br.readLine());;
      		f.close();
        }
        catch (Exception e)
        { // if we can't read the high score file...oh well.
        }
        
        // force soundmanager singleton to initialize
        SoundManager.get();

        startGame();

        // start our ghetto timer loop
        timer = new Timer(15, this);  // appx 60fps
        timer.start();
    }
    
    /**
     * Initialize the game.
     */
    private void startGame()
    {
    	lives=START_LIVES;
    	gameover=false;
    	score = 0;
    	levelnum = 1;

    	initLevel();
    }
    
    /**
     * initialize a level for play
     */
    private void initLevel(){
    	levelinfo = new Level (levelnum, B_WIDTH, B_HEIGHT);
        crawler = new Crawler(levelinfo);
        exes.clear();
        int ncols = levelinfo.getColumns().size();
        for (int i=0; i<levelinfo.getNumExes(); i++ ) {
            exes.add(new Ex(r.nextInt(ncols),
            		levelnum > 1 ? r.nextBoolean() : false,
            		ncols, 
       				levelinfo.exesCanMove(),
       				levelinfo.isContinuous() ));
        }
        spikes.clear();
        if (levelinfo.getNumSpikes() > 0) {
        	boolean hasSpike[] = new boolean[ncols];
        	for (int i=0; i<ncols;i++)
        		hasSpike[i]=true;
        	for (int i=0; i<(ncols-levelinfo.getNumSpikes());) {
        		int sc = r.nextInt(ncols);
        		if (hasSpike[sc]) {
        			hasSpike[sc] = false;
        			i++;
        		}
        	}
        	for (int i=0; i<ncols;i++)
        		if (hasSpike[i])
        			spikes.add(new Spike(i));
        	
        }
        crawler.resetZapper();

        //also do whatever we need when replaying a level
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
        enemymissiles.clear();
        if (exes.size() == 0) {
        	// need at least one ex
            exes.add(new Ex(r.nextInt(levelinfo.getColumns().size()),
            		r.nextBoolean(),
            		levelinfo.getColumns().size(), 
       				levelinfo.exesCanMove(),
       				levelinfo.isContinuous() ));
            exes.get(0).resetZ(LEVEL_DEPTH *5/4);
        }
        else {
          for (Ex ex : exes )
            ex.resetZ(r.nextInt(LEVEL_DEPTH*2 + LEVEL_DEPTH *levelinfo.getNumExes()/5) + LEVEL_DEPTH*5/4);
        }
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
    	if (z<-Ex.HEIGHT) // switch to a constant slope to avoid math oblivion for negative z
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
     * Draw the actual board, based on the coordinates of the front of the 
     * current level.  depth axis is generated.
     * 
     * @param g2d
     * @param colCoords
     */
    private void drawBoard(Graphics2D g2d, List<int[]> colCoords, int playerCol){
    	int oldx = 0, oldy=0, oldbackx=0, oldbacky=0;
    	int z=LEVEL_DEPTH;
    	Color boardColor = levelinfo.getLevelColor();
    	if (superzapperTicksLeft > 0) { 
    		boardColor = new Color(r.nextInt(255),r.nextInt(255),r.nextInt(255));
    		superzapperTicksLeft--;
    	}
        g2d.setColor(boardColor);
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
        	        g2d.setColor(boardColor);
    			}
    		}
    		else {
    			if (i == playerCol || i == playerCol+1){
    		        g2d.setColor(Color.YELLOW);
    			}
    			g2d.drawLine(x,  y, backx, backy);
    			if (i == playerCol || i == playerCol + 1){
        	        g2d.setColor(boardColor);
    			}
    		}
    		oldx=x;
    		oldy=y;
    		oldbackx=backx;
    		oldbacky=backy;
    	}
    }

    /**
     * draw centered text at inpassed y.
     * 
     * @param g2d
     * @param text
     * @param y
     */
	private void drawCenteredText(Graphics2D g2d, String text, float y, Font fnt) {
		g2d.setFont(fnt);
		g2d.drawString(text, (getWidth()-this.getFontMetrics(fnt).stringWidth(text))/2, y);
	}
	private void drawCenteredText(Graphics2D g2d, String text, float y) {
		drawCenteredText(g2d, text, y, stdfnt);
	}

    /**
     * Main refresh routine.
     */
    public void paint(Graphics g) {
    	Graphics2D g2d = (Graphics2D)g;

		if (pause)
    	{
    		g.setColor(Color.white);
    		g.setFont(bigfnt);
    		drawCenteredText(g2d, "PAUSED", getHeight() / 2);
    		return;
    	}
    	
    	super.paint(g); // will clear screen

    	if (!gameover) {
    		// draw level board
    		drawBoard(g2d, levelinfo.getBoardFrontCoords(), crawler.getColumn());

    		// draw crawler
    		if (crawler.isVisible()){
    			Color c = Color.YELLOW;
    			if (dptLeft > 0)
    				c = new Color(r.nextInt(255),r.nextInt(255),r.nextInt(255));
    			drawObject(g2d, c, crawler.getCoords(), crawlerzoffset);
    		}
    		
    		if (boardpov < -Crawler.CHEIGHT) {
    			// pov shows game level board in the distance; add stars for fun
    			for (List<int[]> s : stars) {
    				Color c = new Color(r.nextInt(255),r.nextInt(255),r.nextInt(255));
    				drawObject(g2d, c, s);
    			}
    		}

    		// draw crawler's missiles
    		Color missileColors[] = {Color.BLUE, Color.RED, Color.green};
    		for (Missile m : crawler.getMissiles()) {
    			if (m.isVisible()) {
    				drawObject(g2d, Color.YELLOW, m.getCoords(levelinfo));
    				drawObject(g2d, missileColors[r.nextInt(missileColors.length)], m.getLayerCoords(levelinfo));
    			}
    		}

    		// draw exes
    		for (Ex ex : exes) {
    			if (ex.isVisible())
    				if (ex.isPod())
        				drawObject(g2d, Color.MAGENTA, ex.getCoords(levelinfo), crawlerzoffset);
    				else
    					drawObject(g2d, Color.RED, ex.getCoords(levelinfo), crawlerzoffset);
    			else {
    				// not visible but still in list means just killed
    				drawObject(g2d, Color.WHITE, ex.getDeathCoords(levelinfo)); 
    			}
    		}

    		// draw enemy missiles
    		for (Missile exm : enemymissiles) {
    			if (exm.isVisible()) {
    				drawObject(g2d, Color.GRAY, exm.getCoords(levelinfo));
    				drawObject(g2d, Color.RED, exm.getLayerCoords(levelinfo));
    			}
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

    	g2d.setColor(Color.GREEN);
		g2d.setFont(bigfnt);
//		g2d.drawString("SCORE:", 5, 15);
		g2d.drawString(Integer.toString(score), 100, 50);
		if (score > hiscore)
			hiscore = score;
		g2d.setFont(stdfnt);
		drawCenteredText(g2d, "HIGH: " + hiscore, 30);
		drawCenteredText(g2d, "LEVEL: "+levelnum, 55);
		g2d.drawString("LIVES:", 680, 30);
		g2d.drawString(Integer.toString(lives), 745, 30);
		
		if (levelprep){
			g2d.setColor(Color.YELLOW);
			drawCenteredText(g2d, "SUPERZAPPER RECHARGE", B_HEIGHT *2/3);
		}

	
		if (gameover) {
    		g.setColor(Color.GREEN);
    		drawCenteredText(g2d, "GAME OVER", getHeight() / 2, bigfnt);
    		drawCenteredText(g2d, "PRESS SPACE TO RESTART", getHeight() * 3/4);

    		FileWriter f=null;
            try {
            	f = new FileWriter("wbt.hi");
            	f.write(Integer.toString(hiscore));
        		f.close();
            }
            catch (Exception e)
            { // if we can't write the hi score file...oh well.	
            }
    		
    	}

    	Toolkit.getDefaultToolkit().sync();
    	g.dispose();
    }
    
    private boolean isPlayerDead(){
    	return (clearboard && !levelcleared) || crawlerSpiked;
    }
    

    /**
     * Timer driven update function, handles position updating for
     * everything on board.  
     * Driven by this.timer.
     * 
     */
    public void actionPerformed(ActionEvent e) {

    	if (pause)
    		return; // if we're on pause, don't do anything.

    	// if player died, they don't get to move crawler or superzap
    	if (!isPlayerDead()){
    		crawler.move(crawlerzoffset);
        	if (superzapperTicksLeft == 0 && crawler.isSuperzapping()) {
        		superzapperTicksLeft = SUPERZAPPER_TICKS;
        	}
    	}
   	
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

    			if (boardpov > LEVEL_DEPTH * 5/4)
    			{
     				levelnum++;
    				initLevel();
    				boardpov = -LEVEL_DEPTH * 2;
    				levelprep=true;
    			}
    		}
    		else if (lives > 0)
    		{   // player died but not out of lives.
    			// pause, then suck crawler down and restart level
    			dptLeft -=1;
    			if (dptLeft <= 0) {
    				crawlerzoffset += SPEED_LEV_ADVANCE*2;
    				if (crawlerzoffset > LEVEL_DEPTH)
    					replayLevel();
    			}
    		}
    		else
    		{ // player died and game is over.  advance everything along z away from player.
    			if (boardpov > -LEVEL_DEPTH *5)
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

    		if (!isPlayerDead())
    		{   // if the player is alive, the exes and spikes can move and shoot
    			for (int i = 0; i < exes.size(); i++) {
    				Ex ex = (Ex) exes.get(i);
    				if (ex.isVisible()) 
    				{
    					ex.move(B_WIDTH, crawler.getColumn());
    					if (ex.getZ() <= 0) {
    				        if (ex.isPod()) {
    				        	// we're at the top of the board; split the pod
    				        	exes.add(ex.spawn());
    				        	ex.setPod(false);
    				        }
    					}
    					if ((ex.getZ() < LEVEL_DEPTH) 
    							&& (r.nextInt(10000) < levelinfo.getExFireBPS()))
    					{ // this ex fires a missile
    						enemymissiles.add(new Missile(ex.getColumn(), ex.getZ(), false));
        	        		SoundManager.get().play(Sound.ENEMYFIRE);
    					}
    				}
    				else 
    					exes.remove(i);
    			}
    			
    			for (Spike s : spikes) {
    				if (s.isVisible()) {
    					if (s.isSpinnerVisible()) {
    						s.move();
        					if ((s.getSpinnerZ() < LEVEL_DEPTH) 
        							&& (r.nextInt(10000) < levelinfo.getExFireBPS()/4))
        					{ // with 1/4 the frequency of an ex, this spinner fires a missile
        						enemymissiles.add(new Missile(s.getColumn(), s.getSpinnerZ(), false));
            	        		SoundManager.get().play(Sound.ENEMYFIRE);
        					}
    					}
    				}
    			}
    		}
    		

    		// update ex missiles
    		for (int i = 0; i < enemymissiles.size(); i++) {
    			Missile exm = (Missile) enemymissiles.get(i);
    			if (exm.isVisible()) 
    				exm.move(LEVEL_DEPTH);
    			else {
    				enemymissiles.remove(i);
    			}
    		}

    		if (!isPlayerDead())
    			checkCollisions();

    		// did player clear level?
    		if (exes.size() <= 0 && !crawlerSpiked && !levelcleared)
    		{
    			levelcleared = true;
    			clearboard = true;
        		SoundManager.get().play(Sound.LEVELCLEAR);
    		}
    	}
    	repaint();  
    }
    
    private void playerDeath() {
    	lives--;
    	clearboard = true;
		dptLeft = DEATH_PAUSE_TICKS;
		SoundManager.get().play(Sound.CRAWLERDEATH);
    }

    /**
     * check for relevant in-game collisions
     */
    public void checkCollisions() {
    	int cCol = crawler.getColumn();

    	if (clearboard && levelcleared && !crawlerSpiked) {
    		// check spike/player
    		for (Spike s : spikes) {
    			if (s.isVisible() && s.getColumn() == cCol && ((LEVEL_DEPTH-s.getLength()) < crawlerzoffset)) {
    				playerDeath();
    				crawlerSpiked = true;
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
    		for (Missile exm : enemymissiles) {
    			if (exm.isVisible() 
    					&& (exm.getColumn() == crawler.getColumn()) && (exm.getZPos() < Crawler.CHEIGHT)) 
    			{
    				playerDeath();
    				exm.setVisible(false);
    				break;
    			}
    		}

    	}

    	// while not really a collision, the superzapper acts more or less like a 
    	// collision with all on-board non-pod exes, so it goes here.
    	if (superzapperTicksLeft == SUPERZAPPER_TICKS/2) {
    		// halfway through the superzap, actually destroy exes
    		for (Ex ex : exes){
    			if (ex.getZ() < LEVEL_DEPTH && !ex.isPod()) {
    				ex.setVisible(false);
            		SoundManager.get().play(Sound.ENEMYDEATH);
    			}
    		}
    	}

    	// check player's missiles vs everything
    	int ncols = levelinfo.getColumns().size();
    	for (Missile m : crawler.getMissiles()) {
    		// vs exes:
    		Ex newEx = null; // if this missile hits a pod, we may spawn a new ex
    		for (Ex ex : exes) {
    			// check for normal missile/ex collision, also ex adjacent to crawler
    			if (m.isVisible() 
    					&& (m.getColumn() == ex.getColumn() && (Math.abs(m.getZPos() - ex.getZ())< Ex.HEIGHT)) 
    					|| ((m.getColumn() == crawler.getColumn()) 
    							&& (ex.getZ() <= 0)
    							&& (r.nextInt(10) < 9)  // 90% success rate for hitting adjacent exes
    							&& (m.getZPos() < Crawler.CHEIGHT*2)
    							&& (((ex.getColumn() +1)%ncols == crawler.getColumn())
    									|| ((crawler.getColumn()+1)%ncols == ex.getColumn())))){
    				if (ex.isPod()) { 
    					// this ex is a pod; split into normal exes
    					score += Ex.PODSCOREVAL;
    					m.setVisible(false);
    					ex.setPod(false);
    					newEx = ex.spawn();
    	        		SoundManager.get().play(Sound.ENEMYDEATH);
    				}
    				else {
    					// player hit ex
    					m.setVisible(false);
    					ex.setVisible(false);
    					score += Ex.SCOREVAL;
    	        		SoundManager.get().play(Sound.ENEMYDEATH);
    					break;
    				}
    			}
    		}
    		if (newEx != null)
    			exes.add(newEx);

    		// vs exmissiles:
    		if (m.isVisible()) {
    			for (Missile exm : enemymissiles) {
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
    						&& ((LEVEL_DEPTH - s.getLength()) < m.getZPos())) {
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
            else if (!isPlayerDead())
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

