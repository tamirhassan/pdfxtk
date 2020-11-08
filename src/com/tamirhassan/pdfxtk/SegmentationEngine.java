package com.tamirhassan.pdfxtk;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.tamirhassan.pdfxtk.comparators.BFEdgeScoringComparator;
import com.tamirhassan.pdfxtk.graph.AdjacencyEdge;
import com.tamirhassan.pdfxtk.graph.AdjacencyGraph;
import com.tamirhassan.pdfxtk.model.TextBlock;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.TextLine;
import com.tamirhassan.pdfxtk.model.TextFragment;
import com.tamirhassan.pdfxtk.model.TextSegment;
import com.tamirhassan.pdfxtk.utils.ImgOutputUtils;
import com.tamirhassan.pdfxtk.utils.ListUtils;
import com.tamirhassan.pdfxtk.utils.SegmentUtils;
import com.tamirhassan.pdfxtk.utils.Utils;

//public class Segmentation<T extends CompositeSegment> {

public class SegmentationEngine
{
	final protected static boolean SWALLOW_CHECK = true; // normally true
//	final protected static boolean INVERSE_SWALLOW_CHECK = true;
	final protected static boolean SECOND_PASS = true;
	
	protected final static boolean SCORING_COMPARATOR = false;
	
	AdjacencyGraph<TextFragment> ag;
	String imgPath = "intermediate/segmentation-";
	GenericSegment pageDims = null;
	SegmentationRules rules;
	
	boolean resort = true;
	boolean checkOverlap = false;
	
	List<SegmentationResult> segmentationHistory = 
			new ArrayList<SegmentationResult>();
	
	List<List<TextFragment>> affectedFragments =
			new ArrayList<List<TextFragment>>();
	
	public AdjacencyGraph<TextFragment> getAg() {
		return ag;
	}

	public void setAg(AdjacencyGraph<TextFragment> ag) {
		this.ag = ag;
	}

	public String getImgPath() {
		return imgPath;
	}

	public void setImgPath(String imgPath) {
		this.imgPath = imgPath;
	}

	public GenericSegment getPageDims() {
		return pageDims;
	}

	public void setPageDims(GenericSegment pageDims) {
		this.pageDims = pageDims;
	}

	public boolean isDebugPrint() {
		return debugPrint;
	}

	public void setDebugPrint(boolean debugPrint) {
		this.debugPrint = debugPrint;
	}

	public boolean isDebugImg() {
		return debugImg;
	}

	public void setDebugImg(boolean debugImg) {
		this.debugImg = debugImg;
	}

	public boolean isDebugImgFail() {
		return debugImgFail;
	}

	public void setDebugImgFail(boolean debugImgFail) {
		this.debugImgFail = debugImgFail;
	}

	public boolean isResort() {
		return resort;
	}

	public void setResort(boolean resort) {
		this.resort = resort;
	}

	public boolean isCheckOverlap() {
		return checkOverlap;
	}

	public void setCheckOverlap(boolean checkOverlap) {
		this.checkOverlap = checkOverlap;
	}

	boolean debugPrint = false;
	boolean debugImg = false;
	boolean debugImgFail = false;
	
	/*
	List<AdjacencyEdge<TextFragment>> orderedEdges;
	List<AdjacencyEdge<TextFragment>> rejectedEdges;
	List<CandidateBlock> segmentationResult;
	HashMap<TextFragment, CandidateBlock> blockMap;
	*/
	
	public SegmentationEngine(AdjacencyGraph<TextFragment> ag, SegmentationRules sr)
	{
		this.ag = ag;
		this.rules = sr;
	}
	
	// 2019-04-29 BEGIN BEST FIRST 
	
	// intrinsic edge properties
	
	/**
	 * Entry method - starts the segmentation process
	 * 
	 * @return
	 */
	public List<TextBlock> doSegmentation()
	{
		// duplicate edge list so unaffected by sorting
		List<AdjacencyEdge<TextFragment>> orderedEdges = 
				new ArrayList<AdjacencyEdge<TextFragment>>();
		orderedEdges.addAll(ag.getEdges());
				
		rules.setPass(3);
		
		SegmentationResult result = new SegmentationResult();
		
		// start with each fragment as a cluster
		for (TextFragment ts : ag.getVertSegmentList())
		{
			TextBlock newBlock = new TextBlock(ts);
			result.segments.add(newBlock);
			result.blockMap.put(ts, newBlock);
		}

		// first result in segmentation history
		segmentationHistory.add(new SegmentationResult(result));
		List<TextFragment> af = new ArrayList<TextFragment>();
		af.addAll(ag.getVertSegmentList());
		affectedFragments.add(af);
		
		// FIRST PASS
		
		int index = -1;
		int total = ag.getEdges().size();
		
//		performPass(index, result, orderedEdges, true, true);
		performPass(index, result, orderedEdges, resort, false);
		
		if (debugPrint) System.out.println("finished segmentation with index = " + index);
		
		return result.segments;
	}
	
