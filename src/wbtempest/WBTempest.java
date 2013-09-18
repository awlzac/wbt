package wbtempest;

import javax.swing.JFrame;

/**
 * Main/Entrypoint for wbtempest. 
 * @author ugliest
 *
 */
public class WBTempest extends JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 183599951344L;

	public WBTempest() {
        Board board = new Board();
        add(board);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(board.getWidth(), board.getHeight());
        setLocationRelativeTo(null);
        setTitle("WBTempest");
        setResizable(false);
        setVisible(true);
    }

    public static void main(String[] args) {
        new WBTempest();
    }
}
