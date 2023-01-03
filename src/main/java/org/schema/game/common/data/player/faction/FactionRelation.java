package org.schema.game.common.data.player.faction;

import org.schema.common.util.TranslatableEnum;
import org.schema.schine.common.language.Lng;
import org.schema.schine.common.language.Translatable;
import org.schema.schine.network.objects.NetworkObject;
import org.schema.schine.network.objects.remote.RemoteIntegerArray;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.Tag.Type;
import org.schema.schine.resource.tag.TagSerializable;

import javax.vecmath.Vector3f;
import java.util.Locale;

public class FactionRelation implements TagSerializable {


	public int a;
	public int b;
	public byte rel;

	public FactionRelation() {
	}

	public FactionRelation(int a, int b, byte rel) {
		super();
		set(a, b);
		this.rel = rel;
	}

	public static long getCode(int af, int bf) {
		int a = Math.min(Math.abs(af), Math.abs(bf));
		int b = Math.max(Math.abs(af), Math.abs(bf));
		return (long) a * (long) Integer.MAX_VALUE + b;
	}

	public static byte getRelationFromString(String string) {
		if (string.toLowerCase(Locale.ENGLISH).equals("enemy")) {
			return RType.ENEMY.code;
		}
		if (string.toLowerCase(Locale.ENGLISH).equals("friend") || string.toLowerCase(Locale.ENGLISH).equals("ally")) {
			return RType.FRIEND.code;
		}
		if (string.toLowerCase(Locale.ENGLISH).equals("neutral")) {
			return RType.NEUTRAL.code;
		}
		if(string.toLowerCase(Locale.ENGLISH).equals("federation")) return RType.FEDERATION.code;
		if(string.toLowerCase(Locale.ENGLISH).equals("non-aggression")) return RType.NON_AGGRESSION.code;
		throw new IllegalArgumentException();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return a + b * 100000 + rel * 100023;
	}

	;

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		if (arg0 != null && arg0 instanceof FactionRelation) {
			FactionRelation o = (FactionRelation) arg0;
			return a == o.a && b == o.b && rel == o.rel;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Rel[a=" + a + ", b=" + b + ", rel=" + getRelation().name() + "]";
	}

	public boolean contains(int id) {
		return a == id || b == id;
	}

	@Override
	public void fromTagStructure(Tag tag) {
		Tag[] subs = (Tag[]) tag.getValue();

		a = (Integer) subs[0].getValue();
		b = (Integer) subs[1].getValue();
		rel = (Byte) subs[2].getValue();
	}

	@Override
	public Tag toTagStructure() {
		return new Tag(Type.STRUCT, null, new Tag[]{new Tag(Type.INT, null, a), new Tag(Type.INT, null, b), new Tag(Type.BYTE, null, rel), FinishTag.INST});
	}

	public long getCode() {
		return getCode(a, b);
	}

	public RType getRelation() {
		return RType.values()[rel];
	}

	public void setRelation(byte relationship) {
		this.rel = relationship;

	}

	public RemoteIntegerArray getRemoteArray(NetworkObject synch) {
		RemoteIntegerArray ar = new RemoteIntegerArray(3, synch);
		ar.set(0, a);
		ar.set(1, b);
		ar.set(2, (int) rel);
		return ar;
	}


	public boolean isEnemy() {
		return rel == RType.ENEMY.code;
	}

	public boolean isFriend() {
		return rel == RType.FRIEND.code;
	}

	public boolean isNeutral() {
		return rel == RType.NEUTRAL.code;
	}

	public boolean isRelation(RType type) {
		return rel == type.code;
	}

	public void set(int a, int b) {
		assert (a != b);
		assert (a != 0 && b != 0);
		this.a = Math.min(a, b);
		this.b = Math.max(a, b);
	}

	public void setEnemy() {
		rel = RType.ENEMY.code;
	}

	public void setFriend() {
		rel = RType.FRIEND.code;
	}

	public void setNeutral() {
		rel = RType.NEUTRAL.code;
	}

	public void setRelation(RType type) {
		rel = type.code;
	}
	public enum AttackType{
		NONE(0),
		OWNER(1),
		NEUTRAL(2),
		ENEMY(4),
		FACTION(8),
		ALLY(16),
		;
		private static int all;
		static {
			all = 0;
			for(AttackType t : values()) {
				all |= t.code;
			}
		}

		public static int get(AttackType ... mm) {
			int c = 0;
			for(AttackType t : mm) {
				c |= t.code;
			}
			return c;
		}
		public static int getAll() {
			return all;
		}

		public final int code;
		public static boolean isAttacking(int code, AttackType t) {
			return (code & t.code) == t.code;
		}
		private AttackType(int c) {
			this.code = c;
		}
	}

	public enum RType implements TranslatableEnum {
		NEUTRAL(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("NEUTRAL");
			}
		}, 0, new Vector3f(0.5f, 0.7f, 0.9f), (byte) 0),
		ENEMY(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("ENEMY");
			}
		}, -1, new Vector3f(1, 0, 0), (byte) 1),
		FRIEND(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("FRIEND");
			}
		}, 1, new Vector3f(0, 1, 0), (byte) 2),
		NON_AGGRESSION(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("NEUTRAL (NO AGGRESSION)");
			}
		}, 3, new Vector3f(0.81f, 0.81f, 0.36f), (byte) 3),
		FEDERATION(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("FEDERATION");
			}
		}, 2, new Vector3f(0.19f, 0.65f, 0.55f), (byte) 4);


		private final Translatable name;
		public final int sortWeight;
		public final Vector3f defaultColor;
		public final byte code;


		private RType(Translatable name, int sortWeight, Vector3f defaultColor, byte code) {
			this.name = name;
			this.sortWeight = sortWeight;
			this.defaultColor = defaultColor;
			this.code = code;
		}

		public String getName(){
			return name.getName(this);
		}
	}

}