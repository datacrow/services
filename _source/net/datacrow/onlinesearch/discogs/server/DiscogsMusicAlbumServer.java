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

package net.datacrow.onlinesearch.discogs.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.MusicAlbum;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.Setting;
import net.datacrow.onlinesearch.discogs.mode.KeywordSearchMode;
import net.datacrow.onlinesearch.discogs.task.DiscogsAudioCdSearch;

public class DiscogsMusicAlbumServer implements IServer {

    private Collection<Region> regions = new ArrayList<Region>();
    private Collection<SearchMode> modes = new ArrayList<SearchMode>();
    
    public DiscogsMusicAlbumServer() {
        regions.add(new Region("us", "Default (english)", "http://www.discogs.com/"));
        modes.add(new KeywordSearchMode(MusicAlbum._A_TITLE));
    }
    
    public int getModule() {
        return DcModules._MUSICALBUM;
    }

    public String getName() {
        return "Discogs";
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public Collection<SearchMode> getSearchModes() {
        return modes;
    }

    public String getUrl() {
        return "http://www.discogs.com/";
    }

    public Collection<Setting> getSettings() {
        return null;
    }
    
    public SearchTask getSearchTask(IOnlineSearchClient listener, SearchMode mode, Region region, String query, DcObject client) {
        DiscogsAudioCdSearch task = new DiscogsAudioCdSearch(listener, this, region, mode, query);
        task.setClient(client);
        return task;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
