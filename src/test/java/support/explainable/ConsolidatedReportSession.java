package support.explainable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Produces one stakeholder-ready report from Extent-style steps, Explainable AI findings,
 * and any self-healing locator decision. One session is held per test thread.
 */
public final class ConsolidatedReportSession {
    public record ExecutedStep(String status, String action, String observed, long elapsedMillis) { }
    public record TestSummary(String testName, String outcome, String businessUpdate, String aiDiagnosis,
                              List<ExecutedStep> steps, SelfHealingLocatorAgent.HealingDecision healing) { }

    private static final ThreadLocal<ConsolidatedReportSession> CURRENT = new ThreadLocal<>();
    private final String testName;
    private final Instant startedAt;
    private final List<ExecutedStep> steps = new ArrayList<>();
    private SelfHealingLocatorAgent.HealingDecision healing;
    private String businessOverride;

    private ConsolidatedReportSession(String testName) {
        this.testName = testName;
        this.startedAt = Instant.now();
    }

    public static void start(String testName) { CURRENT.set(new ConsolidatedReportSession(testName)); }

    public static void recordExtentStep(String status, String action, String observed, long elapsedMillis) {
        current().steps.add(new ExecutedStep(status, action, observed, elapsedMillis));
    }

    public static void recordHealing(SelfHealingLocatorAgent.HealingDecision decision) {
        current().healing = decision;
    }

    /** Optional product/release language supplied by the test owner for a business-facing suite view. */
    public static void setBusinessUpdate(String update) {
        current().businessOverride = update;
    }

