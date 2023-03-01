import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;


class BoardStatus {
    int minRow;
    int maxRow;
    int minCol;
    int maxCol;
    int noOfWhites;
    int noOfBlacks;

    BoardStatus(int minRow, int maxRow, int minCol, int maxCol, int noOfWhites, int noOfBlacks) {
        this.minRow = minRow;
        this.maxRow = maxRow;
        this.minCol = minCol;
        this.maxCol = maxCol;
        this.noOfWhites = noOfWhites;
        this.noOfBlacks = noOfBlacks;
    }
}

public class homework {

    //Parse from playtest.txt or create new only if you cant find any data
    static BoardStatus boardStatus = new BoardStatus(1,19,1,19,0,0);
    static final String BLACK = "BLACK";
    static final  String WHITE = "WHITE";

    static char[][] board = new char[20][20];
    public static void main(String[] args) throws IOException {
        // Read input from file
        String color = "";
        double timeRemaining = 0.0;
        int whiteCaptured = 0;
        int blackCaptured = 0;

            File file = new File(System.getProperty("user.dir") + "/src/input.txt");
            Scanner scanner = new Scanner(file);

            PrintWriter output = new PrintWriter(new File("output.txt"));
            // Read color
            color = scanner.nextLine();

            // Read time remaining
            timeRemaining = Double.parseDouble(scanner.nextLine());

            // Read number of pieces captured by each player
            String[] captures = scanner.nextLine().split(",");
            whiteCaptured = Integer.parseInt(captures[0]);
            blackCaptured = Integer.parseInt(captures[1]);

            // Read board
            for (int i = 19; i >= 1; i--) {
                String line = scanner.nextLine();
                for (int j = 1; j <= 19; j++) {
                    board[i][j] = line.charAt(j-1);
                }
            }

        updateBoardStatus(board);
        int[] move = findNextMove(color, timeRemaining, whiteCaptured, blackCaptured);

        // Print output
        output.println(convertMoveFormat(move));

        //Print board status for debugging

        scanner.close();
        output.close();
    }

    private static String convertMoveFormat(int[] move) {
        int colValue =  move[1] < 9 ? move[1] + 64 : move[1] + 65;
        return new StringBuilder().append(move[0]).append((char)colValue).toString();
    }

    private static void updateBoardStatus(char[][] board) {
        //Update other variables of boardstatus as well later
        for(int i=19; i>=1; i--) {
            for(int j=1; j<=19; j++) {
                if(board[i][j] == 'w') {
                    boardStatus.noOfWhites++;
                }
                else if(board[i][j] == 'b') {
                    boardStatus.noOfBlacks++;
                }
            }
        }
    }

    static int[] findNextMove(String color, double timeRemaining, int whiteCaptured, int blackCaptured) {
        int[] ans = new int[2];
        //Case when first move on board is ours
        if(boardStatus.noOfBlacks == 0 && boardStatus.noOfWhites == 0) {
            ans[0] = 10;
            ans[1] = 10;
        }
        if(boardStatus.noOfWhites == 1) {
            if(color.equals(BLACK)) {
                ans[0] = 13;
                ans[1] = 7;
            }
            else {
                System.out.println("Here");
                setEmptyCorner(ans);
            }
        }
        return ans;
    }

    private static void setEmptyCorner(int[] ans) {
        System.out.println(board[7][7]);
        if(board[7][7] == '.') {
            ans[0] = 7;
            ans[1] = 7;
        }
        else if(board[13][7] == '.') {
            ans[0] = 13;
            ans[1] = 7;
        }
        else if(board[13][13] == '.') {
            ans[0] = 13;
            ans[1] = 13;
        }
        else {
            ans[0] = 7;
            ans[1] = 13;
        }
    }
}