package sm.selflearn.samskrtam.samcli.io;

import sm.selflearn.samskrtam.samcli.model.MwEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, flat parser for the Cologne Monier-Williams text file (mw.txt).
 *
 * Each dictionary entry begins with a {@code <L>...} header line and ends at
 * {@code <LEND>}. The header line carries the entry id, page/column, key1,
 * key2, optional homonym and entry number. The remaining text (between the
 * header and {@code <LEND>}) is kept verbatim as the entry body — no detailed
 * markup parsing is performed, only what fits a flat table.
 */
public final class MwFileParser {

    private static final Pattern HEADER = Pattern.compile(
            "<L>([^<]*)<pc>([^<]*)<k1>([^<]*)<k2>([^<]*)(?:<h>([^<]*))?<e>([^<]*)(.*)");

    private static final String LEND = "<LEND>";

    private MwFileParser() {
    }

    public static void parse(Path file, Consumer<MwEntry> sink) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            MwEntry current = null;
            StringBuilder body = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<L>")) {
                    if (current != null) {
                        sink.accept(finish(current, body));
                    }
                    Matcher m = HEADER.matcher(line);
                    if (m.matches()) {
                        current = new MwEntry(
                                m.group(1), m.group(2), m.group(3), m.group(4),
                                m.group(5), m.group(6));
                        body = new StringBuilder();
                        if (m.group(7) != null && !m.group(7).isEmpty()) {
                            body.append(m.group(7));
                        }
                    } else {
                        current = null;
                        body = null;
                    }
                } else if (line.contains(LEND)) {
                    if (current != null) {
                        String tail = line.substring(0, line.indexOf(LEND));
                        if (!tail.isEmpty()) {
                            if (body.length() > 0) {
                                body.append('\n');
                            }
                            body.append(tail);
                        }
                        sink.accept(finish(current, body));
                        current = null;
                        body = null;
                    }
                } else if (current != null) {
                    if (body.length() > 0) {
                        body.append('\n');
                    }
                    body.append(line);
                }
            }

            if (current != null) {
                sink.accept(finish(current, body));
            }
        }
    }

    private static MwEntry finish(MwEntry entry, StringBuilder body) {
        entry.setBody(body == null ? "" : body.toString().trim());
        return entry;
    }
}
