import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;


class BoardStatus {
    int minRow;
    int maxRow;
    int minCol;
    int maxCol;
    int noOfWhites;
    int noOfBlacks;
    int noOfPlayerCaptures;
    int noOfOpponentCaptures;

    BoardStatus(int minRow, int maxRow, int minCol, int maxCol, int noOfWhites, int noOfBlacks, int noOfPlayerCaptures, int noOfOpponentCaptures) {
        this.minRow = minRow;
        this.maxRow = maxRow;
        this.minCol = minCol;
        this.maxCol = maxCol;
        this.noOfWhites = noOfWhites;
        this.noOfBlacks = noOfBlacks;
        this.noOfPlayerCaptures = noOfPlayerCaptures;
        this.noOfOpponentCaptures = noOfOpponentCaptures;
    }
}

public class homework {

    private static final int[] ROW_DIRECTION = {0, 1, 1, -1};
    private static final int[] COL_DIRECTION = {1, 0, -1, 1};
    //Parse from playtest.txt or create new only if you cant find any data
    static BoardStatus boardStatus = new BoardStatus(1, 19, 1, 19, 0, 0, 0, 0);
    static final String BLACK = "BLACK";
    static final String WHITE = "WHITE";

    //Scores
    static final int captureScore = 10000;
    static final int openTesseraScore = 5000;
    static final int openTriaScore = 1000;
    static final int stretchTriaScore = 300;
    static final int openTwoScore = 200;
    static final int stretchTwoScore = 10;

    static char player;
    static char opponent;
    static List<int[]> validMoves = new ArrayList<>();

    static int[] playerNextMove = new int[2];
    static int currentBoardScore = 0;

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

        player = color.equals(BLACK) ? 'b' : 'w';
        opponent = (player == 'b') ? 'w' : 'b';

        // Read time remaining
        timeRemaining = Double.parseDouble(scanner.nextLine());

        // Read number of pieces captured by each player
        String[] captures = scanner.nextLine().split(",");
        whiteCaptured = Integer.parseInt(captures[0]);
        blackCaptured = Integer.parseInt(captures[1]);

        boardStatus.noOfPlayerCaptures = player == 'b' ? blackCaptured : whiteCaptured;
        boardStatus.noOfOpponentCaptures = opponent == 'b' ? blackCaptured : whiteCaptured;


        // Read board
        for (int i = 19; i >= 1; i--) {
            String line = scanner.nextLine();
            for (int j = 1; j <= 19; j++) {
                board[i][j] = line.charAt(j - 1);
            }
        }

        updateBoardStatus(board);
        int[] move = findNextMove(color, timeRemaining);

        // Print output
        output.println(convertMoveFormat(move));

        //Print board status for debugging

