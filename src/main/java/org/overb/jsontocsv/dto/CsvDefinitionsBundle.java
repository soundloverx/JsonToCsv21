package org.overb.jsontocsv.dto;

import java.util.List;

public record CsvDefinitionsBundle(String root, List<CsvColumnDefinition> definitions) {
}
