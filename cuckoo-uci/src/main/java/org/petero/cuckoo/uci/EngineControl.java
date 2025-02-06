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

package org.petero.cuckoo.uci;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.petero.cuckoo.engine.chess.Book;
import org.petero.cuckoo.engine.chess.ComputerPlayer;
import org.petero.cuckoo.engine.chess.Move;
import org.petero.cuckoo.engine.chess.MoveGen;
import org.petero.cuckoo.engine.chess.Parameters;
import org.petero.cuckoo.engine.chess.Parameters.CheckParam;
import org.petero.cuckoo.engine.chess.Parameters.ComboParam;
import org.petero.cuckoo.engine.chess.Parameters.ParamBase;
import org.petero.cuckoo.engine.chess.Parameters.SpinParam;
import org.petero.cuckoo.engine.chess.Parameters.StringParam;
import org.petero.cuckoo.engine.chess.Position;
import org.petero.cuckoo.engine.chess.Search;
import org.petero.cuckoo.engine.chess.TextIO;
import org.petero.cuckoo.engine.chess.TranspositionTable;
import org.petero.cuckoo.engine.chess.TranspositionTable.TTEntry;
import org.petero.cuckoo.engine.chess.UndoInfo;

/**
 * Control the search thread.
 * 
 * @author petero
 */
public class EngineControl {
	final PrintStream os;

	Thread engineThread;
	private final Object threadMutex;
	Search sc;
	TranspositionTable tt;
	final MoveGen moveGen;

	Position pos;
	long[] posHashList;
	int posHashListSize;
	boolean ponder; // True if currently doing pondering
	boolean onePossibleMove;
	boolean infinite;

	int minTimeLimit;
	int maxTimeLimit;
	int maxDepth;
	int maxNodes;
	List<Move> searchMoves;

	// Options
	int hashSizeMB = 16;
	boolean ownBook = false;
	boolean analyseMode = false;
	boolean ponderMode = true;

	// Reduced strength variables
	int strength = 1000;
	long randomSeed = 0;

	/**
	 * This class is responsible for sending "info" strings during search.
	 */
	private record SearchListener(PrintStream os) implements Search.Listener {

	@Override
		public void notifyDepth(int depth) {
			os.printf("info depth %d%n", depth);
		}

		@Override
		public void notifyCurrMove(Move m, int moveNr) {
			os.printf("info currmove %s currmovenumber %d%n", moveToString(m), moveNr);
		}

		@Override
		public void notifyPV(int depth, int score, int time, int nodes, int nps, boolean isMate, boolean upperBound,
				boolean lowerBound, ArrayList<Move> pv) {
			StringBuilder pvBuf = new StringBuilder();
			for (Move m : pv) {
				pvBuf.append(" ");
				pvBuf.append(moveToString(m));
			}
			String bound = "";
			if (upperBound) {
				bound = " upperbound";
			} else if (lowerBound) {
				bound = " lowerbound";
			}
			os.printf("info depth %d score %s %d%s time %d nodes %d nps %d pv%s%n", depth, isMate ? "mate" : "cp",
					score, bound, time, nodes, nps, pvBuf);
		}

		@Override
		public void notifyStats(int nodes, int nps, int time) {
			os.printf("info nodes %d nps %d time %d%n", nodes, nps, time);
		}
	}

	public EngineControl(PrintStream os) {
		this.os = os;
		threadMutex = new Object();
		setupTT();
		moveGen = new MoveGen();
	}

	public final void startSearch(Position pos, ArrayList<Move> moves, SearchParams sPar) {
		setupPosition(new Position(pos), moves);
		computeTimeLimit(sPar);
		ponder = false;
		infinite = (maxTimeLimit < 0) && (maxDepth < 0) && (maxNodes < 0);
		startThread(minTimeLimit, maxTimeLimit, maxDepth, maxNodes);
		searchMoves = sPar.searchMoves;
	}

  	public final void startPonder(Position pos, List<Move> moves, SearchParams sPar) {
		setupPosition(new Position(pos), moves);
		computeTimeLimit(sPar);
		ponder = true;
		infinite = false;
		startThread(-1, -1, -1, -1);
	}

