/*
    CuckooChess - A java chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.cuckoo.engine.guibase;

import java.util.*;

import org.petero.cuckoo.engine.chess.ChessParseError;
import org.petero.cuckoo.engine.chess.ComputerPlayer;
import org.petero.cuckoo.engine.chess.Game;
import org.petero.cuckoo.engine.chess.Game.GameState;
import org.petero.cuckoo.engine.chess.HumanPlayer;
import org.petero.cuckoo.engine.chess.Move;
import org.petero.cuckoo.engine.chess.MoveGen;
import org.petero.cuckoo.engine.chess.Piece;
import org.petero.cuckoo.engine.chess.Player;
import org.petero.cuckoo.engine.chess.Position;
import org.petero.cuckoo.engine.chess.Search;
import org.petero.cuckoo.engine.chess.TextIO;
import org.petero.cuckoo.engine.chess.UndoInfo;

/**
 * The glue between the chess engine and the GUI.
 * @author petero
 */
public class ChessController {
    Player humanPlayer;
    ComputerPlayer computerPlayer;
    Game game;
    final GUIInterface gui;
    boolean humanIsWhite;
    Thread computerThread;
    int threadStack;       // Thread stack size, or zero to use OS default

    // Search statistics
    String thinkingPV;

    class SearchListener implements Search.Listener {
        int currDepth = 0;
        int currMoveNr = 0;
        String currMove = "";
        int currNodes = 0;
        int currNps = 0;
        int currTime = 0;

        int pvDepth = 0;
        int pvScore = 0;
        boolean pvIsMate = false;
        boolean pvUpperBound = false;
        boolean pvLowerBound = false;
        String pvStr = "";

