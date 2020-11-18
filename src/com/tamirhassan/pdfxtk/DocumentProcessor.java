package com.tamirhassan.pdfxtk;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import com.tamirhassan.pdfxtk.exceptions.DocumentProcessingException;
import com.tamirhassan.pdfxtk.model.CharSegment;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.Page;
import com.tamirhassan.pdfxtk.model.TextBlock;
import com.tamirhassan.pdfxtk.model.TextFragment;
import com.tamirhassan.pdfxtk.utils.ImgOutputUtils;
import com.tamirhassan.pdfxtk.utils.XMLUtils;

public class DocumentProcessor 
{
    // This is the default encoding of the text to be output.    
    private static final String DEFAULT_ENCODING =
									            //null;
									            //"ISO-8859-1";
									            //"ISO-8859-6"; //arabic
									            //"US-ASCII";
									            "UTF-8";
											    //"UTF-16";
											    //"UTF-16BE";
											    //"UTF-16LE";

    protected boolean toConsole = false;
    protected boolean toXHTML = true;
    protected boolean borders = true;
    protected boolean rulingLines = true;
    protected boolean processSpaces = false;
    protected boolean outputStages = false;
    protected String password = "";
    protected String encoding = DEFAULT_ENCODING;
    protected String inFile = null;
    protected String outFile = null;
	protected int startPage = 1;
	protected int endPage = Integer.MAX_VALUE;

	protected PDDocument PDDoc;  // required for e.g. rendering
	protected List<TextFragment> mtf;
	protected List<CharSegment> chars;
	protected GenericSegment pageDims;
	protected int currPageNo;
	
	// for debugging
	protected final static boolean OUTPUT_TEXT_IMG = false;
	
    /**
     * Empty constructor
     * 
     */
    protected DocumentProcessor()
    {
    	
    }
    
    protected PDDocument getPDDoc() {
		return PDDoc;
	}

	public void setPDDoc(PDDocument pDDoc) {
		PDDoc = pDDoc;
	}

	/**
	 * @return list of merged text fragments
	 */
	protected List<TextFragment> getMtf() {
		return mtf;
	}

	public void setMtf(List<TextFragment> mtf) {
		this.mtf = mtf;
	}

	public List<CharSegment> getChars() {
		return chars;
	}

	public void setChars(List<CharSegment> chars) {
		this.chars = chars;
	}

	protected GenericSegment getPageDims() {
		return pageDims;
	}

	public void setPageDims(GenericSegment pageDims) {
		this.pageDims = pageDims;
	}

	protected int getCurrPageNo() {
		return currPageNo;
	}

	public void setCurrPageNo(int currPageNo) {
		this.currPageNo = currPageNo;
	}

	/**
	 * simplest method to simply return the text in the order it was written to the pdf.
	 * extend this method
	 */
	protected Page customPageProcessing()
    {
        Page theResult = new Page();
        for (TextFragment tf : getMtf()) 
        	theResult.getItems().add(new TextBlock(tf));
    	return theResult;
    }
    
    public void processDocument() throws DocumentProcessingException
	{
		System.out.println("Using input file: " + getInFile());
        System.out.println("Using output file: " + getOutFile());

        // load the input file
        File inputFile = new File(getInFile());

        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

//	    	File file = openFile();

        try 
        {
	        PDDoc = PDDocument.load(inputFile);
	        
	        int docStartPage = 1;
	        int docEndPage = PDDoc.getNumberOfPages();

	        List<Page> pages = new ArrayList<>();
	
	        for (int page = docStartPage; page <= docEndPage; page ++) {
	        	if (page >= startPage && page <= endPage)
	        	{
		        	System.out.println("Processing page: " + page);
		        	currPageNo = page;
		            CustomStreamEngine cse = new CustomStreamEngine(PDDoc.getPage(page - 1)); // or null?
		
		            cse.processPage(PDDoc.getPage(page - 1));
		
		            if (OUTPUT_TEXT_IMG) {
		                ImgOutputUtils.outputPNG(cse.getChars(), cse.getPageDimensions(), "intermediate/text-chars.png");
		                ImgOutputUtils.outputPNG(cse.getInstructions(), cse.getPageDimensions(),
		                        "intermediate/text-instr.png");
		                ImgOutputUtils.outputPNG(cse.getTextFragments(), cse.getPageDimensions(),
		                        "intermediate/text-subinstr.png");
		            }
		            
		            // Initial segmentation
	
	//			        List<TextFragment> mtf = cse.getMergedFragmentsFromCharsTrim(0.3f);
	//			        List<TextFragment> mtf = cse.getMergedFragmentsFromCharsTrimRemoveSpaces(0.3f);
		            mtf = cse.getMergedFragmentsFromChars(0.6f, true);
		            chars = cse.getChars();
		            pageDims = cse.getPageDimensions();
		
	                Page theResult = customPageProcessing();
	                
	                // TODO: Add items to page
	                pages.add(theResult);
	        	}
	        } 
	        
	        XMLUtils.outputXHTMLResult(pages, getOutFile());
	        PDDoc.close();
	        
        } 
        catch (IOException e) 
        {
            throw new DocumentProcessingException(e);
        }
	}



