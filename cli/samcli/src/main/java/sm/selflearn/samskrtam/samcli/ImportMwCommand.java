package sm.selflearn.samskrtam.samcli;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import sm.selflearn.samskrtam.samcli.config.ConfigService;
import sm.selflearn.samskrtam.samcli.config.SamcliConfig;
import sm.selflearn.samskrtam.samcli.importing.MwImporter;
import sm.selflearn.samskrtam.samcli.io.DataSourceFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(
        name = "import-mw",
        description = "Import the Monier-Williams dictionary (mw.txt) into a NEW flat table/schema."
)
public class ImportMwCommand extends AbstractSamcliCommand implements Callable<Integer> {

    private final ConfigService configService;

    @Option(names = "--source-file", description = "Path to mw.txt (overrides samcli.yml mw.source-file).")
    private String sourceFile;

    @Option(names = "--schema", description = "Target schema (must be a NEW schema, not the dictionary-service 'cologne_mw').")
    private String schema;

    @Option(names = "--table", description = "Target flat table (overrides samcli.yml mw.table).")
    private String table;

    @Option(names = "--batch-size", description = "Rows per INSERT batch (overrides samcli.yml mw.batch-size).")
    private Integer batchSize;

    @Option(names = "--truncate", description = "TRUNCATE the target table before import for idempotent re-import.")
    private boolean truncate;

    private JdbcTemplate jdbcTemplate;

    public ImportMwCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        SamcliConfig cfg = configService.loadConfig(configFile);
        SamcliConfig.MwConfig mw = cfg.getMw();

        String src = firstNonNull(sourceFile, mw.getSourceFile());
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException(
                    "Source file not set. Provide --source-file or mw.source-file in samcli.yml.");
        }
        String sch = firstNonNull(schema, mw.getSchema(), "mw_flat");
        String tbl = firstNonNull(table, mw.getTable(), "mw_entries");
        int bs = (batchSize != null && batchSize > 0)
                ? batchSize
                : (mw.getBatchSize() > 0 ? mw.getBatchSize() : 1000);

        java.nio.file.Path file = java.nio.file.Paths.get(src);
        if (!java.nio.file.Files.exists(file)) {
            throw new IllegalArgumentException("Source file not found: " + file);
        }

        org.slf4j.LoggerFactory.getLogger(ImportMwCommand.class).info(
                "import-mw: schema='{}', table='{}', file='{}', batch-size={}, truncate={}",
                sch, tbl, file, bs, truncate);

        if (jdbcTemplate == null) {
            jdbcTemplate = DataSourceFactory.createJdbcTemplate();
        }

        MwImporter importer = new MwImporter(jdbcTemplate);
        importer.prepareSchema(sch, tbl);
        int imported = importer.importFile(file, sch, tbl, bs, truncate);

        org.slf4j.LoggerFactory.getLogger(ImportMwCommand.class).info(
                "import-mw: done, {} entries imported into \"{}\".\"{}\".", imported, sch, tbl);
        return 0;
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
