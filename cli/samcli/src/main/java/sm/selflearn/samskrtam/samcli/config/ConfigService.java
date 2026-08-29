package sm.selflearn.samskrtam.samcli.config;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared infrastructure for locating and loading the external samcli YAML config.
 * Not tied to any specific command — inject this bean from any future command.
 */
@Component
public class ConfigService {

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Resolve the config file location:
     * 1) the {@code --config} CLI option, 2) the {@code SAMCLI_CONFIG} env var,
     * 3) the default {@code ./samcli.yml}.
     */
    public Path resolveConfigPath(String configOption) {
        String path = configOption;
        if (isBlank(path)) {
            path = System.getenv("SAMCLI_CONFIG");
        }
        if (isBlank(path)) {
            path = "samcli.yml";
        }
        return Paths.get(path);
    }

    public SamcliConfig loadConfig(String configOption) {
        return ConfigLoader.load(resolveConfigPath(configOption));
    }
}
