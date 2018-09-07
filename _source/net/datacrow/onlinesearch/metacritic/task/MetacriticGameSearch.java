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
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Software;
import net.datacrow.core.resources.DcResources;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class MetacriticGameSearch extends MetacriticSearch {

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public MetacriticGameSearch(IOnlineSearchClient listener, IServer server,
            SearchMode mode, String query) {

        super(listener, server, mode, query);
    }

    @Override
    protected DcObject getItem(Object key, boolean full) throws Exception {
        return getItem(new URL(getServer().getUrl() + "/games/platforms" + key), full);
    }
    
    @Override
    protected DcObject getItem(URL url, boolean full) throws Exception {
        DcObject software = DcModules.get(DcModules._SOFTWARE).getItem();

        Document document = HtmlUtils.getDocument(url, "UTF-8");
        
        String link = url.toString();
        String id = link.substring(link.indexOf("platforms") + 9);
        
        software.addExternalReference(DcRepository.ExternalReferences._METACRITICS, id);
        
        setTitle(software, document);
        software.setValue(Software._I_WEBPAGE, link);
        software.setValue(DcObject._SYS_SERVICEURL, link);

        setDescription(software, document);
        setPlatform(software, link);
        setYear(software, document);
        setRating(software, document);
        setInfo(software, Software._K_CATEGORIES, document, "Genre(s):", ",");
        
        if (full) {
            setInfo(software, Software._G_PUBLISHER, document, "Publisher:", "/");
            setInfo(software, Software._F_DEVELOPER, document, "Developer:", "/");
            setMultiplayer(software, document);
            setFrontImage(software, Software._M_PICTUREFRONT, document);
        }
        
        return software;
    }
    
    private void setPlatform(DcObject software, String link) throws Exception {
        String platform = StringUtils.getValueBetween("platforms/", "/", link);
        platform = platform.equalsIgnoreCase("xbx") ? "Xbox" :
                   platform.equalsIgnoreCase("pc") ? "Windows" : platform;
        DataLayer.getInstance().createReference(software, Software._H_PLATFORM, platform);
    }
    
    private void setMultiplayer(DcObject software, Document document) throws Exception {
        String players = getInfo(software, document, "Players:");
        if (players != null) {
            try {
                int count = Integer.valueOf(StringUtils.trim(players));
                software.setValue(Software._L_MULTI, count > 1 ? DcResources.getText("lblYes") : DcResources.getText("lblNo"));
            } catch (NumberFormatException nfe) {}
        }
    }

    private void setYear(DcObject software, Document document) throws Exception {
        String date = getInfo(software, document, "Release Date:");
        if (date != null && date.indexOf(", ") > -1) {
            String year = date.substring(date.indexOf(", ") + 2);
            try {
                software.setValue(Software._C_YEAR, Long.valueOf(year));
            } catch (NumberFormatException nfew) {}
        }
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();
        
        String searchURL = getServer().getUrl() + "/search/process?sort=relevance&ty=3&ts=" + getQuery();
        Document document = HtmlUtils.getDocument(new URL(searchURL), "UTF8");
        NodeList nodeList = (NodeList) xpath.evaluate("html//a[starts-with(@href,'http://www.metacritic.com/games/')]/@href", document, XPathConstants.NODESET);
        int length = nodeList.getLength();
        for(int i = 0; i < length; i++) {
            String link = nodeList.item(i).getTextContent();
            link = link.substring(link.indexOf("/games/platforms") + "/games/platforms".length());
            link = link.indexOf("?") > -1 ? link.substring(0, link.indexOf("?")) : link;
            keys.add(link);
        }
        return keys;
    }
    
}
