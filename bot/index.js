const {
    Client,
    GatewayIntentBits,
    REST,
    Routes,
    SlashCommandBuilder,
    EmbedBuilder
} = require("discord.js");

const client = new Client({
    intents: [GatewayIntentBits.Guilds]
});

// ===============================
// ENVIRONMENT VARIABLES
// ===============================

const TOKEN = process.env.DISCORD_TOKEN;
const CLIENT_ID = process.env.DISCORD_CLIENT_ID;
const GUILD_ID = process.env.DISCORD_GUILD_ID;

const API_URL = process.env.API_URL;
const API_SECRET = process.env.API_SECRET;

const LINKED_ROLE_ID = process.env.LINKED_ROLE_ID;

const ASTER_LOGO = process.env.ASTER_LOGO_URL || "";
const ASTER_BANNER = process.env.ASTER_BANNER_URL || "";

// ===============================
// SLASH COMMANDS
// ===============================

const commands = [
    new SlashCommandBuilder()
        .setName("link")
        .setDescription("Link your Aster MC Minecraft account")
        .addStringOption(option =>
            option
                .setName("code")
                .setDescription("Your 6-digit Minecraft linking code")
                .setRequired(true)
        ),

    new SlashCommandBuilder()
        .setName("profile")
        .setDescription("View your linked Aster MC account"),

    new SlashCommandBuilder()
        .setName("linkinfo")
        .setDescription("View your Aster MC linking information")
].map(command => command.toJSON());
// ===============================
// REGISTER COMMANDS
// ===============================

async function registerCommands() {
    const rest = new REST({ version: "10" }).setToken(TOKEN);

    await rest.put(
        Routes.applicationGuildCommands(CLIENT_ID, GUILD_ID),
        {
            body: commands
        }
    );

    console.log("Aster MC slash commands registered!");
}

// ===============================
// API REQUEST
// ===============================

