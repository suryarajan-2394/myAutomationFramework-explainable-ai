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
                    .max(Comparator.comparingLong(BenchmarkSummary::modified)).orElse(null);
            if (latest == null) return unavailable();
            String source = Files.readString(latest, StandardCharsets.UTF_8);
            var match = Pattern.compile("<article class='alert'>(.*?)</article>", Pattern.DOTALL).matcher(source);
            if (!match.find()) return unavailable();
            String text = match.group(1).replaceAll("</(?:h2|p|small)>", "\n")
                    .replaceAll("<[^>]*>", "").strip();
            return "<section class='card'><h2>Controlled benchmark decision summary</h2>"
                    + "<p><b>Source:</b> " + escape(latest.getFileName().toString()) + "</p>"
                    + "<p>This is the latest available local benchmark, run separately from this live test. "
                    + "It may describe an earlier code version; rerun the benchmark after changing recovery rules.</p>"
                    + "<p style='white-space:pre-line'>" + escape(text) + "</p></section>";
        } catch (IOException error) {
            return unavailable();
        }
    }

    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException error) { return Long.MIN_VALUE; }
    }

    private static String unavailable() {
        return "<section class='card'><h2>Controlled benchmark decision summary</h2>"
                + "<p>No readable benchmark summary is available. Run LocatorBenchmarkTest before the live test "
                + "to include its results. No benchmark metrics have been inferred.</p></section>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
