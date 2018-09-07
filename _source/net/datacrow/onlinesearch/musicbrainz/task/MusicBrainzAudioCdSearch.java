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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModule;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcField;
import net.datacrow.core.objects.DcMapping;
import net.datacrow.core.objects.DcMediaObject;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.AudioCD;
import net.datacrow.core.objects.helpers.AudioTrack;
import net.datacrow.core.objects.helpers.MusicAlbum;
import net.datacrow.core.objects.helpers.MusicTrack;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.OnlineSearchHelper;
import net.datacrow.core.services.OnlineServices;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionException;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;

public class MusicBrainzAudioCdSearch extends MusicBrainzSearch {

    private static Logger logger = Logger.getLogger(MusicBrainzAudioCdSearch.class.getName());
    
    public MusicBrainzAudioCdSearch(IOnlineSearchClient listener, 
                             IServer server, 
                             Region region, 
                             SearchMode mode,
                             String query) {
        
        super(listener, server, region, mode, query);
    }

    private String getLink(Object id) {
        return getAddress() + "/release/" + id + "?type=xml&inc=tracks+artist+release-events";
    }

    @Override
	protected DcObject getItem(Object id, boolean full) throws Exception {
        URL url = new URL(getLink(id));
        return getItem(url, full);
	}

    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }
    
	protected DcObject getItem(URL url, boolean full) throws Exception {
	    // occasional connection errors (forbidden)
	    String xml = "";
	    try {
	        xml = HttpConnectionUtil.getInstance().retrievePage(url);
	    } catch (HttpConnectionException hce) {
	        try {
	            sleep(1000);
	        } catch (Exception ignore) {}
	        xml = HttpConnectionUtil.getInstance().retrievePage(url);
	    }
        
        DcModule module = DcModules.get(getServer().getModule());
        DcObject dco = module.getItem();
        
        String link = url.toString();
        
        String id = link.substring(link.toLowerCase().indexOf("/release/") + 9);
        id = id.indexOf("?") > 0 ? id.substring(0, id.indexOf("?")) : id;
        dco.addExternalReference(DcRepository.ExternalReferences._MUSICBRAINZ, id);
        
        setTitle(dco, xml);
        setYear(dco, xml);
        setWebpage(dco, link);
        dco.setValue(DcObject._SYS_SERVICEURL, link);
        
        Collection<DcObject> artists = setArtists(dco, xml);
        addTracks(dco, artists, xml);

        if (full)
            addCovers(dco);
        
        return dco;
	}

    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        String url = getAddress() + "/release/?type=xml&limit=" + getMaximum();
        url += getMode().getSearchCommand(getQuery());
        String page = HttpConnectionUtil.getInstance().retrievePage(url);

        Collection<Object> keys = new ArrayList<Object>();
        keys.addAll(getKeys(page, "release"));

        return keys;
    }    
    
	private void setTitle(DcObject dco, String xml) {
		String title = HtmlUtils.toPlainText(StringUtils.getValueBetween("<title>", "</title>", xml));
		while (title.length() > 1 && (title.startsWith("\r") || title.startsWith("\n")))
		    title = title.substring(1);
		
		dco.setValue(DcMediaObject._A_TITLE, title);
	}
	
	private void setYear(DcObject dco, String xml) {
        String key = "event date=\"";
        int idx = xml.indexOf(key);
        if (idx > -1) {
            String s = xml.substring(idx + key.length(), xml.length());
            
        	String year = s.indexOf("-") > 0 ? s.substring(0, s.indexOf("-")) : 
        				  s.indexOf('"') > 0 ? s.substring(0, s.indexOf('"')) : null;

            if (year != null)
            	dco.setValue(DcMediaObject._C_YEAR, StringUtils.getContainedNumber(year));
        } 
	}	
	
	private void setWebpage(DcObject dco, String link) {
		dco.setValue(dco.getModule().getIndex() == DcModules._AUDIOCD ? AudioCD._M_WEBPAGE : MusicAlbum._N_WEBPAGE, link);
	}
	
	@SuppressWarnings("unchecked")
    private void addCovers(DcObject dco) {
        OnlineServices os = dco.getModule().getOnlineServices();
        IServer server = os.getServer("Amazon");
        
        if (server == null) return;
        
        OnlineSearchHelper osh = new OnlineSearchHelper(dco.getModule().getIndex(), SearchTask._ITEM_MODE_FULL);
        osh.setServer(server);
        osh.setMaximum(5);
        osh.setRegion(os.getDefaultRegion());
        
        String title = dco.getDisplayString(DcMediaObject._A_TITLE); 
        Collection<DcMapping> artists = (Collection<DcMapping>) dco.getValue(MusicAlbum._F_ARTISTS);
        
        Collection<DcObject> objects = osh.query(title, dco);
        for (DcObject result : objects) {
            String titleNew = (String) result.getValue(MusicAlbum._A_TITLE);
            Collection<DcMapping> artistsNew = (Collection<DcMapping>) result.getValue(MusicAlbum._F_ARTISTS);
            
            if (titleNew != null && artistsNew != null) {
                boolean sameArtists = false;
                
                for (DcObject artist : artists) {
                    for (DcObject artistNew : artistsNew) 
                        sameArtists |=  StringUtils.equals(artist.toString(), artistNew.toString());
                }
                
                if (StringUtils.equals(titleNew, title) && sameArtists) {
                    for (DcField field : dco.getFields()) {
                        if (field.getValueType() == DcRepository.ValueTypes._PICTURE)
                            dco.setValue(field.getIndex(), result.getValue(field.getIndex()));
                    }
                }
            }
        }
	}
    
    private void addTracks(DcObject dco, Collection<DcObject> artists, String xml) {
    	int childModIdx = dco.getModule().getChild().getIndex();
    	
        Collection<String> c = StringUtils.getValuesBetween("<track id", "</track>", xml);
        int number = 1;
        
        int titleIdx = childModIdx == DcModules._AUDIOTRACK ? AudioTrack._A_TITLE : MusicTrack._A_TITLE; 
        int nrIdx = childModIdx == DcModules._AUDIOTRACK ? AudioTrack._F_TRACKNUMBER : MusicTrack._F_TRACKNUMBER;
        int lengthIdx = childModIdx == DcModules._AUDIOTRACK ? AudioTrack._H_PLAYLENGTH : MusicTrack._J_PLAYLENGTH;
        int artistIdx = childModIdx == DcModules._AUDIOTRACK ? AudioTrack._K_ARTIST : MusicTrack._G_ARTIST;
        
        for (String s : c) {
            
            if (isCancelled()) break;
            
            DcObject track = DcModules.get(childModIdx).getItem();
            
            String title = HtmlUtils.toPlainText(StringUtils.getValueBetween("<title>", "</title>", s));
            
            while (title.length() > 1 && (title.startsWith("\r") || title.startsWith("\n")))
                title = title.substring(1);
            
            track.setValue(titleIdx, title);
            track.setValue(nrIdx, Long.valueOf(number));

            try {
                String duration = HtmlUtils.toPlainText(StringUtils.getValueBetween("<duration>", "</duration>", s));
                if (duration.length() > 0) {
                    int seconds = Integer.parseInt(duration.trim()) / 1000;
                    track.setValue(lengthIdx, Long.valueOf(seconds));
                }
            } catch (Exception e) {
                logger.warn("Could not set the play length for [" + track + "]", e);
            }
            
            for (DcObject artist : artists)
                DataLayer.getInstance().addMapping(track, artist, artistIdx);
            
            dco.addChild(track);            
            
            number++;
        }
    }
    
    private Collection<DcObject> setArtists(DcObject dco, String part) {
        Collection<String> artists = StringUtils.getValuesBetween("<artist", "</artist>", part);
        Collection<DcObject> result = new ArrayList<DcObject>();
        
        int fieldIdx = dco.getModule().getIndex() == DcModules._AUDIOCD ? AudioCD._F_ARTISTS : MusicAlbum._F_ARTISTS;
        
        for (String artist : artists) {
            try {
                String id = StringUtils.getValueBetween("id=\"", "\"", artist);
                String name = HtmlUtils.toPlainText(StringUtils.getValueBetween("<name>", "</name>", artist));
                
                int module = DcModules.getReferencedModule(dco.getField(fieldIdx)).getIndex();
                
                DcObject person = DataLayer.getInstance().getObjectByExternalID(module, DcRepository.ExternalReferences._MUSICBRAINZ, id);
                person = person == null ? DataLayer.getInstance().getObjectForString(module, name) : person;
                
                if (person == null) {
                    if (dco.getModule().getSettings().getBoolean(DcRepository.ModuleSettings.stOnlineSearchSubItems)) {
                        MusicBrainzArtist mbPerson = new MusicBrainzArtist(getServer().getUrl(), true);
                        person = mbPerson.get(id);
                    } else {
                        person = DcModules.get(module).getItem();
                        person.setValue(DcAssociate._A_NAME, name);
                        person.addExternalReference(DcRepository.ExternalReferences._MUSICBRAINZ, id);
                    }
                    person.setIDs();
                }
                
                result.add(person);
                DataLayer.getInstance().addMapping(dco, person, dco.getModule().getIndex() == DcModules._AUDIOCD ? AudioCD._F_ARTISTS : MusicAlbum._F_ARTISTS);
            } catch (Exception e) {
                logger.error("An error occurred while creating artist " + artist, e);
            }
        }
        
        return result;
    }
}