async function apiRequest(endpoint, method, body = null) {

    const options = {
        method,
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${API_SECRET}`
        }
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(
        `${API_URL}${endpoint}`,
        options
    );

    let data;

    try {
        data = await response.json();
    } catch {
        data = {};
    }

    return {
        status: response.status,
        data
    };
}

// ===============================
// ASTER MC EMBED
// ===============================

function createEmbed(title, description) {

    const embed = new EmbedBuilder()
        .setColor(0x111111)
        .setTitle(title)
        .setDescription(description)
        .setFooter({
            text: "Aster MC • Minecraft Network"
        })
        .setTimestamp();

    if (ASTER_LOGO) {
        embed.setThumbnail(ASTER_LOGO);
    }

    if (ASTER_BANNER) {
        embed.setImage(ASTER_BANNER);
    }

    return embed;
}
// ===============================
// BOT READY
// ===============================

client.once("ready", async () => {

    console.log(`Aster MC Bot online as ${client.user.tag}`);

    try {

        await registerCommands();

    } catch (error) {

        console.error(
            "Failed to register commands:",
            error
        );
    }
});

// ===============================
// INTERACTION HANDLER
// ===============================

client.on("interactionCreate", async interaction => {

    if (!interaction.isChatInputCommand()) {
        return;
    }

    // ===========================
    // /link COMMAND
    // ===========================

    if (interaction.commandName === "link") {

        const code = interaction.options
            .getString("code", true)
            .trim();

        // Check code format

        if (!/^\d{6}$/.test(code)) {

            const embed = createEmbed(
                "❌ Invalid Code",
                "Please enter a valid **6-digit** Minecraft linking code."
            );

            return interaction.reply({
                embeds: [embed],
                ephemeral: true
            });
        }

        // Defer while contacting API

        await interaction.deferReply({
            ephemeral: true
        });

        try {

            const result = await apiRequest(
                "/api/link/verify",
                "POST",
                {
                    discordId: interaction.user.id,
                    code: code
                }
            );
            // ===========================
            // SUCCESSFUL LINK
            // ===========================

            if (result.data.success) {

                // Give the Linked role

                if (LINKED_ROLE_ID) {

                    try {

                        const member =
                            await interaction.guild.members.fetch(
                                interaction.user.id
                            );

                        await member.roles.add(
                            LINKED_ROLE_ID
                        );

                    } catch (roleError) {

                        console.error(
                            "Could not give Linked role:",
                            roleError
                        );
                    }
                }

                const username =
                    result.data.minecraftUsername;

                const embed = createEmbed(
                    "⛏️ Aster MC",
                    `## Account Linked ✓\n\n` +
                    `Your Discord account has been successfully linked to your Minecraft account.\n\n` +
                    `**Minecraft Username**\n` +
                    `\`${username}\`\n\n` +
                    `**Status**\n` +
                    `Linked ✓`
                );

                return interaction.editReply({
                    embeds: [embed]
                });
            }

            // ===========================
            // INVALID / EXPIRED CODE
            // ===========================

            if (result.status === 404) {

                const embed = createEmbed(
                    "❌ Invalid or Expired Code",
                    "That linking code is invalid or has expired.\n\n" +
                                        "Use **/link** in Minecraft to generate a new code."
                );

                return interaction.editReply({
                    embeds: [embed]
                });
            }

            // ===========================
            // OTHER API ERRORS
            // ===========================

            const embed = createEmbed(
                "❌ Linking Failed",
                "Something went wrong while linking your account.\n\n" +
                "Please try again."
            );

            return interaction.editReply({
                embeds: [embed]
            });
        } catch (error) {

            console.error("Link error:", error);

            const embed = createEmbed(
                "❌ Service Error",
                "The Aster MC linking service could not be reached.\n\n" +
                "Please try again later."
            );

            return interaction.editReply({
                embeds: [embed]
            });
        }
    }
    // ===========================
    // /profile COMMAND
    // ===========================

    if (interaction.commandName === "profile") {

        await interaction.deferReply({
            ephemeral: true
        });

        try {

            const result = await apiRequest(
                `/api/link/${interaction.user.id}`,
                "GET"
            );

            if (!result.data.linked) {

                const embed = createEmbed(
                    "⛏️ Aster MC",
                    "## Not Linked\n\n" +
                    "Your Discord account is not linked to a Minecraft account.\n\n" +
                    "Use **/link** after generating a code in Minecraft."
                );

                return interaction.editReply({
                    embeds: [embed]
                });
            }

            const embed = createEmbed(
                "⛏️ Aster MC",
                `## Minecraft Profile\n\n` +
                `**Username**\n` +
                `\`${result.data.minecraftUsername}\`\n\n` +
                `**UUID**\n` +
                `\`${result.data.minecraftUuid}\`\n\n` +
                `**Status**\n` +
                `Linked ✓`
            );

            return interaction.editReply({
                embeds: [embed]
            });

        } catch (error) {

            console.error("Profile error:", error);

            const embed = createEmbed(
                "❌ Service Error",
                "The Aster MC linking service could not be reached."
            );

            return interaction.editReply({
                embeds: [embed]
            });
        }
    }
    // ===========================
    // /linkinfo COMMAND
    // ===========================

    if (interaction.commandName === "linkinfo") {

        await interaction.deferReply({
            ephemeral: true
        });

        try {

            const result = await apiRequest(
                `/api/link/${interaction.user.id}`,
                "GET"
            );

            if (!result.data.linked) {

                const embed = createEmbed(
                    "⛏️ Aster MC",
                    "You currently don't have a linked Minecraft account."
                );

                return interaction.editReply({
                    embeds: [embed]
                });
            }

            const linkedAt = result.data.linkedAt
                ? `<t:${Math.floor(
                    new Date(result.data.linkedAt).getTime() / 1000
                )}:F>`
                : "Unknown";

            const embed = createEmbed(
                "⛏️ Aster MC",
                `## Link Information\n\n` +
                `**Minecraft**\n` +
                `\`${result.data.minecraftUsername}\`\n\n` +
                `**Minecraft UUID**\n` +
                `\`${result.data.minecraftUuid}\`\n\n` +
                `**Discord**\n` +
                `<@${interaction.user.id}>\n\n` +
                `**Linked At**\n` +
                `${linkedAt}`
            );

            return interaction.editReply({
                embeds: [embed]
            });

        } catch (error) {

            console.error("Link info error:", error);

            const embed = createEmbed(
                "❌ Service Error",
                "The Aster MC linking service could not be reached."
            );

            return interaction.editReply({
                embeds: [embed]
            });
        }
    }
});

// ===============================
// LOGIN
// ===============================

if (!TOKEN) {
    console.error("DISCORD_TOKEN is missing!");
    process.exit(1);
}

if (!CLIENT_ID) {
    console.error("DISCORD_CLIENT_ID is missing!");
    process.exit(1);
}

if (!GUILD_ID) {
    console.error("DISCORD_GUILD_ID is missing!");
    process.exit(1);
}

if (!API_URL) {
    console.error("API_URL is missing!");
    process.exit(1);
}

if (!API_SECRET) {
    console.error("API_SECRET is missing!");
    process.exit(1);
}

client.login(TOKEN);