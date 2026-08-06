package support.explainable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dependency-free Explainable AI agent for Selenium/TestNG.
 * Explanations are deterministic: facts are test evidence and diagnoses are rule-based inferences.
 */
public final class ExplainableAiAgent {
    private ExplainableAiAgent() { }

    public enum Outcome { PASS, FAIL, SKIP }
    public record Step(String action, String expected, String observed, Outcome outcome,
                       Duration elapsed, String evidence) { }
    public record Explanation(String summary, String diagnosis, String nextAction,
                               boolean isInference, String stakeholderSummary) { }

    public static Run begin(String testName) {
        return new Run(testName, Instant.now());
    }

    public static final class Run {
        private final String testName;
        private final Instant start;
        private final List<Step> steps = new ArrayList<>();

        private Run(String testName, Instant start) {
            this.testName = testName;
            this.start = start;
        }

        public Run record(String action, String expected, String observed,
                          Outcome outcome, long elapsedMillis) {
            return record(action, expected, observed, outcome, elapsedMillis, "");
        }

        public Run record(String action, String expected, String observed,
                          Outcome outcome, long elapsedMillis, String evidence) {
            steps.add(new Step(action, expected, observed, outcome,
                    Duration.ofMillis(elapsedMillis), evidence == null ? "" : evidence));
            return this;
        }

        public Report complete(Outcome outcome, Throwable failure) {
            String message = failure == null ? "" : rootMessage(failure);
            return new Report(testName, start, Instant.now(), List.copyOf(steps), outcome, message);
        }
    }

    public static final class Report {
        private final String testName;
        private final Instant start;
        private final Instant end;
        private final List<Step> steps;
        private final Outcome outcome;
        private final String failureMessage;

        private Report(String testName, Instant start, Instant end, List<Step> steps,
                       Outcome outcome, String failureMessage) {
            this.testName = testName;
            this.start = start;
            this.end = end;
            this.steps = steps;
            this.outcome = outcome;
            this.failureMessage = failureMessage;
        }

        public Explanation explain() {
            Step failed = steps.stream().filter(step -> step.outcome() == Outcome.FAIL)
                    .findFirst().orElse(null);
            if (outcome == Outcome.PASS && failed == null) {
                return new Explanation(
                    "PASS: " + testName + " completed " + steps.size() + " recorded UI step(s).",
                    "Observed evidence matches every recorded expectation.",
                    "No action is required; retain the business-level event trail for future regressions.",
                    false,
                    "The customer journey worked as expected: customers can log in, add an item, and see it in their cart.");
            }
            String action = failed == null ? "test execution" : failed.action();
            String evidence = failed == null ? failureMessage : failed.observed();
            String diagnosis = diagnose(evidence);
            return new Explanation(
                outcome + ": " + testName + " stopped at \"" + action + "\".",
                diagnosis + " Evidence: " + (evidence.isBlank() ? "none supplied" : evidence),
                recommend(diagnosis),
                true,
                businessConclusion(diagnosis));
        }

