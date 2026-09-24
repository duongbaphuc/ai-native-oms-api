package com.company.shared.csvvat.service;

import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.model.CalculatedItem;
import com.company.shared.csvvat.model.OrderCalculationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Production-ready implementation of {@link CsvWriterService} using Apache Commons CSV.
 * <p>
 * Generates enriched CSV content in UTF-8 format containing original order attributes,
 * computed line items, and three concluding summary rows aligned with accounting standards.
 */
public class CommonsCsvWriterService implements CsvWriterService {

    public static final String DEFAULT_ITEM_NAME_HEADER = "Item Name";
    public static final String DEFAULT_QUANTITY_HEADER = "Quantity";
    public static final String DEFAULT_UNIT_PRICE_HEADER = "Unit Price";
    public static final String DEFAULT_VAT_HEADER = "VAT (%)";
    public static final String HEADER_LINE_SUBTOTAL = "Line Subtotal";
    public static final String HEADER_ITEM_VAT = "Item VAT";
    public static final String DEFAULT_LINE_TOTAL_HEADER = "Line Total";

    public static final String LABEL_SUBTOTAL = "TỔNG TIỀN TRƯỚC THUẾ (SUBTOTAL)";
    public static final String LABEL_TOTAL_VAT = "TỔNG THUẾ VAT (TOTAL VAT)";
    public static final String LABEL_GRAND_TOTAL = "TỔNG THANH TOÁN (GRAND TOTAL)";

    private final CsvColumnMapping columnMapping;
    private final boolean writeUtf8Bom;

    /**
     * Constructs a writer service using standard default column headers and UTF-8 BOM enabled.
     */
    public CommonsCsvWriterService() {
        this(CsvColumnMapping.defaultMapping(), true);
    }

    /**
     * Constructs a writer service with custom column header mapping and UTF-8 BOM enabled.
     *
     * @param columnMapping the column mapping configuration
     */
    public CommonsCsvWriterService(CsvColumnMapping columnMapping) {
        this(columnMapping, true);
    }

    /**
     * Constructs a writer service with custom column mapping and configurable UTF-8 BOM behavior.
     *
     * @param columnMapping the column mapping configuration
     * @param writeUtf8Bom  whether to write the UTF-8 BOM (0xEF, 0xBB, 0xBF) to binary streams
     */
    public CommonsCsvWriterService(CsvColumnMapping columnMapping, boolean writeUtf8Bom) {
        this.columnMapping = columnMapping != null ? columnMapping : CsvColumnMapping.defaultMapping();
        this.writeUtf8Bom = writeUtf8Bom;
    }

    public CsvColumnMapping getColumnMapping() {
        return columnMapping;
    }

    public boolean isWriteUtf8Bom() {
        return writeUtf8Bom;
    }

    @Override
    public void write(OrderCalculationResult result, OutputStream outputStream) throws CsvVatException {
        Objects.requireNonNull(result, "OrderCalculationResult must not be null");
        Objects.requireNonNull(outputStream, "OutputStream must not be null");

        try {
            if (writeUtf8Bom) {
                // Write standard UTF-8 BOM (0xEF, 0xBB, 0xBF)
                // Ensures Microsoft Excel on Windows recognizes UTF-8 encoding and renders
                // Vietnamese diacritics/accents flawlessly without font distortion.
                outputStream.write(0xEF);
                outputStream.write(0xBB);
                outputStream.write(0xBF);
            }

            OutputStreamWriter osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            write(result, bw);
            bw.flush();
        } catch (IOException e) {
            throw new CsvVatException("Failed to write CSV to OutputStream: " + e.getMessage(), e);
        }
    }

    @Override
    public void write(OrderCalculationResult result, Writer writer) throws CsvVatException {
        Objects.requireNonNull(result, "OrderCalculationResult must not be null");
        Objects.requireNonNull(writer, "Writer must not be null");

        String[] headers = getHeaders();
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader(headers)
                    .build();

            CSVPrinter printer = new CSVPrinter(writer, format);

            for (CalculatedItem item : result.items()) {
                printer.printRecord(
                        item.itemName(),
                        item.quantity().toPlainString(),
                        item.unitPrice().toPlainString(),
                        item.vatPercentage().toPlainString(),
                        item.lineSubtotal().toPlainString(),
                        item.vatAmount().toPlainString(),
                        item.lineTotalWithVat().toPlainString()
                );
            }

            // Concluding Accounting Summary Rows
            printer.printRecord(LABEL_SUBTOTAL, "", "", "", result.subtotal().toPlainString(), "", "");
            printer.printRecord(LABEL_TOTAL_VAT, "", "", "", "", result.totalVat().toPlainString(), "");
            printer.printRecord(LABEL_GRAND_TOTAL, "", "", "", "", "", result.grandTotal().toPlainString());

            printer.flush();
        } catch (IOException e) {
            throw new CsvVatException("Failed to serialize CSV records: " + e.getMessage(), e);
        }
    }

    @Override
    public String writeToString(OrderCalculationResult result) throws CsvVatException {
        Objects.requireNonNull(result, "OrderCalculationResult must not be null");
        StringWriter sw = new StringWriter();
        write(result, sw);
        return sw.toString();
    }

    private String[] getHeaders() {
        return new String[] {
                defaultIfBlank(columnMapping.itemNameColumn(), DEFAULT_ITEM_NAME_HEADER),
                defaultIfBlank(columnMapping.quantityColumn(), DEFAULT_QUANTITY_HEADER),
                defaultIfBlank(columnMapping.unitPriceColumn(), DEFAULT_UNIT_PRICE_HEADER),
                defaultIfBlank(columnMapping.vatPercentageColumn(), DEFAULT_VAT_HEADER),
                HEADER_LINE_SUBTOTAL,
                HEADER_ITEM_VAT,
                defaultIfBlank(columnMapping.lineTotalColumn(), DEFAULT_LINE_TOTAL_HEADER)
        };
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
