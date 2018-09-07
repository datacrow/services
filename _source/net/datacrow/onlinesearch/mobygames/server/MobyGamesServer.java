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

package net.datacrow.onlinesearch.mobygames.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.Setting;

public abstract class MobyGamesServer implements IServer {

    private Collection<Region> regions = new ArrayList<Region>();
    
    public MobyGamesServer() {
        regions.add(new Region("us", "Default (english)", "http://www.mobygames.com/search/quick?q="));
    }
    
    public String getName() {
        return "MobyGames";
    }
    
    public Collection<Setting> getSettings() {
        return null;
    }   
    
    public Collection<SearchMode> getSearchModes() {
        return null;
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public String getUrl() {
        return "http://www.mobygames.com/";
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
