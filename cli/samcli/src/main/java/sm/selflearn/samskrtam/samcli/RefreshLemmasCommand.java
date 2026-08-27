package sm.selflearn.samskrtam.samcli;

import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import sm.selflearn.samskrtam.samcli.config.ConfigService;
import sm.selflearn.samskrtam.samcli.config.SamcliConfig;
import sm.selflearn.samskrtam.samcli.io.DataSourceFactory;
import sm.selflearn.samskrtam.samcli.io.SqlScriptRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Rebuilds the unified fuzzy-search lemma index (lingua.lemmas) by executing the
 * SQL index script. The script location is configurable via
 * {@code --script} / {@code lemmas.script-file} and falls back to the
 * bundled classpath resource when not found on disk.
 */
@Component
@CommandLine.Command(
        name = "refresh-lemmas",
        description = "Rebuild the unified lemma index (lingua.lemmas) from all present dictionaries."
)
public class RefreshLemmasCommand extends AbstractSamcliCommand implements java.util.concurrent.Callable<Integer> {

    private final ConfigService configService;

    @Option(names = "--script", description = "Path to the lemma-index SQL script (overrides lemmas.script-file).")
    private String scriptFile;

    private JdbcTemplate jdbcTemplate;

    public RefreshLemmasCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        SamcliConfig cfg = configService.loadConfig(configFile);
        SamcliConfig.LemmasConfig lemmas = cfg.getLemmas();

        String scriptPath = firstNonNull(scriptFile, lemmas.getScriptFile(),
                "etcetera/sql/lingua_index_lemmas.sql");

        String script = loadScript(scriptPath);

        if (jdbcTemplate == null) {
            jdbcTemplate = DataSourceFactory.createJdbcTemplate();
        }

        LoggerFactory.getLogger(RefreshLemmasCommand.class).info(
                "refresh-lemmas: executing index script '{}'...", scriptPath);
        int executed = SqlScriptRunner.run(jdbcTemplate, script);
        LoggerFactory.getLogger(RefreshLemmasCommand.class).info(
                "refresh-lemmas: done, {} statements executed.", executed);
        return 0;
    }

    private String loadScript(String scriptPath) {
        Path file = Path.of(scriptPath);
        if (Files.exists(file)) {
            try {
                return Files.readString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read SQL script: " + file, e);
            }
        }
        // Fallback to the bundled classpath resource (single source: etcetera/sql, copied at build time).
        try (InputStream in = RefreshLemmasCommand.class.getResourceAsStream("/sql/lingua_index_lemmas.sql")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read bundled SQL script", e);
        }
        throw new IllegalArgumentException(
                "SQL script not found at '" + scriptPath + "' and no bundled resource present.");
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