        scanner.close();
        output.close();
    }

    private static String convertMoveFormat(int[] move) {
        int colValue = move[1] < 9 ? move[1] + 64 : move[1] + 65;
        return new StringBuilder().append(move[0]).append((char) colValue).toString();
    }

    private static void updateBoardStatus(char[][] board) {
        //Update other variables of boardstatus as well later
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == 'w') {
                    boardStatus.noOfWhites++;
                } else if (board[i][j] == 'b') {
                    boardStatus.noOfBlacks++;
                }
            }
        }
    }

    static int[] findNextMove(String color, double timeRemaining) {
        int[] ans = new int[2];
        //Case when first move on board is ours
        if (boardStatus.noOfBlacks == 0 && boardStatus.noOfWhites == 0) {
            ans[0] = 10;
            ans[1] = 10;
            return ans;
        }
        if (boardStatus.noOfWhites == 1) {
            if (color.equals(BLACK)) {
                ans[0] = 13;
                ans[1] = 7;
            } else {
                setEmptyCorner(ans);
            }
            return ans;
        }
        //Get the one move change be comparing prev from text file or calculate for the first time
        getLegalMovesAndMinMaxRowCol();

        int rootScore = alphaBeta(board, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, boardStatus.noOfPlayerCaptures, boardStatus.noOfOpponentCaptures, false);
        System.out.println(rootScore);
        System.out.println("Score of stone: " + getStoneScore(board, 12,7, player));

        return playerNextMove;
    }

    private static void getLegalMovesAndMinMaxRowCol() {
        int k = 0;
        boolean isMinSet = false;
        boolean isMinColSetForRow;
        boardStatus.minCol = 20;
        for (int i = 1; i <= 19; i++) {
            isMinColSetForRow = false;
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == '.') {
                    validMoves.add(new int[]{i, j});
                } else {
                    if (!isMinSet) {
                        boardStatus.minRow = i;
                        isMinSet = true;
                    }
                    if (!isMinColSetForRow) {
                        boardStatus.minCol = Math.min(boardStatus.minCol, j);
                        isMinColSetForRow = true;
                    }
                    boardStatus.maxRow = i;
                    boardStatus.maxCol = Math.max(boardStatus.maxCol, j);
                }
            }
        }
    }


    private static void getCurrentBoardScore() {
        int pairs = 0;
        Set<Integer> usedStones = new HashSet<>(); // keep track of stones used in pairs

        // Check for pairs
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] != '.' && !usedStones.contains(i * 19 + j)) {
                    char currentColor = board[i][j];

                    // Check horizontally for pairs
                    if (j <= 19 && board[i][j + 1] == currentColor && !usedStones.contains(i * 19 + j + 1)) {
                        pairs++;
                        usedStones.add(i * 19 + j);
                        usedStones.add(i * 19 + j + 1);
                    }

                    // Check vertically for pairs
                    if (i <= 19 && board[i + 1][j] == currentColor && !usedStones.contains((i + 1) * 19 + j)) {
                        pairs++;
                        usedStones.add(i * 19 + j);
                        usedStones.add((i + 1) * 19 + j);
                    }

                    // Check diagonally for pairs
                    if (i <= 19 && j <= 19 && board[i + 1][j + 1] == currentColor && !usedStones.contains((i + 1) * 19 + j + 1)) {
                        pairs++;
                        usedStones.add(i * 19 + j);
                        usedStones.add((i + 1) * 19 + j + 1);
                    }
                }
            }
        }

        System.out.println("Number of pairs: " + pairs);
    }

    public static int alphaBeta(char[][] board, int depth, int alpha, int beta, int playerCaptures, int opponentCaptures, boolean isMaximizingPlayer) {
        if(playerCaptures == 5) {
            return Integer.MAX_VALUE;
        }
        if(opponentCaptures == 5) {
            return Integer.MIN_VALUE;
        }

        if (depth == 0) {
            return evaluate(board, playerCaptures, opponentCaptures);
        }

        List<int[]> validMoves = getValidMoves(board);

        if (isMaximizingPlayer) {
            int bestScore = Integer.MIN_VALUE;
            for (int[] move : validMoves) {
                int score;
                char[][] newBoard = makeMove(board, move, opponent);
                opponentCaptures += getNoOfCaptures(newBoard, move, opponent);
               if(isWinningMove(newBoard, move, opponent)) {
                    score = Integer.MIN_VALUE;
                }
                else {
                    score = alphaBeta(newBoard, depth - 1, alpha, beta, playerCaptures, opponentCaptures, false);
                }
                if(score > bestScore) {
                    playerNextMove[0] = move[0];
                    playerNextMove[1] = move[1];
                }
                bestScore = Math.max(score, bestScore);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int[] move : validMoves) {
                int score;
                char[][] newBoard = makeMove(board, move, player);
                playerCaptures += getNoOfCaptures(newBoard, move, player);
                if(isWinningMove(newBoard, move, player)) {
                    score = Integer.MAX_VALUE;
                }
                else {
                    score = alphaBeta(newBoard, depth - 1, alpha, beta, playerCaptures, opponentCaptures, true);
                }
                if(score > bestScore) {
                    playerNextMove[0] = move[0];
                    playerNextMove[1] = move[1];
                }
                bestScore = Math.min(score, bestScore);
                beta = Math.min(beta, score);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestScore;
        }
    }

    private static boolean isWinningMove(char[][] board, int[] move, char currentPlayer) {
        int row = move[0];
        int col = move[1];

        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 4, 0), Math.min(col + 4, 19) + 1);
        String plusFourHorizontal = Arrays.toString(temp);
        String plusFourVertical = Arrays.toString(getColSegment(board, row, col, 4));
        String plusFourDiagonalRight = Arrays.toString(getDiagRightSegment(board, row, col, 4));
        String plusFourDiagonalLeft = Arrays.toString(getDiagLeftSegment(board, row, col, 4));

        return plusFourHorizontal.contains(getWinPattern(currentPlayer)) || plusFourVertical.contains(getWinPattern(currentPlayer)) ||
                plusFourDiagonalRight.contains(getWinPattern(currentPlayer)) || plusFourDiagonalLeft.contains(getWinPattern(currentPlayer));
    }

    private static int getNoOfCaptures(char[][] board, int[] move, char currentPlayer) {
        int row = move[0];
        int col = move[1];

        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col + 3, 19) + 1);
        String plusThreeHorizontal = Arrays.toString(temp);
        String plusThreeVertical = Arrays.toString(getColSegment(board, row, col, 3));
        String plusThreeDiagonalRight = Arrays.toString(getDiagRightSegment(board, row, col, 4));
        String plusThreeDiagonalLeft = Arrays.toString(getDiagLeftSegment(board, row, col, 4));
        int ans = 0;
        //Capture Move
        //TODO: Remove the captured coins and update the board
        if(plusThreeHorizontal.contains(getCapturePattern(currentPlayer))) {
            ans++;
        }
        if(plusThreeVertical.contains(getCapturePattern(currentPlayer))) {
            ans++;
        }
        if(plusThreeDiagonalRight.contains(getCapturePattern(currentPlayer))) {
            ans++;
        }
        if(plusThreeDiagonalLeft.contains(getCapturePattern(currentPlayer))) {
            ans++;
        }

        return ans;
    }

    private static char[][] makeMove(char[][] board, int[] move, char player) {
        char[][] newBoard = new char[20][20];
        for(int i=1; i<=19; i++) {
            for(int j=1; j<=19; j++) {
                newBoard[i][j] = board[i][j];
            }
        }
        newBoard[move[0]][move[1]] = player;
        return newBoard;
    }

    private static List<int[]> getValidMoves(char[][] board) {
        List<int[]> validMoves = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == '.') {
                    validMoves.add(new int[]{i, j});
                }
            }
        }
        return validMoves;
    }

    public static int evaluate(char[][] board, int whiteCaptures, int blackCaptures) {
        //Assuming five captures is handled before calling this function
        int playerScore = (player == 'w' ? whiteCaptures : blackCaptures) * captureScore;
        int opponentScore = (opponent == 'w' ? whiteCaptures : blackCaptures) * captureScore;

        // calculate score based on stone counts
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == player) {
                    playerScore += getStoneScore(board, i, j, player);
                } else if (board[i][j] == opponent) {
                    opponentScore += getStoneScore(board, i, j, opponent);
                }
                if(playerScore == Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
                else if(opponentScore == Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
            }
        }

        // return combined score
        return playerScore - opponentScore;
    }

    private static int getStoneScore(char[][] board, int row, int col, char currentPlayer) {
        System.out.println("Current stone: "  + board[row][col]);
        int score = 0;

        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 4, 0), Math.min(col + 4, 19) + 1);
        String plusFourHorizontal = Arrays.toString(temp);
        String plusFourVertical = Arrays.toString(getColSegment(board, row, col, 4));
        String plusFourDiagonalRight = Arrays.toString(getDiagRightSegment(board, row, col, 4));
        String plusFourDiagonalLeft = Arrays.toString(getDiagLeftSegment(board, row, col, 4));

        temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col + 3, 19) + 1);
        String plusThreeHorizontal = Arrays.toString(temp);
        String plusThreeVertical = Arrays.toString(getColSegment(board, row, col, 3));
        String plusThreeDiagonalRight = Arrays.toString(getDiagRightSegment(board, row, col, 4));
        String plusThreeDiagonalLeft = Arrays.toString(getDiagLeftSegment(board, row, col, 4));

        temp = Arrays.copyOfRange(board[row], Math.max(col - 2, 0), Math.min(col + 2, 19) + 1);
        String plusTwoHorizontal = String.valueOf(temp);
        String plusTwoVertical = Arrays.toString(getColSegment(board, row, col, 2));
        String plusTwoDiagonalRight = Arrays.toString(getDiagRightSegment(board, row, col, 2));
        String plusTwoDiagonalLeft = Arrays.toString(getDiagLeftSegment(board, row, col, 2));

        //Open tessera
        if(plusFourHorizontal.contains(getOpenTesseraPattern(currentPlayer))) {
            score += openTesseraScore;
        }
        if(plusFourVertical.contains(getOpenTesseraPattern(currentPlayer))) {
            score += openTesseraScore;
        }
        if(plusFourDiagonalRight.contains(getOpenTesseraPattern(currentPlayer))) {
            score += openTesseraScore;
        }
        if(plusFourDiagonalLeft.contains(getOpenTesseraPattern(currentPlayer))) {
            score += openTesseraScore;
        }

        //Open tria
        if(plusThreeHorizontal.contains(getOpenTriaPattern(currentPlayer))) {
            score += openTriaScore;
        }
        if(plusThreeVertical.contains(getOpenTriaPattern(currentPlayer))) {
            score += openTriaScore;
        }
        if(plusThreeDiagonalRight.contains(getOpenTriaPattern(currentPlayer))) {
            score += openTriaScore;
        }
        if(plusThreeDiagonalLeft.contains(getOpenTriaPattern(currentPlayer))) {
            score += openTriaScore;
        }

        //Stretch tria
        if(plusThreeHorizontal.contains(getStretchTriaPattern1(currentPlayer)) || plusThreeHorizontal.contains(getStretchTriaPattern2(currentPlayer))) {
            score += stretchTriaScore;
        }
        if(plusThreeVertical.contains(getStretchTriaPattern1(currentPlayer)) || plusThreeHorizontal.contains(getStretchTriaPattern2(currentPlayer))) {
            score += stretchTriaScore;
        }
        if(plusThreeDiagonalRight.contains(getStretchTriaPattern1(currentPlayer)) || plusThreeHorizontal.contains(getStretchTriaPattern2(currentPlayer))) {
            score += stretchTriaScore;
        }
        if(plusThreeDiagonalLeft.contains(getStretchTriaPattern1(currentPlayer)) || plusThreeHorizontal.contains(getStretchTriaPattern2(currentPlayer))) {
            score += stretchTriaScore;
        }

        //Open two
        System.out.println("Horizontal: "  +plusTwoHorizontal);
        if(plusTwoHorizontal.contains(getOpenTwoPattern(currentPlayer))) {
            score += openTwoScore;
        }
        if(plusTwoVertical.contains(getOpenTwoPattern(currentPlayer))) {
            score += openTwoScore;
        }
        if(plusTwoDiagonalRight.contains(getOpenTwoPattern(currentPlayer))) {
            score += openTwoScore;
        }
        if(plusTwoDiagonalLeft.contains(getOpenTwoPattern(currentPlayer))) {
            score += openTwoScore;
        }

        //Stretch two
        if(plusTwoHorizontal.contains(getStretchTwoPattern(currentPlayer))) {
            score += stretchTwoScore;
        }
        if(plusTwoVertical.contains(getStretchTwoPattern(currentPlayer))) {
            score += stretchTwoScore;
        }
        if(plusTwoDiagonalRight.contains(getStretchTwoPattern(currentPlayer))) {
            score += stretchTwoScore;
        }
        if(plusTwoDiagonalLeft.contains(getStretchTwoPattern(currentPlayer))) {
            score += stretchTwoScore;
        }

        return score;
    }

    private static String getStretchTwoPattern(char currentPlayer) {
        return currentPlayer == 'w' ? "w.w" : "b.b";
    }

    private static String getOpenTwoPattern(char currentPlayer) {
        return currentPlayer == 'w' ? ".ww." : ".bb.";
    }

    private static String getStretchTriaPattern1(char currentPlayer) {
        return currentPlayer == 'w' ? "w.ww" : "b.bb";
    }
    private static String getStretchTriaPattern2(char currentPlayer) {
        return currentPlayer == 'w' ? "ww.w" : "bb.b";
    }

    private static String getOpenTriaPattern(char currentPlayer) {
        return currentPlayer == 'w' ? ".www." : ".bbb.";
    }

    private static String getOpenTesseraPattern(char currentPlayer) {
        return currentPlayer == 'w' ? ".wwww." : ".bbbb.";
    }

    private static String getCapturePattern(char currentPlayer) {
        return currentPlayer == 'w' ? "wbbw" : "bwwb";
    }

    private static String getWinPattern(char currentPlayer) {
        return currentPlayer == 'w' ? "wwwww" : "bbbbb";
    }



    private static char[] getDiagLeftSegment(char[][] board, int row, int col, int size) {
        char[] diagLeftSegment = new char[9];
        int k = 0;
        for(int i = row + size, j = col - size; i >= row - size && j <= col + size; i--, j++) {
            if(!isValidPosition(19, i, j))
                continue;
            diagLeftSegment[k++] = board[i][j];
        }
        return diagLeftSegment;
    }

    private static char[] getDiagRightSegment(char[][] board, int row, int col, int size) {
        char[] diagRightSegment = new char[9];
        int k = 0;
        for(int i = row - size, j = col - size; i <= row + size && j <= col + size; i++, j++) {
            if(!isValidPosition(19, i, j))
                continue;
            diagRightSegment[k++] = board[i][j];
        }
        return diagRightSegment;
    }

    private static char[] getColSegment(char[][] board, int row, int col, int size) {
        char[] colSegment = new char[9];
        int k = 0;
        for(int j = Math.max(0, row - size); j <= Math.min(row + size, 19); j++) {
            colSegment[k++] = board[j][col];
        }
        return colSegment;
    }

    public static int countCaptures(char[][] board, char player) {
        int captures = 0;
        int boardSize = 19;
        char opponent = player == 'b' ? 'w' : 'b';
        int[][] DIRECTIONS = new int[][]{{0,1},{1,0},{1,1},{1,-1}};

        // Check all 8 directions for each stone on the board
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == player) {
                    for (int[] direction : DIRECTIONS) {
                        int x = i + direction[0];
                        int y = j + direction[1];
                        if (isValidPosition(boardSize, x, y) && board[x][y] == opponent) {
                            // Check if the opponent's stones can be captured in this direction
                            int captured = countCapturedStones(board, x, y, direction[0], direction[1], player);
                            if (captured > 0) {
                                captures += captured;
                            }
                        }
                    }
                }
            }
        }
        return captures;
    }

    private static int countCapturedStones(char[][] board, int row, int col, int dx, int dy, char player) {
        int boardSize = 19;
        int captured = 0;
        char opponent = player == 'b' ? 'w' : 'b';

        int i = row + dx;
        int j = col + dy;
        while (isValidPosition(boardSize, i, j) && board[i][j] == opponent) {
            captured++;
            i += dx;
            j += dy;
        }

        // Check if the chain of opponent's stones is surrounded on both sides
        if (isValidPosition(boardSize, i, j) && board[i][j] == player && captured == 2) {
            return captured;
        } else {
            return 0;
        }
    }

    private static boolean isValidPosition(int boardSize, int row, int col) {
        return row >= 1 && row <= boardSize && col >= 1 && col <= boardSize;
    }

    /*public static void sortMoves(char[][] board, List<int[]> moves) {
        // Separate moves into different categories
        List<int[]> captureMoves = new ArrayList<>();
        List<int[]> openThreeMoves = new ArrayList<>();
        List<int[]> blockThreeMoves = new ArrayList<>();
        List<int[]> openTwoMoves = new ArrayList<>();
        List<int[]> blockTwoMoves = new ArrayList<>();
        List<int[]> otherMoves = new ArrayList<>();

        for (int[] move : moves) {
            if (isCaptureMove(move)) {
                captureMoves.add(move);
            } else if (isOpenThreeMove(move)) {
                openThreeMoves.add(move);
            } else if (isBlockThreeMove(board, move)) {
                blockThreeMoves.add(move);
            } else if (isOpenTwoMove(board, move)) {
                openTwoMoves.add(move);
            } else if (isBlockTwoMove(board, move)) {
                blockTwoMoves.add(move);
            } else {
                otherMoves.add(move);
            }
        }

        // Sort moves in each category based on their value
        Collections.sort(captureMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numCaptured1 = numStonesCaptured(board, move1);
                int numCaptured2 = numStonesCaptured(board, move2);
                return Integer.compare(numCaptured2, numCaptured1);
            }
        });

        Collections.sort(openThreeMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numOpenThrees1 = numOpenThreesCreated(board, move1);
                int numOpenThrees2 = numOpenThreesCreated(board, move2);
                return Integer.compare(numOpenThrees2, numOpenThrees1);
            }
        });

        Collections.sort(blockThreeMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numBlockThrees1 = numBlockThreesCreated(board, move1);
                int numBlockThrees2 = numBlockThreesCreated(board, move2);
                return Integer.compare(numBlockThrees2, numBlockThrees1);
            }
        });

        Collections.sort(openTwoMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numOpenTwos1 = numOpenTwosCreated(board, move1);
                int numOpenTwos2 = numOpenTwosCreated(board, move2);
                return Integer.compare(numOpenTwos2, numOpenTwos1);
            }
        });

        Collections.sort(blockTwoMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numBlockTwos1 = numBlockTwosCreated(board, move1);
                int numBlockTwos2 = numBlockTwosCreated(board, move2);
                return Integer.compare(numBlockTwos2, numBlockTwos1);
            }
        });

        // Combine all the sorted lists
        moves.clear();
        moves.addAll(captureMoves);
        moves.addAll(openThreeMoves);
        moves.addAll(blockThreeMoves);
        moves.addAll(openTwoMoves);
        moves.addAll(blockTwoMoves);
        moves.addAll(otherMoves);
    }

    public static boolean isCaptureMove(int[] move) {
        int row = move[0];
        int col = move[1];
        char opponent = (player == 'w') ? 'b' : 'w';
        if (board[row][col] != '.') {
            return false; // can't place a stone on an occupied cell
        }
        int capturedStones = 0;
        // Check for captures in horizontal direction
        for (int c = Math.max(0, col - 2); c <= Math.min(18, col + 2); c++) {
            if (c == col) {
                continue;
            }
            if (board[row][c] == player) {
                capturedStones++;
            } else if (board[row][c] == opponent) {
                capturedStones = 0;
                break;
            }
        }
        if (capturedStones >= 2) {
            return true;
        }
        capturedStones = 0;
        // Check for captures in vertical direction
        for (int r = Math.max(0, row - 2); r <= Math.min(18, row + 2); r++) {
            if (r == row) {
                continue;
            }
            if (board[r][col] == player) {
                capturedStones++;
            } else if (board[r][col] == opponent) {
                capturedStones = 0;
                break;
            }
        }
        if (capturedStones >= 2) {
            return true;
        }
        capturedStones = 0;
        // Check for captures in diagonal direction (top-left to bottom-right)
        for (int i = -2; i <= 2; i++) {
            int r = row + i;
            int c = col + i;
            if (r < 0 || r > 18 || c < 0 || c > 18) {
                continue;
            }
            if (r == row && c == col) {
                continue;
            }
            if (board[r][c] == player) {
                capturedStones++;
            } else if (board[r][c] == opponent) {
                capturedStones = 0;
                break;
            }
        }
        if (capturedStones >= 2) {
            return true;
        }
        capturedStones = 0;
        // Check for captures in diagonal direction (bottom-left to top-right)
        for (int i = -2; i <= 2; i++) {
            int r = row - i;
            int c = col + i;
            if (r < 0 || r > 18 || c < 0 || c > 18) {
                continue;
            }
            if (r == row && c == col) {
                continue;
            }
            if (board[r][c] == player) {
                capturedStones++;
            } else if (board[r][c] == opponent) {
                capturedStones = 0;
                break;
            }
        }
        if (capturedStones >= 2) {
            return true;
        }
        return false;
    }


    public static boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x <= 19 && y <= 19;
    }

    private static void getUpdatedBoardScore(int[][] updatedMove) {

    }

    private static int[][] getUpdatedMove() {
        return new int[][]{{1, 2}};
    }



*/

    private static void setEmptyCorner(int[] ans) {
        System.out.println(board[7][7]);
        if (board[7][7] == '.') {
            ans[0] = 7;
            ans[1] = 7;
        } else if (board[13][7] == '.') {
            ans[0] = 13;
            ans[1] = 7;
        } else if (board[13][13] == '.') {
            ans[0] = 13;
            ans[1] = 13;
        } else {
            ans[0] = 7;
            ans[1] = 13;
        }
    }


}