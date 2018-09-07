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
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTaskUtilities;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.util.isbn.ISBN;
import net.datacrow.util.isbn.InvalidBarCodeException;

import org.w3c.dom.Document;

public class AmazonBookSearch extends AmazonSearch {

    public AmazonBookSearch(IOnlineSearchClient listener, 
                            IServer server, 
                            Region region,
                            SearchMode mode,
                            String query) {
    
        super(listener, server, region, mode, query);
    }

    @Override
    protected void preSearchCheck() {
        SearchTaskUtilities.checkForIsbn(this);
    }

    @Override
    protected DcObject getItem(Object o, boolean full) throws Exception {
        Document document = o instanceof Document ? (Document) o : getDocument((String) o);
        DcObject book = DcModules.get(DcModules._BOOK).getItem();
        
        setValue(document, "ASIN", book, DcObject._SYS_EXTERNAL_REFERENCES);
        String asin = book.getExternalReference(DcRepository.ExternalReferences._ASIN);
        setServiceURL(book, asin);
        
        setValue(document, "ItemAttributes/Title", book, Book._A_TITLE);
        setYear(document, "ItemAttributes/PublicationDate", book, Book._C_YEAR);
        setNumberOfPages(document, book);
        setISBN(document, book);
        setValue(document, "DetailPageURL", book, Book._H_WEBPAGE);
        setRating(document, book, Book._E_RATING);
        
        setValue(document, "ItemAttributes/Binding", book, Book._U_BINDING);
        setValue(document, "ItemAttributes/Edition", book, Book._V_EDITION_TYPE);
        setValue(document, "ItemAttributes/Languages/Language[Type='Published']/Name", book, Book._D_LANGUAGE);
        
        if (full) {
            setDescription(document, book, Book._B_DESCRIPTION);
            setValue(document, "ItemAttributes/Publisher", book, Book._F_PUBLISHER);
            setValue(document, "ItemAttributes/Author", book, Book._G_AUTHOR);
            setImages(book, document, Book._K_PICTUREFRONT, null);
        }

        return book;
    }
    
    private void setNumberOfPages(Document document, DcObject book) {
        String s = getValue(document, "ItemAttributes/NumberOfPages");
        if (s != null) {
            try {
                book.setValue(Book._T_NROFPAGES, Long.parseLong(s));
            } catch (NumberFormatException nfe) {}
        }
    }
    
    private void setISBN(Document document, DcObject book) {
        String s =  getValue(document, "ItemAttributes/ISBN");
        try {
            if (ISBN.isISBN10(s)) {
                book.setValue(Book._J_ISBN10, s);
                book.setValue(Book._N_ISBN13, ISBN.getISBN13(s));
            } else if (ISBN.isISBN13(s)) {
                book.setValue(Book._N_ISBN13, s);
                book.setValue(Book._J_ISBN10, ISBN.getISBN10(s));
            }
        } catch (InvalidBarCodeException ibe) {
            listener.addError(ibe);
        }
    }
}