	public boolean isToConsole() {
		return toConsole;
	}

	public void setToConsole(boolean toConsole) {
		this.toConsole = toConsole;
	}

	public boolean isToXHTML() {
		return toXHTML;
	}

	public void setToXHTML(boolean toXHTML) {
		this.toXHTML = toXHTML;
	}

	public boolean isBorders() {
		return borders;
	}

	public void setBorders(boolean borders) {
		this.borders = borders;
	}

	public boolean isRulingLines() {
		return rulingLines;
	}

	public void setRulingLines(boolean rulingLines) {
		this.rulingLines = rulingLines;
	}

	public boolean isProcessSpaces() {
		return processSpaces;
	}

	public void setProcessSpaces(boolean processSpaces) {
		this.processSpaces = processSpaces;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	protected String getInFile() {
		return inFile;
	}

	public void setInFile(String inFile) {
		this.inFile = inFile;
	}

	protected String getOutFile() {
		return outFile;
	}

	public void setOutFile(String outFile) {
		this.outFile = outFile;
	}

	public int getStartPage() {
		return startPage;
	}

	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}

	public int getEndPage() {
		return endPage;
	}

	public void setEndPage(int endPage) {
		this.endPage = endPage;
	}
    
    /**
     * Creates and outputs a labelled image for debugging purposes.
     * Does not appear to be in use at the moment.
     *
     * @param blocks
     * @param se
     * @param imgFilename
     */
    protected static void createLabelledImage
    (List<TextBlock> blocks, SegmentationEngine se, String imgFilename) {
//		BufferedImage bi =
//        	TableUtils.createImage
//        	(blocks, se.getPageDims(), 4.0f);

//		se.addTextToImage2(bi, blocks, se.getPageDims(), 4.0f);
        BufferedImage bi = se.colourBlocks(blocks, se.getPageDims(), 4.0f);

//		String imgFilename = "segmentation-labelled-blocks.jpeg";

        File outputfile = new File(imgFilename);
        try {
            ImageIO.write(bi, "jpeg", outputfile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Example PDFRenderer subclass, uses MyPageDrawer for custom rendering.
     */
    protected static class MyPDFRenderer extends PDFRenderer {
        MyPDFRenderer(PDDocument document) {
            super(document);
        }

        @Override
        protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
            return new MyPageDrawer(parameters);
        }
    }

    /**
     * Example PageDrawer subclass with custom rendering.
     */
    protected static class MyPageDrawer extends PageDrawer {
        MyPageDrawer(PageDrawerParameters parameters) throws IOException {
            super(parameters);
        }

        /**
         * Color replacement.
         */
        @Override
        protected Paint getPaint(PDColor color) throws IOException {
            // if this is the non-stroking color
            if (getGraphicsState().getNonStrokingColor() == color) {
                // find red, ignoring alpha channel
                if (color.toRGB() == (Color.RED.getRGB() & 0x00FFFFFF)) {
                    // replace it with blue
                    return Color.BLUE;
                }
            }
            return super.getPaint(color);
        }

        /**
         * Glyph bounding boxes.
         */
        @Override
        protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode,
                                 Vector displacement) throws IOException {
            // draw glyph
            super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);

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
        }

        /**
         * Filled path bounding boxes.
         */
        @Override
        public void fillPath(int windingRule) throws IOException {
            // bbox in user units
            Shape bbox = getLinePath().getBounds2D();

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
        }

        /**
         * Custom annotation rendering.
         */
        @Override
        public void showAnnotation(PDAnnotation annotation) throws IOException {
            // save
            saveGraphicsState();

            // 35% alpha
            getGraphicsState().setNonStrokeAlphaConstant(0.35);
            super.showAnnotation(annotation);

            // restore
            restoreGraphicsState();
        }
    }
}