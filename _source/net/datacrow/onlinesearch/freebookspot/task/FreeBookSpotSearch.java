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

package net.datacrow.onlinesearch.freebookspot.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcMediaObject;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.SearchTaskUtilities;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.Utilities;
import net.datacrow.util.http.HttpConnectionUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FreeBookSpotSearch extends SearchTask {

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public FreeBookSpotSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }

    @Override
    protected DcObject getItem(Object key, boolean full) throws Exception {
        return getItem(new URL(getServer().getUrl() + key), full);
    }
    
    protected DcObject getItem(URL url, boolean full) throws Exception {
        DcObject book = DcModules.get(DcModules._BOOK).getItem();

        Document document = HtmlUtils.getDocument(url, "UTF-8");
        
        String link = url.toString();
        String id = link.substring(getServer().getUrl().length());
        
        book.addExternalReference(DcRepository.ExternalReferences._FREEBOOKSPOT, id);
        
        setTitle(book, document);
        book.setValue(Book._H_WEBPAGE, link);
        book.setValue(DcObject._SYS_SERVICEURL, link);

        setYear(book, document);
        setInfo(book, Book._G_AUTHOR, document, "FormView1_BookAuthorLabel", "/");
        setInfo(book, Book._F_PUBLISHER, document, "FormView1_PublisherLabel", "/");
        setInfo(book, Book._T_NROFPAGES, document, "FormView1_PagesLabel", null);
        setInfo(book, Book._D_LANGUAGE, document, "FormView1_BookLanguageLabel", "/");
        setInfo(book, Book._J_ISBN10, document, "FormView1_ISBNLabel", null);
        setInfo(book, Book._N_ISBN13, document, "FormView1_ISBNLabel0", null);
        setInfo(book, Book._U_BINDING, document, "FormView1_FormatLabel", null);
        
        setDescription(book, document);
        
        if (full) {
            setFrontImage(book, document);
        }
        
        return book;
    }
    
    private void setFrontImage(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//img[@id='CoverImage']/@src", document, XPathConstants.NODE);
        
        if (node != null) {
            String link = node.getTextContent();
            link = getServer().getUrl() + link.substring(1);
            byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
            if (image != null) dco.setValue(Book._K_PICTUREFRONT, new DcImageIcon(image));
        }
    }

    private void setYear(DcObject dco, Document document) throws Exception {
        String year = getInfo(dco, document, "FormView1_PublishDateLabel");
        if (year != null) {
            year = year.indexOf(",") > -1 ? year.substring(year.indexOf(",") + 1) : year;
            dco.setValue(DcMediaObject._C_YEAR, StringUtils.trim(year));
        }
    }
    
    private void setTitle(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//span[@id='Label1']", document, XPathConstants.NODE);
        if (node != null) {
            String title = node.getTextContent();
            title = title.toLowerCase().indexOf("free download") > -1 ? title.substring(0, title.toLowerCase().indexOf("free download")) : title;
            dco.setValue(DcMediaObject._A_TITLE, StringUtils.trim(title));
        }
    }

    private void setDescription(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//textarea[@id='TDescribtion']", document, XPathConstants.NODE);
        if (node != null) dco.setValue(DcMediaObject._B_DESCRIPTION, StringUtils.trim(node.getTextContent()));
    }
    
    protected String getInfo(DcObject dco, Document document, String tag) throws Exception {
        Node node = (Node) xpath.evaluate("//span[@id='" + tag + "']", document, XPathConstants.NODE);
        if (node != null) {
            String value = node.getTextContent();
            return Utilities.isEmpty(value) ? null : value;
        }
        return null;
    }
    
    protected void setInfo(DcObject dco, int fieldIdx, Document document, String tag, String seperator) throws Exception {
        String values = getInfo(dco, document, tag);
        if (values != null) {
            seperator = seperator == null ? " " : seperator;
            
            if (dco.getField(fieldIdx).getFieldType() == DcRepository.Components._REFERENCEFIELD ||
                dco.getField(fieldIdx).getFieldType() == DcRepository.Components._REFERENCESFIELD) {
                StringTokenizer st = new StringTokenizer(values, seperator);
                while (st.hasMoreElements()) {
                    String value = (String) st.nextElement();
                    DataLayer.getInstance().createReference(dco, fieldIdx, StringUtils.trim(value));
                }
            } else {
                dco.setValue(fieldIdx, StringUtils.trim(values));
            }
        }
    }
    
    
    @Override
    protected void preSearchCheck() {
        SearchTaskUtilities.checkForIsbn(this);
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();

        Document document = HtmlUtils.getDocument(new URL(getServer().getUrl()), "UTF8");
        
        Node node = (Node) xpath.evaluate("html//input[@id='__VIEWSTATE']/@value", document, XPathConstants.NODE);
        String viewState = node.getTextContent();
        viewState = viewState.replaceAll("\\/", "%2F");
        viewState = viewState.replaceAll("\\+", "%2B");
        
        node = (Node) xpath.evaluate("html//input[@id='__EVENTVALIDATION']/@value", document, XPathConstants.NODE);
        String eventValidation = node.getTextContent(); 
        eventValidation = eventValidation.replaceAll("\\/", "%2F");
        eventValidation = eventValidation.replaceAll("\\+", "%2B");
        
        String searchURL = getMode().getSearchCommand(getQuery()) + "&__VIEWSTATE=" + viewState + "&__EVENTVALIDATION=" + eventValidation + "&__VIEWSTATEENCRYPTED=";
        document = HtmlUtils.getDocument(new URL(searchURL), "UTF8");
        
        NodeList nodeList = (NodeList) xpath.evaluate("html//a[starts-with(@href,'http://www.freebookspot.in/Books')]/@href", document, XPathConstants.NODESET);
        for(int i = 0; i < nodeList.getLength(); i++) {
            String link = nodeList.item(i).getTextContent();
            link = link.substring(getServer().getUrl().length());
            link = link.indexOf("?") > -1 ? link.substring(0, link.indexOf("?")) : link;
            
            if (!keys.contains(link))
                keys.add(link);
        }

        return keys;
    }
    
}
