package sm.selflearn.samskrtam.samcli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Component
@CommandLine.Command(
        name = "samcli",
        mixinStandardHelpOptions = true,
        version = "samcli 0.0.1",
        description = "Samskrtam console utility for dictionary import and DB record processing.",
        subcommands = {ImportMwCommand.class}
)
public class RootCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