        public void writeHtml(Path file) throws IOException {
            Files.createDirectories(file.getParent());
            Explanation explanation = explain();
            StringBuilder rows = new StringBuilder();
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                rows.append("<tr><td>").append(i + 1).append("</td><td>")
                    .append(html(step.action())).append("</td><td>")
                    .append(html(step.expected())).append("</td><td>")
                    .append(html(step.observed())).append("</td><td class=\"")
                    .append(step.outcome().name().toLowerCase(Locale.ROOT)).append("\">")
                    .append(step.outcome()).append("</td><td>")
                    .append(step.elapsed().toMillis()).append(" ms</td><td>")
                    .append(html(step.evidence())).append("</td></tr>");
            }
            String page = """
                <!doctype html><html><head><meta charset="utf-8"><title>Explainable AI Test Report</title>
                <style>body{font:15px Arial;margin:32px;color:#172033}.meta{color:#64748b}.card{background:#f8fafc;border-left:5px solid #2563eb;padding:16px;margin:20px 0}.pass{color:#15803d;font-weight:bold}.fail{color:#b91c1c;font-weight:bold}.skip{color:#a16207;font-weight:bold}table{border-collapse:collapse;width:100%%}th,td{border:1px solid #cbd5e1;padding:9px;text-align:left;vertical-align:top}th{background:#e2e8f0}</style>
                </head><body><h1>Explainable AI Test Report</h1><p class="meta">%s · %s → %s · %d step(s)</p>
                <div class="card"><h2>%s</h2><p><b>Business summary:</b> %s</p><p><b>Observed fact:</b> %s</p><p><b>%s:</b> %s</p><p><b>Suggested next action:</b> %s</p></div>
                <h2>Evidence timeline</h2><table><tr><th>#</th><th>Action</th><th>Expected</th><th>Observed</th><th>Outcome</th><th>Time</th><th>Evidence</th></tr>%s</table></body></html>
                """.formatted(html(testName), start, end, steps.size(), outcome,
                    html(explanation.summary()), html(explanation.stakeholderSummary()),
                    explanation.isInference() ? "Rule-based inference" : "Conclusion",
                    html(explanation.diagnosis()), html(explanation.nextAction()), rows);
            Files.writeString(file, page, StandardCharsets.UTF_8);
        }

        public void writeJson(Path file) throws IOException {
            Files.createDirectories(file.getParent());
            Explanation explanation = explain();
            StringBuilder json = new StringBuilder("{\n  \"test\": \"").append(json(testName))
                .append("\",\n  \"outcome\": \"").append(outcome)
                .append("\",\n  \"summary\": \"").append(json(explanation.summary()))
                .append("\",\n  \"diagnosis\": \"").append(json(explanation.diagnosis()))
                .append("\",\n  \"businessSummary\": \"").append(json(explanation.stakeholderSummary()))
                .append("\",\n  \"inferred\": ").append(explanation.isInference())
                .append(",\n  \"steps\": [\n");
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                json.append("    {\"action\": \"").append(json(step.action()))
                    .append("\", \"outcome\": \"").append(step.outcome())
                    .append("\", \"observed\": \"").append(json(step.observed())).append("\"}")
                    .append(i + 1 == steps.size() ? "\n" : ",\n");
            }
            json.append("  ]\n}\n");
            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
        }

        private static String diagnose(String detail) {
            String text = detail == null ? "" : detail.toLowerCase(Locale.ROOT);
            if (text.contains("timeout") || text.contains("wait"))
                return "The UI did not reach the expected state within the configured wait.";
            if (text.contains("no such element") || text.contains("not found"))
                return "The expected UI element was not available when the action ran.";
            if (text.contains("assert") || text.contains("expected") || text.contains("actual"))
                return "The assertion expectation differed from the application's observed state.";
            if (text.contains("stale"))
                return "The page re-rendered, making the stored element reference stale.";
            return "A recorded test step reported a failure; inspect its observed value and evidence.";
        }

        private static String businessConclusion(String diagnosis) {
            if (diagnosis.contains("wait")) return "The checkout screen did not become ready. A customer may be unable to complete a purchase.";
            if (diagnosis.contains("element")) return "A required part of the page was unavailable. Customers may be blocked from continuing.";
            if (diagnosis.contains("assertion")) return "The website showed something different from what the business expected.";
            if (diagnosis.contains("stale")) return "The page changed while the customer journey was running, so the journey could not continue.";
            return "The customer journey stopped before completion. The delivery team should investigate the first failed step.";
        }

        private static String recommend(String diagnosis) {
            if (diagnosis.contains("wait")) return "Validate the locator and add an explicit wait for the business state, not a fixed sleep.";
            if (diagnosis.contains("element")) return "Validate the locator against the current page and attach a screenshot or DOM snippet.";
            if (diagnosis.contains("assertion")) return "Compare test data with the requirement; change the assertion only if the requirement changed.";
            if (diagnosis.contains("stale")) return "Re-locate the element after the state change and wait for the refreshed UI.";
            return "Review the first failed step; it is the closest observed cause in this execution.";
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error; while (root.getCause() != null) root = root.getCause();
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
    private static String html(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private static String json(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }
}
