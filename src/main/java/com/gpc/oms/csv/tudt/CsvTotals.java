package com.gpc.oms.csv.tudt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order CSV totals library (tudt namespace).
 *
 * <p>D-19 overload surface: scanHeaders (String + Path), calculate without
 * csvOut (String + Path), calculate with csvOut (String + Path).
 * Primary return is {@link Totals}; the CSV file is optional side output.
 *
 * <p>Contract lock (01-01): headers case-sensitive exact match after trim,
 * leading UTF-8 BOM stripped, csvOut null means no file side-output,
 * empty or header-only input throws IllegalArgumentException, literal
 * TOTAL product collision is a known v1.1 limitation.
 */
public final class CsvTotals {

    private CsvTotals() {
    }

    public record Totals(BigDecimal goods, BigDecimal vat, BigDecimal payable) {
    }

    public static List<String> scanHeaders(String csv) {
        if (csv == null || csv.strip().isEmpty()) {
            throw new IllegalArgumentException("Empty CSV input");
        }
        String stripped = stripBom(csv);
        try (BufferedReader br = new BufferedReader(new StringReader(stripped))) {
            String line = br.readLine();
            while (line != null && line.strip().isEmpty()) {
                line = br.readLine();
            }
            if (line == null) {
                throw new IllegalArgumentException("Empty CSV input");
            }
            List<String> header = trimAll(parseLine(line));
            for (int i = 0; i < header.size(); i++) {
                if (header.indexOf(header.get(i)) != i) {
                    throw new IllegalArgumentException(
                            "Duplicate header '" + header.get(i) + "' at line 1");
                }
            }
            return header;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read CSV input", e);
        }
    }

