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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcObject;
import net.datacrow.util.XMLParser;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MusicBrainzArtist {

    private static Logger logger = Logger.getLogger(MusicBrainzArtist.class.getName());
    
    private final String address;
    
    public MusicBrainzArtist(String address, boolean full) {
        this.address = address;
    }
    
    public DcObject get(String id) throws Exception {
        return get(new URL(getLink(id)));
    }    
    
    public DcObject get(URL url) throws Exception {
        return parse(HttpConnectionUtil.getInstance().retrievePage(url));
    }
    
    public String getLink(Object id) {
        return address + "artist/" + id + "?type=xml&inc=sa-Album";
    }
    
    public DcObject parse(String xml) throws Exception {
        DcObject artist = DcModules.get(DcModules._MUSICARTIST).getItem(); 
            
        Document document = getDocument(xml);
        Element e = document.getDocumentElement();

        Element elArtist = (Element) e.getElementsByTagName("artist").item(0);
        String id = elArtist.getAttribute("id");
        String link = getLink(id);

        artist.setValue(DcObject._SYS_SERVICEURL, link);
        artist.addExternalReference(DcRepository.ExternalReferences._MUSICBRAINZ, id);
        
        String name = XMLParser.getString(e, "name");
        
        StringBuffer sb = new StringBuffer("");
        if (e.getElementsByTagName("life-span") != null) {
            Element ndLife = (Element) e.getElementsByTagName("life-span").item(0);
            if (ndLife != null) {
                NamedNodeMap nnm = ndLife.getAttributes();
                
                Node ndBegin = nnm.getNamedItem("begin");
                Node ndEnd = nnm.getNamedItem("end");
                if (ndBegin != null && ndEnd != null) {
                    String yrBegin = ndBegin.getNodeValue();
                    String yrEnd = ndEnd.getNodeValue();
                    sb.append("Active period: " + yrBegin + " - " + yrEnd + "\r\n");
                }
            }
        }
        
        Collection<String> c = new ArrayList<String>();
        
        NodeList releases = document.getElementsByTagName("release");
        for (int i = 0; i < releases.getLength(); i++) {
            Element release = (Element) releases.item(i);
            String title = XMLParser.getString(release, "title"); 
            if (!c.contains(title))
                c.add(title);
        }
        
        if (c.size() > 0) {
            sb.append(sb.length() > 0 ? "\r\n" : "");
            sb.append("releases:\r\n");
        }
        
        for (String s : c) {
            sb.append(s);
            sb.append("\r\n");
        }
        
        artist.setValue(DcAssociate._A_NAME, name);
        artist.setValue(DcAssociate._B_DESCRIPTION, sb.toString());
        
        return artist;                
    }
    
    protected Document getDocument(String xml) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            return db.parse(bis);
        } catch(Exception e) {
            logger.error("Error occurred while parsing XML", e);
            logger.debug(xml);
        }
        return null;
    }      
}