	/**
	 * Does not appear to be in use any more
	 * 
	 * @param input
	 * @param weight
	 * @return
	 */
	public static float weight(float input, float weight)
	{
		return (input * weight) + (1.0f - weight);
	}
	
	// completeBrickwork
	
	// check that evaluation methods work!
	
	/**
	 * Used by comparator to order edges "best first"
	 * i.e. prpcess edges which can be processed non-ambiguously first
	 * to avoid backtracking
	 * 
	 * Does not appear to be in use at the moment
	 * 
	 * @param e
	 * @param result
	 * @param debugText
	 * @return float value
	 */
	public static float edgeOrderingValue(AdjacencyEdge<? extends TextSegment> e,
			SegmentationResult result)
	{
		return edgeOrderingValue(e, result, false);
	}
	
	/**
	 * Used by comparator to order edges "best first"
	 * i.e. prpcess edges which can be processed non-ambiguously first
	 * to avoid backtracking
	 * 
	 * Does not appear to be in use at the moment
	 * 
	 * @param e
	 * @param result
	 * @param debugText
	 * @return float value
	 */
	public static float edgeOrderingValue(AdjacencyEdge<? extends TextSegment> e,
			SegmentationResult result, boolean debugText)
	{
		/*
		System.out.println("sd 1:");
		System.out.println("nd with 0.5. " + Utils.normgaussian(0.5f, 0.0f, 1.0f));
		System.out.println("nd with 1.0: " + Utils.normgaussian(1.0f, 0.0f, 1.0f));
		System.out.println("nd with 1.5: " + Utils.normgaussian(1.5f, 0.0f, 1.0f));
		System.out.println("nd with 2.0: " + Utils.normgaussian(2.0f, 0.0f, 1.0f));
		
		System.out.println("sd 0.5:");
		System.out.println("nd with 0.5. " + Utils.normgaussian(0.5f, 0.0f, 0.5f));
		System.out.println("nd with 1.0: " + Utils.normgaussian(1.0f, 0.0f, 0.5f));
		System.out.println("nd with 1.5: " + Utils.normgaussian(1.5f, 0.0f, 0.5f));
		System.out.println("nd with 2.0: " + Utils.normgaussian(2.0f, 0.0f, 0.5f));
		
		System.out.println("sd 2.0:");
		System.out.println("nd with 0.5. " + Utils.normgaussian(0.5f, 0.0f, 2.0f));
		System.out.println("nd with 1.0: " + Utils.normgaussian(1.0f, 0.0f, 2.0f));
		System.out.println("nd with 1.5: " + Utils.normgaussian(1.5f, 0.0f, 2.0f));
		System.out.println("nd with 2.0: " + Utils.normgaussian(2.0f, 0.0f, 2.0f));
		
		sd 1:
		nd with 0.5. 0.8824969
		nd with 1.0: 0.60653067
		nd with 1.5: 0.32465246
		nd with 2.0: 0.13533528
		sd 0.5:
		nd with 0.5. 0.60653067
		nd with 1.0: 0.13533528
		nd with 1.5: 0.011108996
		nd with 2.0: 3.3546262E-4
		sd 2.0:
		nd with 0.5. 0.9692332
		nd with 1.0: 0.8824969
		nd with 1.5: 0.7548396
		nd with 2.0: 0.60653067
		*/	
		
		float retVal = 1.0f;
		
		if (debugText)
		{
			System.out.println("in bfEdgeOrdering Value with: " + e);
		}
		
		TextBlock blockFrom = result.blockMap.get(e.getNodeFrom());
		TextBlock blockTo = result.blockMap.get(e.getNodeTo());
		
		if (e.getDirection() == AdjacencyEdge.REL_BELOW)
		{
			// currently not in use! later when trying out
			TextLine lineFrom = blockFrom.getLastItem();
			TextLine lineTo = blockTo.getFirstItem();

			// prioritize thinner, column-like structures
//			float avgWidth = 0.5f * 
//					(e.getNodeFrom().getWidth() + e.getNodeTo().getWidth());
//			float relAvgWidth = avgWidth / e.avgFontSize();
			
			float maxWidth = e.getNodeFrom().getWidth();
			if (e.getNodeTo().getWidth() > maxWidth)
				maxWidth = e.getNodeTo().getWidth();
			float relMaxWidth = maxWidth / e.avgFontSize();
			
			float widthGauss = Utils.normgaussian(relMaxWidth, 2.0f, 2.0f);
			
			// prioritize (vertical) edges that join aligned lines
			
			// penalize (vertical) edges with little horizontal overlap
			// intersection coords
			float iX1 = e.getNodeFrom().getX1();
			if (e.getNodeTo().getX1() > iX1) iX1 = e.getNodeTo().getX1();
			float iX2 = e.getNodeFrom().getX2();
			if (e.getNodeTo().getX2() < iX2) iX2 = e.getNodeTo().getX2();
			
			// maximal (union) coords
			// with overhang on one side generally greater penalty
			float uX1 = e.getNodeFrom().getX1();
			if (e.getNodeTo().getX1() < uX1) uX1 = e.getNodeTo().getX1();
			float uX2 = e.getNodeFrom().getX2();
			if (e.getNodeTo().getX2() > uX2) uX2 = e.getNodeTo().getX2();
			
			float relIntersection = (iX2 - iX1) / (uX2 - uX1);
			
			// TODO: widthGauss should have less of an effect?
			retVal = retVal * weight(widthGauss, 0.2f) * weight(relIntersection, 0.2f);
			
			if (debugText)
			{
				System.out.println("relMaxWidth: " + relMaxWidth);
				System.out.println("widthGauss: " + widthGauss);
				System.out.println("relIntersection: " + relIntersection);
				System.out.println("retVal is now: " + retVal);
			}
		}
		else
		{
			// horizontal edge
			retVal = retVal * 0.5f;
			if (debugText)
				System.out.println("horizontal edge - retVal reduced by half: " + retVal);
		}
		
		// prioritize shorter edges (in particular vertical ones)
		
		
		if (blockFrom.getItems().size() == 1 && blockTo.getItems().size() == 1)
		{
			// one-to-one
			if (debugText)
				System.out.println("one-to-one");
		}
		else if (blockFrom.getItems().size() == 1 || blockTo.getItems().size() == 1)
		{
			// one-to-many
			retVal = retVal * 0.75f;
			if (debugText)
				System.out.println("one-to-many - retVal reduced by 1/4: " + retVal);
		}
		else
		{
			// many-to-many
			retVal = retVal * 0.5f;
			if (debugText)
				System.out.println("many-to-many - retVal reduced by half: " + retVal);
		}

		// penalize fontsize differences
//		if (!Utils.sameFontSize(e.getNodeFrom(), e.getNodeTo()))
//			retVal = retVal * 0.5f;
		
		float fontSizeDiff = e.getNodeFrom().getFontSize() - e.getNodeTo().getFontSize();
		float relFontSizeDiff = fontSizeDiff / e.avgFontSize();
		
		float fontSizeDiffGauss = Utils.normgaussian(relFontSizeDiff, 0.0f, 0.1f);
		
		retVal = retVal * weight(fontSizeDiffGauss, 0.4f);
		
		if (debugText)
		{
			System.out.println("relFontSizeDiff: " + relFontSizeDiff);
			System.out.println("fontSizeDiffGauss: " + fontSizeDiffGauss);
			System.out.println("retVal is now: " + retVal);
		}
		
		// prioritize shorter edges (in particular vertical ones)
		float edgeLength = e.physicalLength();
		if (e.isVertical()) edgeLength = e.baselineDistance();
		float relDistance = edgeLength / e.avgFontSize();
		
		// treat 1.0 line spacing as zero
		if (e.isVertical())
		{
			if (relDistance < 1.0f)
				relDistance = 1.0f;
			relDistance -= 1.0f;
		}
		
		float relDistGauss = Utils.normgaussian(relDistance, 0.0f, 1.0f);
		
		retVal = retVal * relDistGauss;
		
		if (debugText)
		{
			System.out.println("relDistance: " + relDistance);
			System.out.println("relDistGauss: " + relDistGauss);
			System.out.println("retVal is now: " + retVal);
		}
		
		return retVal;
	}
	
	
	// 2019-04-29 END BEST FIRST
	
	
	/**
	 * Adds rejectedEdges to orderedEdges for revisiting on next pass for the fragment
	 * (activeFragment) that has been changed
	 * 
	 * @param activeFragment
	 * @param orderedEdges
	 * @param rejectedEdges
	 * @return
	 */
	protected List<AdjacencyEdge<TextFragment>> addEdgesToRevisit(
			TextFragment activeFragment, 
			List<AdjacencyEdge<TextFragment>> orderedEdges,
			List<AdjacencyEdge<TextFragment>> rejectedEdges)
	{
		List<AdjacencyEdge<TextFragment>> retVal = 
				new ArrayList<AdjacencyEdge<TextFragment>>();
		
		List<AdjacencyEdge<TextFragment>> foundEdges = 
				new ArrayList<AdjacencyEdge<TextFragment>>();
		
		for (AdjacencyEdge<TextFragment> e : rejectedEdges)
		{
			if (e.getNodeFrom() == activeFragment || e.getNodeTo() == activeFragment)
			{
				// TODO: add other edges in same line - how?
				
				foundEdges.add(e);
			}
		}
		
		orderedEdges.addAll(foundEdges);
		rejectedEdges.removeAll(foundEdges);
		
		return retVal;
	}

