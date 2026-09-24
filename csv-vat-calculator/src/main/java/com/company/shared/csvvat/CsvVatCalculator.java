package com.company.shared.csvvat;

import com.company.shared.csvvat.config.CalculatorConfig;
import com.company.shared.csvvat.config.CsvColumnMapping;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.model.OrderItem;
import com.company.shared.csvvat.service.CommonsCsvReaderService;
import com.company.shared.csvvat.service.CommonsCsvWriterService;
import com.company.shared.csvvat.service.CsvReaderService;
import com.company.shared.csvvat.service.CsvWriterService;
import com.company.shared.csvvat.service.DefaultVatCalculator;
import com.company.shared.csvvat.service.OrderCsvProcessor;
import com.company.shared.csvvat.service.VatCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Main Facade entry point for the {@code csv-vat-calculator} library (v2.0).
 * <p>
 * Coordinates dynamic CSV reading, strict monetary calculation &amp; line total cross-validation,
 * and enriched CSV writing across multiple output modalities.
 * <p>
 * Supported Output Modes:
 * <ol>
 *     <li><b>File Output:</b> {@link #process(Path, Path)}</li>
 *     <li><b>Stream I/O:</b> {@link #process(InputStream, OutputStream)} or {@link #process(Reader, Writer)}</li>
 *     <li><b>In-Memory String CSV:</b> {@link #processToString(Path)} or {@link #processToString(InputStream)}</li>
 *     <li><b>Pure POJO/Record Result:</b> {@link #processToResult(Path)} or {@link #processToResult(InputStream)}</li>
 * </ol>
 */
public class CsvVatCalculator implements OrderCsvProcessor {

    private static final Logger log = LoggerFactory.getLogger(CsvVatCalculator.class);

    private final CalculatorConfig config;
    private final CsvReaderService readerService;
    private final VatCalculator vatCalculator;
    private final CsvWriterService writerService;

    /**
     * Constructs a calculator with full configuration and default production services.
     *
     * @param config the calculator configuration
     */
    public CsvVatCalculator(CalculatorConfig config) {
        this(
                Objects.requireNonNull(config, "CalculatorConfig must not be null"),
                new CommonsCsvReaderService(config.columnMapping()),
                new DefaultVatCalculator(config),
                new CommonsCsvWriterService(config.columnMapping(), config.writeUtf8Bom())
        );
    }

    /**
     * Constructs a calculator with explicit custom service dependencies.
     */
    public CsvVatCalculator(
            CalculatorConfig config,
            CsvReaderService readerService,
            VatCalculator vatCalculator,
            CsvWriterService writerService
    ) {
        this.config = Objects.requireNonNull(config, "CalculatorConfig must not be null");
        this.readerService = Objects.requireNonNull(readerService, "CsvReaderService must not be null");
        this.vatCalculator = Objects.requireNonNull(vatCalculator, "VatCalculator must not be null");
        this.writerService = Objects.requireNonNull(writerService, "CsvWriterService must not be null");
    }

    /**
     * Creates a new instance using standard production defaults.
     */
    public static CsvVatCalculator createDefault() {
        return builder().build();
    }

    /**
     * Creates a new instance configured with the specified {@link CalculatorConfig}.
     */
    public static CsvVatCalculator create(CalculatorConfig config) {
        return new CsvVatCalculator(config);
    }

    /**
     * Creates a fluent {@link Builder} to customize configuration and behaviors.
     */
    public static Builder builder() {
        return new Builder();
    }

    // =========================================================================
    // Mode 1 & 2: Process to Output (Files, Streams, Writers)
    // =========================================================================

    @Override
    public OrderCalculationResult process(Path inputPath, Path outputPath) throws CsvVatException {
        Objects.requireNonNull(inputPath, "inputPath must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");

        log.info("Processing order CSV from {} to {}", inputPath, outputPath);
        try (InputStream is = Files.newInputStream(inputPath);
             OutputStream os = Files.newOutputStream(outputPath)) {
            return process(is, os);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(outputPath);
            } catch (IOException ignored) {
                // Ignore cleanup error
            }
            if (e instanceof CsvVatException cve) {
                throw cve;
            }
            throw new CsvVatException("Failed to process order CSV files: " + inputPath + " -> " + outputPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads order CSV from an input {@link File} and writes calculation output to an output {@link File}.
     *
     * @param inputFile  the input CSV file
     * @param outputFile the output CSV file
     * @return the calculated order result domain record
     * @throws CsvVatException if reading, calculation, or serialization fails
     */
    public OrderCalculationResult process(File inputFile, File outputFile) throws CsvVatException {
        Objects.requireNonNull(inputFile, "inputFile must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        return process(inputFile.toPath(), outputFile.toPath());
    }

    @Override
    public OrderCalculationResult process(InputStream inputStream, OutputStream outputStream) throws CsvVatException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(outputStream, "outputStream must not be null");

        List<OrderItem> rawItems = readerService.read(inputStream);
        OrderCalculationResult result = vatCalculator.calculateOrder(rawItems);
        writerService.write(result, outputStream);
        return result;
    }

    @Override
    public OrderCalculationResult process(Reader reader, Writer writer) throws CsvVatException {
        Objects.requireNonNull(reader, "reader must not be null");
        Objects.requireNonNull(writer, "writer must not be null");

        List<OrderItem> rawItems = readerService.read(reader);
        OrderCalculationResult result = vatCalculator.calculateOrder(rawItems);
        writerService.write(result, writer);
        return result;
    }

    // =========================================================================
    // Mode 3: In-Memory String CSV (processToString)
    // =========================================================================

    /**
     * Reads order CSV from a file path and serializes the enriched calculation output to a String.
     *
     * @param inputPath path to the input CSV file
     * @return the serialized enriched CSV string
     * @throws CsvVatException if reading, calculation, or serialization fails
     */
    public String processToString(Path inputPath) throws CsvVatException {
        Objects.requireNonNull(inputPath, "inputPath must not be null");
        try (InputStream is = Files.newInputStream(inputPath)) {
            return processToString(is);
        } catch (IOException e) {
            throw new CsvVatException("Failed to read CSV from path: " + inputPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads order CSV from a {@link File} and serializes the enriched calculation output to a String.
     *
     * @param inputFile the source input CSV file
     * @return the serialized enriched CSV string
     * @throws CsvVatException if reading, calculation, or serialization fails
     */
    public String processToString(File inputFile) throws CsvVatException {
        Objects.requireNonNull(inputFile, "inputFile must not be null");
        return processToString(inputFile.toPath());
    }

    /**
     * Reads order CSV from an {@link InputStream} and serializes the enriched calculation output to a String.
     *
     * @param inputStream the source input stream
     * @return the serialized enriched CSV string
     * @throws CsvVatException if reading, calculation, or serialization fails
     */
    public String processToString(InputStream inputStream) throws CsvVatException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        List<OrderItem> rawItems = readerService.read(inputStream);
        OrderCalculationResult result = vatCalculator.calculateOrder(rawItems);
        return writerService.writeToString(result);
    }

    /**
     * Reads order CSV from a {@link Reader} and serializes the enriched calculation output to a String.
     *
     * @param reader the source reader
     * @return the serialized enriched CSV string
     * @throws CsvVatException if reading, calculation, or serialization fails
     */
    public String processToString(Reader reader) throws CsvVatException {
        Objects.requireNonNull(reader, "reader must not be null");
        List<OrderItem> rawItems = readerService.read(reader);
        OrderCalculationResult result = vatCalculator.calculateOrder(rawItems);
        return writerService.writeToString(result);
    }

    // =========================================================================
    // Mode 4: Pure Result POJO/Record (processToResult)
    // =========================================================================

    /**
     * Reads order CSV from a file path and calculates financial totals without CSV serialization overhead.
     *
     * @param inputPath path to the input CSV file
     * @return the calculated order result domain record
     * @throws CsvVatException if reading or calculation fails
     */
    public OrderCalculationResult processToResult(Path inputPath) throws CsvVatException {
        Objects.requireNonNull(inputPath, "inputPath must not be null");
        try (InputStream is = Files.newInputStream(inputPath)) {
            return processToResult(is);
        } catch (IOException e) {
            throw new CsvVatException("Failed to read CSV from path: " + inputPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads order CSV from a {@link File} and calculates financial totals without CSV serialization overhead.
     *
     * @param inputFile the source input CSV file
     * @return the calculated order result domain record
     * @throws CsvVatException if reading or calculation fails
     */
    public OrderCalculationResult processToResult(File inputFile) throws CsvVatException {
        Objects.requireNonNull(inputFile, "inputFile must not be null");
        return processToResult(inputFile.toPath());
    }

    /**
     * Reads order CSV from an {@link InputStream} and calculates financial totals without CSV serialization overhead.
     *
     * @param inputStream the source input stream
     * @return the calculated order result domain record
     * @throws CsvVatException if reading or calculation fails
     */
    public OrderCalculationResult processToResult(InputStream inputStream) throws CsvVatException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        List<OrderItem> rawItems = readerService.read(inputStream);
        return vatCalculator.calculateOrder(rawItems);
    }

    /**
     * Reads order CSV from a {@link Reader} and calculates financial totals without CSV serialization overhead.
     *
     * @param reader the source reader
     * @return the calculated order result domain record
     * @throws CsvVatException if reading or calculation fails
     */
    public OrderCalculationResult processToResult(Reader reader) throws CsvVatException {
        Objects.requireNonNull(reader, "reader must not be null");
        List<OrderItem> rawItems = readerService.read(reader);
        return vatCalculator.calculateOrder(rawItems);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public CalculatorConfig getConfig() {
        return config;
    }

    public CsvReaderService getReaderService() {
        return readerService;
    }

    public VatCalculator getVatCalculator() {
        return vatCalculator;
    }

    public CsvWriterService getWriterService() {
        return writerService;
    }

    // =========================================================================
    // Fluent Builder Pattern
    // =========================================================================

    /**
     * Fluent Builder for constructing configured {@link CsvVatCalculator} instances.
     */
    public static class Builder {
        private final CalculatorConfig.Builder configBuilder = CalculatorConfig.builder();
        private CsvReaderService customReaderService;
        private VatCalculator customVatCalculator;
        private CsvWriterService customWriterService;

        public Builder columnMapping(CsvColumnMapping mapping) {
            configBuilder.columnMapping(mapping);
            return this;
        }

        public Builder discrepancyStrategy(LineTotalDiscrepancyStrategy strategy) {
            configBuilder.discrepancyStrategy(strategy);
            return this;
        }

        public Builder discrepancyTolerance(BigDecimal tolerance) {
            configBuilder.discrepancyTolerance(tolerance);
            return this;
        }

        public Builder currencyScale(int scale) {
            configBuilder.currencyScale(scale);
            return this;
        }

        public Builder roundingMode(RoundingMode mode) {
            configBuilder.roundingMode(mode);
            return this;
        }

        public Builder writeUtf8Bom(boolean writeUtf8Bom) {
            configBuilder.writeUtf8Bom(writeUtf8Bom);
            return this;
        }

        public Builder readerService(CsvReaderService readerService) {
            this.customReaderService = readerService;
            return this;
        }

        public Builder vatCalculator(VatCalculator vatCalculator) {
            this.customVatCalculator = vatCalculator;
            return this;
        }

        public Builder writerService(CsvWriterService writerService) {
            this.customWriterService = writerService;
            return this;
        }

        public CsvVatCalculator build() {
            CalculatorConfig config = configBuilder.build();
            CsvReaderService reader = customReaderService != null
                    ? customReaderService : new CommonsCsvReaderService(config.columnMapping());
            VatCalculator calculator = customVatCalculator != null
                    ? customVatCalculator : new DefaultVatCalculator(config);
            CsvWriterService writer = customWriterService != null
                    ? customWriterService : new CommonsCsvWriterService(config.columnMapping(), config.writeUtf8Bom());
            return new CsvVatCalculator(config, reader, calculator, writer);
        }
    }
}
