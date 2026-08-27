package sm.selflearn.samskrtam.samcli.io;

import sm.selflearn.samskrtam.samcli.cae.CaeEntry;
import sm.selflearn.samskrtam.samcli.cae.CaeEntryParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Минимальный, потоковый парсер файла Cappeller (cae.txt).
 *
 * Каждая словарная запись начинается с {@code <L>...} (заголовочная строка)
 * и заканчивается маркером {@code <LEND>}. Заголовок и тело записи
 * разбираются в {@link CaeEntryParser#parseEntryBlock(String)} — здесь
 * только поблочное чтение файла и делегирование.
 */
public final class CaeFileParser {

    private static final String LEND = "<LEND>";

    private CaeFileParser() {
    }

    public static void parse(Path file, Consumer<CaeEntry> sink) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            StringBuilder block = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("<L>")) {
                    block = new StringBuilder();
                    block.append(line).append('\n');
                } else if (line.contains(LEND)) {
                    if (block != null) {
                        String tail = line.substring(0, line.indexOf(LEND));
                        if (!tail.isEmpty()) {
                            block.append(tail);
                        }
                        CaeEntry e = CaeEntryParser.parseEntryBlock(block.toString());
                        if (e != null) {
                            sink.accept(e);
                        }
                        block = null;
                    }
                } else if (block != null) {
                    block.append(line).append('\n');
                }
            }
        }
    }
}
