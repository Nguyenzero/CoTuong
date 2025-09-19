package sample;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;

import java.util.HashMap;
import java.util.Map;

public class BoardController {
    @FXML
    private Pane chessBoard;

    private final Group boardLayer = new Group();
    private final Group piecesLayer = new Group();
    // NEW: labels layer
    private final Group hudLayer = new Group();
    private final Text topSideLabel = new Text();
    private final Text bottomSideLabel = new Text();

    private GameClient client;

    private enum Side { RED, BLACK }
    private enum PieceType { KING, ADVISOR, ELEPHANT, HORSE, ROOK, CANNON, SOLDIER }

    private static class Piece {
        final String id;
        final PieceType type;
        final Side side;
        Piece(String id, PieceType type, Side side) {
            this.id = id; this.type = type; this.side = side;
        }
    }

    // UI nodes of pieces by id
    private final Map<String, StackPane> pieceNodes = new HashMap<>();
    // Board model: rows 0..9 (top->bottom), cols 0..8 (left->right)
    private final Piece[][] board = new Piece[10][9];

    private Side mySide = null;           // assigned by server
    private Side currentTurn = Side.RED;  // RED starts
    // NEW: view orientation (true when player is BLACK → BLACK at bottom)
    private boolean flipY = false;

    private Piece selectedPiece = null;
    private int selectedRow = -1, selectedCol = -1;

    // Board metrics
    private static final double MARGIN_RATIO = 0.06; // 6% margin
    private double x0, y0, x1, y1, cellW, cellH;

    // Block input after victory
    private boolean gameOver = false;

