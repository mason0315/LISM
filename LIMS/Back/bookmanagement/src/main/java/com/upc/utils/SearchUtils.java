package com.upc.utils;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
/**
 * @author xvhaiyang
 * @version 1.0
 * {@code @description:}
 * @since 2025-07-16
 */
public class SearchUtils {
    public static List<String> generateSubstrings(String text, int minLength) {
        List<String> result = new ArrayList<>();
        if (text == null || text.length() < minLength) return result;

        for (int i = 0; i < text.length(); i++) {
            for (int j = i + minLength; j <= text.length(); j++) {
                result.add(text.substring(i, j));
            }
        }

        return result;
    }
}
