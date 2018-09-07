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

package net.datacrow.onlinesearch.amazon.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.DcSettings;
import net.datacrow.core.settings.Setting;

public abstract class AmazonServer implements IServer {

    private Collection<Region> regions = new ArrayList<Region>();
    
    public AmazonServer() {
        regions.add(new Region("us", "United States (english)", "ecs.amazonaws.com"));
        regions.add(new Region("uk", "United Kingdom (english)", "ecs.amazonaws.co.uk"));
        regions.add(new Region("de", "Germany (german)", "ecs.amazonaws.de"));
        regions.add(new Region("fr", "France (french)", "ecs.amazonaws.fr"));
        regions.add(new Region("ca", "Canada (english)", "ecs.amazonaws.ca"));
    }
    
    public String getName() {
        return "Amazon";
    }

    public Collection<Region> getRegions() {
        return regions;
    }
    
    public Collection<Setting> getSettings() {
        Collection<Setting> settings = new ArrayList<Setting>();
        settings.add(DcSettings.getSetting(DcRepository.Settings.stAmazonRetrieveFeatureListing));
        settings.add(DcSettings.getSetting(DcRepository.Settings.stAmazonRetrieveEditorialReviews));
        settings.add(DcSettings.getSetting(DcRepository.Settings.stAmazonRetrieveUserReviews));
        return settings;
    }    

    public String getUrl() {
        return "ecs.amazonaws.com";
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
