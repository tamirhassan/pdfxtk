package com.tamirhassan.pdfxtk;

import com.tamirhassan.pdfxtk.model.GenericSegment;

/**
 * Object to represent one segmentation operation
 *
 * @param <T> Type of segment that is being segmented/clustered into blocks
 */
public class SegmentationOperation<T extends GenericSegment> {
	
	public final static int SEG_SUCCESS = -1;
	public final static int SEG_FAILURE =  0;
	public final static int SEG_FAILURE_NO_REVISIT =  -100;
	public final static int SEG_FAILURE_RIVER =  -101;
	public final static int SEG_BRANCH  =  2;

	protected int exitStatus;
	protected T result = null;
	
	public SegmentationOperation(int exitStatus, T result)
	{
		this.exitStatus = exitStatus;
		this.result = result;
	}
	
	public SegmentationOperation(int exitStatus)
	{
		this.exitStatus = exitStatus;
	}

	public int getExitStatus() {
		return exitStatus;
	}

	public void setExitStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}

	public T getResult() {
		return result;
	}

	public void setResult(T result) {
		this.result = result;
	}
}
