/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.crosswalk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.harvest.HarvestedItem;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.jdom.Element;
import org.jdom.Namespace;

import gr.ekt.cerif.enumerations.semantics.ClassEnum;

public class CerifIngestionCrosswalk implements IngestionCrosswalk {
	private static final Namespace DC_NS = Namespace.getNamespace("http://www.dspace.org/xmlns/dspace/dim");
	private static final Namespace OAI_DC_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/");
	private static final Namespace CERIF_NS = Namespace.getNamespace("urn:xmlns:org:eurocris:cerif-1.6-2");

	private static final String PREFIX = "fvgcerif";

	private static final String RENDER_ISSUED = "issued";
	private static final String RENDER_AUTHOR = "author";

	private HashMap<String, String> cerif2metadata = new HashMap<String, String>();
	private String sourceRef = "";

	private void init() throws IOException {
		String propsFilename = ConfigurationManager.getProperty("crosswalk." + PREFIX + ".properties." + PREFIX);
		String parent = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator;
		File propsFile = new File(parent, propsFilename);
		Properties qdcProps = new Properties();
		FileInputStream pfs = null;
		try {
			pfs = new FileInputStream(propsFile);
			qdcProps.load(pfs);
		} finally {
			if (pfs != null) {
				try {
					pfs.close();
				} catch (IOException ioe) {
				}
			}
		}
		Enumeration<String> pe = (Enumeration<String>) qdcProps.propertyNames();
		while (pe.hasMoreElements()) {
			String oaidc = pe.nextElement();
			String fvgdc = qdcProps.getProperty(oaidc);
			cerif2metadata.put(oaidc, fvgdc);
		}
	}

	public void ingest(Context context, DSpaceObject dso, List<Element> metadata)
			throws CrosswalkException, SQLException, AuthorizeException, IOException {
		Element wrapper = new Element("wrap", metadata.get(0).getNamespace());
		init();
		wrapper.addContent(metadata);
		ingest(context, dso, wrapper);
	}

