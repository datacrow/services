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

package net.datacrow.onlinesearch.sourceforge.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcMediaObject;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Software;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SourceforgeSoftwareSearch extends SearchTask {

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public SourceforgeSoftwareSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }

    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }

    @Override
    protected DcObject getItem(Object key, boolean full) throws Exception {
        return getItem(new URL(getServer().getUrl() + key), full);
    }
    
    protected DcObject getItem(URL url, boolean full) throws Exception {
        DcObject software = DcModules.get(DcModules._SOFTWARE).getItem();

        Document document = HtmlUtils.getDocument(url, "UTF-8");
        
        String link = url.toString();
        String id = link.substring(getServer().getUrl().length());
        
        software.addExternalReference(DcRepository.ExternalReferences._SOURCEFORGE, id);
        
        software.setValue(Software._I_WEBPAGE, link);
        software.setValue(DcObject._SYS_SERVICEURL, link);
        
        setTitle(software, document);
        setDescription(software, document);
        setCategories(software, document);
        setYear(software, document);
        setDevelopers(software, document);
        
        if (full) 
            setImages(software, document);

        return software;
    }

    private void setTitle(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
        String title = node.getTextContent();
        title = title.indexOf("|") > -1 ? title.substring(0, title.indexOf("|")) : title;
        dco.setValue(DcMediaObject._A_TITLE, StringUtils.trim(title));
    }

    private void setImages(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//a[starts-with(@href,'/project/screenshots')]/@href", document, XPathConstants.NODE);
        if (node != null) {
            String link = node.getTextContent();
            
            document = HtmlUtils.getDocument(new URL(getServer().getUrl() + link), "UTF-8");
            
            NodeList images = (NodeList) xpath.evaluate("//img[starts-with(@src,'/dbimage.php?')]/@src", document, XPathConstants.NODESET);
            Collection<String> imageIDs = new ArrayList<String>();
            
            if (images != null) {
                int image = 0;
                for (int i = 0; i < images.getLength() && image < 3; i++) {
                    String id = images.item(i).getTextContent();
                    id = id.substring(id.indexOf("?id=") + 4);
                    
                    if (image > 0) 
                        id = String.valueOf(Integer.valueOf(id) + 1);
                    
                    if (!imageIDs.contains(id)) {
                        imageIDs.add(id);
                        byte[] b = HttpConnectionUtil.getInstance().retrieveBytes("http://sourceforge.net/dbimage.php?id="+ id);
                        if (b != null) {
                            int fieldIdx = image == 0 ? Software._P_SCREENSHOTONE :
                                           image == 1 ? Software._Q_SCREENSHOTTWO : Software._R_SCREENSHOTTHREE;
                            dco.setValue(fieldIdx, b);
                            image++;
                        }
                    }
                }
            }
        }
    }
    
    private void setDevelopers(DcObject dco, Document document) throws Exception {
        NodeList maintainers = (NodeList) xpath.evaluate("//p[@id='maintainers']/a", document, XPathConstants.NODESET);
        
        if (maintainers != null) {
            for (int i = 0; i < maintainers.getLength(); i++)
                DataLayer.getInstance().createReference(dco, Software._F_DEVELOPER, maintainers.item(i).getTextContent());
        }
    }
    
    private void setYear(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//h2[text()='Release Date:']/following-sibling::div", document, XPathConstants.NODE);
        if (node != null) {
            String year = node.getTextContent();
            year = year.substring(0, year.indexOf("-"));
            dco.setValue(Software._C_YEAR, year);
        }
    }
        
    private void setDescription(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//p[@class='editable']", document, XPathConstants.NODE);
        String description = node != null ? StringUtils.trim(node.getTextContent()) : "";
                
        NodeList features = (NodeList) xpath.evaluate("//ul[@class='features']/li", document, XPathConstants.NODESET);
        
        if (features != null) {
            for (int i = 0; i < features.getLength(); i++) {
                description += i == 0 ? "\n\n" : description.length() > 0 ? "\n" : "";
                description += "* " + features.item(i).getTextContent();
            }
        }
        
        dco.setValue(DcMediaObject._B_DESCRIPTION, description);
    }
    
    private void setCategories(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//h2[text()='Topics:']/following-sibling::div", document, XPathConstants.NODE);
        if (node != null) {
            String categories = node.getTextContent();
            StringTokenizer st = new StringTokenizer(categories, ",");
            while (st.hasMoreElements()) {
                String category = (String) st.nextElement();
                DataLayer.getInstance().createReference(dco, Software._K_CATEGORIES, StringUtils.trim(category));
            }
        }
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();
        
        String searchURL = getServer().getUrl() + "/search/?type_of_search=soft&words=" + getQuery();
        Document document = HtmlUtils.getDocument(new URL(searchURL), "UTF8");
        NodeList nodeList = (NodeList) xpath.evaluate("html//a[starts-with(@href,'/projects/')]/@href", document, XPathConstants.NODESET);
        int length = nodeList.getLength();
        for(int i = 0; i < length; i++) {
            String key = nodeList.item(i).getTextContent();
            
            if (!keys.contains(key) && key.indexOf("/files/") == -1)
                keys.add(key);
        }
        return keys;
    }
    
}
