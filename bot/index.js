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

// Optional Aster MC images
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
            "Authorization": `Bearer ${API 