/**
 * pdfXtk - PDF Extraction Toolkit
 * Copyright (c) by the authors/contributors.  All rights reserved.
 * This project includes code from PDFBox and TouchGraph.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the names pdfXtk or PDF Extraction Toolkit; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://pdfxtk.sourceforge.net
 *
 */
package com.tamirhassan.pdfxtk.comparators;

import java.util.Comparator;

import com.tamirhassan.pdfxtk.SegmentationEngine;
import com.tamirhassan.pdfxtk.SegmentationResult;
import com.tamirhassan.pdfxtk.graph.AdjacencyEdge;
import com.tamirhassan.pdfxtk.model.TextSegment;

/**
 * @author Tamir Hassan, pdfanalyser@tamirhassan.com
 * @version PDF Analyser 0.9
 * 
 * Sorts the edges according to the ordering value, which prioritizes
 * edges where non-ambiguous decisions can be made
 */
public class BFEdgeScoringComparator implements 
		Comparator<AdjacencyEdge<? extends TextSegment>>
{
	SegmentationResult result;
	
	public BFEdgeScoringComparator(SegmentationResult result)
	{
		this.result = result;
	}
	
	public int compare(AdjacencyEdge<? extends TextSegment> e1, 
		AdjacencyEdge<? extends TextSegment> e2)
	{
		// e2 > e1 => negative result, i.e. e1 comes before e2
		// sort is in ascending order
//			return (int) (e1.physicalLength() - e2.physicalLength());
		
		float score1 = SegmentationEngine.edgeOrderingValue(e1, result);
		float score2 = SegmentationEngine.edgeOrderingValue(e2, result);
		
		// this avoids int rounding errors
		// sort is in descending order; highest scoring edges first
		
		/*
		System.out.println("score1: " + score1);
		System.out.println("score2: " + score2);
		
		if (score2 > score1) System.out.println("returning 1");
		else if (score2 == score1) System.out.println("returning 0");
		else System.out.println("returning -1");
		*/
		
		
//		if (score2 - score1 > 0) return 1;
		if (score2 > score1) return 1;
		else if (score2 == score1) return 0;
		else return -1;
		
		
		/*
		if (score2 > score1) return 1;
		else if (score2 < score1) return -1;
		else return 0;
		*/
	}

	public boolean equals(Object obj)
	{
		return obj.equals(this);
	}
}