/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uci;

import chess.ChessParseError;
import chess.Move;
import chess.Piece;
import chess.Position;
import chess.TextIO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class UCIProtocolTest {

    public UCIProtocolTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of tokenize method, of class UCIProtocol.
     */
    @Test
    public void testTokenize() {
        System.out.println("tokenize");
        UCIProtocol uci = new UCIProtocol();
        String[] result = uci.tokenize("  a b   c de \t \t fgh");
        assertEquals(5, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("de", result[3]);
        assertEquals("fgh", result[4]);
    }

    /**
     * Test of stringToMove method, of class UCIProtocol.
     */
    @Test
    public void testStringToMove() throws ChessParseError {
        System.out.println("stringToMove");
        UCIProtocol uci = new UCIProtocol();
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        Move m = uci.stringToMove("e2e4");
        assertEquals(TextIO.stringToMove(pos, "e4"), m);
        m = uci.stringToMove("e2e5");
        assertEquals(new Move(12, 12+8*3, Piece.EMPTY), m);

        m = uci.stringToMove("e2e5q");
        assertEquals(null, m);

        m = uci.stringToMove("e7e8q");
        assertEquals(Piece.WQUEEN, m.promoteTo);
        m = uci.stringToMove("e7e8r");
        assertEquals(Piece.WROOK, m.promoteTo);
        m = uci.stringToMove("e7e8b");
        assertEquals(Piece.WBISHOP, m.promoteTo);
        m = uci.stringToMove("e2e1n");
        assertEquals(Piece.BKNIGHT, m.promoteTo);
        m = uci.stringToMove("e7e8x");
        assertEquals(null, m);  // Invalid promotion piece
        m = uci.stringToMove("i1i3");
        assertEquals(null, m);  // Outside board
    }
}
