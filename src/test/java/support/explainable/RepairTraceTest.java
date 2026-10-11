package support.explainable;
import org.testng.Assert;
import org.testng.annotations.Test;
public class RepairTraceTest {
    @Test public void showsBothAttemptsWithoutClaimingSourceRewrite() {
        RepairTrace.clear();
        try {
            RepairTrace.add("Attempt 1: failed", "Original locator missing <script>", "src/test/java/pages/HomePage.java:54");
            RepairTrace.add("Runtime repair", "CSS .old -> DATA_TEST cart; source not rewritten", "src/test/java/pages/HomePage.java:54");
            RepairTrace.add("Attempt 2: passed", "Expected cart verified", "src/test/java/pages/HomePage.java:54");
            String html = RepairTrace.html();
            Assert.assertTrue(html.contains("Attempt 1: failed"));
            Assert.assertTrue(html.contains("Attempt 2: passed"));
            Assert.assertTrue(html.contains("HomePage.java:54"));
            Assert.assertTrue(html.contains("do not edit or commit"));
            Assert.assertFalse(html.contains("<script>"));
        } finally { RepairTrace.clear(); }
    }
}
