package com.tamirhassan.pdfxtk;

import com.tamirhassan.pdfxtk.model.TextBlock;

/**
 * Return object of segmentation algorithm
 * containing result and exit status
 *
 */
public class BlockOperation extends SegmentationOperation<TextBlock>
{
	public BlockOperation(int exitStatus, TextBlock result)
	{
		super(exitStatus, result);
	}
	
	public BlockOperation(int exitStatus)
	{
		super(exitStatus);
	}
}