import com.sun.org.apache.xml.internal.security.algorithms.implementations.IntegrityHmac;
import org.w3c.dom.stylesheets.LinkStyle;

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

    private Map<String, MoveData> transpositionTable = new HashMap<>();

    //Scores
    static final int captureScore = 10000;
    static final int openTesseraScore = 6000;
    static final int stretchFourScore = 5000;
    static final int openTriaScore = 1000;
    static final int stretchTriaScore = 500;
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
        sortValidMoves();
        int rootScore = alphaBeta(board, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, boardStatus.noOfPlayerCaptures, boardStatus.noOfOpponentCaptures, false, validMoves);
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

    private static class MoveData {
        private int score;
        private int depth;
        private int[] bestMove = new int[2];

        public MoveData(int score, int depth) {
            this.score = score;
            this.depth = depth;
        }

        public MoveData(int score, int depth, int[] bestMove) {
            this.score = score;
            this.depth = depth;
            this.bestMove[0] = bestMove[0];
            this.bestMove[1] = bestMove[1];
        }
    }

    public int alphaBeta(char[][] board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        String boardKey = getBoardKey(board);
        if (transpositionTable.containsKey(boardKey)) {
            MoveData moveData = transpositionTable.get(boardKey);
            if (moveData.getDepth() >= depth) {
                if (moveData.getType() == NodeType.EXACT) {
                    return moveData.getScore();
                } else if (moveData.getType() == NodeType.LOWERBOUND) {
                    alpha = Math.max(alpha, moveData.getScore());
                } else if (moveData.getType() == NodeType.UPPERBOUND) {
                    beta = Math.min(beta, moveData.getScore());
                }
                if (alpha >= beta) {
                    return moveData.getScore();
                }
            }
        }

        if (depth == 0 || board.isGameOver()) {
            int score = board.evaluate();
            transpositionTable.put(boardKey, new MoveData(score, depth, NodeType.EXACT));
            return score;
        }

        if (validMoves.isEmpty()) {
            int score = board.evaluate();
            transpositionTable.put(boardKey, new MoveData(score, depth, NodeType.EXACT));
            return score;
        }

        if (maximizingPlayer) {
            int maxScore = Integer.MIN_VALUE;
            int[] bestMove = new int[2];
            bestMove[0] = Integer.MIN_VALUE;
            for (int[] move : validMoves) {
                if(board[move[0]][move[1]] != '.') continue;
                char[][] newBoard = makeMove(board, move, player);
                int score = alphaBeta(newBoard, depth - 1, alpha, beta, false);
                if (score > maxScore) {
                    maxScore = score;
                    bestMove[0] = move[0];
                    bestMove[1] = move[1];
                }
                alpha = Math.max(alpha, score);
                if (alpha >= beta) {
                    break;
                }
            }
            if (bestMove[0] == Integer.MIN_VALUE) {
                transpositionTable.put(boardKey, new MoveData(maxScore, depth, NodeType.EXACT, bestMove));
            }
            return maxScore;
        } else {
            int minScore = Integer.MAX_VALUE;
            Move bestMove = null;
            for (Move move : validMoves) {
                board.makeMove(move, Player.MIN);
                int score = alphaBeta(board, depth - 1, alpha, beta, true);
                board.undoMove(move);
                if (score < minScore) {
                    minScore = score;
                    bestMove = move;
                }
                beta = Math.min(beta, score);
                if (alpha >= beta) {
                    break;
                }
            }
            if (bestMove != null) {
                transpositionTable.put(boardKey, new MoveData(minScore, depth, NodeType.EXACT, bestMove));
            }
            return minScore;
        }
    }

    public static int alphaBeta(char[][] board, int depth, int alpha, int beta, int playerCaptures, int opponentCaptures, boolean isMaximizingPlayer, List<int[]> validMoves) {
        if(playerCaptures == 5) {
            return Integer.MAX_VALUE;
        }
        if(opponentCaptures == 5) {
            return Integer.MIN_VALUE;
        }

        if (depth == 0) {
            return evaluate(board, playerCaptures, opponentCaptures);
        }

        if (isMaximizingPlayer) {
            int bestScore = Integer.MIN_VALUE;
            for (int[] move : validMoves) {
                if(board[move[0]][move[1]] != '.') continue;
                int score;
                char[][] newBoard = makeMove(board, move, opponent);
                opponentCaptures += getNoOfCaptures(newBoard, move, opponent);
               if(isWinningMove(newBoard, move, opponent)) {
                    score = Integer.MIN_VALUE;
                }
                else {
                    score = alphaBeta(newBoard, depth - 1, alpha, beta, playerCaptures, opponentCaptures, false, validMoves);
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
                if(board[move[0]][move[1]] != '.') continue;
                int score;
                char[][] newBoard = makeMove(board, move, player);
                playerCaptures += getNoOfCaptures(newBoard, move, player);
                if(isWinningMove(newBoard, move, player)) {
                    score = Integer.MAX_VALUE;
                }
                else {
                    score = alphaBeta(newBoard, depth - 1, alpha, beta, playerCaptures, opponentCaptures, true, validMoves);
                }
                if(score > bestScore || score == Integer.MAX_VALUE) {
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

        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 5, 0), Math.min(col + 5, 19) + 1);
        String plusFiveHorizontal = String.valueOf(temp);
        String plusFiveVertical = String.valueOf(getColSegment(board, row, col, 5));
        String plusFiveDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 5));
        String plusFiveDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 5));

        return plusFiveHorizontal.contains(getWinPattern(currentPlayer)) || plusFiveVertical.contains(getWinPattern(currentPlayer)) ||
                plusFiveDiagonalRight.contains(getWinPattern(currentPlayer)) || plusFiveDiagonalLeft.contains(getWinPattern(currentPlayer));
    }

    private static int getNoOfCaptures(char[][] board, int[] move, char currentPlayer) {
        int row = move[0];
        int col = move[1];
        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col + 3, 19) + 1);
        String plusThreeHorizontal = String.valueOf(temp);
        String plusThreeVertical = String.valueOf(getColSegment(board, row, col, 3));
        String plusThreeDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
        String plusThreeDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));
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

    /*private static List<int[]> getValidMoves(char[][] board) {
        List<int[]> validMoves = new ArrayList<>();
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == '.') {
                    validMoves.add(new int[]{i, j});
                }
            }
        }
        return validMoves;
    }*/

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
        String plusFourHorizontal = String.valueOf(temp);
        String plusFourVertical = String.valueOf(getColSegment(board, row, col, 4));
        String plusFourDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
        String plusFourDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));

        temp = Arrays.copyOfRange(board[row], Math.max(col - 5, 0), Math.min(col + 5, 19) + 1);
        String plusFiveHorizontal = String.valueOf(temp);
        String plusFiveVertical = String.valueOf(getColSegment(board, row, col, 5));
        String plusFiveDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 5));
        String plusFiveDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 5));

        temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col + 3, 19) + 1);
        String plusThreeHorizontal = String.valueOf(temp);
        String plusThreeVertical = String.valueOf(getColSegment(board, row, col, 3));
        String plusThreeDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
        String plusThreeDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));

        temp = Arrays.copyOfRange(board[row], Math.max(col - 2, 0), Math.min(col + 2, 19) + 1);
        String plusTwoHorizontal = String.valueOf(temp);
        String plusTwoVertical = String.valueOf(getColSegment(board, row, col, 2));
        String plusTwoDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 2));
        String plusTwoDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 2));

        //Win pattern
        String str = getWinPattern(currentPlayer);
            if (plusFiveHorizontal.contains(str)) {
                return Integer.MAX_VALUE;
            }
            if (plusFiveVertical.contains(str)) {
                return Integer.MAX_VALUE;
            }
            if (plusFiveDiagonalRight.contains(str)) {
                return Integer.MAX_VALUE;
            }
            if (plusFiveDiagonalLeft.contains(str)) {
                return Integer.MAX_VALUE;
            }

        //Open Tessera
        String[] tempStringList = getOpenTesseraPatterns(currentPlayer);
        Set<String> foundSet= new HashSet<>();
        for(String s : tempStringList) {
            if (plusFourHorizontal.contains(s)) {
                foundSet.add(s);
                score += openTesseraScore;
            }
            if (plusFourVertical.contains(s)) {
                foundSet.add(s);
                score += openTesseraScore;
            }
            if (plusFourDiagonalRight.contains(s)) {
                foundSet.add(s);
                score += openTesseraScore;
            }
            if (plusFourDiagonalLeft.contains(s)) {
                foundSet.add(s);
                score += openTesseraScore;
            }
        }

        //Stretch Four
        tempStringList = getStretchFourPatterns(currentPlayer);
        for(String s : tempStringList) {
            if (plusFourHorizontal.contains(s) && !foundSet.contains(s)) {
                score += stretchFourScore;
            }
            if (plusFourVertical.contains(s) && !foundSet.contains(s)) {
                score += stretchFourScore;
            }
            if (plusFourDiagonalRight.contains(s) && !foundSet.contains(s)) {
                score += stretchFourScore;
            }
            if (plusFourDiagonalLeft.contains(s) && !foundSet.contains(s)) {
                score += stretchFourScore;
            }
        }

        //Open Tria
        tempStringList = getOpenTriaPatterns(currentPlayer);
        foundSet= new HashSet<>();
        for(String s : tempStringList) {
            if (plusThreeHorizontal.contains(s)) {
                foundSet.add(s);
                score += openTriaScore;
            }
            if (plusThreeVertical.contains(s)) {
                foundSet.add(s);
                score += openTriaScore;
            }
            if (plusThreeDiagonalRight.contains(s)) {
                foundSet.add(s);
                score += openTriaScore;
            }
            if (plusThreeDiagonalLeft.contains(s)) {
                foundSet.add(s);
                score += openTriaScore;
            }
        }

        //Stretch Three
        tempStringList = getStretchThreePatterns(currentPlayer);
        for(String s : tempStringList) {
            if (plusThreeHorizontal.contains(s) && !foundSet.contains(s)) {
                score += stretchTriaScore;
            }
            if (plusThreeVertical.contains(s) && !foundSet.contains(s)) {
                score += stretchTriaScore;
            }
            if (plusThreeDiagonalRight.contains(s) && !foundSet.contains(s)) {
                score += stretchTriaScore;
            }
            if (plusThreeDiagonalLeft.contains(s) && !foundSet.contains(s)) {
                score += stretchTriaScore;
            }
        }

        //Open Two
        tempStringList = getOpenTwoPatterns(currentPlayer);
        foundSet= new HashSet<>();
        for(String s : tempStringList) {
            if (plusTwoHorizontal.contains(s)) {
                foundSet.add(s);
                score += openTwoScore;
            }
            if (plusTwoVertical.contains(s)) {
                foundSet.add(s);
                score += openTwoScore;
            }
            if (plusTwoDiagonalRight.contains(s)) {
                foundSet.add(s);
                score += openTwoScore;
            }
            if (plusTwoDiagonalLeft.contains(s)) {
                foundSet.add(s);
                score += openTwoScore;
            }
        }

        //Stretch Two
        tempStringList = getStretchTwoPatterns(currentPlayer);
        for(String s : tempStringList) {
            if (plusTwoHorizontal.contains(s) && !foundSet.contains(s)) {
                score += stretchTwoScore;
            }
            if (plusTwoVertical.contains(s) && !foundSet.contains(s)) {
                score += stretchTwoScore;
            }
            if (plusTwoDiagonalRight.contains(s) && !foundSet.contains(s)) {
                score += stretchTwoScore;
            }
            if (plusTwoDiagonalLeft.contains(s) && !foundSet.contains(s)) {
                score += stretchTwoScore;
            }
        }

        return score;
    }

    private static String[] getBlockWinPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{"bbwbb", "wbbbb", "bwbbb", "bbbwb", "bbbbw"} : new String[]{"bwwww", "wbwww", "wwbww", "wwwbw", "wwwwb"};
    }

    private static String[] getBlockTesseraPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{"bbbw", "wbbb", "bbwb", "bwbb"} : new String[]{"wwwb", "bwww", "wwbw", "wbww"};
    }

    private static String[] getBlockTriaPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{"bbw", "wbb", "bwb"} : new String[]{"wwb", "bww", "wbw"};
    }

    private static String[] getBlockTwoPatterns() {
        return new String[]{"bw", "wb"};
    }
    private static String getCapturePattern(char currentPlayer) {
        return currentPlayer == 'w' ? "wbbw" : "bwwb";
    }

    private static String[] getBlockCapturePatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{"wwwb", "bwww"} : new String[] {"bbbw", "wbbb"};
    }

    private static String[] getOpenTesseraPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".wwww.", "www.w.", "ww.ww.", ".w.www", ".ww.ww"} : new String[]{".bbbb.", "bbb.b.", "bb.bb.", ".b.bbb", ".bb.bb"};
    }

    private static String[] getStretchFourPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".wwww", "wwww.", "ww.ww", "w.www", "www.w"} : new String[]{".bbbb", "bbbb.", "bb.bb", "b.bbb", "bbb.b"};
    }

    private static String[] getOpenTriaPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".www.", ".ww.w", "w.ww."} : new String[]{".bbb.", ".bb.b", "b.bb."};
    }

    private static String[] getStretchThreePatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".www", "www.", "ww.w", "w.ww"} : new String[]{".bbb", "bbb.", "bb.b", "b.bb"};
    }

    private static String[] getOpenTwoPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".ww.", "w.w.", ".w.w"} : new String[]{".bb.", "b.b.", ".b.b"};
    }

    private static String[] getStretchTwoPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{"w.w", "ww.", ".ww"} : new String[]{"b.b", "bb.", ".bb"};
    }


    private static String getWinPattern(char currentPlayer) {
        return currentPlayer == 'w' ? "wwwww" : "bbbbb";
    }



    private static char[] getDiagLeftSegment(char[][] board, int row, int col, int size) {
        char[] diagLeftSegment = new char[11];
        int k = 0;
        for(int i = row + size, j = col - size; i >= row - size && j <= col + size; i--, j++) {
            if(!isValidPosition(19, i, j))
                continue;
            diagLeftSegment[k++] = board[i][j];
        }
        return diagLeftSegment;
    }

    private static char[] getDiagRightSegment(char[][] board, int row, int col, int size) {
        char[] diagRightSegment = new char[11];
        int k = 0;
        for(int i = row - size, j = col - size; i <= row + size && j <= col + size; i++, j++) {
            if(!isValidPosition(19, i, j))
                continue;
            diagRightSegment[k++] = board[i][j];
        }
        return diagRightSegment;
    }

    private static char[] getColSegment(char[][] board, int row, int col, int size) {
        char[] colSegment = new char[11];
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

    public static void sortValidMoves() {
        // Separate moves into different categories
        List<int[]> blockWinMoves = new ArrayList<>();
        List<int[]> winMoves = new ArrayList<>();
        List<int[]> captureMoves = new ArrayList<>();
        List<int[]> openTesseraMoves = new ArrayList<>();
        List<int[]> stretchFourMoves = new ArrayList<>();
        List<int[]> openTriaMoves = new ArrayList<>();
        List<int[]> stretchThreeMoves = new ArrayList<>();
        List<int[]> openTwoMoves = new ArrayList<>();
        List<int[]> blockTesseraMoves = new ArrayList<>();
        List<int[]> blockTriaMoves = new ArrayList<>();
        List<int[]> stretchTwoMoves = new ArrayList<>();
        List<int[]> blockTwoMoves = new ArrayList<>();
        List<int[]> otherMoves = new ArrayList<>();

        for (int[] move : validMoves) {
            int row = move[0], col = move[1];

            board[row][col] = player;

            char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 4, 0), Math.min(col + 4, 19) + 1);
            String plusFourHorizontal = String.valueOf(temp);
            String plusFourVertical = String.valueOf(getColSegment(board, row, col, 4));
            String plusFourDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
            String plusFourDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));

            temp = Arrays.copyOfRange(board[row], Math.max(col - 5, 0), Math.min(col + 5, 19) + 1);
            String plusFiveHorizontal = String.valueOf(temp);
            String plusFiveVertical = String.valueOf(getColSegment(board, row, col, 5));
            String plusFiveDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 5));
            String plusFiveDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 5));

            temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col + 3, 19) + 1);
            String plusThreeHorizontal = String.valueOf(temp);
            String plusThreeVertical = String.valueOf(getColSegment(board, row, col, 3));
            String plusThreeDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
            String plusThreeDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));

            temp = Arrays.copyOfRange(board[row], Math.max(col - 2, 0), Math.min(col + 2, 19) + 1);
            String plusTwoHorizontal = String.valueOf(temp);
            String plusTwoVertical = String.valueOf(getColSegment(board, row, col, 2));
            String plusTwoDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 2));
            String plusTwoDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 2));

            temp = Arrays.copyOfRange(board[row], Math.max(col - 1, 0), Math.min(col + 1, 19) + 1);
            String plusOneHorizontal = String.valueOf(temp);
            String plusOneVertical = String.valueOf(getColSegment(board, row, col, 1));
            String plusOneDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 1));
            String plusOneDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 1));


            if(isWinningMove(board, move, player)) {
                winMoves.add(move);
            } else if (isCaptureMove(plusThreeHorizontal, plusThreeVertical, plusThreeDiagonalRight, plusThreeDiagonalLeft)) {
                captureMoves.add(move);
            } else if (isBlockWinMove(plusFiveHorizontal, plusFiveVertical, plusFiveDiagonalRight, plusFiveDiagonalLeft)) {
                blockWinMoves.add(move);
            } else if (isOpenTesseraMove(plusFourHorizontal, plusFourVertical, plusFourDiagonalRight, plusFourDiagonalLeft)) {
                openTesseraMoves.add(move);
            } else if (isStretchFourMove(plusFourHorizontal, plusFourVertical, plusFourDiagonalRight, plusFourDiagonalLeft)) {
                stretchFourMoves.add(move);
            } else if (isOpenTriaMove(plusThreeHorizontal, plusThreeVertical, plusThreeDiagonalRight, plusThreeDiagonalLeft)) {
                openTriaMoves.add(move);
            } else if (isStretchTriaMove(plusThreeHorizontal, plusThreeVertical, plusThreeDiagonalRight, plusThreeDiagonalLeft)) {
                stretchThreeMoves.add(move);
            } else if (isOpenTwoMove(plusTwoHorizontal, plusTwoVertical, plusTwoDiagonalRight, plusTwoDiagonalLeft)) {
                openTwoMoves.add(move);
            } else if (isBlockTesseraMove(plusThreeHorizontal, plusThreeVertical, plusThreeDiagonalRight, plusThreeDiagonalLeft)) {
                blockTesseraMoves.add(move);
            } else if (isBlockTriaMove(plusTwoHorizontal, plusTwoVertical, plusTwoDiagonalRight, plusTwoDiagonalLeft)) {
                blockTriaMoves.add(move);
            } else if (isStretchTwoMove(plusTwoHorizontal, plusTwoVertical, plusTwoDiagonalRight, plusTwoDiagonalLeft)) {
                stretchTwoMoves.add(move);
            } else if (isBlockTwoMove(plusOneHorizontal, plusOneVertical, plusOneDiagonalRight, plusOneDiagonalLeft)) {
                blockTwoMoves.add(move);
            } else {
                otherMoves.add(move);
            }
            board[row][col] = '.';
        }

        /*// Sort moves in each category based on their value
        Collections.sort(captureMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numCaptured1 = numStonesCaptured(board, move1);
                int numCaptured2 = numStonesCaptured(board, move2);
                return Integer.compare(numCaptured2, numCaptured1);
            }
        });

        Collections.sort(openTriaMoves, new Comparator<int[]>() {
            @Override
            public int compare(int[] move1, int[] move2) {
                int numOpenThrees1 = numOpenThreesCreated(board, move1);
                int numOpenThrees2 = numOpenThreesCreated(board, move2);
                return Integer.compare(numOpenThrees2, numOpenThrees1);
            }
        });

        Collections.sort(blockTriaMoves, new Comparator<int[]>() {
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
        });*/

        // Combine all the sorted lists
        validMoves.clear();
        validMoves.addAll(winMoves);
        validMoves.addAll(blockWinMoves);
        validMoves.addAll(captureMoves);
        validMoves.addAll(openTesseraMoves);
        validMoves.addAll(stretchFourMoves);
        validMoves.addAll(openTriaMoves);
        validMoves.addAll(stretchThreeMoves);
        validMoves.addAll(openTwoMoves);
        validMoves.addAll(blockTesseraMoves);
        validMoves.addAll(blockTriaMoves);
        validMoves.addAll(stretchTwoMoves);
        validMoves.addAll(blockTwoMoves);
        validMoves.addAll(otherMoves);
    }

    private static boolean isBlockWinMove(String plusFiveHorizontal, String plusFiveVertical, String plusFiveDiagonalRight, String plusFiveDiagonalLeft) {
        String[] patterns = getBlockWinPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= plusFiveHorizontal.contains(s) || plusFiveVertical.contains(s)
                    || plusFiveDiagonalRight.contains(s) || plusFiveDiagonalLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isOpenTesseraMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getOpenTesseraPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isStretchFourMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getStretchFourPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isOpenTriaMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getOpenTriaPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }
    private static boolean isStretchTriaMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getStretchThreePatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isOpenTwoMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getOpenTwoPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isBlockTesseraMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockTesseraPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isBlockTriaMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockTriaPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isBlockTwoMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockTwoPatterns();
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }

    private static boolean isStretchTwoMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getStretchTwoPatterns(player);
        boolean ans = false;
        for(String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if(ans) return true;
        }
        return ans;
    }


    private static boolean isCaptureMove(String plusThreeHorizontal, String plusThreeVertical, String plusThreeDiagonalRight, String plusThreeDiagonalLeft) {
        String pattern = getCapturePattern(player);
        return plusThreeHorizontal.contains(pattern) || plusThreeVertical.contains(pattern)
                || plusThreeDiagonalRight.contains(pattern) || plusThreeDiagonalLeft.contains(pattern);
    }

    private static void getUpdatedBoardScore(int[][] updatedMove) {

    }

    private static int[][] getUpdatedMove() {
        return new int[][]{{1, 2}};
    }




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