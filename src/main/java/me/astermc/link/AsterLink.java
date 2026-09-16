package me.astermc.link;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.geysermc.floodgate.api.FloodgateApi;
import org.slf4j.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;

@Plugin(
        id = "asterlink",
        name = "AsterLink",
        version = "1.0.0",
        description = "Aster MC Discord linking for Velocity, Geyser and Floodgate",
        authors = {"Aster MC"},
        dependencies = {
                @Dependency(id = "floodgate")
        }
)
public class AsterLink {

    private final ProxyServer server;
    private final Logger logger;

    private final SecureRandom random = new SecureRandom();

    private String apiUrl;
    private String apiSecret;

    @Inject
    public AsterLink(
            ProxyServer server,
            Logger logger
    ) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        loadConfig();

        CommandManager commandManager =
                server.getCommandManager();

        commandManager.register(
                commandManager.metaBuilder("link")
                        .plugin(this)
                        .build(),
                new LinkCommand()
        );

        logger.info("AsterLink has been enabled!");
        logger.info("Aster MC Discord linking command registered!");
    }

    private void loadConfig() {

        apiUrl = System.getenv("ASTERLINK_API_URL");
        apiSecret = System.getenv("ASTERLINK_API_SECRET");

        if (apiUrl == null || apiUrl.isBlank()) {
            logger.warn("ASTERLINK_API_URL is not configured!");
        }

        if (apiSecret == null || apiSecret.isBlank()) {
            logger.warn("ASTERLINK_API_SECRET is not configured!");
        }
    }

    private class LinkCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {

            if (!(invocation.source() instanceof Player player)) {

                invocation.source().sendMessage(
                        Component.text(
                                "Only players can use this command."
                        )
                );

                return;
            }

            FloodgateApi floodgate =
                    FloodgateApi.getInstance();

            UUID uuid = player.getUniqueId();

            if (!floodgate.isFloodgatePlayer(uuid)) {

                player.sendMessage(
                        Component.text(
                                "§cAsterLink is currently available for Bedrock players only."
                        )
                );

                return;
            }

            if (apiUrl == null || apiSecret == null) {

                player.sendMessage(
                        Component.text(
                                "§cAsterLink is not configured correctly."
                        )
                );

                return;
            }

            String code = generateCode();

            player.sendMessage(
                    Component.text(
                            "§8§m--------------------------"
                    )
            );

            player.sendMessage(
                    Component.text(
                            "§b§lASTER MC §7| Discord Linking"
                    )
            );

            player.sendMessage(
                    Component.text("")
            );

            player.sendMessage(
                    Component.text(
                            "§fYour linking code:"
                    )
            );

            player.sendMessage(
                    Component.text(
                            "§b§l" + code
                    )
            );

            player.sendMessage(
                    Component.text("")
            );

            player.sendMessage(
                    Component.text(
                            "§7Use §f/link " + code +
                            " §7in the Aster MC Discord."
                    )
            );

            player.sendMessage(
                    Component.text(
                            "§7This code expires in §f5 minutes§7."
                    )
            );

            player.sendMessage(
                    Component.text(
                            "§8§m--------------------------"
                    )
            );

            sendCodeToApi(player, code);
        }
    }

    private String generateCode() {

        return String.format(
                "%06d",
                random.nextInt(1_000_000)
        );
    }

    private void sendCodeToApi(
            Player player,
            String code
    ) {

        server.getScheduler()
                .buildTask(this, () -> {

                    try {

                        URI uri = URI.create(
                                apiUrl +
                                "/api/link/create"
                        );

                        HttpURLConnection connection =
                                (HttpURLConnection)
                                        uri.toURL().openConnection();

                        connection.setRequestMethod("POST");

                        connection.setRequestProperty(
                                "Content-Type",
                                "application/json"
                        );

                        connection.setRequestProperty(
                                "Authorization",
                                "Bearer " + apiSecret
                        );

                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);

                        connection.setDoOutput(true);

                        String json = """
                                {
                                  "minecraftUuid": "%s",
                                  "minecraftUsername": "%s",
                                  "code": "%s"
                                }
                                """.formatted(
                                player.getUniqueId(),
                                escapeJson(player.getUsername()),
                                code
                        );

                        try (OutputStream output =
                                     connection.getOutputStream()) {

                            output.write(
                                    json.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            );
                        }

                        int responseCode =
                                connection.getResponseCode();

                        if (responseCode != 200) {

                            logger.warn(
                                    "AsterLink API returned HTTP {}",
                                    responseCode
                            );

                            player.sendMessage(
                                    Component.text(
                                            "§cCould not create your linking code."
                                    )
                            );
                        }

                        connection.disconnect();

                    } catch (Exception error) {

                        logger.warn(
                                "Could not connect to AsterLink API: {}",
                                error.getMessage()
                        );

                        player.sendMessage(
                                Component.text(
                                        "§cCould not connect to the Aster MC linking service."
                                )
                        );
                    }

                })
                .schedule();
    }

    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}