package support.explainable;

import java.nio.file.Files;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BenchmarkSummaryTest {
    @Test
    public void embedsSummaryAsEscapedTextAndIdentifiesSource() throws Exception {
        var directory = Files.createTempDirectory("benchmark-summary-");
        var file = directory.resolve("locator-benchmark-11-10-2026_19-55-49.html");
        try {
            Files.writeString(file, "<article class='alert'><div data-correct='1' data-false='1' data-total='14'></div><h2>Decision summary</h2><p>1 verified; 1 incorrect; precision 50.0%</p><script>alert('bad')</script></article>");
            String result = BenchmarkSummary.section(directory);
            Assert.assertTrue(result.contains("precision 50.0%"));
            Assert.assertTrue(result.contains(file.getFileName().toString()));
            Assert.assertTrue(result.contains("run separately"));
            Assert.assertFalse(result.contains("<script>"));
            Assert.assertTrue(result.contains("Proposed acceptance targets"));
            Assert.assertTrue(result.contains("at least 95%"));
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
    }

    @Test
    public void missingBenchmarkDoesNotInventMetrics() throws Exception {
        var directory = Files.createTempDirectory("no-benchmark-");
        try { Assert.assertTrue(BenchmarkSummary.section(directory).contains("No readable benchmark")); }
        finally { Files.deleteIfExists(directory); }
    }
}
