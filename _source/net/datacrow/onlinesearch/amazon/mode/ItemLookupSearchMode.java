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

import java.util.HashMap;
import java.util.Map;

import net.datacrow.core.services.SearchMode;


public class ItemLookupSearchMode extends SearchMode {

    private final String label;
    private final String type;
    private String index;
    
    public static final String _ASIN = "ASIN";
    public static final String _UPC = "UPC";
    public static final String _EAN = "EAN";
    
    private static Map<String, String> descriptions = new HashMap<String, String>();
    
    static {
    	descriptions.put(_ASIN, "Asin");
    	descriptions.put(_UPC, "UPC Barcode");
    	descriptions.put(_EAN, "EAN Barcode");
    }
    
    public static String getDescription(String type) {
    	return descriptions.get(type);
    }
    
    public ItemLookupSearchMode(String index, String label, String type, int fieldBinding) {
        super(fieldBinding);
        this.label = label;
        this.type = type;
        this.index = index;
    }

    @Override
    public String getDisplayName() {
        return label;
    }

    @Override
    public String getSearchCommand(String s) {
    	if (type.equals(_ASIN))
    		return "Operation=ItemLookup&ItemId=" + s + "&IdType=" + type;
    	else
    		return "Operation=ItemLookup&ItemId=" + s + "&IdType=" + type + "&SearchIndex=" + index;
    }
    
    @Override
    public boolean singleIsPerfect() {
        return true;
    }
    
    @Override
    public boolean keywordSearch() {
        return false;
    }     
}
