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

package net.datacrow.onlinesearch.metacritic.task;

import java.net.URL;
import java.util.StringTokenizer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.objects.DcMediaObject;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Software;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.SearchTaskUtilities;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.DcSettings;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class MetacriticSearch extends SearchTask {

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public MetacriticSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }

    protected abstract DcObject getItem(URL url, boolean full) throws Exception;
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }

    @Override
    protected void preSearchCheck() {
        SearchTaskUtilities.checkForIsbn(this);
    }
    
    protected void setTitle(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("html//h1", document, XPathConstants.NODE);
        if (node != null) dco.setValue(DcMediaObject._A_TITLE, node.getTextContent());
    }
    
    protected void setDescription(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("html//p[@class='summarytext']", document, XPathConstants.NODE);
        String description = node != null ? StringUtils.trim(node.getTextContent()) : "";
        
        if (DcSettings.getBoolean(DcRepository.Settings.stMetacriticRetrieveCriticReviews)) {
            NodeList publications = (NodeList) xpath.evaluate("html//p[@class='quote']", document, XPathConstants.NODESET);
            for (int i = 0; i < publications.getLength(); i++) {
                description += description.length() > 0 ? "\n\n" : "";
                description += StringUtils.trim(publications.item(i).getTextContent());
            }
        }
        
        if (description.length() > 0) 
            dco.setValue(Software._B_DESCRIPTION, description);
    }
    
    protected void setFrontImage(DcObject dco, int fieldIdx, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("html//div[@id='bigpic']/img/@src", document, XPathConstants.NODE);
        if (node != null) {
            String link = getServer().getUrl() + node.getTextContent();
            byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
            if (image != null) dco.setValue(fieldIdx, new DcImageIcon(image));
        }
    }

    protected void setRating(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("html//div[@id='metascore']", document, XPathConstants.NODE);
        if (node != null) {
            try {
                int rating = Integer.valueOf(StringUtils.trim(node.getTextContent()));
                dco.setValue(DcMediaObject._E_RATING, Long.valueOf(Math.round(rating / 10)));
            } catch (NumberFormatException nfe) {}
        }
    }

    
    protected String getInfo(DcObject dco, Document document, String tag) throws Exception {
        Node node = (Node) xpath.evaluate("//strong[text()='" + tag + "']", document, XPathConstants.NODE);
        if (node != null) {
            String value = node.getParentNode().getTextContent();
            return value.substring(tag.length() + 1);
        }
        return null;
    }
    
    protected void setInfo(DcObject dco, int fieldIdx, Document document, String tag, String seperator) throws Exception {
        String values = getInfo(dco, document, tag);
        if (values != null) {
            seperator = seperator == null ? " " : seperator;
            StringTokenizer st = new StringTokenizer(values, seperator);
            while (st.hasMoreElements()) {
                String value = (String) st.nextElement();
                DataLayer.getInstance().createReference(dco, fieldIdx, StringUtils.trim(value));
            }
        }
    }
}
