package de.intranda.goobi.plugins;

public enum ArkDataCiteEnumeration {
	CREATOR("datacite.creator"),
	TITLE("datacite.title"),
	PUBLISHER("datacite.publisher"),
	PUBLICATIONYEAR("datacite.publicationyear"),
	RESOURCETYPE("datacite.resourcetype");

	final private String notation;

	ArkDataCiteEnumeration(String notation) {
		this.notation = notation;
	}

	@Override
	public String toString() {
		return notation;
	}

}
