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

package net.datacrow.onlinesearch.softpedia.task;

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
import net.datacrow.onlinesearch.softpedia.mode.LinuxSearchMode;
import net.datacrow.onlinesearch.softpedia.mode.SoftpediaSearchMode;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SoftpediaSearch extends SearchTask {

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public SoftpediaSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }

    @Override
    protected DcObject getItem(Object key, boolean full) throws Exception {
        return getItem(new URL((String) key), full);
    }
    
    protected DcObject getItem(URL url, boolean full) throws Exception {
        DcObject software = DcModules.get(DcModules._SOFTWARE).getItem();

        Document document = HtmlUtils.getDocument(url, "UTF-8");
        
        String link = url.toString();
        software.addExternalReference(DcRepository.ExternalReferences._SOFTPEDIA, link);
        
        software.setValue(Software._I_WEBPAGE, link);
        software.setValue(DcObject._SYS_SERVICEURL, link);

        setTitle(software, document);
        setDescription(software, document);
        setRating(software, document);
        
        setOther(software, document);
        
        if (getMode() instanceof LinuxSearchMode)
            DataLayer.getInstance().createReference(software, Software._H_PLATFORM, "Linux");
         
        if (full)
            setScreenshots(software, document);
        
        return software;
    }
    
    private void setScreenshots(DcObject dco, Document document) throws Exception {
        SoftpediaSearchMode mode = (SoftpediaSearchMode) getMode();
        String expression = "//a[starts-with(@href, '" +  mode.getBaseURL() + "/progScreenshots/')]/@href";
        Node node = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
        if (node != null) {
            String link = node.getTextContent();
            Document imagesDoc = HtmlUtils.getDocument(new URL(link), "UTF-8");
            
            expression = "//img[starts-with(@src, '" +  mode.getBaseURL() + "/screenshots/')]/@src";
            NodeList nodes = (NodeList) xpath.evaluate(expression, imagesDoc, XPathConstants.NODESET);
            
            if (nodes != null) {
                int imgIdx = Software._P_SCREENSHOTONE;
                for (int i = 0; i < nodes.getLength() && (imgIdx == Software._P_SCREENSHOTONE || imgIdx == Software._Q_SCREENSHOTTWO || imgIdx == Software._R_SCREENSHOTTHREE) ; i++) {
                    link = nodes.item(i).getTextContent();
                    byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
                    if (image != null) dco.setValue(imgIdx++, new DcImageIcon(image));
                }
            }
        }
    }

    private void setRating(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//td[@id='rater__upd']", document, XPathConstants.NODE);
        if (node != null) {
            String rating = StringUtils.getValueBetween("(", "/", node.getTextContent());
            try {
                dco.setValue(DcMediaObject._E_RATING, Long.valueOf(Math.round(Double.valueOf(rating) * 2)));
            } catch (NumberFormatException nfe) {}
        }
    }    
    
    private void setOther(DcObject dco, Document document) throws Exception {
        SoftpediaSearchMode mode = (SoftpediaSearchMode) getMode();
        Node node = (Node) xpath.evaluate("//a[starts-with(@href, '" + mode.getBaseURL() + "/developer')]", document, XPathConstants.NODE);
        node = node == null ? (Node) xpath.evaluate("//td//a[@href='http://drivers.softpedia.com']", document, XPathConstants.NODE) : node;
        
        if (node != null) {
            String text = node.getParentNode().getTextContent();
            text = StringUtils.trim(text).replaceAll("\t", "");
            StringTokenizer st = new StringTokenizer(text, "\n");
            
            int idx = 0;
            while (st.hasMoreElements()) {
                String value = StringUtils.trim((String) st.nextElement());
                
                if (idx == mode.getIndex(SoftpediaSearchMode._DEVELOPER)) {
                   String developer = value.indexOf("|") > -1 ? value.substring(0, value.indexOf("|")) : value;
                   DataLayer.getInstance().createReference(dco, Software._F_DEVELOPER, StringUtils.trim(developer));
                
                } else if (idx == mode.getIndex(SoftpediaSearchMode._YEAR)) {
                    String year = StringUtils.getValueBetween(",", ",", value);
                    year = StringUtils.getContainedNumber(year.trim());
                    if (year.length() > 0 && year.length() <= 4) 
                        dco.setValue(DcMediaObject._C_YEAR, Long.valueOf(year));
                
                } else if (idx == mode.getIndex(SoftpediaSearchMode._CATEGORY)) {
                    String category = value.lastIndexOf("\\") > -1 ? value.substring(value.lastIndexOf("\\") + 1) :
                                      value.lastIndexOf("/") > -1 ? value.substring(value.lastIndexOf("/") + 1) : value;
                                      
                    StringTokenizer categories = new StringTokenizer(category, "/");
                    while (categories.hasMoreElements()) {
                        DataLayer.getInstance().createReference(dco, Software._K_CATEGORIES, StringUtils.trim((String) categories.nextElement()));
                    }
                    
                } else if (idx == mode.getIndex(SoftpediaSearchMode._LICENSE)) {
                    String license = value.indexOf("/") > -1 ? value.substring(0, value.indexOf("/")) : value;
                    DataLayer.getInstance().createReference(dco, Software._Z_LICENSE, StringUtils.trim(license));
                
                } else if (idx == mode.getIndex(SoftpediaSearchMode._GENRE)) {
                    String license = value.indexOf("/") > -1 ? value.substring(value.lastIndexOf("/") + 1) : null;
                    
                    if (license != null && StringUtils.trim(license).equalsIgnoreCase("free"))
                        DataLayer.getInstance().createReference(dco, Software._Z_LICENSE, "Freeware");
                        
                    String genre = value.indexOf("/") > -1 ? value.substring(0, value.lastIndexOf("/")) : null;
                    StringTokenizer genres = new StringTokenizer(genre, "/");
                    while (genres.hasMoreElements()) {
                        String category = StringUtils.trim((String) genres.nextElement());
                        
                        if (category.equalsIgnoreCase("Shareware Games"))
                            DataLayer.getInstance().createReference(dco, Software._Z_LICENSE, "Shareware");
                        else if (category.equalsIgnoreCase("Freeware Games"))
                            DataLayer.getInstance().createReference(dco, Software._Z_LICENSE, "Freeware");
                        else    
                            DataLayer.getInstance().createReference(dco, Software._K_CATEGORIES, category);
                    }
                
                } else if (idx == mode.getIndex(SoftpediaSearchMode._SIZE_OS)) {
                    String size = value.indexOf("/") > -1 ? value.substring(0, value.indexOf("/")).trim() : value.trim();
                    String os = value.indexOf("/") > -1 ? value.substring(value.indexOf("/") + 1) : null;
                    
                    if (os != null) {
                        os = StringUtils.trim(os);
                        os = os.toLowerCase().startsWith("windows") ? "Windows" : os;
                        os = os.endsWith(" or later") ? os.substring(0, os.length() - 9) : os;
                        os = os.endsWith(" or higher") ? os.substring(0, os.length() - 10) : os;
                        DataLayer.getInstance().createReference(dco, Software._H_PLATFORM, os);
                    }
                    
                    try {
                        if (size.lastIndexOf(" ") > -1) {
                            String type = StringUtils.trim(size.substring(size.lastIndexOf(" ")));
                            size = size.substring(0, size.lastIndexOf(" "));
                            Long kb = Double.valueOf((Double.valueOf(StringUtils.trim(size)) * (type.equalsIgnoreCase("KB") ? 1000 : type.equalsIgnoreCase("MB") ? 1000000 : 1000000000))).longValue();
                            dco.setValue(Software._SYS_FILESIZE, kb);
                        }
                    } catch (NumberFormatException nfe) {}
                }       
                
                idx++;
            }
        }
    }
    
    private void setTitle(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//td[@class='pagehead2']//h1", document, XPathConstants.NODE);
        if (node != null) {
            String title = node.getTextContent();
            dco.setValue(DcMediaObject._A_TITLE, StringUtils.trim(title));
        }
    }

    private void setDescription(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//div[@class='desch2']", document, XPathConstants.NODE);
        if (node != null) {
            String description = StringUtils.trim(node.getTextContent());
            description = description.replaceAll("\t", "");
            dco.setValue(DcMediaObject._B_DESCRIPTION, description);
        }
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();
        
        SoftpediaSearchMode mode = (SoftpediaSearchMode) getMode();
        Document document = HtmlUtils.getDocument(new URL(mode.getSearchCommand(getQuery())), "UTF8");

        NodeList nodeList = (NodeList) xpath.evaluate("html//table[@class='narrow_listheadings']//a[starts-with(@href, '" + 
                mode.getKeyBaseURL()+ "')]/@href", document, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++) {
            String link = nodeList.item(i).getTextContent();            
            if (!keys.contains(link) && link.toLowerCase().indexOf("/trailer/") == -1)
                keys.add(link);
        }
        return keys;
    }
}
