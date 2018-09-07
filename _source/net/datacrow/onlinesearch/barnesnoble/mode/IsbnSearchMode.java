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

package net.datacrow.onlinesearch.barnesnoble.mode;

import net.datacrow.util.isbn.ISBN;


public class IsbnSearchMode extends net.datacrow.core.services.IsbnSearchMode {
    
    public IsbnSearchMode(int fieldBinding) {
        super(fieldBinding);
    }
    
	@Override
	public String getSearchCommand(String s) {
	    s = super.getSearchCommand(s);
	    
	    try {
	        s = ISBN.getISBN10(s);
	    } catch (Exception e) {}
	    
		return "http://search.barnesandnoble.com/booksearch/isbnInquiry.asp?ISBSRC=Y&ISBN=" + s;
	}
}
