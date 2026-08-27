package sm.selflearn.samskrtam.samcli.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    private static final String CLASSPATH_CONFIG = "/samcli.yml";

    private ConfigLoader() {
    }

    public static SamcliConfig load(Path path) {
        if (path != null && Files.exists(path)) {
            try {
                return YAML.readValue(path.toFile(), SamcliConfig.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot read samcli config: " + path, e);
            }
        }
        // Fallback to the bundled classpath default (so the tool works regardless
        // of the current working directory, e.g. when launched via gradle bootRun).
        try (InputStream in = ConfigLoader.class.getResourceAsStream(CLASSPATH_CONFIG)) {
            if (in != null) {
                return YAML.readValue(in, SamcliConfig.class);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read bundled samcli config", e);
        }
        // Neither an external file nor a bundled default was found.
        String tried = (path != null) ? path.toAbsolutePath().toString() : "samcli.yml";
        throw new IllegalStateException(
                "samcli config not found at " + tried
                        + " and no bundled default is present. Provide it via --config or set SAMCLI_CONFIG.");
    }
}