    public static List<String> scanHeaders(Path csv) {
        if (csv == null) {
            throw new IllegalArgumentException("CSV path must not be null");
        }
        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String line = br.readLine();
            while (line != null && line.strip().isEmpty()) {
                line = br.readLine();
            }
            if (line == null) {
                throw new IllegalArgumentException("Empty CSV input");
            }
            if (!line.isEmpty() && line.charAt(0) == '﻿') {
                line = line.substring(1);
            }
            List<String> header = trimAll(parseLine(line));
            for (int i = 0; i < header.size(); i++) {
                if (header.indexOf(header.get(i)) != i) {
                    throw new IllegalArgumentException(
                            "Duplicate header '" + header.get(i) + "' at line 1");
                }
            }
            return header;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read CSV file: " + csv, e);
        }
    }

    public static Totals calculate(String csv, Map<String, String> mapping) {
        return calculate(csv, mapping, null);
    }

    public static Totals calculate(Path csv, Map<String, String> mapping) {
        return calculate(csv, mapping, null);
    }

    public static Totals calculate(String csv, Map<String, String> mapping, Path csvOut) {
        if (csv == null || csv.strip().isEmpty()) {
            throw new IllegalArgumentException("Empty CSV input");
        }
        String stripped = stripBom(csv);
        List<String> rawLines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(stripped))) {
            String line;
            while ((line = br.readLine()) != null) {
                rawLines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read CSV input", e);
        }
        return calculateLines(rawLines, mapping, csvOut, false);
    }

    public static Totals calculate(Path csv, Map<String, String> mapping, Path csvOut) {
        if (csv == null) {
            throw new IllegalArgumentException("CSV path must not be null");
        }
        List<String> rawLines = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first && !line.isEmpty() && line.charAt(0) == '﻿') {
                    line = line.substring(1);
                }
                first = false;
                rawLines.add(line);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read CSV file: " + csv, e);
        }
        return calculateLines(rawLines, mapping, csvOut, true);
    }

    private static Totals calculateLines(List<String> rawLines, Map<String, String> mapping,
            Path csvOut, boolean fromPath) {
        int headerNo = -1;
        List<String> header = null;
        for (int i = 0; i < rawLines.size(); i++) {
            if (!rawLines.get(i).strip().isEmpty()) {
                headerNo = i + 1;
                header = trimAll(parseLine(rawLines.get(i)));
                break;
            }
        }
        if (header == null) {
            throw new IllegalArgumentException("Empty CSV input");
        }
        if (!header.isEmpty() && !header.get(0).isEmpty() && header.get(0).charAt(0) == '﻿') {
            header.set(0, header.get(0).substring(1));
        }
        Map<String, Integer> indexByHeader = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            if (indexByHeader.putIfAbsent(header.get(i), i) != null) {
                throw new IllegalArgumentException(
                        "Duplicate header '" + header.get(i) + "' at line " + headerNo);
            }
        }
        Map<String, String> effective = defaultIfEmpty(mapping);
        if (mapping != null && !mapping.isEmpty()) {
            for (String logical : new String[]{"product", "quantity", "unit_price", "vat_rate"}) {
                if (!mapping.containsKey(logical)) {
                    throw new IllegalArgumentException(
                            "Missing mapping for logical field '" + logical + "'");
                }
            }
        }
        int productIdx = resolveIndex(effective, "product", indexByHeader);
        int qtyIdx = resolveIndex(effective, "quantity", indexByHeader);
        int priceIdx = resolveIndex(effective, "unit_price", indexByHeader);
        int vatIdx = resolveIndex(effective, "vat_rate", indexByHeader);

        BigDecimal goods = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal payable = BigDecimal.ZERO;
        List<List<String>> rows = new ArrayList<>();
        List<BigDecimal[]> computed = new ArrayList<>();
        int dataRows = 0;
        for (int i = headerNo; i < rawLines.size(); i++) {
            String line = rawLines.get(i);
            if (line.strip().isEmpty()) {
                continue;
            }
            int rowN = i + 1;
            List<String> cells = trimAll(parseLineWithRow(line, rowN));
            rows.add(cells);
            String qtyStr = cellAt(cells, qtyIdx, rowN, "quantity");
            String priceStr = cellAt(cells, priceIdx, rowN, "unit_price");
            String vatStr = cellAt(cells, vatIdx, rowN, "vat_rate");
            if (qtyStr.isEmpty() || priceStr.isEmpty() || vatStr.isEmpty()) {
                String col = qtyStr.isEmpty() ? "quantity"
                        : priceStr.isEmpty() ? "unit_price" : "vat_rate";
                throw new IllegalArgumentException(
                        "Row " + rowN + ": column '" + col + "' value '' blank numeric cell");
            }
            BigDecimal qty = parseNumeric(qtyStr, rowN, "quantity");
            BigDecimal price = parseNumeric(priceStr, rowN, "unit_price");
            BigDecimal rate = parseNumeric(vatStr, rowN, "vat_rate");
            try {
                qty.toBigIntegerExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException(
                        "Row " + rowN + ": column 'quantity' value '" + qtyStr + "' not an integer", e);
            }
            if (qty.signum() < 0) {
                throw new IllegalArgumentException(
                        "Row " + rowN + ": column 'quantity' value '" + qtyStr + "' must be >= 0");
            }
            if (price.signum() < 0) {
                throw new IllegalArgumentException(
                        "Row " + rowN + ": column 'unit_price' value '" + priceStr + "' must be >= 0");
            }
            rate = normalizeVatRate(rate, rowN, vatStr);
            BigDecimal lineTotal = qty.multiply(price);
            BigDecimal vatAmt = lineTotal.multiply(rate);
            BigDecimal pay = lineTotal.add(vatAmt);
            goods = goods.add(lineTotal);
            vat = vat.add(vatAmt);
            payable = payable.add(pay);
            computed.add(new BigDecimal[]{lineTotal, vatAmt, pay});
            dataRows++;
        }
        if (dataRows == 0) {
            throw new IllegalArgumentException("Header-only CSV input, no data rows");
        }
        Totals totals = new Totals(round2(goods), round2(vat), round2(payable));
        if (csvOut != null) {
            writeResultCsv(csvOut, header, rows, computed, totals, productIdx);
        }
        return totals;
    }

    private static BigDecimal parseNumeric(String raw, int rowN, String logical) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Row " + rowN + ": column '" + logical + "' value '" + raw + "' not a number", e);
        }
    }

    // D-12 costly reinterpretation isolated here so Phase 2 can revisit.
    private static BigDecimal normalizeVatRate(BigDecimal rate, int rowN, String raw) {
        if (rate.signum() < 0) {
            throw new IllegalArgumentException(
                    "Row " + rowN + ": column 'vat_rate' value '" + raw + "' must be >= 0");
        }
        if (rate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Row " + rowN + ": column 'vat_rate' value '" + raw + "' exceeds 100%");
        }
        if (rate.compareTo(BigDecimal.ONE) > 0) {
            return rate.divide(new BigDecimal("100"));
        }
        return rate;
    }

    private static void writeResultCsv(Path csvOut, List<String> header, List<List<String>> rows,
            List<BigDecimal[]> computed, Totals totals, int productIdx) {
        List<String> out = new ArrayList<>();
        List<String> headerOut = new ArrayList<>(header);
        headerOut.add("line_total");
        headerOut.add("vat_amount");
        headerOut.add("payable");
        out.add(joinEscaped(headerOut));
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = new ArrayList<>(rows.get(i));
            while (row.size() < header.size()) {
                row.add("");
            }
            row.add(round2(computed.get(i)[0]).toPlainString());
            row.add(round2(computed.get(i)[1]).toPlainString());
            row.add(round2(computed.get(i)[2]).toPlainString());
            out.add(joinEscaped(row));
        }
        List<String> totalRow = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            totalRow.add(i == productIdx ? "TOTAL" : "");
        }
        totalRow.add(totals.goods().toPlainString());
        totalRow.add(totals.vat().toPlainString());
        totalRow.add(totals.payable().toPlainString());
        out.add(joinEscaped(totalRow));
        String body = String.join("\n", out) + "\n";
        try {
            Path parent = csvOut.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(csvOut, body, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot write CSV output: " + csvOut, e);
        }
    }

    static List<String> parseLine(String line) {
        return parseLineWithRow(line, -1);
    }

    private static List<String> parseLineWithRow(String line, int rowN) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        if (inQuotes) {
            if (rowN > 0) {
                throw new IllegalArgumentException("Row " + rowN + ": unterminated quoted field");
            }
            throw new IllegalArgumentException("Unterminated quoted field");
        }
        return fields;
    }

    static String escape(String f) {
        if (f.contains(",") || f.contains("\"") || f.contains("\n")) {
            return "\"" + f.replace("\"", "\"\"") + "\"";
        }
        return f;
    }

    private static String joinEscaped(List<String> fields) {
        List<String> escaped = new ArrayList<>(fields.size());
        for (String f : fields) {
            escaped.add(escape(f));
        }
        return String.join(",", escaped);
    }

    private static List<String> trimAll(List<String> fields) {
        List<String> out = new ArrayList<>(fields.size());
        for (String f : fields) {
            out.add(f.strip());
        }
        return out;
    }

    private static String cellAt(List<String> cells, int idx, int rowN, String logical) {
        if (idx >= cells.size()) {
            return "";
        }
        return cells.get(idx);
    }

    private static int resolveIndex(Map<String, String> mapping, String logical,
            Map<String, Integer> indexByHeader) {
        String actual = mapping.getOrDefault(logical, logical);
        Integer idx = indexByHeader.get(actual);
        if (idx == null) {
            throw new IllegalArgumentException(
                    "Missing column for logical field '" + logical + "' (header '" + actual + "')");
        }
        return idx;
    }

    private static Map<String, String> defaultIfEmpty(Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            Map<String, String> defaults = new HashMap<>();
            defaults.put("product", "product");
            defaults.put("quantity", "quantity");
            defaults.put("unit_price", "unit_price");
            defaults.put("vat_rate", "vat_rate");
            return defaults;
        }
        return mapping;
    }

    private static String stripBom(String s) {
        if (!s.isEmpty() && s.charAt(0) == '﻿') {
            return s.substring(1);
        }
        return s;
    }

    private static BigDecimal round2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
