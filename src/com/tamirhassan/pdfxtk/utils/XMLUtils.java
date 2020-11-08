package com.tamirhassan.pdfxtk.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// require xercesImpl.jar
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.tamirhassan.pdfxtk.exceptions.DocumentProcessingException;
import com.tamirhassan.pdfxtk.model.GenericSegment;
import com.tamirhassan.pdfxtk.model.IXHTMLSegment;
import com.tamirhassan.pdfxtk.model.Page;

/**
 * Utility methods for generating the XML output
 *
 */
public class XMLUtils 
{
	/**
     * This is the default encoding of the text to be output.
     */
    public static final String DEFAULT_ENCODING =
        //null;
        //"ISO-8859-1";
        //"ISO-8859-6"; //arabic
        //"US-ASCII";
        "UTF-8";
        //"UTF-16";
        //"UTF-16BE";
        //"UTF-16LE";
    
    /**
     * Generates an XML resultDocument from the processing result passed in theResult
     * 
     * @param theResult - result document in internal model consisting of a list of Pages
     * @param toXHTML - true for XHTML output, false for XMIllum output (no longer in use)
     * @param borders - true if the resulting HTML should make table borders black
     * @return the XML result document
     * @throws DocumentProcessingException
     */
	public static org.w3c.dom.Document processResultToXMLDocument
	(List<Page> theResult, boolean toXHTML, boolean borders)
	throws DocumentProcessingException
	{
	    org.w3c.dom.Document resultDocument;
	    
	    // only used in the case of XHTML
	    Element newBodyElement = null;
	    Element docElement = null;
	    
	    // set up the XML file
	    try
	    {
	        if (toXHTML)
	        {
	            resultDocument = setUpXML("html");
	            docElement = resultDocument.getDocumentElement();
	            if (borders)
	            {
	            	// add borders stuff here
	            	Element newHeadElement = resultDocument.createElement("head");
	            	Element newStyleElement = resultDocument.createElement("style");
	            	newStyleElement.setAttribute("type", "text/css");
	            	Text newTextElement = resultDocument.createTextNode
	        			("table {border-collapse: collapse;}");
	            	Text newTextElement2 = resultDocument.createTextNode
	            		("td, th {border: 1px solid grey; padding: 2px 4px;}");
	            	newStyleElement.appendChild(newTextElement);
	            	newStyleElement.appendChild(newTextElement2);
	            	newHeadElement.appendChild(newStyleElement);
	            	docElement.appendChild(newHeadElement);
	            }
	            newBodyElement = resultDocument.createElement("body");
	        }
	        else
	        {
	            resultDocument = setUpXML("PDFResult");
	            docElement = resultDocument.getDocumentElement();
	        }
	    }
	    catch (ParserConfigurationException e)
	    {
	        throw new DocumentProcessingException(e);
	    }
	    
	    // add the new page element
	    //docElement = resultDocument.getDocumentElement();
	    
	    int pageNo = 0;
	    Iterator resultIter = theResult.iterator();
	    while(resultIter.hasNext())
	    {
	    	GenericSegment gs = (GenericSegment)resultIter.next();
	    	if (gs instanceof Page)
	    	{
	            Page resultPage = (Page)gs;
	            pageNo ++;
	            if (toXHTML)
	            {
	                resultPage.setPageNo(pageNo);
	                resultPage.addAsXHTML(resultDocument, newBodyElement);
	            }
	            else
	            {
	                Element newPageElement = resultDocument.createElement("page");
	                newPageElement.setAttribute("page_number", Integer.toString(pageNo));
	                //we want to use the MediaBox!
	                //resultPage.findBoundingBox();
	                // System.out.println("Result page: " + resultPage);
	                
	                resultPage.addAsXmillum(resultDocument, newPageElement, 
		                	resultPage, Utils.XML_RESOLUTION);
	                
	                docElement.appendChild(newPageElement);
	            }
	    	}
	    	else if (gs instanceof IXHTMLSegment)//(gs.getClass() == Cluster.class || gs.getClass() == strRasterSegment.class)
	    	{
	    		IXHTMLSegment c = (IXHTMLSegment)gs;
	    		if (toXHTML)
	            {
	                c.addAsXHTML(resultDocument, newBodyElement);
	            }
	    		// for XMIllum output, the top-level segment is always a Page
	    	}
	        // run NG on page
	        // output page (cluster-wise) to ontology
	    }
	
	    if (toXHTML)
	        docElement.appendChild(newBodyElement);
	    
	    return resultDocument;
	}
	