	/**
	 * Processes the edge as part of the segmentation pass iteration
	 * 
	 * @param e - the edge
	 * @param result - the current state to be processed
	 * @param orderedEdges - list of edges passed on
	 * @param index - current index for debugging
	 * @param total - total number of edges to be processed (for debugging output)
	 * @return BlockOperation representing what has been done
	 */
	public BlockOperation processEdge(AdjacencyEdge<TextFragment> e, SegmentationResult result,
			List<AdjacencyEdge<TextFragment>> orderedEdges, int index, int total)
	{
		// DEBUG
		if (e.isVertical() && e.getNodeFrom().getText().contains("HRESHOLD") ||
				e.getNodeTo().getText().contains("HRESHOLD"))
		{
			if (debugPrint) System.out.println("200");
		}
		
		// DEBUG
		if (debugPrint)
		{
    		System.out.println("----------------------");
			System.out.println("index: " + index);
			System.out.println("Edge: " + e + " hashCode: " + e.hashCode());
			System.out.println("ordering value: " + 
					SegmentationEngine.edgeOrderingValue(e, result, false));
		}
		
		TextBlock blockFrom = result.blockMap.get(e.getNodeFrom());
		TextBlock blockTo = result.blockMap.get(e.getNodeTo());
		
		boolean changed = false;

//		processEdge(e);
		
		BlockOperation bo = new BlockOperation(BlockOperation.SEG_FAILURE);
		
		// initialize affected fragment list (remains empty in case of SEG_FAILURE)
		List<TextFragment> af = new ArrayList<TextFragment>();
		affectedFragments.add(index, af);
		SegmentationResult dupResult = null; // stays null if not SEG_SUCCESS
		    		
		if (blockFrom != blockTo)
		{
			bo = rules.mergeBlocks(blockFrom, blockTo, e);
			
			// TODO: move above and return negative bo if no debugging info required
			if (e.isVertical() && bo.exitStatus == BlockOperation.SEG_SUCCESS)
			{
				// assume edge always points downwards
				
				// check that no shorter edge connects e.nodeFrom with another node above it
				for (AdjacencyEdge e1 : ag.getEdges())
				{
					// comparing fontsize + physical length 
					// as physical length can be tiny in some cases
					float afs = Utils.avg(e.avgFontSize(), e1.avgFontSize());
					if (e1.isVertical() && ((afs + e1.physicalLength()) * 1.10f) < 
							(afs + e.physicalLength()))
					{
						if (e1.getNodeTo() == e.getNodeFrom() ||
								e1.getNodeFrom() == e.getNodeTo())
						{
							// fail
							// return bo;
							bo.exitStatus = BlockOperation.SEG_FAILURE;
						}
					}
				}
			}
			
			// check if merging would cause an overlap
			// (no need to check if already failed)
			
			if (checkOverlap)
			{
//				if (blockFrom.getItems().size() > 1 && blockTo.getItems().size() > 1)
				if (true)
				{
					if (bo.exitStatus == BlockOperation.SEG_SUCCESS || 
							bo.exitStatus == BlockOperation.SEG_BRANCH)
					{
	    				List<GenericSegment> intersectingSegments =	ListUtils.
	    						findElementsWithCentresWithinBBox(result.segments, bo.getResult());
	    				List<GenericSegment> extraSegments = new ArrayList<GenericSegment>();
	    				
						for (GenericSegment gs : intersectingSegments)
							if (!(gs == blockFrom || gs == blockTo))
								extraSegments.add(gs);
						
						if (extraSegments.size() > 0)
						{
							boolean matchFontSize = (rules.pass <= 2);
							
							// change bo.result
							for (GenericSegment gs : extraSegments)
							{
								TextBlock cb = (TextBlock)gs;
								BlockOperation bo2 =
										rules.mergeBlockToBlock(bo.getResult(), cb, matchFontSize);
								
								// this method also runs verifyBlock
								if (bo2.exitStatus == BlockOperation.SEG_FAILURE ||
										bo2.exitStatus == BlockOperation.SEG_FAILURE_NO_REVISIT)
								{
									bo.exitStatus = BlockOperation.SEG_FAILURE;
									break;
								}
							}
						}
					}
				}
			}
			
			if (bo.exitStatus == BlockOperation.SEG_SUCCESS)
			{
    			changed = true;
    			
    			for (TextLine l : blockFrom.getItems())
					for (TextFragment tf : l.getItems())
						result.blockMap.put(tf, bo.getResult());
    			result.segments.remove(blockFrom);
				
				for (TextLine l : blockTo.getItems())
					for (TextFragment tf : l.getItems())
						result.blockMap.put(tf, bo.getResult());
				result.segments.remove(blockTo);
				
				result.segments.add(bo.getResult());
				
				// do the same for the extraSegments
				
				addEdgesToRevisit(e.getNodeFrom(), orderedEdges, result.rejectedEdges);
				addEdgesToRevisit(e.getNodeTo(), orderedEdges, result.rejectedEdges);
				
				// update affected fragments
    			af.addAll(blockFrom.fragments());
    			af.addAll(blockTo.fragments());
    			ListUtils.removeDuplicates(af);
    			
    			// update segmentation history
    			dupResult = new SegmentationResult(result);
    			// TODO: this is costly! perhaps only add affected blocks?
			}
    		else if (bo.exitStatus != BlockOperation.SEG_FAILURE_NO_REVISIT)
    		{
    			if (e.isVertical())
    			{
    				blockFrom.setRejectDistance(e.physicalLength());
    				blockTo.setRejectDistance(e.physicalLength());
    			}
    			
    			result.rejectedEdges.add(e);
    		}
    		
			String imgFilename = String.format ("%05d", index) +
					"-" + String.format ("%03d", 0);
			
    		// output here
			if (changed && pageDims != null && debugImg)// && e.isVertical())
			{
				ImgOutputUtils.outputPNG(result.segments, pageDims, 
		        		imgPath + imgFilename + ".png");
				
				System.out.println("written image: " + imgFilename + " of " + total);
				System.out.println();
			}
			if (!changed && pageDims != null && debugImgFail)
			{
				List<TextBlock> rejectedMerge = new ArrayList<TextBlock>();
				rejectedMerge.add(blockFrom);
				rejectedMerge.add(blockTo);
				
				ImgOutputUtils.outputPNG(rejectedMerge, pageDims, 
		        		imgPath + imgFilename + ".png");
				
				System.out.println("written image: " + imgFilename + " of " + total);
				System.out.println();
			
			}
		}
		
		// update segmentation history
//		SegmentationResult dupResult = new SegmentationResult(result);
//		SegmentationResult dupResult = new SegmentationResult();
		segmentationHistory.add(index, dupResult);
		
		return bo;
	}
	
