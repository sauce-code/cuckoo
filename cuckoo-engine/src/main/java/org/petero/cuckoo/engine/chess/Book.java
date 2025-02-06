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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.security.SecureRandom;
import java.util.*;

/**
 * Implements an opening book.
 * @author petero
 */
public class Book {
    public static class BookEntry {
        final Move move;
        int count;
        BookEntry(Move move) {
            this.move = move;
            count = 1;
        }
    }
    private static Map<Long, List<BookEntry>> bookMap;
    private static Random rndGen;
    private static int numBookMoves = -1;
    private final boolean verbose;

    public Book(boolean verbose) {
        this.verbose = verbose;
    }

    private void initBook() {
        if (numBookMoves >= 0)
            return;
        long t0 = System.currentTimeMillis();
        bookMap = new HashMap<>();
        rndGen = new SecureRandom();
        rndGen.setSeed(System.currentTimeMillis());
        numBookMoves = 0;
        try {
            InputStream inStream = getClass().getResourceAsStream("/book.bin");
            List<Byte> buf = new ArrayList<>(8192);
            byte[] tmpBuf = new byte[1024];
            while (true) {
                assert inStream != null;
                int len = inStream.read(tmpBuf);
                if (len <= 0) break;
                for (int i = 0; i < len; i++)
                    buf.add(tmpBuf[i]);
            }
            inStream.close();
            Position startPos = TextIO.readFEN(TextIO.START_POS_FEN);
            Position pos = new Position(startPos);
            UndoInfo ui = new UndoInfo();
            int len = buf.size();
            for (int i = 0; i < len; i += 2) {
                int b0 = buf.get(i); if (b0 < 0) b0 += 256;
                int b1 = buf.get(i+1); if (b1 < 0) b1 += 256;
                int move = (b0 << 8) + b1;
                if (move == 0) {
                    pos = new Position(startPos);
                } else {
                    boolean bad = ((move >> 15) & 1) != 0;
                    int prom = (move >> 12) & 7;
                    Move m = new Move(move & 63, (move >> 6) & 63,
                                      promToPiece(prom, pos.whiteMove));
                    if (!bad)
                        addToBook(pos, m);
                    pos.makeMove(m, ui);
                }
            }
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        } catch (IOException ex) {
            System.out.println("Can't read opening book resource");
            throw new RuntimeException();
        }
        if (verbose) {
            long t1 = System.currentTimeMillis();
            System.out.printf("Book moves:%d (parse time:%.3f)%n", numBookMoves,
                    (t1 - t0) / 1000.0);
        }
    }

    /** Add a move to a position in the opening book. */
    private void addToBook(Position pos, Move moveToAdd) {
        List<BookEntry> ent = bookMap.computeIfAbsent(pos.zobristHash(), k -> new ArrayList<>());
        for (BookEntry be : ent) {
            if (be.move.equals(moveToAdd)) {
                be.count++;
                return;
            }
        }
        BookEntry be = new BookEntry(moveToAdd);
        ent.add(be);
        numBookMoves++;
    }

