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

package net.datacrow.onlinesearch.freebookspot.mode;

import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.SearchMode;


public class KeywordSearchMode extends SearchMode {
    
    public KeywordSearchMode() {
        super(Book._A_TITLE);
    }
    
    @Override
    public String getDisplayName() {
    	return "Keyword";
    }

    @Override
    public String getSearchCommand(String s) {
        return "http://www.freebookspot.in/Default.aspx?__EVENTTARGET=&__EVENTARGUMENT=&TTitleSearch=" + s + "&TAuthorSearch=&TISBNSearch=";
    }

    @Override
    public boolean singleIsPerfect() {
        return false;
    }

    @Override
    public boolean keywordSearch() {
        return true;
    }     
}
