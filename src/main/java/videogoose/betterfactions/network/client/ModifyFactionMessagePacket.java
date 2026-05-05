package videogoose.betterfactions.network.client;

import api.common.GameCommon;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRelationOffer;
import org.schema.game.common.data.player.faction.FactionRelationOfferAcceptOrDecline;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import videogoose.betterfactions.data.persistent.faction.FactionData;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;
import videogoose.betterfactions.manager.FactionDiplomacyManager;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.mixin.CustomRelationType;

import java.io.IOException;

/**
 * Modifies a faction message (read/unread/delete/accept/deny).
 */
public class ModifyFactionMessagePacket extends Packet {

    private FactionMessage message;
    private int mode;

    public ModifyFactionMessagePacket() {}

    public ModifyFactionMessagePacket(FactionMessage message, int mode) {
        this.message = message;
        this.mode = mode;
    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
        message = packetReadBuffer.readObject(FactionMessage.class);
        mode = packetReadBuffer.readInt();
    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
        packetWriteBuffer.writeObject(message);
        packetWriteBuffer.writeInt(mode);
    }

    @Override
    public void processPacketOnClient() {}

    @Override
    public void processPacketOnServer(PlayerState playerState) {
        FactionData factionData = FactionManager.getFactionData(message.toId);
        if (factionData == null) return;

        switch (mode) {
            case FactionMessage.MARK_READ -> {
                message.read = true;
                FactionManager.updateData(message);
            }
            case FactionMessage.MARK_UNREAD -> {
                message.read = false;
                FactionManager.updateData(message);
            }
            case FactionMessage.DELETE -> {
                factionData.removeMessage(message);
                FactionManager.updateData(factionData);
            }
            case FactionMessage.ACCEPT -> {
                processDiplomaticAccept(playerState);
                factionData.removeMessage(message);
                FactionManager.updateData(factionData);
            }
            case FactionMessage.DENY -> {
                processDiplomaticDeny(playerState);
                factionData.removeMessage(message);
                FactionManager.updateData(factionData);
            }
            case FactionMessage.COUNTER -> {
                // Counter-offer: remove original message, the client will open a new dialog
                // to compose counter-terms and send as COUNTER_OFFER
                factionData.removeMessage(message);
                FactionManager.updateData(factionData);
                // The actual counter-offer message is sent via SendFactionMessagePacket
                // from the client's counter-offer dialog
            }
        }
    }

    private void processDiplomaticAccept(PlayerState playerState) {
        Faction from = GameCommon.getGameState().getFactionManager().getFaction(message.fromId);
        Faction to = GameCommon.getGameState().getFactionManager().getFaction(message.toId);
        if (from == null || to == null) return;

        switch (message.messageType) {
            case ALLIANCE_OFFER -> {
                // Accept alliance: set relation to FRIEND
                GameCommon.getGameState().getFactionManager().setRelationServer(
                    from.getIdFaction(), to.getIdFaction(), FactionRelation.RType.FRIEND.code
                );
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.ACCEPT_ALLIANCE
                );
                // Notify the offering faction
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " accepted your alliance offer", "We accept your alliance.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
                BetterFactions.getInstance().logInfo(to.getName() + " accepted alliance with " + from.getName());
            }
            case NON_AGGRESSION_PACT -> {
                // Accept NAP: set relation to NON_AGGRESSION
                GameCommon.getGameState().getFactionManager().setRelationServer(
                    from.getIdFaction(), to.getIdFaction(), CustomRelationType.NON_AGGRESSION
                );
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.ACCEPT_NON_AGGRESSION_PACT
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " accepted your non-aggression pact", "We accept the pact.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
                BetterFactions.getInstance().logInfo(to.getName() + " accepted NAP with " + from.getName());
            }
            case OFFER_PEACE -> {
                // Accept peace: set relation to NEUTRAL
                GameCommon.getGameState().getFactionManager().setRelationServer(
                    from.getIdFaction(), to.getIdFaction(), FactionRelation.RType.NEUTRAL.code
                );
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.ACCEPT_PEACE_OFFER
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " accepted your peace offer", "We accept peace.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
                BetterFactions.getInstance().logInfo(to.getName() + " accepted peace with " + from.getName());
            }
            case FEDERATION_INVITE -> {
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.ACCEPT_FEDERATION_OFFER
                );
            }
            case DEMAND_CONCESSION -> {
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.ACCEPT_DEMAND
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " accepted your demands", "We comply with your demands.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
                BetterFactions.getInstance().logInfo(to.getName() + " accepted demands from " + from.getName());
                //TODO: Execute actual demand terms (transfer territory, credits, etc.)
            }
            case COUNTER_OFFER -> {
                // Accept counter-offer: execute based on original type
                // For now, treat as generic acceptance
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " accepted your counter-offer", "We accept the revised terms.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
                BetterFactions.getInstance().logInfo(to.getName() + " accepted counter-offer from " + from.getName());
                //TODO: Execute counter-offer terms based on originalType field
            }
            default -> {}
        }
    }

    private void processDiplomaticDeny(PlayerState playerState) {
        Faction from = GameCommon.getGameState().getFactionManager().getFaction(message.fromId);
        Faction to = GameCommon.getGameState().getFactionManager().getFaction(message.toId);
        if (from == null || to == null) return;

        switch (message.messageType) {
            case ALLIANCE_OFFER -> {
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.REJECT_ALLIANCE
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " declined your alliance offer", "We decline your alliance offer.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
            }
            case NON_AGGRESSION_PACT -> {
                // Remove the pending relation offer
                long offerCode = FactionRelationOffer.getCode(from.getIdFaction(), to.getIdFaction());
                FactionRelationOfferAcceptOrDecline decline = new FactionRelationOfferAcceptOrDecline(
                    playerState.getName(), offerCode, false
                );
                GameCommon.getGameState().getFactionManager().getToAddFactionRelationOfferAccepts().add(decline);
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.REJECT_NON_AGGRESSION_PACT
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " declined your non-aggression pact", "We decline your pact offer.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
            }
            case OFFER_PEACE -> {
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.REJECT_PEACE_OFFER
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " rejected your peace offer", "We reject your peace offer.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
            }
            case FEDERATION_INVITE -> {
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.REJECT_FEDERATION_OFFER
                );
            }
            case DEMAND_CONCESSION -> {
                // Reject demand: generates a casus belli for the demanding faction (Phase 3)
                FactionDiplomacyManager.forceDiplomacyAction(
                    to.getIdFaction(), from.getIdFaction(), FactionDiplomacyAction.DiploActionType.REJECT_DEMAND
                );
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " rejected your demands", "We refuse your demands.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
                BetterFactions.getInstance().logInfo(to.getName() + " rejected demands from " + from.getName());
                //TODO: Generate REJECTED_DEMAND casus belli for the demanding faction (Phase 3.1)
            }
            case COUNTER_OFFER -> {
                // Reject counter-offer
                FactionMessage reply = new FactionMessage(to, from,
                    to.getName() + " rejected your counter-offer", "We reject the revised terms.",
                    FactionMessage.MessageType.REPLY
                );
                FactionManager.getFactionData(from).addMessage(reply);
            }
            default -> {}
        }
    }
}
