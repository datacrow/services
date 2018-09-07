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
import net.datacrow.util.http.HttpConnectionUtil;
import net.datacrow.util.isbn.ISBN;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BolBookSearch extends SearchTask {

    private static Logger logger = Logger.getLogger(BolBookSearch.class.getName());

    private final static XPath xpath = XPathFactory.newInstance().newXPath();

    public BolBookSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }

    @Override
    protected DcObject getItem(Object key, boolean full) throws Exception {
        return getItem(new URL(getServer().getUrl() + key), full);
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

        Document document = HtmlUtils.getDocument(url, "UTF-8");

        String link = url.toString();

        if (link.endsWith(".html")) {
            String id = link.substring(0, link.lastIndexOf("/"));
            id = id.substring(id.lastIndexOf("/") + 1);
            book.addExternalReference(DcRepository.ExternalReferences._BOL, id);
        } else {
            logger.error("Could not extract ID for Bol.com link " + link);
        }

        setTitle(book, document);
        setDescription(book, document);
        setAuthor(book, document, full);
        setIsbn(book, document);

        if (full) {
            setImage(book, document);
            setOther(book, document);
        }

        book.setValue(Book._H_WEBPAGE, link);
        book.setValue(DcObject._SYS_SERVICEURL, link);

        return book;
    }

    private String clean(String s) {
        String cleaned = s.replaceAll("\n", "");
        cleaned = cleaned.replaceAll("\r", "");
        cleaned = cleaned.startsWith("Beschrijving") ? cleaned.substring(12) : cleaned;
        cleaned = cleaned.endsWith("Terug naar het overzicht") ? cleaned.substring(0, cleaned.indexOf("Terug naar het overzicht")) : cleaned;
        return cleaned.trim();
    }

    private void setOther(DcObject book, Document document) throws Exception {
        NodeList nodelist = (NodeList) xpath.evaluate("//div[@class='first_tab_paragraph']/label", document, XPathConstants.NODESET);
        int length = nodelist == null ? 0 : nodelist.getLength();
        for (int i = 0; i < length; i++) {
            Node node = nodelist.item(i);
            String text = clean(node.getTextContent());
            if (text.toLowerCase().startsWith("genre:")) {
                DataLayer.getInstance().createReference(book, Book._I_CATEGORY, text.substring(6).trim());
            } else if (text.toLowerCase().startsWith("verschijningsjaar:")) {
                try {
                    book.setValue(Book._C_YEAR, Long.valueOf(text.substring(18).trim()));
                } catch (NumberFormatException nfe) {}
            } else if (text.toLowerCase().startsWith("serie:")) {
                book.setValue(Book._O_SERIES, text.substring(6).trim());
            } else if (text.toLowerCase().startsWith("uitgever:")) {
                DataLayer.getInstance().createReference(book, Book._F_PUBLISHER, text.substring(9).trim());
            }
        }
    }

    private void setIsbn(DcObject book, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//dl[1]", document, XPathConstants.NODE);

        if (node != null) {
            String text = node.getTextContent();
            int idx = text.toUpperCase().indexOf("ISBN13");
            if (idx > -1) {
                String isbn = text.substring(idx + 7);
                isbn = isbn.substring(0, isbn.indexOf("\n"));
                if (ISBN.isISBN13(isbn)) {
                    book.setValue(Book._N_ISBN13, isbn);
                    book.setValue(Book._J_ISBN10, ISBN.getISBN10(isbn));
                }
            }
        }
    }

    private void setTitle(DcObject book, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//h1[@class='main_header']", document, XPathConstants.NODE);
        String title = node.getTextContent();
        book.setValue(Book._A_TITLE, clean(title));
    }

    private void setDescription(DcObject book, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//div[@id='js_product_description']", document, XPathConstants.NODE);
        if (node != null) {
            String description = node.getTextContent();
            book.setValue(Book._B_DESCRIPTION, clean(description));
        } else {
            node = (Node) xpath.evaluate("//div[@class='product_description']", document, XPathConstants.NODE);
            if (node != null) {
                String description = node.getTextContent();
                book.setValue(Book._B_DESCRIPTION, clean(description));
            }
        }
    }

    private void setAuthor(DcObject book, Document document, boolean full) throws Exception {
        Node node = (Node) xpath.evaluate("//div[@class='accesoires_author']", document, XPathConstants.NODE);

        if (node == null)
            return;

        String author = node.getTextContent();
        author = clean(author);
        Element element = (Element) xpath.evaluate("//div[@class='author_details']/a", document, XPathConstants.NODE);
        if (element != null) {
            String link = element.getAttribute("href");
            BolAuthor.setAuthor(book, author, getServer().getUrl() + link, full);
        } else {
            DataLayer.getInstance().createReference(book, Book._G_AUTHOR, author);
        }
    }

    private void setImage(DcObject book, Document document) throws Exception {
        Element element = (Element) xpath.evaluate("//li[@id='item_1']/a", document, XPathConstants.NODE);
        if (element != null) {
            String link = element.getAttribute("href");
            if (link.toLowerCase().startsWith("http://")) {
                byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
                book.setValue(Book._K_PICTUREFRONT, new DcImageIcon(image));
            }
        }

        // no image found: use the thumbnail picture if available.
        if (book.getValue(Book._K_PICTUREFRONT) == null) {
            element = (Element) xpath.evaluate("//div[@class='product_image']/img", document, XPathConstants.NODE);
            if (element != null) {
                String link = element.getAttribute("src");
                if (link.toLowerCase().startsWith("http://")) {
                    byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(link);
                    book.setValue(Book._K_PICTUREFRONT, new DcImageIcon(image));
                }
            }
        }
    }

    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();
        Document document = HtmlUtils.getDocument(new URL(getMode().getSearchCommand(getQuery())), "UTF8");
        NodeList nodeList = (NodeList) xpath.evaluate("html//a", document, XPathConstants.NODESET);
        int length = nodeList.getLength();
        for(int i = 0; i < length; i++) {
            Element element = (Element) nodeList.item(i);
            String link = element.getAttribute("href");
            link = link.indexOf(";") > -1 ? link.substring(0,link.indexOf(";")) : link;
            if (link.startsWith("/nl/p/") && !keys.contains(link) && link.indexOf("boeken/") > -1)
                keys.add(link);
        }
        return keys;
    }
}
