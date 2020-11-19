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
package com.tamirhassan.pdfxtk.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tamirhassan.pdfxtk.comparators.X1Comparator;
import com.tamirhassan.pdfxtk.comparators.X2Comparator;
import com.tamirhassan.pdfxtk.comparators.Y1Comparator;
import com.tamirhassan.pdfxtk.comparators.Y2Comparator;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.IXHTMLSegment;
import com.tamirhassan.pdfxtk.model.TextSegment;
import com.tamirhassan.pdfxtk.utils.ListUtils;
import com.tamirhassan.pdfxtk.utils.SegmentUtils;
import com.tamirhassan.pdfxtk.utils.Utils;

/**
 * AdjacencyGraph -- the neighbourhood graph
 * 
 * @author Tamir Hassan, tamir@roundtrippdf.com
 * @version RT PDF 0.9
 */
public class AdjacencyGraph<T extends GenericSegment>// extends DocumentGraph 
{   
//	protected List<T> nodes;
	protected List<AdjacencyEdge<T>> edges;
	
	/*
	protected List<T> neighboursLeft;
	protected List<T> neighboursRight;
	protected List<T> neighboursAbove;
	protected List<T> neighboursBelow;
	*/
	
	// temporary for edge generation only
	protected List<T> horiz;
	protected List<T> vert;
	
	protected X1Comparator xComp = new X1Comparator();
	protected Y1Comparator yComp = new Y1Comparator();

	// 2011-11-17 does not appear to be in use!
//	protected HashMap<T, List<AdjacencyEdge<T>>> edgesFrom;
//	protected HashMap<T, List<AdjacencyEdge<T>>> edgesTo;
	
	public final static int NEIGHBOUR_INTERSECTING_X_Y_MID = 0;
	public final static int NEIGHBOUR_X_Y_INTERSECT = 1;
	
	protected int neighbourRules = 1;
	
	/**
     * Constructor.
     *
     * @param to fill in! todo!
     */
    public AdjacencyGraph()
    // initialize a blank neighbourhood graph
    {
//    	nodes = new ArrayList<T>();
    	edges = new ArrayList<AdjacencyEdge<T>>();
    	
    	horiz = new ArrayList<T>();
    	vert = new ArrayList<T>();
    	
//    	2011-01-27 TODO: implement HashMap
//    	edgesFrom = new HashMap<T, List<AdjacencyEdge<T>>>();
//    	edgesTo = new HashMap<T, List<AdjacencyEdge<T>>>();
    }
    
    /*
    public List<T> getNodes() {
		return nodes;
	}

	public void setNodes(List<T> nodes) {
		this.nodes = nodes;
	}
	*/
    
    /**
     * creates a new graph object with shallow copies
     * of horiz, vert and edge lists
     * 
     * @return
     */
    public AdjacencyGraph<T> duplicate()
    {
    	AdjacencyGraph<T> retVal = new AdjacencyGraph<T>();
    	retVal.getHorizSegmentList().addAll(this.getHorizSegmentList());
    	retVal.getVertSegmentList().addAll(this.getVertSegmentList());
    	retVal.getEdges().addAll(this.getEdges());
    	
    	return retVal;
    }
    
	public void addList(List<? extends T> nodes)
	{
		horiz.addAll(nodes);
		vert.addAll(nodes);
	}
	
	public List<AdjacencyEdge<T>> getEdges() {
		return edges;
	}

	public void setEdges(List<AdjacencyEdge<T>> edges) {
		this.edges = edges;
	}
	
	public List<AdjacencyEdge<T>> horizEdges() {
		List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
		for (AdjacencyEdge<T> ae : edges)
			if (ae.isHorizontal()) retVal.add(ae);
		return retVal;
	}
	
	public List<AdjacencyEdge<T>> vertEdges() {
		List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
		for (AdjacencyEdge<T> ae : edges)
			if (ae.isVertical()) retVal.add(ae);
		return retVal;
	}

	public List<T> getHorizSegmentList() {
		return horiz;
	}

	public List<T> getVertSegmentList() {
		return vert;
	}

	public int getNeighbourRules() {
		return neighbourRules;
	}

	public void setNeighbourRules(int neighbourRules) {
		this.neighbourRules = neighbourRules;
	}

