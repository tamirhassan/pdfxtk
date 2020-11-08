package com.tamirhassan.pdfxtk;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import com.tamirhassan.pdfxtk.model.CharSegment;
import com.tamirhassan.pdfxtk.model.CompositeSegment;
import com.tamirhassan.pdfxtk.model.Page;
import com.tamirhassan.pdfxtk.model.TextFragment;
import com.tamirhassan.pdfxtk.model.TextSegment;
import com.tamirhassan.pdfxtk.utils.Utils;

// TODO: add sourcOps!

/**
 * This class extends the PDFBox stream engine to create the respective objects
 * in our model/representation when the various PDF operators are processed when
 * reading in the PDF content stream
 * 
 * @author tam
 *
 */
public class CustomStreamEngine extends PDFGraphicsStreamEngine
{
	final static boolean DEBUG_PRINT = false;
	
	// contains CompositeSegments of different sub-types
	protected List<CompositeSegment<? extends TextSegment>> instrList = 
			new ArrayList<CompositeSegment<? extends TextSegment>>();
	
//	TODO: block that begins with BT and ends with ET - might be useful?
//	protected CompositeSegment<CompositeSegment<TextFragment>> block = null;
	
	// either a TextFragment (Tj) or a CompositeSegment<TF> (TJ)
	protected CompositeSegment<? extends TextSegment> instr = null; 
	
	// relevant only when processing a TJ operator
	protected TextFragment subinstr = null;
	
	// true when processing a TJ operator; false for Tj
	boolean addToSubinstr = false;
	
	/**
	 * @return Returns generated list of found characters after processing
	 */
	public List<CharSegment> getChars()
	{
		List<CharSegment> retVal = new ArrayList<CharSegment>();
		for (CompositeSegment<? extends TextSegment> instr : instrList)
		{
			if (instr instanceof TextFragment)
			{
				retVal.addAll(((TextFragment) instr).getItems());
			}
			else if (instr instanceof CompositeSegment<?>)
			{
				CompositeSegment<TextFragment> cs = 
						(CompositeSegment<TextFragment>) instr;
				for (TextFragment frag : cs.getItems())
				{
					retVal.addAll(frag.getItems());
				}
			}
		}
		return retVal;
	}
	
	/**
	 * @return Returns generated list of found text fragments after processing
	 */
	public List<TextFragment> getTextFragments()
	{
		List<TextFragment> retVal = 
				new ArrayList<TextFragment>();
		for (CompositeSegment<? extends TextSegment> instr : instrList)
		{
			if (instr instanceof TextFragment)
			{
				retVal.add((TextFragment) instr);
			}
			else if (instr instanceof CompositeSegment<?>)
			{
				CompositeSegment<TextFragment> cs = 
						(CompositeSegment<TextFragment>) instr;
				retVal.addAll(cs.getItems());
			}
		}
		return retVal;
	}
	
