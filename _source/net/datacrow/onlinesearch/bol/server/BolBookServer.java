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

package net.datacrow.onlinesearch.bol.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.Setting;
import net.datacrow.onlinesearch.bol.mode.AuthorSearchMode;
import net.datacrow.onlinesearch.bol.mode.AuthorSearchModeEN;
import net.datacrow.onlinesearch.bol.mode.IsbnSearchMode;
import net.datacrow.onlinesearch.bol.mode.IsbnSearchModeEN;
import net.datacrow.onlinesearch.bol.mode.KeywordSearchMode;
import net.datacrow.onlinesearch.bol.mode.KeywordSearchModeEN;
import net.datacrow.onlinesearch.bol.task.BolBookSearch;

public class BolBookServer implements IServer {
    
    private Collection<Region> regions = new ArrayList<Region>();
    private Collection<SearchMode> modes = new ArrayList<SearchMode>();

    public BolBookServer() {
        regions.add(new Region("nl", "Default (dutch)", "http://www.bol.com/"));
        modes.add(new KeywordSearchMode(Book._A_TITLE));
        modes.add(new KeywordSearchModeEN(Book._A_TITLE));
        modes.add(new IsbnSearchMode(Book._N_ISBN13));
        modes.add(new IsbnSearchModeEN(Book._N_ISBN13));
        modes.add(new AuthorSearchMode(Book._A_TITLE));
        modes.add(new AuthorSearchModeEN(Book._A_TITLE));
    }

    public int getModule() {
        return DcModules._BOOK;
    }

    public Collection<Setting> getSettings() {
        return null;
    }

    public String getName() {
        return "Bol.com";
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public Collection<SearchMode> getSearchModes() {
        return modes;
    }

    public String getUrl() {
        return "http://www.bol.com";
    }
    
    public SearchTask getSearchTask(IOnlineSearchClient listener, SearchMode mode, Region region, String query, DcObject client) {
        BolBookSearch task = new BolBookSearch(listener, this, mode, query);
        task.setClient(client);
        return task;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
