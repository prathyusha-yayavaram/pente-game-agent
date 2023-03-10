import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

import java.io.*;
import java.util.*;



public class calibrate {

    //Parse from playtest.txt or create new only if you cant find any data
    static BoardStatus boardStatus = new BoardStatus(1, 19, 1, 19, 0, 0, 0, 0);
    static final String BLACK = "BLACK";
    static final String WHITE = "WHITE";

    //Scores
    static final int captureScore = 10000;
    static final int openTesseraScore = 10000;
    static final int stretchFourScore = 5000;
    static final int openTriaScore = 1000;
    static final int stretchTriaScore = 500;
    static final int openTwoScore = 200;
    static final int stretchTwoScore = 10;

    static int[] preCalculatedFinalMove = new int[2];
    static char player;
    static char opponent;
    static List<int[]> validMoves = new ArrayList<>();

    static char[][] board = {
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', 'b', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', 'b', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', 'w', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', 'w', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'},
        {'.','.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.'}
    };

    static HashMap<Integer, BoardNode> nodeData = new HashMap<>();

    public static void main(String[] args) throws IOException {
        // Read input from file
        String color = "";
        double timeRemaining = 0.0;
        int whiteCaptured = 0;
        int blackCaptured = 0;

        File file = new File(System.getProperty("user.dir") + "/src/input.txt");
        Scanner scanner = new Scanner(file);

        PrintWriter output = new PrintWriter(new File("calibrate.txt"));
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

        long startTime = System.currentTimeMillis();
        updateBoardStatus(board);
        findNextMove(color, 3);
        long endTime = System.currentTimeMillis();
        output.println(endTime - startTime);

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

    static int[] findNextMove(String color, int depth) {
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
        getLegalMoves();
        boolean isBlockOpenTesseraMoveExists = sortValidMoves(board, validMoves, player);
        if(isBlockOpenTesseraMoveExists) {
            return preCalculatedFinalMove;
        }
        BoardNode rootNode = alphaBeta(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, boardStatus.noOfPlayerCaptures, boardStatus.noOfOpponentCaptures, true, validMoves);

        return (rootNode.bestMove[0] == 0 && rootNode.bestMove[1] == 0) ? validMoves.get(0) : rootNode.bestMove;
    }

    private static void getLegalMoves() {
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == '.') {
                    validMoves.add(new int[]{i, j});
                }
            }
        }
    }


    public static BoardNode alphaBeta(char[][] board, int depth, int alpha, int beta, int playerCaptures, int opponentCaptures, boolean isMaximizingPlayer, List<int[]> validMoves) {
        char curPlayer = isMaximizingPlayer ? player : opponent;
        int hashKey = generateHashKey(curPlayer, board);
        if(nodeData.containsKey(hashKey)) {
            return nodeData.get(hashKey);
        }
        if (playerCaptures == 5) {
            BoardNode node = new BoardNode(hashKey, null, Integer.MAX_VALUE);
            nodeData.put(hashKey, node);
            return node;
        }
        if (opponentCaptures == 5) {
            BoardNode node = new BoardNode(hashKey, null, Integer.MIN_VALUE);
            nodeData.put(hashKey, node);
            return node;
        }

        if (depth == 0) {
            return new BoardNode(hashKey, null, evaluate(board, playerCaptures, opponentCaptures));
        }

        if (isMaximizingPlayer) {
            int bestScore = Integer.MIN_VALUE;
            int[] bestMove = new int[2];
            for (int[] move : validMoves) {
                if (board[move[0]][move[1]] != '.') continue;
                int score;
                char[][] newBoard = makeMove(board, move, player);
                int newCaptures = getNoOfCaptures(newBoard, move, player);
                playerCaptures += newCaptures;
                if (isWinningMove(newBoard, move, player)) {
                    BoardNode node = new BoardNode(hashKey, move, Integer.MAX_VALUE);
                    nodeData.put(hashKey, node);
                    return node;
                } else {
                    score = alphaBeta(newBoard, depth - 1, alpha, beta, playerCaptures, opponentCaptures, false, validMoves).score;
                }
                if (score > bestScore) {
                    bestMove = move;
                }

                bestScore = Math.max(score, bestScore);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) {
                    break;
                }
                playerCaptures -= newCaptures;
            }

            return new BoardNode(hashKey, bestMove, bestScore);
        } else {
            int bestScore = Integer.MAX_VALUE;
            int[] bestMove = new int[2];
            for (int[] move : validMoves) {
                if (board[move[0]][move[1]] != '.') continue;
                int score;
                char[][] newBoard = makeMove(board, move, opponent);
                int newCaptures = getNoOfCaptures(newBoard, move, opponent);
                opponentCaptures += newCaptures;
                if (isWinningMove(newBoard, move, opponent)) {
                    BoardNode node = new BoardNode(hashKey, move, Integer.MIN_VALUE);
                    nodeData.put(hashKey, node);
                    return node;
                } else {
                    score = alphaBeta(newBoard, depth - 1, alpha, beta, playerCaptures, opponentCaptures, true, validMoves).score;
                }
                if (score < bestScore) {
                    bestMove = move;
                }
                bestScore = Math.min(score, bestScore);
                beta = Math.min(beta, score);
                if (beta <= alpha) {
                    break;
                }
                opponentCaptures -= newCaptures;
            }
            BoardNode node = new BoardNode(hashKey, bestMove, bestScore);
            nodeData.put(hashKey, node);
            return node;
        }
    }

    public static int generateHashKey(char c, char[][] board) {
        StringBuilder sb = new StringBuilder();
        for (char[] row : board) {
            sb.append(Arrays.toString(row));
        }
        sb.append(c);
        return sb.toString().hashCode();
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
        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col, 19) + 1);
        String horizontalLeft = String.valueOf(temp).trim();
        temp = Arrays.copyOfRange(board[row], Math.max(col, 0), Math.min(col + 3, 19) + 1);
        String horizontalRight = String.valueOf(temp).trim();
        String verticalTop = String.valueOf(getColSegmentTop(board, row, col, 3)).trim();
        String verticalBottom = String.valueOf(getColSegmentBottom(board, row, col, 3)).trim();
        String diagRightTop = String.valueOf(getDiagRightSegmentTop(board, row, col, 3)).trim();
        String diagRightBottom = String.valueOf(getDiagRightSegmentBottom(board, row, col, 3)).trim();
        String diagLeftTop = String.valueOf(getDiagLeftSegmentTop(board, row, col, 3)).trim();
        String diagLeftBottom = String.valueOf(getDiagLeftSegmentBottom(board, row, col, 3)).trim();
        int ans = 0;
        //Capture Move
        //TODO: Remove the captured coins and update the board
        if (horizontalLeft.length() == 4) {
            if(horizontalLeft.contains(getCapturePattern(currentPlayer))) {
                board[row][col-1] = '.';
                board[row][col-2] = '.';
                ans++;
            }
        }
        if (horizontalRight.length() == 4) {
            if(horizontalRight.contains(getCapturePattern(currentPlayer))) {
                board[row][col+1] = '.';
                board[row][col+2] = '.';
                ans++;
            }
        }
        if (verticalTop.length() == 4) {
            if(verticalTop.contains(getCapturePattern(currentPlayer))) {
                board[row-1][col] = '.';
                board[row-2][col] = '.';
                ans++;
            }
        }
        if (verticalBottom.length() == 4) {
            if(verticalBottom.contains(getCapturePattern(currentPlayer))) {
                board[row+1][col] = '.';
                board[row+2][col] = '.';
                ans++;
            }
        }
        if (diagRightTop.length() == 4) {
            if(diagRightTop.contains(getCapturePattern(currentPlayer))) {
                board[row-1][col-1] = '.';
                board[row-2][col-2] = '.';
                ans++;
            }
        }
        if (diagRightBottom.length() == 4) {
            if(diagRightBottom.contains(getCapturePattern(currentPlayer))) {
                board[row+1][col+1] = '.';
                board[row+2][col+2] = '.';
                ans++;
            }
        }
        if (diagLeftTop.length() == 4) {
            if(diagLeftTop.contains(getCapturePattern(currentPlayer))) {
                board[row+1][col-1] = '.';
                board[row+2][col-2] = '.';
                ans++;
            }
        }
        if (diagLeftBottom.length() == 4) {
            if(diagLeftBottom.contains(getCapturePattern(currentPlayer))) {
                board[row-1][col+1] = '.';
                board[row-2][col+2] = '.';
                ans++;
            }
        }
        return ans;
    }

    private static char[][] makeMove(char[][] board, int[] move, char player) {
        char[][] newBoard = new char[20][20];
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                newBoard[i][j] = board[i][j];
            }
        }
        newBoard[move[0]][move[1]] = player;
        return newBoard;
    }

    public static int evaluate(char[][] board, int playerCaptures, int opponentCaptures) {
        //Assuming five captures is handled before calling this function
        int playerScore = playerCaptures * captureScore;
        int opponentScore = opponentCaptures * captureScore;

        // calculate score based on stone counts
        for (int i = 1; i <= 19; i++) {
            for (int j = 1; j <= 19; j++) {
                if (board[i][j] == player) {
                    playerScore += getStoneScore(board, i, j, player);
                } else if (board[i][j] == opponent) {
                    opponentScore += getStoneScore(board, i, j, opponent);
                }
                if (playerScore == Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                } else if (opponentScore == Integer.MIN_VALUE) {
                    return Integer.MIN_VALUE;
                }
            }
        }
        // return combined score
        return playerScore - opponentScore;
    }

    private static int getStoneScore(char[][] board, int row, int col, char currentPlayer) {
        int score = 0;

        char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 4, 0), Math.min(col + 4, 19) + 1);
        String plusFourHorizontal = String.valueOf(temp);
        String plusFourVertical = String.valueOf(getColSegment(board, row, col, 4));
        String plusFourDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
        String plusFourDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));

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
        if (plusFourHorizontal.contains(str)) {
            return Integer.MAX_VALUE;
        }
        if (plusFourVertical.contains(str)) {
            return Integer.MAX_VALUE;
        }
        if (plusFourDiagonalRight.contains(str)) {
            return Integer.MAX_VALUE;
        }
        if (plusFourDiagonalLeft.contains(str)) {
            return Integer.MAX_VALUE;
        }

        //Open Tessera
        str = getOpenTesseraPattern(currentPlayer);
        boolean isFoundBefore = false;
        if (plusFourHorizontal.contains(str)) {
            isFoundBefore = true;
            score += openTesseraScore;
        }
        if (plusFourVertical.contains(str)) {
            isFoundBefore = true;
            score += openTesseraScore;
        }
        if (plusFourDiagonalRight.contains(str)) {
            isFoundBefore = true;
            score += openTesseraScore;
        }
        if (plusFourDiagonalLeft.contains(str)) {
            isFoundBefore = true;
            score += openTesseraScore;
        }

        //Stretch Four
        String[] tempStringList = getStretchFourPatterns(currentPlayer);
        for (String s : tempStringList) {
            if (plusFourHorizontal.contains(s) && !isFoundBefore) {
                score += stretchFourScore;
            }
            if (plusFourVertical.contains(s) && !isFoundBefore) {
                score += stretchFourScore;
            }
            if (plusFourDiagonalRight.contains(s) && !isFoundBefore) {
                score += stretchFourScore;
            }
            if (plusFourDiagonalLeft.contains(s) && !isFoundBefore) {
                score += stretchFourScore;
            }
        }

        //Open Tria
        str = getOpenTriaPattern(currentPlayer);
        isFoundBefore = false;
        if (plusThreeHorizontal.contains(str)) {
            isFoundBefore = true;
            score += openTriaScore;
        }
        if (plusThreeVertical.contains(str)) {
            isFoundBefore = true;
            score += openTriaScore;
        }
        if (plusThreeDiagonalRight.contains(str)) {
            isFoundBefore = true;
            score += openTriaScore;
        }
        if (plusThreeDiagonalLeft.contains(str)) {
            isFoundBefore = true;
            score += openTriaScore;
        }

        //Stretch Three
        tempStringList = getStretchThreePatterns(currentPlayer);
        for (String s : tempStringList) {
            if (plusThreeHorizontal.contains(s) && !isFoundBefore) {
                score += stretchTriaScore;
            }
            if (plusThreeVertical.contains(s) && !isFoundBefore) {
                score += stretchTriaScore;
            }
            if (plusThreeDiagonalRight.contains(s) && !isFoundBefore) {
                score += stretchTriaScore;
            }
            if (plusThreeDiagonalLeft.contains(s) && !isFoundBefore) {
                score += stretchTriaScore;
            }
        }

        //Open Two
        str = getOpenTwoPattern(currentPlayer);
        isFoundBefore = false;
        for (String s : tempStringList) {
            if (plusTwoHorizontal.contains(s)) {
                isFoundBefore = true;
                score += openTwoScore;
            }
            if (plusTwoVertical.contains(s)) {
                isFoundBefore = true;
                score += openTwoScore;
            }
            if (plusTwoDiagonalRight.contains(s)) {
                isFoundBefore = true;
                score += openTwoScore;
            }
            if (plusTwoDiagonalLeft.contains(s)) {
                isFoundBefore = true;
                score += openTwoScore;
            }
        }

        //Stretch Two
        tempStringList = getStretchTwoPatterns(currentPlayer);
        for (String s : tempStringList) {
            if (plusTwoHorizontal.contains(s) && !isFoundBefore) {
                score += stretchTwoScore;
            }
            if (plusTwoVertical.contains(s) && !isFoundBefore) {
                score += stretchTwoScore;
            }
            if (plusTwoDiagonalRight.contains(s) && !isFoundBefore) {
                score += stretchTwoScore;
            }
            if (plusTwoDiagonalLeft.contains(s) && !isFoundBefore) {
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

    private static String[] getBlockOpenTesseraPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".bbbw.", ".wbbb.", ".bbwb.", ".bwbb."} : new String[]{".wwwb.", ".bwww.", ".wwbw.", ".wbww."};
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
        return currentPlayer == 'w' ? new String[]{"wwwb", "bwww"} : new String[]{"bbbw", "wbbb"};
    }

    private static String getOpenTesseraPattern(char currentPlayer) {
        return currentPlayer == 'w' ? ".wwww." : ".bbbb.";
    }

    private static String[] getStretchFourPatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".wwww", "wwww.", "ww.ww", "w.www", "www.w"} : new String[]{".bbbb", "bbbb.", "bb.bb", "b.bbb", "bbb.b"};
    }

    private static String getOpenTriaPattern(char currentPlayer) {
        return currentPlayer == 'w' ? ".www." : ".bbb.";
    }

    private static String[] getStretchThreePatterns(char currentPlayer) {
        return currentPlayer == 'w' ? new String[]{".www", "www.", "ww.w", "w.ww"} : new String[]{".bbb", "bbb.", "bb.b", "b.bb"};
    }

    private static String getOpenTwoPattern(char currentPlayer) {
        return currentPlayer == 'w' ? ".ww." : ".bb.";
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
        for (int i = row + size, j = col - size; i >= row - size && j <= col + size; i--, j++) {
            if (!isValidPosition(19, i, j))
                continue;
            diagLeftSegment[k++] = board[i][j];
        }
        return diagLeftSegment;
    }

    private static char[] getDiagLeftSegmentTop(char[][] board, int row, int col, int size) {
        char[] diagLeftSegment = new char[11];
        int k = 0;
        for (int i = row + size, j = col - size; i >= row && j <= col; i--, j++) {
            if (!isValidPosition(19, i, j))
                continue;
            diagLeftSegment[k++] = board[i][j];
        }
        return diagLeftSegment;
    }

    private static char[] getDiagLeftSegmentBottom(char[][] board, int row, int col, int size) {
        char[] diagLeftSegment = new char[11];
        int k = 0;
        for (int i = row, j = col; i >= row - size && j <= col + size; i--, j++) {
            if (!isValidPosition(19, i, j))
                continue;
            diagLeftSegment[k++] = board[i][j];
        }
        return diagLeftSegment;
    }


    private static char[] getDiagRightSegment(char[][] board, int row, int col, int size) {
        char[] diagRightSegment = new char[11];
        int k = 0;
        for (int i = row - size, j = col - size; i <= row + size && j <= col + size; i++, j++) {
            if (!isValidPosition(19, i, j))
                continue;
            diagRightSegment[k++] = board[i][j];
        }
        return diagRightSegment;
    }

    private static char[] getDiagRightSegmentTop(char[][] board, int row, int col, int size) {
        char[] diagRightSegment = new char[11];
        int k = 0;
        for (int i = row - size, j = col - size; i <= row && j <= col; i++, j++) {
            if (!isValidPosition(19, i, j))
                continue;
            diagRightSegment[k++] = board[i][j];
        }
        return diagRightSegment;
    }

    private static char[] getDiagRightSegmentBottom(char[][] board, int row, int col, int size) {
        char[] diagRightSegment = new char[11];
        int k = 0;
        for (int i = row, j = col; i <= row + size && j <= col + size; i++, j++) {
            if (!isValidPosition(19, i, j))
                continue;
            diagRightSegment[k++] = board[i][j];
        }
        return diagRightSegment;
    }

    private static char[] getColSegment(char[][] board, int row, int col, int size) {
        char[] colSegment = new char[11];
        int k = 0;
        for (int j = Math.max(0, row - size); j <= Math.min(row + size, 19); j++) {
            colSegment[k++] = board[j][col];
        }
        return colSegment;
    }

    private static char[] getColSegmentTop(char[][] board, int row, int col, int size) {
        char[] colSegment = new char[11];
        int k = 0;
        for (int j = Math.max(0, row - size); j <= row; j++) {
            colSegment[k++] = board[j][col];
        }
        return colSegment;
    }

    private static char[] getColSegmentBottom(char[][] board, int row, int col, int size) {
        char[] colSegment = new char[11];
        int k = 0;
        for (int j = row; j <= Math.min(row + size, 19); j++) {
            colSegment[k++] = board[j][col];
        }
        return colSegment;
    }

    private static boolean isValidPosition(int boardSize, int row, int col) {
        return row >= 1 && row <= boardSize && col >= 1 && col <= boardSize;
    }

    public static boolean sortValidMoves(char[][] board, List<int[]> validMoves, char player) {
        // Separate moves into different categories
        List<int[]> blockWinMoves = new ArrayList<>();
        List<int[]> winMoves = new ArrayList<>();
        List<int[]> blockCaptureMoves = new ArrayList<>();
        List<int[]> captureMoves = new ArrayList<>();
        List<int[]> openTesseraMoves = new ArrayList<>();
        List<int[]> stretchFourMoves = new ArrayList<>();
        List<int[]> openTriaMoves = new ArrayList<>();
        List<int[]> stretchThreeMoves = new ArrayList<>();
        List<int[]> openTwoMoves = new ArrayList<>();
        List<int[]> blockTesseraMoves = new ArrayList<>();
        List<int[]> blockOpenTesseraMoves = new ArrayList<>();
        List<int[]> blockTriaMoves = new ArrayList<>();
        List<int[]> stretchTwoMoves = new ArrayList<>();
        List<int[]> blockTwoMoves = new ArrayList<>();
        List<int[]> otherMoves = new ArrayList<>();
        boolean ans = false;
        for (int[] move : validMoves) {
            int row = move[0], col = move[1];

            board[row][col] = player;

            char[] temp = Arrays.copyOfRange(board[row], Math.max(col - 4, 0), Math.min(col + 4, 19) + 1);
            String plusFourHorizontal = String.valueOf(temp);
            String plusFourVertical = String.valueOf(getColSegment(board, row, col, 4));
            String plusFourDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 4));
            String plusFourDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 4));

            temp = Arrays.copyOfRange(board[row], Math.max(col - 3, 0), Math.min(col + 3, 19) + 1);
            String plusThreeHorizontal = String.valueOf(temp);
            String plusThreeVertical = String.valueOf(getColSegment(board, row, col, 3));
            String plusThreeDiagonalRight = String.valueOf(getDiagRightSegment(board, row, col, 3));
            String plusThreeDiagonalLeft = String.valueOf(getDiagLeftSegment(board, row, col, 3));

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


            if (isWinningMove(board, move, player)) {
                winMoves.add(move);
            } else if (isBlockWinMove(plusFourHorizontal, plusFourVertical, plusFourDiagonalRight, plusFourDiagonalLeft)) {
                ans = true;
                preCalculatedFinalMove[0] = move[0];
                preCalculatedFinalMove[1] = move[1];
                blockWinMoves.add(move);
            } else if (isBlockOpenTesseraMove(plusFourHorizontal, plusFourVertical, plusFourDiagonalRight, plusFourDiagonalLeft)) {
                ans = true;
                preCalculatedFinalMove[0] = move[0];
                preCalculatedFinalMove[1] = move[1];
                blockOpenTesseraMoves.add(move);
            } else if (isCaptureMove(plusThreeHorizontal, plusThreeVertical, plusThreeDiagonalRight, plusThreeDiagonalLeft)) {
                captureMoves.add(move);
            } else if (isBlockCaptureMove(plusThreeHorizontal, plusThreeVertical, plusThreeDiagonalRight, plusThreeDiagonalLeft)) {
                blockCaptureMoves.add(move);
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

        // Combine all the sorted lists
        validMoves.clear();
        validMoves.addAll(winMoves);
        validMoves.addAll(blockWinMoves);
        validMoves.addAll(captureMoves);
        validMoves.addAll(blockCaptureMoves);
        validMoves.addAll(openTesseraMoves);
        validMoves.addAll(stretchFourMoves);
        validMoves.addAll(openTriaMoves);
        validMoves.addAll(stretchThreeMoves);
        validMoves.addAll(openTwoMoves);
        validMoves.addAll(blockOpenTesseraMoves);
        validMoves.addAll(blockTesseraMoves);
        validMoves.addAll(blockTriaMoves);
        validMoves.addAll(stretchTwoMoves);
        validMoves.addAll(blockTwoMoves);
        validMoves.addAll(otherMoves);
        return ans;
    }

    private static boolean isBlockWinMove(String plusFiveHorizontal, String plusFiveVertical, String plusFiveDiagonalRight, String plusFiveDiagonalLeft) {
        String[] patterns = getBlockWinPatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= plusFiveHorizontal.contains(s) || plusFiveVertical.contains(s)
                    || plusFiveDiagonalRight.contains(s) || plusFiveDiagonalLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isBlockCaptureMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockCapturePatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isOpenTesseraMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String s = getOpenTesseraPattern(player);
        return horizontal.contains(s) || vertical.contains(s)
                || diagRight.contains(s) || diagLeft.contains(s);
    }

    private static boolean isStretchFourMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getStretchFourPatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isOpenTriaMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String s = getOpenTriaPattern(player);
        return horizontal.contains(s) || vertical.contains(s)
                || diagRight.contains(s) || diagLeft.contains(s);
    }

    private static boolean isStretchTriaMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getStretchThreePatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isOpenTwoMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String s = getOpenTwoPattern(player);
        return horizontal.contains(s) || vertical.contains(s)
                || diagRight.contains(s) || diagLeft.contains(s);
    }

    private static boolean isBlockTesseraMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockTesseraPatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isBlockOpenTesseraMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockOpenTesseraPatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isBlockTriaMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockTriaPatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isBlockTwoMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getBlockTwoPatterns();
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isStretchTwoMove(String horizontal, String vertical, String diagRight, String diagLeft) {
        String[] patterns = getStretchTwoPatterns(player);
        boolean ans = false;
        for (String s : patterns) {
            ans |= horizontal.contains(s) || vertical.contains(s)
                    || diagRight.contains(s) || diagLeft.contains(s);
            if (ans) return true;
        }
        return ans;
    }

    private static boolean isCaptureMove(String plusThreeHorizontal, String plusThreeVertical, String plusThreeDiagonalRight, String plusThreeDiagonalLeft) {
        String pattern = getCapturePattern(player);
        return plusThreeHorizontal.contains(pattern) || plusThreeVertical.contains(pattern)
                || plusThreeDiagonalRight.contains(pattern) || plusThreeDiagonalLeft.contains(pattern);
    }

    private static void setEmptyCorner(int[] ans) {
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