# Changelog

## 2017-07-30: Version 1.12 (Maven Port)
- Reuploaded the project on GitHub as an Eclipse Maven Project.
- Applied cleanup.
- Set required Java version to 1.8.

## 2011-10-24: Version 1.12 (ELO: m=61 s=1.6)
- Don't use aspiration window when mate score has been found.
- Optimized the legal move generator. This has minimal effect on playing strength, but improves perft speed a lot.
- Added bishop back rank penalty.
- Don't LMR reduce captures more than one ply.
- Avoid array index out of bounds exception if the engine is asked to search a position where there are no legal moves.
- Optimized piece values using CLOP.
- Store razoring results in the transposition table.
- Use SEE sign and MVV/LVA to order moves in non-q-search nodes.
- Try equal captures before non-captures in non-q-search nodes.
- Update killer moves when a transposition table hit gives a beta cutoff.
- Implemented signSEE function that can be used when only the sign of SEE is needed.
- Ignore squares attacked by enemy pawns when computing mobility.
- Added evaluation of KRKP end games, based on statistics gathered from EGTB information.
- Skip null-move search if the static score is below beta.
- Update hash entry "generation" value after a transposition table hit.
- Optimized bishop pair bonus using CLOP.
- Optimized futility margins using CLOP.

## 2011-06-12: Version 1.11 (ELO: m=47 s=4.8)
- Do extended futility pruning also on depth 4.
- Implemented razoring.
- Added support for fractional ply extensions.
- Use MVV/LVA ordering (but SEE pruning) in the q-search.
- Some speed optimizations.
- Use IID also at non-PV nodes.
- Made aspiration window a bit smaller.
- More aggressive futility pruning.
- Increase beta in two steps when a root move fails high.
- More accurate evaluation of end games with insufficient mating material.

## 2011-04-23: Version 1.10 (ELO: m=13 s=2.9)
- More aggressive futility pruning.
- Increased passed pawn bonus a little.
- Added small penalty for non-developed bishops.
- Implemented adjustable playing strength.

## 2011-03-16: Version 1.09
- Speeded up the SEE function by using bitboard techniques.
- Use a lookup table to speed up castle bonus calculations.
- Implemented specialized move generator function for check evasions.
- Some speed optimizations.
- Implemented specialized make/unmake move functions used by SEE.
- Allow more time to resolve fail lows at the root.
- Bug fix. Reset en passant square when making a null move.
- Don't allow too much time to resolve a fail low if too close to the next time control.
- Stop searching immediately after a ponder hit if only one legal move.
- Store successful null-move prunings in the transposition table.
- Give extra bonus to passed pawns if enemy king is outside "the square".
- Only try two hash slots when inserting in the transposition table.
- Try harder to keep PV nodes in the transposition table.
- Use a hash table to speed up king safety evaluation.
- Adjusted the knight piece square tables.
- Added code to dump the search tree to a binary file, and code to browse and search the dump file.

## 2011-01-30: Version 1.08
- Modified the bishop piece square table.
- Added penalty for bishop trapped behind pawn on a2/a7/h2/h7.
- Added isolated pawn penalty.
- Fixed race condition in UCI mode with quick ponder misses.
- Added backward pawn penalty.
- Modified the history heuristic calculations.

## 2011-01-18: Version 1.07
- Speed optimizations.
- Only apply the "drawish factor" for opposite colored bishops if the non-pawn material is balanced.
- Added evaluation of KQKP end games.
- Added evaluation term for two rooks on the 7th rank.
- Implemented delta pruning in the q-search.
- Improved reduction logic. Don't allow reduction of hash moves and killer moves, but allow larger reductions for other moves.
- Fixed bug related to stalemate traps and the 50-move rule.
- Allow null moves and LMR also in PV nodes.
- Don't do futility pruning and LMR for passed pawn pushes to the 6th and 7th ranks.
- Added bonus in end games for having king close to opponents passed pawn promotions squares.
- Don't allow null moves when side to move doesn't have pawns. This fixes problems in KQKR end games.
- Extend the search one ply when going into a pawn end game.
- Reduced queen mobility bonus.
- Increased the passed pawn bonus and made it non-linear.
- King safety adjustment.
- Added a threat bonus evaluation term.
- Penalize unmoved d/e pawns.
- Undo LMR if the threat move slides through the LMR move "from square". Idea from Stockfish.
- Ponder even if there is only one valid move.
- Allow LMR also at the root.

## 2010-12-31: Version 1.06
- Speed optimizations.
- Implemented non-linear mobility scores.
- Fixed bad interaction of futility pruning/hashing/fail soft.
- Tweaked rook piece square table a little.
- Order root moves according to search tree size on previous ply.
- Don't trust mate scores from null move searches.
- Increased the bishop pair bonus.
- Tweaked king safety a bit.
- Removed mate threat extension.
- Only extend recaptures if the recapture is reasonable according to SEE.
- Use a built-in bitbase for KPK evaluation.
- Reduced the aspiration window to 25 centipawns.
- Prune checking moves with negative SEE score in q-search.
- Allow reductions and pruning of losing (according to SEE) captures.
- Added end game evaluation of rook pawn and wrong-colored bishop.
- Added penalty for rook trapped behind non-castled king.
- Don't allow double null moves.
- Allow null move search to go directly into q-search.
- More depth reduction in IID at large depths.
- Implemented null move threat detection, to be able to undo unsafe LMR reductions. Idea from Stockfish.
- Don't overwrite a transposition table hash entry with inferior information for the same position. Could happen in IID.
- Added evaluation of unstoppable passed pawns in pawn end games.
- Include attacked squares close to the king in king safety evaluation.
- Implemented magic bitboard move generator.
- Use bitboard techniques for evaluation.
- Created specialized move generator functions to generate checks.
- Correctly handle pseudo-legal but not legal en passant squares.

## 2010-07-04: Version 1.05
- Speed optimizations.
- Avoid dynamic memory allocations during search.
- Don't do mate threat extensions too deep down in the search tree. Fixes search tree explosion in WAC 21.
- Incremental update of material scores.
- Created specialized move generator functions to generate captures.
- Created a function givesCheck that decides if making a move gives check to the opponent.
- Implemented a pawn hash table.
- Store static evaluation scores in the transposition table.
- If there is only one legal move at the root, make that move immediately.

## 2010-06-12: Version 1.04