	/**
	 * returns the length of the common horizontal edge
	 * of two segments
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	protected float yEdgeIntersect(T seg1, T seg2)
	{
		
		if (seg1.getY1() >= seg2.getY1() && seg1.getY1() <= seg2.getY2())
		{
			// seg1.getY1() lies within bounds of seg2
			// intersection between seg1.getY1() and min(seg1.getY2(), seg2.getY2())
			return Utils.minimum(seg1.getY2(), seg2.getY2()) - seg1.getY1();
		}
		else if (seg2.getY1() >= seg1.getY1() && seg2.getY1() <= seg1.getY2())
		{
			// seg2.getY1() lies within bounds of seg1
			// intersection between seg2.getX1() and min(seg1.getX2(), seg2.getX2())
			return Utils.minimum(seg1.getY2(), seg2.getY2()) - seg2.getY1();
		}
		
		return 0;
	}
	
	/**
	 * returns the length of the common vertical edge
	 * of two segments
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	protected float xEdgeIntersect(T seg1, T seg2)
	{
		// seg1.getX1() lies within bounds of seg2
		//    ###      ####  seg1
		//  #######  ####    seg2
		if (seg1.getX1() >= seg2.getX1() && seg1.getX1() <= seg2.getX2())
		{
			// intersection between seg1.getX1() and min(seg1.getX2(), seg2.getX2())
			return Utils.minimum(seg1.getX2(), seg2.getX2()) - seg1.getX1();
		}
		// seg2.getX1() lies within bounds of seg1
		//	#######  ####    seg1
		//    ###      ####  seg2
		else if (seg2.getX1() >= seg1.getX1() && seg2.getX1() <= seg1.getX2())
		{
			// intersection between seg2.getX1() and min(seg1.getX2(), seg2.getX2())
			return Utils.minimum(seg1.getX2(), seg2.getX2()) - seg2.getX1();
		}
				
		return 0;
	}
	
	/**
	 * returns first neighbour below thisSegment according to
	 * straight-line distance
	 * 
	 * @param thisSegment
	 * @return found neighbour or null if no neighbour found within line of sight
	 */
	protected T findNeighbourBelow(T thisSegment)
    {
//		T retVal = null;
//		float currSLD = -1;
	    
	    // returns -1 if the given node is not in the list
	    // TODO: return an exception in this case
	    int index = vert.indexOf(thisSegment);
	    
	    // look for neighbours below, return the first or null!
	    // list is sorted in Y1 (baseline) order
        for (int n = index + 1; n < vert.size(); n ++)
        {
        	T o = vert.get(n);
        	
//        	float vertDist = thisSegment.getY1() - o.getY2();
            
        	// use line-of-sight definition
    		boolean isNeighbour = SegmentUtils.horizIntersect(o, thisSegment);
    		
        	if (neighbourRules == NEIGHBOUR_INTERSECTING_X_Y_MID)
        	{
        		isNeighbour = SegmentUtils.horizIntersect(o, thisSegment) 
                		&& (SegmentUtils.horizIntersect(thisSegment, o.getXmid()) 
                    	|| SegmentUtils.horizIntersect(o, thisSegment.getXmid()));
        	}
            
            if (isNeighbour)
            {
            	// calculate straight line distance from horizontal centres
            	// of vertical edges
            	
            	float horizDist = thisSegment.getXmid() - o.getXmid();
            	if (horizDist < 0) horizDist = o.getXmid() - thisSegment.getXmid();
            	
            	// extra check for "touching" segments
            	if (SegmentUtils.vertIntersect(o, thisSegment))
        		{
            		/*
        			float vertMidDist = thisSegment.getYmid() - o.getYmid();
        			if (horizDist > vertMidDist) continue;
        			*/
            		
            		if (yEdgeIntersect(o, thisSegment) > xEdgeIntersect(o, thisSegment))
            			continue;
        		}
            	
            	return o;
            	
            	/*
            	float sld = (float) Math.sqrt((Math.pow(vertDist, 2) + Math.pow(horizDist, 2))); 
            	
            	// assign retVal or update
            	// if sld is shorter than that of previous retVal
            	
            	if (retVal == null || sld < currSLD)
            	{
            		retVal = o;
            		currSLD = sld;
            	}
            	*/
            }

            // check whether to exit loop
            /*
            if (retVal != null && vertDist > currSLD)
            {
            	return retVal;
            }
            */
        }
//      return retVal;
        return null;
    }
	
