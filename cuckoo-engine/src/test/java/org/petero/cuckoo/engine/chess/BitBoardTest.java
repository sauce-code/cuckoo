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

public class BitBoardTest {

    /** Test of kingAttacks, of class BitBoard. */
    @Test
    public void testKingAttacks() {
        System.out.println("kingAttacks");
        assertEquals(5, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("g1")]));
        assertEquals(3, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("h1")]));
        assertEquals(3, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("a1")]));
        assertEquals(5, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("a2")]));
        assertEquals(3, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("h8")]));
        assertEquals(5, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("a6")]));
        assertEquals(8, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("b2")]));
    }

    /** Test of knightAttacks, of class BitBoard. */
    @Test
    public void testKnightAttacks() {
        System.out.println("knightAttacks");
        assertEquals(3, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("g1")]));
        assertEquals(2, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("a1")]));
        assertEquals(2, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("h1")]));
        assertEquals(4, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("h6")]));
        assertEquals(4, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("b7")]));
        assertEquals(8, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("c6")]));
        assertEquals((1L<<TextIO.getSquare("e2")) |
                     (1L<<TextIO.getSquare("f3")) |
                     (1L<<TextIO.getSquare("h3")),
                     BitBoard.knightAttacks[TextIO.getSquare("g1")]);
    }
    
    /** Test of squaresBetween[][], of class BitBoard. */
    @Test
    public void testSquaresBetween() {
        System.out.println("squaresBetween");
        // Tests that the set of nonzero elements is correct
        for (int sq1 = 0; sq1 < 64; sq1++) {
            for (int sq2 = 0; sq2 < 64; sq2++) {
                int d = BitBoard.getDirection(sq1, sq2);
                if (d == 0) {
                    assertEquals(0, BitBoard.squaresBetween[sq1][sq2]);
                } else {
                    int dx = Position.getX(sq1) - Position.getX(sq2);
                    int dy = Position.getY(sq1) - Position.getY(sq2);
                    if (Math.abs(dx * dy) == 2) { // Knight direction
                        assertEquals(0, BitBoard.squaresBetween[sq1][sq2]);
                    } else {
                        if ((Math.abs(dx) > 1) || (Math.abs(dy) > 1)) {
                            assertNotEquals(0, BitBoard.squaresBetween[sq1][sq2]);
                        } else {
                            assertEquals(0, BitBoard.squaresBetween[sq1][sq2]);
                        }
                    }
                }
            }
        }

        assertEquals(0x0040201008040200L, BitBoard.squaresBetween[0][63]);
        assertEquals(0x000000001C000000L, BitBoard.squaresBetween[TextIO.getSquare("b4")][TextIO.getSquare("f4")]);
    }

    /**
     * If there is a piece type that can move from "from" to "to", return the
     * corresponding direction, 8*dy+dx.
     */
    private static int computeDirection(int from, int to) {
        int dx = Position.getX(to) - Position.getX(from);
        int dy = Position.getY(to) - Position.getY(from);
        if (dx == 0) {                   // Vertical rook direction
            if (dy == 0) return 0;
            return (dy > 0) ? 8 : -8;
        }
        if (dy == 0)                    // Horizontal rook direction
            return (dx > 0) ? 1 : -1;
        if (Math.abs(dx) == Math.abs(dy)) // Bishop direction
            return ((dy > 0) ? 8 : -8) + (dx > 0 ? 1 : -1);
        if (Math.abs(dx * dy) == 2)     // Knight direction
            return dy * 8 + dx;
        return 0;
    }

    @Test
    public void testGetDirection() {
        System.out.println("getDirection");
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                assertEquals(computeDirection(from, to), BitBoard.getDirection(from, to));
            }
        }
    }

    @Test
    public void testTrailingZeros() {
        System.out.println("trailingZeros");
        for (int i = 0; i < 64; i++) {
            long mask = 1L << i;
            assertEquals(i, BitBoard.numberOfTrailingZeros(mask));
        }
    }
}
