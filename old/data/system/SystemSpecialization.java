package thederpgamer.betterfactions.data.system;

import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.other.API;

/**
 * Interface for system specializations.
 * <p>Each system that a faction controls can be set to a specialization which gives boosts to certain actions and abilities.</p>
 * <p>For example, a faction can set their system to "Intel" which gives them the ability to spy on neighboring systems and gives them more insight into what's
 * happening around them.</p>
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
@API
public interface SystemSpecialization extends SerializationInterface {

	void initialize(FactionSystemData systemData);
}