	/**
	 * returns first neighbour above thisSegment according to
	 * straight-line distance
	 * 
	 * @param thisSegment
	 * @return found neighbour or null if no neighbour found within line of sight
	 */
	protected T findNeighbourAbove(T thisSegment)
    {
//		T retVal = null;
//		float currSLD = -1;
	    
	    // returns -1 if the given node is not in the list
	    // TODO: return an exception in this case
	    int index = vert.indexOf(thisSegment);
	    
	    // look for neighbours above, return the first or null!
	    // list is sorted in Y1 (baseline) order
	    for (int n = index - 1; n >= 0; n --)
        {
        	T o = vert.get(n);
        	
//        	float vertDist = o.getY1() - thisSegment.getY2();
            
        	 // use line-of-sight definition
        	boolean isNeighbour = SegmentUtils.horizIntersect(o, thisSegment);
        	
        	if (neighbourRules == NEIGHBOUR_INTERSECTING_X_Y_MID)
        	{
        		isNeighbour = SegmentUtils.horizIntersect(o, thisSegment) 
		        		&& (SegmentUtils.horizIntersect(thisSegment, o.getXmid()) 
		            	|| SegmentUtils.horizIntersect(o, thisSegment.getXmid()));
        	}
            
            if (isNeighbour)
            {
            	// calculate straight line distance from horizontal centres
            	// of vertical edges
            	
            	float horizDist = thisSegment.getXmid() - o.getXmid();
            	if (horizDist < 0) horizDist = o.getXmid() - thisSegment.getXmid();
            	
            	// extra check for "touching" segments
            	if (SegmentUtils.vertIntersect(o, thisSegment))
        		{
            		/*
        			float vertMidDist = o.getYmid() - thisSegment.getYmid();
        			if (horizDist > vertMidDist) continue;
        			*/
            		
        			if (yEdgeIntersect(o, thisSegment) > xEdgeIntersect(o, thisSegment))
            			continue;
        		}
            	
            	return o;
            	
            	/*
            	float sld = (float) Math.sqrt((Math.pow(vertDist, 2) + Math.pow(horizDist, 2))); 
            	
            	// assign retVal or update
            	// if sld is shorter than that of previous retVal
            	if (retVal == null || sld < currSLD)
            	{
            		retVal = o;
            		currSLD = sld;
            	}
            	*/
            }

            // check whether to exit loop
            /*
            if (retVal != null && vertDist > currSLD)
            {
            	return retVal;
            }
            */
        }
//      return retVal;
	    return null;
    }
	
    /**
	 * returns first neighbour left of thisSegment according to
	 * straight-line distance
	 * 
	 * @param thisSegment
	 * @return found neighbour or null if no neighbour found within line of sight
	 */
	protected T findNeighbourLeft(T thisSegment)
    {
//		T retVal = null;
//		float currSLD = -1;
	    
	    // returns -1 if the given node is not in the list
	    // TODO: return an exception in this case
	    int index = horiz.indexOf(thisSegment);
	    
	    // look for neighbours above, return the first or null!
	    // list is sorted in X1 order
	    for (int n = index - 1; n >= 0; n --)
        {
        	T o = horiz.get(n);
        	
//        	float horizDist = thisSegment.getX2() - o.getX1();
            
            // use line-of-sight definition
        	boolean isNeighbour = SegmentUtils.vertIntersect(o, thisSegment);
        	
        	if (neighbourRules == NEIGHBOUR_INTERSECTING_X_Y_MID)
        	{
        		isNeighbour = SegmentUtils.vertIntersect(o, thisSegment) 
		        		&& (SegmentUtils.vertIntersect(thisSegment, o.getXmid()) 
		            	|| SegmentUtils.vertIntersect(o, thisSegment.getXmid()));
        	}
            
            if (isNeighbour)
            {
            	// calculate straight line distance from horizontal centres
            	// of vertical edges
            	
            	float vertDist = thisSegment.getYmid() - o.getYmid();
            	if (vertDist < 0) vertDist = o.getYmid() - thisSegment.getYmid();
            	
            	// extra check for "touching" segments
            	if (SegmentUtils.horizIntersect(o, thisSegment))
        		{
            		/*
        			float horizMidDist = thisSegment.getXmid() - o.getXmid();
        			if (vertDist > horizMidDist) continue;
        			*/
            		
            		if (xEdgeIntersect(o, thisSegment) > yEdgeIntersect(o, thisSegment))
            			continue;
        		}
            	
            	return o;
            	
            	/*
            	float sld = (float) Math.sqrt((Math.pow(vertDist, 2) + Math.pow(horizDist, 2))); 
            	
            	// assign retVal or update
            	// if sld is shorter than that of previous retVal
            	if (retVal == null || sld < currSLD)
            	{
            		retVal = o;
            		currSLD = sld;
            	}
            	*/
            }

            // check whether to exit loop
            /*
            if (retVal != null && horizDist > currSLD)
            {
            	return retVal;
            }
            */
        }
//      return retVal;
        return null;
    }
    
