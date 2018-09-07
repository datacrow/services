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

package net.datacrow.onlinesearch.barnesnoble.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
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
import net.datacrow.util.isbn.ISBN;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BarnesNobleSearch extends SearchTask {

    private static Logger logger = Logger.getLogger(BarnesNobleSearch.class.getName());
    
    private final static XPath xpath = XPathFactory.newInstance().newXPath();
	
    public BarnesNobleSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }
    
    @Override
    protected DcObject getItem(Object id, boolean full) throws Exception {
        return getItem(new URL("http://search.barnesandnoble.com/booksearch/isbnInquiry.asp?z=y&EAN=" + id), full);
    }
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }

    @Override
    protected void preSearchCheck() {
        SearchTaskUtilities.checkForIsbn(this);
    }

    protected DcObject getItem(URL url, boolean full) throws Exception {
        
        DcObject book = DcModules.get(DcModules._BOOK).getItem();
        
        Document doc = HtmlUtils.getDocument(url, "iso-8859-1");
        
        String link = url.toString();
        
        String ean = getEAN(link);
        
        setTitle(book, doc);
        setYear(book, doc);
        setIsbn(book, ean);
        book.setValue(Book._H_WEBPAGE, link);
        book.setValue(DcObject._SYS_SERVICEURL, link);
        book.addExternalReference(DcRepository.ExternalReferences._BARNES_NOBLE, ean);
        
        if (full) {
            setDescription(book, doc);
            setPublisher(book, doc);
            setAuthor(book, doc);
            setCover(book, ean);
        }
        
        return book;
    }
    
    private void setCover(DcObject book, String ean) {
        try {
        	String url = ("http://search.barnesandnoble.com/booksearch/imageviewer.asp?ean=" + ean);
        	String page = HttpConnectionUtil.getInstance().retrievePage(url);
        	
        	int idx = page.indexOf("images:['http://images.barnesandnoble.com/");
        	if (idx > -1) {
        		String part = page.substring(idx + 9);
        		setPictureFront(part.substring(0, part.indexOf("'")), book);
        	} else {
        		idx = page.indexOf("id=\"LARGE_IMAGE\"");
        		if (idx > -1) {
        			String part = page.substring(idx);
        			part = part.substring(part.indexOf("http://"));
        			setPictureFront(part.substring(0, part.indexOf("\"")), book);
        		}
        	}
        	
        } catch (Exception e) {
        	logger.warn("Could load image for " + ean, e);
        }
    }
    
    private void setYear(DcObject book, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//li[@class='pubDate']", document, XPathConstants.NODE);
        if (node != null) {
            String year = StringUtils.getContainedNumber(node.getTextContent());
            if (!Utilities.isEmpty(year)) book.setValue(Book._C_YEAR, Long.valueOf(year));
        }
    }
    
    private void setPictureFront(String url, DcObject book) throws Exception {
    	byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(url);
    	book.setValue(Book._K_PICTUREFRONT, new DcImageIcon(image));    	
    }
    
    private String getEAN(String url) {
        return url.indexOf("&EAN=") > -1 ? url.substring(url.indexOf("&EAN=") + 5) : null;
    }
    
    private void setIsbn(DcObject dco, String ean) {
        if (ean != null) {
            if (ISBN.isISBN10(ean)) 
                dco.setValue(Book._J_ISBN10, ean);
            else if (ISBN.isISBN13(ean))
                dco.setValue(Book._N_ISBN13, ean);
        }
    }
    
    private void setAuthor(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
        String author = node.getTextContent();
        
        if (author.indexOf(",") > 0) {
            author = author.substring(0, author.lastIndexOf(","));
            author = author.substring(author.lastIndexOf(",") + 1);
            DataLayer.getInstance().createReference(dco, Book._G_AUTHOR, author.trim());
        }
    }
    
    private void setPublisher(DcObject dco, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//li[@class='publisher']", document, XPathConstants.NODE);
        if (node != null) {
            String publisher = node.getTextContent();
            publisher = publisher.toLowerCase().startsWith("publisher:") ?  publisher.substring(10).trim() : publisher;
            DataLayer.getInstance().createReference(dco, Book._F_PUBLISHER,  HtmlUtils.toPlainText(publisher));
        }
    }
    
    private void setDescription(DcObject dco, Document document) throws Exception {
        StringBuffer sb = new StringBuffer();
        
        Node node = (Node) xpath.evaluate("//h3[text()='Synopsis']/following-sibling::p", document, XPathConstants.NODE);
        if (node != null)
            sb.append(node.getTextContent());

        NodeList list = (NodeList) xpath.evaluate("//h3[text()='From Barnes & Noble']/following-sibling::p", document, XPathConstants.NODESET);
        if (list != null) {
            for (int i = 0; i < list.getLength(); i++) {
                String s = list.item(i).getTextContent();
                
                if (!s.startsWith("The Barnes & Noble Review")) {
                    sb.append(sb.length() > 0 ? "\n" : "");
                    sb.append(list.item(i).getTextContent());
                }
            }
        }

        node = (Node) xpath.evaluate("//h3[text()='Publisher']/following-sibling::p", document, XPathConstants.NODE);
        if (node != null) {
            sb.append(sb.length() > 0 ? "\n\n" : "");
            sb.append(node.getTextContent());
        }

        dco.setValue(Book._B_DESCRIPTION, sb.toString());
    }
    
    private void setTitle(DcObject dco, Document doc) throws Exception {
        Node node = (Node) xpath.evaluate("//title", doc, XPathConstants.NODE);
        String title = node.getTextContent();
        int idx = title.toLowerCase().indexOf("noble.com - books:");
        
        while ((idx = title.indexOf(",")) > -1)
            title = title.substring(0, idx);
        
        dco.setValue(Book._A_TITLE, title);
    }

    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();
        String html = HttpConnectionUtil.getInstance().retrievePage(getMode().getSearchCommand(getQuery()));
        
        int idx = html.indexOf("EAN=");
        while (idx > -1) {
            String s = html.substring(idx + 4);
            
            String ean = "";
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isDigit(c))
                    ean += c;
                else
                    break;
            }
            
            if (!keys.contains(ean) && (ISBN.isISBN10(ean) || ISBN.isISBN13(ean)))
                keys.add(ean);
            
            html = html.substring(idx + 10);
            idx = html.indexOf("EAN=");
        }
        
        return keys;
    }
}