	/**
	 * Performs a segmentation pass, processing the edges in turn
	 * 
	 * @param index - index number to begin with (for debugging)
	 * @param result - current result to process further
	 * @param orderedEdges - edges in their best-first order
	 */
	public void performPass(int index, SegmentationResult result,
			List<AdjacencyEdge<TextFragment>> orderedEdges)
	{
		performPass(index, result, orderedEdges, false, false);
	}
	
	/**
	 * Performs a segmentation pass, processing the edges in turn
	 * 
	 * @param index - index number to begin with (for debugging)
	 * @param result - current result to process further
	 * @param orderedEdges - edges in their best-first order
	 * @param resort - whether to resort edges after each iteration
	 * @param completeBrickwork - whether to "swallow" overlapping segments at each iteration
	 */
	public void performPass(int index, SegmentationResult result,
			List<AdjacencyEdge<TextFragment>> orderedEdges, boolean resort, 
			boolean completeBrickwork)
	{
		int total = ag.getEdges().size();
		int resortCount = 1;
		
		int hc = -1;
		
		boolean riverProcessing = false;
		List<AdjacencyEdge<TextFragment>> riverEdges = new ArrayList<AdjacencyEdge<TextFragment>>();
		
		while (!orderedEdges.isEmpty())
		{
			long t = System.currentTimeMillis();
			if (resortCount > 0) resortCount --;
			if (resort && resortCount <= 0)
			{
				Collection col = orderedEdges;
//				Comparators.verifyTransitivity(new BFEdgeScoringComparator(result), col);
				Collections.sort(orderedEdges, new BFEdgeScoringComparator(result));
			}
			
			AdjacencyEdge<TextFragment> e = orderedEdges.remove(0);
			if (resort && debugPrint)
			{
				// debug output only
				System.out.println("resortCount is now: " + resortCount);
				edgeOrderingValue(e, result, debugPrint);
			}
    		index ++;
    		// INSERT DEBUG LOOP HERE
    		// to perform code at a specific index
    		
    		BlockOperation bo = processEdge(e, result, orderedEdges, index, total);
    		
    		if (completeBrickwork && bo.getExitStatus() == BlockOperation.SEG_SUCCESS)
    		{
    				
				List<TextBlock> intersectingSegments = new ArrayList<TextBlock>();
				
				// find other segments intersecting the recently joined segment
				for (TextBlock b : result.segments)
				{
					if (b != bo.getResult() && SegmentUtils.centresIntersect(bo.getResult(), b))
					{
						intersectingSegments.add(b);
					}
				}
				
				if (intersectingSegments.size() > 0)
				{
					// and find the appropriate edges leading to them
					// from the recently joined segment
					List<AdjacencyEdge<TextFragment>> intersectingEdges =
							new ArrayList<AdjacencyEdge<TextFragment>>();
					
					if (index == 350)
					{
						ListUtils.printList(orderedEdges);
					}
					
					for (AdjacencyEdge<TextFragment> e2 : orderedEdges)
					{
						if (true)//isVertical())
						{
							if (e.getNodeTo().getText().contains("impossible"))
							{
								System.out.println();
							}
							
	    					TextBlock blockFrom = result.blockMap.get(e2.getNodeFrom());
	    					TextBlock blockTo = result.blockMap.get(e2.getNodeTo());
	    					
	    					if (blockFrom != blockTo) // otherwise no point in processing it
	    					{
	    						if (blockFrom == bo.getResult() || blockTo == bo.getResult())
	    						{
	    							// intersectingSegments should not contain bo.result
	    							if (intersectingSegments.contains(blockFrom) ||
		    								intersectingSegments.contains(blockTo))
		    						{
		    							intersectingEdges.add(e2);
		    						}
	    						}
	    					}
						}
					}
					
					Collections.sort(intersectingEdges, new BFEdgeScoringComparator(result));
					
					orderedEdges.addAll(0, intersectingEdges);
					resortCount += intersectingEdges.size();
					
					if (debugPrint)
					{
						if (intersectingEdges.size() > 0)
							System.out.println("added " + intersectingEdges.size() + " intersecting edges");
						System.out.println("resortCount is now: " + resortCount);
					}
				}
			}
    		else if (!riverProcessing && bo.getExitStatus() == BlockOperation.SEG_FAILURE_RIVER)
    		{
    			riverEdges.add(e);
    		}
    		
    		if (!riverProcessing && orderedEdges.isEmpty())
    		{
    			if (debugPrint) System.out.println("processing rivers ...");
    			riverProcessing = true; // not currently used, as rules has the value
    			rules.ignoreRivers = true;
    			orderedEdges.addAll(riverEdges);
    		}
    		
    		List<TextFragment> af = new ArrayList<TextFragment>();
    		affectedFragments.add(index, af);
    		if (bo.getExitStatus() == BlockOperation.SEG_SUCCESS)
    		{
    			// TODO: add all blockFrom and To!
    			af.add(e.getNodeFrom());
    			af.add(e.getNodeTo());
    		}
    		else
    		{
    			
    		}
    		
    		if (debugPrint) System.out.println("index was: " + index + "; time for iteration: " 
    				+ (System.currentTimeMillis() - t));
		}
	}
	
