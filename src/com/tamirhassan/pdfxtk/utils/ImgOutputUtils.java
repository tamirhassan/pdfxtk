package com.tamirhassan.pdfxtk.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import com.tamirhassan.pdfxtk.graph.AdjacencyEdge;
import com.tamirhassan.pdfxtk.graph.AdjacencyGraph;
import com.tamirhassan.pdfxtk.model.CharSegment;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.TextBlock;

public class ImgOutputUtils 
{
	
	final public static boolean DEBUG_TEXT = false;
	
	public static void outputPNG(List<? extends GenericSegment> segments, 
			GenericSegment pageBounds, String filename)
	{
		outputPNG(segments, pageBounds, filename, 1.0f);
	}
	
	
	/**
	 * todo: 
	 * 
	 * @param bi
	 * @param segments
	 * @param pageBounds
	 * @param scale
	 * @param col
	 */
	public static void drawSegments(BufferedImage bi, List<? extends GenericSegment> segments, 
			GenericSegment pageBounds, float scale, Color col)
	{
		Graphics2D g = bi.createGraphics();
		
		GenericSegment dim = new GenericSegment(
				pageBounds.getX1() * scale, 
				pageBounds.getX2() * scale, 
				pageBounds.getY1() * scale, 
				pageBounds.getY2() * scale
		);
		
		for (GenericSegment gs : segments)
		{
			// draw text lines first if CandidateBlock
			/*
			if (gs instanceof CandidateBlock)
			{
				CandidateBlock cb = (CandidateBlock)gs;
				
				for (GenericSegment gs2 : cb.getItems())
				{
					RTTextLine tl = (RTTextLine)gs2;
					
					GenericSegment s2 = new GenericSegment(
							tl.getX1() * scale, 
							tl.getX2() * scale, 
							tl.getY1() * scale, 
							tl.getY2() * scale);
					
					g.setColor(Color.lightGray);
					
					g.drawRect((int)s2.getX1(), (int)dim.getHeight() - (int)s2.getY2(), 
							(int)s2.getWidth(), (int)s2.getHeight());
					
					g.setColor(Color.black);
				}
			}
			*/
			
			GenericSegment s = new GenericSegment(
					gs.getX1() * scale, 
					gs.getX2() * scale, 
					gs.getY1() * scale, 
					gs.getY2() * scale
			);
			
			g.setColor(col);
			
			g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
					(int)s.getWidth(), (int)s.getHeight());
			
			g.setColor(Color.black);
		}
	}
	
	
	public static BufferedImage createImage(List<? extends GenericSegment> segments, 
			GenericSegment pageBounds, float scale)
	{
		long t1 = System.currentTimeMillis();
		
		GenericSegment dim = new GenericSegment(
				pageBounds.getX1() * scale, 
				pageBounds.getX2() * scale, 
				pageBounds.getY1() * scale, 
				pageBounds.getY2() * scale
		);
		
		BufferedImage bi = new BufferedImage((int)dim.getWidth(), (int)dim.getHeight(), 
				BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g = bi.createGraphics();
		
		// white background
		g.setColor(Color.white);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		
		g.setColor(Color.black);
		for (GenericSegment gs : segments)
		{
			GenericSegment s = new GenericSegment(
					gs.getX1() * scale, 
					gs.getX2() * scale, 
					gs.getY1() * scale, 
					gs.getY2() * scale
			);
			
			if (gs instanceof CharSegment)
			{
				CharSegment cs = (CharSegment)gs;
				if (cs.getText() != null && cs.getText().equals(" "))
				{
					g.setColor(Color.red);
					
					g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
							(int)s.getWidth(), (int)s.getHeight());
					
					g.setColor(Color.black);
				}
				else
				{
					g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
							(int)s.getWidth(), (int)s.getHeight());
				}
			}
			else
			{
				g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
						(int)s.getWidth(), (int)s.getHeight());
			}
			
			g.setColor(Color.black);
		}
		
		long t2 = System.currentTimeMillis();
		
		if (DEBUG_TEXT) System.out.println("Time to generate image: " + (t2 - t1));
		
		return bi;
	}
	
	/*
	// TODO: refactor and include as extendable class
	
	public static BufferedImage createImage(
			AdjacencyGraph<? extends GenericSegment> ag, 
			GenericSegment pageBounds, float scale)
	{
		long t1 = System.currentTimeMillis();
		
		GenericSegment dim = new GenericSegment(
				pageBounds.getX1() * scale, 
				pageBounds.getX2() * scale, 
				pageBounds.getY1() * scale, 
				pageBounds.getY2() * scale
		);
		
		BufferedImage bi = new BufferedImage((int)dim.getWidth(), (int)dim.getHeight(), 
				BufferedImage.TYPE_INT_RGB);
		
		Graphics2D g = bi.createGraphics();
		
		// white background
		g.setColor(Color.white);
		g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		
		g.setColor(Color.black);
		for (GenericSegment gs : ag.getVertSegmentList())
		{
			GenericSegment s = new GenericSegment(
					gs.getX1() * scale, 
					gs.getX2() * scale, 
					gs.getY1() * scale, 
					gs.getY2() * scale
			);
			
			if (gs instanceof CharSegment)
			{
				CharSegment cs = (CharSegment)gs;
				if (cs.getText() != null && cs.getText().equals(" "))
				{
					g.setColor(Color.gray);
					
					g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
							(int)s.getWidth(), (int)s.getHeight());
					
					g.setColor(Color.black);
				}
				else
				{
					g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
							(int)s.getWidth(), (int)s.getHeight());
				}
			}
			else
			{
				if (gs instanceof TextBlock)
					g.setColor(Color.blue);
					
				g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
						(int)s.getWidth(), (int)s.getHeight());
			}
			
			g.setColor(Color.black);
		}
		g.setColor(Color.red);
		for (AdjacencyEdge<? extends GenericSegment> e : ag.getEdges())
		{
			GenericSegment s = e.toBoundingSegment();
			g.drawRect((int)s.getX1(), (int)dim.getHeight() - (int)s.getY2(), 
					(int)s.getWidth(), (int)s.getHeight());
		}
		g.setColor(Color.black);
		
		long t2 = System.currentTimeMillis();
		
		if (DEBUG_TEXT) System.out.println("Time to generate image: " + (t2 - t1));
		
		return bi;
	}
	*/
	
	public static void outputPNG(List<? extends GenericSegment> segments, 
			GenericSegment pageBounds, String filename, float scale)
	{
//		Uncomment for JPEG output (faster)
//		String newFilename = filename.replaceFirst("\\.png$", ".jpeg");
//		File outputfile = new File(newFilename);
		File outputfile = new File(filename);
		try 
		{
			long t3 = System.currentTimeMillis();
			ImageIO.write(createImage(segments, pageBounds, scale), "png", outputfile);
			long t4 = System.currentTimeMillis();
			if (DEBUG_TEXT) System.out.println("Time to write image: " + (t4 - t3));
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/*
	public static void outputPNG(AdjacencyGraph<? extends GenericSegment> ag, 
			GenericSegment pageBounds, String filename, float scale)
	{
//		Uncomment for JPEG output (faster)
//		String newFilename = filename.replaceFirst("\\.png$", ".jpeg");
//		File outputfile = new File(newFilename);
		File outputfile = new File(filename);
		try 
		{
			long t3 = System.currentTimeMillis();
			ImageIO.write(createImage(ag, pageBounds, scale), "png", outputfile);
			long t4 = System.currentTimeMillis();
			if (DEBUG_TEXT) System.out.println("Time to write image: " + (t4 - t3));
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
}
