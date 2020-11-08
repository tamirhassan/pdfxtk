package com.tamirhassan.pdfxtk;

import java.util.Collections;

import com.tamirhassan.pdfxtk.comparators.Y1Comparator;
import com.tamirhassan.pdfxtk.graph.AdjacencyEdge;
import com.tamirhassan.pdfxtk.model.TextBlock;
import com.tamirhassan.pdfxtk.model.TextFragment;
import com.tamirhassan.pdfxtk.model.TextLine;
import com.tamirhassan.pdfxtk.utils.SegmentUtils;
import com.tamirhassan.pdfxtk.utils.Utils;

/**
 * Class to represent heuristic rules for segmentation
 * 
 */
public class SegmentationRules 
{
//	protected boolean matchFontSize = true;
	protected int pass = 1;
	
	// 2 - old (Feb)
	// 3 - after refactor/simplification, before branching (Mar)
	// 4 - new, with branching experiments (Apr)
	protected int version = 4;
	
	protected boolean ignoreRivers = false;
	
	public SegmentationRules()
	{
		// do nothing
	}

	/**
	 * Segmentation consists of several passes through list of edges
	 * @return integer of pass number
	 */
	public int getPass() {
		return pass;
	}

	/**
	 * Segmentation consists of several passes through list of edges
	 * set pass number as integer
	 * @param pass
	 */
	public void setPass(int pass) {
		this.pass = pass;
	}

	/*
	public boolean isMatchFontSize() {
		return matchFontSize;
	}

	public void setMatchFontSize(boolean matchFontSize) {
		this.matchFontSize = matchFontSize;
	}
	*/
	
	// external checks
	
	// is anything "closer" when merging high line spacings
	
	// are any whitespace rivers or alignment stripes
	
	// does the merging overlap other blocks? what if they get swallowed up?
	
	// does the merging create any whitespace rivers that weren't there before?
	
	// is the result plausible? (brickwork, no rivers)
	
	/**
	 * For the pass, whether at each iteration the algorithm should check for whitespace rivers
	 * 
	 * @return
	 */
	public boolean isIgnoreRivers() {
		return ignoreRivers;
	}

	/**
	 * For the pass, whether at each iteration the algorithm should check for whitespace rivers
	 * 
	 * @param ignoreRivers
	 */
	public void setIgnoreRivers(boolean ignoreRivers) {
		this.ignoreRivers = ignoreRivers;
	}

	/**
	 * Checks whether to merge b1 with b2 according to edge e
	 * and performs the merge if necessary
	 * 
	 * @param b1
	 * @param b2
	 * @param e
	 * @return Result as BlockOperation
	 */
	public BlockOperation mergeBlocks
			(TextBlock b1, TextBlock b2, AdjacencyEdge<TextFragment> e)
	{
		// CHECK REJECT DISTANCE
		
		// rejectDistance currently only set for verticals
		// might be replaced by something better
		if (e.isVertical())
		{
			if (e.physicalLength() > (b1.getRejectDistance() * 1.1))
			{
				return new BlockOperation
						(SegmentationOperation.SEG_FAILURE_NO_REVISIT);
			}
			if (e.physicalLength() > (b2.getRejectDistance() * 1.1))
			{
				return new BlockOperation
						(SegmentationOperation.SEG_FAILURE_NO_REVISIT);
			}
		}
		
		// LIMIT EDGE LENGTHS FOR ALL PASSES
		
		if (true)//(pass == 1)
		{
			if (e.isHorizontal())
			{
				// only add superscripts to existing lines, etc.
				if (e.physicalLength() > e.avgFontSize())
				{
					return new BlockOperation
							(SegmentationOperation.SEG_FAILURE_NO_REVISIT);
				}
			}
			else if (e.isVertical())
			{
				// max edge length of 3.0f (2.5* spacing)
				if (e.baselineDistance() > (e.avgFontSize() * 3.0f))
				{
					return new BlockOperation
							(SegmentationOperation.SEG_FAILURE_NO_REVISIT);
				}
			}
		}
		
		// no font size check for horizontal - only superscript etc.
//		boolean matchFontSize = false;
		boolean matchFontSize = true;
		if (matchFontSize && e.isVertical())
		{
			if (!Utils.withinAvg(b1.getFontSize(), b2.getFontSize(), 0.1f))
				return new BlockOperation(BlockOperation.SEG_FAILURE);
		}
		
		// GENERATE NEW BLOCK AND VERIFY IT
		
		BlockOperation retVal = generateMergedBlock(b1, b2, e, matchFontSize);
		
		return retVal;
	}
	
