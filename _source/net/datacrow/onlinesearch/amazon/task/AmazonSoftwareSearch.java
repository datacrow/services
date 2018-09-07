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

package net.datacrow.onlinesearch.amazon.task;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Software;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.StringUtils;

import org.w3c.dom.Document;

public class AmazonSoftwareSearch extends AmazonSearch {


    public AmazonSoftwareSearch(IOnlineSearchClient listener, 
                                IServer server, 
                                Region region,
                                SearchMode mode,
                                String query) {
    
        super(listener, server, region, mode, query);
    }

    @Override
    protected DcObject getItem(Object o, boolean full) throws Exception {
        Document document = o instanceof Document ? (Document) o : getDocument((String) o);
        DcObject software = DcModules.get(DcModules._SOFTWARE).getItem();
        
        setValue(document, "ASIN", software, Software._SYS_EXTERNAL_REFERENCES);
        String asin = software.getExternalReference(DcRepository.ExternalReferences._ASIN);
        setServiceURL(software, asin);
        
        setValue(document, "ItemAttributes/Title", software, Software._A_TITLE);
        setValue(document, "ItemAttributes/Binding", software, Software._W_STORAGEMEDIUM);
        setValue(document, "ItemAttributes/EAN", software, Software._X_EAN);
        setValue(document, "DetailPageURL", software, Software._I_WEBPAGE);
        setValue(document, "ItemAttributes/Platform", software, Software._H_PLATFORM);
        setYear(document, "ItemAttributes/ReleaseDate", software, Software._C_YEAR);
        setCategory(document, software);
        
        if (full) {
            setRating(document, software, Software._E_RATING);
            setDescription(document, software, Software._B_DESCRIPTION);
            setValue(document, "ItemAttributes/Manufacturer", software, Software._F_DEVELOPER);
            setValue(document, "ItemAttributes/Publisher", software, Software._G_PUBLISHER);
          
            setImages(software, document, 
                      Software._M_PICTUREFRONT, 
                      new int[] {Software._P_SCREENSHOTONE, 
                                 Software._Q_SCREENSHOTTWO, 
                                 Software._R_SCREENSHOTTHREE});
        }
        
        return software;
    }
    
    private void setCategory(Document document, DcObject software) {
        String category = getValue(document, "ItemAttributes/Genre");
        if (category != null) {
            category = category.indexOf("_") > -1 ? category.substring(0, category.indexOf("_")) : category;
            DataLayer.getInstance().createReference(software, Software._K_CATEGORIES, StringUtils.capitalize(category));
        }
    }
}