	/**
	 * Sets up the XML document structure
	 * 
	 * @param nodeName
	 * @return
	 * @throws ParserConfigurationException
	 */
	protected static org.w3c.dom.Document setUpXML(String nodeName) 
	        throws ParserConfigurationException
    {
        //try
        //{
            DocumentBuilderFactory myFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder myDocBuilder = myFactory.newDocumentBuilder();
            DOMImplementation myDOMImpl = myDocBuilder.getDOMImplementation();
            // resultDocument = myDOMImpl.createDocument("at.ac.tuwien.dbai.pdfwrap", "PDFResult", null);
            org.w3c.dom.Document resultDocument = 
                myDOMImpl.createDocument("at.ac.tuwien.dbai.pdfwrap", nodeName, null);
            return resultDocument;
        //}
        //catch (ParserConfigurationException e)
        //{
         //   e.printStackTrace();
         //   return null;
        //}
        
    }
	
	/**
	 * Returns the contents of the file in a byte array.
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static byte[] getBytesFromFile(File file) throws IOException {
        	InputStream is = new FileInputStream(file);
    
        // Get the size of the file
        long length = file.length();
    
        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
    
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int)length];
    
        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }
    
        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }
    
        // Close the input stream and return bytes
        is.close();
        return bytes;
    }
    
    
    /**
     * Outputs the processing result as XHTML
     *
     * @param pages       - processing result as list of Page objects
     * @param outFilePath - full path of output file, including filename and extension
     * @throws IOException
     */
    public static void outputXHTMLResult(List<Page> pages, String outFilePath)
            throws IOException 
    {
        try 
        {
            Document resultDocument = XMLUtils.processResultToXMLDocument(pages, true, true);

            Writer output = null;

            boolean toConsole = false;
            String encoding = null;
            System.out.println("writing " + outFilePath);
            File outFile = new File(outFilePath);

            if (toConsole) 
            {
                output = new OutputStreamWriter(System.out);
            } 
            else 
            {
                if (encoding != null) 
                {
                    output = new OutputStreamWriter(
                            new FileOutputStream(outFile), encoding);
                } 
                else 
                {
                    //use default encoding
                    output = new OutputStreamWriter(
                            new FileOutputStream(outFile));
                }
            }
            XMLUtils.serializeXML(resultDocument, output);

            if (output != null) 
            {
                output.close();
            }
        } 
        catch (DocumentProcessingException dpe) 
        {
            dpe.printStackTrace();
        }
    }
    
    
    /**
     * Serializes the resultDocument to XML
     * 
     * @param resultDocument
     * @return
     * @throws DocumentProcessingException
     */
    public static byte[] serializeXML(org.w3c.dom.Document resultDocument)
    		throws DocumentProcessingException
    {
        // calls the above and returns a byte[] from the XML Document.
        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        
        try
        {
        	Writer output = new OutputStreamWriter(outStream, DEFAULT_ENCODING);
            serializeXML(resultDocument, output);
        }
        catch (IOException e)
        {
        	throw new DocumentProcessingException(e);
    	}

        return outStream.toByteArray();
    }
    
    /**
     * Serializes the resultDocument to XML
     * 
     * @param resultDocument
     * @param outStream
     * @throws DocumentProcessingException
     */
    public static void serializeXML(org.w3c.dom.Document resultDocument, OutputStream outStream)
    		throws DocumentProcessingException
    {
        try
        {
        	Writer output = new OutputStreamWriter(outStream, DEFAULT_ENCODING);
            serializeXML(resultDocument, output);
        }
        catch (IOException e)
        {
        	throw new DocumentProcessingException(e);
    	}
    }
    
    /**
     * Serializes the resultDocument to XML
     * 
     * @param resultDocument
     * @param output
     * @throws IOException
     */
    public static void serializeXML
        (org.w3c.dom.Document resultDocument, Writer output)
        throws IOException
    {
        // The third parameter in the constructor method for
        // _OutputFormat_ controls whether indenting should be
        // used.  Unfortunately, I have found some bugs in the
        // indenting implementation that have corrupted the text
        // so I have switched it off. 
         
        OutputFormat myOutputFormat =
            new OutputFormat(resultDocument,
                             "UTF-8",
                             true);

        // output used to be replaced with System.out
        XMLSerializer s = 
        new XMLSerializer(output, 
                              myOutputFormat);

        try {
        s.serialize(resultDocument);
        // next line added by THA 21.03.05
        output.flush();
        }
        catch (IOException e) {
            System.err.println("Couldn't serialize document: "+
               e.getMessage());
            throw e;
        }        

         // end of addition
    }
}