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

package org.petero.cuckoo.engine.chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import org.petero.cuckoo.engine.chess.TranspositionTable.TTEntry;

/**
 *
 * @author petero
 */
public class Search {
    final static int plyScale = 8; // Fractional ply resolution

    Position pos;
    final MoveGen moveGen;
    final Evaluate eval;
    final KillerTable kt;
    final History ht;
    final long[] posHashList;         // List of hashes for previous positions up to the last "zeroing" move.
    int posHashListSize;        // Number of used entries in posHashList
    final int posHashFirstNew;        // First entry in posHashList that has not been played OTB.
    final TranspositionTable tt;
    TreeLogger log = null;

    private static final class SearchTreeInfo {
        final UndoInfo undoInfo;
        final Move hashMove;         // Temporary storage for local hashMove variable
        boolean allowNullMove; // Don't allow two null-moves in a row
        final Move bestMove;         // Copy of the best found move at this ply
        Move currentMove;      // Move currently being searched
        int lmr;               // LMR reduction amount
        long nodeIdx;
        SearchTreeInfo() {
            undoInfo = new UndoInfo();
            hashMove = new Move(0, 0, 0);
            allowNullMove = true;
            bestMove = new Move(0, 0, 0);
        }
    }
    final SearchTreeInfo[] searchTreeInfo;

    // Time management
    long tStart;            // Time when search started
    long minTimeMillis;     // Minimum recommended thinking time
    long maxTimeMillis;     // Maximum allowed thinking time
    boolean searchNeedMoreTime; // True if negaScout should use up to maxTimeMillis time.
    private int maxNodes;   // Maximum number of nodes to search (approximately)
    int nodesToGo;          // Number of nodes until next time check
    public final int nodesBetweenTimeCheck = 5000; // How often to check remaining time

    // Reduced strength variables
    private int strength = 1000; // Strength (0-1000)
    boolean weak = false;        // Set to strength < 1000
    long randomSeed = 0;

    // Search statistics stuff
    int nodes;
    int qNodes;
    int[] nodesPlyVec;
    int[] nodesDepthVec;
    int totalNodes;
    long tLastStats;        // Time when notifyStats was last called
    boolean verbose;

    public static final int MATE0 = 32000;

    public static final int UNKNOWN_SCORE = -32767; // Represents unknown static eval score
    int q0Eval; // Static eval score at first level of quiescence search 

    public Search(Position pos, long[] posHashList, int posHashListSize, TranspositionTable tt) {
        this.pos = new Position(pos);
        this.moveGen = new MoveGen();
        this.posHashList = posHashList;
        this.posHashListSize = posHashListSize;
        this.tt = tt;
        eval = new Evaluate();
        kt = new KillerTable();
        ht = new History();
        posHashFirstNew = posHashListSize;
        initNodeStats();
        minTimeMillis = -1;
        maxTimeMillis = -1;
        searchNeedMoreTime = false;
        maxNodes = -1;
        final int vecLen = 200;
        searchTreeInfo = new SearchTreeInfo[vecLen];
        for (int i = 0; i < vecLen; i++) {
            searchTreeInfo[i] = new SearchTreeInfo();
        }
    }

    /**
     * Used to get various search information during search
     */
    public interface Listener {
        void notifyDepth(int depth);
        void notifyCurrMove(Move m, int moveNr);
        void notifyPV(int depth, int score, int time, int nodes, int nps,
                      boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv);
        void notifyStats(int nodes, int nps, int time);
    }

    Listener listener;
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private static final class MoveInfo {
        final Move move;
        int nodes;
        MoveInfo(Move m) { move = m;  nodes = 0; }
        public static final class SortByScore implements Comparator<MoveInfo> {
            @Override
			public int compare(MoveInfo mi1, MoveInfo mi2) {
                if ((mi1 == null) && (mi2 == null))
                    return 0;
                if (mi1 == null)
                    return 1;
                if (mi2 == null)
                    return -1;
                return mi2.move.score - mi1.move.score;
            }
        }
        public static final class SortByNodes implements Comparator<MoveInfo> {
            @Override
			public int compare(MoveInfo mi1, MoveInfo mi2) {
                if ((mi1 == null) && (mi2 == null))
                    return 0;
                if (mi1 == null)
                    return 1;
                if (mi2 == null)
                    return -1;
                return mi2.nodes - mi1.nodes;
            }
        }
    }

    final public void timeLimit(int minTimeLimit, int maxTimeLimit) {
        minTimeMillis = minTimeLimit;
        maxTimeMillis = maxTimeLimit;
    }

    final public void setStrength(int strength, long randomSeed) {
        if (strength < 0) strength = 0;
        if (strength > 1000) strength = 1000;
        this.strength = strength;
        weak = strength < 1000;
        this.randomSeed = randomSeed;
    }