	// 2018-10-15
	// adapted from private method in LineProcessor
	// TODO: move to Utils
	/**
	 * Returns true if prevBlock and thisBlock are close enough to be part of the same line of text
	 * 
	 * @param prevBlock
	 * @param thisBlock
	 * @param maxX - X threshold
	 * @param maxY - Y threshold
	 * @return
	 */
	protected static boolean sameLine(TextSegment prevBlock, TextSegment thisBlock, 
			float maxX, float maxY)
	{
//		System.out.println("\n\nin sameLine with prev: " + prevBlock);
//		System.out.println("in sameLine with this: " + thisBlock);
		
		// going backwards (currently assuming LtoR)
		if (thisBlock.getX1() < prevBlock.getXmid()) return false;
		
//		System.out.print("one ");
		
		if (!Utils.sameFontSize(prevBlock, thisBlock)) return false;
		
//		System.out.print("two ");
		
		float afs = (thisBlock.getFontSize() + prevBlock.getFontSize()) * 0.5f;
		float xDist = thisBlock.getX1() - prevBlock.getX2();
		float xDistRel = xDist / afs;
		
		if (xDistRel > maxX) return false;
		
//		System.out.print("three ");
		
		// baseline calculation here; do not account for 
		// super-/subscript at this stage
		float yDist = thisBlock.getY1() - prevBlock.getY1();
		// take magnitude
		if (yDist < 0) yDist = 0 - yDist;
		
		if (yDist > maxY) return false;
		
//		System.out.print("TRUE ");
		
		return true;
		
	}
	
	
	/*
	 * looks at characters in sequence
	 * TODO: TextFragment contains type TextSegment - change to char?
	 * 
	 */
	/**
	 * Looks at characters in sequence, returning generated list
	 * containing chars as well as list of codes (levels) added to list passed
	 * 
	 * @param threshold - does not seem to be used
	 * @param levels - empty list to pass
	 * @return
	 */
	public List<CharSegment> generateCharList
			(float threshold, List<Integer> levels)
	{
		List<CharSegment> retVal = 
				new ArrayList<CharSegment>();
		
		// avoid crashing if no levels passed
		if (levels == null) 
			levels = new ArrayList<Integer>();
		
		for (CompositeSegment<? extends TextSegment> instr : instrList)
		{
			// Tj instruction
			if (instr instanceof TextFragment)
			{
				TextFragment tf = (TextFragment)instr;
				int index = -1;
				for (TextSegment ts : instr.getItems())
				{
					index ++;
					
					// always contains characters
					CharSegment cs = (CharSegment)ts;
					
					if (index == 0) // first
						levels.add(11);
					else
						levels.add(10);
					
					retVal.add(cs);
				}
			}
			// TJ instruction
			else if (instr instanceof CompositeSegment<?>)
			{
				// hier!
				
				CompositeSegment<TextFragment> cts = 
						(CompositeSegment<TextFragment>) instr;
				
				// operand
				int operandIndex = -1;
				for (TextFragment tf : cts.getItems())
				{
					operandIndex ++;
					int index = -1;
					for (TextSegment ts : tf.getItems())
					{
						index ++;
						
						// always contains characters
						CharSegment cs = (CharSegment)ts;
						
						if (index == 0)
							if (operandIndex == 0)
								levels.add(22); // new instruction
							else
								levels.add(21); // new operand
						else
							levels.add(20); // new character
						
						retVal.add(cs);
					}
				}
			}
		}
		
		return retVal;
	}
	
