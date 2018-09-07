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

import net.datacrow.core.objects.DcObject;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.util.http.HttpConnectionUtil;

public class MusicBrainzArtistSearch extends MusicBrainzSearch {

    public MusicBrainzArtistSearch(IOnlineSearchClient listener, 
                                   IServer server, 
                                   Region region, 
                                   SearchMode mode,
                                   String query) {
        
        super(listener, server, region, mode, query);
    }
    
    @Override
	protected DcObject getItem(Object id, boolean full) throws Exception {
		URL url = new URL(new MusicBrainzArtist(getServer().getUrl(), true).getLink(id));
		return getItem(url);
	}

    @Override
	protected DcObject getItem(URL url) throws Exception {
        String xml = HttpConnectionUtil.getInstance().retrievePage(url);
		return new MusicBrainzArtist(getServer().getUrl(), true).parse(xml);
	}

    @Override
	protected Collection<Object> getItemKeys() {
		Collection<Object> ids = new ArrayList<Object>();
		
		try {
			String url = getAddress() + "/artist/?type=xml&limit=" + getMaximum() + "&name=" + getQuery();
			String page = HttpConnectionUtil.getInstance().retrievePage(url);
			ids.addAll(getKeys(page, "artist"));
		} catch (Exception e) {
			listener.addError(e);
		}
			
		return ids;
	}
}
