package PDFDiffFX;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class PDFDiff {

    static String filename1, filename2, outFile;

    private static void printUsage() {
        System.out.println(
                   "Usage: pdfDiff <file1> <file2> <dest>\n" +
                   "file1: the first of two files to compare\n" +
                   "file2: the second file to compare\n" +
                   "dest: the path to the file to create as output\n" +
                   "[-d]: dump the command-line report to a .txt file"
                   );
    }

    private static String createSummary(LinkedList<diff_match_patch.Diff> diff_list, ArrayList<Integer> graphicalDiffPageArray) {
        LinkedList<diff_match_patch.Diff> diff = new LinkedList<>(diff_list);
        StringBuilder result = new StringBuilder();
        // text
        if (diff.isEmpty()) {
            result.append("No textual differences identified.\n");
        } else {
            // this regex filters out matches that are just whitespace, including carriage returns and non-breaking spaces,
            // as well as the bulk of the list, which contains entries that are the same in both documents
            diff.removeIf(d -> d.operation == diff_match_patch.Operation.EQUAL || d.text.matches("[\\s\r\\u00A0\\u2003]+"));
            if (diff.isEmpty()) {
                result.append("Textual differences found, but only in whitespace and/or non-printing characters.\n");
            } else {
                result.append("Textual differences identified.\n");
                int i = 0;
                for (diff_match_patch.Diff d : diff) {
                    result.append(formatDiff(d, ++i));
                }
                result.append("\nTo quickly find these differences in the report, use CTRL+F to search for the keyword PDFDIFF.");
            }
        }
        // visual
        if (graphicalDiffPageArray == null || graphicalDiffPageArray.isEmpty()) {
            result.append("\n\nNo visual differences identified.");
        } else {
            result.append("\n\nVisual differences identified on the following pages:\n");
            for (int i = 0; i < graphicalDiffPageArray.size(); i++) {
                if (i > 0)
                    result.append(",");
                result.append(i);
            }
        }
        return result.toString();
    }

    private static String formatDiff(diff_match_patch.Diff d, int index) {
        return String.format( "#%d: %s, \"%s\"\n", index, d.operation, d.text.trim() );
    }

    private static byte[] pageToByteArray(PDDocument page) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        page.save(baos);
        page.close();
        return baos.toByteArray();
    }

    private static PDDocument byteArrayToPdf(byte[] doc) throws IOException {
        return PDDocument.load(doc);
    }

    private static PDDocument pagesToPdf(ArrayList<PDDocument> pages) {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        PDDocument doc = new PDDocument();
        try {
            for (PDDocument page : pages) {
                pdfMerger.appendDocument(doc, page);
            }
        } catch (IOException _ioe) {
            AlertBox.display("Document Error", "Merging of PDF pages failed");
            doc = null;
        }
        return doc;
    }

    private static ArrayList<PDDocument> pdfToPages(PDDocument document) {
        try {
            return new ArrayList<>(new Splitter().split(document));
        } catch (IOException e) {
            AlertBox.display("Document error", "Error encountered while attempting to paginate document");
            return null;
        }
    }

    private static PDDocument graphicalDiffPage(PDDocument page1, PDDocument page2) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PDDocument result = null;
        try {
            InputStream is1 = new ByteArrayInputStream(pageToByteArray(page1));
            InputStream is2 = new ByteArrayInputStream(pageToByteArray(page2));
            CompareResult comp = new PdfComparator<>(is1, is2).compare();
            // only add if different
            if (!comp.writeTo(baos))
                result = PDDocument.load(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static ArrayList<Integer> generateGraphicalDiff(ArrayList<PDDocument> file1Pages, ArrayList<PDDocument> file2Pages)
            throws IOException {
//        CompareResult graphicalDiff = new PdfComparator<>(filename1, filename2).compare();
//        if (!graphicalDiff.isEqual()) {
//            graphicalDiff.writeTo(outFile+"_visual_diff");
//        }

        ArrayList<PDDocument> graphicalDiffPages = new ArrayList<>();
        ArrayList<Integer> diffArray = new ArrayList<>();
        if (file1Pages != null && file2Pages != null) {
            int minPages = Math.min(file1Pages.size(), file2Pages.size());
            for (int i = 0; i < minPages; i++) {
                PDDocument pageDiff = graphicalDiffPage(file1Pages.get(i), file2Pages.get(i));
                // if differences found, add to list
                if (pageDiff != null) {
                    graphicalDiffPages.add(pageDiff);
                    diffArray.add(i);
                }
            }
        }
        // if difflist not empty, write to file
        if (!graphicalDiffPages.isEmpty()) {
            PDDocument graphicalDiff = pagesToPdf(graphicalDiffPages);
            graphicalDiff.save(outFile + "_visual_diff.pdf");
            graphicalDiff.close();
            for (PDDocument page : graphicalDiffPages)
                page.close();
        }
        return diffArray;
    }

    private static void generateTextualDiff(ArrayList<PDDocument> file1Pages, ArrayList<PDDocument> file2Pages) {

    }

    // enhancement: instead of scraping entire file, go page-by-page to make it easier to find?
    // mention that we can list excluded areas

    public static void main(String[] args) {

        if (args.length < 3) {
            printUsage();
            return;
        }

        ArrayList<String> argList = new ArrayList<>(Arrays.asList(args));

        // flag for copying stdout to file
        boolean dump = false;
        if (argList.contains("-d")) {
            dump = true;
            argList.remove("-d");
        }

        // flag for doing graphical diff
        boolean graphical = false;
        if (argList.contains("-g")) {
            graphical = true;
            argList.remove("-g");
        }

        filename1 = argList.get(0);
        filename2 = argList.get(1);
        outFile   = argList.get(2);

        // open documents
        File file1 = new File(filename1);
        try (PDDocument doc1 = PDDocument.load(file1)) {
            File file2 = new File(filename2);
            try (PDDocument doc2 = PDDocument.load(file2)) {

                ArrayList<Integer> graphicalDiffPageArray = null;

                // go page-by-page

                // compare graphically
                ArrayList<PDDocument> file1Pages = pdfToPages(doc1);
                ArrayList<PDDocument> file2Pages = pdfToPages(doc2);
                if (graphical) {
                    graphicalDiffPageArray = generateGraphicalDiff(file1Pages, file2Pages);
                }

                // TODO: make configurable by page range
                // TODO: make text go by pages
                // TODO: report by page number

                // compare textually
                generateTextualDiff(file1Pages, file2Pages);

                // compare documents as wholes

                // strip text from both documents
                PDFTextStripper textStripper = new PDFTextStripper();
                String file1Text = textStripper.getText(doc1);
                String file2Text = textStripper.getText(doc2);

                // compute difference between documents
                diff_match_patch dmp = new diff_match_patch();

                // HTML diff
                LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(file1Text, file2Text);
                String html = dmp.diff_prettyHtml(diff);

                // do any desired cleanup/formatting
                html = html.replaceAll("&para;", "");
                html = html.replaceAll("<ins style=\"background:#e6ffe6;\"> </ins><span>", "");

                // flag any differences for easy searching
                html = html.replaceAll("<del", " PDFDIFF:<del").replaceAll("<ins", " PDFDIFF:<ins");

                // dump to file
                try (PrintWriter outWriter = new PrintWriter(outFile+".html")) {
                    outWriter.print(html);
                }

                // cleaned-up version, highlights differences, more readable
                LinkedList<diff_match_patch.Diff> semDiff = dmp.diff_main(file1Text, file2Text);
                dmp.diff_cleanupSemantic(semDiff);

                if (dump) {
                    String summaryFile = outFile + "_summary.txt";
                    try ( PrintWriter pw = new PrintWriter( new File(summaryFile) ) ) {
                        System.out.println("Copying this report to " + summaryFile);
                        pw.println(createSummary(semDiff, graphicalDiffPageArray));
                    }
                }
                AlertBox.display( "Summary Report", createSummary(semDiff, graphicalDiffPageArray) );
            }
        } catch (IOException _ioe) {
            System.out.println("Could not open files");
        }

    }
}
