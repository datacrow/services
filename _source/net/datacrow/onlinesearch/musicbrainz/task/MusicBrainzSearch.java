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

package net.datacrow.onlinesearch.musicbrainz.task;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class MusicBrainzSearch extends SearchTask {

    public MusicBrainzSearch(IOnlineSearchClient listener, 
                             IServer server, 
                             Region region, 
                             SearchMode mode,
                             String query) {
        
        super(listener, server, region, mode, query);
    }
    
    @Override
    public String getWhiteSpaceSubst() {
        return "+";
    }        
    
    protected Document getDocument(String xml) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            return db.parse(bis);
        } catch(Exception e) {
            listener.addError(e);
        }
        return null;
    }
    
    protected Collection<String> getKeys(String xml, String tag) {
        
        Document document = getDocument(xml);
        NodeList nl = document != null ? document.getElementsByTagName(tag) : null;
        
        Collection<String> ids = new ArrayList<String>();
        if (nl != null) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                NamedNodeMap nnm = node.getAttributes();
                String id = nnm.getNamedItem("id").getNodeValue();
                ids.add(id);
            }
        }
        return ids;
    }     
}
