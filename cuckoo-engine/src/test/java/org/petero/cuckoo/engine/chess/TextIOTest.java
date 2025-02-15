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

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Optional;

/**
 *
 * @author petero
 */
public class TextIOTest {

    private Move move(Position pos, String strMove) {
        Optional<Move> optionalMove = TextIO.stringToMove(pos, strMove);
        assertTrue(optionalMove.isPresent());
        return optionalMove.get();
    }

    private Move uciMove(String strMove) {
        Optional<Move> optionalMove = TextIO.uciStringToMove(strMove);
        assertTrue(optionalMove.isPresent());
        return optionalMove.get();
    }

    /**
     * Test of readFEN method, of class TextIO.
     */
    @Test
    public void testReadFEN() throws ChessParseError {
        String fen = "rnbqk2r/1p3ppp/p7/1NpPp3/QPP1P1n1/P4N2/4KbPP/R1B2B1R b kq - 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        assertEquals(Piece.WQUEEN, pos.getPiece(Position.getSquare(0, 3)));
        assertEquals(Piece.BKING, pos.getPiece(Position.getSquare(4, 7)));
        assertEquals(Piece.WKING, pos.getPiece(Position.getSquare(4, 1)));
        assertFalse(pos.whiteMove);
        assertFalse(pos.a1Castle());
        assertFalse(pos.h1Castle());
        assertTrue(pos.a8Castle());
        assertTrue(pos.h8Castle());

        fen = "8/3k4/8/5pP1/1P6/1NB5/2QP4/R3K2R w KQ f6 1 2";
        pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        assertEquals(1, pos.halfMoveClock);
        assertEquals(2, pos.fullMoveCounter);

        // Must have exactly one king
        boolean wasError = testFENParseError("8/8/8/8/8/8/8/kk1K4 w - - 0 1");
        assertTrue(wasError);

        // Must not be possible to capture the king
        wasError = testFENParseError("8/8/8/8/8/8/8/k1RK4 w - - 0 1");
        assertTrue(wasError);
        
        // Make sure bogus en passant square information is removed
        fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        pos = TextIO.readFEN(fen);
        assertEquals(-1, pos.getEpSquare());
        
        // Test for too many rows (slashes)
        wasError = testFENParseError("8/8/8/8/4k3/8/8/8/KBN5 w - - 0 1");
        assertTrue(wasError);
        
        // Test for too many columns
        wasError = testFENParseError("8K/8/8/8/4k3/8/8/8 w - - 0 1");
        assertTrue(wasError);
        
        // Pawns must not be on first/last rank
        wasError = testFENParseError("kp6/8/8/8/8/8/8/K7 w - - 0 1");
        assertTrue(wasError);
        
        wasError = testFENParseError("kr/pppp/8/8/8/8/8/KBR w");
        assertFalse(wasError);  // OK not to specify castling flags and ep square
        
        wasError = testFENParseError("k/8/8/8/8/8/8/K");
        assertTrue(wasError);   // Error side to move not specified
        
        wasError = testFENParseError("");
        assertTrue(wasError);

        wasError = testFENParseError("    |");
        assertTrue(wasError);

        wasError = testFENParseError("1B1B4/6k1/7r/7P/6q1/r7/q7/7K b - - acn 6; acs 0;");
        assertFalse(wasError);  // Extra stuff after FEN string is allowed
    }

    /** Tests if trying to parse a FEN string causes an error. */
    private boolean testFENParseError(String fen) {
        boolean wasError;
        wasError = false;
        try {
            TextIO.readFEN(fen);
        } catch (ChessParseError err) {
            wasError = true;
        }
        return wasError;
    }
    
