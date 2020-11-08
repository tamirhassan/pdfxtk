package com.tamirhassan.pdfxtk;

public class CLI {

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
    
	public static final String PASSWORD = "-password";
    public static final String ENCODING = "-encoding";
    public static final String CONSOLE = "-console";
    public static final String START_PAGE = "-startPage";
    public static final String END_PAGE = "-endPage";
    
	/**
     * Infamous main method.
     *
     * @param args Command line arguments, should be one and a reference to a file.
     *
     * @throws Exception If there is an error parsing the document.
     */
    public static void main(String[] args)
    {
        boolean toConsole = false;
        boolean toXHTML = true;
        boolean borders = true;
        boolean rulingLines = true;
        boolean processSpaces = false;
        boolean outputStages = false;
        int currentArgumentIndex = 0;
        String password = "";
        String encoding = DEFAULT_ENCODING;
        String inFile = null;
        String outFile = null;
        int startPage = 1;
        int endPage = Integer.MAX_VALUE;
        for( int i=0; i<args.length; i++ )
        {
            if( args[i].equals( PASSWORD ) )
            {
                i++;
                if( i >= args.length )
                {
                    usage();
                }
                password = args[i];
            }
            else if( args[i].equals( ENCODING ) )
            {
                i++;
                if( i >= args.length )
                {
                    usage();
                }
                encoding = args[i];
            }
            else if( args[i].equals( START_PAGE ) )
            {
                i++;
                if( i >= args.length )
                {
                    usage();
                }
                startPage = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( END_PAGE ) )
            {
                i++;
                if( i >= args.length )
                {
                    usage();
                }
                endPage = Integer.parseInt( args[i] );
            }
            else if( args[i].equals( CONSOLE ) )
            {
                toConsole = true;
            }
            else
            {
                if( inFile == null )
                {
                    inFile = args[i];
                }
                else
                {
                    outFile = args[i];
                }
            }
        }

        if( inFile == null )
        {
            usage();
        }

        if( outFile == null && inFile.length() >4 )
        {
            outFile = inFile.substring( 0, inFile.length() -4 ) + ".html";
        }
        
        DocumentProcessor dp = new PageAnalyser();
        dp.inFile = inFile;
        dp.outFile = outFile;

        dp.startPage = startPage;
        dp.endPage = endPage;
        
        dp.toConsole = toConsole;
        dp.toXHTML = toXHTML;
        dp.borders = borders;
        dp.rulingLines = rulingLines;
        dp.processSpaces = processSpaces;
        dp.outputStages = outputStages;
        dp.password = password;
        dp.encoding = encoding;
        
        try
        {
        	dp.processDocument();
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        }
    }
    
    /**
     * This will print the usage requirements and exit.
     */
    private static void usage()
    {
        System.err.println( "Usage: java com.roundtrippdf.FindTables [OPTIONS] <PDF file> [Text File]\n" +
            "  -password  <password>        Password to decrypt document\n" +
            "  -encoding  <output encoding> (ISO-8859-1,UTF-16BE,UTF-16LE,...)\n" +
            "  -console                     Send text to console instead of file\n" +
            "  -startPage <number>          The first page to start extraction(1 based)\n" +
            "  -endPage <number>            The last page to extract(inclusive)\n" +
            "  <PDF file>                   The PDF document to use\n" +
            "  [Text File]                  The file to write the text to\n"
            );
        System.exit( 1 );
    }
}
