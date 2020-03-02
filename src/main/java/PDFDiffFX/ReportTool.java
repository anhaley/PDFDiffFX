package PDFDiffFX;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportTool {

    PDFTextStripper textStripper = new PDFTextStripper();
    diff_match_patch dmp = new diff_match_patch();
    String outFilePrefix;

    public ReportTool(String outFilePrefix) throws IOException {
        this.outFilePrefix = outFilePrefix;
    }

    /**
     * Generates a list of textual diffs in String form containing HTML that highlights differences.
     * The list represents pages to make it easier to isolate and find diffs in a large document.
     * These pages are then combined and written to a file.
     * @param file1Pages The pages of the first document
     * @param file2Pages The pages of the second document
     * @throws IOException if an error is encountered while stripping text from the documents
     */
    void generatePaginatedTextualDiff(
            List<PDDocument> file1Pages, List<PDDocument> file2Pages) throws IOException {
        if (file1Pages == null || file2Pages == null) {
            AlertBox.display("Pagination error", "Error encountered while paginating documents");
            return;
        }
        List<String> paginatedStringDiffs = new ArrayList<>();

        int minPages = Math.min(file1Pages.size(), file2Pages.size());
        for (int i = 0; i < minPages; i++) {
            String page1 = textStripper.getText(file1Pages.get(i));
            String page2 = textStripper.getText(file2Pages.get(i));

            // HTML diff
            String html = generateHtmlDiffFromStrings(page1, page2);

            // if any differences flagged, add page to report
            if (html.contains("<del") || html.contains("<ins")) {
                String header = "<br><p style=\"page-break-before:always; font-weight:bold; text-indent:20em;\">-----Page "
                        + (i + 1) + "-----</p><br>";
                paginatedStringDiffs.add(header + html);
            }
        }
        // leftovers
        if (file1Pages.size() > minPages) {
            for (int i = minPages; i < file1Pages.size(); i++) {
                String page = textStripper.getText(file1Pages.get(i));
                // pages don't exist in file 2, so mark them as deletions
                paginatedStringDiffs.add("<del style=\"background:#ffe6e6;\">" + page + "</del>");
            }
        } else if (file2Pages.size() > minPages) {
            for (int i = minPages; i < file1Pages.size(); i++) {
                String page = textStripper.getText(file2Pages.get(i));
                // pages don't exist in file 1, so mark them as insertions
                paginatedStringDiffs.add("<ins style=\"background:#e6ffe6;\">" + page + "</ins>");
            }
        }
        // dump to file
        if (paginatedStringDiffs.size() != 0) {
            try (PrintWriter outWriter = new PrintWriter(outFilePrefix +"_paginated_textual_diff.html")) {
                StringBuilder sb = new StringBuilder();
                for (String s : paginatedStringDiffs) {
                    sb.append(s);
                }
                outWriter.print(sb.toString());
            }
        }
    }

    /**
     * Takes two strings and feeds them to the diff engine to generate HTML-formatted comparison.
     * @param text1 The "original" or "expected" text
     * @param text2 The "actual" text
     * @return The HTML-formatted result
     */
    private String generateHtmlDiffFromStrings(String text1, String text2) {

        LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(text1, text2);
        String html = dmp.diff_prettyHtml(diff);

        // do any desired cleanup/formatting
        html = html.replaceAll("&para;", "");
        // these lines get inserted where no meaningful difference exists, for reasons that are unclear as of yet
        html = html.replaceAll("<ins style=\"background:#e6ffe6;\"> </ins><span>", "");
        return html;
    }

    /**
     * Scrapes both files in their entirety and writes out the diffed text. While this is tedious to review, it
     * has the advantage that it is not thrown off by misaligned pages. If/when a better solution to this problem
     * is found, this can/should be replaced.
     * @param file1 The first document
     * @param file2 The second document
     */
    void generateWholeTextualDiff(PDDocument file1, PDDocument file2) throws IOException {
        String doc1 = textStripper.getText(file1);
        String doc2 = textStripper.getText(file2);
        String html = generateHtmlDiffFromStrings(doc1, doc2);
        String[] lines = html.split("(?<=<br>)");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) { // streamify?
            if (line.contains("<ins") || line.contains("<del")) {
                line = "<pre>PDFDIFF: </pre>" + line;
            }
            sb.append(line);
        }
        html = sb.toString();
        try (PrintWriter outWriter = new PrintWriter(outFilePrefix +"_whole_textual_diff.html")) {
            outWriter.print(html);
        }
    }

    /**
     * Generates a PDDocument containing the result of a visual diff operation. Elements new to doc2 are
     * colored green; elements missing from doc2 are colored red.
     * @param doc1 The "expected" or "original" document
     * @param doc2 The "actual" document
     * @return The visual diff
     */
    private PDDocument graphicalDiffPage(PDDocument doc1, PDDocument doc2) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PDDocument result = null;
        try {
            InputStream is1 = new ByteArrayInputStream(pageToByteArray(doc1));
            InputStream is2 = new ByteArrayInputStream(pageToByteArray(doc2));
            CompareResult comp = new PdfComparator<>(is1, is2).compare();
            // only add if different
            if (!comp.writeTo(baos))
                result = PDDocument.load(baos.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Converts a PDDocument to a byte array.
     * @param doc The document to convert
     * @return The byte array result
     * @throws IOException if storing the document into the ByteArrayOutputStream fails
     */
    private byte[] pageToByteArray(PDDocument doc) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        return baos.toByteArray();
    }

    /**
     * Splits a PDDocument into a list of individual pages.
     * @param document The input document
     * @return The list of pages
     */
    List<PDDocument> pdfToPages(PDDocument document) {
        try {
            return new ArrayList<>(new Splitter().split(document));
        } catch (IOException e) {
            AlertBox.display("Document error", "Error encountered while attempting to paginate document");
            return null;
        }
    }

    /**
     * Generates a file containing the visual diff of two documents. Writes the result to a named file on disk.
     * @param file1Pages The list of pages in document 1
     * @param file2Pages The list of pages in document 2
     * @return A list of pages where differences were identified
     * @throws IOException if error encountered in writing to file
     */
    List<Integer> generatePaginatedGraphicalDiff(List<PDDocument> file1Pages, List<PDDocument> file2Pages)
            throws IOException {

        List<PDDocument> graphicalDiffPages = new ArrayList<>();
        List<Integer> diffArray = new ArrayList<>();
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
                appendLeftoverGraphicalPages(graphicalDiffPages, file1Pages, diffArray, minPages);
            } else if (file2Pages.size() > minPages) {
                appendLeftoverGraphicalPages(graphicalDiffPages, file2Pages, diffArray, minPages);
            }
        }
        // if difflist not empty, write to file
        if (!graphicalDiffPages.isEmpty()) {
            PDDocument graphicalDiff = pagesToPdf(graphicalDiffPages);
            graphicalDiff.save(outFilePrefix + "_visual_diff.pdf");
//            graphicalDiff.close();
//            for (PDDocument page : graphicalDiffPages)
//                page.close();
        }
        return diffArray;
    }

    /**
     * Takes a list of PDDocuments (i.e., individual pages) and joins them into a single document.
     * @param pages The input list
     * @return The joined document
     */
    private PDDocument pagesToPdf(List<PDDocument> pages) {
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

    /**
     * When one document compared by generateGraphicalDiff is longer than the other, there will be leftover pages.
     * Add those pages to the result list of differing pages.
     * @param graphicalDiffPages The partial list of differing pages
     * @param pages The pages of the larger document
     * @param diffPageNums A list of the pages with identified differences
     * @param startingPage The index of the page to start on
     */
    private void appendLeftoverGraphicalPages(
            List<PDDocument> graphicalDiffPages, List<PDDocument> pages, List<Integer> diffPageNums, int startingPage) {
        for (int i = startingPage; i < pages.size(); i++) {
            graphicalDiffPages.add(pages.get(i));
            diffPageNums.add(i);
        }
    }

    /**
     * Returns a textual summary of the differences found in the files.
     * @param diff_list The semantic diff list generated by diff_match_patch
     * @param graphicalDiffPageArray A list of pages where visual diffs have been identified
     * @return A string containing the summary report
     */
    private String createSummary(List<diff_match_patch.Diff> diff_list, List<Integer> graphicalDiffPageArray) {
        List<diff_match_patch.Diff> diff = new LinkedList<>(diff_list);
        StringBuilder result = new StringBuilder();
        // text
        if (diff.isEmpty()) {
            result.append("No textual differences identified.\n");
        } else {
            // this regex filters out matches that are just whitespace, including carriage returns and non-breaking spaces,
            // as well as the bulk of the list, which contains entries that are the same in both documents
            diff.removeIf(d -> d.operation == diff_match_patch.Operation.EQUAL ||
                    d.text.matches("[\\s\r\\u00A0\\u2003]+"));
            if (diff.isEmpty()) {
                result.append("Textual differences found, but only in whitespace and/or non-printing characters.\n");
            } else {
                result.append("Textual differences identified.\n");
                for (diff_match_patch.Diff d : diff) {
                    result.append( String.format( "%s, \"%s\"\n", d.operation, d.text.trim() ) );
                }
            }
        }
        // visual
        if (graphicalDiffPageArray != null) {
            if (graphicalDiffPageArray.isEmpty()) {
                result.append("\n\nNo visual differences identified.");
            } else {
                result.append("\n\nVisual differences identified on the following pages:\n");
                List<Integer> oneIndexedPages = graphicalDiffPageArray.stream().map(i -> i+1).collect(Collectors.toList());
                result.append( compressNumList(oneIndexedPages) );
            }
        }
        String pageNumsNote = "\n\nNOTE: Page numbers are based on File 1 (the \"original\" document). If File 2 " +
                "is longer or shorter than File 1, these page numbers will not necessarily match File 2.";

        return result.append(pageNumsNote).toString();
    }

    /**
     * Takes a list of numbers and compresses them into ranges.
     * E.g., [1,2,3,4,6,7,8,10] becomes [1-4,6-8,10]
     * @param nums The list of numbers
     * @return A compressed list in String form
     */
    private String compressNumList(List<Integer> nums) {
        StringBuilder result = new StringBuilder();
        int previous = nums.get(0), start = previous;
        for (int next : nums.subList(1, nums.size())) {
            // if increment is 1, skip; else, gap is larger, so write as range
            if (previous != next - 1) {
                result.append(compressRun(start, previous)).append(",");
                start = next;
            }
            previous = next;
        }
        return result.append(compressRun(start, previous)).toString();
    }

    /**
     * Helper method for compressRange. Once a run has ended, this method returns the compressed
     * representation of the start and end of the run.
     * @param start The first element in the run
     * @param end The last element in the run
     * @return A string representation of the run
     */
    private String compressRun(int start, int end) {
        String ret = String.valueOf(start);
        if (start != end)
            ret += (end - start > 1 ? "-" : ",") + end;
        return ret;
    }

    /**
     * Displays the summary of differences to the user. If checkBoxCopySummary.checked(), write to file.
     * @param doc1 The first document
     * @param doc2 The second document
     * @param graphicalDiffPageNums The list of pages with graphical diffs, as reported earlier
     * @throws IOException if text stripping results in an error
     */
    void showSummary(PDDocument doc1, PDDocument doc2, List<Integer> graphicalDiffPageNums, boolean dump)
            throws IOException {
        LinkedList<diff_match_patch.Diff> semDiff = dmp.diff_main(textStripper.getText(doc1), textStripper.getText(doc2));
        dmp.diff_cleanupSemantic(semDiff);
        String summary = createSummary(semDiff, graphicalDiffPageNums);
        if (dump) {
            String summaryFile = outFilePrefix + "_summary.txt";
            try ( PrintWriter pw = new PrintWriter( new File(summaryFile) ) ) {
                System.out.println("Copying this report to " + summaryFile);
                pw.println(summary);
            }
        }
        AlertBox.display( "Summary Report", createSummary(semDiff, graphicalDiffPageNums) );
    }

}
