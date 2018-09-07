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

import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.onlinesearch.musicbrainz.task.MusicBrainzArtistSearch;

public class MusicBrainzArtistServer extends MusicBrainzServer {
    
    public MusicBrainzArtistServer() {
        super();
    }

    public int getModule() {
        return DcModules._MUSICARTIST;
    }

    public Collection<SearchMode> getSearchModes() {
        return null;
    }

    public SearchTask getSearchTask(IOnlineSearchClient listener,
                                    SearchMode mode, 
                                    Region region, 
                                    String query,
                                    DcObject client) {
        
        MusicBrainzArtistSearch task = new MusicBrainzArtistSearch(listener, this, region, mode, query);
        task.setClient(client);
        return task;
    }
}
