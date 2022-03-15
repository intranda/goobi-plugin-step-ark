package de.intranda.goobi.plugins;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;

public class ArkRestClient {

	private String uri;
	private String nameAssigningAuthorityNumber;
	private String auth;

	/**
	 * @param Uri	URL of the ark servuce
	 * @param NAAN	Name Assigning Authority Number
	 * @param User	Username of the API User
	 * @param Password Password of the User
	 */
	public ArkRestClient(String Uri, String NAAN, String User, String Password) {
		this.auth = Base64.getEncoder().encodeToString((User + ":" + Password).getBytes());
		if (!Uri.startsWith("https"))
			throw new IllegalArgumentException("Bad URL - only https is permitted");
		uri = (!Uri.endsWith("/")) ? Uri + "/" : Uri;
		nameAssigningAuthorityNumber = NAAN;
	}

	/**
	 * Mints a new Archival Resource Key with Metadata on the provided shoulder
	 * 
	 * @param _target  the identifier's target URL. Defaults to the identifier's
	 *                 ARKetype URL
	 * @param shoulder shoulder on which the new Key will be minted
	 * @param ercWho   name of the entity responsible for the ARK
	 * @param ercWhat  readable name describing the identifier (i.e. name of a book)
	 * @param ercWhen  A timestamp (can be only a year, a full date or a date range)
	 * @return String with new Archival Resource Key
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String mintArkWithMetadata(String _target, String shoulder, String ercWho, String ercWhat, String ercWhen)
			throws ClientProtocolException, IOException {
		HashMap<String, String> metadata = new HashMap<String, String>();
		metadata.put(ArkInternalEnumeration._target.toString(), _target);
		metadata.put(ArkErcEnumeration.WHO.toString(), ercWho);
		metadata.put(ArkErcEnumeration.WHAT.toString(), ercWhat);
		metadata.put(ArkErcEnumeration.WHEN.toString(), ercWhen);
		return mintArkWithMetadata(shoulder, metadata);
	}

	/**
	 * Mints a new Archival Resource Key with Metadata on the provided shoulder
	 * 
	 * @param shoulder shoulder on which the new Key will be minted
	 * @param metadata Hashmap with metadata relevant to the Key
	 * @return String with new Archival Resource Key
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public String mintArkWithMetadata(String shoulder, HashMap<String, String> metadata)
			throws ClientProtocolException, IOException, IllegalArgumentException {
		validateKeysOfMetadataHashMap(metadata);
		Request request = Request.Post(uri + "shoulder/ark:/" + nameAssigningAuthorityNumber + "/" + shoulder);
		request = addHeaders(request);

		String response = request.addHeader("Content-Type", "text/plain")
				.bodyString(createMetadataBodyString(metadata), ContentType.TEXT_PLAIN).execute()
				.handleResponse(new ArkResponseHandler());

		return response.replace("success: ", "");
	}


	/**
	 * Mints a new Archival Resource Key on the provided shoulder
	 * 
	 * @param shoulder shoulder on which the new Key will be minted
	 * @return String with new Archival Resource Key
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public String mintArk(String shoulder) throws ClientProtocolException, IOException {
		Request request = Request.Post(uri + "shoulder/ark:/" + nameAssigningAuthorityNumber + "/" + shoulder);
		request = addHeaders(request);

		String response = request.execute().handleResponse(new ArkResponseHandler());

		return response.replace("success: ", "");
	}

	// TODO Delete this test method
	public boolean deleteArk(String ARK) throws ClientProtocolException, IOException {
		Request request = Request.Delete(uri + "id/" + ARK);
		request = addHeaders(request);

		String response = request.addHeader("Content-Type", "text/plain").execute()
				.handleResponse(new ArkResponseHandler());
		if (response.startsWith("success: ")) {
			return true;
		} else {
			return false;
		}
	}

	// TODO Delete this test method
	public String getMetadata(String ARK) throws ClientProtocolException, IOException {
		return Request.Get("https://www.arketype.ch/id/" + ARK).addHeader("Accept", "text/plain").execute()
				.handleResponse(new ArkResponseHandler());
	}

	public String updateArk(String ARK, HashMap<String, String> metadata)
			throws ClientProtocolException, IOException, IllegalArgumentException {
		validateKeysOfMetadataHashMap(metadata);
		Request request = Request.Post(uri + "id/" + ARK);
		request = addHeaders(request);
		return request.addHeader("Content-Type", "text/plain")
				.bodyString(createMetadataBodyString(metadata), ContentType.TEXT_PLAIN).execute()
				.handleResponse(new ArkResponseHandler());
	}

	/**
	 * Helper method that validates the keys or names of the HashMap. It raises an
	 * IllegalArgumentException if an unknown Key is detected
	 * 
	 * @param metadata Hashmmap with the metadaty
	 * @throws IllegalArgumentException if the HashMap contains Keys that are not in
	 *                                  one of the Enumerations
	 */
	private void validateKeysOfMetadataHashMap(HashMap<String, String> metadata) throws IllegalArgumentException {

		outer_loop: for (String key : metadata.keySet()) {

			boolean keyok = false;
			for (ArkInternalEnumeration internal : ArkInternalEnumeration.values()) {
				keyok = key.equals(internal.toString());
				if (keyok)
					continue outer_loop;
			}

			for (ArkErcEnumeration erc : ArkErcEnumeration.values()) {
				keyok = key.equals(erc.toString());
				if (keyok)
					continue outer_loop;
			}

			for (ArkDataCiteEnumeration datacite : ArkDataCiteEnumeration.values()) {
				keyok = key.equals(datacite.toString());
				if (keyok)
					continue outer_loop;
			}
			throw new IllegalArgumentException("Unknown key was used in metadata: " + key);
		}
	}

	/**
	 * Helper Method that Creates the ANVL-String from a Hashmap
	 * 
	 * @param metadata HashMap with names and values
	 * @return ANVL-compliant Body string
	 */
	private String createMetadataBodyString(HashMap<String, String> metadata) {
		StringBuilder sb = new StringBuilder();
		metadata.forEach((key, value) -> {
			sb.append(key + ": " + escapeAnvl(value) + "\n");
		});
		return sb.toString();
	}

	/**
	 * Helper method that escapes the string like it's suggested in the ARKEtype API
	 * Documentation https://www.arketype.ch/doc/apidoc.html
	 * 
	 * @param element string value which may contain characters that must be escaped
	 * @return escaped string
	 */
	private String escapeAnvl(String element) {
		return element.replace("%", "%25").replace("\n", "%0A").replace("\r", "%0D").replace(":", "%3A");
	}

	/**
	 * Helper Method that adds Authorization- and Accept- Header to a given Request
	 * 
	 * @param request Request Object which needs Authorization and Accept Headers
	 * @return returns the Request with Authorization and Accept Header
	 */
	private Request addHeaders(Request request) {
		return request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth).addHeader("Accept", "text/plain");
	}

}