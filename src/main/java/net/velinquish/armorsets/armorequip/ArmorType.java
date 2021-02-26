package net.velinquish.armorsets.armorequip;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ArmorType{
	HELMET(5), CHESTPLATE(6), LEGGINGS(7), BOOTS(8);

	private final int slot;

	ArmorType(int slot){
		this.slot = slot;
	}

	/**
	 * Attempts to match the ArmorType for the specified ItemStack.
	 *
	 * @param itemStack The ItemStack to parse the type of.
	 * @return The parsed ArmorType. (null if none were found.)
	 */
	public final static ArmorType matchType(final ItemStack itemStack){
		if(itemStack == null || itemStack.getType().equals(Material.AIR)) return null;
		//The next 2 lines are added by Velinquish to support block helmets
		if (itemStack.getType().isBlock())
			return HELMET;
		String type = itemStack.getType().name();
		if(type.endsWith("_HELMET") || type.endsWith("_SKULL")) return HELMET;
		else if(type.endsWith("_CHESTPLATE")) return CHESTPLATE;
		else if(type.endsWith("_LEGGINGS")) return LEGGINGS;
		else if(type.endsWith("_BOOTS")) return BOOTS;
		else return null;
	}

	public int getSlot(){
		return slot;
	}
}
