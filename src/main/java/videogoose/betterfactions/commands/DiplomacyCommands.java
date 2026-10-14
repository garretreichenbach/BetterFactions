package videogoose.betterfactions.commands;

import api.chat.ChatCommand;
import api.chat.CommandArg;
import api.chat.CommandContext;
import api.mod.StarLoader;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.data.GameServerState;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.Federation;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;

import java.util.Date;
import java.util.function.Consumer;

/**
 * Admin commands. They act through the same manager calls a negotiated agreement ends at, so an
 * admin-made treaty is the same record a player can then see, break or renegotiate.
 */
public final class DiplomacyCommands {

	private DiplomacyCommands() {
	}

	private record Command(String name, String usage, String description, Consumer<CommandContext> run, CommandArg... args) implements ChatCommand {
		@Override
		public String getName() {
			return name;
		}

		//the parser enforces these: a command declaring none refuses any argument
		@Override
		public CommandArg[] getArgs() {
			return args;
		}

		@Override
		public String getDescription() {
			return description + " Usage: /" + name + " " + usage;
		}

		@Override
		public void execute(CommandContext ctx) {
			try {
				run.accept(ctx);
			} catch (RuntimeException e) {
				ctx.reply("Usage: /" + name + " " + usage + " (" + e.getMessage() + ")");
			}
		}
	}

	public static void register(BetterFactions mod) {
		StarLoader.registerCommand(mod, new Command("faction_treaty_sign", "<factionA> <factionB> <type> [termHours]",
				"Signs a treaty between two factions, with no offer and no acceptance.", DiplomacyCommands::sign,
				CommandArg.integer("factionA", "10001"), CommandArg.integer("factionB", "10002"), CommandArg.string("type", "starmade:alliance"), CommandArg.integer("termHours", "48").optional()));
		StarLoader.registerCommand(mod, new Command("faction_treaty_break", "<factionA> <factionB> <type>",
				"Tears up a treaty between two factions, breakable or not.", DiplomacyCommands::breakTreaty,
				CommandArg.integer("factionA", "10001"), CommandArg.integer("factionB", "10002"), CommandArg.string("type", "starmade:alliance")));
		StarLoader.registerCommand(mod, new Command("faction_treaty_list", "<faction>",
				"Lists the treaties a faction holds, and the registered treaty types.", DiplomacyCommands::list,
				CommandArg.integer("faction", "10001")));
		StarLoader.registerCommand(mod, new Command("faction_emblem_clear", "<faction>",
				"Removes a faction's uploaded emblem and tells the faction who did it.", DiplomacyCommands::emblemClear,
				CommandArg.integer("faction", "10001")));
		StarLoader.registerCommand(mod, new Command("faction_federation_list", "",
				"Lists every federation, its leader and its members.", DiplomacyCommands::federationList));
		StarLoader.registerCommand(mod, new Command("faction_federation_dissolve", "<federationId>",
				"Winds up a federation, ending the alliances its membership created.", DiplomacyCommands::federationDissolve,
				CommandArg.integer("federationId", "1")));
		StarLoader.registerCommand(mod, new Command("senate_pause", "[on|off]",
				"DEBUG: nothing the Senate votes on takes effect. A vote that passes waits as if for the leader, the founder's own proposals included, so assent and veto can be tried; one that fails stays open. Assenting still carries it out. No argument flips it. Unpaused, each vote is settled at its next ballot or its close. Resets on restart.",
				ctx -> {
					String[] raw = ctx.rawArgs();
					videogoose.betterfactions.civics.Senate.paused = raw.length == 0 ? !videogoose.betterfactions.civics.Senate.paused : raw[0].equalsIgnoreCase("on");
					ctx.reply("Senate votes are " + (videogoose.betterfactions.civics.Senate.paused ? "paused: passed votes wait for the leader, even the founder's own, until /senate_pause off." : "running again."));
				}, CommandArg.string("onOff", "on").optional()));
		StarLoader.registerCommand(mod, new Command("faction_npc_yield", "<npcFactionId> [on|off]",
				"DEBUG: makes an NPC faction accept every demand made of it (treaties like vassalage, and terms it gives). NPC factions only; resets on restart.", DiplomacyCommands::npcYield,
				CommandArg.integer("npcFactionId", "-10000000"), CommandArg.string("onOff", "on").optional()));
		StarLoader.registerCommand(mod, new Command("yesman", "[on|off]",
				"DEBUG: every NPC faction accepts every treaty and proposal offered to it. Rule checks (permissions, valid terms) still apply. No argument flips it. Resets on restart.",
				ctx -> {
					String[] raw = ctx.rawArgs();
					DiplomacyManager dm = DiplomacyManager.server();
					dm.debugYesman = raw.length == 0 ? !dm.debugYesman : raw[0].equalsIgnoreCase("on");
					ctx.reply(dm.debugYesman ? "[SUCCESS] NPC factions now accept every offer (debug)" : "[SUCCESS] NPC factions judge offers normally again");
				}, CommandArg.string("onOff", "on").optional()));
	}

	private static FactionManager fm() {
		return GameServerState.instance.getFactionManager();
	}

	private static String label(int factionId) {
		Faction f = fm().getFaction(factionId);
		return f != null ? f.getName() : String.valueOf(factionId);
	}

