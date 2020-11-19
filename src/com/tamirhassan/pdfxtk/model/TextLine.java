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
package com.tamirhassan.pdfxtk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tamirhassan.pdfxtk.comparators.X1Comparator;
import com.tamirhassan.pdfxtk.model.TextLine;
import com.tamirhassan.pdfxtk.utils.SegmentUtils;
import com.tamirhassan.pdfxtk.utils.Utils;


/**
 * Text line document element
 * contains TextFragments
 * 
 * @author Tamir Hassan, pdfanalyser@tamirhassan.com
 * @version PDF Analyser 0.9
 */
public class TextLine extends CompositeSegment<TextFragment>
{
	public TextLine()
	{
		super();
		this.items = new ArrayList<TextFragment>();
	}
	
	/**
	 * Creates a new line with one fragment
	 * 
	 * @param tf
	 */
	public TextLine(TextFragment tf)
	{
		super();
		this.items.add(tf);
		this.setCalculatedFields(tf);
	}
	
	/**
	 * creates a shallow copy of the line object pointing
	 * to the same text fragment objects
	 * 
	 * @return
	 */
	public TextLine(TextLine lineToCopy)
	{
		super();
		this.items.addAll(lineToCopy.getItems());
		this.setCalculatedFields(lineToCopy);
	}
	
    public TextLine(
        float x1,
        float x2,
        float y1,
        float y2,
        String text,
        String fontName,
        float fontSize
        )
    {
		super(x1, x2, y1, y2, text, fontName, fontSize);
		this.items = new ArrayList<TextFragment>();
    }
    
    public TextLine(
        float x1,
        float x2,
        float y1,
        float y2
        )
    {
		super(x1, x2, y1, y2);
		this.items = new ArrayList<TextFragment>();
    }

    public TextLine(
        float x1,
        float x2,
        float y1,
        float y2,
        String text,
        String fontName,
        float fontSize,
		List<TextFragment> items
        )
    {
    	super(x1, x2, y1, y2, text, fontName, fontSize);
    	this.items = items;
    }
    
    public TextLine(
        float x1,
        float x2,
        float y1,
        float y2,
		List<TextFragment> items
        )
    {
    	super(x1, x2, y1, y2);
    	this.items = items;
    }
    
    public boolean addSegment(TextFragment newSegment)
    {
    	return addSegment(newSegment, false);
    }
    
    public boolean addSegment(TextFragment newSegment, boolean middle)
	{
    	return addSegment(newSegment, middle, false);
	}
    
	// TODO: calculated fields:
    // find fontsize -> if variable then -1; if empty then 0 (constants)
    // find font
    // find average word spacing
    // find average character spacing
    // super/subscript and accidentals no effect
    // object for "multiple font" (no majority font)
    //   e.g. by font change halfway through line
    //   currently only a string - set to "undef" constant;
    //   later encapsulated font object (RTFont containing PDFont)
    // variance measure of baseline (accidentals ignored)
    //
    // TODO: 
    // accidentals affect demerits
    // multiple font object affects demerits
    // wobbly baseline affects demerits
    // wobbly spacing affects demerits
    
    public boolean addSegment(TextFragment newSegment, boolean middle, boolean fontsize)
	{
    	// TODO: maybe undefined fontsize e.g. if there are only 2 items with different sizes???
    	// (although good edge ordering should avoid this!)
    	
    	float retVal = 1.0f;
    	boolean addToRight = true;
    	boolean addSpace = false;
    	
    	System.out.println("in addSegment");
    	
    	if (items.size() == 0)
    	{
    		// nothing to check
    	}
    	else
    	{
        	// check vertical overlap
    		// TODO: verify that this works; better gaussian tailoff
    		if (!SegmentUtils.vertCentresIntersect(this, newSegment))
    			retVal -= 0.6f;
        	
    		// check if baseline/fontsize match (sameLine method)
    		if (fontsize && !Utils.sameFontSize(this, newSegment)) retVal -= 0.2f;
    		if (!Utils.within(this.getY1(), newSegment.getY1(), 0.1f)) retVal -= 0.2f;
    		// TODO: gaussian tailoffs
    		
    		// if not, check if super, subscript or other "accidental"
    		// TODO!
    		
        	// accidental leads to a demerit
        	
    		// check if should be added to left or right
    		// maximum allowed overlap = 0.25 * lineheight
    		
    		float tolerance = this.getHeight() * 0.25f;
    		float distance = newSegment.getX1() - this.getX2();
    		
    		// assume adding to right
    		if (newSegment.getX2() <= this.getX1() + tolerance)
    		{
    			// adding to left
    			addToRight = false;
    			distance = this.getX1() - newSegment.getX2();
    		}
    		else if (newSegment.getX1() < this.getX2() - tolerance)
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
    				retVal -= 0.6f;
    			}
    		}
    		
        	// also check distance - demerit based on gaussian function
    		// can be negative, but only up to 0.25 * lineheight
    		float relDistance = distance / this.getHeight();
    		
    		System.out.println("reld: " + relDistance);
    		
    		if (relDistance > 0.05f)
    		{
    			System.out.println("setting addSpace to true");
    			addSpace = true;
    		}
    		
    		// floor at 0.5
    		float gaussMultiplicator = 0.5f + (0.5f * Utils.normgaussian(relDistance, 0, 2));
    		retVal *= gaussMultiplicator;
    	}
    	
		if (retVal > 0.5)
		{
			if (addToRight)
			{
				if (addSpace)
				{
					items.add(new BlankSpace());	
				}
				items.add(newSegment);
			}
			else
			{
				items.add(0, newSegment);
			}
			
			// TODO: desired for super/subscript?
			this.growBoundingBox(newSegment);
			
			Collections.sort(items, new X1Comparator());
			
			// TODO: deal with font size!
			
			return true;
		}
		else
		{
			return false;
		}
	}
    
    // TODO: penalize lines with small numbers of words (e.g. 1) in later eval method?
    
    public float evalSpacing()
    {
    	// pre: items are sorted
    	
    	float retVal;
    	
    	if (items.size() == 0)
    	{
    		// nothing to check
    		retVal = -1.0f;
    	}
    	else if (items.size() == 1)
    	{
    		retVal = 1.0f;
    	}
    	else
    	{
    		float maxSpace = 0;
    		TextFragment prevFrag = null;
    		for (TextFragment thisFrag : items)
    		{
    			if (prevFrag != null)
    			{
    				float space = thisFrag.getX1() - prevFrag.getX2();
    				
    				if (space > maxSpace)
    					maxSpace = space;
    			}
    			prevFrag = thisFrag;
    		}
    		
    		// also check distance - demerit based on gaussian function
    		// can be negative, but only up to 0.25 * lineheight
    		float relDistance = maxSpace / this.getHeight();
    		
    		// floor at 0.5
    		float gaussMultiplicator = 0.5f + (0.5f * Utils.normgaussian(relDistance, 0, 2));
    		retVal = gaussMultiplicator;
    	}
    	
    	return retVal;
    }
    
    
    /*
    public boolean addSegment(TextFragment newSegment, 
			TextFragment existingSegment)
	{
		// see if it overlaps an existing line (vertically)
		
		// see if, for this overlap, baseline/fontsize match (sameLine method)
		
		// if not, see if super, subscript or other "accidental"
		
		// return true if 
		return false;
	}
	*/
    
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
        	default:
        		type = "error";
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
}
