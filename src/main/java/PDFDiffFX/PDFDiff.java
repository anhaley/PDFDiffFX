package PDFDiffFX;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.PdfComparator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

    private static String createSummary(LinkedList<diff_match_patch.Diff> diff_list) {
        LinkedList<diff_match_patch.Diff> diff = new LinkedList<>(diff_list);
        StringBuilder result = new StringBuilder();
        if (diff.isEmpty()) {
            result.append("Documents identical.\n");
        } else {
            // this regex filters out matches that are just whitespace, including carriage returns and non-breaking spaces,
            // as well as the bulk of the list, which contains entries that are the same in both documents
            diff.removeIf(d -> d.operation == diff_match_patch.Operation.EQUAL || d.text.matches("[\\s\r\\u00A0\\u2003]+"));
            if (diff.isEmpty()) {
                result.append("Differences found, but only in whitespace and/or non-printing characters.\n");
            } else {
                result.append("Differences identified.\n");
                int i = 0;
                for (diff_match_patch.Diff d : diff) {
                    result.append(formatDiff(d, ++i));
                }
                result.append("\nTo quickly find these differences in the report, use CTRL+F to search for the keyword PDFDIFF.");
            }
        }
        return result.toString();
    }

    private static String formatDiff(diff_match_patch.Diff d, int index) {
        return String.format( "#%d: %s, \"%s\"\n", index, d.operation, d.text.trim() );
    }

    private static ArrayList<BufferedImage> pdfToImages(PDDocument pdDocument/*, String folderPath*/) {
        ArrayList<BufferedImage> images = new ArrayList<>();
        try {
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
//                BufferedImage bImage = pdfRenderer.renderImage(i);
                images.add(pdfRenderer.renderImage(i));
            }
        } catch (IOException e){
            System.out.println("Error encountered during rendering of PDF to images.");
            e.printStackTrace();
            return null;
        }

        return images;
    }


    private static PDDocument compareImageLists(ArrayList<BufferedImage> doc1, ArrayList<BufferedImage> doc2) {
        if (doc1 == null || doc2 == null) {
            System.out.println("Input document null during conversion to images.");
            return null;
        }
        ArrayList<Integer> differentPages = new ArrayList<>();
        ArrayList<BufferedImage> composites = new ArrayList<>();
        PDDocument pdfOut = new PDDocument();
        for (int i = 0; i < (Math.max(doc1.size(), doc2.size())); i++) {
            if (i >= doc1.size() || i >= doc2.size()) {
                differentPages.add(i);
                continue;
            }
            // compare images and add composite to list
            boolean identical = compareImage(doc1.get(i), doc2.get(i), composites);
            // if different, add to differentPages
            if (!identical) {
                differentPages.add(i);
            }
            // add page to PDF
            BufferedImage composite = composites.get(i);
            int width = composite.getWidth();
            int height = composite.getHeight();
            PDPage page = new PDPage(new PDRectangle(width, height));
            pdfOut.addPage(page);
            try {
                PDImageXObject img = LosslessFactory.createFromImage(pdfOut, composite);
                try (PDPageContentStream contentStream = new PDPageContentStream(pdfOut, page, PDPageContentStream.AppendMode.APPEND, false, true))
                {
                     contentStream.drawImage(img, 0, 0 );
                }
            } catch (IOException _e) {
                System.out.println("Could not add image to PDF.");
                break;
            }

        }
        reportDifferentPages(differentPages);
        return pdfOut;
    }

    private static boolean compareImage(BufferedImage bufferedImage1, BufferedImage bufferedImage2, ArrayList<BufferedImage> composites) {
        return false;
    }

    private static void reportDifferentPages(ArrayList<Integer> diffs) {
        if (diffs.isEmpty()) {
            System.out.println("No graphical differences found.");
        } else {
            StringBuilder sb = new StringBuilder("Graphical differences found on pages: [");
            for (int i = 0; i < diffs.size(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(diffs.get(i) + 1);
            }
            sb.append("]");
            System.out.println(sb.toString());
        }
    }

    private static void generateGraphicalDiff() throws IOException {
        CompareResult graphicalDiff = new PdfComparator(filename1, filename2).compare();
        if (!graphicalDiff.isEqual()) {
            graphicalDiff.writeTo(outFile+"_visual_diff");
        }
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

                // if some flag, compare graphically
                if (graphical)
                    generateGraphicalDiff();

                // cleaned-up version, highlights differences, more readable
                LinkedList<diff_match_patch.Diff> semDiff = dmp.diff_main(file1Text, file2Text);
                dmp.diff_cleanupSemantic(semDiff);

                AlertBox.display( "Summary Report", createSummary(semDiff) );
                if (dump) {
                    String summaryFile = outFile + "_summary.txt";
                    try ( PrintWriter pw = new PrintWriter( new File(summaryFile) ) ) {
                        System.out.println("Copying this report to " + summaryFile);
                        pw.println(createSummary(semDiff));
                    }
                }
            }
        } catch (IOException _ioe) {
            System.out.println("Could not open files");
        }

    }
}
