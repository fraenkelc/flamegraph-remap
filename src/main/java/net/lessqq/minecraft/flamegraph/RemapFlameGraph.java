package net.lessqq.minecraft.flamegraph;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RemapFlameGraph {
    public static final Pattern PATTERN = Pattern.compile(":::(func\\S*)([ <])");
    public static final Logger logger = Logger.getLogger(RemapFlameGraph.class.getName());

    public static void main(String[] args) throws IOException {
        initLogging();

        if (args.length != 2) {
            logger.severe("Missing / wrong arguments: RemapFlameGraph path/to/methods.csv flamegraph.svg");
            System.exit(1);
        }
        Path mapping = new File(args[0]).toPath();
        Path graph = new File(args[1]).toPath();
        logger.info(() -> "Processing '" + graph.toString() + "' using mapping '" + mapping + "'.");
        Map<String, String> map = readMapping(mapping);
        processGraph(graph, map);
        logger.info("done.");
    }

    private static void initLogging() {
        try (InputStream is = RemapFlameGraph.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load logging.properties", e);
        }

    }

    private static void processGraph(Path graph, Map<String, String> mapping) {
        String filename = graph.getFileName().toString();
        if (filename.endsWith(".svg")) {
            filename = filename.substring(0, filename.length() - 4);
        }
        Path destination = graph.resolveSibling(filename + "-remapped.svg");
        try (Stream<String> lines = Files.lines(graph)) {
            Files.write(destination,
                    (Iterable<StringBuffer>) () -> lines.map((String l) -> remapLine(l, mapping)).iterator());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private static StringBuffer remapLine(String l, Map<String, String> mapping) {
        StringBuffer sb = new StringBuffer();
        Matcher m = PATTERN.matcher(l);
        while (m.find()) {
            String searge = mapping.get(m.group(1));
            String replacement;
            if (searge != null) {
                replacement = ":::" + searge + m.group(2);
            } else {
                replacement = m.group();
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb;
    }

    private static Map<String, String> readMapping(Path mapping) {
        Map<String, String> result = new HashMap<>();
        try (Stream<String> lines = Files.lines(mapping);) {
            lines.forEach(line -> {
                String[] parts = line.split(",", 3);
                result.put(parts[0], parts[1]);
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        logger.info(() -> "Loaded " + result.size() + " mappings.");
        return result;
    }
}
