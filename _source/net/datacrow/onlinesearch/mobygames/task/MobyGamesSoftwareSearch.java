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
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Software;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnection;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * @author Robert Jan van der Waals
 */
public class MobyGamesSoftwareSearch extends MobyGamesSearch {

    private static Logger logger = Logger.getLogger(MobyGamesSoftwareSearch.class.getName());

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public MobyGamesSoftwareSearch(IOnlineSearchClient listener, IServer server, String query) {
        super(listener, server,  query);
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        
        Collection<Object> ids = new ArrayList<Object>();
        String url = getAddress() + "search/quick?q=" + getQuery();
        
        
        Document document = HtmlUtils.getDocument(new URL(url), "UTF-8");
        
        String expression = "//a[starts-with(@href,'/game/')]/@href";
        NodeList nodeList = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        int length = nodeList.getLength();
        for(int i = 0; i < length; i++) {
            String key = nodeList.item(i).getTextContent();
            if (!ids.contains(key))
                ids.add(key);
        }

        return ids;
    }

    @Override
    protected DcObject getItem(Object id, boolean full) throws Exception {
        String ID = (String) id;
        ID = ID.startsWith("/") ? ID.substring(1, ID.length()) : ID;
        String address = ID.toLowerCase().startsWith("http") ? ID : getAddress() + ID;
        return getItem(new URL(address), full);
    }
   
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }
    
    protected DcObject getItem(URL url, boolean full) throws Exception {
        DcObject software = DcModules.get(DcModules._SOFTWARE).getItem();
        
        String page = HttpConnectionUtil.getInstance().retrievePage(url);
        
        String s = url.toString();
        String ID = s.substring(s.toLowerCase().lastIndexOf("mobygames.com/") + 14);

        String title = StringUtils.getValueBetween("<title>", "</title>", page);
        title = title.indexOf("- MobyGames") > -1 ? title.substring(0, title.indexOf("- MobyGames")) : title;
        
        software.setValue(Software._A_TITLE, title.trim());
        software.setValue(Software._I_WEBPAGE, url.toString());
        software.setValue(DcObject._SYS_SERVICEURL, url.toString());
        software.addExternalReference(DcRepository.ExternalReferences._MOBYGAMES, ID);
        
        try {
            String released = getInfo(page, "RELEASED</DIV>", "RELEASED");
            if (released.indexOf(".") > -1)
                released = released.substring(released.lastIndexOf(".") + 1, released.length());
            else if (released.indexOf(",") > -1)
                released = released.substring(released.lastIndexOf(",") + 1, released.length()).trim();
            
            software.setValue(Software._C_YEAR, Long.valueOf(released));
        } catch (Exception ignore) {}

        
        if (full) {
            // try to shorten the result string
            int idx = page.toUpperCase().indexOf("<DIV CLASS=\"RIGHTPANEL\">");
            page = idx > -1 ? page.substring(idx, page.length()) : page;
            
            setCompany(software, page, "PUBLISHED BY</DIV>", "PUBLISHED", Software._G_PUBLISHER);
            setCompany(software, page, "DEVELOPED BY</DIV>", "DEVELOPED", Software._F_DEVELOPER);
    
            software.setValue(Software._B_DESCRIPTION, getDescription(page, "DESCRIPTION</H2>", "RIGHTPANELMAIN", "<DIV"));
    
            DataLayer.getInstance().createReference(software, Software._H_PLATFORM, getInfo(page, "PLATFORM</DIV>", "PLATFORM"));
            DataLayer.getInstance().createReference(software, Software._K_CATEGORIES, getInfo(page, "GENRE</DIV>", "COREGAMEGENRE"));
            
            setCovers(software, url.toString());
            setScreenshots(software, ID, page);
            
            setRating(software, page);
        }
        
        return software;        
    }
    
    private void setRating(DcObject software, String page) {
        String s = "";
        if (page.indexOf("<td>Rating</td>") > -1) {
            int idx = page.indexOf("<td>Rating</td>");
            s = page.substring(idx, page.indexOf("stars", idx));
            s = StringUtils.getValueBetween("alt=", "\"", s);
        } else if (page.indexOf("scoreBoxMed") > -1) {
            s = page.substring(page.indexOf("scoreBoxMed"));
            s = StringUtils.getValueBetween(">", "</div>", s);
        }
            
        if (s.trim().length() > 0) {
            try {
                float f = Float.valueOf(s) * 2;
                software.setValue(Software._E_RATING, Long.valueOf(Math.round(f)));
            } catch (Exception e) {
                logger.warn("Could not parse the rating for " + software + ". Value invalid : " + s);
            }
        }
    }
    
    
    private void setCompany(DcObject software, String page, String tag1, String tag2, int field) {
        String html = page;
        int module = DcModules.getReferencedModule(software.getField(field)).getIndex();
        int idx = html.toUpperCase().indexOf(tag1) > -1 ? html.toUpperCase().indexOf(tag1) :
                  html.toUpperCase().indexOf(tag2);

        String name = null;
        URL url = null;

        if (idx > -1) {
            html = html.substring(idx);
            String link = StringUtils.getValueBetween("a href=\"", "\"", html);
          
            try {
                if (link.trim().length() > 0) {
                    name = StringUtils.getValueBetween("a href=\"", "</a>", html);
                    name = HtmlUtils.toPlainText(name.substring(name.indexOf(">") + 1));
                  
                    link = link.startsWith("/") ? link.substring(1) : link;
                    link = getServer().getUrl() + link;
                    url = new URL(link);
                }
            } catch (Exception e) {
                logger.error("Could not create / retrieve valid URL for company", e);
            }
        }
              
        if (name == null || url == null)
            return;
      
        try {
            String link = url.toString();
            String id = link.substring(link.lastIndexOf("/") + 1);
            
            DcObject company = DataLayer.getInstance().getObjectByExternalID(module, DcRepository.ExternalReferences._MOBYGAMES, id); 
            company = company == null ? DataLayer.getInstance().getObjectForString(module, name) : company;
            
            if (company == null) {
                if (DcModules.get(DcModules._SOFTWARE).getSettings().getBoolean(DcRepository.ModuleSettings.stOnlineSearchSubItems)) {
                    MobyGamesCompany companyFinder = new MobyGamesCompany(module, true);
                    company = companyFinder.get(url);
                } else {
                    company = DcModules.get(module).getItem();
                    company.setValue(DcAssociate._A_NAME, name);
                }
                
                // The name found using this finder might change from the orginal. Check again.
                DcObject existing = DataLayer.getInstance().getObjectForString(module, (String) company.getValue(DcAssociate._A_NAME));
                if (existing == null) {
                    company.setIDs();
                } else {
                    company.clearValues(true);
                    company = existing;
                }
            }
            
            company.addExternalReference(DcRepository.ExternalReferences._MOBYGAMES, id);
            DataLayer.getInstance().createReference(software, field, company);
          
        } catch (Exception e) {
            logger.error("Error while saving company " + name, e);
        }
    }
    
    private void setCovers(DcObject software, String address) throws Exception {
        try {
            String page = HttpConnectionUtil.getInstance().retrievePage(address + "/cover-art");

            String frontCoverTag = "FRONT COVER\"";
            String backCoverTag = "BACK COVER\"";
            String mediaCoverTag = "MEDIA\"";
            
            setMediaImage(software, frontCoverTag, Software._M_PICTUREFRONT, page);
            setMediaImage(software, backCoverTag, Software._N_PICTUREBACK, page);
            setMediaImage(software, mediaCoverTag, Software._O_PICTURECD, page);

        } catch (Exception e) {
            if (logger.isDebugEnabled())
                logger.debug("Image not found at " + address + "/covert-art", e);
        }
    }
    
    private void setScreenshots(DcObject software, String ID, String mainPage) throws Exception {
        String link = getImagesLink(ID, mainPage);
        int[] fields = {Software._P_SCREENSHOTONE, Software._Q_SCREENSHOTTWO, Software._R_SCREENSHOTTHREE};

        try {
            String page = HttpConnectionUtil.getInstance().retrievePage(getAddress() + link);
            String check = (ID + "/SCREENSHOTS/").toUpperCase();
            int idx = page.toUpperCase().indexOf(check);
            int counter = 0;
            while (idx > -1 && counter < fields.length && !isCancelled()) {
                int start = StringUtils.backtrack(page, idx, "<a href=\"");
                start = start > -1 ? start : idx;
                
                String part = page.substring(start);
                part = part.startsWith("/") ? part.substring(1) : part;
                
                String imagePageUrl = part.substring(0,  part.indexOf("\">"));
                
                part = HttpConnectionUtil.getInstance().retrievePage(getAddress() + imagePageUrl);
                int screenIdx = part.toLowerCase().indexOf("screenshot:");
                if (screenIdx > -1) {
                    try {
                        part = part.substring(screenIdx);
                        part = part.substring(part.toLowerCase().indexOf("src=\"") + 5);
                        String imageUrl = part.substring(0, part.indexOf("\""));
                        
                        imageUrl = !imageUrl.toLowerCase().startsWith("http") ? 
                                   getAddress() + (imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl) :
                                   imageUrl;
                        
                        software.setValue(fields[counter], 
                                new DcImageIcon(HttpConnectionUtil.getInstance().retrieveBytes(imageUrl)));
                    } catch (Exception e) {
                        logger.error("Error while retrieving screenshot", e);
                    }
                }
                counter++;
                page = page.substring(idx + check.length());
                idx = page.toUpperCase().indexOf(check);
            }
        } catch (Exception e) {
            logger.error("Error while retrieving screenshots", e);
        }
    }
    
    private String getImagesLink(String ID, String page) {
        int idx = page.indexOf(ID + "/screenshots");
        idx = idx > -1 ? StringUtils.backtrack(page, idx, "<a href=\"") : idx;
        if (idx > -1) {
            String link = page.substring(idx);
            link = link.substring(0, link.indexOf("\">"));
            return link.startsWith("/") ? link.substring(1) : link;
        } else {
            String link = ID + "/screenshots/";
            try {
                HttpConnection conn = HttpConnectionUtil.getInstance().getConnection(new URL(getAddress() + link));
                if (conn.exists()) {
                    conn.close();
                    return link;
                }
    
                link = "game" + ID + "/screenshots/";
                conn = HttpConnectionUtil.getInstance().getConnection(new URL(getAddress() + link)); 
                if (conn.exists()) {
                    conn.close();
                    return link;
                }
            } catch (Exception e) {
                logger.error("Error while trying to retrieve valid images URL [" + link + "]", e);
            }
            return link;
        }
    }
    
    private void setMediaImage(DcObject software, String tag, int field, String page) {
        String html = page;
        try {
            int idx = html.toUpperCase().indexOf(tag);
            if (idx > -1) {
                String part = html.substring(0,  idx);
                part = part.substring(part.lastIndexOf("\n"));
                
                idx = part.indexOf("<a href=\"") > -1 ? part.indexOf("<a href=\"") + 9 : part.indexOf("src=\"") + 5;
                part = part.substring(idx);
                part = part.substring(0, part.indexOf("\""));
                part = part.startsWith("/") ? part.substring(1, part.length()) : part;
                
                String imagePageUrl = part.startsWith("http") ? part : getAddress() + part;
                
                html = HttpConnectionUtil.getInstance().retrievePage(imagePageUrl);
                
                idx = html.toLowerCase().indexOf("http://www.mobygames.com/images/covers/");
                if (idx > -1) {
                    part = html.substring(idx);
                    part = part.substring(0, part.indexOf("\""));
                    
                    String imageUrl = part.startsWith("http") ? part : getAddress() + part;
                    byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(imageUrl);
                    software.setValue(field, new DcImageIcon(image));
                }
            }
        } catch (Exception e) {
            logger.error("Error while retrieving media images", e);
        }
    }
    
    private String getDescription(String page, String s1, String s2, String end) {
        String s = page.toUpperCase();
        int idx = s.indexOf(s1) > -1 ? s.indexOf(s1) : s.indexOf(s2);
        String part = null;
        if (idx > -1) {
            part = page.substring(idx, page.length());
            idx = part.toUpperCase().indexOf(end);
            if (idx > -1) {
                idx = part.toUpperCase().indexOf("</H2>");
                part = idx > -1 ? part.substring(idx + 5, part.length()) : part;
                part = part.substring(0, part.toUpperCase().indexOf(end));
                
                int index = part.indexOf("<a");
                while (index > -1) {
                    try {
                        String first = part.substring(0, index);
                        
                        String temp = part.substring(index, part.length());
                        String middle = temp.substring(temp.indexOf(">") + 1, temp.indexOf("</a>"));
                        String last = temp.substring(temp.indexOf("</a>") + 4);
                        part = first + middle + last;
                        
                        index = part.indexOf("<a");
                    } catch (Exception e) {
                        logger.warn("Error while cleaning description text", e);
                    }
                }
                part = HtmlUtils.toPlainText(part);
            }
        }
        return part;
    }
    
    private String getInfo(String page, String s1, String s2) {
        String orgPage = page;
        String s = page.toUpperCase();
        int idx = s.indexOf(s1) > -1 ? s.indexOf(s1) : s.indexOf(s2);
        String part = null;

        try {
            if (idx > -1) {
                part = orgPage.substring(idx, orgPage.length());
                part = part.substring(part.toUpperCase().indexOf("<A HREF=\""), part.length());
                part = part.substring(part.indexOf(">") + 1, part.toUpperCase().indexOf("</A>"));
                part = HtmlUtils.toPlainText(part);
            }
        } catch (StringIndexOutOfBoundsException exp) {}
        
        return part;
    }
}
