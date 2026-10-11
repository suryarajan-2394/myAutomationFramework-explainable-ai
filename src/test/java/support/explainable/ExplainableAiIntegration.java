package support.explainable;

import java.io.IOException;
import java.nio.file.Path;
import util.utility;

/** Thread-safe bridge for BaseTest; it works with parallel TestNG execution. */
public final class ExplainableAiIntegration {
    private static final ThreadLocal<ExplainableAiAgent.Run> RUN = new ThreadLocal<>();
    private ExplainableAiIntegration() { }

    public static void start(String testName) { RUN.set(ExplainableAiAgent.begin(testName)); }

    public static void record(String action, String expected, String observed,
                              ExplainableAiAgent.Outcome outcome, long elapsedMillis) {
        current().record(action, expected, observed, outcome, elapsedMillis);
    }

    public static ExplainableAiAgent.Report finish(boolean passed, boolean skipped,
                                                    Throwable failure, Path reportDirectory)
            throws IOException {
        return finish(passed, skipped, failure, reportDirectory, utility.timeStamp());
    }

    public static ExplainableAiAgent.Report finish(boolean passed, boolean skipped,
                                                    Throwable failure, Path reportDirectory, String stamp)
            throws IOException {
        ExplainableAiAgent.Outcome outcome = skipped ? ExplainableAiAgent.Outcome.SKIP
                : passed ? ExplainableAiAgent.Outcome.PASS : ExplainableAiAgent.Outcome.FAIL;
        ExplainableAiAgent.Report report = current().complete(outcome, failure);
        try {
            report.writeHtml(reportDirectory.resolve("explanation-" + stamp + ".html"));
            if (Boolean.getBoolean("framework.reporting.json.enabled"))
                report.writeJson(reportDirectory.resolve("explanation-" + stamp + ".json"));
        } finally { RUN.remove(); }
        return report;
    }

    private static ExplainableAiAgent.Run current() {
        ExplainableAiAgent.Run run = RUN.get();
        if (run == null) throw new IllegalStateException("Call ExplainableAiIntegration.start() in @BeforeMethod.");
        return run;
    }
}
