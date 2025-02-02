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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 *
 * @author petero
 */
public class UCIProtocolTest {

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
}
