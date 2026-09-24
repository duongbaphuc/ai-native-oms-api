package com.company.shared.csvvat.service;

import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.model.OrderCalculationResult;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contract for serializing calculated order results into enriched CSV representations.
 */
public interface CsvWriterService {

    void write(OrderCalculationResult result, Writer writer) throws CsvVatException;

    void write(OrderCalculationResult result, OutputStream outputStream) throws CsvVatException;

    default void write(OrderCalculationResult result, Path path) throws CsvVatException {
        try (OutputStream os = Files.newOutputStream(path)) {
            write(result, os);
        } catch (Exception e) {
            if (e instanceof CsvVatException cve) {
                throw cve;
            }
            throw new CsvVatException("Failed to write CSV to path: " + path, e);
        }
    }

    /**
     * Serializes the calculated order result directly into an in-memory CSV string.
     */
    default String writeToString(OrderCalculationResult result) throws CsvVatException {
        StringWriter sw = new StringWriter();
        write(result, sw);
        return sw.toString();
    }
}