    /** Return a random book move for a position, or null if out of book. */
    public final Move getBookMove(Position pos) {
        initBook();
        List<BookEntry> bookMoves = bookMap.get(pos.zobristHash());
        if (bookMoves == null) {
            return null;
        }
        
        MoveGen.MoveList legalMoves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, legalMoves);
        int sum = 0;
        for (BookEntry be : bookMoves) {
            boolean contains = false;
            for (int mi = 0; mi < legalMoves.size; mi++)
                if (legalMoves.m[mi].equals(be.move)) {
                    contains = true;
                    break;
                }
            if (!contains) {
                // If an illegal move was found, it means there was a hash collision.
                return null;
            }
            sum += getWeight(be.count);
        }
        if (sum <= 0) {
            return null;
        }
        int rnd = rndGen.nextInt(sum);
        sum = 0;
        for (BookEntry bookMove : bookMoves) {
            sum += getWeight(bookMove.count);
            if (rnd < sum) {
                return bookMove.move;
            }
        }
        // Should never get here
        throw new RuntimeException();
    }

    private int getWeight(int count) {
        double tmp = Math.sqrt(count);
        return (int)(tmp * Math.sqrt(tmp) * 100 + 1);
    }

    /** Return a string describing all book moves. */
    public final String getAllBookMoves(Position pos) {
        initBook();
        StringBuilder ret = new StringBuilder();
        List<BookEntry> bookMoves = bookMap.get(pos.zobristHash());
        if (bookMoves != null) {
            for (BookEntry be : bookMoves) {
                String moveStr = TextIO.moveToString(pos, be.move, false);
                ret.append(moveStr);
                ret.append("(");
                ret.append(be.count);
                ret.append(") ");
            }
        }
        return ret.toString();
    }

    /** Creates the book.bin file. */
    public static void main(String[] args) {
        List<Byte> binBook = createBinBook();
    try (FileOutputStream out = new FileOutputStream("../src/book.bin")) {
      int bookLen = binBook.size();
      byte[] binBookA = new byte[bookLen];
      for (int i = 0; i < bookLen; i++) binBookA[i] = binBook.get(i);
      out.write(binBookA);
    } catch (IOException e) {
        e.printStackTrace();
    }
  }

  public static List<Byte> createBinBook() {
        List<Byte> binBook = new ArrayList<>(0);
        try {
            InputStream inStream = Object.class.getResourceAsStream("/book.txt");
            assert inStream != null;
            InputStreamReader inFile = new InputStreamReader(inStream);
            BufferedReader inBuf = new BufferedReader(inFile);
            LineNumberReader lnr = new LineNumberReader(inBuf);
            String line;
            while ((line = lnr.readLine()) != null) {
            if (line.startsWith("#") || (line.isEmpty())) {
                    continue;
                }
                if (!addBookLine(line, binBook)) {
                    System.out.printf("Book parse error, line:%d\n", lnr.getLineNumber());
                    throw new RuntimeException();
                }
            }
            lnr.close();
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        } catch (IOException ex) {
            System.out.println("Can't read opening book resource");
            throw new RuntimeException();
        }
        return binBook;
    }

    /** Add a sequence of moves, starting from the initial position, to the binary opening book. */
    private static boolean addBookLine(String line, List<Byte> binBook) throws ChessParseError {
        Position pos = TextIO.readFEN(TextIO.START_POS_FEN);
        UndoInfo ui = new UndoInfo();
        String[] strMoves = line.split(" ");
        for (String strMove : strMoves) {
            int bad = 0;
            if (strMove.endsWith("?")) {
                strMove = strMove.substring(0, strMove.length() - 1);
                bad = 1;
            }
            Optional<Move> m = TextIO.stringToMove(pos, strMove);
            if (m.isEmpty()) {
                return false;
            }
            int prom = pieceToProm(m.get().promoteTo);
            int val = m.get().from + (m.get().to << 6) + (prom << 12) + (bad << 15);
            binBook.add((byte)(val >> 8));
            binBook.add((byte)(val & 255));
            pos.makeMove(m.get(), ui);
        }
        binBook.add((byte)0);
        binBook.add((byte)0);
        return true;
    }

    private static int pieceToProm(int p) {
        return switch (p) {
            case Piece.WQUEEN, Piece.BQUEEN -> 1;
            case Piece.WROOK, Piece.BROOK -> 2;
            case Piece.WBISHOP, Piece.BBISHOP -> 3;
            case Piece.WKNIGHT, Piece.BKNIGHT -> 4;
            default -> 0;
        };
    }
    
    private static int promToPiece(int prom, boolean whiteMove) {
        return switch (prom) {
            case 1 -> whiteMove ? Piece.WQUEEN : Piece.BQUEEN;
            case 2 -> whiteMove ? Piece.WROOK : Piece.BROOK;
            case 3 -> whiteMove ? Piece.WBISHOP : Piece.BBISHOP;
            case 4 -> whiteMove ? Piece.WKNIGHT : Piece.BKNIGHT;
            default -> Piece.EMPTY;
        };
    }
}