	private static String who(CommandContext ctx) {
		return ctx.getSender() != null ? ctx.getSender().getName() : "ADMIN";
	}

	private static boolean pairExists(CommandContext ctx, int a, int b) {
		if (a == b) {
			ctx.reply("[ERROR] a faction cannot hold a treaty with itself");
			return false;
		}
		for (int id : new int[]{a, b}) {
			if (fm().getFaction(id) == null) {
				ctx.reply("[ERROR] faction ID not found: " + id);
				return false;
			}
		}
		return true;
	}

	private static void sign(CommandContext ctx) {
		int a = ctx.getInt(0);
		int b = ctx.getInt(1);
		String type = ctx.getString(2).trim();
		int hours = ctx.optInt(3, 0);
		if (!pairExists(ctx, a, b)) {
			return;
		}
		if (TreatyTypeRegistry.FEDERATION.equals(type)) {
			ctx.reply("[ERROR] federation treaties follow membership; use the federation commands");
			return;
		}
		long expiresAt = hours > 0 ? System.currentTimeMillis() + hours * 3600000L : 0L;
		if (!DiplomacyManager.server().signTreatyServer(a, b, type, expiresAt, "")) {
			ctx.reply("[ERROR] unknown treaty type: " + type + ". Known: " + TreatyTypeRegistry.list());
			return;
		}
		ctx.reply("[SUCCESS] signed " + type + " between " + label(a) + " and " + label(b));
	}

	private static void breakTreaty(CommandContext ctx) {
		int a = ctx.getInt(0);
		int b = ctx.getInt(1);
		String type = ctx.getString(2).trim();
		if (!pairExists(ctx, a, b)) {
			return;
		}
		if (TreatyTypeRegistry.FEDERATION.equals(type)) {
			ctx.reply("[ERROR] federation treaties follow membership; use the federation commands");
			return;
		}
		if (!DiplomacyManager.server().breakTreatyServer(a, b, type)) {
			ctx.reply("[ERROR] no " + type + " in force between " + label(a) + " and " + label(b));
			return;
		}
		ctx.reply("[SUCCESS] broke " + type + " between " + label(a) + " and " + label(b));
	}

	private static void list(CommandContext ctx) {
		int a = ctx.getInt(0);
		if (fm().getFaction(a) == null) {
			ctx.reply("[ERROR] faction ID not found: " + a);
			return;
		}
		ctx.reply("Registered treaty types: " + TreatyTypeRegistry.list());
		int found = 0;
		for (Faction other : fm().getFactionCollection()) {
			for (FactionTreaty t : DiplomacyManager.server().getTreaties(a, other.getIdFaction()).getTreaties()) {
				ctx.reply(t.typeId + ": " + label(t.from) + " -> " + label(t.to)
						+ (t.expiresAt == FactionTreaty.NO_EXPIRY ? " (no term)" : " (expires " + new Date(t.expiresAt) + ")"));
				found++;
			}
		}
		if (found == 0) {
			ctx.reply(label(a) + " holds no treaties");
		}
	}

	private static void emblemClear(CommandContext ctx) {
		Faction f = fm().getFaction(ctx.getInt(0));
		if (f == null) {
			ctx.reply("[ERROR] no faction with id " + ctx.getInt(0));
			return;
		}
		if (DiplomacyManager.server().emblemOf(f).isEmpty()) {
			ctx.reply("[ERROR] " + f.getName() + " has no emblem");
			return;
		}
		DiplomacyManager.server().clearEmblem(f, who(ctx));
		ctx.reply("[SUCCESS] removed emblem of " + f.getName());
	}

	private static void federationList(CommandContext ctx) {
		var all = DiplomacyManager.server().getFederations();
		if (all.isEmpty()) {
			ctx.reply("No federations");
			return;
		}
		for (Federation f : all) {
			StringBuilder b = new StringBuilder("[").append(f.id).append("] ").append(f.name)
					.append(" led by ").append(label(f.leader)).append(":");
			for (int i = 0; i < f.members.size(); i++) {
				b.append(' ').append(label(f.members.getInt(i)));
			}
			for (Federation.FederationVote v : f.votes) {
				b.append("  voting: ").append(DiplomacyManager.server().describeVote(v));
			}
			ctx.reply(b.toString());
		}
	}

	private static void federationDissolve(CommandContext ctx) {
		int id = ctx.getInt(0);
		if (!DiplomacyManager.server().dissolveFederationServer(id)) {
			ctx.reply("[ERROR] no federation with id " + id);
			return;
		}
		ctx.reply("[SUCCESS] dissolved federation " + id);
	}

	private static void npcYield(CommandContext ctx) {
		Faction f = fm().getFaction(ctx.getInt(0));
		if (f == null) {
			ctx.reply("[ERROR] no faction with id " + ctx.getInt(0));
			return;
		}
		String arg = ctx.optString(1, "");
		boolean on = arg.isEmpty() ? !DiplomacyManager.server().isDebugYielding(f.getIdFaction()) : arg.equalsIgnoreCase("on");
		if (!DiplomacyManager.server().setDebugYielding(f, on)) {
			ctx.reply("[ERROR] " + f.getName() + " is not an NPC faction");
			return;
		}
		ctx.reply("[SUCCESS] " + f.getName() + (on ? " now accepts every demand (debug)" : " judges demands normally again"));
	}
}