    final public Move iterativeDeepening(MoveGen.MoveList scMovesIn,
            int maxDepth, int initialMaxNodes, boolean verbose) {
        tStart = System.currentTimeMillis();
        totalNodes = 0;
        if (scMovesIn.size <= 0)
            return null; // No moves to search
        MoveInfo[] scMoves = new MoveInfo[scMovesIn.size];
        for (int mi = 0, len = 0; mi < scMovesIn.size; mi++) {
            Move m = scMovesIn.m[mi];
            scMoves[len++] = new MoveInfo(m);
        }
        maxNodes = initialMaxNodes;
        nodesToGo = 0;
        Position origPos = new Position(pos);
        int bestScoreLastIter = 0;
        Move bestMove = scMoves[0].move;
        this.verbose = verbose;
        if ((maxDepth < 0) || (maxDepth > 100)) {
            maxDepth = 100;
        }
        for (SearchTreeInfo treeInfo : searchTreeInfo) {
            treeInfo.allowNullMove = true;
        }
        try {
        int depth;
        for (depth = 1; ; depth++) {
            initNodeStats();
            if (listener != null) listener.notifyDepth(depth);
            int aspirationDelta = (Math.abs(bestScoreLastIter) <= MATE0 / 2) ? 20 : 1000;
            int alpha = depth > 1 ? Math.max(bestScoreLastIter - aspirationDelta, -Search.MATE0) : -Search.MATE0;
            int bestScore = -Search.MATE0;
            UndoInfo ui = new UndoInfo();
            boolean needMoreTime = false;
            for (int mi = 0; mi < scMoves.length; mi++) {
                searchNeedMoreTime = (mi > 0);
                Move m = scMoves[mi].move;
                if ((listener != null) && (System.currentTimeMillis() - tStart >= 1000)) {
                    listener.notifyCurrMove(m, mi + 1);
                }
                nodes = qNodes = 0;
                posHashList[posHashListSize++] = pos.zobristHash();
                boolean givesCheck = MoveGen.givesCheck(pos, m);
                int beta;
                if (depth > 1) {
                    beta = (mi == 0) ? Math.min(bestScoreLastIter + aspirationDelta, Search.MATE0) : alpha + 1;
                } else {
                    beta = Search.MATE0;
                }

                int lmr = 0;
                boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY);
                boolean isPromotion = (m.promoteTo != Piece.EMPTY);
                if ((depth >= 3) && !isCapture && !isPromotion) {
                    if (!givesCheck && notPassedPawnPush(pos, m)) {
                        if (mi >= 3)
                            lmr = 1;
                    }
                }
                pos.makeMove(m, ui);
                SearchTreeInfo sti = searchTreeInfo[0];
                sti.currentMove = m;
                sti.lmr = lmr * plyScale;
                sti.nodeIdx = -1;
                int score = -negaScout(-beta, -alpha, 1, (depth - lmr - 1) * plyScale, -1, givesCheck);
                if ((lmr > 0) && (score > alpha)) {
                    sti.lmr = 0;
                    score = -negaScout(-beta, -alpha, 1, (depth - 1) * plyScale, -1, false);
                }
                int nodesThisMove = nodes + qNodes;
                posHashListSize--;
                pos.unMakeMove(m, ui);
                {
                    int type = TTEntry.T_EXACT;
                    if (score <= alpha) {
                        type = TTEntry.T_LE;
                    } else if (score >= beta) {
                        type = TTEntry.T_GE;
                    }
                    m.score = score;
                    tt.insert(pos.historyHash(), m, type, 0, depth, UNKNOWN_SCORE);
                }
                if (score >= beta) {
                    int retryDelta = aspirationDelta * 2;
                    while (score >= beta) {
                        beta = Math.min(score + retryDelta, Search.MATE0);
                        retryDelta = Search.MATE0 * 2;
                        if (mi != 0)
                            needMoreTime = true;
                        bestMove = m;
                        if (verbose)
                            System.out.printf("%-6s %6d %6d %6d >=\n", TextIO.moveToString(pos, m, false),
                                    score, nodes, qNodes);
                        notifyPV(depth, score, false, true, m);
                        nodes = qNodes = 0;
                        posHashList[posHashListSize++] = pos.zobristHash();
                        pos.makeMove(m, ui);
                        int score2 = -negaScout(-beta, -score, 1, (depth - 1) * plyScale, -1, givesCheck);
                        score = Math.max(score, score2);
                        nodesThisMove += nodes + qNodes;
                        posHashListSize--;
                        pos.unMakeMove(m, ui);
                    }
                } else if ((mi == 0) && (score <= alpha)) {
                    int retryDelta = Search.MATE0 * 2;
                    while (score <= alpha) {
                        alpha = Math.max(score - retryDelta, -Search.MATE0);
                        needMoreTime = searchNeedMoreTime = true;
                        if (verbose)
                            System.out.printf("%-6s %6d %6d %6d <=\n", TextIO.moveToString(pos, m, false),
                                    score, nodes, qNodes);
                        notifyPV(depth, score, true, false, m);
                        nodes = qNodes = 0;
                        posHashList[posHashListSize++] = pos.zobristHash();
                        pos.makeMove(m, ui);
                        score = -negaScout(-score, -alpha, 1, (depth - 1) * plyScale, -1, givesCheck);
                        nodesThisMove += nodes + qNodes;
                        posHashListSize--;
                        pos.unMakeMove(m, ui);
                    }
                }
                if (verbose || ((listener != null) && (depth > 1))) {
                    boolean havePV = false;
                    String PV = "";
                    if ((score > alpha) || (mi == 0)) {
                        havePV = true;
                        if (verbose) {
                            PV = TextIO.moveToString(pos, m, false) + " ";
                            pos.makeMove(m, ui);
                            PV += tt.extractPV(pos);
                            pos.unMakeMove(m, ui);
                        }
                    }
                    if (verbose) {
                        System.out.printf("%-6s %6d %6d %6d%s %s\n",
                                TextIO.moveToString(pos, m, false), score,
                                nodes, qNodes, (score > alpha ? " *" : ""), PV);
                    }
                    if (havePV && (depth > 1)) {
                        notifyPV(depth, score, false, false, m);
                    }
                }
                scMoves[mi].move.score = score;
                scMoves[mi].nodes = nodesThisMove;
                bestScore = Math.max(bestScore, score);
                if (depth > 1) {
                    if ((score > alpha) || (mi == 0)) {
                        alpha = score;
                        MoveInfo tmp = scMoves[mi];
                        for (int i = mi - 1; i >= 0;  i--) {
                            scMoves[i + 1] = scMoves[i];
                        }
                        scMoves[0] = tmp;
                        bestMove = scMoves[0].move;
                    }
                }
                if (depth > 1) {
                    long timeLimit = needMoreTime ? maxTimeMillis : minTimeMillis;
                    if (timeLimit >= 0) {
                        long tNow = System.currentTimeMillis();
                        if (tNow - tStart >= timeLimit)
                            break;
                    }
                }
            }
            if (depth == 1) {
                Arrays.sort(scMoves, new MoveInfo.SortByScore());
                bestMove = scMoves[0].move;
                notifyPV(depth, bestMove.score, false, false, bestMove);
            }
            long tNow = System.currentTimeMillis();
            if (verbose) {
                for (int i = 0; i < 20; i++) {
                    System.out.printf("%2d %7d %7d\n", i, nodesPlyVec[i], nodesDepthVec[i]);
                }
                System.out.printf("Time: %.3f depth:%d nps:%d\n", (tNow - tStart) * .001, depth,
                        (int)(totalNodes / ((tNow - tStart) * .001)));
            }
            if (maxTimeMillis >= 0) {
                if (tNow - tStart >= minTimeMillis)
                    break;
            }
            if (depth >= maxDepth)
                break;
            if (maxNodes >= 0) {
                if (totalNodes >= maxNodes)
                    break;
            }
            int plyToMate = Search.MATE0 - Math.abs(bestScore);
            if (depth >= plyToMate)
                break;
            bestScoreLastIter = bestScore;

            if (depth > 1) {
                // Moves that were hard to search should be searched early in the next iteration
                Arrays.sort(scMoves, 1, scMoves.length, new MoveInfo.SortByNodes());
            }
        }
        } catch (StopSearch ss) {
            pos = origPos;
        }
        notifyStats();

