package com.tamirhassan.pdfxtk;

import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.TextLine;

/**
 * Return object of segmentation algorithm
 * containing result and exit status
 *
 */
public class LineOperation extends SegmentationOperation<TextLine>
{
	public LineOperation(int exitStatus, TextLine result)
	{
		super(exitStatus, result);
	}
	
	public LineOperation(int exitStatus)
	{
		super(exitStatus);
	}
}