    /**
	 * returns first neighbour left of thisSegment according to
	 * straight-line distance
	 * 
	 * @param thisSegment
	 * @return found neighbour or null if no neighbour found within line of sight
	 */
	protected T findNeighbourRight(T thisSegment)
    {
//		T retVal = null;
//		float currSLD = -1;
	    
	    // returns -1 if the given node is not in the list
	    // TODO: return an exception in this case
	    int index = horiz.indexOf(thisSegment);
	    
	    // look for neighbours above, return the first or null!
	    // list is sorted in X1 order
	    for (int n = index + 1; n < horiz.size(); n ++)
        {
        	T o = horiz.get(n);
        	
//        	float horizDist = o.getX1() - thisSegment.getX2();
            
        	// use line-of-sight definition
        	boolean isNeighbour = SegmentUtils.vertIntersect(o, thisSegment);
        	
        	if (neighbourRules == NEIGHBOUR_INTERSECTING_X_Y_MID)
        	{
            	isNeighbour = SegmentUtils.vertIntersect(o, thisSegment) 
		        		&& (SegmentUtils.vertIntersect(thisSegment, o.getXmid()) 
		            	|| SegmentUtils.vertIntersect(o, thisSegment.getXmid()));
        	}
        	
            if (isNeighbour)
            {
            	// calculate straight line distance from horizontal centres
            	// of vertical edges
            	
            	float vertDist = thisSegment.getYmid() - o.getYmid();
            	if (vertDist < 0) vertDist = o.getYmid() - thisSegment.getYmid();
            	
            	// extra check for "touching" segments
            	if (SegmentUtils.horizIntersect(o, thisSegment))
        		{
            		/*
        			float horizMidDist = o.getXmid() - thisSegment.getXmid();
        			if (vertDist > horizMidDist) continue;
        			*/
            		
            		if (xEdgeIntersect(o, thisSegment) > yEdgeIntersect(o, thisSegment))
            			continue;
        		}
            	
            	return o;
            	
            	/*
            	float sld = (float) Math.sqrt((Math.pow(vertDist, 2) + Math.pow(horizDist, 2))); 
            	
            	// assign retVal or update
            	// if sld is shorter than that of previous retVal
            	if (retVal == null || sld < currSLD)
            	{
            		retVal = o;
            		currSLD = sld;
            	}
            	*/
            }

            // check whether to exit loop
            /*
            if (retVal != null && horizDist > currSLD)
            {
            	return retVal;
            }
            */
        }
//      return retVal;
        return null;
    }
    