	/**
	 * Returns the (hierarchical) merge history for a given block
	 * 
	 * @param b
	 * @return
	 */
	public List<List<TextBlock>> mergeHistoryForBlock(TextBlock b)
	{
		List<List<TextBlock>> retVal = new ArrayList<List<TextBlock>>();
		
		for (int i = segmentationHistory.size() - 1; i >= 0; i --)
		{
			List<TextFragment> af = affectedFragments.get(i);
			
			List<TextFragment> bf = b.fragments();
			
			// list intersection
			// see https://stackoverflow.com/questions/5283047/intersection-and-union-of-arraylists-in-java
//			if (af.retainAll(b.fragments()).size() > 0) O n^2
			if (ListUtils.containsOne(bf, af))
			{
				List<TextBlock> subSegmentation = new ArrayList<TextBlock>();
				
				SegmentationResult r = segmentationHistory.get(i);
				
				for (TextFragment tf : bf)
				{
					subSegmentation.add(r.getBlockMap().get(tf));
				}
				
				ListUtils.removeDuplicates(subSegmentation);
				
				retVal.add(subSegmentation);
			}
		}
		
		return retVal;
	}
	
	/**
	 * After segmentation, runs post-processing to unmerge blocks according to
	 * heuristics
	 * 
	 * @param blocks
	 * @return result
	 */
	public List<TextBlock> postUnmerging(List<TextBlock> blocks)
	{
		List<TextBlock> retVal = new ArrayList<TextBlock>();
		
		// only true/false
		
		/*
		for (CandidateBlock cb : blocks)
		{
			if (rules.verifyBlock(cb) == SegmentationOperation.SEG_SUCCESS)
			{
				retVal.add(cb);
			}
			else
			{
				List<List<CandidateBlock>> mh = mergeHistoryForBlock(cb);
				
				for (List<CandidateBlock> sh : mh)
				{
					boolean valid = true;
					for (CandidateBlock cb2: sh)
					{
						if (rules.verifyBlock(cb) == SegmentationOperation.SEG_FAILURE)
							valid = false;
					}
					
					if (valid)
					{
						retVal.addAll(sh);
						// NB: Should always add final segmentation as it is always valid
						// TODO: check this?
						break;
					}
				}
			}
		}
		*/
		
		// with scores, calculate the maximum
		
//		int count = 0;
		for (TextBlock cb : blocks)
		{
			/*
			if (cb.evalAsParagraph() >= 0.675 && cb.evalAsParagraph() <= 0.685)
			{
				count ++;
				if (count == 4)
				{
					System.out.println(" brown block " + cb);
				}
			}
			*/
			
			List<List<TextBlock>> mh = mergeHistoryForBlock(cb);
			
			float maxEval = cb.evalBlock();
			List<TextBlock> maxResult = new ArrayList<TextBlock>();
			maxResult.add(cb);
			
			for (List<TextBlock> sh : mh)
			{
				int numItems = 0;
//				System.out.println("numItems=" + numItems);
				float thisEval = 1.0f;
				for (TextBlock cb2 : sh)
				{
					numItems ++;
//					System.out.println("cb2.evalBlock=" +  cb2.evalBlock() + " fs=" + cb2.getFontSize() + " b=" + cb2.evalAsBullet() + " p=" + cb2.evalAsParagraph() + " c=" + cb2.evalAsCell());
//					System.out.println(cb2);
					thisEval = thisEval * cb2.evalBlock();
				}
				
				// new: check if there is overlap and decimate
				boolean overlap = false;
				for (TextBlock cb3 : sh)
				{
					for (TextBlock cb4 : sh)
					{
						if (cb3 != cb4)
						{
							if (SegmentUtils.centresIntersect(cb3, cb4))
							{
								overlap = true;
							}
						}
					}
				}
				if (overlap)
				{
					thisEval -= 2.0f;
				}
				
				float countScalingFactor = 0.75f + (0.25f * (1.0f / numItems));
				thisEval = thisEval * countScalingFactor;
				
				if (thisEval > maxEval)
				{
					maxEval = thisEval;
					maxResult.clear();
					maxResult.addAll(sh);
				}
			}
			
			retVal.addAll(maxResult);
		}
		
		return retVal;
	}
	
