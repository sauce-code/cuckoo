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

/**
 *
 * @author petero
 */
public class MoveTest {
    
    /**
     * Test of move constructor, of class Move.
     */
    @Test
    public void testMoveConstructor() {
        System.out.println("MoveTest");
        int f = Position.getSquare(4, 1);
        int t = Position.getSquare(4, 3);
        int p = Piece.WROOK;
        Move move = new Move(f, t, p);
        assertEquals(move.from, f);
        assertEquals(move.to,t);
        assertEquals(move.promoteTo, p);
    }
    
    /**
     * Test of equals, of class Move.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Move m1 = new Move(Position.getSquare(0, 6), Position.getSquare(1, 7), Piece.WROOK);
        Move m2 = new Move(Position.getSquare(0, 6), Position.getSquare(0, 7), Piece.WROOK);
        Move m3 = new Move(Position.getSquare(1, 6), Position.getSquare(1, 7), Piece.WROOK);
        Move m4 = new Move(Position.getSquare(0, 6), Position.getSquare(1, 7), Piece.WKNIGHT);
        Move m5 = new Move(Position.getSquare(0, 6), Position.getSquare(1, 7), Piece.WROOK);
        assertFalse(m1.equals(m2));
        assertFalse(m1.equals(m3));
        assertFalse(m1.equals(m4));
        assertEquals(m1, m5);
    }
}
