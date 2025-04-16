import java.io.Serializable;

public class PracticeGameObj implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean win;
    private char[][] board;
    private String inputString;

    public PracticeGameObj(boolean win, char[][] board, String inputString) {
        this.win = win;
        this.board = board;
        this.inputString = inputString;
    }
    public String getInputString() {return inputString;}
    public void setInputString(String testString) {this.inputString = testString;}

    public char[][] getBoard() { return board; }
    public void setBoard(char[][] board) { this.board = board; }

    public boolean isWin() { return win; }
    public void setWin(boolean win) { this.win = win; }

}