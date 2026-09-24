package com.company.shared.csvvat.service;

import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.exception.CsvRowValidationException;
import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.exception.InvalidCsvFormatException;
import com.company.shared.csvvat.model.OrderItem;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Production-ready implementation of {@link CsvReaderService} using Apache Commons CSV.
 * <p>
 * Features:
 * <ul>
 *     <li>Supports explicit dynamic column mapping via {@link CsvColumnMapping}.</li>
 *     <li>Intelligent multilingual alias auto-detection (English and Vietnamese NFD normalization) when mapping is not dynamic.</li>
 *     <li>Reads optional input line total for cross-validation against computed values.</li>
 *     <li>Transparent UTF-8 BOM (Byte Order Mark) detection and stripping.</li>
 *     <li>Strict row-level data validation reporting row numbers, column names, and invalid values.</li>
 *     <li>Graceful skipping of blank or whitespace lines.</li>
 * </ul>
 */
public class CommonsCsvReaderService implements CsvReaderService {

    private static final Logger log = LoggerFactory.getLogger(CommonsCsvReaderService.class);

    private static final Set<String> ITEM_NAME_ALIASES = Set.of(
            "itemname", "item", "product", "productname", "tenmathang", "tensanpham", "mathang", "hanghoa", "description", "name"
    );

    private static final Set<String> QUANTITY_ALIASES = Set.of(
            "quantity", "qty", "soluong", "sl", "amount"
    );

    private static final Set<String> UNIT_PRICE_ALIASES = Set.of(
            "unitprice", "price", "dongia", "gia", "unitcost", "cost"
    );

    private static final Set<String> VAT_ALIASES = Set.of(
            "vat", "vatpercentage", "vatrate", "thuevat", "thue", "phantramvat", "tax", "taxrate"
    );

    private static final Set<String> LINE_TOTAL_ALIASES = Set.of(
            "linetotal", "total", "thanhtien", "tongtien", "subtotal"
    );

    private final CsvColumnMapping columnMapping;

    /**
     * Constructs a reader service with default alias auto-detection.
     */
    public CommonsCsvReaderService() {
        this(null);
    }

    /**
     * Constructs a reader service with customized {@link CsvColumnMapping}.
     *
     * @param columnMapping the column mapping configuration, or null for auto-detect
     */
    public CommonsCsvReaderService(CsvColumnMapping columnMapping) {
        this.columnMapping = columnMapping;
    }

    public CsvColumnMapping getColumnMapping() {
        return columnMapping;
    }

