package support.explainable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe suite roll-up for parallel TestNG regression runs.
 * Keeps executive metrics concise and preserves every test's Extent/XAI/healing evidence in details.
 */
public final class RegressionSuiteReport {
    private static final ConcurrentLinkedQueue<ConsolidatedReportSession.TestSummary> TESTS = new ConcurrentLinkedQueue<>();
    private static volatile String suiteName = "Regression Suite";
    private static volatile Instant startedAt;

    private RegressionSuiteReport() { }

    public static void start(String name) {
        TESTS.clear();
        suiteName = name == null || name.isBlank() ? "Regression Suite" : name;
        startedAt = Instant.now();
    }

    public static void record(ConsolidatedReportSession.TestSummary summary) {
        if (summary == null) throw new IllegalArgumentException("Test summary is required.");
        TESTS.add(summary);
    }

    public static void write(Path destination) throws IOException {
        List<ConsolidatedReportSession.TestSummary> tests = new ArrayList<>(TESTS);
        tests.sort(Comparator.comparing(ConsolidatedReportSession.TestSummary::testName));
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, render(tests), StandardCharsets.UTF_8);
    }

    private static String render(List<ConsolidatedReportSession.TestSummary> tests) {
        long cleanPass = tests.stream().filter(test -> test.outcome().equals("PASS")).count();
        long recovered = tests.stream().filter(test -> test.outcome().equals("PASS AFTER RECOVERY")).count();
        long failed = tests.stream().filter(test -> test.outcome().equals("FAIL")).count();
        long healed = tests.stream().filter(test -> test.healing() != null && test.healing().appliedThisRun()).count();

        StringBuilder index = new StringBuilder();
        StringBuilder detail = new StringBuilder();
        for (ConsolidatedReportSession.TestSummary test : tests) {
            String style = test.outcome().equals("FAIL") ? "fail" : test.outcome().contains("RECOVERY") ? "recovered" : "pass";
            String healingLabel = test.healing() == null ? "None required"
                    : test.healing().appliedThisRun() ? "Recovered: " + test.healing().successRateLabel() : "No safe replacement";
            index.append("<tr><td>").append(html(test.testName())).append("</td><td class=\"").append(style).append("\">")
                    .append(html(test.outcome())).append("</td><td>").append(test.steps().size()).append("</td><td>")
                    .append(html(healingLabel)).append("</td><td>").append(html(test.businessUpdate())).append("</td></tr>");

            StringBuilder steps = new StringBuilder();
            for (ConsolidatedReportSession.ExecutedStep step : test.steps()) {
                steps.append("<tr><td>").append(html(step.status())).append("</td><td>").append(html(step.action()))
                        .append("</td><td>").append(html(step.observed())).append("</td><td>").append(step.elapsedMillis()).append(" ms</td></tr>");
            }
            String healing = test.healing() == null ? "No locator recovery was required."
                    : "Failed locator: " + test.healing().failedLocator().display()
                    + " | Replacement: " + (test.healing().replacementLocator() == null ? "None" : test.healing().replacementLocator().display())
                    + " | Reliability: " + test.healing().successRateLabel();
            detail.append("""
                <details><summary><span class="%s">%s</span> - %s</summary>
                <p><b>Business update:</b> %s</p><p><b>Explainable AI:</b> %s</p><p><b>Self-healing:</b> %s</p>
                <table><tr><th>Status</th><th>Executed step</th><th>Observed result</th><th>Time</th></tr>%s</table></details>
                """.formatted(style, html(test.outcome()), html(test.testName()), html(test.businessUpdate()),
                        html(test.aiDiagnosis()), html(healing), steps));
        }

        String businessOverview = failed == 0
                ? "All " + tests.size() + " regression checks completed. " + recovered
                    + " journey(s) were automatically recovered, with no remaining release blockers."
                : failed + " journey(s) remain blocked and require delivery-team attention before release.";
        return """
            <!doctype html><html><head><meta charset="utf-8"><title>Regression Suite Executive Report</title>
            <style>
            body{margin:0;background:#f4f7fb;color:#172033;font:15px Arial,sans-serif}.hero{padding:34px 9%%;background:#102a43;color:#fff}.hero h1{margin:0;font-size:28px}.hero p{margin:7px 0;color:#cbd5e1}.wrap{max-width:1280px;margin:26px auto;padding:0 24px}.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.kpi,.panel,details{background:#fff;border-radius:12px;padding:20px;box-shadow:0 2px 8px #cbd5e166}.metric{font-size:28px;font-weight:700;margin-bottom:7px}.label{color:#64748b}.pass{color:#15803d;font-weight:700}.recovered{color:#2563eb;font-weight:700}.fail{color:#b42318;font-weight:700}.panel{margin-top:18px}.panel h2{margin:0 0 13px}.business{background:#eff6ff;border-left:5px solid #2563eb;padding:16px;border-radius:4px}table{border-collapse:collapse;width:100%%}th,td{padding:11px;border-bottom:1px solid #e2e8f0;text-align:left;vertical-align:top}th{background:#f8fafc;color:#475569}details{margin:12px 0}summary{cursor:pointer;font-size:16px;font-weight:600}details p{line-height:1.5}@media(max-width:760px){.kpis{grid-template-columns:1fr}}</style>
            </head><body><header class="hero"><h1>Regression Suite Executive Report</h1><p>%s | %s | Consolidated Extent execution, Explainable AI, and self-healing evidence</p></header>
            <main class="wrap"><section class="kpis"><div class="kpi"><div class="metric">%d</div><div class="label">Tests executed</div></div><div class="kpi"><div class="metric pass">%d</div><div class="label">Passed cleanly</div></div><div class="kpi"><div class="metric recovered">%d</div><div class="label">Passed after recovery</div></div><div class="kpi"><div class="metric fail">%d</div><div class="label">Release blockers</div></div></section>
            <section class="panel"><h2>Business update</h2><div class="business"><b>Suite readiness</b><br>%s<br><br><b>Automation resilience:</b> %d locator recovery event(s) were validated and retained as auditable evidence.</div></section>
            <section class="panel"><h2>Test portfolio</h2><table><tr><th>Test case</th><th>Outcome</th><th>Steps</th><th>Recovery</th><th>Business update</th></tr>%s</table></section>
            <section class="panel"><h2>Test-by-test evidence</h2><p>Expand a test to view its Extent-style steps, Explainable AI diagnosis, and self-healing details.</p>%s</section></main></body></html>
            """.formatted(html(suiteName), startedAt == null ? "Not started" : startedAt, tests.size(), cleanPass, recovered, failed,
                    html(businessOverview), healed, index, detail);
    }

    private static String html(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
