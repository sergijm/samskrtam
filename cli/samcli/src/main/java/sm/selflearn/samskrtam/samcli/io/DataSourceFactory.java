package sm.selflearn.samskrtam.samcli.io;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Builds a JDBC {@link JdbcTemplate} from environment variables (set via .env):
 * SPRING_DATASOURCE_URL, DB_USER, DB_PASSWORD. No Spring bean is involved, so
 * running commands like {@code import-mw --help} never opens a connection.
 */
public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static JdbcTemplate createJdbcTemplate() {
        String url = System.getenv("SPRING_DATASOURCE_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_URL is required (set it via .env for samcli).");
        }
        // The .env defines SPRING_DATASOURCE_URL with ${DB_HOST}/${DB_PORT}/${DB_NAME}
        // placeholders. Docker Compose expands them, but direct launches (gradle bootRun)
        // do not, so resolve them here to behave like the other services.
        url = expandPlaceholders(url);
        if (url.contains("${")) {
            throw new IllegalStateException(
                    "SPRING_DATASOURCE_URL still contains unresolved placeholders; "
                            + "ensure DB_HOST, DB_PORT and DB_NAME are set in the environment.");
        }
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(url);
        ds.setUser(user);
        ds.setPassword(password);
        return new JdbcTemplate(ds);
    }

    private static String expandPlaceholders(String value) {
        String result = value;
        for (String key : new String[]{"DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD"}) {
            String envValue = System.getenv(key);
            if (envValue != null) {
                result = result.replace("${" + key + "}", envValue);
            }
        }
        return result;
    }
}
