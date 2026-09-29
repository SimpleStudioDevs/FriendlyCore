package com.friendlysmp.core.features.commandmaker.util;

import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FilterUtil {
    public static List<String> filter(String partial, Collection<String> options) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(partial, options, result);
        return result;
    }
}
