package de.intranda.goobi.plugins;

import java.io.IOException;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.VariableReplacer;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class ArkStepPlugin implements IStepPluginVersion2 {

	@Getter
	private String title = "intranda_step_ark";
	@Getter
	private Step step;

	private String returnPath;
	private String metadataType;
	private String uri;
	private String naan;
	private String apiUser;
	private String apiPassword;
	private String shoulder;
	private String creator;
	private String dctitle;
	private String publisher;
	private String publicationYear;
	private String resourceType;
	private String target;

	@Override
	public void initialize(Step step, String returnPath) {
		this.returnPath = returnPath;
		this.step = step;

		// read parameters from correct block in configuration file
		SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);

		metadataType = myconfig.getString("metadataType", "ark");
		uri = myconfig.getString("uri", "http://example.com");
		naan = myconfig.getString("naan", "1111111");
		apiUser = myconfig.getString("apiUser", "Nutzer");
		apiPassword = myconfig.getString("apiPassword", "Password");
		shoulder = myconfig.getString("shoulder", "Password");
		
		creator = myconfig.getString("metadataCreator");
		dctitle =  myconfig.getString("metadataTitle");
		publisher =  myconfig.getString("metadataPublisher");
		publicationYear = myconfig.getString("metadataPublicationYear");
		resourceType =  myconfig.getString("metadataResourceType");
		
		target = myconfig.getString("publicationUrl");
		
		log.info("Ark step plugin initialized");
	}

	@Override
	public PluginGuiType getPluginGuiType() {
		return PluginGuiType.NONE;
	}

	@Override
	public String getPagePath() {
		return "/uii/plugin_step_ark.xhtml";
	}

	@Override
	public PluginType getType() {
		return PluginType.Step;
	}

	@Override
	public String cancel() {
		return "/uii" + returnPath;
	}

	@Override
	public String finish() {
		return "/uii" + returnPath;
	}

	@Override
	public int getInterfaceVersion() {
		return 0;
	}

	@Override
	public HashMap<String, StepReturnValue> validate() {
		return null;
	}

	@Override
	public boolean execute() {
		PluginReturnValue ret = run();
		return ret != PluginReturnValue.ERROR;
	}

	@Override
	public PluginReturnValue run() {
		boolean successful = true;
		boolean foundExistingArk = false;
		
		ArkRestClient arkClient = new ArkRestClient(uri, naan, apiUser, apiPassword);

		try {
			// read mets file
			Fileformat ff = step.getProzess().readMetadataFile();
			Prefs prefs = step.getProzess().getRegelsatz().getPreferences();
			DocStruct logical = ff.getDigitalDocument().getLogicalDocStruct();
			VariableReplacer replacer = new VariableReplacer(ff.getDigitalDocument(), prefs, step.getProzess(),step);
			HashMap<String, String> mdata = new HashMap<String, String>();
			creator =replacer.replace(creator);
			mdata.put(ArkInternalEnumeration._profile.toString(), "datacite");
			mdata.put(ArkInternalEnumeration._target.toString(),replacer.replace(target));
			mdata.put(ArkDataCiteEnumeration.CREATOR.toString(), replacer.replace (creator));
			mdata.put(ArkDataCiteEnumeration.TITLE.toString(), replacer.replace(dctitle));
			mdata.put(ArkDataCiteEnumeration.PUBLICATIONYEAR.toString(), replacer.replace(publicationYear));
			mdata.put(ArkDataCiteEnumeration.PUBLISHER.toString(), replacer.replace(publisher));
			mdata.put(ArkDataCiteEnumeration.RESOURCETYPE.toString(), resourceType);
			
			if (logical.getType().isAnchor()) {
				logical = logical.getAllChildren().get(0);
			}

			// find existing ARKs
			for (Metadata md : logical.getAllMetadata()) {
				if (md.getType().getName().equals(metadataType)) {
					foundExistingArk = true;
					String existingArk = md.getValue();
					successful = arkClient.updateArk(existingArk, mdata);
				}
			}
			
			// if no ARKs found yet register a new one
			if (!foundExistingArk) {
				Metadata md = new Metadata(prefs.getMetadataTypeByName(metadataType));
				String myNewArk = arkClient.mintArkWithMetadata(shoulder, mdata);
				md.setValue(myNewArk);
				logical.addMetadata(md);
				
				// save the mets file
				step.getProzess().writeMetadataFile(ff);
				successful=true;
			}

		} catch (ReadException | PreferencesException | WriteException | IOException | InterruptedException
				| SwapException | DAOException  | MetadataTypeNotAllowedException e) {
			log.error(e);
		}

		log.info("Ark step plugin executed");
		if (!successful) {
			return PluginReturnValue.ERROR;
		}
		return PluginReturnValue.FINISH;
	}
}
