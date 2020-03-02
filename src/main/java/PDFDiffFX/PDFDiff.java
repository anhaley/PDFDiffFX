package PDFDiffFX;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PDFDiff {

    String filename1, filename2, outFilePrefix, outDir;
    boolean dump = false, graphical = false;

    /**
     * Prints usage instructions. For debugging purposes.
     */
    private void printUsage() {
        System.out.println(
                   "Usage: pdfDiff <file1> <file2> <dest>\n" +
                   "file1: the first of two files to compare\n" +
                   "file2: the second file to compare\n" +
                   "dest: the path to the file to create as output\n" +
                   "[-d]: dump the command-line report to a .txt file"
                   );
    }

    /**
     * Processes the input arguments and populates class variables.
     * @param args The input arguments supplied by the calling class
     */
    private void processArgs(String[] args) throws IOException {

        List<String> argList = new ArrayList<>(Arrays.asList(args));

        // flag for copying stdout to file
        if (argList.contains("-d")) {
            dump = true;
            argList.remove("-d");
        }

        // flag for doing graphical diff
        if (argList.contains("-g")) {
            graphical = true;
            argList.remove("-g");
        }

        filename1 = argList.get(0);
        filename2 = argList.get(1);
        outDir = argList.get(2);
        File od = new File(outDir);
        if (!od.exists())
            if (!od.mkdir())
                throw new IOException();
        String[] pathComponents = outDir.split("/");
        outFilePrefix = outDir + "/" + pathComponents[pathComponents.length - 1];
    }

    // TODO: make configurable by page range
    // TODO: add .ini file to configure settings, including excluded regions
    //      maybe then add toolbar item to open/configure these options
    //      maybe add option, when summary comes back, to say "ignore diffs in this region next time"
    //      or have checkbox that causes the diff regions to be dumped to a file for analysis/coordinate usage
    // TODO: add tests
    // TODO: add a Readme that explains the different reports and how to interpret them.
    //      include pictures of example reports
    //      add a button/toolbar item to the GUI to launch this Readme
    // TODO: figure out how to close the documents without breaking functionality
    // TODO: solve summary window sizing issue
    public static void main(String[] args) {
        PDFDiff engine = new PDFDiff();

        if (args.length < 3) {
            engine.printUsage();
            return;
        }

        try {
            engine.processArgs(args);
            ReportTool reportTool = new ReportTool(engine.outFilePrefix);

            // open documents
            File file1 = new File(engine.filename1);
            try (PDDocument doc1 = PDDocument.load(file1)) {
                File file2 = new File(engine.filename2);
                try (PDDocument doc2 = PDDocument.load(file2)) {

                    // generate whole-document comparison
                    reportTool.generateWholeTextualDiff(doc1, doc2);

                    // compare page-by-page
                    List<PDDocument> file1Pages = reportTool.pdfToPages(doc1);
                    List<PDDocument> file2Pages = reportTool.pdfToPages(doc2);

                    // compare graphically
                    List<Integer> graphicalDiffPageNums = null;
                    if (engine.graphical) {
                        graphicalDiffPageNums = reportTool.generatePaginatedGraphicalDiff(file1Pages, file2Pages);
                    }

                    // compare textually
                    reportTool.generatePaginatedTextualDiff(file1Pages, file2Pages);

                    // generate summary
                    reportTool.showSummary(doc1, doc2, graphicalDiffPageNums, engine.dump);
                }
            }
        } catch (IOException _ioe) {
            AlertBox.display("File error", "Could not open files. Consult developer's console.");
            System.out.println("Could not open files");
            _ioe.printStackTrace();
        }
    }
}
