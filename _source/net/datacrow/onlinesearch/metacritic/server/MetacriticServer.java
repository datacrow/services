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

package net.datacrow.onlinesearch.metacritic.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.DcSettings;
import net.datacrow.core.settings.Setting;

public abstract class MetacriticServer implements IServer {
    
    private Collection<Region> regions = new ArrayList<Region>();
    private Collection<SearchMode> modes = new ArrayList<SearchMode>();
    
    public MetacriticServer() {
        regions.add(new Region("en", "Default (english)", "http://www.metacritic.com"));
    }
    
    public String getName() {
        return "Metacritic";
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public Collection<SearchMode> getSearchModes() {
        return modes;
    }

    public String getUrl() {
        return "http://www.metacritic.com";
    }

    public Collection<Setting> getSettings() {
        Collection<Setting> settings = new ArrayList<Setting>();
        settings.add(DcSettings.getSetting(DcRepository.Settings.stMetacriticRetrieveCriticReviews));
        return settings;
    }   
    
    @Override
    public String toString() {
        return getName();
    }
}
