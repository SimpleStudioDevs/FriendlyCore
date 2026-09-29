package com.friendlysmp.core.features.commandmaker.arguments;

import org.bukkit.Bukkit;

import java.util.List;

public class ArgVerification {

    public static boolean isInteger(String arg) {
        try {
            Integer.parseInt(arg);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isFloat(String arg) {
        try {
            Float.parseFloat(arg);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean validString(String arg, List<String> options) {
        if (options == null || options.isEmpty()) return true;
        else return options.contains(arg);
    }

    public static boolean validPlayer(String arg) {
        return Bukkit.getPlayerExact(arg) != null;
    }
}
