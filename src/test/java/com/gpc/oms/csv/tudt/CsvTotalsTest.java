package com.gpc.oms.csv.tudt;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
