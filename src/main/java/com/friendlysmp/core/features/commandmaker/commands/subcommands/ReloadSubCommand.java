package com.friendlysmp.core.features.commandmaker.commands.subcommands;

import com.friendlysmp.core.features.commandmaker.CommandFeature;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand {

    public static void runCommand(CommandSender sender, CommandFeature feature) {
        feature.reload();
        sender.sendMessage(Component.text("CommandMaker has been reloaded!", NamedTextColor.GREEN));
    }
}
