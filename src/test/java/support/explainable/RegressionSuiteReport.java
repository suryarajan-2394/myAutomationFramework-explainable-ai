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
        long healingAssessed = tests.stream().filter(test -> test.healing() != null).count();
        long executedSteps = tests.stream().mapToLong(test -> test.steps().size()).sum();
        long passedSteps = tests.stream().flatMap(test -> test.steps().stream())
                .filter(step -> step.status().equalsIgnoreCase("PASS")).count();
        long failedSteps = tests.stream().flatMap(test -> test.steps().stream())
                .filter(step -> step.status().equalsIgnoreCase("FAIL")).count();
        long informationSteps = Math.max(0, executedSteps - passedSteps - failedSteps);

        int cleanPercent = percentage(cleanPass, tests.size());
        int recoveredPercent = percentage(recovered, tests.size());
        int failedPercent = percentage(failed, tests.size());
        int stepPassPercent = percentage(passedSteps, executedSteps);
        int healingPercent = percentage(healed, healingAssessed);

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
        String outcomeChart = """
            <section class="panel charts" aria-label="Executive visual summary">
              <h2>Executive visual summary</h2>
              <div class="chart-grid">
                <article class="chart"><h3>Test outcomes</h3>
                  <div class="donut-row"><div class="donut" style="background:conic-gradient(#15803d 0 %d%%,#2563eb %d%% %d%%,#b42318 %d%% 100%%)" role="img" aria-label="%d percent clean pass, %d percent recovered, %d percent blocked"><span>%d%%<small>release-ready</small></span></div>
                  <ul class="legend"><li><i class="swatch clean"></i>Clean pass <b>%d</b></li><li><i class="swatch recovery"></i>Recovered <b>%d</b></li><li><i class="swatch blocker"></i>Blocked <b>%d</b></li></ul></div>
                  <p class="chart-note">A recovered test completed after a verified locator replacement; it is not counted as a hidden failure.</p></article>
                <article class="chart"><h3>Execution quality</h3>
                  <div class="bar-label"><span>Successful steps</span><b>%d%%</b></div><div class="bar-track"><span class="bar-pass" style="width:%d%%"></span></div>
                  <div class="bar-label"><span>Attention needed</span><b>%d</b></div><div class="bar-track"><span class="bar-fail" style="width:%d%%"></span></div>
                  <p class="chart-note">%d of %d recorded Extent-style steps passed. %d informational step(s) are excluded from the quality rate.</p></article>
                <article class="chart"><h3>Self-healing effectiveness</h3>
                  <div class="healing-value">%d%%<small>safe recovery rate</small></div>
                  <div class="bar-track"><span class="bar-recovery" style="width:%d%%"></span></div>
                  <p class="chart-note">%d of %d assessed locator issue(s) were safely recovered and logged with replacement evidence and reliability history.</p></article>
              </div>
            </section>
            """.formatted(cleanPercent, cleanPercent, cleanPercent + recoveredPercent, cleanPercent + recoveredPercent,
                    cleanPercent, recoveredPercent, failedPercent, cleanPercent + recoveredPercent,
                    cleanPass, recovered, failed, stepPassPercent, stepPassPercent, failedSteps,
                    percentage(failedSteps, executedSteps), passedSteps, executedSteps, informationSteps,
                    healingPercent, healingPercent, healed, healingAssessed);
        return """
            <!doctype html><html><head><meta charset="utf-8"><title>Regression Suite Executive Report</title>
            <style>
            body{margin:0;background:#f4f7fb;color:#172033;font:15px Arial,sans-serif}.hero{padding:34px 9%%;background:#102a43;color:#fff}.hero h1{margin:0;font-size:28px}.hero p{margin:7px 0;color:#cbd5e1}.wrap{max-width:1280px;margin:26px auto;padding:0 24px}.kpis{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.kpi,.panel,details{background:#fff;border-radius:12px;padding:20px;box-shadow:0 2px 8px #cbd5e166}.metric{font-size:28px;font-weight:700;margin-bottom:7px}.label{color:#64748b}.pass{color:#15803d;font-weight:700}.recovered{color:#2563eb;font-weight:700}.fail{color:#b42318;font-weight:700}.panel{margin-top:18px}.panel h2{margin:0 0 13px}.business{background:#eff6ff;border-left:5px solid #2563eb;padding:16px;border-radius:4px}table{border-collapse:collapse;width:100%%}th,td{padding:11px;border-bottom:1px solid #e2e8f0;text-align:left;vertical-align:top}th{background:#f8fafc;color:#475569}details{margin:12px 0}summary{cursor:pointer;font-size:16px;font-weight:600}details p{line-height:1.5}.chart-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:22px}.chart{min-width:0}.chart h3{font-size:16px;margin:0 0 14px}.donut-row{display:flex;align-items:center;gap:18px}.donut{width:126px;height:126px;border-radius:50%%;position:relative;flex:none}.donut:after{content:"";position:absolute;inset:20px;background:#fff;border-radius:50%%}.donut span{position:absolute;z-index:1;inset:35px 0 0;text-align:center;font-size:24px;font-weight:700}.donut small,.healing-value small{display:block;font-size:11px;font-weight:400;color:#64748b;margin-top:3px}.legend{list-style:none;margin:0;padding:0;line-height:1.9;min-width:130px}.legend b{float:right;margin-left:16px}.swatch{display:inline-block;width:10px;height:10px;border-radius:50%%;margin-right:7px}.clean,.bar-pass{background:#15803d}.recovery,.bar-recovery{background:#2563eb}.blocker,.bar-fail{background:#b42318}.bar-label{display:flex;justify-content:space-between;margin:16px 0 6px}.bar-track{height:12px;border-radius:99px;background:#e2e8f0;overflow:hidden}.bar-track span{display:block;height:100%%;border-radius:99px}.healing-value{font-size:38px;font-weight:700;color:#2563eb;margin:24px 0 10px}.chart-note{color:#64748b;line-height:1.45;margin:14px 0 0;font-size:13px}@media(max-width:900px){.chart-grid{grid-template-columns:1fr}.chart{padding-bottom:8px}.kpis{grid-template-columns:repeat(2,1fr)}}@media(max-width:560px){.kpis{grid-template-columns:1fr}.wrap{padding:0 14px}.donut-row{align-items:flex-start}}</style>
            </head><body><header class="hero"><h1>Regression Suite Executive Report</h1><p>%s | %s | Consolidated Extent execution, Explainable AI, and self-healing evidence</p></header>
            <main class="wrap"><section class="kpis"><div class="kpi"><div class="metric">%d</div><div class="label">Tests executed</div></div><div class="kpi"><div class="metric pass">%d</div><div class="label">Passed cleanly</div></div><div class="kpi"><div class="metric recovered">%d</div><div class="label">Passed after recovery</div></div><div class="kpi"><div class="metric fail">%d</div><div class="label">Release blockers</div></div></section>
            <section class="panel"><h2>Business update</h2><div class="business"><b>Suite readiness</b><br>%s<br><br><b>Automation resilience:</b> %d locator recovery event(s) were validated and retained as auditable evidence.</div></section>
            %s
            <section class="panel"><h2>Test portfolio</h2><table><tr><th>Test case</th><th>Outcome</th><th>Steps</th><th>Recovery</th><th>Business update</th></tr>%s</table></section>
            <section class="panel"><h2>Test-by-test evidence</h2><p>Expand a test to view its Extent-style steps, Explainable AI diagnosis, and self-healing details.</p>%s</section></main></body></html>
            """.formatted(html(suiteName), startedAt == null ? "Not started" : startedAt, tests.size(), cleanPass, recovered, failed,
                    html(businessOverview), healed, outcomeChart, index, detail);
    }

    private static int percentage(long numerator, long denominator) {
        return denominator == 0 ? 0 : (int) Math.round(numerator * 100.0 / denominator);
    }

    private static String html(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
