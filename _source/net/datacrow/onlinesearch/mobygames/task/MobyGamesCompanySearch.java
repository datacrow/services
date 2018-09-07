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

package net.datacrow.onlinesearch.mobygames.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.objects.DcObject;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionUtil;

public class MobyGamesCompanySearch extends MobyGamesSearch {
    
    private int module;

    public MobyGamesCompanySearch(IOnlineSearchClient listener, int module, IServer server, String query) {
        super(listener, server, query);
        this.module = module;
    }
    
    @Override
    protected DcObject getItem(Object id, boolean full) throws Exception {
        URL url = new URL(getServer().getUrl() + "company/" + id);
        return getItem(url, full);
    }
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }
    
    protected DcObject getItem(URL url, boolean full) throws Exception {
        MobyGamesCompany mgc = new MobyGamesCompany(module, full);
        return mgc.get(url);
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        String url = getAddress() + "search/site?search=" + getQuery() + "&c=1&c2=1";
        String webpage = HttpConnectionUtil.getInstance().retrievePage(url);
        Collection<Object> keys = new ArrayList<Object>();
        keys.addAll(StringUtils.getValuesBetween("<a href=\"/company/", "/", webpage));
        return keys;
    }
}