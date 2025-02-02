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

import java.io.Serial;

/**
 * Exception class to represent parse errors in FEN or algebraic notation.
 * @author petero
 */
public class ChessParseError extends Exception {
    @Serial
    private static final long serialVersionUID = -6051856171275301175L;
    public ChessParseError() {
    }
    public ChessParseError(String msg) {
        super(msg);
    }
}
