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

package net.datacrow.onlinesearch.amazon.mode;

import net.datacrow.util.isbn.ISBN;

public class IsbnSearchMode extends net.datacrow.core.services.IsbnSearchMode {

    private final String index;
    
    public IsbnSearchMode(String index, int fieldBinding) {
        super(fieldBinding);
        this.index = index;
    }

    @Override
    public String getSearchCommand(String s) {
        s = super.getSearchCommand(s);
		String isbn13 = s;
		try {
			if (ISBN.isISBN10(s))
				isbn13 = ISBN.getISBN13(s);
		} catch (Exception e) {}
		
		return "Operation=ItemLookup&ItemId=" + isbn13 + "&IdType=ISBN&SearchIndex=" + index;
    }
}
