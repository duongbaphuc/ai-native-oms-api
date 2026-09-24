package com.company.shared.csvvat.service;

import com.company.shared.csvvat.exception.CsvVatException;
import com.company.shared.csvvat.model.OrderItem;

import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Contract for reading and parsing raw order CSV data into structured domain objects.
 */
public interface CsvReaderService {

    List<OrderItem> read(Reader reader) throws CsvVatException;

    List<OrderItem> read(InputStream inputStream) throws CsvVatException;

    default List<OrderItem> read(Path path) throws CsvVatException {
        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        } catch (Exception e) {
            if (e instanceof CsvVatException cve) {
                throw cve;
            }
            throw new CsvVatException("Failed to read CSV from path: " + path, e);
        }
    }
}
