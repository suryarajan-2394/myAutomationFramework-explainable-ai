package support.explainable;

import java.util.ArrayList;
import java.util.List;

/** Runtime repair events are separate from final TestNG failures. No source files are edited by a test. */
final class RepairTrace {
    private record Event(String stage, String detail, String source) { }
    private static final ThreadLocal<List<Event>> EVENTS = ThreadLocal.withInitial(ArrayList::new);
    static void clear() { EVENTS.remove(); }
    static void add(String stage, String detail, String source) { EVENTS.get().add(new Event(stage, detail, source)); }
    static String html() {
        if (EVENTS.get().isEmpty()) return "<section class='card'><h2>Automatic repair history</h2><p>No locator repair was required in this execution.</p></section>";
        StringBuilder result = new StringBuilder("<section class='card'><h2>Automatic repair history</h2><p>These are locator attempts within one test execution. Runtime replacements do not edit or commit Java source files.</p>");
        for (Event event : EVENTS.get()) result.append("<div style='background:white;border:1px solid #dbe3ec;border-radius:10px;padding:16px;margin:12px 0'><h3>")
                .append(escape(event.stage)).append("</h3><p>").append(escape(event.detail))
                .append("</p><p><b>Source location:</b> ").append(escape(event.source)).append("</p></div>");
        return result.append("</section>").toString();
    }
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