	/**
	 * As getMergedFragmentsFromChars but runs trim too
	 * Not currently in use
	 * 
	 * @param threshold
	 * @return
	 */
	public List<TextFragment> getMergedFragmentsFromCharsTrim(float threshold)
	{
		List<TextFragment> retVal = new ArrayList<TextFragment>();
				
		List<TextFragment> frags = getMergedFragmentsFromChars(threshold, false);
		
		for (TextFragment tf : frags)
		{
			int startIndex = 0;
			int endIndex = tf.getItems().size() - 1;
			
			for (int i = 0; i < tf.getItems().size(); i ++)
			{
				startIndex = i;
				CharSegment cs = tf.getItems().get(i);
				if (cs.getText() != null && !cs.getText().equals(" "))
					break;
			}
			
			for (int i = tf.getItems().size() - 1; i >= -1; i --)
			{
				// allow setting to -1 to enable zero-length
				// to deal with empty fragments
				endIndex = i;
				if (i >= 0)
				{
					CharSegment cs = tf.getItems().get(i);
					if (cs.getText() != null && !cs.getText().equals(" "))
						break;
				}
			}
			
			if (endIndex < (startIndex - 1))
			{
				// bug! should not happen
			}
			else if (endIndex == (startIndex - 1))
			{
				// fragment was completely blank; remove
			}
			else if (startIndex != 0 || endIndex != tf.getItems().size() - 1)
			{
				// rebuild fragment
				TextFragment newFrag = new TextFragment();
				newFrag.setCalculatedFields(tf);
				
				for (int i = startIndex; i <= endIndex; i ++)
					newFrag.getItems().add(tf.getItems().get(i));
				
				newFrag.findBoundingBox();
				newFrag.findText();
				retVal.add(newFrag);
			}
			else
			{
				// add fragment without changing
				retVal.add(tf);
			}
		}
		
		return retVal;
	}
	
	
	/**
	 * As getMergedFragmentsFromChars but runs trim too
	 * TODO: break apart if more than three spaces
	 * Not currently in use
	 * 
	 * @param threshold
	 * @return
	 */
	public List<TextFragment> getMergedFragmentsFromCharsTrimRemoveSpaces(float threshold)
	{
		int maxGap = 1;
		
		List<TextFragment> retVal = new ArrayList<TextFragment>();
				
		List<TextFragment> frags = getMergedFragmentsFromChars(threshold, false);
		
		for (TextFragment tf : frags)
		{
			int gapWidth = 0;
			int startIndex = -1;
			int endIndex = -1;
			
			for (int i = 0; i < tf.getItems().size(); i ++)
			{
				CharSegment cs = tf.getItems().get(i);
				
				if (cs.getText() != null && !cs.getText().equals(" "))
				{
					if (startIndex == -1)
						startIndex = i;
					
					// increment endIndex
					endIndex = i;
				}
				else
				{
					if (startIndex >= 0)
					{
						gapWidth += 1;
								
						if (gapWidth > maxGap)
						{
							// add new item to retVal
							TextFragment newFrag = new TextFragment();
							// sets font, etc. BBox and text recalculated later
							newFrag.setCalculatedFields(tf);
							
							for (int j = startIndex; j <= endIndex; j ++)
								newFrag.getItems().add(tf.getItems().get(j));
							
							newFrag.findBoundingBox();
							newFrag.findText();
							retVal.add(newFrag);
							
							startIndex = -1;
							endIndex = -1;
							gapWidth = 0;
						}
					}
				}
			}
			
			if (startIndex >= 0) 
			{
				// endIndex should be set to last non-space char
				
				TextFragment newFrag = new TextFragment();
				// sets font, etc. BBox and text recalculated later
				newFrag.setCalculatedFields(tf);
				
				for (int j = startIndex; j <= endIndex; j ++)
					newFrag.getItems().add(tf.getItems().get(j));
				
				newFrag.findBoundingBox();
				newFrag.findText();
				retVal.add(newFrag);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Goes through the chars in sequence and merges them into TextFragments
	 * if they are close enough (word level). This improves processing speed of later
	 * steps if text in the document is written in its reading order
	 * 
	 * @param threshold
	 * @param removeSpaces
	 * @return
	 */
	public List<TextFragment> getMergedFragmentsFromChars(float threshold, 
			boolean removeSpaces)
	{
		List<TextFragment> retVal = 
				new ArrayList<TextFragment>();
		
		// not used ... yet
		List<Integer> levels = new ArrayList<Integer>();
		List<CharSegment> chars = generateCharList(threshold, levels);
				
		CharSegment prevChar = null;
		TextFragment mergedFrag = new TextFragment();
		for (CharSegment thisChar : chars)
		{
			if (DEBUG_PRINT) System.out.println ("thisChar.getText: " + thisChar.getText());
			// TODO: handle spaces!
			if (!removeSpaces || thisChar.getText() == null || !thisChar.getText().equals(" "))
			{
				if (prevChar == null)
				{
					// add first fragment
					mergedFrag.getItems().add(thisChar);
				}
				else
				{
					if (sameLine(prevChar, thisChar, threshold, 0.1f))
					{
						mergedFrag.getItems().add(thisChar);
					}
					else
					{
						mergedFrag.setCalculatedFields();
						retVal.add(mergedFrag);
						
						mergedFrag = new TextFragment();
						mergedFrag.getItems().add(thisChar);
					}
				}
				prevChar = thisChar;
			}
		}
		
		// add last fragment
		if (!retVal.contains(mergedFrag))
		{
			mergedFrag.setCalculatedFields();
			retVal.add(mergedFrag);
		}


		return retVal;
	}
	
	/**
	 * looks at text instructions in sequence
	 * Similar to getMergedFragmentsFromChars but works on the instruction level instead
	 * Not currently in use
	 * 
	 * 
	 */
	public List<TextFragment> getMergedTextFragments(float threshold)
	{
		List<TextFragment> retVal = 
				new ArrayList<TextFragment>();
		for (CompositeSegment<? extends TextSegment> instr : instrList)
		{
//			System.out.println("processing instruction: " + instr);
			if (instr instanceof TextFragment)
			{
				// assume that all subinstructions always
				// belong together
				retVal.add((TextFragment) instr);
			}
			else if (instr instanceof CompositeSegment<?>)
			{
				// hier!
				
				CompositeSegment<TextFragment> cs = 
						(CompositeSegment<TextFragment>) instr;
				
				TextFragment prevFrag = null;
				TextFragment mergedFrag = new TextFragment();
				for (TextFragment frag : cs.getItems())
				{
					if (prevFrag == null)
					{
						// add first fragment
						mergedFrag.getItems().addAll(frag.getItems());
					}
					else
					{
						if (sameLine(prevFrag, frag, threshold, 0.1f))
						{
							mergedFrag.getItems().addAll(frag.getItems());
						}
						else
						{
							mergedFrag.setCalculatedFields();
							retVal.add(mergedFrag);
							
							mergedFrag = new TextFragment();
							mergedFrag.getItems().addAll(frag.getItems());
						}
					}
					prevFrag = frag;
				}
				
				if (!retVal.contains(mergedFrag))
				{
					mergedFrag.setCalculatedFields();
					retVal.add(mergedFrag);
				}

//				retVal.addAll(cs.getItems());
			}
		}
		return retVal;
	}
	
	/**
	 * looks across neighbouring instructions too
	 * 
	 * Similar to getMergedFragmentsFromChars but works on the instruction level instead
	 * Not currently in use
	 * 
	 * @param threshold
	 * @return
	 */
	public List<TextFragment> getMergedTextFragments2(float threshold)
	{
		List<TextFragment> mergedFrags = getMergedTextFragments(threshold);
		List<TextFragment> retVal = new ArrayList<TextFragment>();
		
		TextFragment prevFrag = null;
		TextFragment mergedFrag = new TextFragment();
		for (TextFragment frag : mergedFrags)
		{
			if (prevFrag == null)
			{
				// add first fragment
				mergedFrag.getItems().addAll(frag.getItems());
			}
			else
			{
				if (sameLine(prevFrag, frag, threshold, 0.1f))
				{
					mergedFrag.getItems().addAll(frag.getItems());
				}
				else
				{
					mergedFrag.setCalculatedFields();
					retVal.add(mergedFrag);
					
					mergedFrag = new TextFragment();
					mergedFrag.getItems().addAll(frag.getItems());
				}
			}
			prevFrag = frag;
		}

		if (!retVal.contains(mergedFrag))
		{
			mergedFrag.setCalculatedFields();
			retVal.add(mergedFrag);
		}
		
		return retVal;
	}
	
	/**
	 * @return Generate list containing text at instruction (operator) level
	 */
	public List<CompositeSegment<? extends TextSegment>> getInstructions()
	{
		List<CompositeSegment<? extends TextSegment>> retVal = 
				new ArrayList<CompositeSegment<? extends TextSegment>>();
		retVal.addAll(instrList); // duplicates the list
		return retVal;
	}
	
	/**
	 * 
	 * @return Page dimensions in empty Page object, cropped to MediaBox
	 */
	public Page getPageDimensions()
	{
		PDRectangle crop = getPage().getMediaBox();
		
		return new Page(crop.getLowerLeftX(), crop.getUpperRightX(),
				crop.getLowerLeftY(), crop.getUpperRightY());
	}

	/**
	 * Constructor
	 * 
	 * @param PDPage to process
	 */
	protected CustomStreamEngine(PDPage page) 
    {
		super(page);
		// TODO Auto-generated constructor stub
	}

	private GeneralPath linePath = new GeneralPath();
        
	
	/**
     * Glyph bounding boxes.
     */
    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode,
                             Vector displacement) throws IOException
    {
        // draw glyph
        super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);

//        System.out.println("showed glyph: " + unicode + " with width: " + font.getWidth(code));
        
        // bbox in EM -> user units
        Shape bbox = new Rectangle2D.Float(0, 0, font.getWidth(code) / 1000, 0.75f);
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        bbox = at.createTransformedShape(bbox);
        
        CharSegment cs = new CharSegment((float)bbox.getBounds2D().getX(), 
        		(float)bbox.getBounds2D().getX() + (float)bbox.getBounds2D().getWidth(), 
        		(float)bbox.getBounds2D().getY(), 
        		(float)bbox.getBounds2D().getY() + (float)bbox.getBounds2D().getHeight());
        
        cs.setText(unicode);
        cs.setFontName(font.getName());
        cs.setFontSize(textRenderingMatrix.getScaleY());
        
        if (addToSubinstr)
        	subinstr.getItems().add(cs);
        else
        	((TextFragment) instr).getItems().add(cs);
        
        /*
        // bbox in EM -> user units
        Shape bbox = new Rectangle2D.Float(0, 0, font.getWidth(code) / 1000, 1);
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        bbox = at.createTransformedShape(bbox);
        
        // save
        Graphics2D graphics = getGraphics();
        Color color = graphics.getColor();
        Stroke stroke = graphics.getStroke();
        Shape clip = graphics.getClip();

        // draw
        graphics.setClip(graphics.getDeviceConfiguration().getBounds());
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(.5f));
        graphics.draw(bbox);

        // restore
        graphics.setStroke(stroke);
        graphics.setColor(color);
        graphics.setClip(clip);
        */
    }
    
    
    /**
     * Returns the current line path. This is reset to empty after each fill/stroke.
     */
    protected final GeneralPath getLinePath()
    {
        return linePath;
    }
    
    /**
     * Filled path bounding boxes.
     */
    @Override
    public void fillPath(int windingRule) throws IOException
    {
        // bbox in user units
//        Shape bbox = getLinePath().getBounds2D();
    	Rectangle2D bbox = getLinePath().getBounds2D();
        
        /*
        // draw path (note that getLinePath() is now reset)
        super.fillPath(windingRule);
        
        // save
        Graphics2D graphics = getGraphics();
        Color color = graphics.getColor();
        Stroke stroke = graphics.getStroke();
        Shape clip = graphics.getClip();

        // draw
        graphics.setClip(graphics.getDeviceConfiguration().getBounds());
        graphics.setColor(Color.GREEN);
        graphics.setStroke(new BasicStroke(.5f));
        graphics.draw(bbox);

        // restore
        graphics.setStroke(stroke);
        graphics.setColor(color);
        graphics.setClip(clip);
        */
    }
    
    
    /**
     * Custom annotation rendering.
     */
    @Override
    public void showAnnotation(PDAnnotation annotation) throws IOException
    {
        // save
        saveGraphicsState();
        
        // 35% alpha
        getGraphicsState().setNonStrokeAlphaConstant(0.35);
        super.showAnnotation(annotation);
        
        // restore
        restoreGraphicsState();
    }
    
	@Override
	public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawImage(PDImage pdImage) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clip(int windingRule) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveTo(float x, float y) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lineTo(float x, float y) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Point2D getCurrentPoint() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void closePath() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endPath() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void strokePath() throws IOException {
		// TODO Auto-generated method stub
		
	}

	/*
	@Override
	public void fillPath(int windingRule) throws IOException {
		// TODO Auto-generated method stub
		
	}
	*/
	
	@Override
	public void fillAndStrokePath(int windingRule) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shadingFill(COSName shadingName) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	   /**
     * Called when the BT operator is encountered. This method is for overriding in subclasses, the
     * default implementation does nothing.
     *
     * @throws IOException if there was an error processing the text
     */
    public void beginText() throws IOException
    {
        // overridden in subclasses
    }

    /**
     * Called when the ET operator is encountered. This method is for overriding in subclasses, the
     * default implementation does nothing.
     *
     * @throws IOException if there was an error processing the text
     */
    public void endText() throws IOException
    {
        // overridden in subclasses
    }


    /**
     * Called when a string of text with spacing adjustments is to be shown.
     *
     * @param array array of encoded text strings and adjustments
     * @throws IOException if there was an error showing the text
     */
    public void showTextStrings(COSArray array) throws IOException
    {
//    	System.out.println("showing text strings: " + array);
    	
    	// change temporarily for this operation
    	addToSubinstr = true;
    	
    	instr = new CompositeSegment<TextFragment>();
    	subinstr = new TextFragment();
    	
    	super.showTextStrings(array);
    	
    	if (subinstr.getItems().size() > 0)
    	{
    		subinstr.findBoundingBox();
    		subinstr.findText();
    		((CompositeSegment<TextFragment>)instr).getItems().add(subinstr);
    	}
    	
    	instr.findBoundingBox();
    	instr.findText();
    	instrList.add(instr);
    	
//    	System.out.println("added instr: " + instr);
    	
    	addToSubinstr = false;
    }


    /**
     * Applies a text position adjustment from the TJ operator. May be overridden in subclasses.
     *
     * @param tx x-translation
     * @param ty y-translation
     * 
     * @throws IOException if something went wrong
     */
    protected void applyTextAdjustment(float tx, float ty) throws IOException
    {
        // update the text matrix
    	super.applyTextAdjustment(tx, ty);
//    	System.out.println("applied adjustment tx: " + tx);
    	
//    	System.out.println(subinstr);
    	
        // break subinstruction segment
    	if (subinstr.getItems().size() > 0)
    	{
	    	subinstr.findBoundingBox();
	    	subinstr.findText();
	    	((CompositeSegment<TextFragment>)instr).getItems().add(subinstr);
    	}
    	subinstr = new TextFragment();
    }
    
    
    /**
     * Process text from the PDF Stream. You should override this method if you want to
     * perform an action when encoded text is being processed.
     *
     * @param string the encoded text
     * @throws IOException if there is an error processing the string
     */
    protected void showText(byte[] string) throws IOException
    {
//    	is also called by showTextStrings!
//    	addToSubinstr = false;
    	
    	if (!addToSubinstr)
    	{
    		instr = new TextFragment();
    	}
    	
    	super.showText(string);
    	
    	if (!addToSubinstr)
    	{
	    	instr.findBoundingBox();
	    	instr.findText();
	    	instrList.add(instr);
    	}
    }

}

