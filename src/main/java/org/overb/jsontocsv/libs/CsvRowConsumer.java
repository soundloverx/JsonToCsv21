package org.overb.jsontocsv.libs;

import com.opencsv.CSVWriter;
import org.overb.jsontocsv.enums.CsvNullStyles;

import java.util.Objects;
import java.util.function.Consumer;

public class CsvRowConsumer {

    public static Consumer<String[]> rowWriter(CSVWriter writer, CsvNullStyles nulLStyle) {
        Objects.requireNonNull(writer, "writer must not be null");
        return row -> {
            if (row == null) {
                writer.writeNext(null);
                return;
            }
            String[] out = new String[row.length];
            for (int i = 0; i < row.length; i++) {
                String original = row[i];
                if (original == null) {
                    out[i] = (nulLStyle == CsvNullStyles.LITERAL_NULL) ? "NULL" : "";
                    continue;
                }
                boolean needsQuote = original.indexOf(',') >= 0 ||
                        original.indexOf('"') >= 0 ||
                        original.indexOf('\t') >= 0 || original.indexOf('\n') >= 0 || original.indexOf('\r') >= 0 ||
                        original.startsWith(" ") || original.endsWith(" ");
                if (needsQuote) {
                    out[i] = "\"" + original.replace("\"", "\"\"") + "\"";
                } else {
                    out[i] = original;
                }
            }
            writer.writeNext(out);
        };
    }
}
