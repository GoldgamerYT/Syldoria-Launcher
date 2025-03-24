package ch.RP.LauncherLib.Features.Discord;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

public class RPC {

    private long start;
    private DiscordRPC rpc = DiscordRPC.INSTANCE;
    private DiscordEventHandlers handlers;
    private boolean initialized = false;

    public void init() {
        handlers = new DiscordEventHandlers();
        handlers.ready = (user) -> System.out.println("Discord RPC bereit!");
        start = System.currentTimeMillis();

        rpc.Discord_Initialize("1319420052419903601", handlers, true, "");
        initialized = true;
    }

    public static RPC instance() {
        return new RPC();
    }

    public static void update() {
        RPC rpc1 = new RPC();

        if (!rpc1.initialized) {
            rpc1.init();
        }

        DiscordRichPresence richPresence = new DiscordRichPresence();
        richPresence.state = RPCConfig.STATE;
        richPresence.details = RPCConfig.DETAILS;
        richPresence.largeImageKey = RPCConfig.LARGE_IMAGE_KEY;
        richPresence.largeImageText = RPCConfig.LARGE_IMAGE_TEXT;
        richPresence.smallImageKey = RPCConfig.SMALL_IMAGE_KEY;
        richPresence.smallImageText = RPCConfig.SMALL_IMAGE_TEXT;
        richPresence.startTimestamp = rpc1.start;

        rpc1.rpc.Discord_UpdatePresence(richPresence);
    }
}
