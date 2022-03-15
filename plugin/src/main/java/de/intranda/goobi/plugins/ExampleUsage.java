package de.intranda.goobi.plugins;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;


public class ExampleUsage {
	
	public void exampleCall() {
		//create client with URI, NAN, nutzername und passwort...
		ArkRestClient client = new ArkRestClient("https://www.arketype.ch/", "99999", "apitest5", "vvHSbmDqiJqpKdcMJOA1");
		
		// generate HashMap with metadata
		// don't create entries with null as value or key ->IllegalArguementException will be raised
		HashMap<String, String> metadata = new HashMap<String, String>();
		metadata.put(ArkErcEnumeration.WHO.toString(), "Stephen Hawking");
		metadata.put(ArkErcEnumeration.WHAT.toString(), "The Universe in a Nutshell");
		metadata.put(ArkErcEnumeration.WHEN.toString(), LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
		metadata.put(ArkInternalEnumeration._target.toString(), "http://example.com/");
		metadata.put(ArkDataCiteEnumeration.PUBLICATIONYEAR.toString(), ""+LocalDate.now().getYear());
		
		
		//status reserved means the ark won't go public
		metadata.put(ArkInternalEnumeration._status.toString(), "reserved");
		
		String newArk;
		
		try {
			//fk3 is the shoulder of the apitest5 user
			newArk = client.mintArkWithMetadata("fk3", metadata);
			
			//or we could create Helper Methods like this
		    newArk = client.mintArkWithMetadata("http://example.com/", "fk3", "Clark Kent", "Titel des Werks", "Datum");
			
		    //update of an ArkID
		    //change metadata
			metadata.put(ArkErcEnumeration.WHAT.toString(), "A Brief History of Time");
			// updateArk with ArkID -> ark:/12148/btv1b8449691v
		    client.updateArk(newArk, metadata);
				
		} catch (IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