	public final void ponderHit() {
		Search mySearch;
		synchronized (threadMutex) {
			mySearch = sc;
		}
		if (mySearch != null) {
			if (onePossibleMove) {
				if (minTimeLimit > 1)
					minTimeLimit = 1;
				if (maxTimeLimit > 1)
					maxTimeLimit = 1;
			}
			mySearch.timeLimit(minTimeLimit, maxTimeLimit);
		}
		infinite = (maxTimeLimit < 0) && (maxDepth < 0) && (maxNodes < 0);
		ponder = false;
	}

	public final void stopSearch() {
		stopThread();
	}

	public final void newGame() {
		randomSeed = new Random().nextLong();
		tt.clear();
	}

	/**
	 * Compute thinking time for current search.
	 */
	public final void computeTimeLimit(SearchParams sPar) {
		minTimeLimit = -1;
		maxTimeLimit = -1;
		maxDepth = -1;
		maxNodes = -1;
		if (sPar.depth > 0) {
			maxDepth = sPar.depth;
		} else if (sPar.mate > 0) {
			maxDepth = sPar.mate * 2 - 1;
		} else if (sPar.moveTime > 0) {
			minTimeLimit = maxTimeLimit = sPar.moveTime;
		} else if (sPar.nodes > 0) {
			maxNodes = sPar.nodes;
		} else {
			int moves = sPar.movesToGo;
			if (moves == 0) {
				moves = 999;
			}
			moves = Math.min(moves, 45); // Assume 45 more moves until end of
											// game
			if (ponderMode) {
				final double ponderHitRate = 0.35;
				moves = (int) Math.ceil(moves * (1 - ponderHitRate));
			}
			boolean white = pos.whiteMove;
			int time = white ? sPar.wTime : sPar.bTime;
			int inc = white ? sPar.wInc : sPar.bInc;
			final int margin = Math.min(1000, time * 9 / 10);
			int timeLimit = (time + inc * (moves - 1) - margin) / moves;
			minTimeLimit = (int) (timeLimit * 0.85);
			maxTimeLimit = (int) (minTimeLimit * (Math.max(2.5, Math.min(4.0, moves / 2))));

			// Leave at least 1s on the clock, but can't use negative time
			minTimeLimit = clamp(minTimeLimit, time - margin);
			maxTimeLimit = clamp(maxTimeLimit, time - margin);
		}
	}

	private static int clamp(int val, int max) {
		if (val < 1) {
			return 1;
		} else return Math.min(val, max);
	}

	private void startThread(final int minTimeLimit, final int maxTimeLimit, int maxDepth, final int maxNodes) {
        // Must not start new search until old search is finished
        sc = new Search(pos, posHashList, posHashListSize, tt);
		sc.timeLimit(minTimeLimit, maxTimeLimit);
		sc.setListener(new SearchListener(os));
		sc.setStrength(strength, randomSeed);
		MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
		MoveGen.removeIllegal(pos, moves);
		if ((searchMoves != null) && (!searchMoves.isEmpty())) {
			Arrays.asList(moves.m).retainAll(searchMoves);
		}
		final MoveGen.MoveList srchMoves = moves;
		onePossibleMove = false;
		if ((srchMoves.size < 2) && !infinite) {
			onePossibleMove = true;
			if (!ponder) {
				if ((maxDepth < 0) || (maxDepth > 2))
					maxDepth = 2;
			}
		}
		tt.nextGeneration();
		final int srchmaxDepth = maxDepth;
		engineThread = new Thread(() -> {
			Move m = null;
			if (ownBook && !analyseMode) {
				Book book = new Book(false);
				m = book.getBookMove(pos);
			}
			if (m == null) {
				m = sc.iterativeDeepening(srchMoves, srchmaxDepth, maxNodes, false);
			}
			while (ponder || infinite) {
				// We should not respond until told to do so. Just wait
				// until
				// we are allowed to respond.
				try {
					Thread.sleep(10);
				} catch (InterruptedException ex) {
					break;
				}
			}
			Move ponderMove = getPonderMove(pos, m);
			synchronized (threadMutex) {
				if (ponderMove != null) {
					os.printf("bestmove %s ponder %s%n", moveToString(m), moveToString(ponderMove));
				} else {
					os.printf("bestmove %s%n", moveToString(m));
				}
				engineThread = null;
				sc = null;
			}
		});
		engineThread.start();
	}