	/**
	 * Debug method for outputting visual representation
	 * Not currently in use
	 * 
	 * @param blocks
	 * @param pageBounds
	 * @param scale
	 * @return
	 */
	public void addTextToImage(BufferedImage bi, List<TextBlock> blocks, 
			GenericSegment pageBounds, float scale)
	{
		GenericSegment dim = new GenericSegment(
				pageBounds.getX1() * scale, 
				pageBounds.getX2() * scale, 
				pageBounds.getY1() * scale, 
				pageBounds.getY2() * scale
		);
		
		Graphics2D g = bi.createGraphics();
		g.setFont(new Font("Lucida Sans", Font.PLAIN, 12)); 
		g.setColor(Color.red);
		
		for (TextBlock cb : blocks)
		{
			GenericSegment s = new GenericSegment(
					cb.getX1() * scale, 
					cb.getX2() * scale, 
					cb.getY1() * scale, 
					cb.getY2() * scale
			);
			
			DecimalFormat df2 = new DecimalFormat("#.##");
			
			g.drawString(
					
					  "al: " + df2.format(cb.evalAlignment()) 
					+ " \nls: " + df2.format(cb.evalLineSpacing())
					+ " \nol: " + cb.evalOverlappingLines()
					+ " \nws: " + cb.evalWhitespace()
					+ " \nfs: " + cb.getFontSize()
					+ " \ntext: " + cb.getText()
					
					, (int)s.getX1(), (int)dim.getHeight() - (int)s.getY2());
		}
	}
	
