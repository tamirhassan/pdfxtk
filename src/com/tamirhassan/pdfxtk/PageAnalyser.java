package com.tamirhassan.pdfxtk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tamirhassan.pdfxtk.comparators.XmidComparator;
import com.tamirhassan.pdfxtk.comparators.Y1Comparator;
import com.tamirhassan.pdfxtk.comparators.Y2Comparator;
import com.tamirhassan.pdfxtk.graph.AdjacencyGraph;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.Page;
import com.tamirhassan.pdfxtk.model.TextBlock;
import com.tamirhassan.pdfxtk.model.TextFragment;
import com.tamirhassan.pdfxtk.utils.ImgOutputUtils;
import com.tamirhassan.pdfxtk.utils.ListUtils;
import com.tamirhassan.pdfxtk.utils.SegmentUtils;

/* modified from CustomPageDrawer.java
 * these portions are subject to the following license:
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Main method to load a document and return a list of paragraphs in XHTML format
 *
 * @author Tamir Hassan
 */
public class PageAnalyser extends DocumentProcessor{

    // 
    // the following constants are used for debugging
    //
    protected final static boolean DEBUG_1 = false;
    protected final static boolean DEBUG_2 = false;
    protected final static boolean DEBUG_3 = false;
    
    protected final static boolean WRITE_IMAGES = false;
    
    protected final static boolean DEBUG_PRINT = false;
    //	protected final static boolean DEBUG_PRINT = false;
    protected final static boolean DEBUG_IMG_FAIL = false;
    protected final static boolean DEBUG_IMG_SH = false;
    //	protected final static boolean DEBUG_IMG_SH = true;
    //	protected final static boolean DEBUG_IMG = true;
    protected final static boolean DEBUG_IMG = false;
    protected final static boolean DEBUG_IMG_TF = false;
    protected final static boolean DEBUG_IMG_TF_FAIL = false;
    //	protected final static boolean DEBUG_IMG_FAIL = true;
    //	protected final static boolean LIMIT_COORDS = true;
    protected final static boolean LIMIT_COORDS = false;

    //	protected final static boolean RESORT = true;
    protected final static boolean RESORT = false;
    //	protected final static boolean CHECK_OVERLAP = false;
    protected final static boolean CHECK_OVERLAP = true;
    	protected final static boolean UNMERGE = false;
//    protected final static boolean UNMERGE = true;

    protected final static boolean DEBUG_IMG_UT = false;
    //	protected final static boolean DEBUG_IMG_UT = false;
    
	public PageAnalyser()
	{
		
	}
	
	public Page customPageProcessing()
	{
		if (OUTPUT_TEXT_IMG)
            ImgOutputUtils.outputPNG(getMtf(),
                    getPageDims(), "intermediate/text-merged-subinstr.png");


        List<TextFragment> mtfCrop = new ArrayList<>();
        for (TextFragment tf : getMtf()) {
            if (LIMIT_COORDS) {
                if (tf.getY1() > 328.0 && tf.getY1() < 686.0)
                    mtfCrop.add(tf);
            } else {
                mtfCrop.add(tf);
            }
        }

        if (mtfCrop.size() == 0) {
            System.err.println("No text found!");

            if (LIMIT_COORDS) {
                System.out.println("LIMIT_COORDS is true!");
                System.exit(0);
            }
        }

        List<TextBlock> unmergedBlocks =
                initialSegmentation(mtfCrop, getPageDims());
        Collections.sort(unmergedBlocks, new Y1Comparator());
        
        
        Page theResult = new Page();
        theResult.getItems().addAll(unmergedBlocks);
        
        return theResult;
	}
	
