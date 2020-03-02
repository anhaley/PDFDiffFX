import PDFDiffFX.PDFDiff;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class PDFDiffTest {

    static ByteArrayOutputStream stdout;
    static String testOutFile = "results/testOut";
    String pdfDir = "src/test/resources/";
    String policyPdf = pdfDir + "PDFs/00CG596140.pdf";
    String policyPdfMod = pdfDir + "PDFs/00CG596140_modified.pdf";

    @Rule
    public JavaFXThreadingRule jfxRule = new JavaFXThreadingRule();

    @BeforeEach
    void setUp() {
        stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
    }

    @Test
    void identicalDocumentsHaveNoDifferences() {
        PDFDiff.main(new String[] { policyPdf, policyPdf, testOutFile});
        assertFalse(PDFDiffTest.stdout.toString().contains("Differences identified"));
    }

    @Test
    void differentDocumentsIdentifiedAsDifferent() {
        PDFDiff.main(new String[] {policyPdf, policyPdfMod, testOutFile});
        final String output = PDFDiffTest.stdout.toString();
        assertTrue(output.contains("Differences identified"));
        assertTrue(output.contains("#1: DELETE, \"ERISA\""));
        assertTrue(output.contains("#2: DELETE, \"d the differenc\""));
        assertTrue(output.contains("#3: INSERT, \"sdfsfsdf fewfg asdd th\""));
        assertTrue(output.contains("#4: INSERT, \"and some other\""));
    }

    @Test
    void htmlGenerated() {
        File file = new File (testOutFile+".html");
        if (file.exists()) {
            assertTrue(file.delete());
        }
        assertFalse(file.exists());
        PDFDiff.main(new String[] {policyPdf, policyPdf, testOutFile});
        assertTrue(file.exists());
    }

    @Test
    void htmlFileStructuredCorrectly() {
        PDFDiff.main(new String[] {policyPdf, policyPdf, testOutFile});
        try ( Scanner fileScanner = new Scanner( new File(testOutFile+".html") ) ) {
            assertEquals("<span>PREVIEW", fileScanner.nextLine());
            assertEquals("<br>WORK REQUEST", fileScanner.nextLine());
            assertEquals("<br>Print Date January 30, 2020 1 Quantity", fileScanner.nextLine());
        } catch (IOException _ioe) {
            fail("Error opening file: " + _ioe.getMessage());
        }
    }

    @Test
    void dumpFlagCreatesFileWithReport() {
        PDFDiff.main(new String[] {policyPdf, policyPdfMod, testOutFile, "-d"});
        File file = new File(testOutFile+"_summary.txt");
        assertTrue(file.exists());
        try ( Scanner fileScanner = new Scanner(file) ) {
            assertEquals("Analysis complete.", fileScanner.nextLine());
            assertEquals(fileScanner.nextLine(), "Differences identified.");
            assertEquals(fileScanner.nextLine(), "#1: DELETE, \"ERISA\"");
            assertEquals(fileScanner.nextLine(), "#2: DELETE, \"d the differenc\"");
            assertEquals(fileScanner.nextLine(), "#3: INSERT, \"sdfsfsdf fewfg asdd th\"");
        } catch (FileNotFoundException _e) {
            fail("File containing report could not be opened");
        }
    }

    @Test
    void errorMessagePrintsIfFilesNotFound() {
        PDFDiff.main(new String[] {"iDontExist", "neitherDoI", testOutFile});
        assertTrue(PDFDiffTest.stdout.toString().contains("Could not open files"));
    }

    @Test
    void flagArgConsumedCorrectly() {
        String expected = "Copying this report to " + testOutFile + "_summary.txt";
        PDFDiff.main(new String[] {policyPdf, policyPdf, testOutFile, "-d"});
        assertTrue(PDFDiffTest.stdout.toString().contains(expected));

        stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        PDFDiff.main(new String[] {policyPdf, policyPdf, "-d", testOutFile});
        assertTrue(PDFDiffTest.stdout.toString().contains(expected));

        stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        PDFDiff.main(new String[] {policyPdf, "-d", policyPdf, testOutFile});
        assertTrue(PDFDiffTest.stdout.toString().contains(expected));

        stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        PDFDiff.main(new String[] {"-d", policyPdf, policyPdf, testOutFile});
        assertTrue(PDFDiffTest.stdout.toString().contains(expected));

        stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        PDFDiff.main(new String[] {policyPdf, policyPdf, testOutFile});
        assertFalse(PDFDiffTest.stdout.toString().contains(expected));
    }

    // figure out why extra spaces are getting encoded as weird chars, how to prevent

    // test image generation

    @AfterAll
    static void tearDown() {
        // delete all created files
        String[] createdFiles = {testOutFile+".pdf", testOutFile+".html", testOutFile+".txt", testOutFile+"_summary.txt"};
        for (String file : createdFiles) {
            File f = new File(file);
            if (f.exists()) {
                assertTrue(f.delete());
                assertFalse(f.exists());
            }
        }
    }
}