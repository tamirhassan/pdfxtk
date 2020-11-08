package com.tamirhassan.pdfxtk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tamirhassan.pdfxtk.graph.AdjacencyEdge;
import com.tamirhassan.pdfxtk.model.TextBlock;
import com.tamirhassan.pdfxtk.model.TextFragment;

/**
 * Object to represent state after segmentation has been run
 * on a list of segments and their edges and store which edges were rejected
 * and mapping between new blocks and which segments they contain
 * for quick access
 *
 */
public class SegmentationResult {

	List<AdjacencyEdge<TextFragment>> orderedEdges; // not in use
	List<AdjacencyEdge<TextFragment>> rejectedEdges;
	List<TextBlock> segments;
	HashMap<TextFragment, TextBlock> blockMap;
	
	protected SegmentationResult()
	{
		rejectedEdges = new ArrayList<AdjacencyEdge<TextFragment>>();
		segments = new ArrayList<TextBlock>();
		blockMap = new HashMap<TextFragment, TextBlock>();
	}
	
	protected SegmentationResult(SegmentationResult objToCopy)
	{
		this();
		
		this.rejectedEdges.addAll(objToCopy.rejectedEdges);
		this.blockMap.putAll(objToCopy.blockMap);
		for (TextBlock b : objToCopy.segments)
		{
			TextBlock copyOfB = new TextBlock(b);
			this.segments.add(copyOfB);
			
			// update hashmap value to point to copied object
			for (Map.Entry<TextFragment, TextBlock> entry : this.blockMap.entrySet())
			{
				if (entry.getValue() == b)
					entry.setValue(copyOfB);
			}
		}
	}

	public List<AdjacencyEdge<TextFragment>> getOrderedEdges() {
		return orderedEdges;
	}

	public void setOrderedEdges(List<AdjacencyEdge<TextFragment>> orderedEdges) {
		this.orderedEdges = orderedEdges;
	}

	public List<AdjacencyEdge<TextFragment>> getRejectedEdges() {
		return rejectedEdges;
	}

	public void setRejectedEdges(List<AdjacencyEdge<TextFragment>> rejectedEdges) {
		this.rejectedEdges = rejectedEdges;
	}

	public List<TextBlock> getSegments() {
		return segments;
	}

	public void setSegments(List<TextBlock> segments) {
		this.segments = segments;
	}

	public HashMap<TextFragment, TextBlock> getBlockMap() {
		return blockMap;
	}

	public void setBlockMap(HashMap<TextFragment, TextBlock> blockMap) {
		this.blockMap = blockMap;
	}
}
