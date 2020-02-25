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
import java.util.List;

public class PDFDiff {

    static String filename1, filename2, outFile;
    static boolean dump = false, graphical = false;

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

        if (graphicalDiffPageArray != null) {
            if (graphicalDiffPageArray.isEmpty()) {
                result.append("\n\nNo visual differences identified.");
            } else {
                result.append("\n\nVisual differences identified on the following pages:\n");
                result.append(compressRange(graphicalDiffPageArray));
            }
        }
        return result.toString();
    }

    private static String compressRange(ArrayList<Integer> pageArray) {
        StringBuilder result = new StringBuilder();
        int previous = pageArray.get(0), start = previous;
        for (int next : pageArray.subList(1, pageArray.size())) {
            // if increment is 1, skip; else, gap is larger, so write as range
            if (previous != next - 1) {
                result.append(compressRangeHelper(start, previous)).append(",");
                start = next;
            }
            previous = next;
        }
        return result.append(compressRangeHelper(start, previous)).toString();
    }

    private static String compressRangeHelper(int start, int previous) {
        String ret = String.valueOf(start);
        if (start != previous)
            ret += (previous - start > 1 ? "-" : ",") + previous;
        return ret;
    }

    private static String formatDiff(diff_match_patch.Diff d, int index) {
        return String.format( "#%d: %s, \"%s\"\n", index, d.operation, d.text.trim() );
    }

    private static byte[] pageToByteArray(PDDocument page) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        page.save(baos);
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
            // leftovers
            if (file1Pages.size() > minPages) {
                appendLeftoverPages(graphicalDiffPages, file1Pages, diffArray, minPages);
            } else if (file2Pages.size() > minPages) {
                appendLeftoverPages(graphicalDiffPages, file2Pages, diffArray, minPages);
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

    private static void appendLeftoverPages(ArrayList<PDDocument> graphicalDiffPages, ArrayList<PDDocument> pages,
                                            ArrayList<Integer> diffArray, int startingPage) {
        for (int i = startingPage; i < pages.size(); i++) {
            graphicalDiffPages.add(pages.get(i));
            diffArray.add(i);
        }
    }

    private static void generateTextualDiff(List<String> paginatedStringDiffs, List<Integer> pagesWithDiffs,
                                              List<PDDocument> file1Pages, List<PDDocument> file2Pages) throws IOException {
        if (file1Pages == null || file2Pages == null) {
            AlertBox.display("Pagination error", "Error encountered while paginating documents");
            return;
        }
        PDFTextStripper textStripper = new PDFTextStripper();
        diff_match_patch dmp = new diff_match_patch();

        int minPages = Math.min(file1Pages.size(), file2Pages.size());
        for (int i = 0; i < minPages; i++) {
            String page1 = textStripper.getText(file1Pages.get(i));
            String page2 = textStripper.getText(file2Pages.get(i));

            // compute difference between documents

            // HTML diff
            LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(page1, page2);
            String html = dmp.diff_prettyHtml(diff);

            // do any desired cleanup/formatting
            html = html.replaceAll("&para;", "");
            html = html.replaceAll("<ins style=\"background:#e6ffe6;\"> </ins><span>", "");

            // flag any differences for easy searching
            html = html.replaceAll("<del", " PDFDIFF:<del").replaceAll("<ins", " PDFDIFF:<ins");
            html = "Page " + i + "\n\n" + html;
            // if any differences flagged, add to report
            if (html.contains("PDFDIFF")) {
                paginatedStringDiffs.add(html);
                pagesWithDiffs.add(i);
            }
        }
    }

    static void processArgs(String[] args) {

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
        outFile   = argList.get(2);
    }

    // mention that we can list excluded areas

    public static void main(String[] args) {

        if (args.length < 3) {
            printUsage();
            return;
        }

        processArgs(args);

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
                // TODO: extract report generation into separate class to cut down on file size

                // compare textually
                List<String> paginatedStringDiffs = new ArrayList<>();
                List<Integer> pagesWithDiffs = new ArrayList<>();
                diff_match_patch dmp = new diff_match_patch();
                generateTextualDiff(paginatedStringDiffs, pagesWithDiffs, file1Pages, file2Pages);

                // dump to file
                if (paginatedStringDiffs.size() != 0) {
                    try (PrintWriter outWriter = new PrintWriter(outFile+".html")) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : paginatedStringDiffs) {
                            sb.append(s);
                        }
                        outWriter.print(sb.toString());
                    }
                }

                // TODO: take pagesWithDiffs and feed to summary generator; use page numbers instead of counter in those lines

                // cleaned-up version, highlights differences, more readable
                PDFTextStripper textStripper = new PDFTextStripper();
                LinkedList<diff_match_patch.Diff> semDiff = dmp.diff_main(textStripper.getText(doc1), textStripper.getText(doc2));
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
