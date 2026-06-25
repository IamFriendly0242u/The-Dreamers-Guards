package net.thedreamers.guards.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.thedreamers.guards.Thedreamers_guards;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.punishment.FlagSession;
import net.thedreamers.guards.punishment.PunishmentExecutor;
import net.thedreamers.guards.punishment.SuspensionManager;
import net.thedreamers.guards.webhook.DiscordWebhook;
import net.fabricmc.loader.api.FabricLoader;

public class GuardsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guards")
                .then(Commands.literal("verify")
                        .then(Commands.argument("version", StringArgumentType.word())
                                .then(Commands.argument("token", StringArgumentType.word())
                                        .then(Commands.argument("modList", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String clientVersion = StringArgumentType.getString(context, "version");
                                                    String token = StringArgumentType.getString(context, "token");
                                                    String modList = StringArgumentType.getString(context, "modList");
                                                    String playerName = player.getScoreboardName();
                                                    String playerUuid = player.getUUID().toString();

                                                    Thedreamers_guards.PENDING_VERIFICATION.remove(player.getUUID());

                                                    MinecraftServer server = context.getSource().getServer();
                                                    server.execute(() -> {
                                                        String serverVersion = FabricLoader.getInstance()
                                                                .getModContainer("thedreamers_guards")
                                                                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                                                                .orElse("1.0.0");

                                                        if (!clientVersion.equals(serverVersion)) {
                                                            Component mismatchMessage = Component.literal(String.format(
                                                                    AntiCheatConfig.getKickVersionMismatchMessage(),
                                                                    serverVersion, clientVersion
                                                            ));
                                                            player.connection.disconnect(mismatchMessage);
                                                            DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "FAILED", "Version Mismatch. Client: " + clientVersion + " | Server: " + serverVersion);
                                                            return;
                                                        }

                                                        if (token.equals("DIRTY_CHEATER")) {
                                                            FlagSession.start(server, player, "Illegal Modifications (Client-Side Checked)");
                                                            DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "FAILED", "Client-Side Scanner flagged illegal mods loaded.");
                                                            return;
                                                        }

                                                        String[] blacklist = AntiCheatConfig.getModBlacklist();
                                                        for (String cheat : blacklist) {
                                                            if (!cheat.trim().isEmpty() && modList.toLowerCase().contains(cheat.toLowerCase())) {
                                                                FlagSession.start(server, player, "Illegal Mod: " + cheat);
                                                                DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "FAILED", "Blacklisted modification signature found: " + cheat);
                                                                return;
                                                            }
                                                        }

                                                        DiscordWebhook.sendVerifyAlert(server, playerName, playerUuid, "SUCCESS", "Passed secure authentication payload validation check.");
                                                    });
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("action")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"kick", "ban"}, builder))
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String adminName = source.getTextName();
                                            if (source.getEntity() instanceof ServerPlayer player) {
                                                NameAndId nameAndId = new NameAndId(player.getGameProfile().id(), player.getGameProfile().name());
                                                if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                                    player.sendSystemMessage(Component.literal("§c[Guards] Nice try, but this command is for admins only!"));
                                                    return 0;
                                                }
                                            }
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            String type = StringArgumentType.getString(ctx, "type").toUpperCase();

                                            if (!FlagSession.resolve(target.getUUID(), type)) {
                                                PunishmentExecutor.execute(source.getServer(), target, "Manual Admin Enforcement", type);
                                            }

                                            DiscordWebhook.sendActionAlert(source.getServer(), adminName, target.getScoreboardName(), type);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Guards] Guards Action Successfully: " + type + " the " + target.getScoreboardName()), false);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("pardon")
                        .then(Commands.argument("playerName", StringArgumentType.word())
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    String adminName = source.getTextName();
                                    if (source.getEntity() instanceof ServerPlayer player) {
                                        NameAndId nameAndId = new NameAndId(player.getGameProfile().id(), player.getGameProfile().name());
                                        if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                            player.sendSystemMessage(Component.literal("§c[Guards] Nice try, but this command is for admins only!"));
                                            return 0;
                                        }
                                    }
                                    String playerName = StringArgumentType.getString(ctx, "playerName");
                                    if (SuspensionManager.pardon(playerName)) {
                                        DiscordWebhook.sendPardonAlert(source.getServer(), adminName, playerName);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Guards] Guards Action Successfully: PARDON the " + playerName), false);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§c[Guards] Player is not in the suspended list!"));
                                        return 0;
                                    }
                                })
                        )
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            String adminName = source.getTextName();
                            if (source.getEntity() instanceof ServerPlayer player) {
                                NameAndId nameAndId = new NameAndId(player.getGameProfile().id(), player.getGameProfile().name());
                                if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                    player.sendSystemMessage(Component.literal("§c[Guards] Nice try, but this command is for admins only!"));
                                    return 0;
                                }
                            }
                            try {
                                AntiCheatConfig.loadConfig();
                                DiscordWebhook.init();
                                DiscordWebhook.sendReloadAlert(source.getServer(), adminName);
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[TheDreamers Guards] Configuration, embeds, and localization reloaded successfully!"), true);
                                return 1;
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("§c[TheDreamers Guards] Failed to reload configuration! Check server console."));
                                e.printStackTrace();
                                return 0;
                            }
                        })
                )
        );
    }
}