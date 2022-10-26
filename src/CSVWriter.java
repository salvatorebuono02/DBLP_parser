import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CSVWriter {

    public static void convertToCSV(List<List<String>> dataLines, String CSVFileName) throws IOException {
        File csvOutputFile = new File(CSVFileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            dataLines.stream()
                    .map(CSVWriter::convertToCSV)
                    .forEach(pw::println);
        }
        // assertTrue(csvOutputFile.exists());
    }

    private static String convertToCSV(List<String> data) {
        String[] array = data.toArray(new String[0]);
        return Stream.of(array)
                .map(CSVWriter::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    private static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}