	// from https://programming.guide/java/drawing-multiline-strings-with-graphics.html
	/**
	 * Debug method for outputting visual representation
	 * Not currently in use
	 * 
	 */
	void drawString(Graphics g, String text, int x, int y) {
	    int lineHeight = g.getFontMetrics().getHeight();
	    for (String line : text.split("\n"))
	        g.drawString(line, x, y += lineHeight);
	}
	
	/**
	 * Debug method for outputting visual representation
	 * Not currently in use
	 * 
	 * @param blocks
	 * @param pageBounds
	 * @param scale
	 * @return
	 */
	public void addTextToImage2(BufferedImage bi, List<TextBlock> blocks, 
			GenericSegment pageBounds, float scale)
	{
		GenericSegment dim = new GenericSegment(
				pageBounds.getX1() * scale, 
				pageBounds.getX2() * scale, 
				pageBounds.getY1() * scale, 
				pageBounds.getY2() * scale
		);
		
		Graphics2D g = bi.createGraphics();
		g.setFont(new Font("Lucida Sans", Font.PLAIN, 12)); 
		g.setColor(Color.red);
		
		for (TextBlock cb : blocks)
		{
			GenericSegment s = new GenericSegment(
					cb.getX1() * scale, 
					cb.getX2() * scale, 
					cb.getY1() * scale, 
					cb.getY2() * scale
			);
			
			DecimalFormat df2 = new DecimalFormat("#.##");
			
			drawString(g, 
					
					"eval: " + df2.format(cb.evalBlock()) 
					+ " \nal: " + df2.format(cb.evalAlignment()) 
					+ " \nls: " + df2.format(cb.evalLineSpacing())
					+ " \nnl: " + cb.evalNumLines()
					+ " \nws: " + cb.evalWhitespace()
					+ " \nol: " + cb.evalOverlappingLines()
					
					, (int)s.getX1(), (int)dim.getHeight() - (int)s.getY2());
		}
	}
	
