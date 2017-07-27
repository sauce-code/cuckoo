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

package org.petero.cuckoo.tui;

import java.io.IOException;

import org.petero.cuckoo.engine.chess.ComputerPlayer;
import org.petero.cuckoo.engine.chess.HumanPlayer;
import org.petero.cuckoo.engine.chess.Player;
import org.petero.cuckoo.engine.chess.TreeLogger;
import org.petero.cuckoo.uci.UCIProtocol;

/**
 *
 * @author petero
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
    	if ((args.length == 0)) {
            Player whitePlayer = new HumanPlayer();
            ComputerPlayer blackPlayer = new ComputerPlayer();
            blackPlayer.setTTLogSize(21);
            TUIGame game = new TUIGame(whitePlayer, blackPlayer);
            game.play();
        } else if ((args.length == 1) && args[0].equals("tree")) {
            TreeLogger.main(new String[]{args[1]});
        } else {
            UCIProtocol.main(false);
        }
    }
}