    /**
     * Test of moveToString method, of class TextIO.
     */
    @Test
    public void testMoveToString() throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.START_POS_FEN);
        assertEquals(TextIO.START_POS_FEN, TextIO.toFEN(pos));
        Move move = new Move(Position.getSquare(4, 1), Position.getSquare(4, 3),
                Piece.EMPTY);
        boolean longForm = true;
        String result = TextIO.moveToString(pos, move, longForm);
        assertEquals("e2-e4", result);

        move = new Move(Position.getSquare(6, 0), Position.getSquare(5, 2), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Ng1-f3", result);
        
        move = new Move(Position.getSquare(4, 7), Position.getSquare(2, 7),
                Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("O-O-O", result);

        String fen = "1r3k2/2P5/8/8/8/4K3/8/8 w - - 0 1";
        pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        move = new Move(Position.getSquare(2,6), Position.getSquare(1,7), Piece.WROOK);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("c7xb8R+", result);

        move = new Move(Position.getSquare(2,6), Position.getSquare(2,7), Piece.WKNIGHT);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("c7-c8N", result);
        
        move = new Move(Position.getSquare(2,6), Position.getSquare(2,7), Piece.WQUEEN);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("c7-c8Q+", result);
    }

    /**
     * Test of moveToString method, of class TextIO, mate/stalemate tests.
     */
    @Test
    public void testMoveToStringMate() throws ChessParseError {
        Position pos = TextIO.readFEN("3k4/1PR5/3N4/8/4K3/8/8/8 w - - 0 1");
        boolean longForm = true;

        Move move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WROOK);
        String result = TextIO.moveToString(pos, move, longForm);
        assertEquals("b7-b8R+", result);    // check
        
        move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WQUEEN);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("b7-b8Q#", result);    // check mate
        
        move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WKNIGHT);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("b7-b8N", result);

        move = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WBISHOP);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("b7-b8B", result);     // stalemate
    }

    /**
     * Test of moveToString method, of class TextIO, short form.
     */
    @Test
    public void testMoveToStringShortForm() throws ChessParseError {
        String fen = "r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1";
        Position pos = TextIO.readFEN(fen);
        assertEquals(fen, TextIO.toFEN(pos));
        boolean longForm = false;
        
        Move move = new Move(Position.getSquare(4,5), Position.getSquare(4,3), Piece.EMPTY);
        String result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Qee4", result);   // File disambiguation needed

        move = new Move(Position.getSquare(2,5), Position.getSquare(4,3), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Qc6e4", result);  // Full disambiguation needed

        move = new Move(Position.getSquare(2,3), Position.getSquare(4,3), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Q4e4", result);   // Row disambiguation needed

        move = new Move(Position.getSquare(2,3), Position.getSquare(2,0), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Qc1+", result);   // No disambiguation needed

        move = new Move(Position.getSquare(0,1), Position.getSquare(0,0), Piece.BQUEEN);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("a1Q", result);    // Normal promotion

        move = new Move(Position.getSquare(0,1), Position.getSquare(1,0), Piece.BQUEEN);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("axb1Q#", result); // Capture promotion and check mate

        move = new Move(Position.getSquare(0,1), Position.getSquare(1,0), Piece.BKNIGHT);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("axb1N", result);  // Capture promotion

        move = new Move(Position.getSquare(3,6), Position.getSquare(4,4), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Ne5", result);    // Other knight pinned, no disambiguation needed

        move = new Move(Position.getSquare(7,6), Position.getSquare(7,4), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("h5", result);     // Regular pawn move
        
        move = new Move(Position.getSquare(5,7), Position.getSquare(3,7), Piece.EMPTY);
        result = TextIO.moveToString(pos, move, longForm);
        assertEquals("Rfd8", result);     // File disambiguation needed
    }

  @Test
  public void testStringToMove_mNe5() throws ChessParseError {
      Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");

      Optional<Move> m;

      Move mNe5 = new Move(Position.getSquare(3, 6), Position.getSquare(4, 4), Piece.EMPTY);
      m = TextIO.stringToMove(pos, "Ne5");
      assertEquals(Optional.of(mNe5), m);
      m = TextIO.stringToMove(pos, "ne");
      assertEquals(Optional.of(mNe5), m);
      m = TextIO.stringToMove(pos, "N");
      assertEquals(Optional.empty(), m);
  }

    @Test
    public void testStringToMove_mQc6e4() throws ChessParseError {
        Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");
        Optional<Move> m;
        Move mQc6e4 = new Move(Position.getSquare(2, 5), Position.getSquare(4, 3), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "Qc6-e4");
        assertEquals(Optional.of(mQc6e4), m);
        m = TextIO.stringToMove(pos, "Qc6e4");
        assertEquals(Optional.of(mQc6e4), m);
        m = TextIO.stringToMove(pos, "Qce4");
        assertEquals(Optional.empty(), m);
        m = TextIO.stringToMove(pos, "Q6e4");
        assertEquals(Optional.empty(), m);
    }

    @Test
    public void testStringToMove_maxb1Q() throws ChessParseError {
        Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");
        Optional<Move> m;
        Move maxb1Q = new Move(Position.getSquare(0, 1), Position.getSquare(1, 0), Piece.BQUEEN);
        m = TextIO.stringToMove(pos, "axb1Q");
        assertEquals(Optional.of(maxb1Q), m);
        m = TextIO.stringToMove(pos, "axb1Q#");
        assertEquals(Optional.of(maxb1Q), m);
        m = TextIO.stringToMove(pos, "axb1Q+");
        assertEquals(Optional.empty(), m);
    }

    @Test
    public void testStringToMove_mh5() throws ChessParseError {
        Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");
        Optional<Move> m;
        Move mh5 = new Move(Position.getSquare(7, 6), Position.getSquare(7, 4), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "h5");
        assertEquals(Optional.of(mh5), m);
        m = TextIO.stringToMove(pos, "h7-h5");
        assertEquals(Optional.of(mh5), m);
        m = TextIO.stringToMove(pos, "h");
        assertEquals(Optional.empty(), m);
    }

    @Test
    public void testStringToMove_from() throws ChessParseError {
        Position pos = TextIO.readFEN("r1b1k2r/1pqpppbp/p5pn/3BP3/8/2pP4/PPPBQPPP/R3K2R w KQkq - 0 12");
        Move m;
        m = move(pos, "bxc3");
        assertEquals(TextIO.getSquare("b2"), m.from);
        m = move(pos, "Bxc3");
        assertEquals(TextIO.getSquare("d2"), m.from);
        m = move(pos, "bxc");
        assertEquals(TextIO.getSquare("b2"), m.from);
        m = move(pos, "Bxc");
        assertEquals(TextIO.getSquare("d2"), m.from);
    }

    @Test
    public void testStringToMove_castling() throws ChessParseError {
        // Test castling. o-o is a substring of o-o-o, which could cause problems.
        Position pos = TextIO.readFEN("5k2/p1pQn3/1p2Bp1r/8/4P1pN/2N5/PPP2PPP/R3K2R w KQ - 0 16");
        Optional<Move> m;
        Move kCastle = new Move(Position.getSquare(4,0), Position.getSquare(6,0), Piece.EMPTY);
        Move qCastle = new Move(Position.getSquare(4,0), Position.getSquare(2,0), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "o");
        assertEquals(Optional.empty(), m);
        m = TextIO.stringToMove(pos, "o-o");
        assertEquals(Optional.of(kCastle), m);
        m = TextIO.stringToMove(pos, "O-O");
        assertEquals(Optional.of(kCastle), m);
        m = TextIO.stringToMove(pos, "o-o-o");
        assertEquals(Optional.of(qCastle), m);
    }

    @Test
    public void testStringToMove_castlingcheck() throws ChessParseError {
        // Test 'o-o+'
        Position pos = TextIO.readFEN("5k2/p1pQn3/1p2Bp1r/8/4P1pN/2N5/PPP2PPP/R3K2R w KQ - 0 16");
        pos.setPiece(Position.getSquare(5,1), Piece.EMPTY);
        pos.setPiece(Position.getSquare(5,5), Piece.EMPTY);
        Optional<Move> m;
        Move kCastle = new Move(Position.getSquare(4,0), Position.getSquare(6,0), Piece.EMPTY);
        Move qCastle = new Move(Position.getSquare(4,0), Position.getSquare(2,0), Piece.EMPTY);
        m = TextIO.stringToMove(pos, "o");
        assertEquals(Optional.empty(), m);
        m = TextIO.stringToMove(pos, "o-o");
        assertEquals(Optional.of(kCastle), m);
        m = TextIO.stringToMove(pos, "o-o-o");
        assertEquals(Optional.of(qCastle), m);
        m = TextIO.stringToMove(pos, "o-o+");
        assertEquals(Optional.of(kCastle), m);
    }

    /**
     * Test of stringToMove method, of class TextIO.
     */
    @Test
    public void testStringToMove_syntax() throws ChessParseError {
        // Test d8=Q+ syntax
        Position pos = TextIO.readFEN("1r3r2/2kP2Rp/p1bN1p2/2p5/5P2/2P5/P5PP/3R2K1 w - -");
        Optional<Move> m = TextIO.stringToMove(pos, "d8=Q+");
        Optional<Move> m2 = TextIO.stringToMove(pos, "d8Q");
        assertEquals(m2, m);
    }

    /**
     * Test of getSquare method, of class TextIO.
     */
    @Test
    public void testGetSquare() {
        assertEquals(Position.getSquare(0, 0), TextIO.getSquare("a1"));
        assertEquals(Position.getSquare(1, 7), TextIO.getSquare("b8"));
        assertEquals(Position.getSquare(3, 3), TextIO.getSquare("d4"));
        assertEquals(Position.getSquare(4, 3), TextIO.getSquare("e4"));
        assertEquals(Position.getSquare(3, 1), TextIO.getSquare("d2"));
        assertEquals(Position.getSquare(7, 7), TextIO.getSquare("h8"));
    }

    /**
     * Test of squareToString method, of class TextIO.
     */
    @Test
    public void testSquareToString() {
        assertEquals("a1", TextIO.squareToString(Position.getSquare(0, 0)));
        assertEquals("h6", TextIO.squareToString(Position.getSquare(7, 5)));
        assertEquals("e4", TextIO.squareToString(Position.getSquare(4, 3)));
    }

    /**
     * Test of asciiBoard method, of class TextIO.
     */
    @Test
    public void testAsciiBoard() throws ChessParseError {
        Position pos = TextIO.readFEN("r4rk1/2pn3p/2q1q1n1/8/2q2p2/6R1/p4PPP/1R4K1 b - - 0 1");
        String aBrd = TextIO.asciiBoard(pos);
        assertEquals(12, aBrd.length() - aBrd.replaceAll("\\*", "").length()); // 12 black pieces
        assertEquals(3, aBrd.length() - aBrd.replaceAll("\\*Q", " ").length()); // 3 black queens
        assertEquals(3, aBrd.length() - aBrd.replaceAll(" P", " ").length()); // 3 white pawns
    }
    
    /**
     * Test of uciStringToMove method, of class TextIO.
     */
    @Test
    public void testUciStringToMove() throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.START_POS_FEN);
        Optional<Move> m = TextIO.uciStringToMove("e2e4");
        assertEquals(TextIO.stringToMove(pos, "e4"), m);
        m = TextIO.uciStringToMove("e2e5");
        assertEquals(Optional.of(new Move(12, 12+8*3, Piece.EMPTY)), m);

        m = TextIO.uciStringToMove("e2e5q");
        assertEquals(Optional.empty(), m);

        Move uciMove;
        uciMove = uciMove("e7e8q");
        assertEquals(Piece.WQUEEN, uciMove.promoteTo);
        uciMove = uciMove("e7e8r");
        assertEquals(Piece.WROOK, uciMove.promoteTo);
        uciMove = uciMove("e7e8b");
        assertEquals(Piece.WBISHOP, uciMove.promoteTo);
        uciMove = uciMove("e2e1n");
        assertEquals(Piece.BKNIGHT, uciMove.promoteTo);
        Optional<Move> uciMoveOpt;
        uciMoveOpt = TextIO.uciStringToMove("e7e8x");
        assertEquals(Optional.empty(), uciMoveOpt);  // Invalid promotion piece
        uciMoveOpt = TextIO.uciStringToMove("i1i3");
        assertEquals(Optional.empty(), uciMoveOpt);  // Outside board
    }
}
