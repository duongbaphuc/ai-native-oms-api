package com.gpc.oms.csv.tudt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
