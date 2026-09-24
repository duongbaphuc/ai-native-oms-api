package com.gpc.oms.csv.tudt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvTotalsTest {

    @Test
    void sampleTwoLines() {
        String csv = "product,quantity,unit_price,vat_rate\n"
                + "Apple,2,10.00,0.10\n"
                + "Banana,1,5.00,10\n";
        CsvTotals.Totals expected = new CsvTotals.Totals(
                new BigDecimal("25.00"), new BigDecimal("2.50"), new BigDecimal("27.50"));

        CsvTotals.Totals identity = CsvTotals.calculate(csv, null);
        assertCompare("goods", expected.goods(), identity.goods());
        assertCompare("vat", expected.vat(), identity.vat());
        assertCompare("payable", expected.payable(), identity.payable());

        String renamed = "TenHang,SoLuong,DonGia,ThueSuat\n"
                + "Apple,2,10.00,0.10\n"
                + "Banana,1,5.00,10\n";
        Map<String, String> mapping = Map.of(
                "product", "TenHang",
                "quantity", "SoLuong",
                "unit_price", "DonGia",
                "vat_rate", "ThueSuat");
        CsvTotals.Totals custom = CsvTotals.calculate(renamed, mapping);
        assertCompare("mapped goods", identity.goods(), custom.goods());
        assertCompare("mapped vat", identity.vat(), custom.vat());
        assertCompare("mapped payable", identity.payable(), custom.payable());

        List<String> headers = CsvTotals.scanHeaders(
                "﻿product,quantity ,unit_price,vat_rate\nApple,2,10.00,0.10\n");
        assertEquals(List.of("product", "quantity", "unit_price", "vat_rate"), headers);

        List<String> cased = CsvTotals.scanHeaders("Product,quantity,unit_price,vat_rate\n");
        assertEquals("Product", cased.get(0));
        assertTrue(cased.contains("Product"));
    }

    private static void assertCompare(String name, BigDecimal expected, BigDecimal actual) {
        assertTrue(expected.compareTo(actual) == 0,
                name + " expected " + expected + " but was " + actual);
    }

    private static IllegalArgumentException assertIae(Runnable r, String name) {
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            return e;
        }
        throw new AssertionError(name + " expected IllegalArgumentException, none thrown");
    }

    @Test
    void rejectsBadInput() {
        IllegalArgumentException empty = assertIae(() -> CsvTotals.calculate("", null), "empty");
        assertTrue(empty.getMessage().contains("Empty CSV"), "empty msg: " + empty.getMessage());

        IllegalArgumentException blank = assertIae(() -> CsvTotals.calculate("   \n  ", null), "whitespace");
        assertTrue(blank.getMessage().contains("Empty CSV"), "blank msg: " + blank.getMessage());

        IllegalArgumentException headerOnly = assertIae(
                () -> CsvTotals.calculate("product,quantity,unit_price,vat_rate\n", null), "header-only");
        assertTrue(headerOnly.getMessage().contains("Header-only"), "header-only msg: " + headerOnly.getMessage());

        Map<String, String> partial = Map.of("product", "product", "quantity", "quantity");
        IllegalArgumentException missingKey = assertIae(
                () -> CsvTotals.calculate("product,quantity,unit_price,vat_rate\nA,1,2,0.1\n", partial),
                "missing-logical-key");
        assertTrue(missingKey.getMessage().contains("unit_price"), "missing key msg: " + missingKey.getMessage());

        Map<String, String> badHeader = Map.of(
                "product", "nope",
                "quantity", "quantity",
                "unit_price", "unit_price",
                "vat_rate", "vat_rate");
        IllegalArgumentException badMap = assertIae(
                () -> CsvTotals.calculate("product,quantity,unit_price,vat_rate\nA,1,2,0.1\n", badHeader),
                "bad-mapping");
        assertTrue(badMap.getMessage().contains("product") && badMap.getMessage().contains("nope"),
                "bad mapping msg: " + badMap.getMessage());

        IllegalArgumentException dup = assertIae(
                () -> CsvTotals.calculate("product,quantity,product,vat_rate\nA,1,2,0.1\n", null), "dup");
        assertTrue(dup.getMessage().contains("Duplicate header"), "dup msg: " + dup.getMessage());

        IllegalArgumentException dupScan = assertIae(
                () -> CsvTotals.scanHeaders("product,quantity,product,vat_rate\n"), "dup-scan");
        assertTrue(dupScan.getMessage().contains("Duplicate header"), "dup scan msg: " + dupScan.getMessage());
    }

    @Test
    void perLineMath() {
        String csv = "product,quantity,unit_price,vat_rate\n"
                + "\"A,B\",2,10.00,0.10\n"
                + "\n"
                + "\"C\"\"D\",1,5.00,10\n";
        CsvTotals.Totals t = CsvTotals.calculate(csv, null);
        assertCompare("goods", new BigDecimal("25.00"), t.goods());
        assertCompare("vat", new BigDecimal("2.50"), t.vat());
        assertCompare("payable", new BigDecimal("27.50"), t.payable());

        CsvTotals.Totals one = CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\nA,1,100,1\n", null);
        CsvTotals.Totals oneDot = CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\nA,1,100,1.0\n", null);
        CsvTotals.Totals hundred = CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\nA,1,100,100\n", null);
        assertCompare("vat 1", new BigDecimal("100.00"), one.vat());
        assertCompare("vat 1.0", new BigDecimal("100.00"), oneDot.vat());
        assertCompare("vat 100", new BigDecimal("100.00"), hundred.vat());

        CsvTotals.Totals frac = CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\nA,1,100,0.08\n", null);
        assertCompare("vat 0.08", new BigDecimal("8.00"), frac.vat());

        IllegalArgumentException blankNum = assertIae(
                () -> CsvTotals.calculate("product,quantity,unit_price,vat_rate\nA,,2,0.1\n", null),
                "blank-numeric");
        assertTrue(blankNum.getMessage().contains("Row 2") && blankNum.getMessage().contains("quantity"),
                "blank numeric msg: " + blankNum.getMessage());

        IllegalArgumentException neg = assertIae(
                () -> CsvTotals.calculate("product,quantity,unit_price,vat_rate\nA,1,-2,0.1\n", null),
                "negative");
        assertTrue(neg.getMessage().contains("Row 2"), "negative msg: " + neg.getMessage());

        IllegalArgumentException badRow = assertIae(
                () -> CsvTotals.calculate("product,quantity,unit_price,vat_rate\nA,1,2,0.1\n\nB,,3,0.1\n", null),
                "physical-rowN");
        assertTrue(badRow.getMessage().contains("Row 4"), "physical Row N msg: " + badRow.getMessage());

        assertThrows(IllegalArgumentException.class, () -> CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\nA,1.5,2,0.1\n", null));
        assertThrows(IllegalArgumentException.class, () -> CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\n\"A,1,2,0.1\n", null));
    }

    @Test
    void summaryRounding() {
        CsvTotals.Totals t = CsvTotals.calculate(
                "product,quantity,unit_price,vat_rate\nA,1,0.335,0\nB,1,0.335,0\n", null);
        assertCompare("goods accumulated-then-rounded", new BigDecimal("0.67"), t.goods());
        assertTrue(new BigDecimal("0.34").add(new BigDecimal("0.34")).compareTo(t.goods()) != 0,
                "must diverge from naive per-line rounding (0.68), was " + t.goods());
        assertEquals(2, t.goods().scale(), "summary scale 2");
        assertEquals(2, t.vat().scale(), "vat scale 2");
        assertEquals(2, t.payable().scale(), "payable scale 2");
    }

    @Test
    void outputCsv(@TempDir Path tmp) throws Exception {
        String csv = "product,quantity,unit_price,vat_rate,note\n"
                + "Apple,2,10.00,0.10,fresh\n"
                + "Banana,1,5.00,10,sweet\n";
        Path out = tmp.resolve("out.csv");
        CsvTotals.Totals t = CsvTotals.calculate(csv, null, out);

        List<String> lines = Files.readAllLines(out, StandardCharsets.UTF_8);
        assertEquals(4, lines.size(), "header + 2 rows + TOTAL");
        assertEquals("product,quantity,unit_price,vat_rate,note,line_total,vat_amount,payable",
                lines.get(0), "header gains three trailing names");
        assertTrue(lines.get(1).contains("fresh"), "extra column passthrough row1: " + lines.get(1));
        assertTrue(lines.get(1).contains("20.00") && lines.get(1).contains("2.00")
                && lines.get(1).contains("22.00"), "row1 formatted scale 2 plain: " + lines.get(1));
        assertTrue(lines.get(2).contains("sweet"), "extra column passthrough row2: " + lines.get(2));
        assertTrue(lines.get(2).contains("5.00") && lines.get(2).contains("0.50")
                && lines.get(2).contains("5.50"), "row2 formatted scale 2 plain: " + lines.get(2));

        String total = lines.get(3);
        assertTrue(total.startsWith("TOTAL,"), "TOTAL literal in product column: " + total);
        assertTrue(total.contains(t.goods().toPlainString())
                && total.contains(t.vat().toPlainString())
                && total.contains(t.payable().toPlainString()),
                "TOTAL row carries summary values: " + total);
        // TOTAL row never quotes (literal TOTAL + empty fillers + plain numbers),
        // so a plain split counts fields exactly; scanHeaders would reject the
        // empty filler cells as duplicate headers.
        String[] totalCells = total.split(",", -1);
        // TOTAL shape: 5 original cols (literal + 4 empty filler incl. note) + 3 totals
        assertEquals(8, totalCells.length, "TOTAL field count: " + total);

        byte[] raw = Files.readAllBytes(out);
        assertTrue(raw.length > 0 && raw[raw.length - 1] == '\n', "trailing newline");

        // Quoted-comma product round-trips with stable field counts.
        String quoted = "product,quantity,unit_price,vat_rate\n\"A,B\",2,10.00,0.10\n";
        Path qout = tmp.resolve("quoted.csv");
        CsvTotals.calculate(quoted, null, qout);
        List<String> qlines = Files.readAllLines(qout, StandardCharsets.UTF_8);
        assertTrue(qlines.get(1).startsWith("\"A,B\","), "quoted field escaped: " + qlines.get(1));
        List<String> qheader = CsvTotals.scanHeaders(Files.readString(qout, StandardCharsets.UTF_8));
        assertEquals(7, qheader.size(), "re-parse field-count stability");
    }

    @Test
    void pathParity(@TempDir Path tmp) throws Exception {
        String csv = "product,quantity,unit_price,vat_rate\n"
                + "Apple,2,10.00,0.10\n"
                + "Banana,1,5.00,10\n";
        Path in = tmp.resolve("in.csv");
        Files.writeString(in, csv, StandardCharsets.UTF_8);

        assertEquals(CsvTotals.scanHeaders(csv), CsvTotals.scanHeaders(in), "scanHeaders parity");

        CsvTotals.Totals fromString = CsvTotals.calculate(csv, null);
        CsvTotals.Totals fromPath = CsvTotals.calculate(in, null);
        assertCompare("parity goods", fromString.goods(), fromPath.goods());
        assertCompare("parity vat", fromString.vat(), fromPath.vat());
        assertCompare("parity payable", fromString.payable(), fromPath.payable());

        // null csvOut writes no file.
        Path noFile = tmp.resolve("never-created.csv");
        CsvTotals.calculate(csv, null, null);
        assertTrue(Files.notExists(noFile), "null csvOut creates no file");

        // csvOut creates parent dirs and overwrites existing target.
        Path nested = tmp.resolve("a/b/result.csv");
        CsvTotals.calculate(in, null, nested);
        assertTrue(Files.exists(nested), "parent dirs created");
        Files.writeString(nested, "junk", StandardCharsets.UTF_8);
        CsvTotals.Totals viaPath = CsvTotals.calculate(in, null, nested);
        List<String> lines = Files.readAllLines(nested, StandardCharsets.UTF_8);
        assertTrue(lines.get(0).endsWith("line_total,vat_amount,payable"), "overwrite: " + lines.get(0));
        assertCompare("csvOut totals match", fromString.goods(), viaPath.goods());

        // Parentless relative path does not NPE.
        Path parentless = Path.of("parentless0103_" + System.nanoTime() + ".csv");
        try {
            CsvTotals.calculate(csv, null, parentless);
            assertTrue(Files.exists(parentless), "parentless target created");
        } finally {
            Files.deleteIfExists(parentless);
        }
    }
}
