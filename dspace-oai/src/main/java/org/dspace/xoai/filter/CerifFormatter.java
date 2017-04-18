package org.dspace.xoai.filter;

import com.lyncode.xoai.dataprovider.data.ICustomFormatter;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Element.Field;

public class CerifFormatter implements ICustomFormatter {

	public CerifFormatter() {
		// VSTODO Auto-generated constructor stub
	}

	public String getCustomXml(Item item) {
		
		String cerifText = "";
		for(Element e:item.getMetadata().getMetadata().getElement()) 
			if (e.getName().equalsIgnoreCase("cerif")) {
				for(Field f:e.getField()) {
					if(f.getName().contentEquals("openaire")) { //VSTODO: parametrizzare
						cerifText = f.getValue();
					}
					break;
				}
				break;
			}		
		return cerifText;
	}
}
