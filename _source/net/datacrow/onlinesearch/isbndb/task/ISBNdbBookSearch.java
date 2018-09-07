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

package net.datacrow.onlinesearch.isbndb.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.Picture;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.OnlineSearchHelper;
import net.datacrow.core.services.OnlineServices;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.SearchTaskUtilities;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.onlinesearch.amazon.mode.ItemLookupSearchMode;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.Utilities;
import net.datacrow.util.isbn.ISBN;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ISBNdbBookSearch extends SearchTask {

    private static Logger logger = Logger.getLogger(ISBNdbBookSearch.class.getName());
    
    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    public ISBNdbBookSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
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

        String link = url.toString();

        book.setValue(Book._H_WEBPAGE, link);
        book.setValue(DcObject._SYS_SERVICEURL, link);
        
        Document document = HtmlUtils.getDocument(url, "UTF-8");
        
        setTitle(book, document);
        setDescription(book, document);
        setDetails(book, document);
        
        setISBN(book, document);
        
        if (full) {
            setPublisher(book, document);
            setAuthor(book, document);
            setASIN(book, document);
            retrieveAmazonDetails(book);
        }
        
        return book;
    }
    
    private void setTitle(DcObject book, Document document) throws Exception {
      Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
      book.setValue(Book._A_TITLE, node.getTextContent());
    }

    private void setDescription(DcObject book, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//H2[text()='Summary:']", document, XPathConstants.NODE);
        String description = getFollowingText(node);
        if (description != null) {
            description = description.replaceAll("\n", " ");
            description = description.replaceAll("\r", " ");
            book.setValue(Book._B_DESCRIPTION, description);
        }
    }  
    
    /**
     * Retrieves the image from Amazon
     * @param book
     */
    private void retrieveAmazonDetails(DcObject book) {
        String ASIN = book.getExternalReference(DcRepository.ExternalReferences._ASIN);
        if (ASIN != null) {
            OnlineServices os = book.getModule().getOnlineServices();
            IServer server = os.getServer("Amazon");
            
            if (server == null) return;
            
            OnlineSearchHelper osh = new OnlineSearchHelper(DcModules._BOOK, SearchTask._ITEM_MODE_FULL);
            osh.setServer(server);
            osh.setMaximum(1);
            osh.setRegion(os.getDefaultRegion());
            osh.setMode(new ItemLookupSearchMode(ItemLookupSearchMode._ASIN, "", "", Book._SYS_EXTERNAL_REFERENCES));
            List<DcObject> results = osh.query(ASIN, null);
            
            if (results.size() == 1) {
                DcObject dco = results.get(0);
                Picture picture = (Picture) dco.getValue(Book._K_PICTUREFRONT);

                if (picture != null) {
                    byte[] image = picture.getBytes();
                    
                    if (image != null)
                        book.setValue(Book._K_PICTUREFRONT, new DcImageIcon(image));
                    
                    if (dco.isFilled(Book._I_CATEGORY) && !book.isFilled(Book._I_CATEGORY))
                        book.setValue(Book._I_CATEGORY, dco.getValue(Book._I_CATEGORY));
    
                    if (dco.isFilled(Book._B_DESCRIPTION) && !book.isFilled(Book._B_DESCRIPTION))
                        book.setValue(Book._B_DESCRIPTION, dco.getValue(Book._B_DESCRIPTION));
                }
            }
        }
    }
    
    private void setDetails(DcObject book, Document document) throws Exception {
        Node node = (Node) xpath.evaluate("//H2[text()='Book Details:']", document, XPathConstants.NODE);
        String details = getFollowingText(node);
        
        if (details == null) return;
        
        String pages = StringUtils.getValueBetween("Physical Description:", "pages", details).trim();
        if (!Utilities.isEmpty(pages)) {
            try {
                pages = pages.indexOf(";") > 0 ? pages.substring(pages.indexOf(";") + 1).trim() : pages;
                book.setValue(Book._T_NROFPAGES, Long.parseLong(pages));
            } catch (NumberFormatException nfe) {
                logger.error("Could not set pages (ISBNdn.com)", nfe);
            }
        }
        
        String binding = StringUtils.getValueBetween("Edition Info:", "\n", details).trim();
        binding = details.indexOf("Edition Info:") > -1 ? details.substring(details.indexOf("Edition Info:") + 13) : details;
        if (!Utilities.isEmpty(binding)) {
            binding = binding.indexOf(";") > 0 ? binding.substring(0, binding.indexOf(";")) : binding;
            
            if (!binding.equalsIgnoreCase("Unknown Binding")) {
                binding = binding.toLowerCase();
                if (binding.indexOf("pbk") > -1 || binding.indexOf("paperback") > -1)
                    DataLayer.getInstance().createReference(book, Book._U_BINDING, "Paperback");
                else if (binding.indexOf("cover") > -1)
                    DataLayer.getInstance().createReference(book, Book._U_BINDING, "Hardcover");
            }
        }
    }   
    
    private void setPublisher(DcObject book, Document document) throws Exception {
        String expression = "//A[starts-with(@HREF,'/d/publisher/')]";
        Node node = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
        if (node != null)
            DataLayer.getInstance().createReference(book, Book._F_PUBLISHER, node.getTextContent());
    }
    
    private void setAuthor(DcObject book, Document document) throws Exception {
        String expression = "//A[starts-with(@HREF,'/d/person/')]";
        Node node = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
        if (node != null)
            DataLayer.getInstance().createReference(book, Book._G_AUTHOR, node.getTextContent());
    }
    
    private void setASIN(DcObject book, Document document) throws Exception {
        String expression = "//iframe[@id='iframeamazon']/@src";
        Node node = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
        if (node != null) {
            String text = node.getTextContent();
            text = StringUtils.getValueBetween("asins=", "&", text);
            if (text.length() > 0)
                book.addExternalReference(DcRepository.ExternalReferences._ASIN, text);
        }
    }    
    
    private void setISBN(DcObject book, Document document) throws Exception {
        String expression = "//DIV[@CLASS='bookInfo']";
        Node node = (Node) xpath.evaluate(expression, document, XPathConstants.NODE);
        if (node != null) {
            String isbn = StringUtils.getValueBetween("ISBN:", "\n", node.getTextContent()).trim();
            isbn = StringUtils.getContainedNumber(isbn);
            if (ISBN.isISBN10(isbn)) {
                book.setValue(Book._J_ISBN10, isbn);
                book.addExternalReference(DcRepository.ExternalReferences._ISBNDB, isbn);
            } else if (ISBN.isISBN13(isbn)) {
                book.setValue(Book._N_ISBN13, isbn);
                book.addExternalReference(DcRepository.ExternalReferences._ISBNDB, isbn);
            }
        }
    } 
    
    private String getFollowingText(Node node) {
        if (node == null) return null;
        
        StringBuffer sb = new StringBuffer();
        Node sibling = node.getNextSibling();
        while (sibling != null && (sibling.getNodeType() == Node.TEXT_NODE || sibling.getNodeName().equals("BR"))) {
            sb.append(sibling.getTextContent());
            sibling = sibling.getNextSibling();
        }
        
        return StringUtils.trim(sb.toString());
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> keys = new ArrayList<Object>();
        String link = getServer().getUrl() +  "/search-all.html?kw=" + getQuery();
        Document document = HtmlUtils.getDocument(new URL(link), "UTF-8");
        
        String expression = "//A[starts-with(@HREF,'/d/book/')]/@HREF";
        NodeList nodeList = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
        int length = nodeList.getLength();
        for(int i = 0; i < length; i++) {
            String key = nodeList.item(i).getTextContent();
            keys.add(key);
            
            if (ISBN.isISBN10(getQuery()) || ISBN.isISBN13(getQuery()))
                break;
        }
        return keys;
    }
}