	/**
	 * Debug method for colouring blocks when outputting visual representation
	 * Not currently in use
	 * 
	 * @param blocks
	 * @param pageBounds
	 * @param scale
	 * @return
	 */
	public BufferedImage colourBlocks(List<TextBlock> blocks, 
			GenericSegment pageBounds, float scale)
	{
		GenericSegment dim = new GenericSegment(
				pageBounds.getX1() * scale, 
				pageBounds.getX2() * scale, 
				pageBounds.getY1() * scale, 
				pageBounds.getY2() * scale
		);
		
		BufferedImage bi = new BufferedImage((int)dim.getWidth(), (int)dim.getHeight(), 
				BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g2d = bi.createGraphics();
		g2d.setFont(new Font("Lucida Sans", Font.PLAIN, 12)); 
		
		// white background
		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		
		g2d.setColor(Color.black);
				
		for (TextBlock cb : blocks)
		{
			GenericSegment s = new GenericSegment(
					cb.getX1() * scale, 
					cb.getX2() * scale, 
					cb.getY1() * scale, 
					cb.getY2() * scale
			);
			
			int r = (int)(cb.evalAsParagraph() * 255f);
			int g = (int)(cb.evalAsCell() * 255f);
			int b = (int)(cb.evalAsBullet() * 255f);
			
			Color c = new Color(r, g, b);
			g2d.setColor(c);
			
			g2d.fillRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
					(int)s.getWidth(), (int)s.getHeight());
			
			g2d.setColor(Color.black);
			g2d.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
					(int)s.getWidth(), (int)s.getHeight());
			
			DecimalFormat df2 = new DecimalFormat("#.##");
			
			g2d.drawString(
					
					  "b" + df2.format(cb.evalAsBullet()) 
					+ " \np" + df2.format(cb.evalAsParagraph())
					+ " \nco" + df2.format(cb.evalAsCol())
					+ " \nce" + df2.format(cb.evalAsCell())
					
					, (int)s.getX1(), (int)dim.getHeight() - (int)s.getY2());
		}
		
		return bi;
	}
}

// 2019-05-05 conflicts with SegmentationEngine.MergeOp

/*
class MergeOp
{
	TextFragment nodeFrom;
	TextFragment nodeTo;
	TextFragment newSeg;
}
*/
