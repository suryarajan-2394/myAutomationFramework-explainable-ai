package support.explainable;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
public class BenchmarkMetricsTest {
    @Test public void executionErrorsDoNotDisappearFromAppliedRepairs() {
        var summary = BenchmarkMetrics.summarize(List.of(
                new BenchmarkMetrics.Attempt(true, true, "VERIFIED RECOVERY"),
                new BenchmarkMetrics.Attempt(true, true, "FALSE HEAL"),
                new BenchmarkMetrics.Attempt(true, true, "EXECUTION ERROR"),
                new BenchmarkMetrics.Attempt(true, false, "EXECUTION ERROR"),
                new BenchmarkMetrics.Attempt(false, false, "BASELINE FAILED")));
        Assert.assertEquals(summary.applied(), 3L);
        Assert.assertEquals(summary.correct(), 1L);
        Assert.assertEquals(summary.falseHeals(), 1L);
        Assert.assertEquals(summary.unverified(), 1L);
        Assert.assertEquals(summary.errors(), 2L);
        Assert.assertEquals(summary.precision(), "33.3%");
    }
    @Test public void abstentionsAreNotIncorrectRecoveries() {
        var summary = BenchmarkMetrics.summarize(List.of(new BenchmarkMetrics.Attempt(true, false, "BLOCKED SAFELY")));
        Assert.assertEquals(summary.precision(), "Not measured");
        Assert.assertEquals(summary.blocked(), 1L);
        Assert.assertEquals(summary.falseHeals(), 0L);
    }
}