        private void setSearchInfo() {
            StringBuilder buf = new StringBuilder();
            buf.append(String.format("%n[%d] ", pvDepth));
            if (pvUpperBound) {
                buf.append("<=");
            } else if (pvLowerBound) {
                buf.append(">=");
            }
            if (pvIsMate) {
                buf.append(String.format("m%d", pvScore));
            } else {
                buf.append(String.format("%.2f", pvScore / 100.0));
            }
            buf.append(pvStr);
            buf.append(String.format("%n"));
            buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d", currDepth,
                    currMoveNr, currMove, currTime / 1000.0, currNodes, currNps));
            final String newPV = buf.toString();
            gui.runOnUIThread(() -> {
			    thinkingPV = newPV;
			    setThinkingPV();
			});
        }

        @Override
		public void notifyDepth(int depth) {
            currDepth = depth;
            setSearchInfo();
        }

        @Override
		public void notifyCurrMove(Move m, int moveNr) {
            currMove = TextIO.moveToString(new Position(game.pos), m, false);
            currMoveNr = moveNr;
            setSearchInfo();
        }

        @Override
		public void notifyPV(int depth, int score, int time, int nodes, int nps, boolean isMate,
                boolean upperBound, boolean lowerBound, ArrayList<Move> pv) {
            pvDepth = depth;
            pvScore = score;
            currTime = time;
            currNodes = nodes;
            currNps = nps;
            pvIsMate = isMate;
            pvUpperBound = upperBound;
            pvLowerBound = lowerBound;

            StringBuilder buf = new StringBuilder();
            Position pos = new Position(game.pos);
            UndoInfo ui = new UndoInfo();
            for (Move m : pv) {
                buf.append(String.format(" %s", TextIO.moveToString(pos, m, false)));
                pos.makeMove(m, ui);
            }
            pvStr = buf.toString();
            setSearchInfo();
        }

        @Override
		public void notifyStats(int nodes, int nps, int time) {
            currNodes = nodes;
            currNps = nps;
            currTime = time;
            setSearchInfo();
        }
    }
    final SearchListener listener;
    
    public ChessController(GUIInterface gui) {
        this.gui = gui;
        listener = new SearchListener();
        thinkingPV = "";
        threadStack = 0;
    }

    public void setThreadStackSize(int size) {
        threadStack = size;
    }
    
    public final void newGame(boolean humanIsWhite, int ttLogSize, boolean verbose) {
        stopComputerThinking();
        this.humanIsWhite = humanIsWhite;
        humanPlayer = new HumanPlayer();
        computerPlayer = new ComputerPlayer();
        computerPlayer.verbose = verbose;
        computerPlayer.setTTLogSize(ttLogSize);
        computerPlayer.setListener(listener);
        if (humanIsWhite) {
            game = new Game(humanPlayer, computerPlayer);
        } else {
            game = new Game(computerPlayer, humanPlayer);
        }
    }
    public final void startGame() {
        gui.setSelection(-1);
        updateGUI();
        startComputerThinking();
    }
    
    public final void setPosHistory(List<String> posHistStr) {
        try {
            String fen = posHistStr.getFirst();
            Position pos = TextIO.readFEN(fen);
            game.processString("new");
            game.pos = pos;
            String[] strArr = posHistStr.get(1).split(" ");
            final int arrLen = strArr.length;
            for (String s : strArr) {
                game.processString(s);
            }
            int numUndo = Integer.parseInt(posHistStr.get(2));
            for (int i = 0; i < numUndo; i++) {
                game.processString("undo");
            }
        } catch (ChessParseError e) {
            // Just ignore invalid positions
        }
    }
    
    public final List<String> getPosHistory() {
        return game.getPosHistory();
    }
    
    public String getFEN() {
        return TextIO.toFEN(game.pos);
    }
    
    /** Convert current game to PGN format. */
    public String getPGN() {
        StringBuilder pgn = new StringBuilder();
        List<String> posHist = getPosHistory();
        String fen = posHist.getFirst();
        String moves = game.getMoveListString(true);
        if (game.getGameState() == GameState.ALIVE)
            moves += " *";
        int year, month, day;
        {
            Calendar now = Calendar.getInstance();
            year = now.get(Calendar.YEAR);
            month = now.get(Calendar.MONTH) + 1;
            day = now.get(Calendar.DAY_OF_MONTH);
        }
        pgn.append(String.format("[Date \"%04d.%02d.%02d\"]%n", year, month, day));
        String white = "Player";
        String black = ComputerPlayer.engineName;
        if (!humanIsWhite) {
            String tmp = white; white = black; black = tmp;
        }
        pgn.append(String.format("[White \"%s\"]%n", white));
        pgn.append(String.format("[Black \"%s\"]%n", black));
        pgn.append(String.format("[Result \"%s\"]%n", game.getPGNResultString()));
        if (!fen.equals(TextIO.START_POS_FEN)) {
            pgn.append(String.format("[FEN \"%s\"]%n", fen));
            pgn.append("[SetUp \"1\"]\n");
        }
        pgn.append("\n");
        String[] strArr = moves.split(" ");
        int currLineLength = 0;
        final int arrLen = strArr.length;
        for (String s : strArr) {
            String move = s.trim();
            int moveLen = move.length();
            if (moveLen > 0) {
                if (currLineLength + 1 + moveLen >= 80) {
                    pgn.append("\n");
                    pgn.append(move);
                    currLineLength = moveLen;
                } else {
                    if (currLineLength > 0) {
                        pgn.append(" ");
                        currLineLength++;
                    }
                    pgn.append(move);
                    currLineLength += moveLen;
                }
            }
        }
        pgn.append("\n\n");
        return pgn.toString();
    }

    public void setPGN(String pgn) throws ChessParseError {
        // First pass, remove comments
        {
            StringBuilder out = new StringBuilder();
            Scanner sc = new Scanner(pgn);
            sc.useDelimiter("");
            while (sc.hasNext()) {
                String c = sc.next();
                if (c.equals("{")) {
                    sc.skip("[^}]*}");
                } else if (c.equals(";")) {
                    sc.skip("[^\n]*\n");
                } else {
                    out.append(c);
                }
            }
            pgn = out.toString();
            sc.close();
        }

        // Parse tag section
        Position pos = TextIO.readFEN(TextIO.START_POS_FEN);
        Scanner sc = new Scanner(pgn);
        sc.useDelimiter("\\s+");
        while (sc.hasNext("\\[.*")) {
            String tagName = sc.next();
            if (tagName.length() > 1) {
                tagName = tagName.substring(1);
            } else {
                tagName = sc.next();
            }
            String tagValue = sc.findWithinHorizon(".*\\]", 0);
            tagValue = tagValue.trim();
            if (tagValue.charAt(0) == '"')
                tagValue = tagValue.substring(1);
            if (tagValue.charAt(tagValue.length()-1) == ']')
                tagValue = tagValue.substring(0, tagValue.length() - 1);
            if (tagValue.charAt(tagValue.length()-1) == '"')
                tagValue = tagValue.substring(0, tagValue.length() - 1);
            if (tagName.equals("FEN")) {
                pos = TextIO.readFEN(tagValue);
            }
        }
        game.processString("new");
        game.pos = pos;

        // Handle (ignore) recursive annotation variations
        {
            StringBuilder out = new StringBuilder();
            sc.useDelimiter("");
            int level = 0;
            while (sc.hasNext()) {
                String c = sc.next();
                if (c.equals("(")) {
                    level++;
                } else if (c.equals(")")) {
                    level--;
                } else if (level == 0) {
                    out.append(c);
                }
            }
            pgn = out.toString();
        }
        sc.close();

        // Parse move text section
        sc = new Scanner(pgn);
        sc.useDelimiter("\\s+");
        while (sc.hasNext()) {
            String strMove = sc.next();
            strMove = strMove.replaceFirst("\\$?[0-9]*\\.*([^?!]*)[?!]*", "$1");
            if (strMove.isEmpty()) continue;
            Optional<Move> m = TextIO.stringToMove(game.pos, strMove);
            if (m.isEmpty())
                break;
            game.processString(strMove);
        }
        sc.close();
    }

    public void setFENOrPGN(String fenPgn) throws ChessParseError {
        try {
            Position pos = TextIO.readFEN(fenPgn);
            game.processString("new");
            game.pos = pos;
        } catch (ChessParseError e) {
            // Try read as PGN instead
            setPGN(fenPgn);
        }
        gui.setSelection(-1);
        updateGUI();
        startComputerThinking();
    }

    /** Set color for human player. Doesn't work when computer is thinking. */
    public final void setHumanWhite(final boolean humanIsWhite) {
        if (computerThread != null)
            return;
        if (this.humanIsWhite != humanIsWhite) {
            this.humanIsWhite = humanIsWhite;
            game.processString("swap");
            startComputerThinking();
        }
    }
    
    public final boolean humansTurn() {
        return game.pos.whiteMove == humanIsWhite;
    }

    public final void takeBackMove() {
        if (humansTurn()) {
            if (game.getLastMove() != null) {
                game.processString("undo");
                if (game.getLastMove() != null) {
                    game.processString("undo");
                } else {
                    game.processString("redo");
                }
                updateGUI();
                setSelection();
            }
        } else if (game.getGameState() != Game.GameState.ALIVE) {
            if (game.getLastMove() != null) {
                game.processString("undo");
                if (!humansTurn()) {
                    if (game.getLastMove() != null) {
                        game.processString("undo");
                    } else {
                        game.processString("redo");
                    }
                }
                updateGUI();
                setSelection();
            }
        }
    }

    public final void redoMove() {
        if (humansTurn()) {
            game.processString("redo");
            game.processString("redo");
            updateGUI();
            setSelection();
        }
    }

    public final void humanMove(Move m) {
        if (humansTurn()) {
            if (doMove(m)) {
                updateGUI();
                startComputerThinking();
            } else {
                gui.setSelection(-1);
            }
        }
    }

    Move promoteMove;
    public final void reportPromotePiece(int choice) {
        final boolean white = game.pos.whiteMove;
        promoteMove.promoteTo = switch (choice) {
            case 1 -> white ? Piece.WROOK : Piece.BROOK;
            case 2 -> white ? Piece.WBISHOP : Piece.BBISHOP;
            case 3 -> white ? Piece.WKNIGHT : Piece.BKNIGHT;
            default -> white ? Piece.WQUEEN : Piece.BQUEEN;
        };
        Move m = promoteMove;
        promoteMove = null;
        humanMove(m);
    }

    /**
     * Move a piece from one square to another.
     * @return True if the move was legal, false otherwise.
     */
    private boolean doMove(Move move) {
        Position pos = game.pos;
        MoveGen.MoveList moves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
        int promoteTo = move.promoteTo;
        for (int mi = 0; mi < moves.size; mi++) {
            Move m = moves.m[mi];
            if ((m.from == move.from) && (m.to == move.to)) {
                if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
                    promoteMove = m;
                    gui.requestPromotePiece();
                    return false;
                }
                if (m.promoteTo == promoteTo) {
                    String strMove = TextIO.moveToString(pos, m, false);
                    game.processString(strMove);
                    return true;
                }
            }
        }
        return false;
    }


    private void updateGUI() {
        setStatusString();
        setMoveList();
        setThinkingPV();
        gui.setPosition(game.pos);
    }

    private void setStatusString() {
        String str = game.pos.whiteMove ? "White's move" : "Black's move";
        if (computerThread != null) str += " (thinking)";
        if (game.getGameState() != Game.GameState.ALIVE) {
            str = game.getGameStateString();
        }
        gui.setStatusString(str);
    }

    public final void setMoveList() {
        String str = game.getMoveListString(true);
        gui.setMoveListString(str);
    }
    
    public final void setThinkingPV() {
        String str = "";
        if (gui.showThinking()) {
            str = thinkingPV;
        }
        gui.setThinkingString(str);
    }

    private void setSelection() {
        Move m = game.getLastMove();
        int sq = (m != null) ? m.to : -1;
        gui.setSelection(sq);
    }

    
    private void startComputerThinking() {
    if (game.pos.whiteMove != humanIsWhite && computerThread == null) {
                Runnable run = () -> {
				    computerPlayer.timeLimit(gui.timeLimit(), gui.timeLimit(), false);
				    final String cmd = computerPlayer.getCommand(new Position(game.pos), game.getHistory());
				    gui.runOnUIThread(() -> {
					    game.processString(cmd);
					    thinkingPV = "";
					    updateGUI();
					    setSelection();
					    stopComputerThinking();
					});
				};
                if (threadStack > 0) {
                    ThreadGroup tg = new ThreadGroup("searcher");
                    computerThread = new Thread(tg, run, "searcher", threadStack);
                } else {
                    computerThread = new Thread(run);
                }
                thinkingPV = "";
                updateGUI();
                computerThread.start();
            }

    }

    public synchronized void stopComputerThinking() {
        if (computerThread != null) {
            computerPlayer.timeLimit(0, 0, false);
            try {
                computerThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop thread%n");
            }
            computerThread = null;
            updateGUI();
        }
    }

    public synchronized void setTimeLimit() {
        if (computerThread != null) {
            computerPlayer.timeLimit(gui.timeLimit(), gui.timeLimit(), false);
        }
    }
}