    /**
     * Initial segmentation, to be carried out before ruled and unruled table detection
     *
     * @param mtfCrop
     * @param dims
     * @return
     */
    protected static List<TextBlock> initialSegmentation
    (List<TextFragment> mtfCrop, GenericSegment dims) 
    {
        AdjacencyGraph<TextFragment> ag = new AdjacencyGraph<>();
        // add all TextFragments (i.e. sub-instructions)
        ag.addList(mtfCrop);

        // TODO: join 

//      ag.setNeighbourRules(AdjacencyGraph.NEIGHBOUR_X_Y_INTERSECT);
//      ag.generateEdgesSingle();

        ag.generateEdges();
        ag.removeEdgesAboveLeft();

        SegmentationEngine se = new SegmentationEngine(ag, new SegmentationRules());
        se.setPageDims(dims);
        se.setDebugPrint(DEBUG_PRINT);
        se.setDebugImg(DEBUG_IMG);
        se.setDebugImgFail(DEBUG_IMG_FAIL);
        se.setResort(RESORT);
        se.setCheckOverlap(CHECK_OVERLAP);

        List<TextBlock> blocks = se.doSegmentation();
        if (WRITE_IMAGES) ImgOutputUtils.outputPNG(blocks, dims, "intermediate/segmentation-initialblocks.png");

        List<TextBlock> retVal;
        if (UNMERGE) 
        {
            List<TextBlock> unmergedBlocks = se.postUnmerging(blocks);

            if (WRITE_IMAGES) 
            {
                createLabelledImage(blocks, se, "intermediate/segmentation-labelled-blocks.jpeg");

                ImgOutputUtils.outputPNG(unmergedBlocks, dims, "intermediate/segmentation-unmerged-blocks.png");
                createLabelledImage(unmergedBlocks, se, "intermediate/segmentation-labelled-unmerged-blocks.jpeg");
            }

            retVal = unmergedBlocks;
        } 
        else 
        {

            retVal = blocks;
        }
        
    	Collections.sort(retVal, new Y1Comparator());
    	for (TextBlock b : retVal)
    		b.setCalculatedFields();
    	return retVal;
    }


    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param gs   - list of CandidateBlocks
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findLeftNeighbour(TextBlock gs, List<? extends GenericSegment> segs) {
        List<TextBlock> blocksAtHeight = new ArrayList<>();

        for (GenericSegment gs2 : segs)
            if (gs2 instanceof TextBlock && SegmentUtils.vertCentresIntersect(gs, gs2))
                blocksAtHeight.add((TextBlock) gs2);

//    	if (blocksAtHeight.contains((gs))) System.out.print("assertion failed: " + (3/0));

        blocksAtHeight.add(gs);
        blocksAtHeight.sort(new XmidComparator());

        int index = blocksAtHeight.indexOf(gs) - 1;
        if (index >= 0 && (index < blocksAtHeight.size()))
            return blocksAtHeight.get(index);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param gs   - list of CandidateBlocks
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findRightNeighbour(TextBlock gs, List<? extends GenericSegment> segs) {
        List<TextBlock> blocksAtHeight = new ArrayList<>();

        for (GenericSegment gs2 : segs)
            if (gs2 instanceof TextBlock && SegmentUtils.vertCentresIntersect(gs, gs2))
                blocksAtHeight.add((TextBlock) gs2);

//    	if (blocksAtHeight.contains((gs))) System.out.print("assertion failed: " + (3/0));

        blocksAtHeight.add(gs);
        blocksAtHeight.sort(new XmidComparator());

        int index = blocksAtHeight.indexOf(gs) + 1;
        if (index > 0 && (index < blocksAtHeight.size())) // == 0 when not found!
            return blocksAtHeight.get(index);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param gs   - list of CandidateBlocks
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findAboveNeighbour(TextBlock gs, List<? extends GenericSegment> segs) {
        List<TextBlock> blocksAtWidth = new ArrayList<>();

        for (GenericSegment gs2 : segs)
            if (gs2 instanceof TextBlock && SegmentUtils.horizCentresIntersect(gs, gs2))
                blocksAtWidth.add((TextBlock) gs2);

//    	if (blocksAtWidth.contains((gs))) System.out.print("assertion failed: " + (3/0));

        blocksAtWidth.add(gs);
        blocksAtWidth.sort(new Y1Comparator()); // sorts top to bottom

        int index = blocksAtWidth.indexOf(gs) - 1;
        if (index >= 0 && (index < blocksAtWidth.size()))
            return blocksAtWidth.get(index);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param gs   - list of CandidateBlocks
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findBelowNeighbour(TextBlock gs, List<? extends GenericSegment> segs) {
        List<TextBlock> blocksAtWidth = new ArrayList<>();

        for (GenericSegment gs2 : segs)
            if (gs2 instanceof TextBlock && SegmentUtils.horizCentresIntersect(gs, gs2))
                blocksAtWidth.add((TextBlock) gs2);

//    	if (blocksAtWidth.contains((gs))) System.out.print("assertion failed: " + (3/0));

        blocksAtWidth.add(gs);
        blocksAtWidth.sort(new Y2Comparator()); // sorts top to bottom

        int index = blocksAtWidth.indexOf(gs) + 1;
        if (index > 0 && (index < blocksAtWidth.size())) // == 0 when not found!
            return blocksAtWidth.get(index);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findLeftmostBlock(List<GenericSegment> segs) {
        List<TextBlock> blocks = new ArrayList<>();
        for (GenericSegment gs : segs)
            if (gs instanceof TextBlock)
                blocks.add((TextBlock) gs);

        blocks.sort(new XmidComparator());

        if (blocks.size() > 0)
            return blocks.get(0);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findRightmostBlock(List<GenericSegment> segs) {
        List<TextBlock> blocks = new ArrayList<>();
        for (GenericSegment gs : segs)
            if (gs instanceof TextBlock)
                blocks.add((TextBlock) gs);

        blocks.sort(new XmidComparator());

        if (blocks.size() > 0)
            return blocks.get(blocks.size() - 1);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findTopmostBlock(List<GenericSegment> segs) {
        List<TextBlock> blocks = new ArrayList<>();
        for (GenericSegment gs : segs)
            if (gs instanceof TextBlock)
                blocks.add((TextBlock) gs);

        blocks.sort(new Y2Comparator());

        if (blocks.size() > 0)
            return blocks.get(0);
        else return null;
    }

    /**
     * Methods for finding neighbouring CandidateBlocks after initial segmentation
     * TODO: These should be replaced by non-static methods and
     * the list should be generated only once for both directions
     *
     * @param segs - list of segments from which the CandidateBlocks were built
     * @return nearest left neighbour or null
     */
    protected static TextBlock findBottommostBlock(List<GenericSegment> segs) {
        List<TextBlock> blocks = new ArrayList<>();
        for (GenericSegment gs : segs)
            if (gs instanceof TextBlock)
                blocks.add((TextBlock) gs);

        blocks.sort(new Y1Comparator());

        if (blocks.size() > 0)
            return blocks.get(blocks.size() - 1);
        else return null;
    }
}