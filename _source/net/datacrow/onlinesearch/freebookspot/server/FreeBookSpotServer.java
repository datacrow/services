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

package net.datacrow.onlinesearch.freebookspot.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.Setting;
import net.datacrow.onlinesearch.freebookspot.mode.IsbnSearchMode;
import net.datacrow.onlinesearch.freebookspot.mode.KeywordSearchMode;
import net.datacrow.onlinesearch.freebookspot.task.FreeBookSpotSearch;

public class FreeBookSpotServer implements IServer {
    
    private Collection<Region> regions = new ArrayList<Region>();
    private Collection<SearchMode> modes = new ArrayList<SearchMode>();
    
    public FreeBookSpotServer() {
        regions.add(new Region("en", "Default (english)", "http://www.freebookspot.in"));
        modes.add(new KeywordSearchMode());
        modes.add(new IsbnSearchMode());
    }
    
    public String getName() {
        return "FreeBookSpot";
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public Collection<SearchMode> getSearchModes() {
        return modes;
    }

    public String getUrl() {
        return "http://www.freebookspot.in";
    }

    public Collection<Setting> getSettings() {
        return null;
    }   
    
    @Override
    public int getModule() {
        return DcModules._BOOK;
    }
    
    public SearchTask getSearchTask(IOnlineSearchClient listener,
            SearchMode mode, 
            Region region, 
            String query,
            DcObject client) {

        FreeBookSpotSearch task = new FreeBookSpotSearch(listener, this, mode, query);
        task.setClient(client);
        return task;
    }    
    @Override
    public String toString() {
        return getName();
    }
}
