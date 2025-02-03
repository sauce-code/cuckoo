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

/**
 *
 * @author petero
 */
public class Move {
    /** From square, 0-63. */
    public int from;

    /** To square, 0-63. */
    public int to;

    /** Promotion piece. */
    public int promoteTo;

    public int score;
    
    /** Create a move object. */
    public Move(int from, int to, int promoteTo) {
        this.from = from;
        this.to = to;
        this.promoteTo = promoteTo;
        this.score = 0;
    }

    public Move(int from, int to, int promoteTo, int score) {
        this.from = from;
        this.to = to;
        this.promoteTo = promoteTo;
        this.score = score;
    }

    public Move(Move m) {
        this.from = m.from;
        this.to = m.to;
        this.promoteTo = m.promoteTo;
        this.score = m.score;
    }

    public void copyFrom(Move m) {
        from      = m.from;
        to        = m.to;
        promoteTo = m.promoteTo;
//        score = m.score;
    }

    /** Note that score is not included in the comparison. */
    @Override
    public boolean equals(Object o) {
        if ((o == null) || (o.getClass() != this.getClass()))
            return false;
        Move other = (Move)o;
        if (from != other.from)
            return false;
        if (to != other.to)
            return false;
        return promoteTo == other.promoteTo;
    }
    @Override
    public int hashCode() {
        return (from * 64 + to) * 16 + promoteTo;
    }

    /** Useful for debugging. */
    @Override
	public final String toString() {
        return TextIO.moveToUCIString(this);
    }
}
