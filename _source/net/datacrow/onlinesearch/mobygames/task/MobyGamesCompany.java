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

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcObject;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionUtil;

public class MobyGamesCompany {
    
    private int module;
    private boolean full;
    
    public MobyGamesCompany(int module, boolean full) {
        this.module = module;
        this.full = full;
    }
    
    public DcObject get(URL url) throws Exception {
        DcAssociate person = (DcAssociate) DcModules.get(module).getItem();
        String page = HttpConnectionUtil.getInstance().retrievePage(url);
        
        String s = url.toString();
        String name = HtmlUtils.toPlainText(StringUtils.getValueBetween("<title>", "</title>", page));
        name = name.toLowerCase().startsWith("mobygames - ") ? name = name.substring(12, name.length()) : name;
        String id = s.substring(s.lastIndexOf("/") + 1);

        person.addExternalReference(DcRepository.ExternalReferences._MOBYGAMES, id);
        person.setValue(DcAssociate._A_NAME, name);
        person.setValue(DcAssociate._C_WEBPAGE, s);
        person.setValue(DcObject._SYS_SERVICEURL, s);

        if (full) {
            person.setValue(DcAssociate._B_DESCRIPTION, getDescription(s));
            setPhoto(person, page);
        }
        
        person.setName();        

        return person;
    }    
    
    private void setPhoto(DcObject dco, String page) throws Exception {
        int idx = page.indexOf("<img alt=\"company logo\"");
        if (idx == -1)
            return;
        
        String link = page.substring(idx);
        
        link = StringUtils.getValueBetween("src=\"", "\"", link).trim();
        if (link.length() > 0) {
            link = "http://www.mobygames.com" + link;
            byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
            dco.setValue(DcAssociate._D_PHOTO, new DcImageIcon(image));
        }
    }

    private String getDescription(String url) throws Exception {
        String desc = HttpConnectionUtil.getInstance().retrievePage(url + "/overview");
        int idx = desc.toLowerCase().indexOf("<div class=\"rightpanelmain\">");
        if (idx > -1) 
            desc = desc.substring(idx);
        else 
            return null;
        
        idx = desc.toLowerCase().indexOf("overview</h2>");
        if (idx > -1)
            desc = desc.substring(idx + 13);
        
        idx = desc.lastIndexOf("<br>");
        if (idx > -1)
            desc = desc.substring(0, idx);

        idx = desc.indexOf("<div");
        if (idx > -1)
            desc = desc.substring(0, idx);
        
        if (desc.length() > 50)
            return HtmlUtils.toPlainText(desc);
        else
            return null;
    }
}
