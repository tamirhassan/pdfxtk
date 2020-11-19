package com.tamirhassan.pdfxtk.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tamirhassan.pdfxtk.utils.SegmentUtils;
import com.tamirhassan.pdfxtk.utils.Utils;

/**
 * Methods for building and evaluating text blocks for
 * initial segmentation using the AdjacencyGraph
 * 
 * @author tam
 *
 */
public class TextBlock extends CompositeSegment<TextLine> //implements ResultSegment
	implements IXHTMLSegment
{
	protected float rejectDistance = Float.MAX_VALUE;
	protected boolean branchMerged = false;
	protected boolean branchSplit = false; // add edge or distance?
	
//	Other ideas: firstChild/secondChild (hierarchical)
//	             lastGoodSegmentation
//	Merge history allows linear search
	
//	List<List<CandidateBlock>> mergeHistory = new ArrayList<List<CandidateBlock>>();
	
	final public static int ALIGN_UNSET = -1;
	final public static int ALIGN_LEFT = 0;
	final public static int ALIGN_CENTRE = 1;
	//final public static int ALIGN_CENTRE_KNUTH = 11;
	final public static int ALIGN_JUSTIFY = 2;
	final public static int ALIGN_RIGHT = 3;
	final public static int ALIGN_FORCE_JUSTIFY = 4;
	
	int alignment = -1;
	
	final public static int LBL_UNSET = -1;
	final public static int LBL_PARA = 10;
	final public static int LBL_CELL = 20;
	final public static int LBL_TD = 21;
	final public static int LBL_TH = 22;
	final public static int LBL_CELLS = 25;
	final public static int LBL_LIST = 30;
	final public static int LBL_LIST_LABEL = 31;
	final public static int LBL_LIST_TEXT = 32;
	
//	int label = -1;
	
	float evalPara = -1.0f;
	float evalCell = -1.0f;
	float evalBullet = -1.0f;
	
	float lineSpacing = -1.0f;
	
	int colSpan; // TODO: move elsewhere perhaps
	
	
	public TextBlock()
    {
        super();
    }
	
	/**
	 * Creates candidate block with single line item
	 * and sets its calculated fields
	 * 
	 * @param firstSegment
	 */
	public TextBlock(TextFragment firstSegment)
    {
    	super();
    	
    	this.items.add(new TextLine(firstSegment));
    	this.setCalculatedFields(firstSegment);
    }
	
	/*
	public boolean addSegmentHorizontally(TextFragment newSegment)
	{
		// try adding as superscript to existing line
		
		// 
		
		return false;
	}
	*/
	
	/**
	 * creates a copy with new line objects pointing
	 * to the same text fragment objects
	 * 
	 * @return
	 */
	public TextBlock(TextBlock blockToCopy)
	{
		super();
		
		for (TextLine tl : blockToCopy.items)
			this.items.add(new TextLine(tl));
		this.setCalculatedFields(blockToCopy);
		this.rejectDistance = blockToCopy.rejectDistance;
		this.branchMerged = blockToCopy.branchMerged;
		this.branchSplit = blockToCopy.branchSplit;
		
		this.alignment = blockToCopy.alignment;
//		this.label = blockToCopy.label;
		
		this.evalPara = blockToCopy.evalPara;
		this.evalCell = blockToCopy.evalCell;
		this.evalBullet = blockToCopy.evalBullet;
	}
	
	
	// inherited: List<RTTextLine> items;
	// when adding, this list should be kept 
	// sorted in descending Y order (top to bottom)
	
	public float getRejectDistance() {
		return rejectDistance;
	}

	public void setRejectDistance(float rejectDistance) {
		this.rejectDistance = rejectDistance;
	}

	public boolean isBranchMerged() {
		return branchMerged;
	}

	public void setBranchMerged(boolean branchMerged) {
		this.branchMerged = branchMerged;
	}

	public boolean isBranchSplit() {
		return branchSplit;
	}

	public void setBranchSplit(boolean branchSplit) {
		this.branchSplit = branchSplit;
	}

	public int getAlignment() {
		return alignment;
	}

	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}

	public float getLineSpacing() {
		return lineSpacing;
	}

	public void setLineSpacing(float lineSpacing) {
		this.lineSpacing = lineSpacing;
	}

	/*
	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}
	 */
	
	public float getEvalPara() {
		return evalPara;
	}

	public void setEvalPara(float evalPara) {
		this.evalPara = evalPara;
	}

	public float getEvalCell() {
		return evalCell;
	}

	public void setEvalCell(float evalCell) {
		this.evalCell = evalCell;
	}

	public float getEvalBullet() {
		return evalBullet;
	}

	public void setEvalBullet(float evalBullet) {
		this.evalBullet = evalBullet;
	}

	public int getColSpan() {
		return colSpan;
	}

	public void setColSpan(int colSpan) {
		this.colSpan = colSpan;
	}

	/**
	 * Generates a list of text fragments by recursing the line objects
	 */
	public List<TextFragment> fragments()
	{
		List<TextFragment> retVal = new ArrayList<TextFragment>();
		
		for(TextLine l : items)
			retVal.addAll(l.getItems());
		
		return retVal;
	}
	
	/**
	 * Returns the line containing the fragment
	 */
	public TextLine getLineContainingFragment(TextFragment tf)
	{
		for (TextLine l : items)
		{
			if (l.getItems().contains(tf))
				return l;
		}
		return null;
	}
	
	/**
	 * sets average line spacing (baseline distance) in points
	 * (not as multiple of fontsize)
	 * 
	 * pre: lines must be sorted from top to bottom
	 * 
	 */
	public void findLineSpacing()
	{
		if (items.size() >= 2)
		{
			float lineSum = 0.0f;
			TextLine prevLine = null;
			for (TextLine l : items)
			{
				if (prevLine != null)
				{
					lineSum += (prevLine.getY1() - l.getY1());
				}
				prevLine = l;
			}
			lineSpacing = lineSum / (float)(items.size() - 1);
		}
		else
		{
			lineSpacing = -1.0f;
		}
	}
	
	// demerits a la Knuth(-Plass)
	//
	// penalties: 0 - irrelevant; +1000 prohibited -1000 compulsory
	//
	// adjustment ratio: +ve until 1 (max stretchability) or -ve until -1
	//   i.e. relative space with 1 = loose -1 = tight; >1 too loose; <1 too tight
	// 
	// badness: 100 * (|adj ratio|)^3
	//
	// demerits: (1 + badness + penalty)^2 + additional line penalty
	//   (always positive, from 1 to the thousands)
	//
	// formula quite arbitrary
	//
	// adding 1 to the badness instead of using badness by itself will
	// minimize the total number of lines in cases where there are breaks
	// with approximately zero badness
	
	// TODO: calculated fields
	// likelihood of left alignment
	// likelihood of centre "
	// "             justified "
	// "             force justified "
	//
	// if indentations present,
	//   likelihood of 1st line indentation
	//   likelihood fo several paragraphs with indentations
	
	
	
	
	
	// regular paragraph (i.e. without forced line breaks)
	// funny shape or fit, but syntactically correct
	// possibilities for left, centre and right alignment (max deviation gaussian evaluation)
	// constant line spacing (max deviation gaussian evaluation)
	
	// ORDER
	// regular paras first
	// then add superscripts
	// non-aligned paragraphs if not preferred by aligned
	// L-shaped and funny shaped if not preferred by something else
	
	
	/**
	 * @return Likelihood that the candidate block is a bullet
	 */
	public float evalAsBullet()
	{
		if (items.size() > 1) return 0.0f;
		
		float val = Utils.normgaussian(text.length(), 1.0f, 1.0f);
		
		return (val * 0.9f) + 0.1f;
	}
	
	/**
	 * @return Likelihood that the candidate block is a paragraph
	 */
	public float evalAsParagraph()
	{
		float relWidth = getWidth() / fontSize;
		
		float val = Utils.normgaussian(relWidth, 28.0f, 12.0f);
		
//		return (val * 0.75f) + 0.25f;
		return (val * 0.5f) + 0.5f;
		
		/*
		if (true)
//		if (val < 28.0f)
		{
			return (val * 0.75f) + 0.25f;
		}
		else
		{
			// ensure higher score than cell for unusually long lines
			return (val * 0.5f) + 0.5f;
		}
		*/
	}
	
	/**
	 * @return Likelihood that the candidate block is a cell
	 */
	public float evalAsCell()
	{
		float relWidth = getWidth() / fontSize;
		
//		float val = Utils.normgaussian(relWidth, 4.0f, 2.0f);
		float val = Utils.normgaussian(relWidth, 4.0f, 4.0f);
				
//		return (val * 0.5f) + 0.5f;
		return (val * 0.6f) + 0.4f;
	}
	
	/**
	 * @return Likelihood that the candidate block is a table column
	 */
	public float evalAsCol()
	{
		float relWidth = getWidth() / fontSize;
		
//		float val = Utils.normgaussian(relWidth, 4.0f, 2.0f);
		float val = Utils.normgaussian(relWidth, 4.0f, 4.0f);
		
		// prefer longer columns
		float recipItems = 1.0f / items.size();
		float multiplicator = 1.0f - (recipItems * 0.5f);
		
		// TODO: look at alignment
		
//		return (val * 0.5f) + 0.5f;
//		return (val * 0.6f) + 0.4f;
		return (val * multiplicator * 0.6f) + 0.4f;
	}
	
	/**
	 * @return Likelihood that the candidate block is segmented correctly
	 * Uses a heuristic that calls the other eval methods
	 */
	public float evalBlock()
	{
		float retVal = evalAlignment() * evalLineSpacing() * evalNumLines() * evalWhitespace();
		
		if (!evalOverlappingLines()) retVal = retVal * 0.25f;
		
		return retVal;
	}
	
	
	/**
	 * prefers larger numbers of lines
	 * (asymptotic function)
	 * 
	 * @return
	 */
	public float evalNumLines()
	{
		if (this.items.size() < 1) return 0.0f;
		
		/*
		float numLines = this.items.size();
		
		float retVal = 1.0f - (0.50f * (1.0f / numLines));
		
		// for     0.25
		// returns 0.75        for 1 line
		//         0.875       for 2 lines
		//         0.91666 ... for 3 lines
		//         etc
		
		return retVal;
		*/
		
		else if (this.items.size() == 1) return 0.99f;
		else return 1.0f;
	}
	
	
	/**
	 * returns true if no overlapping lines found (OK)
	 * otherwise false
	 * pre: lines correctly found and sorted in vertical order
	 * 
	 * @return
	 */
	public boolean evalOverlappingLines()
	{
		// check that no line vertically overlaps the centre of another
		for (TextLine l1 : items)
		{
			for (TextLine l2 : items)
			{
				if (l1 != l2)
				{
					if (SegmentUtils.vertCentresIntersect(l1, l2))
						return false;
				}
			}
		}
		
		// check that all sequential pairs of lines intersect one another horizontally
		TextLine prevLine = null;
		for (TextLine thisLine : items)
		{
			if (prevLine != null)
			{
				if (!SegmentUtils.horizIntersect(thisLine, prevLine))
					return false;
			}
			prevLine = thisLine;
		}
		
		return true;
	}
	
	/**
	 * Returns a value corresponding to the evenness of the line spacing
	 * higher = better/more even
	 * max is 1
	 * 
	 * @return
	 */
	public float evalLineSpacing()
	{
		if (items.size() < 2)
			return 1;
	
		// 2019-03-20 changed from distance to als
		// to min/max calculations, so they don't cancel each other out
		
		// calculate existing line spacing average
    	float als = calcAvgLineSpacing();
		
		// assume sorted vertically
//		Collections.sort(blockFrom.getItems(), new YComparator());
		
//    	float maxDeviation = 0.0f;
		float maxLS = Float.MIN_VALUE;
		float minLS = Float.MAX_VALUE;
		TextSegment prevSeg = null;
		for (TextLine thisSeg : items)
		{
			if (prevSeg == null)
			{
				// do nothing
			}
			else
			{
				float lineSpacing = prevSeg.getY1() - thisSeg.getY1();
				/*
				float deviation = lineSpacing - als;
				if (deviation < 0) deviation = 0.0f - deviation;
				
				if (deviation > maxDeviation)
					maxDeviation = deviation;
				*/
				if (lineSpacing > maxLS) maxLS = lineSpacing;
				if (lineSpacing < minLS) minLS = lineSpacing;
			}
			prevSeg = thisSeg;
		}
		
		// OLD: calculate related deviation to fontsize
		// float relMaxDiff = maxDeviation / calcAvgFontSize();
		
		// calculate related deviation to average
//		float relMaxDiff = maxDeviation / als;
		float deviation = maxLS - minLS;
		float relMaxDiff = deviation / als;
		
		return Utils.normgaussian(relMaxDiff, 0, 0.20f);
	}

	
	public float evalWordSpacing()
	{
		// returns minimum value for all lines
		
		if (items.size() == 0)
			return -1.0f;
    	
		float minVal = Float.MAX_VALUE;
		for (TextLine l : items)
		{
			float val = l.evalSpacing();
			if (val < minVal)
				minVal = val;
		}
		
    	return minVal;
	}
	
	/*
	public float evalBlock()
	{
		return evalAlignment() * evalWhitespace();
	}
	*/
	
	/**
	 * 
	 * returns the max alignment score of left/right/cen/jus
	 * multiplied by wordspacing eval
	 * 
	 * and sets alignment variable
	 *  
	 * @return
	 */
	public float evalAlignment()
	{
		// return the max alignment score of left/right/cen/jus
		// multiplied by wordspacing eval
		
		float evalLeft = evalLeftAlign();
		float evalCentre = evalCentreAlign();
		float evalRight = evalRightAlign();
		float evalJustify = evalJustify();
		
		float evalWordSpacing = evalWordSpacing();
		
		// for justify ignore wordspacing eval
		
		float retVal = 0.0f;
		
		if (evalLeft * evalWordSpacing > retVal)
		{
			retVal = evalLeft * evalWordSpacing;
			alignment = ALIGN_LEFT;
		}
		
		if (evalJustify > retVal)
		{
			retVal = evalJustify;
			alignment = ALIGN_JUSTIFY;
		}
		
		if (evalCentre * evalWordSpacing > retVal)
		{
			retVal = evalCentre * evalWordSpacing;
			alignment = ALIGN_CENTRE;
		}
		
		if (evalRight * evalWordSpacing > retVal)
		{
			retVal = evalRight * evalWordSpacing;
			alignment = ALIGN_RIGHT;
		}
		
		return retVal;
	}
	
	public float evalWhitespace()
	{
		return evalWhitespace(0.5f);
	}
	
	/**
	 * returns false if whitespace gap found with
	 * gapWidth on two consecutive lines
	 * or 1/2 gapWidth on more than two consecutive lines
	 * 
	 * @param gapWidth
	 * @return
	 */
	public float evalWhitespace(float gapWidth)
	{
		float retVal = 1.0f;
		
		List<WhitespaceGap> finalGaps = new ArrayList<WhitespaceGap>();
		
		List<WhitespaceGap> currGaps = new ArrayList<WhitespaceGap>();
		for (TextLine l : items)
		{
			List<WhitespaceGap> thisLineGaps = new ArrayList<WhitespaceGap>();
			
			TextSegment prevSeg = null;
			for (TextSegment thisSeg : l.getItems())
			{
				if (prevSeg != null && thisSeg.getX1() > prevSeg.getX2())
				{
					thisLineGaps.add(new WhitespaceGap
							(prevSeg.getX2(), thisSeg.getX1(), l.getY1(), l.getY2(),
									l.getFontSize()));
				}
				
				prevSeg = thisSeg;
			}
			
			List<WhitespaceGap> usedCurrGaps = new ArrayList<WhitespaceGap>();
			List<WhitespaceGap> newCurrGaps = new ArrayList<WhitespaceGap>();
			List<WhitespaceGap> matchedLineGaps = new ArrayList<WhitespaceGap>();
			
			// create lengthened gaps from currGaps by seeing whether they intersect
			// with this line's gaps
			for (WhitespaceGap wgc : currGaps)
			{
				for (WhitespaceGap wgl : thisLineGaps)
				{
					if (SegmentUtils.horizIntersect(wgc, wgl))
					{
						// create new wgc and shrink its horizontal extent if necessary
						// for n+1 lines
						
						WhitespaceGap wgc1 = new WhitespaceGap(wgc);
						wgc1.numLines ++;
						
						if (wgl.getX1() > wgc.getX1()) wgc1.setX1(wgl.getX1());
						if (wgl.getX2() < wgc.getX2()) wgc1.setX2(wgl.getX2());
						
						// set fontsize to the larger of the two
						if (wgl.fontSize > wgc.fontSize)
							wgc1.fontSize = wgl.fontSize;
						
						// mark the old wgc as "used"
						// this will stop it being removed from currGaps later
						usedCurrGaps.add(wgc);
						newCurrGaps.add(wgc1);
						
//						matchedLineGaps.add(wgl);
						
						// create new wgc for this line only for 1 line
						
						/*
						wgc.numLines ++;
						if (wgl.fontSize > wgc.fontSize)
							wgc.fontSize = wgl.fontSize;
						usedCurrGaps.add(wgc);
						matchedLineGaps.add(wgl);
						break;
						*/
						
						// do not break as there may be further gaps to enlarge
					}
				}
			}
			
			
			// move all gaps from currGaps, which did not intersect this line's gaps
			// and therefore cannot be extended any more, to finalGaps
			List<WhitespaceGap> unusedCurrGaps = new ArrayList<WhitespaceGap>();
			for (WhitespaceGap wgc : currGaps)
				if (!usedCurrGaps.contains(wgc))
					unusedCurrGaps.add(wgc);
			finalGaps.addAll(unusedCurrGaps);
			
			// currGaps will now contain the newly extended gaps and the single-liners
			// from the current line
			currGaps.clear();
			currGaps.addAll(newCurrGaps);
			currGaps.addAll(thisLineGaps);
		}
		
		finalGaps.addAll(currGaps);
		
		
		
		for (WhitespaceGap wgf : finalGaps)
		{
			if (wgf.numLines == 2)
			{
				if (wgf.getWidth() > wgf.fontSize * gapWidth)
				{
					retVal = 0;
				}
			}
			if (wgf.numLines > 2)
			{
				if (wgf.getWidth() > wgf.fontSize * gapWidth * 0.5)
				{
					retVal = 0;
				}
			}
		}
		
		return retVal;
	}
	
	// 2019-05-06
	public float evalWhitespaceOld()
	{
		float retVal = 1.0f;
		
		List<WhitespaceGap> finalGaps = new ArrayList<WhitespaceGap>();
		
		List<WhitespaceGap> currGaps = new ArrayList<WhitespaceGap>();
		for (TextLine l : items)
		{
			List<WhitespaceGap> thisLineGaps = new ArrayList<WhitespaceGap>();
			
			TextSegment prevSeg = null;
			for (TextSegment thisSeg : l.getItems())
			{
				if (prevSeg != null && thisSeg.getX1() > prevSeg.getX2())
				{
					thisLineGaps.add(new WhitespaceGap
							(prevSeg.getX2(), thisSeg.getX1(), l.getY1(), l.getY2(),
									l.getFontSize()));
				}
				
				prevSeg = thisSeg;
			}
			
			List<WhitespaceGap> usedCurrGaps = new ArrayList<WhitespaceGap>();
			List<WhitespaceGap> matchedLineGaps = new ArrayList<WhitespaceGap>();
			
			for (WhitespaceGap wgc : currGaps)
			{
				for (WhitespaceGap wgl : thisLineGaps)
				{
					if (SegmentUtils.horizIntersect(wgc, wgl))
					{
						wgc.numLines ++;
						if (wgl.fontSize > wgc.fontSize)
							wgc.fontSize = wgl.fontSize;
						usedCurrGaps.add(wgc);
						matchedLineGaps.add(wgl);
						break;
					}
					// TODO: if doesn't intersect 100%, reduce with
				}
			}
			
			// TODO: if gap is shrunk (or later grown) horizontally, add twice!
			
			// move all gaps from currGaps which are not in usedCurrGaps to finalGaps
			List<WhitespaceGap> unusedCurrGaps = new ArrayList<WhitespaceGap>();
			for (WhitespaceGap wgc : currGaps)
				if (!usedCurrGaps.contains(wgc))
					unusedCurrGaps.add(wgc);
			currGaps.removeAll(unusedCurrGaps);
			finalGaps.addAll(unusedCurrGaps);
			
			// add all unmatched thisLineGaps to currGaps
			for (WhitespaceGap wgl : thisLineGaps)
				if (!matchedLineGaps.contains(wgl))
					currGaps.add(wgl);
		}
		
		finalGaps.addAll(currGaps);
		
		
		
		for (WhitespaceGap wgf : finalGaps)
		{
			if (wgf.numLines == 2)
			{
				if (wgf.getWidth() > wgf.fontSize * 0.5)
				{
					retVal = 0;
				}
			}
			if (wgf.numLines > 2)
			{
				if (wgf.getWidth() > wgf.fontSize * 0.25)
				{
					retVal = 0;
				}
			}
		}
		
		return retVal;
	}
	
	// TODO: for alignment, also depend slightly on segment width?
	
	public float evalLeftAlign()
	{
		if (items.size() < 1)
			return -1;
		
		// calculate average leftmost coordinate
		float sumX1 = 0.0f;
    	int numValues = 0;
		for (TextLine thisSeg : items)
		{
			sumX1 += thisSeg.getX1();
			numValues ++;
		}
		
		float avgX1 = sumX1 / numValues;
		
		float maxDiff = 0;
		
		for (TextLine thisSeg : items)
		{
			float x1Diff = thisSeg.getX1() - avgX1;
			if (x1Diff < 0) x1Diff = 0.0f - x1Diff;
			
			if (x1Diff > maxDiff) maxDiff = x1Diff;
		}
		
		// calculate related deviation to fontsize
		float relMaxDiff = maxDiff / calcAvgFontSize();
		
		// TODO: add 10% of width of block to standard deviation
		return Utils.normgaussian(relMaxDiff, 0, 0.5f);
	}
	
	public float evalCentreAlign()
	{
		if (items.size() < 1)
			return -1;
		
		// calculate average centre coordinate
		float sumXmid = 0.0f;
    	int numValues = 0;
		for (TextLine thisSeg : items)
		{
			sumXmid += thisSeg.getXmid();
			numValues ++;
		}
		
		float avgXmid = sumXmid / numValues;
		
		float maxDiff = 0;
		
		for (TextLine thisSeg : items)
		{
			float xMidDiff = thisSeg.getXmid() - avgXmid;
			if (xMidDiff < 0) xMidDiff = 0.0f - xMidDiff;
			
			if (xMidDiff > maxDiff) maxDiff = xMidDiff;
		}
		
		// calculate related deviation to fontsize
		float relMaxDiff = maxDiff / calcAvgFontSize();
		
		// TODO: add 10% of width of block to standard deviation
		return Utils.normgaussian(relMaxDiff, 0, 0.5f);
	}
	
	public float evalRightAlign(boolean lastLine)
	{
		if (items.size() < 1)
			return -1;
		
		if (!lastLine && items.size() < 2)
			return -1;
		
		// calculate average rightmost coordinate
		float sumX2 = 0.0f;
    	int numValues = 0;
		for (TextLine thisSeg : items)
		{
			if (lastLine || thisSeg != items.get(items.size() - 1))
			{
				sumX2 += thisSeg.getX2();
				numValues ++;
			}
		}
		
		float avgX2 = sumX2 / numValues;
		
		float maxDiff = 0;
		
		for (TextLine thisSeg : items)
		{
			if (lastLine || thisSeg != items.get(items.size() - 1))
			{
				float x2Diff = thisSeg.getX2() - avgX2;
				if (x2Diff < 0) x2Diff = 0.0f - x2Diff;
				
				if (x2Diff > maxDiff) maxDiff = x2Diff;
			}
		}
		
		// calculate related deviation to fontsize
		float relMaxDiff = maxDiff / calcAvgFontSize();
		
		// TODO: add 10% of width of block to standard deviation
		return Utils.normgaussian(relMaxDiff, 0, 0.5f);
	}
	
	public float evalRightAlign()
	{
		return evalRightAlign(true);
	}
	
	public float evalBlockAlign()
	{
		return Utils.minimum(evalLeftAlign(), evalRightAlign());
	}
	
	public float evalJustify()
	{
		if (items.size() < 2) // currently returns -1 and not 1 if no check possible
			return evalLeftAlign();
		else
			return Utils.minimum(evalLeftAlign(), evalRightAlign(false));
	}
	
	public float calcAvgLineSpacing()
    {
    	// calculate existing line spacing average
    	
		// assume sorted vertically
//		Collections.sort(blockFrom.getItems(), new YComparator());
		
    	float sumLineSpaces = 0.0f;
    	int numValues = 0;
		TextSegment prevSeg = null;
		for (TextLine thisSeg : items)
		{
			if (prevSeg == null)
			{
				// do nothing
			}
			else
			{
				float lineSpacing = prevSeg.getY1() - thisSeg.getY1();
				sumLineSpaces += lineSpacing;
				numValues ++;
			}
			prevSeg = thisSeg;
		}
		
		return(sumLineSpaces / numValues);
    }
	
	// potentially useful later for "paragraphs" with variable
	// fontsize/line heights
	public float calcAvgLineGaps()
    {
    	// calculate existing line spacing average
    	
		// assume sorted vertically
//		Collections.sort(blockFrom.getItems(), new YComparator());
		
    	float sumLineGaps = 0.0f;
    	int numValues = 0;
		TextSegment prevSeg = null;
		for (TextLine thisSeg : items)
		{
			if (prevSeg == null)
			{
				// do nothing
			}
			else
			{
				float lineGap = prevSeg.getY1() - thisSeg.getY2();
				sumLineGaps += lineGap;
				numValues ++;
			}
			prevSeg = thisSeg;
		}
		
		return(sumLineGaps / numValues);
    }
	
	public float calcAvgFontSize()
    {
    	// calculate existing font size average
    	
		// assume sorted vertically
//		Collections.sort(blockFrom.getItems(), new YComparator());
		
    	float sumLineSpacings = 0.0f;
    	int numValues = 0;
		for (TextLine thisSeg : items)
		{
			if (thisSeg.getFontSize() <= 0)
			{
//				System.err.println("invalid fontsize of line");
			}
			else
			{
				sumLineSpacings += thisSeg.getFontSize();
				numValues ++;
			}
		}
		
		return(sumLineSpacings / numValues);
    }
	
    // IXMillumSegment
	public void setElementAttributes(Document resultDocument, 
    	Element newSegmentElement, GenericSegment pageDim, float resolution, int id)
    {
        super.setElementAttributes(resultDocument, newSegmentElement, pageDim, 
        		resolution, id);
        
        // TODO: HACK -- the below lines refer to the this.getText() method, as the
        // text currently is not stored.  But this is due to change when the
        // line-finding is integrated.
        newSegmentElement.setAttribute
            ("font-size", Float.toString(this.getFontSize()));
//	        newSegmentElement.setAttribute
//	            ("text-ratio", Float.toString(this.getTextRatio()));
//	        newSegmentElement.setAttribute
//	        	("info", getInfoString());
        
        String type = "unknown";
        switch(classification)
        {
        	case PARAGRAPH:
        		type = "paragraph"; break;
        	case HEADING:
        		type = "heading"; break;
        	case OTHER_TEXT:
        		type = "other-text"; break;
        	case CELL:
        		type = "cell"; break;
        	case ORDERED_LIST_ITEM:
        		type = "ol-item"; break;
        	case UNORDERED_LIST_ITEM:
        		type = "ul-item"; break;
        	case HEADER:
        		type = "header"; break;
        	case FOOTER:
        		type = "footer"; break;
        	case TABLE_HEADING:
        		type = "table-heading"; break;
        	case TABLE_SUBTITLE:
        		type = "table-subtitle"; break;
        	default:
        		type = "other";
        }
        
        newSegmentElement.setAttribute("type", type);
        
        // System.out.println("creating text node: " + this.getText());
        // done in super!
        //newSegmentElement.appendChild
            //(resultDocument.createTextNode(this.getText()));
    }

	public void addAsXHTML(Document resultDocument, Element parent)//, GenericSegment pageDim)
    {
        Element newParagraphElement, newTextElement, tempElement = null;
        
        if (classification == HEADING || classification == HEADING_3)
            newParagraphElement = resultDocument.createElement("h3");
        else if (classification == HEADING_2)
            newParagraphElement = resultDocument.createElement("h2");
        else if (classification == HEADING_1)
            newParagraphElement = resultDocument.createElement("h1");
        else if (classification == UNORDERED_LIST_ITEM)
        {
//        	tempElement = resultDocument.createElement("ul");
//        	newParagraphElement = resultDocument.createElement("li");
        	// 22.01.2011 <ul>s are now separate objects
        	newParagraphElement = resultDocument.createElement("li");
        }
        else
            newParagraphElement = resultDocument.createElement("p");

        // HEADING_1 to HEADING_3 in str mode
        if (classification >= 40 && classification < 60)
        {
        	boolean bold = false;
        	boolean italic = false;
        	boolean underlined = false;
        	int superSubscript = 0;
        	String textToAdd = "";
        	float prevX2 = -1.0f;
        	
        	Iterator iter1 = items.iterator();
        	while(iter1.hasNext())
        	{
        		TextLine tl1 = (TextLine)iter1.next();
//        		System.out.println("tl1: " + tl1);
        		if (tl1.getX1() < prevX2)
				{
					// new line: do we insert a carriage return?
					if (prevX2 < strXPosNewline)
						textToAdd = textToAdd + ("\n");
				}
				prevX2 = tl1.getX2();
				
        		Iterator iter2 = tl1.getItems().iterator();
        		while(iter2.hasNext())
        		{
        			TextLine tl2 = (TextLine)iter2.next();
//        			System.out.println("tl2: " + tl2);
        			TextFragment prevFrag = null;
        			Iterator iter3 = tl2.getItems().iterator();
        			while(iter3.hasNext())
        			{
        				TextFragment tf = (TextFragment)iter3.next();
//        				System.out.println("tf: " + tf);
//        				System.out.println("tf is superSubscript: " + tf.isStrIsUnderlined());
        				
        				// if neither matches the whitespace character
        				// and horiz gap > 0.25(afs)
        				if (prevFrag != null)
        				{
        					float horizGap = tf.getX1() - prevFrag.getX2();
        					float afs = (tf.getFontSize() + prevFrag.getFontSize()) / 2.0f;
        					if (!(tf.getText().trim().matches("[\\s]") || prevFrag.getText().trim().matches("[\\s]")) &&
        						horizGap > afs * 0.15f)
        					{
        						textToAdd = textToAdd + " ";
        					}
        				}	
        				
        				if (tf.isBold() == bold && tf.isItalic() == italic && 
        					tf.isUnderlined() == underlined && tf.getSuperSubscript() == superSubscript)
        				{
        					// same style as previous character
//        					textToAdd.concat(tf.getText()); // doesn't work?!?
        					textToAdd = textToAdd + (tf.getText());
        				}
        				else
        				{
        					// add text
//        					if (textToAdd.length() > 0)
        					if (textToAdd.trim().length() > 0)
        					{
        						if (superSubscript == 1)
        						{
	        						if (underlined)
	        						{
	//        							System.out.println("underlined with textToAdd: " + textToAdd);
	//	        		        		System.out.println("textToAdd.length: " + textToAdd.trim().length());
	        							if (bold && italic)
	    	        		        	{
	        								Element newTextElement4 = resultDocument.createElement("sup");
	        								newParagraphElement.appendChild(newTextElement4);
	        								Element newTextElement3 = resultDocument.createElement("u");
	    	        		        		newTextElement4.appendChild(newTextElement3);
	    	        		            	Element newTextElement2 = resultDocument.createElement("b");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (bold)
	    	        		        	{
	    	        		        		Element newTextElement3 = resultDocument.createElement("sup");
	    	        		        		newParagraphElement.appendChild(newTextElement3);
	    	        		        		Element newTextElement2 = resultDocument.createElement("u");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("b");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (italic)
	    	        		        	{
	    	        		        		Element newTextElement3 = resultDocument.createElement("sup");
	    	        		        		newParagraphElement.appendChild(newTextElement3);
	    	        		        		Element newTextElement2 = resultDocument.createElement("u");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else
	    	        		        	{
	    	        		        		Element newTextElement2 = resultDocument.createElement("sup");
	    	        		        		newParagraphElement.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("u");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	        						}
	        						else
	        						{
	        							if (bold && italic)
	    	        		        	{
	        								Element newTextElement3 = resultDocument.createElement("sup");
	        								newParagraphElement.appendChild(newTextElement3);
	    	        		            	Element newTextElement2 = resultDocument.createElement("b");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (bold)
	    	        		        	{
	    	        		        		Element newTextElement2 = resultDocument.createElement("sup");
	    	        		        		newParagraphElement.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("b");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (italic)
	    	        		        	{
	    	        		        		Element newTextElement2 = resultDocument.createElement("sup");
	    	        		        		newParagraphElement.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else
	    	        		        	{
	    	        		        		newTextElement = resultDocument.createElement("sup");
	    	        		        		newParagraphElement.appendChild(newTextElement);
	    	        		        	}
	        						}
        						}
        						else if (superSubscript == -1)
        						{
//        							System.out.println("outputting subscript");
        							if (underlined)
	        						{
	//        							System.out.println("underlined with textToAdd: " + textToAdd);
	//	        		        		System.out.println("textToAdd.length: " + textToAdd.trim().length());
	        							if (bold && italic)
	    	        		        	{
	        								Element newTextElement4 = resultDocument.createElement("sub");
	        								newParagraphElement.appendChild(newTextElement4);
	        								Element newTextElement3 = resultDocument.createElement("u");
	    	        		        		newTextElement4.appendChild(newTextElement3);
	    	        		            	Element newTextElement2 = resultDocument.createElement("b");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (bold)
	    	        		        	{
	    	        		        		Element newTextElement3 = resultDocument.createElement("sub");
	    	        		        		newParagraphElement.appendChild(newTextElement3);
	    	        		        		Element newTextElement2 = resultDocument.createElement("u");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("b");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (italic)
	    	        		        	{
	    	        		        		Element newTextElement3 = resultDocument.createElement("sub");
	    	        		        		newParagraphElement.appendChild(newTextElement3);
	    	        		        		Element newTextElement2 = resultDocument.createElement("u");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else
	    	        		        	{
	    	        		        		Element newTextElement2 = resultDocument.createElement("sub");
	    	        		        		newParagraphElement.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("u");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	        						}
	        						else
	        						{
	        							if (bold && italic)
	    	        		        	{
	        								Element newTextElement3 = resultDocument.createElement("sub");
	        								newParagraphElement.appendChild(newTextElement3);
	    	        		            	Element newTextElement2 = resultDocument.createElement("b");
	    	        		        		newTextElement3.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (bold)
	    	        		        	{
	    	        		        		Element newTextElement2 = resultDocument.createElement("sub");
	    	        		        		newParagraphElement.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("b");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else if (italic)
	    	        		        	{
	    	        		        		Element newTextElement2 = resultDocument.createElement("sub");
	    	        		        		newParagraphElement.appendChild(newTextElement2);
	    	        		        		newTextElement = resultDocument.createElement("i");
	    	        		        		newTextElement2.appendChild(newTextElement);
	    	        		        	}
	    	        		        	else
	    	        		        	{
	    	        		        		newTextElement = resultDocument.createElement("sub");
	    	        		        		newParagraphElement.appendChild(newTextElement);
	    	        		        	}
	        						}
        						}
        						else // normal text (required in order to initialize newTextElement)
        						{
        							if (underlined)
            						{
//            							System.out.println("underlined with textToAdd: " + textToAdd);
//    	        		        		System.out.println("textToAdd.length: " + textToAdd.trim().length());
            							if (bold && italic)
        	        		        	{
            								Element newTextElement3 = resultDocument.createElement("u");
        	        		        		newParagraphElement.appendChild(newTextElement3);
        	        		            	Element newTextElement2 = resultDocument.createElement("b");
        	        		        		newTextElement3.appendChild(newTextElement2);
        	        		        		newTextElement = resultDocument.createElement("i");
        	        		        		newTextElement2.appendChild(newTextElement);
        	        		        	}
        	        		        	else if (bold)
        	        		        	{
        	        		        		Element newTextElement2 = resultDocument.createElement("u");
        	        		        		newParagraphElement.appendChild(newTextElement2);
        	        		        		newTextElement = resultDocument.createElement("b");
        	        		        		newTextElement2.appendChild(newTextElement);
        	        		        	}
        	        		        	else if (italic)
        	        		        	{
        	        		        		Element newTextElement2 = resultDocument.createElement("u");
        	        		        		newParagraphElement.appendChild(newTextElement2);
        	        		        		newTextElement = resultDocument.createElement("i");
        	        		        		newTextElement2.appendChild(newTextElement);
        	        		        	}
        	        		        	else
        	        		        	{
        	        		        		newTextElement = resultDocument.createElement("u");
        	        		        		newParagraphElement.appendChild(newTextElement);
        	        		        	}
            						}
            						else
            						{
            							if (bold && italic)
        	        		        	{
        	        		            	Element newTextElement2 = resultDocument.createElement("b");
        	        		        		newParagraphElement.appendChild(newTextElement2);
        	        		        		newTextElement = resultDocument.createElement("i");
        	        		        		newTextElement2.appendChild(newTextElement);
        	        		        	}
        	        		        	else if (bold)
        	        		        	{
//        	        		        		System.out.println("bold with textToAdd: " + textToAdd);
//        	        		        		System.out.println("textToAdd.length: " + textToAdd.trim().length());
        	        		        		newTextElement = resultDocument.createElement("b");
        	        		        		newParagraphElement.appendChild(newTextElement);
        	        		        	}
        	        		        	else if (italic)
        	        		        	{
        	        		        		newTextElement = resultDocument.createElement("i");
        	        		        		newParagraphElement.appendChild(newTextElement);
        	        		        	}
        	        		        	else
        	        		        	{
        	        		            	newTextElement = newParagraphElement;
        	        		        	}
            						}
        						}
	        		            
	        		            // the following lines would just add the string
	        		            // without <br/>s
	        		            //newColumnElement.appendChild
	        		    		//(resultDocument.createTextNode(theText));
	        		            String textSection = new String();
	        		            
	        		            for (int n = 0; n < textToAdd.length(); n ++)
	        		            {
	        		            	String thisChar = textToAdd.substring(n, n + 1);
	        		            	if (thisChar.equals("\n"))
	        		            	{
	        		            		newTextElement.appendChild
	        		            			(resultDocument.createTextNode(textSection));
	        		                    newTextElement.appendChild
	        		                    	(resultDocument.createElement("br"));
	        		                    textSection = "";
	        		            	}
	        		            	else
	        		            	{
	        		            		textSection = textSection.concat(thisChar);
	        		            	}
	        		            }
	        		            
	        		            if (textSection.length() > 0)
	        		            	
	        		            	newTextElement.appendChild
	        		            	(resultDocument.createTextNode(textSection));
        					
        					}
        					
        					// update bold and italic
        		            bold = tf.isBold();
        		            italic = tf.isItalic();
        		            underlined = tf.isUnderlined();
        		            textToAdd = "";
        		            textToAdd = textToAdd + (tf.getText());
        		            superSubscript = tf.getSuperSubscript();
        				}
        				prevFrag = tf;
        			}
        		}
        	}
        	
        	// if remaining text
        	if(textToAdd.trim().length() > 0)
        	{
        		if (bold && italic)
	        	{
	            	Element newTextElement2 = resultDocument.createElement("b");
	        		newParagraphElement.appendChild(newTextElement2);
	        		newTextElement = resultDocument.createElement("i");
	        		newTextElement2.appendChild(newTextElement);
	        	}
	        	else if (bold)
	        	{
	        		newTextElement = resultDocument.createElement("b");
	        		newParagraphElement.appendChild(newTextElement);
	        	}
	        	else if (italic)
	        	{
	        		newTextElement = resultDocument.createElement("i");
	        		newParagraphElement.appendChild(newTextElement);
	        	}
	        	else
	        	{
	            	newTextElement = newParagraphElement;
	        	}
	            
	            // the following lines would just add the string
	            // without <br/>s
	            //newColumnElement.appendChild
	    		//(resultDocument.createTextNode(theText));
	            String textSection = new String();
	            
	            for (int n = 0; n < textToAdd.length(); n ++)
	            {
	            	String thisChar = textToAdd.substring(n, n + 1);
	            	if (thisChar.equals("\n"))
	            	{
	            		newTextElement.appendChild
	            			(resultDocument.createTextNode(textSection));
	                    newTextElement.appendChild
	                    	(resultDocument.createElement("br"));
	                    textSection = "";
	            	}
	            	else
	            	{
	            		textSection = textSection.concat(thisChar);
	            	}
	            }
	            
	            if (textSection.length() > 0)
	            	
	            	newTextElement.appendChild
	            	(resultDocument.createTextNode(textSection));

        	}
        	
        }
        else // normal mode
        {
        	if (isBold() && isItalic())
        	{
            	Element newTextElement2 = resultDocument.createElement("b");
        		newParagraphElement.appendChild(newTextElement2);
        		newTextElement = resultDocument.createElement("i");
        		newTextElement2.appendChild(newTextElement);
        	}
        	else if (isBold())
        	{
        		newTextElement = resultDocument.createElement("b");
        		newParagraphElement.appendChild(newTextElement);
        	}
        	else if (isItalic())
        	{
        		newTextElement = resultDocument.createElement("i");
        		newParagraphElement.appendChild(newTextElement);
        	}
        	else
        	{
            	newTextElement = newParagraphElement;
        	}
            
            String theText = this.getText();
            // the following lines would just add the string
            // without <br/>s
            //newColumnElement.appendChild
    		//(resultDocument.createTextNode(theText));
            String textSection = new String();
            
            for (int n = 0; n < theText.length(); n ++)
            {
            	String thisChar = theText.substring(n, n + 1);
            	if (thisChar.equals("\n"))
            	{
            		newTextElement.appendChild
            			(resultDocument.createTextNode(textSection));
                    newTextElement.appendChild
                    	(resultDocument.createElement("br"));
                    textSection = "";
            	}
            	else
            	{
            		textSection = textSection.concat(thisChar);
            	}
            }
            
            if (textSection.length() > 0)
            	
            	newTextElement.appendChild
            	(resultDocument.createTextNode(textSection));
        }

        /*
        newParagraphElement.appendChild
            (resultDocument.createTextNode(this.getText()));
        */
        
        // 22.01.2011 <ul>s are now separate objects
        /*
        if (classification == UNORDERED_LIST_ITEM)
        {
        	tempElement.appendChild(newParagraphElement);
        	parent.appendChild(tempElement);
        }
        else
        */
        	parent.appendChild(newParagraphElement);
    }
	
	class WhitespaceGap extends GenericSegment
	{
		int numLines = 1;
		float fontSize = 0;
		
		public WhitespaceGap(float x1, float x2, float y1, float y2, float fontSize)
		{
			super(x1, x2, y1, y2);
			this.fontSize = fontSize;
		}
		
		// clones
		public WhitespaceGap(WhitespaceGap wgc)
		{
			super(wgc.x1, wgc.x2, wgc.y1, wgc.y2);
			this.fontSize = wgc.fontSize;
			this.numLines = wgc.numLines;
		}
	}
}