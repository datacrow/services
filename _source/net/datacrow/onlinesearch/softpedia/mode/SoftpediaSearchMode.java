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

package net.datacrow.onlinesearch.softpedia.mode;

import net.datacrow.core.objects.helpers.Software;

public abstract class SoftpediaSearchMode extends net.datacrow.core.services.SearchMode {
    
    public static final String _DEVELOPER = "DEVELOPER";
    public static final String _LICENSE = "LICENSE";
    public static final String _YEAR = "YEAR";
    public static final String _CATEGORY = "CATEGORY";
    public static final String _SIZE_OS = "SIZE_OS";
    public static final String _GENRE = "GENRE";
    
    public SoftpediaSearchMode() {
        super(Software._A_TITLE);
    }
    
    @Override
    public boolean keywordSearch() {
        return true;
    }

    @Override
    public boolean singleIsPerfect() {
        return false;
    }

    public abstract String getBaseURL();
    
    public int getIndex(String tag) {
        if (tag.equals(_DEVELOPER))
            return 0;
        else if (tag.equals(_LICENSE))
            return 1;
        else if (tag.equals(_SIZE_OS))
            return 2;        
        else if (tag.equals(_YEAR))
            return 3;
        else if (tag.equals(_CATEGORY))
            return 4;
        else if (tag.equals(_GENRE))
            return -1;        
        else
            return -1;
    }
    
    public String getKeyBaseURL() {
        return getBaseURL() + "/get/";
    }
    
    @Override
    public String getSearchCommand(String s) {
        return getBaseURL() + "/dyn-search.php?search_term=" + super.getSearchCommand(s);
    }    
}
