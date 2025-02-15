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
public class HistoryTest {

    private Move move(Position pos, String strMove) {
        Optional<Move> optionalMove = TextIO.stringToMove(pos, strMove);
        assertTrue(optionalMove.isPresent());
        return optionalMove.get();
    }

    /**
     * Test of getHistScore method, of class History.
     */
    @Test
    public void testGetHistScore() throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.START_POS_FEN);
        History hs = new History();
        Move m1 = move(pos, "e4");
        Move m2 = move(pos, "d4");
        assertEquals(0, hs.getHistScore(pos, m1));

        hs.addSuccess(pos, m1, 1);
        assertEquals(49, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addSuccess(pos, m1, 1);
        assertEquals(49, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addFail(pos, m1, 1);
        assertEquals(2 * 49 / 3, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addFail(pos, m1, 1);
        assertEquals(2 * 49 / 4, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addSuccess(pos, m2, 1);
        assertEquals(2 * 49 / 4, hs.getHistScore(pos, m1));
        assertEquals(49, hs.getHistScore(pos, m2));
    }
}