	/**
	 * Generates a new block by merging b1 and b2 from the ground up
	 * Necessary when merging horizontally to generate new line objects
	 * Verifies the new block and fails if the new block does not pass verification
	 * 
	 * @param b1
	 * @param b2
	 * @param matchFontSize
	 * @return BlockOperation containing new block or failure exit status
	 */
	protected BlockOperation mergeBlockToBlock(TextBlock b1, TextBlock b2,
			boolean matchFontSize)
	{
		TextBlock mergedBlock = new TextBlock(b1); // clones b1
		BlockOperation retVal = new BlockOperation(BlockOperation.SEG_SUCCESS, mergedBlock);
		
		if (b2.getRejectDistance() < b1.getRejectDistance())
			mergedBlock.setRejectDistance(b2.getRejectDistance());
		
		if (b1.isBranchMerged() || b2.isBranchMerged())
			mergedBlock.setBranchMerged(true);

		for (int i = 0; i < b2.getItems().size(); i ++)
		{
			TextLine lnew = b2.getItems().get(i);
			
			int index = -1;
			int matchIndex = -1;
			boolean matchMulti = false;
			TextLine matchedLine = null;
			for (TextLine lold : mergedBlock.getItems())
			{
				index ++;
				
				// check intersection of central 50% (25-75)
				// if intersects more than one line, fail
				
				// either centre intersects either block
				if (SegmentUtils.vertMinIntersect(lnew, lold, 0.5f))
				{
					if (matchedLine != null)
						matchMulti = true;
					matchIndex = index;
					matchedLine = lold;
				}
			}
			
			if (matchMulti)
			{
				return new BlockOperation(BlockOperation.SEG_FAILURE);
			}
			
			if (matchedLine != null)
			{
//					if (matchedLine.addSegment(newFragment, true, matchFontSize)) // adds to middle too
				LineOperation lo = 
						lineMerge(matchedLine, lnew, true, matchFontSize);
				
				if (lo.getExitStatus() == SegmentationOperation.SEG_SUCCESS)
				{
					// replace old line object
					mergedBlock.getItems().set(matchIndex, lo.getResult());
					mergedBlock.growBoundingBox(lnew);
				}
				else
				{
					// overlaps but could not be added
					return new BlockOperation(BlockOperation.SEG_FAILURE);
				}
			}
			else
			{
				// no match, just add above or below
				mergedBlock.getItems().add(lnew);
				mergedBlock.growBoundingBox(lnew);
				
				
			}
			// sort in all cases when edge horizontal or unspecified
			Collections.sort(mergedBlock.getItems(), new Y1Comparator());
			
//			mergedBlock.setFirstChild(new CandidateBlock(b1));
//			mergedBlock.setSecondChild(new CandidateBlock(b2));
		}
//		return retVal;
		return new BlockOperation(verifyBlock(mergedBlock), mergedBlock);
	}
	
	/**
	 * Tries to merge b1 and b2 according to edge e
	 * Runs fast method if edge is vertical; full method if edge is horizontal
	 * 
	 * @param b1
	 * @param b2
	 * @param e
	 * @param matchFontSize - true if font sizes should be the same (within some tolerance)
	 *        matchFontSize is false for some segmentation passes, which deal with superscript, etc.
	 * @return
	 */
	protected BlockOperation generateMergedBlock(TextBlock b1, TextBlock b2,
			AdjacencyEdge<TextFragment> e, boolean matchFontSize)
	{
		// skip unnecessary steps if edge is vertical
		if (b1.getItems().size() > 1 && b2.getItems().size() > 1 &&
				e != null && e.isVertical() &&
				!SegmentUtils.vertIntersect(b1, b2))
		{
			TextBlock mergedBlock = new TextBlock(b1); // clones b1
			BlockOperation retVal = new BlockOperation(BlockOperation.SEG_SUCCESS, mergedBlock);
			
			if (b2.getRejectDistance() < b1.getRejectDistance())
				mergedBlock.setRejectDistance(b2.getRejectDistance());
			
			if (b1.isBranchMerged() || b2.isBranchMerged())
				mergedBlock.setBranchMerged(true);
			
			mergedBlock.getItems().addAll(b2.getItems());
			mergedBlock.growBoundingBox(b2);
			
//			mergedBlock.setFirstChild(new CandidateBlock(b1));
//			mergedBlock.setSecondChild(new CandidateBlock(b2));
			
			if (e.getDirection() == AdjacencyEdge.REL_ABOVE)
			{
				Collections.sort(mergedBlock.getItems(), new Y1Comparator());
			}
			else
			{
				// edge direction REL_BELOW => no need to sort
			}
			
			return new BlockOperation(verifyBlock(mergedBlock), mergedBlock);
		}
		else
		{
			// run full method, generating new line objects
			
			return mergeBlockToBlock(b1, b2, matchFontSize);
		}
	}
	
