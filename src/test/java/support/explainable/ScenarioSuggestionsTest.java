package support.explainable;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;
public class ScenarioSuggestionsTest {
    @Test public void failedAndMissedRepairsTriggerReviewableSuggestions() {
        var m=BenchmarkMetrics.summarize(List.of(new BenchmarkMetrics.Attempt(true,true,"FALSE HEAL",false),new BenchmarkMetrics.Attempt(true,false,"BLOCKED SAFELY",true)));
        String html=ScenarioSuggestions.html(m);
        Assert.assertTrue(html.contains("Observed incorrect recovery"));
        Assert.assertTrue(html.contains("Observed missed recoverable cases"));
        Assert.assertTrue(html.contains("review required"));
    }
    @Test public void noObservedFailureIsNotInvented() {
        String html=ScenarioSuggestions.html(BenchmarkMetrics.summarize(List.of()));
        Assert.assertFalse(html.contains("Observed incorrect recovery"));
        Assert.assertFalse(html.contains("Observed missed recoverable cases"));
    }
}