	public void ingest(Context context, DSpaceObject dso, Element root)
			throws CrosswalkException, SQLException, AuthorizeException, IOException {

		init();
		if (dso.getType() != Constants.ITEM) {
			throw new CrosswalkObjectNotSupported("DIMIngestionCrosswalk can only crosswalk an Item.");
		}
		Item item = (Item) dso;

		if (root == null) {
			System.err.println("The element received by ingest was null");
			return;
		}
		if (root.getChild("cfResPubl", CERIF_NS) == null) {
			System.err.println("No cfResPubl tag found in item " + item.getID());
			return;
		}
		List<Element> metadata = root.getChild("cfResPubl", CERIF_NS).getChildren();
		int place = 0;
		String allpeople = "";
		boolean type = false;
		boolean fulltext = false;
		for (Element element : metadata) {
			String lang = "en";
			if (element.getAttribute("cfLangCode") != null) {
				lang = element.getAttributeValue("cfLangCode");
			}
			if (cerif2metadata.containsKey(element.getName())) {
				if (!"cfPers_ResPubl".equals(element.getName())) {
					if (StringUtils.isBlank(element.getText())) {
						continue;
					}
				}
				String md = cerif2metadata.get(element.getName());
				String render = "";
				if (StringUtils.contains(md, "(")) {
					String meta = StringUtils.substringBefore(md, "(");
					render = StringUtils.substringAfter(StringUtils.remove(md, ")"), "(");
					md = meta;
				}
				String[] mdArray = StringUtils.split(md, "\\.");
				String schema = "";
				String elmnt = "";
				String qlfr = "";

				if (mdArray != null && mdArray.length >= 2) {
					schema = mdArray[0];
					elmnt = mdArray[1];
					qlfr = mdArray.length == 3 ? mdArray[2] : null;
					if (StringUtils.equalsIgnoreCase(render, RENDER_ISSUED)) {
						if (isDateIssued(element.getText())) {
							item.addMetadata(schema, elmnt, qlfr, lang, element.getText());
						} else {
							continue;
						}
					} else if (StringUtils.equalsIgnoreCase(render, RENDER_AUTHOR)) {
						place++;
						if (element.getChild("cfPersId", CERIF_NS) == null) {
							// item.addMetadata(schema, element, qlfr, lang,
							// value);
							continue;
						}
						String remoteID = element.getChild("cfPersId", CERIF_NS).getText();
						ResearcherPage rp = HarvestedItem.getRPByRemoteId(context, sourceRef, remoteID);
						if (rp == null) {
							// item.addMetadata(schema, element, qlfr, lang,
							// value);
							continue;
						}
						if (allpeople.length() > 0) {
							allpeople += "; ";
						}
						allpeople += rp.getFullName();
						item.addMetadata(schema, elmnt, qlfr, lang, rp.getFullName(), rp.getCrisID(), 600);
					} else {
						item.addMetadata(schema, elmnt, qlfr, lang, element.getText());
					}
				}
			} else {
				if ("cfResPubl_Class".equals(element.getName())) {
					String classId = element.getChild("cfClassSchemeId", CERIF_NS).getText();
					if (null == ClassEnum.fromUuid(classId)) {
						continue;
					}
					switch (ClassEnum.fromUuid(classId).getName()) {
						case "Language":
							item.addMetadata("dc", "language", null, lang,
									element.getChild("cfClassId", CERIF_NS).getText());
							break;
						case "Output Types":
							String typeId = element.getChild("cfClassId", CERIF_NS).getText();
							if (null != ClassEnum.fromUuid(typeId)) {
								item.addMetadata("dc", "type", null, lang, ClassEnum.fromUuid(typeId).getName());
								type = true;
							}
							break;
						case "Open Access Types":
							String accessType = element.getChild("cfClassId", CERIF_NS).getText();
							if (null != ClassEnum.fromUuid(accessType)) {
								item.addMetadata("dc", "rights", null, lang, ClassEnum.fromUuid(accessType).getName());
								if("Embargoed Access".equals(ClassEnum.fromUuid(classId).getName())){
									Element embargoDate = element.getChild("cfEndDate", CERIF_NS);
									if(null != embargoDate){
										item.addMetadata("dc", "rights", "embargodate", lang, embargoDate.getText());
									}
								}
							}
							break;
						case "License Types":
							String licenseType = element.getChild("cfClassId", CERIF_NS).getText();
							item.addMetadata("dc", "rights", "license", lang, ClassEnum.fromUuid(licenseType).getName());
							break;
					}
				}
				if ("cfFedId".equals(element.getName())) {
					String classSchemeId = element.getChild("cfFedId_Class", CERIF_NS).getChild("cfClassId", CERIF_NS)
							.getText();
					if (null == ClassEnum.fromUuid(classSchemeId)) {
						continue;
					}
					String classId = element.getChild("cfFedId_Class", CERIF_NS).getChild("cfClassId", CERIF_NS)
							.getText();
					if (null == ClassEnum.fromUuid(classId)) {
						continue;
					}
					switch (ClassEnum.fromUuid(classId).getName()) {
					case "Scopus publication identifier":
						item.addMetadata("dc", "identifier", "scopus", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "DOI":
						item.addMetadata("dc", "identifier", "doi", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "PMCID":
						item.addMetadata("dc", "identifier", "pmid", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "ISI-Number":
						item.addMetadata("dc", "identifier", "isi", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "issn":
						item.addMetadata("dc", "identifier", "issn", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "isbn":
						item.addMetadata("dc", "identifier", "isbn", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "URL":
						item.addMetadata("dc", "identifier", "url", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					case "URI":
						item.addMetadata("dc", "identifier", "urlfvg", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						fulltext = true;
						break;
					case "Handle":
						item.addMetadata("dc", "identifier", "uri", lang,
								element.getChild("cfFedId", CERIF_NS).getText());
						break;
					}
				}
			}
		}
		if (allpeople.length() > 0) {
			item.addMetadata("dc", "description", "allpeople", "en", allpeople);
		}
		if(!type){
			item.addMetadata("dc", "type", null, "en", "Other");
		}
		if(!fulltext){
			item.addMetadata("dc", "rights", null, "en", "No fulltext");
		}
		item.addMetadata("dc", "identifier", "sourcefvg", "en", this.sourceRef);

	}

	private boolean isValidHandle(String uri) {
		boolean isValid = false;
		if (StringUtils.containsIgnoreCase(uri, "/handle/") || StringUtils.containsIgnoreCase(uri, "hdl.handle.net/")) {
			isValid = true;
		}
		return isValid;
	}

	private boolean isDateIssued(String issued) {
		boolean isValid = false;
		if (StringUtils.length(issued) <= 10 && (issued.matches("\\d{4}-[01]\\d-[0-3]\\d")
				|| issued.matches("\\d{4}-[01]\\d") || issued.matches("\\d{4}"))) {
			isValid = true;
		}
		return isValid;
	}

	public void setSorurceRef(String src) {
		this.sourceRef = src;
	}
}