    @Override
    public List<OrderItem> read(InputStream inputStream) throws CsvVatException {
        Objects.requireNonNull(inputStream, "InputStream must not be null");
        try {
            // Transparently detect and discard UTF-8 BOM (0xEF, 0xBB, 0xBF)
            PushbackInputStream pbis = new PushbackInputStream(inputStream, 3);
            byte[] bom = new byte[3];
            int n = pbis.read(bom, 0, 3);
            if (n >= 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF) {
                log.debug("UTF-8 BOM detected and stripped from InputStream.");
            } else if (n > 0) {
                pbis.unread(bom, 0, n);
            }
            return read(new InputStreamReader(pbis, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CsvVatException("Failed to read CSV from InputStream: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OrderItem> read(Reader reader) throws CsvVatException {
        Objects.requireNonNull(reader, "Reader must not be null");

        BufferedReader bufferedReader = (reader instanceof BufferedReader br) ? br : new BufferedReader(reader);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (CSVParser parser = format.parse(bufferedReader)) {
            List<String> rawHeaders = parser.getHeaderNames();
            if (rawHeaders == null || rawHeaders.isEmpty() || rawHeaders.stream().allMatch(String::isBlank)) {
                throw new InvalidCsvFormatException("CSV file is empty or does not contain a header row");
            }

            ResolvedHeaders resolvedHeaders = resolveHeaders(rawHeaders);
            List<OrderItem> items = new ArrayList<>();

            for (CSVRecord record : parser) {
                if (isBlankRecord(record)) {
                    continue;
                }

                int rowNumber = (int) record.getRecordNumber() + 1; // Header is row 1, data starts at row 2

                // 1. Item Name
                String itemName = record.get(resolvedHeaders.itemNameHeader());
                if (itemName == null || itemName.trim().isEmpty()) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.itemNameHeader(), itemName, "Item name must not be blank");
                }
                itemName = itemName.trim();

                // 2. Quantity
                String rawQuantity = record.get(resolvedHeaders.quantityHeader());
                if (rawQuantity == null || rawQuantity.trim().isEmpty()) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.quantityHeader(), rawQuantity, "Quantity must not be blank");
                }
                BigDecimal quantity;
                try {
                    quantity = new BigDecimal(rawQuantity.trim().replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.quantityHeader(), rawQuantity, "Quantity is not a valid number: " + rawQuantity);
                }
                if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.quantityHeader(), rawQuantity, "Quantity cannot be negative: " + quantity);
                }
                if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.quantityHeader(), rawQuantity, "Quantity must be greater than 0");
                }

                // 3. Unit Price
                String rawUnitPrice = record.get(resolvedHeaders.unitPriceHeader());
                if (rawUnitPrice == null || rawUnitPrice.trim().isEmpty()) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.unitPriceHeader(), rawUnitPrice, "Unit price must not be blank");
                }
                BigDecimal unitPrice;
                try {
                    unitPrice = new BigDecimal(rawUnitPrice.trim().replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.unitPriceHeader(), rawUnitPrice, "Unit price is not a valid number: " + rawUnitPrice);
                }
                if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.unitPriceHeader(), rawUnitPrice, "Unit price must not be negative");
                }

                // 4. VAT Percentage
                String rawVat = record.get(resolvedHeaders.vatHeader());
                if (rawVat == null || rawVat.trim().isEmpty()) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.vatHeader(), rawVat, "VAT percentage must not be blank");
                }
                String cleanVat = rawVat.trim();
                if (cleanVat.endsWith("%")) {
                    cleanVat = cleanVat.substring(0, cleanVat.length() - 1).trim();
                }
                BigDecimal vatPercentage;
                try {
                    vatPercentage = new BigDecimal(cleanVat.replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.vatHeader(), rawVat, "VAT percentage is not a valid number: " + rawVat);
                }
                if (vatPercentage.compareTo(BigDecimal.ZERO) < 0) {
                    throw new CsvRowValidationException(rowNumber, resolvedHeaders.vatHeader(), rawVat, "VAT percentage must not be negative");
                }

                // 5. Input Line Total (Optional)
                BigDecimal inputLineTotal = null;
                if (resolvedHeaders.lineTotalHeader() != null) {
                    String rawLineTotal = record.get(resolvedHeaders.lineTotalHeader());
                    if (rawLineTotal != null && !rawLineTotal.trim().isEmpty()) {
                        try {
                            inputLineTotal = new BigDecimal(rawLineTotal.trim().replace(",", ""));
                        } catch (NumberFormatException e) {
                            throw new CsvRowValidationException(rowNumber, resolvedHeaders.lineTotalHeader(), rawLineTotal, "Input line total is not a valid number: " + rawLineTotal);
                        }
                        if (inputLineTotal.compareTo(BigDecimal.ZERO) < 0) {
                            throw new CsvRowValidationException(rowNumber, resolvedHeaders.lineTotalHeader(), rawLineTotal, "Input line total must not be negative");
                        }
                    }
                }

                items.add(new OrderItem(rowNumber, itemName, quantity, unitPrice, vatPercentage, inputLineTotal));
            }

            return items;
        } catch (CsvVatException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new InvalidCsvFormatException("CSV file is empty or contains an invalid header row: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new CsvVatException("I/O error during CSV parsing: " + e.getMessage(), e);
        }
    }

    private ResolvedHeaders resolveHeaders(List<String> headers) {
        boolean isDynamic = columnMapping != null && columnMapping.isDynamic();

        String itemNameCol;
        String quantityCol;
        String unitPriceCol;
        String vatCol;
        String lineTotalCol;

        if (isDynamic) {
            // Dynamic mode: check configured column names strictly
            if (columnMapping.itemNameColumn() != null) {
                itemNameCol = findHeaderByExact(headers, columnMapping.itemNameColumn());
                if (itemNameCol == null) {
                    throw new InvalidCsvFormatException("Configured column for Item Name '" + columnMapping.itemNameColumn() + "' was not found in CSV headers: " + headers);
                }
            } else {
                itemNameCol = findHeaderByAliases(headers, ITEM_NAME_ALIASES);
                if (itemNameCol == null) {
                    throw new InvalidCsvFormatException("Mandatory column 'Item Name' (or alias) was not found in CSV headers: " + headers);
                }
            }

            if (columnMapping.quantityColumn() != null) {
                quantityCol = findHeaderByExact(headers, columnMapping.quantityColumn());
                if (quantityCol == null) {
                    throw new InvalidCsvFormatException("Configured column for Quantity '" + columnMapping.quantityColumn() + "' was not found in CSV headers: " + headers);
                }
            } else {
                quantityCol = findHeaderByAliases(headers, QUANTITY_ALIASES);
                if (quantityCol == null) {
                    throw new InvalidCsvFormatException("Mandatory column 'Quantity' (or alias) was not found in CSV headers: " + headers);
                }
            }

            if (columnMapping.unitPriceColumn() != null) {
                unitPriceCol = findHeaderByExact(headers, columnMapping.unitPriceColumn());
                if (unitPriceCol == null) {
                    throw new InvalidCsvFormatException("Configured column for Unit Price '" + columnMapping.unitPriceColumn() + "' was not found in CSV headers: " + headers);
                }
            } else {
                unitPriceCol = findHeaderByAliases(headers, UNIT_PRICE_ALIASES);
                if (unitPriceCol == null) {
                    throw new InvalidCsvFormatException("Mandatory column 'Unit Price' (or alias) was not found in CSV headers: " + headers);
                }
            }

            if (columnMapping.vatPercentageColumn() != null) {
                vatCol = findHeaderByExact(headers, columnMapping.vatPercentageColumn());
                if (vatCol == null) {
                    throw new InvalidCsvFormatException("Configured column for VAT Percentage '" + columnMapping.vatPercentageColumn() + "' was not found in CSV headers: " + headers);
                }
            } else {
                vatCol = findHeaderByAliases(headers, VAT_ALIASES);
                if (vatCol == null) {
                    throw new InvalidCsvFormatException("Mandatory column 'VAT (%)' (or alias) was not found in CSV headers: " + headers);
                }
            }

            if (columnMapping.lineTotalColumn() != null) {
                lineTotalCol = findHeaderByExact(headers, columnMapping.lineTotalColumn());
                if (lineTotalCol == null) {
                    throw new InvalidCsvFormatException("Configured column for Line Total '" + columnMapping.lineTotalColumn() + "' was not found in CSV headers: " + headers);
                }
            } else {
                lineTotalCol = findHeaderByAliases(headers, LINE_TOTAL_ALIASES);
            }
        } else {
            // Alias Auto-Detect mode
            itemNameCol = findHeaderByAliases(headers, ITEM_NAME_ALIASES);
            if (itemNameCol == null) {
                throw new InvalidCsvFormatException("Mandatory column 'Item Name' (or alias) was not found in CSV headers: " + headers);
            }

            quantityCol = findHeaderByAliases(headers, QUANTITY_ALIASES);
            if (quantityCol == null) {
                throw new InvalidCsvFormatException("Mandatory column 'Quantity' (or alias) was not found in CSV headers: " + headers);
            }

            unitPriceCol = findHeaderByAliases(headers, UNIT_PRICE_ALIASES);
            if (unitPriceCol == null) {
                throw new InvalidCsvFormatException("Mandatory column 'Unit Price' (or alias) was not found in CSV headers: " + headers);
            }

            vatCol = findHeaderByAliases(headers, VAT_ALIASES);
            if (vatCol == null) {
                throw new InvalidCsvFormatException("Mandatory column 'VAT (%)' (or alias) was not found in CSV headers: " + headers);
            }

            lineTotalCol = findHeaderByAliases(headers, LINE_TOTAL_ALIASES);
        }

        return new ResolvedHeaders(itemNameCol, quantityCol, unitPriceCol, vatCol, lineTotalCol);
    }

    static String findHeaderByExact(List<String> headers, String target) {
        if (target == null) {
            return null;
        }
        String cleanTarget = stripBom(target).trim();
        for (String h : headers) {
            if (h != null && stripBom(h).trim().equalsIgnoreCase(cleanTarget)) {
                return h;
            }
        }
        return null;
    }

    private String findHeaderByAliases(List<String> headers, Set<String> aliases) {
        for (String h : headers) {
            if (h != null) {
                String norm = normalizeForAlias(h);
                if (aliases.contains(norm)) {
                    return h;
                }
            }
        }
        return null;
    }

    public static String normalizeForAlias(String input) {
        if (input == null) {
            return "";
        }
        String s = stripBom(input).trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");
        s = s.replace('đ', 'd').replace('Đ', 'D');
        return s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    static String stripBom(String s) {
        if (s != null && s.startsWith("\uFEFF")) {
            return s.substring(1);
        }
        return s;
    }

    private boolean isBlankRecord(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (!record.get(i).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private record ResolvedHeaders(
            String itemNameHeader,
            String quantityHeader,
            String unitPriceHeader,
            String vatHeader,
            String lineTotalHeader
    ) {}
}
