package net.thedreamers.guards.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.thedreamers.guards.Thedreamers_guards;
import net.thedreamers.guards.config.AntiCheatConfig;
import net.thedreamers.guards.punishment.PunishmentExecutor;
import net.thedreamers.guards.punishment.SuspensionManager;
import net.thedreamers.guards.punishment.FlagSession;
import net.thedreamers.guards.webhook.DiscordWebhook;
import net.thedreamers.lib.anticheat.AdminCommandCore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class GuardsCommand {

    private static final AdminCommandCore ADMIN_CORE = new AdminCommandCore(SuspensionManager.getEngine(), Thedreamers_guards.CONFIG_ENGINE);

    private static String cleanFormatting(String input) {
        if (input == null) return "";
        return input.replace("\\u00A7", "§").replace("&", "§").replace("\\n", "\n");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guards")
                .then(Commands.literal("action")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FlagSession.getActiveFlaggedNames(), builder))
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"kick", "ban"}, builder))
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String adminName = source.getTextName();
                                            if (source.getEntity() instanceof ServerPlayer sp) {
                                                NameAndId nameAndId = new NameAndId(sp.getGameProfile().id(), sp.getGameProfile().name());
                                                if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                                    String notOpMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.not_op", "§c[Guards] Nice try, but this command is for admins only!");
                                                    sp.sendSystemMessage(Component.literal(cleanFormatting(notOpMsg)));
                                                    return 0;
                                                }
                                            }
                                            String targetName = StringArgumentType.getString(ctx, "target");
                                            String type = StringArgumentType.getString(ctx, "type").toUpperCase();
                                            ServerPlayer targetPlayer = null;
                                            for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                                                if (p.getScoreboardName().equalsIgnoreCase(targetName)) {
                                                    targetPlayer = p;
                                                    break;
                                                }
                                            }
                                            if (targetPlayer == null) {
                                                String offlineMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.player_offline", "§c[Guards] Player is not online or active!");
                                                source.sendFailure(Component.literal(cleanFormatting(offlineMsg)));
                                                return 0;
                                            }
                                            String defaultReason = "KICK".equalsIgnoreCase(type) ?
                                                    Thedreamers_guards.CONFIG_ENGINE.getLanguageString("kick.action_kick", "§c[The Dreamers Guards]\n\n§6■ PROTECTION SYSTEM ALERT ■\n§7You have been disconnected from the dreamscape realm.\n\n§6» Status: §eAutomated Security Sweep Enforcement") :
                                                    Thedreamers_guards.CONFIG_ENGINE.getLanguageString("kick.action_ban", "§c[The Dreamers Guards]\n\n§4■ CRITICAL BANISHMENT PROCLAMATION ■\n§7Your access rights to this realm have been permanently severed.\n\n§4» Status: §eThe Core Matrix Integrity Protection Violation");
                                            PunishmentExecutor.execute(source.getServer(), targetPlayer, defaultReason, type);
                                            DiscordWebhook.sendActionAlert(source.getServer(), adminName, targetPlayer.getScoreboardName(), type, defaultReason);
                                            String successMsg = String.format(Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.action_success", "§a[Guards] Guards Action Successfully: %s the %s"), type, targetPlayer.getScoreboardName());
                                            source.sendSuccess(() -> Component.literal(cleanFormatting(successMsg)), false);
                                            return 1;
                                        })
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    CommandSourceStack source = ctx.getSource();
                                                    String adminName = source.getTextName();
                                                    if (source.getEntity() instanceof ServerPlayer sp) {
                                                        NameAndId nameAndId = new NameAndId(sp.getGameProfile().id(), sp.getGameProfile().name());
                                                        if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                                            String notOpMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.not_op", "§c[Guards] Nice try, but this command is for admins only!");
                                                            sp.sendSystemMessage(Component.literal(cleanFormatting(notOpMsg)));
                                                            return 0;
                                                        }
                                                    }
                                                    String targetName = StringArgumentType.getString(ctx, "target");
                                                    String type = StringArgumentType.getString(ctx, "type").toUpperCase();
                                                    String customReason = StringArgumentType.getString(ctx, "reason");
                                                    ServerPlayer targetPlayer = null;
                                                    for (ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
                                                        if (p.getScoreboardName().equalsIgnoreCase(targetName)) {
                                                            targetPlayer = p;
                                                            break;
                                                        }
                                                    }
                                                    if (targetPlayer == null) {
                                                        String offlineMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.player_offline", "§c[Guards] Player is not online or active!");
                                                        source.sendFailure(Component.literal(cleanFormatting(offlineMsg)));
                                                        return 0;
                                                    }
                                                    String formattedReason = "KICK".equalsIgnoreCase(type) ?
                                                            Thedreamers_guards.CONFIG_ENGINE.getLanguageString("kick.action_kick_reason", "§c[The Dreamers Guards]\n\n§6■ ADMINISTRATIVE KICK DROP ■\n§7Enforced by order of the server security board.\n\n§6» Reason: §f%s") :
                                                            Thedreamers_guards.CONFIG_ENGINE.getLanguageString("kick.action_ban_reason", "§c[The Dreamers Guards]\n\n§4■ PERMANENT TERMINATION RECORD ■\n§7Enforced by operational command authority staff.\n\n§4» Status: §f%s");
                                                    String finalReasonText = String.format(formattedReason, customReason);
                                                    if (formattedReason.contains("%s")) {
                                                        PunishmentExecutor.execute(source.getServer(), targetPlayer, finalReasonText, type);
                                                    } else {
                                                        PunishmentExecutor.execute(source.getServer(), targetPlayer, formattedReason + "\n§c» Reason: §f" + customReason, type);
                                                    }
                                                    DiscordWebhook.sendActionAlert(source.getServer(), adminName, targetPlayer.getScoreboardName(), type, customReason);
                                                    String successMsg = String.format(Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.action_success_reason", "§a[Guards] Guards Action Successfully: %s the %s for %s"), type, targetPlayer.getScoreboardName(), customReason);
                                                    source.sendSuccess(() -> Component.literal(cleanFormatting(successMsg)), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("pardon")
                        .then(Commands.argument("playerName", StringArgumentType.word())
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[]{"keep", "remove"}, builder))
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            String adminName = source.getTextName();
                                            if (source.getEntity() instanceof ServerPlayer sp) {
                                                NameAndId nameAndId = new NameAndId(sp.getGameProfile().id(), sp.getGameProfile().name());
                                                if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                                    String notOpMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.not_op", "§c[Guards] Nice try, but this command is for admins only!");
                                                    sp.sendSystemMessage(Component.literal(cleanFormatting(notOpMsg)));
                                                    return 0;
                                                }
                                            }
                                            String playerName = StringArgumentType.getString(ctx, "playerName");
                                            String mode = StringArgumentType.getString(ctx, "mode").toLowerCase();
                                            SuspensionManager.getEngine().load();
                                            int currentPhase = SuspensionManager.getEngine().getStage(playerName);
                                            if (currentPhase >= 4 && "keep".equals(mode)) {
                                                String failedPermanentMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.pardon_failed_permanent", "§c[Guards] Action Failed: Cannot use KEEP mode for a Phase 4 permanently suspended player! Please utilize REMOVE mode to completely purge the record!");
                                                source.sendFailure(Component.literal(cleanFormatting(failedPermanentMsg)));
                                                return 0;
                                            }
                                            SuspensionManager.getEngine().pardon(playerName);
                                            for (UserBanListEntry entry : source.getServer().getPlayerList().getBans().getEntries()) {
                                                NameAndId nid = entry.getUser();
                                                if (nid.name().equalsIgnoreCase(playerName)) {
                                                    source.getServer().getPlayerList().getBans().remove(nid);
                                                    try {
                                                        source.getServer().getPlayerList().getBans().save();
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    SuspensionManager.getEngine().pardon(nid.id().toString());
                                                    break;
                                                }
                                            }
                                            File historyFile = new File("config/TheDreamers_Guards/suspension-history.txt");
                                            if (historyFile.exists()) {
                                                Properties props = new Properties();
                                                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(historyFile), StandardCharsets.UTF_8)) {
                                                    props.load(reader);
                                                    String matchedKey = null;
                                                    for (String key : props.stringPropertyNames()) {
                                                        if (key.equalsIgnoreCase(playerName)) {
                                                            matchedKey = key;
                                                            break;
                                                        }
                                                    }
                                                    if (matchedKey != null) {
                                                        String record = props.getProperty(matchedKey);
                                                        if (record != null && record.contains(",")) {
                                                            String[] tokens = record.split(",");
                                                            SuspensionManager.getEngine().pardon(tokens[1]);
                                                            String savedIp = tokens[2];
                                                            SuspensionManager.getEngine().pardon(savedIp);
                                                            source.getServer().getPlayerList().getIpBans().remove(savedIp);
                                                            try {
                                                                source.getServer().getPlayerList().getIpBans().save();
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                        if ("remove".equals(mode)) {
                                                            props.remove(matchedKey);
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                                if ("remove".equals(mode)) {
                                                    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(historyFile), StandardCharsets.UTF_8)) {
                                                        props.store(writer, "The Dreamers Guards Infraction Tracking History");
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                            DiscordWebhook.sendPardonAlert(source.getServer(), adminName, playerName, mode.toUpperCase());
                                            String pardonSuccessMsg = String.format(Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.pardon_success", "§a[Guards] Guards Action Successfully: PARDON (%s) the %s"), mode.toUpperCase(), playerName);
                                            source.sendSuccess(() -> Component.literal(cleanFormatting(pardonSuccessMsg)), false);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("trust")
                        .then(Commands.argument("playerName", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(FlagSession.getActiveFlaggedNames(), builder))
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    String adminName = source.getTextName();
                                    if (source.getEntity() instanceof ServerPlayer sp) {
                                        NameAndId nameAndId = new NameAndId(sp.getGameProfile().id(), sp.getGameProfile().name());
                                        if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                            String notOpMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.not_op", "§c[Guards] Nice try, but this command is for admins only!");
                                            sp.sendSystemMessage(Component.literal(cleanFormatting(notOpMsg)));
                                            return 0;
                                        }
                                    }
                                    String playerName = StringArgumentType.getString(ctx, "playerName");
                                    Thedreamers_guards.TRUSTED_PLAYERS.add(playerName.toLowerCase());
                                    FlagSession.stopSession(source.getServer(), playerName);
                                    DiscordWebhook.sendActionAlert(source.getServer(), adminName, playerName, "TRUST", "Admin granted temporary trust bypass");
                                    String trustSuccessMsg = String.format(Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.trust_success", "§a[Guards] Countdown halted. %s is permitted to stay temporarily."), playerName);
                                    source.sendSuccess(() -> Component.literal(cleanFormatting(trustSuccessMsg)), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            String adminName = source.getTextName();
                            if (source.getEntity() instanceof ServerPlayer sp) {
                                NameAndId nameAndId = new NameAndId(sp.getGameProfile().id(), sp.getGameProfile().name());
                                if (!source.getServer().getPlayerList().isOp(nameAndId)) {
                                    String notOpMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.not_op", "§c[Guards] Nice try, but this command is for admins only!");
                                    sp.sendSystemMessage(Component.literal(cleanFormatting(notOpMsg)));
                                    return 0;
                                }
                            }
                            AntiCheatConfig.loadConfig();
                            DiscordWebhook.init();
                            ADMIN_CORE.processReload();
                            DiscordWebhook.sendReloadAlert(source.getServer(), adminName);
                            String reloadSuccessMsg = Thedreamers_guards.CONFIG_ENGINE.getLanguageString("admin.reload_success", "§a[TheDreamers Guards] Configuration reloaded successfully!");
                            source.sendSuccess(() -> Component.literal(cleanFormatting(reloadSuccessMsg)), true);
                            return 1;
                        })
                )
        );
    }
}