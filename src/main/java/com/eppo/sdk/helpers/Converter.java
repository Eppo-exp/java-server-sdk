package com.eppo.sdk.helpers;

import java.util.List;
import java.util.stream.Collectors;

public class Converter {
    public static List<Double> convertToDecimal(List<String> input) {
        return input.stream().map((value) -> Double.parseDouble(value)).collect(Collectors.toList());
    }

    public static List<Boolean> convertToBoolean(List<String> input) {
        return input.stream().map((value) -> Boolean.parseBoolean(value)).collect(Collectors.toList());
    }
}