        if (log != null) {
            log.close();
            log = null;
        }
        return bestMove;
    }

    private void notifyPV(int depth, int score, boolean uBound, boolean lBound, Move m) {
        if (listener != null) {
            boolean isMate = false;
            if (score > MATE0 / 2) {
                isMate = true;
                score = (MATE0 - score) / 2;
            } else if (score < -MATE0 / 2) {
                isMate = true;
                score = -((MATE0 + score - 1) / 2);
            }
            long tNow = System.currentTimeMillis();
            int time = (int) (tNow - tStart);
            int nps = (time > 0) ? (int)(totalNodes / (time / 1000.0)) : 0;
            ArrayList<Move> pv = tt.extractPVMoves(pos, m);
            listener.notifyPV(depth, score, time, totalNodes, nps, isMate, uBound, lBound, pv);
        }
    }

    private void notifyStats() {
        long tNow = System.currentTimeMillis();
        if (listener != null) {
            int time = (int) (tNow - tStart);
            int nps = (time > 0) ? (int)(totalNodes / (time / 1000.0)) : 0;
            listener.notifyStats(totalNodes, nps, time);
        }
        tLastStats = tNow;
    }

    private static final Move emptyMove = new Move(0, 0, Piece.EMPTY, 0);

    /** 
     * Main recursive search algorithm.
     * @return Score for the side to make a move, in position given by "pos".
     */
    public final int negaScout(int alpha, int beta, int ply, int depth, int recaptureSquare,
                               final boolean inCheck) throws StopSearch {
        if (log != null) {
            SearchTreeInfo sti = searchTreeInfo[ply-1];
            searchTreeInfo[ply].nodeIdx = log.logNodeStart(sti.nodeIdx, sti.currentMove, alpha, beta, ply, depth/plyScale);
        }
        if (--nodesToGo <= 0) {
            nodesToGo = nodesBetweenTimeCheck;
            long tNow = System.currentTimeMillis();
            long timeLimit = searchNeedMoreTime ? maxTimeMillis : minTimeMillis;
            if (    ((timeLimit >= 0) && (tNow - tStart >= timeLimit)) ||
                    ((maxNodes >= 0) && (totalNodes >= maxNodes))) {
                throw new StopSearch();
            }
            if (tNow - tLastStats >= 1000) {
                notifyStats();
            }
        }
        
        // Collect statistics
        if (verbose) {
            if (ply < 20) nodesPlyVec[ply]++;
            if (depth < 20*plyScale) nodesDepthVec[depth/plyScale]++;
        }
        nodes++;
        totalNodes++;
        final long hKey = pos.historyHash();

        // Draw tests
        if (canClaimDraw50(pos)) {
            if (MoveGen.canTakeKing(pos)) {
                int score = MATE0 - ply;
                if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
                return score;
            }
            if (inCheck) {
                MoveGen.MoveList moves = moveGen.pseudoLegalMoves(pos);
                MoveGen.removeIllegal(pos, moves);
                if (moves.size == 0) {            // Can't claim draw if already check mated.
                    int score = -(MATE0-(ply+1));
                    if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
                    moveGen.returnMoveList(moves);
                    return score;
                }
                moveGen.returnMoveList(moves);
            }
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, 0, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
            return 0;
        }
        if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashFirstNew)) {
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, 0, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
            return 0;            // No need to test for mate here, since it would have been
                                 // discovered the first time the position came up.
        }

        int evalScore = UNKNOWN_SCORE;
        // Check transposition table
        TTEntry ent = tt.probe(hKey);
        Move hashMove = null;
        SearchTreeInfo sti = searchTreeInfo[ply];
        if (ent.type != TTEntry.T_EMPTY) {
            int score = ent.getScore(ply);
            evalScore = ent.evalScore;
            int plyToMate = MATE0 - Math.abs(score);
            int eDepth = ent.getDepth();
            if ((beta == alpha + 1) && ((eDepth >= depth) || (eDepth >= plyToMate*plyScale))) {
                if (    (ent.type == TTEntry.T_EXACT) ||
                        (ent.type == TTEntry.T_GE) && (score >= beta) ||
                        (ent.type == TTEntry.T_LE) && (score <= alpha)) {
                    if (score >= beta) {
                        hashMove = sti.hashMove;
                        ent.getMove(hashMove);
                        if (hashMove.from != hashMove.to)
                            if (pos.getPiece(hashMove.to) == Piece.EMPTY)
                                kt.addKiller(ply, hashMove);
                    }
                    if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, ent.type, evalScore, hKey);
                    return score;
                }
            }
            hashMove = sti.hashMove;
            ent.getMove(hashMove);
        }
        
        int posExtend = inCheck ? plyScale : 0; // Check extension

        // If out of depth, perform quiescence search
        if (depth + posExtend <= 0) {
            qNodes--;
            totalNodes--;
            q0Eval = evalScore;
            int score = quiesce(alpha, beta, ply, 0, inCheck);
            int type = TTEntry.T_EXACT;
            if (score <= alpha) {
                type = TTEntry.T_LE;
            } else if (score >= beta) {
                type = TTEntry.T_GE;
            }
            emptyMove.score = score;
            tt.insert(hKey, emptyMove, type, ply, depth, q0Eval);
            if (log != null) log.logNodeEnd(sti.nodeIdx, score, type, evalScore, hKey);
            return score;
        }

        // Try null-move pruning
        // FIXME! Try null-move verification in late endgames. See loss in round 21.
        sti.currentMove = emptyMove;
        if (    (depth >= 3*plyScale) && !inCheck && sti.allowNullMove &&
                (Math.abs(beta) <= MATE0 / 2)) {
            if (MoveGen.canTakeKing(pos)) {
                int score = MATE0 - ply;
                if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_EXACT, evalScore, hKey);
                return score;
            }
            boolean nullOk;
            if (pos.whiteMove) {
                nullOk = (pos.wMtrl > pos.wMtrlPawns) && (pos.wMtrlPawns > 0);
            } else {
                nullOk = (pos.bMtrl > pos.bMtrlPawns) && (pos.bMtrlPawns > 0);
            }
            if (nullOk) {
                if (evalScore == UNKNOWN_SCORE)
                    evalScore = eval.evalPos(pos);
                if (evalScore < beta)
                    nullOk = false;
            }
            if (nullOk) {
                final int R = (depth > 6*plyScale) ? 4*plyScale : 3*plyScale;
                pos.setWhiteMove(!pos.whiteMove);
                int epSquare = pos.getEpSquare();
                pos.setEpSquare(-1);
                searchTreeInfo[ply+1].allowNullMove = false;
                int score = -negaScout(-beta, -(beta - 1), ply + 1, depth - R, -1, false);
                searchTreeInfo[ply+1].allowNullMove = true;
                pos.setEpSquare(epSquare);
                pos.setWhiteMove(!pos.whiteMove);
                if (score >= beta) {
                    if (score > MATE0 / 2)
                        score = beta;
                    emptyMove.score = score;
                    tt.insert(hKey, emptyMove, TTEntry.T_GE, ply, depth, evalScore);
                    if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_GE, evalScore, hKey);
                    return score;
                } else {
                    if ((searchTreeInfo[ply-1].lmr > 0) && (depth < 5*plyScale)) {
                        Move m1 = searchTreeInfo[ply-1].currentMove;
                        Move m2 = searchTreeInfo[ply+1].bestMove; // threat move
                        if (m1.from != m1.to) {
                            if ((m1.to == m2.from) || (m1.from == m2.to) ||
                                ((BitBoard.squaresBetween[m2.from][m2.to] & (1L << m1.from)) != 0)) {
                                // if the threat move was made possible by a reduced
                                // move on the previous ply, the reduction was unsafe.
                                // Return alpha to trigger a non-reduced re-search.
                                if (log != null) log.logNodeEnd(sti.nodeIdx, alpha, TTEntry.T_LE, evalScore, hKey);
                                return alpha;
                            }
                        }
                    }
                }
            }
        }

        // Razoring
        if ((Math.abs(alpha) <= MATE0 / 2) && (depth < 4*plyScale) && (beta == alpha + 1)) {
            if (evalScore == UNKNOWN_SCORE) {
                evalScore = eval.evalPos(pos);
            }
            final int razorMargin = 250;
            if (evalScore < beta - razorMargin) {
                qNodes--;
                totalNodes--;
                q0Eval = evalScore;
                int score = quiesce(alpha-razorMargin, beta-razorMargin, ply, 0, inCheck);
                if (score <= alpha-razorMargin) {
                    emptyMove.score = score;
                    tt.insert(hKey, emptyMove, TTEntry.T_LE, ply, depth, evalScore);
                    if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_LE, evalScore, hKey);
                    return score;
                }
            }
        }

        boolean futilityPrune = false;
        int futilityScore = alpha;
        if (!inCheck && (depth < 5*plyScale) && (posExtend == 0)) {
            if ((Math.abs(alpha) <= MATE0 / 2) && (Math.abs(beta) <= MATE0 / 2)) {
                int margin;
                if (depth <= plyScale) {
                    margin = 61;
                } else if (depth <= 2*plyScale) {
                    margin = 144;
                } else if (depth <= 3*plyScale) {
                    margin = 268;
                } else {
                    margin = 334;
                }
                if (evalScore == UNKNOWN_SCORE) {
                    evalScore = eval.evalPos(pos);
                }
                futilityScore = evalScore + margin;
                if (futilityScore <= alpha) {
                    futilityPrune = true;
                }
            }
        }

        if ((depth > 4*plyScale) && ((hashMove == null) || (hashMove.from == hashMove.to))) {
            boolean isPv = beta > alpha + 1;
            if (isPv || (depth > 8 * plyScale)) {
                // No hash move. Try internal iterative deepening.
                long savedNodeIdx = sti.nodeIdx;
                int newDepth = isPv ? depth  - 2 * plyScale : depth * 3 / 8;
                negaScout(alpha, beta, ply, newDepth, -1, inCheck);
                sti.nodeIdx = savedNodeIdx;
                ent = tt.probe(hKey);
                if (ent.type != TTEntry.T_EMPTY) {
                    hashMove = sti.hashMove;
                    ent.getMove(hashMove);
                }
            }
        }

        // Start searching move alternatives
        // FIXME! Try hash move before generating move list.
        MoveGen.MoveList moves;
        if (inCheck)
            moves = moveGen.checkEvasions(pos);
        else 
            moves = moveGen.pseudoLegalMoves(pos);
        boolean seeDone = false;
        boolean hashMoveSelected = true;
        if (!selectHashMove(moves, hashMove)) {
            scoreMoveList(moves, ply);
            seeDone = true;
            hashMoveSelected = false;
        }

        UndoInfo ui = sti.undoInfo;
        boolean haveLegalMoves = false;
        int illegalScore = -(MATE0-(ply+1));
        int b = beta;
        int bestScore = illegalScore;
        int bestMove = -1;
        int lmrCount = 0;
        for (int mi = 0; mi < moves.size; mi++) {
            if ((mi == 1) && !seeDone) {
                scoreMoveList(moves, ply, 1);
                seeDone = true;
            }
            if ((mi > 0) || !hashMoveSelected) {
                selectBest(moves, mi);
            }
            Move m = moves.m[mi];
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                int score = MATE0-ply;
                if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_EXACT, evalScore, hKey);
                return score;       // King capture
            }
            int newCaptureSquare = -1;
            boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY);
            boolean isPromotion = (m.promoteTo != Piece.EMPTY);
            int sVal;
            boolean mayReduce = (m.score < 53) && (!isCapture || m.score < 0) && !isPromotion;
            boolean givesCheck = MoveGen.givesCheck(pos, m); 
            boolean doFutility = false;
            if (futilityPrune && mayReduce && haveLegalMoves) {
                if (!givesCheck && notPassedPawnPush(pos, m))
                    doFutility = true;
            }
            int score;
            if (doFutility) {
                score = futilityScore;
            } else {
                int moveExtend = 0;
                if (posExtend == 0) {
                    final int pV = Evaluate.pV;
                    if ((m.to == recaptureSquare)) {
                        sVal = SEE(m);
                        int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                        if (sVal > tVal - pV / 2)
                            moveExtend = plyScale;
                    }
                    if ((moveExtend < plyScale) && isCapture && (pos.wMtrlPawns + pos.bMtrlPawns > pV)) {
                        // Extend if going into pawn endgame
                        int capVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                        if (pos.whiteMove) {
                            if ((pos.wMtrl == pos.wMtrlPawns) && (pos.bMtrl - pos.bMtrlPawns == capVal))
                                moveExtend = plyScale;
                        } else {
                            if ((pos.bMtrl == pos.bMtrlPawns) && (pos.wMtrl - pos.wMtrlPawns == capVal))
                                moveExtend = plyScale;
                        }
                    }
                }
                int extend = Math.max(posExtend, moveExtend);
                int lmr = 0;
                if ((depth >= 3*plyScale) && mayReduce && (extend == 0)) {
                    if (!givesCheck && notPassedPawnPush(pos, m)) {
                        lmrCount++;
                        if ((lmrCount > 3) && (depth > 3*plyScale) && !isCapture) {
                            lmr = 2*plyScale;
                        } else {
                            lmr = plyScale;
                        }
                    }
                }
                int newDepth = depth - plyScale + extend - lmr;
                if (isCapture && (givesCheck || (depth + extend) > plyScale)) {
                    // Compute recapture target square, but only if we are not going
                    // into q-search at the next ply.
                    int fVal = Evaluate.pieceValue[pos.getPiece(m.from)];
                    int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                    final int pV = Evaluate.pV;
                    if (Math.abs(tVal - fVal) < pV / 2) {    // "Equal" capture
                        sVal = SEE(m);
                        if (Math.abs(sVal) < pV / 2)
                            newCaptureSquare = m.to;
                    }
                }
                posHashList[posHashListSize++] = pos.zobristHash();
                pos.makeMove(m, ui);
                sti.currentMove = m;
                sti.lmr = lmr;
                score = -negaScout(-b, -alpha, ply + 1, newDepth, newCaptureSquare, givesCheck);
                if (((lmr > 0) && (score > alpha)) ||
                    ((score > alpha) && (score < beta) && (b != beta) && (score != illegalScore))) {
                    sti.lmr = 0;
                    newDepth += lmr;
                    score = -negaScout(-beta, -alpha, ply + 1, newDepth, newCaptureSquare, givesCheck);
                }
                posHashListSize--;
                pos.unMakeMove(m, ui);
            }
            if (weak && haveLegalMoves)
                if (weakPlaySkipMove(pos, m, ply))
                    score = illegalScore;
            m.score = score;

            if (score != illegalScore) {
                haveLegalMoves = true;
            }
            bestScore = Math.max(bestScore, score);
            if (score > alpha) {
                alpha = score;
                bestMove = mi;
                sti.bestMove.from      = m.from;
                sti.bestMove.to        = m.to;
                sti.bestMove.promoteTo = m.promoteTo;
            }
            if (alpha >= beta) {
                if (pos.getPiece(m.to) == Piece.EMPTY) {
                    kt.addKiller(ply, m);
                    ht.addSuccess(pos, m, depth/plyScale);
                    for (int mi2 = mi - 1; mi2 >= 0; mi2--) {
                        Move m2 = moves.m[mi2];
                        if (pos.getPiece(m2.to) == Piece.EMPTY)
                            ht.addFail(pos, m2, depth/plyScale);
                    }
                }
                tt.insert(hKey, m, TTEntry.T_GE, ply, depth, evalScore);
                moveGen.returnMoveList(moves);
                if (log != null) log.logNodeEnd(sti.nodeIdx, alpha, TTEntry.T_GE, evalScore, hKey);
                return alpha;
            }
            b = alpha + 1;
        }
        if (!haveLegalMoves && !inCheck) {
            moveGen.returnMoveList(moves);
            if (log != null) log.logNodeEnd(sti.nodeIdx, 0, TTEntry.T_EXACT, evalScore, hKey);
            return 0;       // Stale-mate
        }
        if (bestMove >= 0) {
            tt.insert(hKey, moves.m[bestMove], TTEntry.T_EXACT, ply, depth, evalScore);
            if (log != null) log.logNodeEnd(sti.nodeIdx, bestScore, TTEntry.T_EXACT, evalScore, hKey);
        } else {
            emptyMove.score = bestScore;
            tt.insert(hKey, emptyMove, TTEntry.T_LE, ply, depth, evalScore);
            if (log != null) log.logNodeEnd(sti.nodeIdx, bestScore, TTEntry.T_LE, evalScore, hKey);
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    /** Return true if move should be skipped in order to make engine play weaker. */
    private boolean weakPlaySkipMove(Position pos, Move m, int ply) {
        long rndL = pos.zobristHash() ^ Position.psHashKeys[0][m.from] ^
                    Position.psHashKeys[0][m.to] ^ randomSeed;
        double rnd = ((rndL & 0x7fffffffffffffffL) % 1000000000) / 1e9;

        double s = strength * 1e-3;
        double offs = 4 - 15 * s;
        double effPly = ply * Evaluate.interpolate(pos.wMtrl + pos.bMtrl, 0, 30, Evaluate.qV * 4, 100) * 1e-2;
        double t = effPly + offs;
        double p = 1/(1+Math.exp(t)); // Probability to "see" move
        boolean easyMove = ((pos.getPiece(m.to) != Piece.EMPTY) ||
                            (ply < 2) || (searchTreeInfo[ply-2].currentMove.to == m.from));
        if (easyMove)
            p = 1-(1-p)*(1-p);
        return rnd > p;
    }

    private static boolean notPassedPawnPush(Position pos, Move m) {
        int p = pos.getPiece(m.from);
        if (pos.whiteMove) {
            if (p != Piece.WPAWN)
                return true;
            if ((BitBoard.wPawnBlockerMask[m.to] & pos.pieceTypeBB[Piece.BPAWN]) != 0)
                return true;
            return m.to < 40;
        } else {
            if (p != Piece.BPAWN)
                return true;
            if ((BitBoard.bPawnBlockerMask[m.to] & pos.pieceTypeBB[Piece.WPAWN]) != 0)
                return true;
            return m.to > 23;
        }
    }

    /**
     * Quiescence search. Only non-losing captures are searched.
     */
    private int quiesce(int alpha, int beta, int ply, int depth, final boolean inCheck) {
        qNodes++;
        totalNodes++;
        int score;
        if (inCheck) {
            score = -(MATE0 - (ply+1));
        } else {
            if ((depth == 0) && (q0Eval != UNKNOWN_SCORE)) {
                score = q0Eval;
            } else {
                score = eval.evalPos(pos);
                if (depth == 0)
                    q0Eval = score;
            }
        }
        if (score >= beta) {
            if ((depth == 0) && (score < MATE0 - ply)) {
                if (MoveGen.canTakeKing(pos)) {
                    // To make stale-mate detection work
                    score = MATE0 - ply;
                }
            }
            return score;
        }
        final int evalScore = score;
        if (score > alpha)
            alpha = score;
        int bestScore = score;
        final boolean tryChecks = (depth > -3);
        MoveGen.MoveList moves;
        if (inCheck) {
            moves = moveGen.checkEvasions(pos);
        } else if (tryChecks) {
            moves = moveGen.pseudoLegalCapturesAndChecks(pos);
        } else {
            moves = moveGen.pseudoLegalCaptures(pos);
        }
        scoreMoveListMvvLva(moves);
        UndoInfo ui = searchTreeInfo[ply].undoInfo;
        for (int mi = 0; mi < moves.size; mi++) {
            if (mi < 8) {
                // If the first 8 moves didn't fail high, this is probably an ALL-node,
                // so spending more effort on move ordering is probably wasted time.
                selectBest(moves, mi);
            }
            Move m = moves.m[mi];
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                return MATE0-ply;       // King capture
            }
            boolean givesCheck = false;
            boolean givesCheckComputed = false;
            if (!inCheck) {
                if ((pos.getPiece(m.to) == Piece.EMPTY) && (m.promoteTo == Piece.EMPTY)) {
                    // Non-capture
                    if (!tryChecks)
                        continue;
                    givesCheck = MoveGen.givesCheck(pos, m);
                    givesCheckComputed = true;
                    if (!givesCheck)
                        continue;
                    if (negSEE(m)) // Needed because m.score is not computed for non-captures
                        continue;
                } else {
                    if (negSEE(m))
                        continue;
                    int capt = Evaluate.pieceValue[pos.getPiece(m.to)];
                    int prom = Evaluate.pieceValue[m.promoteTo];
                    int optimisticScore = evalScore + capt + prom + 200;
                    if (optimisticScore < alpha) { // Delta pruning
                        if ((pos.wMtrlPawns > 0) && (pos.wMtrl > capt + pos.wMtrlPawns) &&
                            (pos.bMtrlPawns > 0) && (pos.bMtrl > capt + pos.bMtrlPawns)) {
                            if (depth -1 > -4) {
                                givesCheck = MoveGen.givesCheck(pos, m);
                                givesCheckComputed = true;
                            }
                            if (!givesCheck) {
                                if (optimisticScore > bestScore)
                                    bestScore = optimisticScore;
                                continue;
                            }
                        }
                    }
                }
            }

            if (!givesCheckComputed) {
                if (depth - 1 > -4) {
                    givesCheck = MoveGen.givesCheck(pos, m);
                }
            }
            final boolean nextInCheck = (depth - 1) > -4 && givesCheck;

            pos.makeMove(m, ui);
            score = -quiesce(-beta, -alpha, ply + 1, depth - 1, nextInCheck);
            pos.unMakeMove(m, ui);
            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    alpha = score;
                    if (alpha >= beta) {
                        moveGen.returnMoveList(moves);
                        return alpha;
                    }
                }
            }
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    /** Return >0, 0, <0, depending on the sign of SEE(m). */
    final public int signSEE(Move m) {
        int p0 = Evaluate.pieceValue[pos.getPiece(m.from)];
        int p1 = Evaluate.pieceValue[pos.getPiece(m.to)];
        if (p0 < p1)
            return 1;
        return SEE(m);
    }

    /** Return true if SEE(m) < 0. */
    final public boolean negSEE(Move m) {
        int p0 = Evaluate.pieceValue[pos.getPiece(m.from)];
        int p1 = Evaluate.pieceValue[pos.getPiece(m.to)];
        if (p1 >= p0)
            return false;
        return SEE(m) < 0;
    }

    private final int[] captures = new int[64];   // Value of captured pieces
    private final UndoInfo seeUi = new UndoInfo();

    /**
     * Static exchange evaluation function.
     * @return SEE score for m. Positive value is good for the side that makes the first move.
     */
    final public int SEE(Move m) {
        final int kV = Evaluate.kV;
        
        final int square = m.to;
        if (square == pos.getEpSquare()) {
            captures[0] = Evaluate.pV;
        } else {
            captures[0] = Evaluate.pieceValue[pos.getPiece(square)];
            if (captures[0] == kV)
                return kV;
        }
        int nCapt = 1;                  // Number of entries in captures[]

        pos.makeSEEMove(m, seeUi);
        boolean white = pos.whiteMove;
        int valOnSquare = Evaluate.pieceValue[pos.getPiece(square)];
        long occupied = pos.whiteBB | pos.blackBB;
        while (true) {
            int bestValue;
            long atk;
            if (white) {
                atk = BitBoard.bPawnAttacks[square] & pos.pieceTypeBB[Piece.WPAWN] & occupied;
                if (atk != 0) {
                    bestValue = Evaluate.pV;
                } else {
                    atk = BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.WKNIGHT] & occupied;
                    if (atk != 0) {
                        bestValue = Evaluate.nV;
                    } else {
                        long bAtk = BitBoard.bishopAttacks(square, occupied) & occupied;
                        atk = bAtk & pos.pieceTypeBB[Piece.WBISHOP];
                        if (atk != 0) {
                            bestValue = Evaluate.bV;
                        } else {
                            long rAtk = BitBoard.rookAttacks(square, occupied) & occupied;
                            atk = rAtk & pos.pieceTypeBB[Piece.WROOK];
                            if (atk != 0) {
                                bestValue = Evaluate.rV;
                            } else {
                                atk = (bAtk | rAtk) & pos.pieceTypeBB[Piece.WQUEEN];
                                if (atk != 0) {
                                    bestValue = Evaluate.qV;
                                } else {
                                    atk = BitBoard.kingAttacks[square] & pos.pieceTypeBB[Piece.WKING] & occupied;
                                    if (atk != 0) {
                                        bestValue = kV;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                atk = BitBoard.wPawnAttacks[square] & pos.pieceTypeBB[Piece.BPAWN] & occupied;
                if (atk != 0) {
                    bestValue = Evaluate.pV;
                } else {
                    atk = BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.BKNIGHT] & occupied;
                    if (atk != 0) {
                        bestValue = Evaluate.nV;
                    } else {
                        long bAtk = BitBoard.bishopAttacks(square, occupied) & occupied;
                        atk = bAtk & pos.pieceTypeBB[Piece.BBISHOP];
                        if (atk != 0) {
                            bestValue = Evaluate.bV;
                        } else {
                            long rAtk = BitBoard.rookAttacks(square, occupied) & occupied;
                            atk = rAtk & pos.pieceTypeBB[Piece.BROOK];
                            if (atk != 0) {
                                bestValue = Evaluate.rV;
                            } else {
                                atk = (bAtk | rAtk) & pos.pieceTypeBB[Piece.BQUEEN];
                                if (atk != 0) {
                                    bestValue = Evaluate.qV;
                                } else {
                                    atk = BitBoard.kingAttacks[square] & pos.pieceTypeBB[Piece.BKING] & occupied;
                                    if (atk != 0) {
                                        bestValue = kV;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            captures[nCapt++] = valOnSquare;
            if (valOnSquare == kV)
                break;
            valOnSquare = bestValue;
            occupied &= ~(atk & -atk);
            white = !white;
        }
        pos.unMakeSEEMove(m, seeUi);
        
        int score = 0;
        for (int i = nCapt - 1; i > 0; i--) {
            score = Math.max(0, captures[i] - score);
        }
        return captures[0] - score;
    }

    /**
     * Compute scores for each move in a move list, using SEE, killer and history information.
     * @param moves  List of moves to score.
     */
    final void scoreMoveList(MoveGen.MoveList moves, int ply) {
        scoreMoveList(moves, ply, 0);
    }
    final void scoreMoveList(MoveGen.MoveList moves, int ply, int startIdx) {
        for (int i = startIdx; i < moves.size; i++) {
            Move m = moves.m[i];
            boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY) || (m.promoteTo != Piece.EMPTY);
            int score = 0;
            if (isCapture) {
                int seeScore = signSEE(m);
                int v = pos.getPiece(m.to);
                int a = pos.getPiece(m.from);
                score = Evaluate.pieceValue[v]/10 * 1000 - Evaluate.pieceValue[a]/10;
                if (seeScore > 0)
                    score += 2000000;
                else if (seeScore == 0)
                    score += 1000000;
                else
                    score -= 1000000;
                score *= 100;
            }
            int ks = kt.getKillerScore(ply, m);
            if (ks > 0) {
                score += ks + 50;
            } else {
                int hs = ht.getHistScore(pos, m);
                score += hs;
            }
            m.score = score;
        }
    }
    private void scoreMoveListMvvLva(MoveGen.MoveList moves) {
        for (int i = 0; i < moves.size; i++) {
            Move m = moves.m[i];
            int v = pos.getPiece(m.to);
            int a = pos.getPiece(m.from);
            m.score = Evaluate.pieceValue[v] * 10000 - Evaluate.pieceValue[a];
        }
    }

    /**
     * Find move with the highest score and move it to the front of the list.
     */
    static void selectBest(MoveGen.MoveList moves, int startIdx) {
        int bestIdx = startIdx;
        int bestScore = moves.m[bestIdx].score;
        for (int i = startIdx + 1; i < moves.size; i++) {
            int sc = moves.m[i].score;
            if (sc > bestScore) {
                bestIdx = i;
                bestScore = sc;
            }
        }
        if (bestIdx != startIdx) {
            Move m = moves.m[startIdx];
            moves.m[startIdx] = moves.m[bestIdx];
            moves.m[bestIdx] = m;
        }
    }

    /** If hashMove exists in the move list, move the hash move to the front of the list. */
    static boolean selectHashMove(MoveGen.MoveList moves, Move hashMove) {
        if (hashMove == null) {
            return false;
        }
        for (int i = 0; i < moves.size; i++) {
            Move m = moves.m[i];
            if (m.equals(hashMove)) {
                moves.m[i] = moves.m[0];
                moves.m[0] = m;
                m.score = 10000;
                return true;
            }
        }
        return false;
    }

    public static boolean canClaimDraw50(Position pos) {
        return (pos.halfMoveClock >= 100);
    }
    
    public static boolean canClaimDrawRep(Position pos, long[] posHashList, int posHashListSize, int posHashFirstNew) {
        int reps = 0;
        for (int i = posHashListSize - 4; i >= 0; i -= 2) {
            if (pos.zobristHash() == posHashList[i]) {
                reps++;
                if (i >= posHashFirstNew) {
                    reps++;
                    break;
                }
            }
        }
        return (reps >= 2);
    }

    private void initNodeStats() {
        nodes = qNodes = 0;
        nodesPlyVec = new int[20];
        nodesDepthVec = new int[20];
        for (int i = 0; i < 20; i++) {
            nodesPlyVec[i] = 0;
            nodesDepthVec[i] = 0;
        }
    }
}
