package com.tamirhassan.pdfxtk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.tamirhassan.pdfxtk.exceptions.DocumentProcessingException;
import com.tamirhassan.pdfxtk.graph.AdjacencyEdge;
import com.tamirhassan.pdfxtk.graph.AdjacencyGraph;
import com.tamirhassan.pdfxtk.model.CharSegment;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.Page;
import com.tamirhassan.pdfxtk.model.TextFragment;
import com.tamirhassan.pdfxtk.model.TextLine;
import com.tamirhassan.pdfxtk.utils.ImgOutputUtils;
import com.tamirhassan.pdfxtk.utils.ListUtils;
import com.tamirhassan.pdfxtk.utils.SegmentUtils;
import com.tamirhassan.pdfxtk.utils.Utils;
import com.tamirhassan.pdfxtk.utils.XMLUtils;

public class DocumentProcessor {

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

	private PDDocument PDDoc;  // required for e.g. rendering
	private List<TextFragment> mtf;
	private List<CharSegment> chars;
	private GenericSegment pageDims;
	private int currPageNo;
	
	// for debugging
	protected final static boolean OUTPUT_TEXT_IMG = false;
	
    private final static int PAGE_NO = -1;

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
	 * extend this method
	 */
	protected Page customPageProcessing()
    {
    	return new Page();
    }
    
    public void processDocument() throws DocumentProcessingException
	{
		System.out.println("Using input file: " + getInFile());
        System.out.println("Using output file: " + getOutFile());

        // load the input file
        File inputFile = new File(getInFile());

        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

//	    	File file = openFile();

        try {
	        PDDoc = PDDocument.load(inputFile);

	        int pageFrom = PAGE_NO;
	        int pageTo = PAGE_NO;
	        if (PAGE_NO <= 0) {
	            pageFrom = 1;
	            pageTo = PDDoc.getNumberOfPages();
	        }
	        List<Page> pages = new ArrayList<>();
	
	        for (int page = pageFrom; page <= pageTo; page++) {
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
	        
	        XMLUtils.outputXHTMLResult(pages, getOutFile());
	        PDDoc.close();
	        
        } catch (IOException e) {
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

	private String getInFile() {
		return inFile;
	}

	public void setInFile(String inFile) {
		this.inFile = inFile;
	}

	private String getOutFile() {
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
    
}