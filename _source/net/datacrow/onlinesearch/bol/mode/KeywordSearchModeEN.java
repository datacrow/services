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

package net.datacrow.onlinesearch.bol.mode;

import net.datacrow.core.services.SearchMode;

public class KeywordSearchModeEN extends SearchMode {

    public KeywordSearchModeEN(int fieldBinding) {
        super(fieldBinding);
    }
    
    @Override
    public String getDisplayName() {
        return "Keyword (english)";
    }

    @Override
    public String getSearchCommand(String s) {
        return "http://www.bol.com/nl/catalog/product-list.html?Nty=1&search=true&searchType=adv&section=books-en&Ntx=mode+matchallpartial&Ntk=ukus_book_title&Ntt=" + s;
    }

    @Override
    public boolean keywordSearch() {
        return true;
    }

    @Override
    public boolean singleIsPerfect() {
        return false;
    }
}
