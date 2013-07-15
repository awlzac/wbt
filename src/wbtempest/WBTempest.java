package wbtempest;

import javax.swing.JFrame;

public class WBTempest extends JFrame {

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