	/**
	 * Verifies block. by running checks 
	 * Currently includes initial checks for the first pass
	 * 
	 * @param b
	 * @return
	 */
	protected int verifyBlock(TextBlock b)
	{
		// currently verifies:
		//   overlapping lines
		//   line spacing
		
		if (b.evalOverlappingLines() == false) 
			return SegmentationOperation.SEG_FAILURE;
		
		if (b.evalLineSpacing() < 0.50) 
			return SegmentationOperation.SEG_FAILURE;
		
		if (!ignoreRivers && b.evalWhitespace() < 0.50) 
			return SegmentationOperation.SEG_FAILURE_RIVER;
		
		// TODO: alignment, etc.
		
		// fontsize if in phase 2
		
		return SegmentationOperation.SEG_SUCCESS;
	}
	
	/**
	 * Merges lines of text l1 and l2
	 * 
	 * @param l1
	 * @param l2
	 * @param middle - if lines should also be merged if one is in the middle of the other
	 * @param fontsizeMatch - true if font sizes should match
	 * @return LineOperation containing result or failure exit status
	 */
	protected LineOperation lineMerge(TextLine l1, TextLine l2, 
			boolean middle, boolean fontsizeMatch)
	{
		// TODO: maybe undefined fontsize e.g. if there are only 2 items with different sizes???
    	// (although good edge ordering should avoid this!)
    	
    	float score = 1.0f;
    	boolean addToRight = true;
    	
    	// check vertical overlap
		// TODO: verify that this works; better gaussian tailoff
		if (!SegmentUtils.vertCentresIntersect(l1, l2))
			score -= 0.6f;
    	
		// check if baseline/fontsize match (sameLine method)
		if (fontsizeMatch && !Utils.sameFontSize(l1, l2)) score -= 0.2f;
		if (!Utils.within(l1.getY1(), l2.getY1(), 0.1f)) score -= 0.2f;
		// TODO: gaussian tailoffs
		
		// if not, check if super, subscript or other "accidental"
		// TODO!
		
    	// accidental leads to a demerit
    	
		// check if should be added to left or right
		// maximum allowed overlap = 0.25 * lineheight
		
		float tolerance = Utils.avg(l1.getHeight(), l2.getHeight()) * 0.25f;
		float distance = l2.getX1() - l1.getX2();
		
		// assume adding to right
		if (l2.getX2() <= l1.getX1() + tolerance)
		{
			// adding to left
			addToRight = false;
			distance = l1.getX1() - l2.getX2();
		}
		else if (l2.getX1() < l1.getX2() - tolerance)
		{
			// too much overlap; add to middle or exit!
			if (middle)
			{
				// currently just add and sort by x coordinate
				// don't check for overlapping/overprinting chars
				// as there might be legitimate reasons to do this

				// do not penalize retVal
				
				// avoid distance penalty
				distance = 0;
			}
			else
			{
				score -= 0.6f;
			}
		}
		
    	// also check distance - demerit based on gaussian function
		// can be negative, but only up to 0.25 * lineheight
		float relDistance = distance / Utils.avg(l1.getHeight(), l2.getHeight());
		
		
		
		// floor at 0.5
		float gaussMultiplicator = 0.5f + (0.5f * Utils.normgaussian(relDistance, 0, 2));
		score *= gaussMultiplicator;
    	
		
//		if (score > 0.5)
		if (true)
		{
			TextLine newLine = new TextLine(l1);
			
			if (addToRight)
				newLine.getItems().addAll(l2.getItems());
			else
				newLine.getItems().addAll(0, l2.getItems());
			
			// TODO: desired for super/subscript?
			newLine.growBoundingBox(l2);
			
			// TODO: deal with font size!
			newLine.findText();
			newLine.findFontName();
			newLine.findFontSize(); // currently mean, change to mode
			
			return new LineOperation(SegmentationOperation.SEG_SUCCESS, newLine);
		}
		else
		{
			return new LineOperation(SegmentationOperation.SEG_FAILURE);
		}
	}
}
