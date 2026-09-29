package com.friendlysmp.core.features.commandmaker.arguments;

import java.util.List;

public record ArgsDefinition(String name, String type, boolean papi, List<String> options) {
}
