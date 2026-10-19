package io.vinum.test.harness;

import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;

/** Adds compact machine-readable results to vanilla logging and JUnit XML, including interactive runs. */
public final class StructuredTestReporter implements TestReporter {
    public static final Path REPORT = Path.of("vinum-test-results.json");
    private static StructuredTestReporter current;
    private final TestReporter xml;
    private final TestReporter log = new LogTestReporter();
    private final Map<String, Result> results = new LinkedHashMap<>();
    private record Result(String test, String status, long durationMillis, String failure) {}

    public StructuredTestReporter() throws javax.xml.parsers.ParserConfigurationException {
        xml = new JUnitLikeTestReporter(new File("vinum-gametest-results.xml"));
        current = this;
    }
    @Override public void onTestFailed(GameTestInfo test) { log.onTestFailed(test); xml.onTestFailed(test); record(test, false); }
    @Override public void onTestSuccess(GameTestInfo test) { log.onTestSuccess(test); xml.onTestSuccess(test); record(test, true); }
    @Override public void finish() { log.finish(); xml.finish(); write(); }

    private void record(GameTestInfo test, boolean passed) {
        results.put(test.getTestName(), new Result(test.getTestName(), passed ? "passed" : "failed", test.getRunTime(),
                passed || test.getError() == null ? null : test.getError().toString()));
        write();
    }
    private void write() {
        Path temporary = REPORT.resolveSibling(REPORT.getFileName() + ".tmp");
        try {
            long failures = results.values().stream().filter(result -> result.status.equals("failed")).count();
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("schemaVersion", 1,
                    "passed", results.size() - failures, "failed", failures, "tests", results.values()));
            Files.writeString(temporary, json + "\n");
            Files.move(temporary, REPORT, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            org.slf4j.LoggerFactory.getLogger(StructuredTestReporter.class).error("Cannot write test report {}", REPORT, error);
        }
    }
    public static String summary() {
        if (current == null || current.results.isEmpty()) return "No world test results yet. Run /vinumtest runall.";
        long failed = current.results.values().stream().filter(result -> result.status.equals("failed")).count();
        return (current.results.size() - failed) + " passed, " + failed + " failed. Report: " + REPORT.toAbsolutePath();
    }
}
