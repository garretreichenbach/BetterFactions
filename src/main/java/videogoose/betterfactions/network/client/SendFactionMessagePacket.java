package videogoose.betterfactions.network.client;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.BetterFactions;
import org.schema.game.common.data.player.faction.FactionRelationOffer;
import videogoose.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import videogoose.betterfactions.data.diplomacy.war.CasusBelli;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;
import videogoose.betterfactions.manager.CasusBelliManager;
import videogoose.betterfactions.mixin.CustomRelationType;
import videogoose.betterfactions.data.serializeable.war.WarData;
import videogoose.betterfactions.data.serializeable.war.WarGoalData;
import videogoose.betterfactions.manager.FactionDiplomacyManager;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.utils.FactionMessageUtils;

import java.io.IOException;

/**
 * Sends a Faction Message to server.
 * <p>[CLIENT] -> [SERVER]</p>
 */
public class SendFactionMessagePacket extends Packet {

    private int fromId;
    private int toId;
    private String title;
    private String message;
    private String messageType;

    public SendFactionMessagePacket() {}

    public SendFactionMessagePacket(FactionMessage factionMessage) {
        fromId = factionMessage.fromId;
        toId = factionMessage.toId;
        title = factionMessage.title;
        message = factionMessage.message;
        messageType = factionMessage.messageType.toString();
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        fromId = packetReadBuffer.readInt();
        toId = packetReadBuffer.readInt();
        title = packetReadBuffer.readString();
        message = packetReadBuffer.readString();
        messageType = packetReadBuffer.readString();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeInt(fromId);
        packetWriteBuffer.writeInt(toId);
        packetWriteBuffer.writeString(title);
        packetWriteBuffer.writeString(message);
        packetWriteBuffer.writeString(messageType);
    }

    @Override
    public void processPacketOnClient() {}

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        Faction from = GameCommon.getGameState().getFactionManager().getFaction(fromId);
        Faction to = GameCommon.getGameState().getFactionManager().getFaction(toId);
        if (from == null || to == null) return;

        // Validate sender belongs to the from faction
        if (playerState.getFactionId() != fromId) {
            BetterFactions.getInstance().logWarning("Player " + playerState.getName() + " tried to send diplomatic message for faction " + fromId + " but belongs to " + playerState.getFactionId());
            return;
        }

        // Check faction permissions
        var member = FactionManager.getPlayerFactionMember(playerState.getName());
        FactionMessage.MessageType type = FactionMessage.MessageType.valueOf(messageType);
        if (member != null && !hasPermissionForType(member, type)) {
            BetterFactions.getInstance().logWarning("Player " + playerState.getName() + " lacks permission for " + type.name());
            return;
        }

