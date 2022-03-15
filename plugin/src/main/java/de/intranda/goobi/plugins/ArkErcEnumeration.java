package de.intranda.goobi.plugins;

public enum ArkErcEnumeration {
	WHO("erc.who"),
	WHAT("erc.what"),
	WHEN("erc.when");
	
	final private String notation;

	private ArkErcEnumeration(String notation) {
		this.notation = notation;
	}
	
	@Override
	public String toString() {
		return notation;
	}
}
