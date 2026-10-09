package com.friendlysmp.core.features.homes;

import com.earth2me.essentials.IEssentials;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class HomesCommand implements CommandExecutor {
    private final HomesFeature feature;
    private final IEssentials essentials;
    public HomesCommand(HomesFeature feature, IEssentials essentials) {
        this.feature = feature;
        this.essentials = essentials;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] strings) {

        return false;
    }
}
