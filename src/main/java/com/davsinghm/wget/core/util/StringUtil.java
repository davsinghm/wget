package com.davsinghm.wget.core.util;

import java.util.List;

public class StringUtil {

    public static String join(String delimiter, List<String> elements) {

        StringBuilder result = new StringBuilder();
        int i = 0;
        for (String element : elements) {
            result.append(element);
            if (++i != elements.size())
                result.append(delimiter);
        }
        return result.toString();
    }

    public static String join(String prefix, String delimiter, List<String> elements) {

        StringBuilder result = new StringBuilder();
        int i = 0;
        for (String element : elements) {
            result.append(element);
            if (++i != elements.size())
                result.append(delimiter);
        }
        return result.toString();
    }
}
