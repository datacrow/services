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

package net.datacrow.onlinesearch.discogs.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModule;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcMediaObject;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.AudioCD;
import net.datacrow.core.objects.helpers.AudioTrack;
import net.datacrow.core.objects.helpers.MusicAlbum;
import net.datacrow.core.objects.helpers.MusicTrack;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.Utilities;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DiscogsAudioCdSearch extends SearchTask {

    private static Logger logger = Logger.getLogger(DiscogsAudioCdSearch.class.getName());
    
    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public DiscogsAudioCdSearch(IOnlineSearchClient listener, 
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
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> ids = new ArrayList<Object>();
        
        String link = getAddress() + getMode().getSearchCommand(getQuery());
        String page = HttpConnectionUtil.getInstance().retrievePage(link);
        
        int idx = page.indexOf("/release/");
        while (idx > -1) {
            String id = StringUtils.getValueBetween("/release/", "\"", page);
            
            String number = StringUtils.getContainedNumber(id);
            if (!Utilities.isEmpty(number) && !ids.contains(number))
                ids.add(number);
            
            page = page.substring(idx + 10);
            idx = page.indexOf("/release/");
        }
        
        return ids;
    }
    
    @Override
    protected DcObject getItem(Object id, boolean full) throws Exception {
        String link = getAddress() +  "release/" + id;
        return getItem(new URL(link), full);
    }
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }
    
    protected DcObject getItem(URL url, boolean full) throws Exception {
        
        Document document = HtmlUtils.getDocument(url, "UTF-8");
        
    	DcModule module = DcModules.get(getServer().getModule());
        DcObject album = module.getItem();

        String link = url.toString();
        String id = link.substring(link.indexOf("release/") + 8);
        
        album.setValue(DcObject._SYS_SERVICEURL, link);
        album.addExternalReference(DcRepository.ExternalReferences._DISCOGS, id);

        setTitle(album, document);
        setYear(album, document);
        setCountry(album, document);
        setWebpage(album, link);
        
        Collection<DcObject> artists = setArtist(album, document);
        addTracks(album, artists, document);
        
        if (full) {            
            setGenres(album, document);
            setImages(album, document);
        }
        
        return album;
    }
    
    private String getValue(Document document, String tag) throws Exception {
        Node node = (Node) xpath.evaluate("//div[text()='" + tag + "']/following-sibling::div", document, XPathConstants.NODE);
        return node != null ? StringUtils.trim(node.getTextContent()) : null;
    }
    
    private void setTitle(DcObject album, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
        String title = node.getTextContent();
        title = title.indexOf("-") > -1 ? title.substring(title.indexOf("-") + 1) : title;
        title = title.indexOf(" at Discogs") > -1 ? title.substring(0, title.indexOf(" at Discogs")) : title;
        album.setValue(DcMediaObject._A_TITLE, title.trim());
    }
    
    private void setYear(DcObject dco, Document document) throws Exception {
        String year = getValue(document, "Released:");
        if (!Utilities.isEmpty(year)) {
            DcModule module = DcModules.get(getServer().getModule());
            year = year.indexOf(" ") > -1 ? year.substring(year.lastIndexOf(" ") + 1) : year;
            int fieldIdx = module.getIndex() == DcModules._AUDIOCD ? AudioCD._C_YEAR : MusicAlbum._C_YEAR;
            dco.setValue(fieldIdx, Long.valueOf(year));
        }
    }    
    
    private void setCountry(DcObject dco, Document document) throws Exception {
        String country = getValue(document, "Country:");
        if (!Utilities.isEmpty(country)) {
            DcModule module = DcModules.get(getServer().getModule());
            int fieldIdx = module.getIndex() == DcModules._AUDIOCD ? AudioCD._F_COUNTRY : MusicAlbum._F_COUNTRY;
            DataLayer.getInstance().createReference(dco, fieldIdx, country);
        }
    } 
    
    private void setGenres(DcObject dco, Document document) throws Exception {
        String genres = getValue(document, "Genre:");
        if (!Utilities.isEmpty(genres)) {
            DcModule module = DcModules.get(getServer().getModule());
            StringTokenizer st = new StringTokenizer(genres, ",");
            int fieldIdx = module.getIndex() == DcModules._AUDIOCD ? AudioCD._G_GENRES : MusicAlbum._G_GENRES;
            while (st.hasMoreElements())
                DataLayer.getInstance().createReference(dco, fieldIdx, StringUtils.trim((String) st.nextElement()));
        }
    }    

    private Collection<DcObject> setArtist(DcObject album, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
        String artist = node.getTextContent();
        artist = artist.indexOf("-") > -1 ? artist.substring(0, artist.indexOf("-")) : artist;
        artist = artist.trim();
        
        Collection<DcObject> artists = new ArrayList<DcObject>();
        if (artist.length() > 0) {
            StringTokenizer st = new StringTokenizer(artist, ","); 
            DcModule module = DcModules.get(getServer().getModule());
            while (st.hasMoreElements())
                artists.add(DataLayer.getInstance().createReference(album, module.getIndex() == DcModules._AUDIOCD ? AudioCD._F_ARTISTS : MusicAlbum._F_ARTISTS, 
                        StringUtils.trim(((String) st.nextElement()))));
        }
        
        return artists;
    }
    
    private void setWebpage(DcObject dco, String link) {
        DcModule module = DcModules.get(getServer().getModule());
        int fieldIdx = module.getIndex() == DcModules._AUDIOCD ? AudioCD._M_WEBPAGE : MusicAlbum._N_WEBPAGE;   
        dco.setValue(fieldIdx, link);
    }
    
    private void addTracks(DcObject album, Collection<DcObject> artists, Document document) throws Exception {
        String syntax = "//h3[text()='Tracklist']/following-sibling::div/table/tr";
        NodeList tracks = (NodeList) xpath.evaluate(syntax, document, XPathConstants.NODESET);
        
        boolean isAudioTrack = album.getModule().getChild().getIndex() == DcModules._AUDIOTRACK;
        
        if (tracks == null) return;
        
        for (int i = 0; i < tracks.getLength(); i++) {
            
            Node nTrack = tracks.item(i);
            
            DcObject track = album.getModule().getChild().getItem();
            
            Node nNumber = (Node) xpath.evaluate(syntax + "[" + (i + 1) + "]/td[@class='track_pos']", nTrack, XPathConstants.NODE);
            Node nTitle = (Node) xpath.evaluate(syntax + "[" + (i + 1) + "]/td[@class='track_title']", nTrack, XPathConstants.NODE);
            Node nLength = (Node) xpath.evaluate(syntax + "[" + (i + 1) + "]/td[@class='track_duration']", nTrack, XPathConstants.NODE);
            NodeList nArtists = (NodeList) xpath.evaluate(syntax + "[" + (i + 1) + "]/td[@class='track_artists']/a", nTrack, XPathConstants.NODESET);
            
            if (nNumber != null) {
                String number = StringUtils.trim(nNumber.getTextContent());
                number = number.replaceAll("A", number.length() == 2 ? "10" : "1");
                number = number.replaceAll("B", number.length() == 2 ? "20" : "2");
                number = number.replaceAll("C", number.length() == 2 ? "30" : "3");
                number = number.replaceAll("D", number.length() == 2 ? "40" : "4");
                number = number.replaceAll("E", number.length() == 2 ? "50" : "5");
                number = number.replaceAll("F", number.length() == 2 ? "60" : "6");
                number = number.replaceAll("G", number.length() == 2 ? "70" : "7");
                number = number.replaceAll("H", number.length() == 2 ? "80" : "8");
                number = number.replaceAll("I", number.length() == 2 ? "90" : "9");
                number = number.replaceAll("J", number.length() == 2 ? "100" : "10");
                number = number.replaceAll("K", number.length() == 2 ? "110" : "11");
                number = number.replaceAll("L", number.length() == 2 ? "120" : "12");
                
                try {
                    track.setValue(isAudioTrack ? AudioTrack._F_TRACKNUMBER : MusicTrack._F_TRACKNUMBER, Long.valueOf(number));
                } catch (NumberFormatException nfe) {
                    logger.debug("Could not set track number for " + album, nfe);
                }
            }
            
            if (nTitle != null) {
                String title = StringUtils.trim(nTitle.getTextContent());
                track.setValue(DcMediaObject._A_TITLE, title);
            }
            
            if (nLength != null) {
                String length = StringUtils.trim(nLength.getTextContent());
                
                if (length.indexOf(":") > -1) {
                    String minutes = length.substring(0, length.indexOf(":"));
                    String seconds = length.substring(length.indexOf(":") + 1);
                    
                    try {
                        Integer min = Integer.valueOf(minutes);
                        Integer sec = Integer.valueOf(seconds);
                        track.setValue(isAudioTrack ? AudioTrack._H_PLAYLENGTH : MusicTrack._J_PLAYLENGTH, Long.valueOf(sec.intValue() + (min.intValue() * 60)));
                    } catch (NumberFormatException nfe) {}
                }
            }

            if (nArtists != null) {
                for (int j = 0; j < nArtists.getLength(); j++) {
                    Node nArtist = nArtists.item(j);
                    DataLayer.getInstance().createReference(track, isAudioTrack ? AudioTrack._K_ARTIST : MusicTrack._G_ARTIST, nArtist.getTextContent());
                }
            }
            
            // not a track..
            if (!Utilities.isEmpty(track.getValue(isAudioTrack ? AudioTrack._F_TRACKNUMBER : MusicTrack._F_TRACKNUMBER)))
                album.addChild(track);
        }
    }

    private void setImages(DcObject album, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//a[starts-with(@href,'/viewimages?release=')]/@href", document, XPathConstants.NODE);
        
        if (node == null) return;
        
        Document imgDoc = HtmlUtils.getDocument(new URL(getServer().getUrl() + node.getTextContent().substring(1)), "UTF-8");
        NodeList nodes = (NodeList) xpath.evaluate("//img[starts-with(@src,'http://www.discogs.com/image')]/@src", imgDoc, XPathConstants.NODESET);
        
        if (nodes == null) return;
        
        // TODO: Fix this
//        for (int i = 0; i < nodes.getLength(); i++) {
//            try {
//                Node nImage = nodes.item(i);
//                
//                byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(nImage.getTextContent());
//                if (!isCancelled()) {
//                    PictureSelectDialog dlg = 
//                        new PictureSelectDialog(listener instanceof JFrame ? (JFrame) listener : null, 
//                                                album, new net.datacrow.util.DcImageIcon(image));
//                    dlg.setVisible(true);
//                }
//            } catch (Exception e) {
//                logger.error(e, e);
//            }
//        }
    }    
}
