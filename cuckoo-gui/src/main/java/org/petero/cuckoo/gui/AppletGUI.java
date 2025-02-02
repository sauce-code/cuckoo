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

package org.petero.cuckoo.gui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.petero.cuckoo.engine.chess.ComputerPlayer;
import org.petero.cuckoo.engine.chess.Move;
import org.petero.cuckoo.engine.chess.Position;
import org.petero.cuckoo.engine.guibase.ChessController;
import org.petero.cuckoo.engine.guibase.GUIInterface;

/**
 * The main class for the chess GUI.
 * @author petero
 */
public class AppletGUI extends javax.swing.JApplet implements GUIInterface {
    private static final long serialVersionUID = 7357610346389734323L;
    ChessBoardPainter cbp;
    ChessController ctrl;
    static final int TT_LOG_SIZE = 19; // Use 2^19 hash entries.
    String moveListStr = "";
    String thinkingStr = "";

    /** Initializes the applet AppletGUI */
    @Override
    public void init() {
        ctrl = new ChessController(this);
        try {
            java.awt.EventQueue.invokeAndWait(() -> {
			    initComponents();
			    cbp = (ChessBoardPainter) chessBoard;
			    ctrl.newGame(playerWhite.isSelected(), TT_LOG_SIZE, true);
			    ctrl.startGame();
			});
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Entry point for the GUI version of the chess program.
     */
    public static void main(String[] args) {
        javax.swing.JApplet theApplet = new AppletGUI();
        theApplet.init();
        javax.swing.JFrame window = new javax.swing.JFrame(ComputerPlayer.engineName);
        window.setContentPane(theApplet);
        window.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        window.pack();
        window.setVisible(true);
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

        playerColor = new javax.swing.ButtonGroup();
        mainPanel = new javax.swing.JPanel();
        chessBoardPanel = new javax.swing.JPanel();
        chessBoard = new ChessBoardPainter();
        jPanel1 = new javax.swing.JPanel();
        newGame = new javax.swing.JButton();
        settingsPanel = new javax.swing.JPanel();
        playerWhite = new javax.swing.JRadioButton();
        playerBlack = new javax.swing.JRadioButton();
        timeLabel = new javax.swing.JLabel();
        timeSlider = new javax.swing.JSlider();
        showThinking = new javax.swing.JCheckBox();
        flipBoard = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextPane();
        statusLine = new javax.swing.JTextField();
        forward = new javax.swing.JButton();
        backward = new javax.swing.JButton();

        chessBoardPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        chessBoardPanel.setPreferredSize(new java.awt.Dimension(500, 500));

        chessBoard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
			public void mousePressed(java.awt.event.MouseEvent evt) {
                ChessBoardMousePressed(evt);
            }
            @Override
			public void mouseReleased(java.awt.event.MouseEvent evt) {
                ChessBoardMouseReleased(evt);
            }
        });
        chessBoard.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
			public void mouseDragged(java.awt.event.MouseEvent evt) {
                ChessBoardMouseDragged(evt);
            }
        });

        javax.swing.GroupLayout ChessBoardPanelLayout = new javax.swing.GroupLayout(chessBoardPanel);
        chessBoardPanel.setLayout(ChessBoardPanelLayout);
        ChessBoardPanelLayout.setHorizontalGroup(
            ChessBoardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(chessBoard, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );
        ChessBoardPanelLayout.setVerticalGroup(
            ChessBoardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(chessBoard, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
        );

        jPanel1.setFocusable(false);

        newGame.setText("New Game");
        newGame.setFocusable(false);
        newGame.addActionListener(evt -> NewGameActionPerformed(evt));

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));
        settingsPanel.setFocusable(false);

        playerColor.add(playerWhite);
        playerWhite.setSelected(true);
        playerWhite.setText("Play White");
        playerWhite.setFocusable(false);

        playerColor.add(playerBlack);
        playerBlack.setText("Play Black");
        playerBlack.setFocusable(false);

        timeLabel.setText("Thinking Time");

        timeSlider.setMajorTickSpacing(15);
        timeSlider.setMaximum(60);
        timeSlider.setMinorTickSpacing(5);
        timeSlider.setPaintLabels(true);
        timeSlider.setPaintTicks(true);
        timeSlider.setValue(5);
        timeSlider.setFocusable(false);
        timeSlider.addChangeListener(evt -> TimeSliderStateChanged(evt));

        showThinking.setText("Show Thinking");
        showThinking.setFocusable(false);
        showThinking.addChangeListener(evt -> ShowThinkingStateChanged(evt));

        flipBoard.setText("Flip Board");
        flipBoard.setFocusable(false);
        flipBoard.addChangeListener(evt -> FlipBoardStateChanged(evt));

        javax.swing.GroupLayout SettingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(SettingsPanelLayout);
        SettingsPanelLayout.setHorizontalGroup(
            SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(showThinking, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(SettingsPanelLayout.createSequentialGroup()
                .addComponent(playerWhite)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 104, Short.MAX_VALUE)
                .addComponent(flipBoard)
                .addContainerGap())
            .addGroup(SettingsPanelLayout.createSequentialGroup()
                .addComponent(timeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(timeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(playerBlack)
        );
        SettingsPanelLayout.setVerticalGroup(
            SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(SettingsPanelLayout.createSequentialGroup()
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(playerWhite)
                    .addComponent(flipBoard))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(playerBlack)
                .addGap(18, 18, 18)
                .addGroup(SettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(timeLabel)
                    .addComponent(timeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showThinking)
                .addContainerGap())
        );

        logTextArea.setEditable(false);
        logTextArea.setVerifyInputWhenFocusTarget(false);
        jScrollPane1.setViewportView(logTextArea);

        statusLine.setEditable(false);
        statusLine.setFocusable(false);

        forward.setText("->");
        forward.setDefaultCapable(false);
        forward.setFocusPainted(false);
        forward.setFocusable(false);
        forward.addActionListener(evt -> ForwardActionPerformed(evt));

        backward.setText("<-");
        backward.setDefaultCapable(false);
        backward.setFocusPainted(false);
        backward.setFocusable(false);
        backward.addActionListener(evt -> BackwardActionPerformed(evt));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                    .addComponent(statusLine, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(newGame)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(backward)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(forward))
                        .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newGame)
                    .addComponent(forward)
                    .addComponent(backward))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLine, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout MainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(MainPanelLayout);
        MainPanelLayout.setHorizontalGroup(
            MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chessBoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        MainPanelLayout.setVerticalGroup(
            MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, MainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(MainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(chessBoardPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void ChessBoardMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ChessBoardMousePressed
        if (ctrl.humansTurn()) {
            int sq = cbp.eventToSquare(evt);
            Move m = cbp.mousePressed(sq);
            if (m != null) {
                ctrl.humanMove(m);
            }
        }
    }//GEN-LAST:event_ChessBoardMousePressed

    private void FlipBoardStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_FlipBoardStateChanged
        cbp.setFlipped(flipBoard.isSelected());
    }//GEN-LAST:event_FlipBoardStateChanged

    private void NewGameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_NewGameActionPerformed
        ctrl.newGame(playerWhite.isSelected(), TT_LOG_SIZE, true);
        ctrl.startGame();
    }//GEN-LAST:event_NewGameActionPerformed

    private void ShowThinkingStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_ShowThinkingStateChanged
        ctrl.setMoveList();
    }//GEN-LAST:event_ShowThinkingStateChanged

    private void BackwardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BackwardActionPerformed
        ctrl.takeBackMove();
    }//GEN-LAST:event_BackwardActionPerformed

    private void ForwardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ForwardActionPerformed
        ctrl.redoMove();
    }//GEN-LAST:event_ForwardActionPerformed

    private void TimeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_TimeSliderStateChanged
        ctrl.setTimeLimit();
    }//GEN-LAST:event_TimeSliderStateChanged

    private void ChessBoardMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ChessBoardMouseDragged
        if (ctrl.humansTurn()) {
            cbp.mouseDragged(evt);
        }
    }//GEN-LAST:event_ChessBoardMouseDragged

    private void ChessBoardMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ChessBoardMouseReleased
        if (ctrl.humansTurn()) {
            int sq = cbp.eventToSquare(evt);
            Move m = cbp.mouseReleased(sq);
            if (m != null) {
                ctrl.humanMove(m);
            }
        }
    }//GEN-LAST:event_ChessBoardMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton backward;
    private javax.swing.JLabel chessBoard;
    private javax.swing.JPanel chessBoardPanel;
    private javax.swing.JCheckBox flipBoard;
    private javax.swing.JButton forward;
    private javax.swing.JTextPane logTextArea;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton newGame;
    private javax.swing.JRadioButton playerBlack;
    private javax.swing.ButtonGroup playerColor;
    private javax.swing.JRadioButton playerWhite;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JCheckBox showThinking;
    private javax.swing.JTextField statusLine;
    private javax.swing.JLabel timeLabel;
    private javax.swing.JSlider timeSlider;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    @Override
	public void setPosition(Position pos) {
        cbp.setPosition(pos);
    }

    @Override
	public void setSelection(int sq) {
        cbp.setSelection(sq);
    }

    @Override
	public void setStatusString(String str) {
        statusLine.setText(str);
    }

    @Override
	public void setMoveListString(String str) {
        moveListStr = str;
        str = moveListStr + "\n" + thinkingStr;
        if (!str.equals(logTextArea.getText())) {
            logTextArea.setText(str);
        }
    }
    
    @Override
	public void setThinkingString(String str) {
        thinkingStr = str;
        str = moveListStr + "\n" + thinkingStr;
        if (!str.equals(logTextArea.getText())) {
            logTextArea.setText(str);
        }
    }
    

    @Override
	public final int timeLimit() {
        return Math.max(25, timeSlider.getValue() * 1000);
    }

    @Override
	public final boolean showThinking() {
        return showThinking.isSelected();
    }

    @Override
	public void requestPromotePiece() {
        runOnUIThread(() -> {
		    Object[] options = { "Queen", "Rook", "Bishop", "Knight" };
		    int choice = JOptionPane.showOptionDialog(
		            cbp, "Promote pawn to?", "Pawn Promotion",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		    ctrl.reportPromotePiece(choice);
		});
    }

    @Override
	public void runOnUIThread(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    @Override
    public boolean randomMode() {
        return false;
    }

    @Override
    public void reportInvalidMove(Move m) {
    }
}
