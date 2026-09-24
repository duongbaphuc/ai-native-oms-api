package com.company.shared.csvvat.service;

import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.model.OrderCalculationResult;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * High-level orchestration contract coordinating reading, calculating, and writing CSV orders.
 */
public interface OrderCsvProcessor {

    OrderCalculationResult process(Reader reader, Writer writer) throws CsvVatException;

    OrderCalculationResult process(InputStream inputStream, OutputStream outputStream) throws CsvVatException;

    default OrderCalculationResult process(Path inputPath, Path outputPath) throws CsvVatException {
        try (InputStream is = Files.newInputStream(inputPath);
             OutputStream os = Files.newOutputStream(outputPath)) {
            return process(is, os);
        } catch (Exception e) {
            if (e instanceof CsvVatException cve) {
                throw cve;
            }
            throw new CsvVatException("Failed to process order CSV files: " + inputPath + " -> " + outputPath, e);
        }
    }
}