        switch (type) {
            case DECLARE_WAR -> processWarDeclaration(from, to);
            case NON_AGGRESSION_PACT -> processNonAggressionPact(from, to);
            case OFFER_PEACE -> processPeaceOffer(from, to);
            case DEMAND_CONCESSION -> processDemand(from, to);
            case COUNTER_OFFER -> processCounterOffer(from, to);
            case GUARANTEE_INDEPENDENCE -> processGuarantee(from, to);
            case CANCEL_GUARANTEE -> processBreakGuarantee(from, to);
            case IMPROVE_RELATIONS -> processImproveRelations(from, to);
            case DECREASE_RELATIONS -> processDecreaseRelations(from, to);
            case INSULT -> processInsult(from, to);
            case SEND_GIFT -> processGift(from, to);
            case EMBARGO -> processEmbargo(from, to);
            case BAN_DIPLOMATS -> processBanDiplomats(from, to);
            default -> processStandardMessage(from, to, type);
        }
    }

    private boolean hasPermissionForType(videogoose.betterfactions.data.persistent.faction.FactionMember member, FactionMessage.MessageType type) {
        return switch (type) {
            case DECLARE_WAR -> member.hasPermission("diplomacy.war");
            case ALLIANCE_OFFER, ALLIANCE_BREAK -> member.hasPermission("diplomacy.alliance");
            case NON_AGGRESSION_PACT, CANCEL_NON_AGGRESSION_PACT -> member.hasPermission("diplomacy.nap");
            case OFFER_PEACE -> member.hasPermission("diplomacy.war");
            case DEMAND_CONCESSION -> member.hasPermission("diplomacy.demand");
            case OFFER_TRADE, CANCEL_TRADE -> member.hasPermission("trade.offer");
            case FEDERATION_INVITE, FEDERATION_REQUEST -> member.hasPermission("federation.invite");
            case IMPROVE_RELATIONS, DECREASE_RELATIONS, INSULT, EMBARGO, CANCEL_EMBARGO, BAN_DIPLOMATS -> member.hasPermission("diplomacy.[ANY]");
            case GUARANTEE_INDEPENDENCE, CANCEL_GUARANTEE -> member.hasPermission("diplomacy.[ANY]");
            case SEND_GIFT -> member.hasPermission("diplomacy.[ANY]");
            default -> true; // General messages don't need special permissions
        };
    }

    private void processWarDeclaration(Faction from, Faction to) {
        // Parse war goal from message metadata
        WarGoalData.WarGoalType warGoalType = WarGoalData.WarGoalType.SHOW_SUPERIORITY;
        if (message != null && message.startsWith("[WAR_GOAL:")) {
            int end = message.indexOf(']');
            if (end > 0) {
                String goalName = message.substring("[WAR_GOAL:".length(), end);
                try {
                    warGoalType = WarGoalData.WarGoalType.valueOf(goalName);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Declare war via game's faction system
        from.declareWarAgainstEntity(to.getIdFaction());

        // Create war data
        WarData warData = new WarData(from.getName() + " vs " + to.getName());
        WarGoalData attackerGoal = new WarGoalData(warGoalType, from.getIdFaction(), to.getIdFaction());
        WarGoalData defenderGoal = new WarGoalData(WarGoalData.WarGoalType.DEFEND_SELF, to.getIdFaction(), from.getIdFaction());
        warData.addAttacker(from.getIdFaction(), attackerGoal);
        warData.addDefender(to.getIdFaction(), defenderGoal);

        // Persist war data
        PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), warData);
        PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());

        // Fire diplomacy action
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.DECLARATION_OF_WAR);

        // Check for casus belli — unjustified wars have consequences
        boolean hasCB = CasusBelliManager.hasCB(from.getIdFaction(), to.getIdFaction());
        if (!hasCB) {
            // Unjustified war: opinion penalty with ALL factions
            var factionManager = GameCommon.getGameState().getFactionManager();
            for (Faction faction : factionManager.getFactionCollection()) {
                if (faction.getIdFaction() != from.getIdFaction() && faction.getIdFaction() != to.getIdFaction()) {
                    if (faction.isPlayerFaction() || faction.isNPC()) {
                        FactionDiplomacyManager.forceDiplomacyAction(
                            from.getIdFaction(), faction.getIdFaction(),
                            FactionDiplomacyAction.DiploActionType.UNJUSTIFIED_WAR
                        );
                    }
                }
            }
            // Grant containment CB to factions with low opinion of the aggressor
            CasusBelliManager.onUnjustifiedWar(from.getIdFaction(), to.getIdFaction());
            BetterFactions.getInstance().logInfo(from.getName() + " declared UNJUSTIFIED war on " + to.getName());
        } else {
            // Consume the CB
            for (CasusBelli cb : CasusBelliManager.getAvailableCBs(from.getIdFaction(), to.getIdFaction())) {
                if (cb.type.unlockedWarGoal == warGoalType) {
                    CasusBelliManager.removeCB(from.getIdFaction(), to.getIdFaction(), cb.type);
                    break;
                }
            }
        }

        // Send war declaration message to target faction
        String cleanMessage = message != null && message.contains("]") ? message.substring(message.indexOf(']') + 1) : "";
        String justification = hasCB ? " (Justified)" : " (Unjustified)";
        FactionMessage warMessage = new FactionMessage(from, to, title + justification, cleanMessage, FactionMessage.MessageType.DECLARE_WAR);
        FactionManager.getFactionData(to).addMessage(warMessage);

        BetterFactions.getInstance().logInfo(from.getName() + " declared war on " + to.getName()
            + " with goal: " + warGoalType.displayName + (hasCB ? " (justified)" : " (UNJUSTIFIED)"));
    }

    private void processNonAggressionPact(Faction from, Faction to) {
        // Create a faction relation offer for NAP
        FactionRelationOffer offer = new FactionRelationOffer();
        offer.a = from.getIdFaction();
        offer.b = to.getIdFaction();
        offer.rel = CustomRelationType.NON_AGGRESSION;
        GameCommon.getGameState().getFactionManager().getRelationOffersToAdd().add(offer);

        // Send message to target faction
        FactionMessage napMessage = new FactionMessage(from, to, title, message, FactionMessage.MessageType.NON_AGGRESSION_PACT);
        FactionManager.getFactionData(to).addMessage(napMessage);

        BetterFactions.getInstance().logInfo(from.getName() + " offered non-aggression pact to " + to.getName());
    }

    private void processGuarantee(Faction from, Faction to) {
        // Guarantee independence: notify the guaranteed faction
        FactionMessage guaranteeMsg = new FactionMessage(from, to, title, message, FactionMessage.MessageType.GUARANTEE_INDEPENDENCE);
        FactionManager.getFactionData(to).addMessage(guaranteeMsg);

        // Fire diplomacy action — positive opinion modifier
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.GUARANTEE_INDEPENDENCE);
        BetterFactions.getInstance().logInfo(from.getName() + " guaranteed independence of " + to.getName());
    }

    private void processCounterOffer(Faction from, Faction to) {
        // Counter-offers are delivered as messages with modified terms
        FactionMessage counterMessage = new FactionMessage(from, to, title, message, FactionMessage.MessageType.COUNTER_OFFER);
        FactionManager.getFactionData(to).addMessage(counterMessage);
        BetterFactions.getInstance().logInfo(from.getName() + " sent counter-offer to " + to.getName());
    }

    private void processDemand(Faction from, Faction to) {
        // Deliver demand message to target faction
        FactionMessage demandMessage = new FactionMessage(from, to, title, message, FactionMessage.MessageType.DEMAND_CONCESSION);
        FactionManager.getFactionData(to).addMessage(demandMessage);

        // Fire diplomacy action — demands cause opinion penalty
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.SEND_DEMAND);
        BetterFactions.getInstance().logInfo(from.getName() + " sent demand to " + to.getName());
    }

    private void processPeaceOffer(Faction from, Faction to) {
        // Peace offers carry their demands in the message — deliver to target faction
        FactionMessage peaceMessage = new FactionMessage(from, to, title, message, FactionMessage.MessageType.OFFER_PEACE);
        FactionManager.getFactionData(to).addMessage(peaceMessage);

        // Fire diplomacy action
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.PEACE_OFFER);
        BetterFactions.getInstance().logInfo(from.getName() + " offered peace to " + to.getName());
    }

    private void processBreakGuarantee(Faction from, Faction to) {
        FactionMessage msg = new FactionMessage(from, to,
            from.getName() + " revoked their guarantee of your independence",
            message, FactionMessage.MessageType.CANCEL_GUARANTEE);
        FactionManager.getFactionData(to).addMessage(msg);
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.BREAK_GUARANTEE);
        BetterFactions.getInstance().logInfo(from.getName() + " broke guarantee of " + to.getName());
    }

    private void processImproveRelations(Faction from, Faction to) {
        // Check if the target faction has banned our diplomats
        var diplomacy = FactionDiplomacyManager.getDiplomacy(to.getIdFaction());
        var entity = diplomacy.entities.get((long) from.getIdFaction());
        if (entity != null && entity.hasActiveAction(FactionDiplomacyAction.DiploActionType.BAN_DIPLOMATS)) {
            BetterFactions.getInstance().logInfo(from.getName() + " tried to improve relations with " + to.getName() + " but diplomats are banned");
            return;
        }
        // Timed modifier: apply IMPROVE_RELATIONS action which adds points per tick
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.IMPROVE_RELATIONS);
        FactionMessage msg = new FactionMessage(from, to,
            from.getName() + " is improving relations with you",
            message, FactionMessage.MessageType.IMPROVE_RELATIONS);
        FactionManager.getFactionData(to).addMessage(msg);
        BetterFactions.getInstance().logInfo(from.getName() + " improving relations with " + to.getName());
    }

    private void processDecreaseRelations(Faction from, Faction to) {
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.DECREASE_RELATIONS);
        FactionMessage msg = new FactionMessage(from, to,
            from.getName() + " is actively worsening relations with you",
            message, FactionMessage.MessageType.DECREASE_RELATIONS);
        FactionManager.getFactionData(to).addMessage(msg);
        BetterFactions.getInstance().logInfo(from.getName() + " decreasing relations with " + to.getName());
    }

    private void processInsult(Faction from, Faction to) {
        // Insults use the player's custom message text
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.INSULT);
        String insultTitle = from.getName() + " has insulted " + to.getName();
        FactionMessage msg = new FactionMessage(from, to, insultTitle, message, FactionMessage.MessageType.INSULT);
        FactionManager.getFactionData(to).addMessage(msg);
        // Insults grant the insulted faction a rivalry CB
        CasusBelliManager.addCB(new CasusBelli(CasusBelli.CBType.RIVALRY, to.getIdFaction(), from.getIdFaction()));
        BetterFactions.getInstance().logInfo(from.getName() + " insulted " + to.getName());
    }

    private void processGift(Faction from, Faction to) {
        // Parse credit amount from message metadata
        int amount = 0;
        if (message != null && message.startsWith("[GIFT:")) {
            int end = message.indexOf(']');
            if (end > 0) {
                try {
                    amount = Integer.parseInt(message.substring("[GIFT:".length(), end));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (amount > 0) {
            // Transfer credits from sender's faction shop to receiver's
            var fromShop = from.getShop();
            if (fromShop != null && fromShop.getCredits() >= amount) {
                fromShop.modCredits(-amount);
                var toShop = to.getShop();
                if (toShop != null) toShop.modCredits(amount);
                FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.SEND_GIFT);
                String cleanMsg = message.contains("]") ? message.substring(message.indexOf(']') + 1) : "";
                FactionMessage msg = new FactionMessage(from, to,
                    from.getName() + " sent you a gift of " + amount + " credits",
                    cleanMsg, FactionMessage.MessageType.SEND_GIFT);
                FactionManager.getFactionData(to).addMessage(msg);
                BetterFactions.getInstance().logInfo(from.getName() + " sent " + amount + " credits to " + to.getName());
            } else {
                BetterFactions.getInstance().logWarning(from.getName() + " tried to send gift but insufficient credits");
            }
        }
    }

    private void processBanDiplomats(Faction from, Faction to) {
        // Ban diplomats: prevents the target from using IMPROVE_RELATIONS on us for a duration
        // We fire the action on OUR side (from's diplomacy toward to), so when 'to' tries to
        // improve relations with us, the check finds the active BAN_DIPLOMATS action
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.BAN_DIPLOMATS);
        FactionMessage msg = new FactionMessage(from, to,
            from.getName() + " has banned your diplomats",
            "Your faction's diplomats have been expelled. Improving relations is blocked for a period of time.",
            FactionMessage.MessageType.BAN_DIPLOMATS);
        FactionManager.getFactionData(to).addMessage(msg);
        BetterFactions.getInstance().logInfo(from.getName() + " banned diplomats from " + to.getName());
    }

    private void processEmbargo(Faction from, Faction to) {
        FactionDiplomacyManager.forceDiplomacyAction(from.getIdFaction(), to.getIdFaction(), FactionDiplomacyAction.DiploActionType.EMBARGO);
        FactionMessage msg = new FactionMessage(from, to,
            from.getName() + " has placed an embargo on " + to.getName(),
            message, FactionMessage.MessageType.EMBARGO);
        FactionManager.getFactionData(to).addMessage(msg);
        BetterFactions.getInstance().logInfo(from.getName() + " embargoed " + to.getName());
    }

    private void processStandardMessage(Faction from, Faction to, FactionMessage.MessageType type) {
        if (org.schema.game.common.data.player.faction.FactionManager.isNPCFactionOrPirateOrTrader(toId)) {
            String response = FactionMessageUtils.getResponseMessage(type, to, from);
            FactionMessage factionMessage = new FactionMessage(to, from, "Reply from " + to.getName(), response, FactionMessage.MessageType.REPLY);
            FactionManager.getFactionData(from).addMessage(factionMessage);
        } else {
            FactionMessage factionMessage = new FactionMessage(from, to, title, message, type);
            FactionManager.getFactionData(to).addMessage(factionMessage);
        }
    }
}
