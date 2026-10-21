package support.explainable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;

/** Adds the latest controlled benchmark as explicitly separate evidence, never as the live test outcome. */
final class BenchmarkSummary {
    private BenchmarkSummary() { }

    static String section(Path directory) {
        if (!Files.isDirectory(directory)) return unavailable();
        try (var files = Files.list(directory)) {
            Path latest = files.filter(p -> p.getFileName().toString().matches("locator-benchmark-.*\\.html"))
                    .filter(Files::isRegularFile)
                    .filter(BenchmarkSummary::belongsToCurrentRun)
                    .max(Comparator.comparingLong(BenchmarkSummary::modified)).orElse(null);
            if (latest == null) return unavailable();
            String source = Files.readString(latest, StandardCharsets.UTF_8);
            var match = Pattern.compile("<article class='alert'[^>]*>(.*?)</article>", Pattern.DOTALL).matcher(source);
            if (!match.find()) return unavailable();
            String text = match.group(1).replaceAll("</(?:h2|p|small|li)>", "\n")
                    .replaceAll("<[^>]*>", "").strip();
            StringBuilder cards = new StringBuilder();
            var metrics = Pattern.compile("data-correct='(\\d+)' data-false='(\\d+)' data-total='(\\d+)' data-applied='(\\d+)' data-unverified='(\\d+)' data-errors='(\\d+)'").matcher(source);
            if (metrics.find()) {
                long correct = Long.parseLong(metrics.group(1)), wrong = Long.parseLong(metrics.group(2));
                long applied = Long.parseLong(metrics.group(4));
                String precision = applied == 0 ? "Not measured" : String.format(java.util.Locale.ROOT, "%.1f%%", 100.0 * correct / applied);
                cards.append("<p><b>Unverified applied repairs:</b> " + metrics.group(5) + " | <b>Execution errors:</b> " + metrics.group(6) + "</p>");
                cards.append("<div class='benchmark-grid'><div class='benchmark-box'><h3>Verified recovery rate (all applied attempts)</h3><strong>")
                        .append(precision).append("</strong><p>").append(correct).append(" correct out of ").append(applied)
                        .append(" applied replacements</p></div><div class='benchmark-box'><h3>Incorrect recoveries</h3><strong>")
                        .append(wrong).append("</strong><p>Target: zero observed</p></div><div class='benchmark-box'><h3>Evidence size</h3><strong>")
                        .append(metrics.group(3)).append("</strong><p>Controlled executions, not independent production samples</p></div></div>");
            }
            cards.append("<div class='benchmark-box'><h3>Proposed acceptance targets</h3><ul>")
                    .append("<li>Correct recovery precision: at least 95% on a broader labelled benchmark.</li>")
                    .append("<li>Incorrect recoveries: zero observed in the acceptance suite.</li>")
                    .append("<li>Recover the eligible changed-class case and reject every unsafe fixture.</li>")
                    .append("<li>Expand to at least 100 independently designed mutations before considering broader rollout.</li>")
                    .append("</ul><p>These are proposed project targets, not an industry standard. A perfect result on this small suite is not production approval.</p></div>");
            for (String paragraph : text.split("\\n")) {
                if (!paragraph.isBlank()) cards.append("<div class='benchmark-box'><p>").append(escape(paragraph)).append("</p></div>");
            }
            return "<style>.benchmark-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px}"
                    + ".benchmark-box{background:white;border:1px solid #dbe3ec;border-radius:10px;padding:18px;margin:12px 0}"
                    + ".benchmark-box h3{margin:0 0 12px}.benchmark-box strong{font-size:28px}.benchmark-box p{line-height:1.6}"
                    + ".benchmark-box li{margin:10px 0}</style><section class='card'><h2>Recovery readiness and improvement plan</h2>"
                    + "<p><b>Benchmark source:</b> " + escape(latest.getFileName().toString()) + "</p>"
                    + "<p>This benchmark was run separately from the live journey in the same invocation. Run ID and code revision match this report.</p>"
                    + cards + "</section>";
        } catch (IOException error) {
            return unavailable();
        }
    }

    private static boolean belongsToCurrentRun(Path file) {
        try { return RunIdentity.matches(Files.readString(file, StandardCharsets.UTF_8)); }
        catch (IOException error) { return false; }
    }

    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException error) { return Long.MIN_VALUE; }
    }

    private static String unavailable() {
        return "<section class='card'><h2>Controlled benchmark decision summary</h2>"
                + "<p>No readable matching benchmark summary is available for this run and code revision. Run the stakeholder profile "
                + "to include its results. No benchmark metrics have been inferred.</p></section>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
