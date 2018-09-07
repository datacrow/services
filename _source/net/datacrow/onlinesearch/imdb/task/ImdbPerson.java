/******************************************************************************
 *                                     __                                     *
 *                              <-----/@@\----->                              *
 *                             <-< <  \\//  > >->                             *
 *                               <-<-\ __ /->->                               *
 *                               Data /  \ Crow                               *
 *                                   ^    ^                                   *
 *                              info@datacrow.net                             *
 *                                                                            *
 *                       This file is part of Data Crow.                      *
 *       Data Crow is free software; you can redistribute it and/or           *
 *        modify it under the terms of the GNU General Public                 *
 *       License as published by the Free Software Foundation; either         *
 *              version 3 of the License, or any later version.               *
 *                                                                            *
 *        Data Crow is distributed in the hope that it will be useful,        *
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *           MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.             *
 *           See the GNU General Public License for more details.             *
 *                                                                            *
 *        You should have received a copy of the GNU General Public           *
 *  License along with this program. If not, see http://www.gnu.org/licenses  *
 *                                                                            *
 ******************************************************************************/

package net.datacrow.onlinesearch.imdb.task;

import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcObject;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ImdbPerson {

    private static Logger logger = Logger.getLogger(ImdbPerson.class.getName());
    
    private static final String address = "http://www.imdb.com";
    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    private final int module;
    private final boolean full;
    
    public ImdbPerson(int module, boolean full) {
        this.module = module;
        this.full = full;
    }
    
    public DcObject get(Object id) throws Exception {
        URL url = new URL(address + "/name/nm" + id);
        return get(url);
    }    
    
    public DcObject get(URL url) throws Exception {
        DcAssociate person = (DcAssociate) DcModules.get(module).getItem();
        
        Document document = HtmlUtils.getDocument(url, "UTF8");
        
        String s = url.toString();
        String id = s.substring(s.indexOf("/name/nm") + 8, s.length());
        
        Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
        person.setValue(DcAssociate._A_NAME, node.getTextContent());
        person.setValue(DcAssociate._C_WEBPAGE, address + "/name/nm" + id);
        person.setValue(DcObject._SYS_SERVICEURL, s);
        person.addExternalReference(DcRepository.ExternalReferences._IMDB, id);
        
        if (full) {
            setBiography(person, id);
            setPhoto(person, document);
        }
        
        person.setName();
        return person;
    }
    
    private void setBiography(DcObject person, String id) throws Exception {
        String address = "http://www.imdb.com/name/nm" + id + "/bio";
        try {
            Document document = HtmlUtils.getDocument(new URL(address), "iso-8859-1");
            Node node1 = (Node) xpath.evaluate("//div[@id='tn15content']//p[1]", document, XPathConstants.NODE);
            Node node2 = (Node) xpath.evaluate("//div[@id='tn15content']//p[2]", document, XPathConstants.NODE);
            
            if (node1 != null && node2 == null) {
                String s = node1.getTextContent();
                if (!s.toLowerCase().startsWith("browse biographies"))
                    person.setValue(DcAssociate._B_DESCRIPTION, s);
            } else if (node1 != null && node2 != null) {
                String s1 = node1.getTextContent();
                String s2 = node2.getTextContent();
                if (!s1.toLowerCase().startsWith("browse biographies"))
                    person.setValue(DcAssociate._B_DESCRIPTION, s1 + (!s2.toLowerCase().startsWith("browse biographies") ? "\n\n" + s2 : ""));
            }
            
        } catch (Exception e) {
            logger.debug("Could not parse information from " + address);
        }
    }
    
    private void setPhoto(DcObject person, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//div[@class='photo']//img", document, XPathConstants.NODE);
        if (node != null) {
            String url =node.getAttributes().getNamedItem("src").getTextContent();
            if (!url.contains("nophoto.jpg")) {
                DcImageIcon icon = new DcImageIcon(HttpConnectionUtil.getInstance().retrieveBytes(url));
                person.setValue(DcAssociate._D_PHOTO, icon);
            }
        }
    }
}