    public static void write(Path destination, ExplainableAiAgent.Report explainableReport) throws IOException {
        ConsolidatedReportSession session = current();
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, session.html(explainableReport), StandardCharsets.UTF_8);
        CURRENT.remove();
    }

    /** Ends this test's collection and returns compact data for RegressionSuiteReport. */
    public static TestSummary finish(ExplainableAiAgent.Report explainableReport) {
        ConsolidatedReportSession session = current();
        TestSummary summary = session.summary(explainableReport);
        CURRENT.remove();
        return summary;
    }

    private TestSummary summary(ExplainableAiAgent.Report explainableReport) {
        ExplainableAiAgent.Explanation explanation = explainableReport.explain();
        boolean recovered = healing != null && healing.appliedThisRun();
        String outcome = recovered ? "PASS AFTER RECOVERY"
                : explanation.summary().startsWith("PASS") ? "PASS" : "FAIL";
        String defaultBusiness = recovered ? healing.businessUpdate() : explanation.stakeholderSummary();
        String business = businessOverride == null || businessOverride.isBlank() ? defaultBusiness : businessOverride;
        return new TestSummary(testName, outcome, business, explanation.diagnosis(), List.copyOf(steps), healing);
    }

    private String html(ExplainableAiAgent.Report explainableReport) {
        ExplainableAiAgent.Explanation explanation = explainableReport.explain();
        boolean recovered = healing != null && healing.appliedThisRun();
        String overall = recovered ? "PASS AFTER RECOVERY" : explanation.summary().startsWith("PASS") ? "PASS" : "ACTION REQUIRED";
        String overallClass = recovered || explanation.summary().startsWith("PASS") ? "good" : "bad";
        String reliability = healing == null ? "N/A" : healing.successRateLabel();
        String confidence = healing == null ? "N/A" : String.format(Locale.ROOT, "%.1f%%", healing.matchConfidence() * 100);
        String business = recovered ? healing.businessUpdate() : explanation.stakeholderSummary();

        StringBuilder rows = new StringBuilder();
        for (int index = 0; index < steps.size(); index++) {
            ExecutedStep step = steps.get(index);
            String statusClass = step.status().equalsIgnoreCase("PASS") ? "pass" : step.status().equalsIgnoreCase("FAIL") ? "fail" : "info";
            rows.append("<tr><td>").append(index + 1).append("</td><td class=\"").append(statusClass).append("\">")
                .append(html(step.status())).append("</td><td>").append(html(step.action())).append("</td><td>")
                .append(html(step.observed())).append("</td><td>").append(step.elapsedMillis()).append(" ms</td></tr>");
        }
        String healingSection = healing == null
                ? "<section class=\"panel\"><h2>Automated recovery</h2><p>No locator recovery was needed for this test.</p></section>"
                : """
                  <section class="panel"><h2>Automated recovery audit</h2>
                  <div class="change"><div><b>Failed locator</b><br>%s</div><div><b>Verified replacement</b><br>%s</div><div><b>Replacement reliability</b><br>%s</div></div>
                  <p><b>Why it failed:</b> %s</p>
                  <p><b>Governance:</b> %s</p></section>
                  """.formatted(html(healing.failedLocator().display()),
                        html(healing.replacementLocator() == null ? "No replacement" : healing.replacementLocator().display()),
                        html(healing.successRateLabel()), html(healing.failureReason()),
                        healing.promotedForFutureRuns() ? "Replacement approved for automatic reuse after measured success." : "Replacement is being monitored before automatic reuse.");

        return """
            <!doctype html><html><head><meta charset="utf-8"><title>Consolidated Test Resilience Report</title>
            <style>
            body{margin:0;background:#f4f7fb;color:#172033;font:15px Arial,sans-serif}.hero{padding:34px 9%%;background:#102a43;color:#fff}.hero h1{margin:0;font-size:28px}.hero p{margin:7px 0;color:#cbd5e1}.wrap{max-width:1180px;margin:26px auto;padding:0 24px}.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.kpi,.panel{background:#fff;border-radius:12px;padding:20px;box-shadow:0 2px 8px #cbd5e166}.metric{font-size:25px;font-weight:700;margin-bottom:7px}.label{color:#64748b}.good,.pass{color:#15803d}.bad,.fail{color:#b42318}.info{color:#2563eb}.panel{margin-top:18px}.panel h2{margin:0 0 13px;font-size:19px}.business{background:#eff6ff;border-left:5px solid #2563eb;padding:16px;border-radius:4px}.change{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.change div{background:#f8fafc;padding:13px;border-radius:7px}table{border-collapse:collapse;width:100%%}th,td{padding:11px;border-bottom:1px solid #e2e8f0;text-align:left;vertical-align:top}th{background:#f8fafc;color:#475569}@media(max-width:760px){.kpis,.change{grid-template-columns:1fr}}</style>
            </head><body><header class="hero"><h1>Consolidated Test Resilience Report</h1><p>%s | Extent execution timeline + Explainable AI + self-healing audit</p></header>
            <main class="wrap"><section class="kpis"><div class="kpi"><div class="metric %s">%s</div><div class="label">Overall test outcome</div></div><div class="kpi"><div class="metric">%d</div><div class="label">Executed test steps</div></div><div class="kpi"><div class="metric">%s</div><div class="label">Replacement reliability</div></div><div class="kpi"><div class="metric">%s</div><div class="label">Locator match confidence</div></div></section>
            <section class="panel"><h2>Business update</h2><div class="business"><b>Stakeholder summary</b><br>%s</div></section>
            <section class="panel"><h2>Explainable AI conclusion</h2><p><b>Outcome:</b> %s</p><p><b>Why:</b> %s</p><p><b>Recommended action:</b> %s</p></section>
            <section class="panel"><h2>Extent-style execution timeline</h2><table><tr><th>#</th><th>Status</th><th>Test action</th><th>Observed result</th><th>Time</th></tr>%s</table></section>
            %s
            </main></body></html>
            """.formatted(html(testName), overallClass, overall, steps.size(), html(reliability), html(confidence),
                    html(business), html(explanation.summary()), html(explanation.diagnosis()), html(explanation.nextAction()),
                    rows, healingSection);
    }

    private static ConsolidatedReportSession current() {
        ConsolidatedReportSession session = CURRENT.get();
        if (session == null) throw new IllegalStateException("Call ConsolidatedReportSession.start() in @BeforeMethod.");
        return session;
    }
    private static String html(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
