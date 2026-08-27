package sm.selflearn.samskrtam.samcli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@CommandLine.Command(
        name = "samcli",
        mixinStandardHelpOptions = true,
        version = "samcli 0.0.1",
        description = "Samskrtam console utility for dictionary import and DB record processing.",
        subcommands = {ImportMwCommand.class, ImportCaeCommand.class, RefreshLemmasCommand.class}
)
public class RootCommand implements java.util.concurrent.Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Option(names = {"-i", "--interactive"},
            description = "Force the interactive command menu even when no TTY is detected "
                    + "(e.g. some IDE run consoles).")
    private boolean interactive;

    @Override
    public Integer call() throws Exception {
        CommandLine root = spec.commandLine();
        Map<String, CommandLine> subcommands = spec.subcommands();
        List<String> names = new ArrayList<>(subcommands.keySet());

        boolean hasConsole = System.console() != null;
        // Read input when: real TTY, forced, or there is piped stdin data ready.
        // Without this guard a closed/empty stdin (CI, piped run) would block forever.
        boolean canRead = hasConsole || interactive || System.in.available() > 0;
        if (!canRead) {
            printMenu(names, subcommands);
            return 0;
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        while (true) {
            printMenu(names, subcommands);
            System.out.print("Select a command (number or name, q to quit): ");

            String line = hasConsole
                    ? System.console().readLine()
                    : reader.readLine();
            if (line == null) {
                return 0; // EOF
            }
            line = line.trim();
            if (line.isEmpty() || line.equalsIgnoreCase("q") || line.equalsIgnoreCase("quit")) {
                return 0;
            }

            String[] tokens = line.split("\\s+");
            String chosen = tokens[0];
            String[] rest = Arrays.copyOfRange(tokens, 1, tokens.length);

            Integer selectedIndex = tryParseInt(chosen);
            if (selectedIndex != null && selectedIndex >= 1 && selectedIndex <= names.size()) {
                chosen = names.get(selectedIndex - 1);
            }

            if (!subcommands.containsKey(chosen)) {
                System.out.println("Unknown command: " + chosen + ". Try again.");
                continue;
            }

            String[] args = new String[1 + rest.length];
            args[0] = chosen;
            System.arraycopy(rest, 0, args, 1, rest.length);
            return root.execute(args);
        }
    }

    private void printMenu(List<String> names, Map<String, CommandLine> subcommands) {
        System.out.println();
        System.out.println("Available commands:");
        int idx = 1;
        for (String name : names) {
            CommandSpec cs = subcommands.get(name).getCommandSpec();
            String[] descArr = cs.usageMessage().description();
            String desc = (descArr != null && descArr.length > 0)
                    ? String.join(" ", descArr) : "";
            System.out.printf("  %2d) %-16s %s%n", idx, name, desc);
            idx++;
        }
        System.out.println();
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