    public void generateEdges()
    {
    	edges.clear();
    	
    	// TODO: clear hash maps too
    	
    	Collections.sort(horiz, new X2Comparator());
    	Collections.sort(vert, new Y2Comparator());
    	
        for (T thisBlock: vert)
        {
        	if (((TextSegment)thisBlock).getText().equals("L"))
        	{
//        		System.out.println("test");
        	}
        	
        	T neighbourLeft = findNeighbourLeft(thisBlock);
        	if (neighbourLeft != null)
        	{
        		edges.add(new AdjacencyEdge<T>(thisBlock, neighbourLeft, AdjacencyEdge.REL_LEFT));
                edges.add(new AdjacencyEdge<T>(neighbourLeft, thisBlock, AdjacencyEdge.REL_RIGHT));
        	}
        	
        	T neighbourAbove = findNeighbourAbove(thisBlock);
        	if (neighbourAbove != null)
        	{
        		edges.add(new AdjacencyEdge<T>(thisBlock, neighbourAbove, AdjacencyEdge.REL_ABOVE));
                edges.add(new AdjacencyEdge<T>(neighbourAbove, thisBlock, AdjacencyEdge.REL_BELOW));
        	}
        }
        
    	Collections.sort(horiz, new X1Comparator());
    	Collections.sort(vert, new Y1Comparator());
    	
    	// TODO: clear hash maps too
    	
        for (T thisBlock: vert)
        {
        	if (((TextSegment)thisBlock).getText().equals("L"))
        	{
//        		System.out.println("test");
        	}
        	
        	T neighbourRight = findNeighbourRight(thisBlock);
        	if (neighbourRight != null)
        	{
        		edges.add(new AdjacencyEdge<T>(thisBlock, neighbourRight, AdjacencyEdge.REL_RIGHT));
                edges.add(new AdjacencyEdge<T>(neighbourRight, thisBlock, AdjacencyEdge.REL_LEFT));
        	}
        	
        	T neighbourBelow = findNeighbourBelow(thisBlock);
        	if (neighbourBelow != null)
        	{
        		edges.add(new AdjacencyEdge<T>(thisBlock, neighbourBelow, AdjacencyEdge.REL_BELOW));
                edges.add(new AdjacencyEdge<T>(neighbourBelow, thisBlock, AdjacencyEdge.REL_ABOVE));
        	}
        }
        
        generateComplementaryEdges();
        removeDuplicateEdges();
        
        // horiz and vert lists remain sorted in X1 and Y1 (baseline)
        // order respectively
    }
    
    public void generateComplementaryEdges()
    {
    	List<AdjacencyEdge<T>> edgesToAdd = 
    		new ArrayList<AdjacencyEdge<T>>();
    	
    	for (AdjacencyEdge<T> e : edges)
    	{
    		int newDirection = -1;
    		if (e.getDirection() == AdjacencyEdge.REL_ABOVE)
    			newDirection = AdjacencyEdge.REL_BELOW;
    		else if (e.getDirection() == AdjacencyEdge.REL_BELOW)
    			newDirection = AdjacencyEdge.REL_ABOVE;
    		else if (e.getDirection() == AdjacencyEdge.REL_LEFT)
    			newDirection = AdjacencyEdge.REL_RIGHT;
    		else if (e.getDirection() == AdjacencyEdge.REL_RIGHT)
    			newDirection = AdjacencyEdge.REL_LEFT;
    		
    		edgesToAdd.add(new AdjacencyEdge<T>
    			(e.nodeTo, e.nodeFrom, newDirection));
    	}
    	edges.addAll(edgesToAdd);
    	
    	removeDuplicateEdges();
    }
    
    // TODO: to speed this up (and other operations...)
    // implement a hash map lookup for edges in the DocumentGraph method
    // or, rather EdgeList?
    /**
     * Removes extra instances of an edge between two
     * given nodes (even if they are distinct objects)
     * 
     * TODO: move to ListUtils?
     */
    protected void removeDuplicateEdges()
    {
    	List<AdjacencyEdge<T>> edgesToRemove = 
    		new ArrayList<AdjacencyEdge<T>>();
    	
    	for (AdjacencyEdge<T> e1 : edges)
    	{
			for (AdjacencyEdge<T> e2 : edges)
			{
				if (e1.getDirection() == e2.getDirection() &&
					e1.getNodeFrom() == e2.getNodeFrom() &&
					e1.getNodeTo() == e2.getNodeTo() &&
					e1 != e2 && !edgesToRemove.contains(e1))
				{
					edgesToRemove.add(e2);
				}
			}
    	}
    	edges.removeAll(edgesToRemove);
    }
    
    public void removeEdgesAboveLeft()
    {
    	List<AdjacencyEdge<? extends GenericSegment>> edgesToRemove =
			new ArrayList<AdjacencyEdge<? extends GenericSegment>>();
		
		for (AdjacencyEdge<? extends GenericSegment> ag : edges)
			if (ag.getDirection() == AdjacencyEdge.REL_LEFT ||
				ag.getDirection() == AdjacencyEdge.REL_ABOVE)
				edgesToRemove.add(ag);
		
		edges.removeAll(edgesToRemove);
    }
    
