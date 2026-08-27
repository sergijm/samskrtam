package sm.selflearn.samskrtam.samcli;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import sm.selflearn.samskrtam.samcli.config.ConfigService;
import sm.selflearn.samskrtam.samcli.config.SamcliConfig;
import sm.selflearn.samskrtam.samcli.importing.CaeImporter;
import sm.selflearn.samskrtam.samcli.io.DataSourceFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

@Component
@CommandLine.Command(
        name = "import-cae",
        description = "Import the Cappeller Sanskrit-English dictionary (cae.txt) into a NEW flat table/schema."
)
public class ImportCaeCommand extends AbstractSamcliCommand implements java.util.concurrent.Callable<Integer> {

    private final ConfigService configService;

    @Option(names = "--source-file", description = "Path to cae.txt (overrides samcli.yml cae.source-file).")
    private String sourceFile;

    @Option(names = "--schema", description = "Target schema (must be a NEW schema, not owned by another service).")
    private String schema;

    @Option(names = "--table", description = "Target flat table (overrides samcli.yml cae.table).")
    private String table;

    @Option(names = "--batch-size", description = "Rows per INSERT batch (overrides samcli.yml cae.batch-size).")
    private Integer batchSize;

    @Option(names = "--truncate", description = "TRUNCATE the target table before import for idempotent re-import.")
    private boolean truncate;

    private JdbcTemplate jdbcTemplate;

    public ImportCaeCommand(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Integer call() {
        SamcliConfig cfg = configService.loadConfig(configFile);
        SamcliConfig.CaeConfig cae = cfg.getCae();

        String src = firstNonNull(sourceFile, cae.getSourceFile());
        if (src == null || src.isBlank()) {
            throw new IllegalArgumentException(
                    "Source file not set. Provide --source-file or cae.source-file in samcli.yml.");
        }
        String sch = firstNonNull(schema, cae.getSchema(), "cologne_cae");
        String tbl = firstNonNull(table, cae.getTable(), "entries");
        int bs = (batchSize != null && batchSize > 0)
                ? batchSize
                : (cae.getBatchSize() > 0 ? cae.getBatchSize() : 1000);

        Path file = Path.of(src);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Source file not found: " + file);
        }

        LoggerFactory.getLogger(ImportCaeCommand.class).info(
                "import-cae: schema='{}', table='{}', file='{}', batch-size={}, truncate={}",
                sch, tbl, file, bs, truncate);

        if (jdbcTemplate == null) {
            jdbcTemplate = DataSourceFactory.createJdbcTemplate();
        }

        CaeImporter importer = new CaeImporter(jdbcTemplate);
        importer.prepareSchema(sch, tbl);
        int imported = importer.importFile(file, sch, tbl, bs, truncate);

        LoggerFactory.getLogger(ImportCaeCommand.class).info(
                "import-cae: done, {} entries imported into \"{}\".\"{}\".", imported, sch, tbl);
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
