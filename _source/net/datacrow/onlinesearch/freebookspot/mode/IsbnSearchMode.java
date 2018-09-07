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


public class IsbnSearchMode extends net.datacrow.core.services.IsbnSearchMode {
    
    public IsbnSearchMode() {
        super(Book._J_ISBN10);
    }
    
	@Override
	public String getSearchCommand(String s) {
	    s = super.getSearchCommand(s);
	    s = getIsbn(s);
	    return "http://www.freebookspot.in/Default.aspx?__EVENTTARGET=&__EVENTARGUMENT=&TTitleSearch=&TAuthorSearch=&TISBNSearch=" + s;
	}
}