    @FXML
    public void initialize() {
        try {
            client = new GameClient("localhost", 12345);
            client.listen(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        chessBoard.getChildren().addAll(boardLayer, piecesLayer, hudLayer);
        // NEW: init HUD labels
        hudLayer.getChildren().addAll(topSideLabel, bottomSideLabel);
        topSideLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        bottomSideLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        setupInitialBoard();

        // Board click handler (snap to nearest intersection)
        chessBoard.setOnMouseClicked(this::handleBoardClick);

        // Relayout on resize
        chessBoard.widthProperty().addListener((o, a, b) -> layoutAll());
        chessBoard.heightProperty().addListener((o, a, b) -> layoutAll());
        javafx.application.Platform.runLater(this::layoutAll);
    }

    // Called by GameClient when server assigns a side
    public void setPlayerSide(String sideStr) {
        this.mySide = "RED".equalsIgnoreCase(sideStr) ? Side.RED : Side.BLACK;
        // NEW: flip when BLACK so BLACK is at the bottom on that client
        this.flipY = (this.mySide == Side.BLACK);
        updateSideLabels();
        layoutAll();
        System.out.println("🎮 You are " + this.mySide);
    }

    // Receive a move from server and apply to board+UI
    public void movePiece(String pieceId, int fromCol, int fromRow, int toCol, int toRow) {
        System.out.println("♟ Update: " + pieceId + " ("+fromCol+","+fromRow+") → ("+toCol+","+toRow+")");
        Piece p = board[fromRow][fromCol];
        if (p == null || !p.id.equals(pieceId)) {
            // Out-of-sync fallback: try to find by id
            p = findPieceById(pieceId);
            if (p == null) return;
            int[] pos = findPiecePos(p);
            if (pos == null) return;
            fromRow = pos[0]; fromCol = pos[1];
        }

        // Capture if any
        Piece target = board[toRow][toCol];
        if (target != null) {
            StackPane targetNode = pieceNodes.get(target.id);
            if (targetNode != null) {
                piecesLayer.getChildren().remove(targetNode);
            }
            pieceNodes.remove(target.id);
            if (target.type == PieceType.KING) {
                // Declare winner and lock the board
                gameOver = true;
                // p.side is the winner
                String winLabel = (p.side == Side.RED) ? "ĐỎ" : "ĐEN";
                WinnerDialog.showWinner(winLabel, mySide == p.side);
            }
        }

        // Move UI node -> reposition by pixel at intersections
        StackPane node = pieceNodes.get(p.id);
        if (node != null) {
            positionPieceNode(node, toCol, toRow);
        }

        // Update model
        board[fromRow][fromCol] = null;
        board[toRow][toCol] = p;

        clearSelection();
        if (!gameOver) toggleTurn();
    }

    // -------------------- Setup --------------------

    private void setupInitialBoard() {
        // Black (top)
        place(PieceType.ROOK,   Side.BLACK, 0,0, "B_R1");
        place(PieceType.HORSE,  Side.BLACK, 1,0, "B_H1");
        place(PieceType.ELEPHANT,Side.BLACK,2,0, "B_E1");
        place(PieceType.ADVISOR,Side.BLACK,3,0, "B_A1");
        place(PieceType.KING,   Side.BLACK, 4,0, "B_K");
        place(PieceType.ADVISOR,Side.BLACK,5,0, "B_A2");
        place(PieceType.ELEPHANT,Side.BLACK,6,0, "B_E2");
        place(PieceType.HORSE,  Side.BLACK, 7,0, "B_H2");
        place(PieceType.ROOK,   Side.BLACK, 8,0, "B_R2");

        place(PieceType.CANNON, Side.BLACK, 1,2, "B_C1");
        place(PieceType.CANNON, Side.BLACK, 7,2, "B_C2");

        place(PieceType.SOLDIER,Side.BLACK, 0,3, "B_S1");
        place(PieceType.SOLDIER,Side.BLACK, 2,3, "B_S2");
        place(PieceType.SOLDIER,Side.BLACK, 4,3, "B_S3");
        place(PieceType.SOLDIER,Side.BLACK, 6,3, "B_S4");
        place(PieceType.SOLDIER,Side.BLACK, 8,3, "B_S5");

        // Red (bottom)
        place(PieceType.ROOK,   Side.RED, 0,9, "R_R1");
        place(PieceType.HORSE,  Side.RED, 1,9, "R_H1");
        place(PieceType.ELEPHANT,Side.RED,2,9, "R_E1");
        place(PieceType.ADVISOR,Side.RED,3,9, "R_A1");
        place(PieceType.KING,   Side.RED, 4,9, "R_K");
        place(PieceType.ADVISOR,Side.RED,5,9, "R_A2");
        place(PieceType.ELEPHANT,Side.RED,6,9, "R_E2");
        place(PieceType.HORSE,  Side.RED, 7,9, "R_H2");
        place(PieceType.ROOK,   Side.RED, 8,9, "R_R2");

        place(PieceType.CANNON, Side.RED, 1,7, "R_C1");
        place(PieceType.CANNON, Side.RED, 7,7, "R_C2");

        place(PieceType.SOLDIER,Side.RED, 0,6, "R_S1");
        place(PieceType.SOLDIER,Side.RED, 2,6, "R_S2");
        place(PieceType.SOLDIER,Side.RED, 4,6, "R_S3");
        place(PieceType.SOLDIER,Side.RED, 6,6, "R_S4");
        place(PieceType.SOLDIER,Side.RED, 8,6, "R_S5");
    }

    private void place(PieceType type, Side side, int col, int row, String id) {
        Piece p = new Piece(id, type, side);
        board[row][col] = p;

        StackPane node = createPieceNode(p);
        piecesLayer.getChildren().add(node);
        pieceNodes.put(p.id, node);

        positionPieceNode(node, col, row);
    }

    private StackPane createPieceNode(Piece p) {
        Color color = (p.side == Side.RED) ? Color.RED : Color.BLACK;

        Circle circle = new Circle(25, Color.BEIGE);
        circle.setStroke(color);
        circle.setStrokeWidth(2);

        Text text = new Text(labelFor(p.type));
        text.setFill(color);

        StackPane piece = new StackPane(circle, text);
        piece.setPickOnBounds(false);

        piece.setOnMouseClicked(e -> {
            int[] pos = findPiecePos(p);
            if (pos != null) {
                int row = pos[0], col = pos[1];
                handleCellClick(col, row);
            }
            e.consume();
        });
        return piece;
    }

    private String labelFor(PieceType t) {
        switch (t) {
            case KING: return "Tướng";
            case ADVISOR: return "Sĩ";
            case ELEPHANT: return "Tượng";
            case HORSE: return "Mã";
            case ROOK: return "Xe";
            case CANNON: return "Pháo";
            case SOLDIER: return "Tốt";
            default: return "?";
        }
    }

    // -------------------- Interaction --------------------

    private void handleBoardClick(MouseEvent e) {
        if (gameOver) return;
        // snap mouse to nearest intersection, considering margins
        if (cellW <= 0 || cellH <= 0) return;
        double mx = e.getX(), my = e.getY();
        int col = (int) Math.round((mx - x0) / cellW);
        int rowDisplay = (int) Math.round((my - y0) / cellH);
        col = Math.max(0, Math.min(8, col));
        rowDisplay = Math.max(0, Math.min(9, rowDisplay));
        // NEW: map display row to model row when flipped
        int row = flipY ? (9 - rowDisplay) : rowDisplay;
        handleCellClick(col, row);
    }

    private void handleCellClick(int col, int row) {
        if (gameOver) return;
        if (mySide == null) return;                   // wait for assignment
        if (currentTurn != mySide) {                  // not your turn
            return;
        }

        Piece at = board[row][col];
        if (selectedPiece == null) {
            if (at != null && at.side == mySide) {
                setSelection(at, row, col);
            }
            return;
        }

        // Clicking own piece -> change selection
        if (at != null && at.side == mySide) {
            setSelection(at, row, col);
            return;
        }

        // Try move
        if (isLegalMove(selectedPiece, selectedRow, selectedCol, row, col)) {
            // Send to server; wait for broadcast to apply
            client.sendMove(selectedPiece.id, selectedCol, selectedRow, col, row);
            // Keep selection until broadcast resolves (or clear it)
            clearSelection();
        }
    }

    private void setSelection(Piece p, int row, int col) {
        clearSelection();
        selectedPiece = p;
        selectedRow = row;
        selectedCol = col;
        StackPane node = pieceNodes.get(p.id);
        if (node != null) {
            Circle c = (Circle) node.getChildren().get(0);
            c.setStrokeWidth(4);
            c.setStroke(Color.ORANGE);
        }
    }

    private void clearSelection() {
        if (selectedPiece != null) {
            StackPane node = pieceNodes.get(selectedPiece.id);
            if (node != null) {
                Circle c = (Circle) node.getChildren().get(0);
                c.setStrokeWidth(2);
                c.setStroke(selectedPiece.side == Side.RED ? Color.RED : Color.BLACK);
            }
        }
        selectedPiece = null;
        selectedRow = -1;
        selectedCol = -1;
    }

    private void toggleTurn() {
        currentTurn = (currentTurn == Side.RED) ? Side.BLACK : Side.RED;
    }

    // -------------------- Move rules --------------------

    private boolean isLegalMove(Piece p, int fr, int fc, int tr, int tc) {
        if (fr == tr && fc == tc) return false;
        if (!inBounds(tr, tc)) return false;

        Piece dest = board[tr][tc];
        if (dest != null && dest.side == p.side) return false;

        int dr = tr - fr, dc = tc - fc;
        int adr = Math.abs(dr), adc = Math.abs(dc);

        switch (p.type) {
            case ROOK:
                if (fr != tr && fc != tc) return false;
                return countBetweenStraight(fr, fc, tr, tc) == 0 && !kingsFaceAfterMove(p, fr, fc, tr, tc);
            case CANNON:
                if (fr != tr && fc != tc) return false;
                int between = countBetweenStraight(fr, fc, tr, tc);
                if (dest == null) {
                    if (between != 0) return false;
                } else {
                    if (between != 1) return false;
                }
                return !kingsFaceAfterMove(p, fr, fc, tr, tc);
            case HORSE:
                if (!((adr == 2 && adc == 1) || (adr == 1 && adc == 2))) return false;
                // horse leg
                if (adr == 2) {
                    int blockR = fr + Integer.signum(dr);
                    if (board[blockR][fc] != null) return false;
                } else {
                    int blockC = fc + Integer.signum(dc);
                    if (board[fr][blockC] != null) return false;
                }
                return !kingsFaceAfterMove(p, fr, fc, tr, tc);
            case ELEPHANT:
                if (!(adr == 2 && adc == 2)) return false;
                // elephant eye
                if (board[fr + dr / 2][fc + dc / 2] != null) return false;
                // cannot cross river
                if (p.side == Side.RED && tr < 5) return false;
                if (p.side == Side.BLACK && tr > 4) return false;
                return !kingsFaceAfterMove(p, fr, fc, tr, tc);
            case ADVISOR:
                if (!(adr == 1 && adc == 1)) return false;
                if (!inPalace(p.side, tr, tc)) return false;
                return !kingsFaceAfterMove(p, fr, fc, tr, tc);
            case KING:
                if (!((adr == 1 && adc == 0) || (adr == 0 && adc == 1))) {
                    // flying general capture
                    if (fc == tc && dest != null && dest.type == PieceType.KING && countBetweenStraight(fr, fc, tr, tc) == 0) {
                        // allowed as special case
                    } else {
                        return false;
                    }
                }
                if (!inPalace(p.side, tr, tc)) return false;
                return !kingsFaceAfterMove(p, fr, fc, tr, tc);
            case SOLDIER:
                // forward direction
                int step = (p.side == Side.RED) ? -1 : 1;
                if (dc == 0 && dr == step) {
                    return !kingsFaceAfterMove(p, fr, fc, tr, tc);
                }
                // after river, can move sideways by 1
                boolean crossed = (p.side == Side.RED) ? (fr <= 4) : (fr >= 5);
                if (crossed && adr == 0 && Math.abs(dc) == 1) {
                    return !kingsFaceAfterMove(p, fr, fc, tr, tc);
                }
                return false;
            default:
                return false;
        }
    }

    private boolean inBounds(int r, int c) { return r >= 0 && r < 10 && c >= 0 && c < 9; }

    private boolean inPalace(Side side, int r, int c) {
        if (c < 3 || c > 5) return false;
        if (side == Side.RED) return r >= 7 && r <= 9;
        return r >= 0 && r <= 2;
    }

    private int countBetweenStraight(int fr, int fc, int tr, int tc) {
        if (fr == tr) {
            int min = Math.min(fc, tc) + 1, max = Math.max(fc, tc) - 1, cnt = 0;
            for (int c = min; c <= max; c++) if (board[fr][c] != null) cnt++;
            return cnt;
        }
        if (fc == tc) {
            int min = Math.min(fr, tr) + 1, max = Math.max(fr, tr) - 1, cnt = 0;
            for (int r = min; r <= max; r++) if (board[r][fc] != null) cnt++;
            return cnt;
        }
        return -1;
    }

    private boolean kingsFaceAfterMove(Piece p, int fr, int fc, int tr, int tc) {
        // simulate move
        Piece fromSave = board[fr][fc];
        Piece toSave = board[tr][tc];
        board[fr][fc] = null;
        board[tr][tc] = fromSave;

        boolean facing = kingsFace();

        // revert
        board[fr][fc] = fromSave;
        board[tr][tc] = toSave;
        return facing;
    }

    private boolean kingsFace() {
        int redR = -1, redC = -1, blackR = -1, blackC = -1;
        for (int r = 0; r < 10; r++) {
            if (board[r][4] != null && board[r][4].type == PieceType.KING) {
                if (board[r][4].side == Side.RED) { redR = r; redC = 4; }
                else { blackR = r; blackC = 4; }
            }
        }
        if (redC == -1 || blackC == -1) return false;
        if (redC != blackC) return false;
        int c = redC;
        int min = Math.min(redR, blackR) + 1, max = Math.max(redR, blackR) - 1;
        for (int r = min; r <= max; r++) if (board[r][c] != null) return false;
        return true;
    }

    private Piece findPieceById(String id) {
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 9; c++)
                if (board[r][c] != null && board[r][c].id.equals(id)) return board[r][c];
        return null;
    }

    private int[] findPiecePos(Piece p) {
        for (int r = 0; r < 10; r++)
            for (int c = 0; c < 9; c++)
                if (board[r][c] == p) return new int[]{r, c};
        return null;
    }

    // -------------------- Drawing and layout --------------------

    private void layoutAll() {
        double w = chessBoard.getWidth();
        double h = chessBoard.getHeight();
        if (w <= 0 || h <= 0) return;

        double margin = Math.min(w, h) * MARGIN_RATIO;
        x0 = margin; y0 = margin;
        x1 = w - margin; y1 = h - margin;
        cellW = (x1 - x0) / 8.0;
        cellH = (y1 - y0) / 9.0;

        drawBoard();
        relayoutAllPieces();
        placeSideLabels(margin);
    }

    private void drawBoard() {
        boardLayer.getChildren().clear();

        // Border
        boardLayer.getChildren().addAll(
                new Line(x0, y0, x1, y0),
                new Line(x0, y1, x1, y1),
                new Line(x0, y0, x0, y1),
                new Line(x1, y0, x1, y1)
        );

        // Horizontal lines (10 ranks)
        for (int r = 0; r <= 9; r++) {
            double y = y0 + r * cellH;
            boardLayer.getChildren().add(new Line(x0, y, x1, y));
        }

        // Vertical lines (9 files) with river break between rows 4 and 5 for inner files
        for (int c = 0; c <= 8; c++) {
            double x = x0 + c * cellW;
            if (c == 0 || c == 8) {
                boardLayer.getChildren().add(new Line(x, y0, x, y1));
            } else {
                // top half
                boardLayer.getChildren().add(new Line(x, y0, x, y0 + 4 * cellH));
                // bottom half
                boardLayer.getChildren().add(new Line(x, y0 + 5 * cellH, x, y1));
            }
        }

        // Palaces (diagonals)
        addPalaceDiagonals(3, 0);
        addPalaceDiagonals(3, 7);
    }

    private void addPalaceDiagonals(int cStart, int rStart) {
        double xA = x0 + cStart * cellW;
        double xB = x0 + (cStart + 2) * cellW;
        double yA = y0 + rStart * cellH;
        double yB = y0 + (rStart + 2) * cellH;
        boardLayer.getChildren().addAll(
                new Line(xA, yA, xB, yB),
                new Line(xB, yA, xA, yB)
        );
    }

    private void relayoutAllPieces() {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                Piece p = board[r][c];
                if (p == null) continue;
                StackPane node = pieceNodes.get(p.id);
                if (node != null) positionPieceNode(node, c, r);
            }
        }
    }

    private void positionPieceNode(StackPane node, int col, int row) {
        // NEW: map model row to display row when flipped
        int rowDisplay = flipY ? (9 - row) : row;
        double cx = x0 + col * cellW;
        double cy = y0 + rowDisplay * cellH;

        double d = Math.min(cellW, cellH) * 0.8; // piece diameter
        node.setPrefSize(d, d);
        node.relocate(cx - d / 2.0, cy - d / 2.0);

        // Scale circle/text to fit
        if (node.getChildren().get(0) instanceof Circle) {
            Circle circle = (Circle) node.getChildren().get(0);
            circle.setRadius(d / 2.0);
        }
        if (node.getChildren().get(1) instanceof Text) {
            Text text = (Text) node.getChildren().get(1);
            text.setStyle("-fx-font-size: " + Math.round(d * 0.33) + "px;");
        }
    }

    // NEW: labels helper
    private void updateSideLabels() {
        if (mySide == null) return;
        Color myColor = (mySide == Side.RED) ? Color.RED : Color.BLACK;
        Color oppColor = (mySide == Side.RED) ? Color.BLACK : Color.RED;

        bottomSideLabel.setText(mySide == Side.RED ? "ĐỎ" : "ĐEN");
        topSideLabel.setText(mySide == Side.RED ? "ĐEN" : "ĐỎ");

        bottomSideLabel.setFill(myColor);
        topSideLabel.setFill(oppColor);
    }

    private void placeSideLabels(double margin) {
        if (mySide == null) {
            bottomSideLabel.setText("ĐỎ");
            topSideLabel.setText("ĐEN");
            bottomSideLabel.setFill(Color.RED);
            topSideLabel.setFill(Color.BLACK);
        }
        double centerX = (x0 + x1) / 2.0;

        bottomSideLabel.setX(centerX - bottomSideLabel.getLayoutBounds().getWidth() / 2.0);
        bottomSideLabel.setY(y1 - margin * 0.4);

        topSideLabel.setX(centerX - topSideLabel.getLayoutBounds().getWidth() / 2.0);
        topSideLabel.setY(y0 + margin * 0.8);
    }
}
