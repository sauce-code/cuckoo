package org.petero.cuckoo.app;
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

import java.io.IOException;

import org.petero.cuckoo.engine.chess.TreeLogger;
import org.petero.cuckoo.gui.AppletGUI;
import org.petero.cuckoo.tui.TUIGame;
import org.petero.cuckoo.uci.UCIProtocol;

/**
 *
 * @author petero
 */
public class Main {

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) throws IOException {
		if ((args.length == 1) && args[0].equals("gui")) {
			AppletGUI.main(args);
		} else if ((args.length == 1) && (args[0].equals("txt") || args[0].equals("tui"))) {
			TUIGame.main(args);
		} else if ((args.length == 2) && args[0].equals("tree")) {
			TreeLogger.main(new String[] { args[1] });
		} else {
			UCIProtocol.main(new String[] { Boolean.toString(false) });
		}
	}

}
