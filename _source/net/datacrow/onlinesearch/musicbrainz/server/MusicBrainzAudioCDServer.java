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

package net.datacrow.onlinesearch.musicbrainz.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.AudioCD;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.onlinesearch.musicbrainz.mode.AlbumByArtistSearch;
import net.datacrow.onlinesearch.musicbrainz.mode.AlbumTitleSearch;
import net.datacrow.onlinesearch.musicbrainz.task.MusicBrainzAudioCdSearch;

public class MusicBrainzAudioCDServer extends MusicBrainzServer {
    
    private Collection<SearchMode> modes = new ArrayList<SearchMode>();
    
    public MusicBrainzAudioCDServer() {
        super();
        modes.add(new AlbumTitleSearch(AudioCD._A_TITLE));
        modes.add(new AlbumByArtistSearch(AudioCD._A_TITLE));
    }

    public int getModule() {
        return DcModules._AUDIOCD;
    }

    public Collection<SearchMode> getSearchModes() {
        return modes;
    }

    public SearchTask getSearchTask(IOnlineSearchClient listener,
                                    SearchMode mode, 
                                    Region region, 
                                    String query,
                                    DcObject client) {
        
        MusicBrainzAudioCdSearch task = new MusicBrainzAudioCdSearch(listener, this, region, mode, query);
        task.setClient(client);
        return task;
    }
}
