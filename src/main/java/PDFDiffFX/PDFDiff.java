package PDFDiffFX;

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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class PDFDiff {

    private static void printUsage() {
        System.out.println(
                   "Usage: pdfDiff <file1> <file2> <dest>\n" +
                   "file1: the first of two files to compare\n" +
                   "file2: the second file to compare\n" +
                   "dest: the path to the file to create as output\n" +
                   "[-d]: dump the command-line report to a .txt file"
                   );
    }

    private static boolean printReport(LinkedList<diff_match_patch.Diff> diff_list, PrintStream out) {
        LinkedList<diff_match_patch.Diff> diff = new LinkedList<>(diff_list);
        out.println("Analysis complete.");
        if (diff.isEmpty()) {
            out.println("Documents identical.");
            return true;
        } else {
            // this regex filters out matches that are just whitespace, including carriage returns and non-breaking spaces,
            // as well as the bulk of the list, which contains entries that are the same in both documents
            diff.removeIf(d -> d.operation == diff_match_patch.Operation.EQUAL || d.text.matches("[\\s\r\\u00A0\\u2003]+"));
            if (diff.isEmpty()) {
                out.println("Differences found, but only in whitespace and/or non-printing characters.");
            } else {
                out.println("Differences identified.");
                int i = 0;
                for (diff_match_patch.Diff d : diff) {
                    printDiff(d, ++i, out);
                }
            }
            return false;
        }
    }

    private static void printDiff(diff_match_patch.Diff d, int index, PrintStream out) {
        out.println( String.format( "#%d: %s, \"%s\"", index, d.operation, d.text.trim() ) );
    }

    private static ArrayList<BufferedImage> pdfToImages(PDDocument pdDocument/*, String folderPath*/) {
        ArrayList<BufferedImage> images = new ArrayList<>();
        try {
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
//                BufferedImage bImage = pdfRenderer.renderImage(i);
                images.add(pdfRenderer.renderImage(i));
            }
//            imageFiles.add(new File(folderPath + "\\template_image.jpg"));
        } catch (IOException e){
            System.out.println("Error encountered during rendering of PDF to images.");
            e.printStackTrace();
            return null;
        }

//        return imageFiles;
        return images;
    }

    private static boolean compareImage(BufferedImage img1, BufferedImage img2, ArrayList<BufferedImage> resultList) {
        boolean identical = true;
        int height = img1.getHeight();
        int width = img1.getWidth();
        BufferedImage composite = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                try {
                    int pixel1 = img1.getRGB(x, y);
                    int pixel2 = img2.getRGB(x, y);
                    if (pixel1 == pixel2) {
                        composite.setRGB(x, y, pixel1);
                    } else {
                        identical = false;
                        int a = 0xff & pixel1 >> 24;
                        int r = 0xff & pixel1 >> 16;
                        int g = 0xff & pixel1 >> 8;
                        int b = 0xff & pixel1;
                        int pixelMod = a << 24 | r << 16 | g << 8 | b;
                        composite.setRGB(x, y, pixelMod);
                    }
                } catch (Exception _e) {
                    composite.setRGB(x, y, 0x80ff0000);
                }
            }
        }
        resultList.add(composite);
        return identical;
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

        String fileName1 = argList.get(0);
        String fileName2 = argList.get(1);
        String outFile = argList.get(2);

        // open documents
        File file1 = new File(fileName1);
        try (PDDocument doc1 = PDDocument.load(file1)) {
            File file2 = new File(fileName2);
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
                // dump to file
                try (PrintWriter outWriter = new PrintWriter(outFile+".html")) {
                    outWriter.print(html);
                }

                // if some flag, compare graphically

                // cleaned-up version, highlights differences, more readable
                LinkedList<diff_match_patch.Diff> semDiff = dmp.diff_main(file1Text, file2Text);
                dmp.diff_cleanupSemantic(semDiff);
                printReport(semDiff, System.out);
                if (dump) {
                    System.out.println("Copying this report to " + outFile + "_summary.txt.");
                    printReport( semDiff, new PrintStream( new File( outFile+"_summary.txt") ) );
                }

            }
        } catch (IOException _ioe) {
            System.out.println("Could not open files");
        }

    }
}