	private void stopThread() {
		Thread myThread;
		Search mySearch;
		synchronized (threadMutex) {
			myThread = engineThread;
			mySearch = sc;
		}
		if (myThread != null) {
			mySearch.timeLimit(0, 0);
			infinite = false;
			ponder = false;
			try {
				myThread.join();
			} catch (InterruptedException ex) {
				throw new RuntimeException();
			}
		}
	}

	private void setupTT() {
		int nEntries = hashSizeMB > 0 ? hashSizeMB * (1 << 20) / 24 : 1024;
		int logSize = (int) Math.floor(Math.log(nEntries) / Math.log(2));
		tt = new TranspositionTable(logSize);
	}

	private void setupPosition(Position pos, List<Move> moves) {
		UndoInfo ui = new UndoInfo();
		posHashList = new long[200 + moves.size()];
		posHashListSize = 0;
		for (Move m : moves) {
			posHashList[posHashListSize++] = pos.zobristHash();
			pos.makeMove(m, ui);
		}
		this.pos = pos;
	}

	/**
	 * Try to find a move to ponder from the transposition table.
	 */
	private Move getPonderMove(Position pos, Move m) {
		if (m == null)
			return null;
		Move ret = null;
		UndoInfo ui = new UndoInfo();
		pos.makeMove(m, ui);
		TTEntry ent = tt.probe(pos.historyHash());
		if (ent.type != TTEntry.T_EMPTY) {
			ret = new Move(0, 0, 0);
			ent.getMove(ret);
			MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
			MoveGen.removeIllegal(pos, moves);
			boolean contains = TranspositionTable.contains(ret, ent, moveGen, pos);
      		if (!contains) {
        		ret = null;
	  		}
		}
		pos.unMakeMove(m, ui);
		return ret;
	}

	private static String moveToString(Move m) {
		if (m == null)
			return "0000";
		String ret = TextIO.squareToString(m.from);
		ret += TextIO.squareToString(m.to);
		ret += TextIO.getPromotionString(m);
		return ret;
	}

	static void printOptions(PrintStream os) {
		os.printf("option name Hash type spin default 16 min 1 max 2048%n");
		os.printf("option name OwnBook type check default false%n");
		os.printf("option name Ponder type check default true%n");
		os.printf("option name UCI_AnalyseMode type check default false%n");
		os.printf(
				"option name UCI_EngineAbout type string default %s by Peter Osterlund, see https://web.comhem.se/petero2home/javachess/index.html%n",
				ComputerPlayer.engineName);
		os.print("option name Strength type spin default 1000 min 0 max 1000\n");

		for (String pName : Parameters.instance().getParamNames()) {
			ParamBase p = Parameters.instance().getParam(pName);
			switch (p.getType()) {
			case CHECK: {
				CheckParam cp = (CheckParam) p;
				os.printf("optionn name %s type check default %s%n", p.getName(), cp.defaultValue ? "true" : "false");
				break;
			}
			case SPIN: {
				SpinParam sp = (SpinParam) p;
				os.printf("option name %s type spin default %d min %d max %d%n", p.getName(), sp.defaultValue, sp.minValue,
						sp.maxValue);
				break;
			}
			case COMBO: {
				ComboParam cp = (ComboParam) p;
				os.printf("option name %s type combo default %s ", cp.getName(), cp.defaultValue);
				for (String s : cp.allowedValues)
					os.printf(" var %s", s);
				os.print("\n");
				break;
			}
			case BUTTON:
				os.printf("option name %s type button%n", p.getName());
				break;
			case STRING: {
				StringParam sp = (StringParam) p;
				os.printf("option name %s type string default %s%n", p.getName(), sp.defaultValue);
				break;
			}
			}
		}
	}

	final void setOption(String optionName, String optionValue) {
		try {
            switch (optionName) {
                case "hash" -> {
                    hashSizeMB = Integer.parseInt(optionValue);
                    setupTT();
                }
                case "ownbook" -> ownBook = Boolean.parseBoolean(optionValue);
                case "ponder" -> ponderMode = Boolean.parseBoolean(optionValue);
                case "uci_analysemode" -> analyseMode = Boolean.parseBoolean(optionValue);
                case "strength" -> strength = Integer.parseInt(optionValue);
                default -> Parameters.instance().set(optionName, optionValue);
            }
		} catch (NumberFormatException ignored) {
		}
	}
}
