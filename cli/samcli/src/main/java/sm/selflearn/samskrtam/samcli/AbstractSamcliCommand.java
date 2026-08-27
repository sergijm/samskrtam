package sm.selflearn.samskrtam.samcli;

import picocli.CommandLine.Option;

/**
 * Base class for all samcli subcommands. Only declares the shared {@code --config}
 * option; config resolution/loading lives in {@link config.ConfigService} so it
 * stays out of command code.
 */
public abstract class AbstractSamcliCommand {

    @Option(names = "--config",
            description = "Path to external YAML config (defaults to env SAMCLI_CONFIG or ./samcli.yml).")
    protected String configFile;
}
