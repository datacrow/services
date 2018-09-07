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

package net.datacrow.onlinesearch.bol.task;

import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BolAuthor {
    
    private static Logger logger = Logger.getLogger(BolAuthor.class.getName());
    
    private final static XPath xpath = XPathFactory.newInstance().newXPath();

    public static void setAuthor(DcObject book, String name, String url, boolean details) throws Exception {
       DcObject author = null;
       if (name.trim().length() > 0)
           author = DataLayer.getInstance().getObjectForString(DcModules._AUTHOR, name);
       
       if (author == null && name.trim().length() > 0) {
           author = DcModules.get(DcModules._AUTHOR).getItem();
           Document document = HtmlUtils.getDocument(new URL(url), "UTF8");
           
           author.setValue(DcAssociate._A_NAME, name);
           author.setValue(DcAssociate._C_WEBPAGE, url);
           
           if (details) {
               setPhoto(author, document);
               setDescription(author, url);
           }
       }
       
       if (author != null) {
           author.setIDs();
           DataLayer.getInstance().addMapping(book, author, Book._G_AUTHOR);
       }
    }

    private static void setPhoto(DcObject author, Document document) throws Exception {
        Element element = (Element) xpath.evaluate("//div[@class='creator_auteur']/img", document, XPathConstants.NODE);
        if (element != null) {
            String link = element.getAttribute("src");
            
            if (link.toLowerCase().startsWith("http://") && link.indexOf("creatorimage_168x210") == -1) {
                link = link.replaceAll("\n", "");
                byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
                author.setValue(DcAssociate._D_PHOTO, new DcImageIcon(image));
            }
        }
    }
    
    private static void setDescription(DcObject author, String url) throws Exception {
        try {
            url = url.replaceAll("index.html", "biografie.html");
            Document document = HtmlUtils.getDocument(new URL(url), "UTF8");
            Node node = (Node) xpath.evaluate("//div[@id='product_biography']", document, XPathConstants.NODE);
            if (node != null) {
                String description = clean(node.getTextContent());
                description = description.startsWith("Biografie") ? description.substring(9) : description;
                author.setValue(DcAssociate._B_DESCRIPTION, clean(description));
            }
        } catch (Exception e) {
            logger.error(e, e);
        }
    }
    
    private static String clean(String s) {
        String cleaned = s.replaceAll("\n", "");
        cleaned = cleaned.replaceAll("\r", "");
        return cleaned.trim();
    }
}