    public List<AdjacencyEdge<T>> edgesForSegment(T seg)
    {
    	List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
    	
    	for (AdjacencyEdge<T> e : edges)
    	{
    		if (e.getNodeFrom() == seg | e.getNodeTo() == seg)
    			retVal.add(e);
    	}
    	
    	return retVal;
    }
    
    /*
     here, we only seem look at nodes pointing FROM
     the given segment in the relevant direction --
     the other edges are duplicated anyway ...
     
     this way, we should end up with each neighbouring
     item once ...
     */
    
    public List<T> returnNeighboursLeft(T thisSeg)
    {
        List<T> retVal = new ArrayList<T>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_LEFT &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e.getNodeTo());
        }
        return retVal;
    }
    
    public List<T> returnNeighboursRight(GenericSegment thisSeg)
    {
    	List<T> retVal = new ArrayList<T>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_RIGHT &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e.getNodeTo());
        }
        return retVal;
    }
    
    public List<T> returnNeighboursAbove(GenericSegment thisSeg)
    {
    	List<T> retVal = new ArrayList<T>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_ABOVE &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e.getNodeTo());
        }
        return retVal;
    }
    
    public List<T> returnNeighboursBelow(GenericSegment thisSeg)
    {
    	List<T> retVal = new ArrayList<T>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_BELOW &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e.getNodeTo());
        }
        return retVal;
    }
    
    public List<AdjacencyEdge<T>> returnEdgesLeft(T thisSeg)
    {
        List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_LEFT &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e);
        }
        return retVal;
    }
    
    public List<AdjacencyEdge<T>> returnEdgesRight(T thisSeg)
    {
        List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_RIGHT &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e);
        }
        return retVal;
    }
    
    public List<AdjacencyEdge<T>> returnEdgesAbove(T thisSeg)
    {
        List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_ABOVE &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e);
        }
        return retVal;
    }
    
    public List<AdjacencyEdge<T>> returnEdgesBelow(T thisSeg)
    {
        List<AdjacencyEdge<T>> retVal = new ArrayList<AdjacencyEdge<T>>();
        for (AdjacencyEdge<T> e : edges)
        {
        	if (e.getDirection() == AdjacencyEdge.REL_BELOW &&
        		e.getNodeFrom() == thisSeg)
        		retVal.add(e);
        }
        return retVal;
    }
    
    public AdjacencyGraph<T> generateSubgraph(List<T> segments)
    {
    	AdjacencyGraph<T> retVal = new AdjacencyGraph<T>();
    	for (AdjacencyEdge<T> ae : edges)
    		if (segments.contains(ae.getNodeFrom()) && 
    			segments.contains(ae.getNodeTo()))
    			retVal.edges.add(ae);
    	for (T seg : horiz)
    		if (segments.contains(seg)) retVal.horiz.add(seg);
    	for (T seg : vert)
    		if (segments.contains(seg)) retVal.vert.add(seg);
    	
    	return retVal;
    }
    
    /**
     * deprecated - used by clusterXThreshold
     * 
     * @param node1
     * @param node2
     * @param newSegment
     */
    public void mergeNodes(T node1, T node2, T newSegment)
    {
    	// list of all edges pointing to or from merged segment
    	List<AdjacencyEdge<T>> newEdges = edgesForSegment(node1);
    	newEdges.addAll(edgesForSegment(node2));
    	ListUtils.removeDuplicates(newEdges);
    	
    	// remove edges between the two segments
    	List<AdjacencyEdge<T>> edgesToRemove = new ArrayList<AdjacencyEdge<T>>();
    	for (AdjacencyEdge<T> e : newEdges)
    	{
    		if (e.getNodeFrom() == node1 && e.getNodeTo() == node2 ||
    				e.getNodeFrom() == node2 && e.getNodeTo() == node1)
    		{
//    			newEdges.remove(e);
    			edgesToRemove.add(e);
    			edges.remove(e);
    		}
    		else if (e.getNodeFrom() == node1 || e.getNodeFrom() == node2)
    		{
    			e.setNodeFrom(newSegment);
    		}
    		else if (e.getNodeTo() == node1 || e.getNodeTo() == node2)
    		{
    			e.setNodeTo(newSegment);
    		}
    	}
    	
    	newEdges.removeAll(edgesToRemove);
    	horiz.remove(node1);
    	horiz.remove(node2);
    	orderedAdd(newSegment);
    }
    
    /**
     * removed - no more generateEdgesSingle method
     * 
     * @param node
     * @param newSeg1
     * @param newSeg2
     */
    /*
    public void splitNodes(T node, T newSeg1, T newSeg2)
    {
    	// remove node
    	horiz.remove(node);
    	vert.remove(node);
    	// remove all edges to/from node
    	edges.removeAll(edgesForSegment(node));
    	
    	orderedAdd(newSeg1);
    	orderedAdd(newSeg2);
    	
    	generateEdgesSingle(newSeg1);
    	generateEdgesSingle(newSeg2);
    	removeDuplicateEdges();
    }
    */
    
    /**
     * deprecated - used by mergeNodes
     * 
     * @param element
     */
    // adapted from https://stackoverflow.com/questions/18144820/inserting-into-sorted-linkedlist-java#19614083
    public void orderedAdd(T element) 
    {      
    	{
	    	ListIterator<T> itr = horiz.listIterator();
	        boolean loop = true;
	    	while(loop) {
	            if (itr.hasNext() == false) {
	                itr.add(element);
	                loop = false;
	                break;
	            }
	
	            T elementInList = itr.next();
	            if (xComp.compare(elementInList,element) > 0) {
	                itr.previous();
	                itr.add(element);
	                System.out.println("Adding to horiz");
	                loop = false;
	                break;
	            }
	        }
    	}
    	
    	{
	    	ListIterator<T> itr = vert.listIterator();
	        boolean loop = true;
	    	while(loop) {
	            if (itr.hasNext() == false) {
	                itr.add(element);
	                loop = false;
	                break;
	            }
	
	            T elementInList = itr.next();
	            if (yComp.compare(elementInList,element) > 0) {
	                itr.previous();
	                itr.add(element);
	                System.out.println("Adding to vert");
	                loop = false;
	                break;
	            }
	        }
    	}
    }
    
    public String toString()
    {
        StringBuffer retVal = new StringBuffer("");
        for (T seg : vert)
        {
            retVal.append(seg.toString() + "\n");
            List<T> neighboursLeft = returnNeighboursLeft(seg);
            List<T> neighboursRight = returnNeighboursRight(seg);
            List<T> neighboursAbove = returnNeighboursAbove(seg);
            List<T> neighboursBelow = returnNeighboursBelow(seg);
            retVal.append("     Neighbours left: " + neighboursLeft.size() +
                " right: " + neighboursRight.size() +
                " above: " + neighboursAbove.size() +
                " below: " + neighboursBelow.size() + "\n");
        }
        return retVal.toString();
    }
    
    /**
     * adds page graph as XML
     * 
     * @param resultDocument
     * @param parent
     * @param pageDim
     * @param resolution
     * @param pageNo if >=0 will be included as attribute
     */
    public void addAsXML(Document resultDocument, Element parent, 
    		GenericSegment pageDim, float resolution, int pageNo)
    {
    	Element newPageElement = resultDocument.createElement("page-graph");
    	if (pageNo >= 0)
    		newPageElement.setAttribute("page-no", Integer.toString(pageNo));
    		
    	// number vert nodes 0-based consec.
    	List<T> vertCopy = new ArrayList<T>();
    	vertCopy.addAll(vert);
    	Collections.sort(vertCopy, new Y1Comparator());
    	
    	int index = 0;
    	for (T node : vertCopy)
    	{
    		node.addAsXML(resultDocument, newPageElement, pageDim, resolution, index);
    		index ++;
    	}
    	
    	// TODO: sort edges horiz first?
        // add edges
        for (AdjacencyEdge<T> e : edges)
        {
        	String elementName = "vert-edge";
        	if (e.isHorizontal()) elementName = "horiz-edge";
        	
        	Element newEdgeElement = resultDocument.createElement(elementName);
        	newEdgeElement.setAttribute("node-from", Integer.toString(vertCopy.indexOf(e.getNodeFrom()))); 
        	newEdgeElement.setAttribute("node-to", Integer.toString(vertCopy.indexOf(e.getNodeTo()))); 
        	
        	newPageElement.appendChild(newEdgeElement);
        }
        
        // output other relations
        
//        newPageElement.appendChild
//            (resultDocument.createTextNode("Page " + pageNo));
        
        parent.appendChild(newPageElement);
        

    }
}
