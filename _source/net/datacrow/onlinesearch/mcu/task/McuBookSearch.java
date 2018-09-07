package net.datacrow.onlinesearch.mcu.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.objects.helpers.Movie;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.SearchTaskUtilities;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.http.HttpConnectionUtil;
import net.datacrow.util.isbn.ISBN;

public class McuBookSearch extends SearchTask {

    public McuBookSearch(IOnlineSearchClient listener, 
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
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> ids = new ArrayList<Object>();
        String url = getAddress() + getMode().getSearchCommand(getQuery());
        String webpage = HttpConnectionUtil.getInstance().retrievePage(url);
        
        int idx = webpage.indexOf("/cgi-brs/BasesHTML/isbn/BRSCGI?CMD=VERDOC&CONF=AEISPA.cnf&BASE=ISBN&");
        while (idx > -1) {
            String id = webpage.substring(idx);
            id = id.substring(0, id.indexOf("\">"));
            id = StringUtils.getValueBetween("DOCN=", "&", id);
            
            if (!ids.contains(id)) 
                ids.add(id);
            
            webpage = webpage.substring(idx + 50);
            idx = webpage.indexOf("/cgi-brs/BasesHTML/isbn/BRSCGI?CMD=VERDOC&CONF=AEISPA.cnf&BASE=ISBN&");
        }
        
        return ids;
    }    

    @Override
    protected DcObject getItem(Object id, boolean full) throws Exception {
        String link = getAddress() + "/cgi-brs/BasesHTML/isbn/BRSCGI?CMD=VERDOC&BASE=ISBN&CONF=AEISPA.cnf&DOCN=" + id;
        return getItem(new URL(link), full);
    }
        
    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }

    protected DcObject getItem(URL url, boolean full) throws Exception {
        String html = HttpConnectionUtil.getInstance().retrievePage(url, "ISO-8859-1");
        
        String link = url.toString();
        String id = link.substring(link.toUpperCase().indexOf("&DOCN=") + 6);

        DcObject book = DcModules.get(DcModules._BOOK).getItem();
        setTitle(book, html);
        book.setValue(DcObject._SYS_SERVICEURL, link);
        book.setValue(Book._H_WEBPAGE, link);
        setYear(book, html);
        book.addExternalReference(DcRepository.ExternalReferences._MCU, id);
        
        if (full) {
            setDescription(book, html);
            setAuthor(book, html);
            setPublisher(book, html);
            setPages(book, html);
            setIsbn(book, html);
        }
        
        return book;
    }
    
    private void setTitle(DcObject dco, String html) {
        String title = getValue(html, "<th scope=\"row\">T&iacute;tulo:</th>", "strong");
        
        while (title.length() > 1 && (title.startsWith("\r") || title.startsWith("\n")))
            title = title.substring(1);

        while (title.length() > 1 && (title.endsWith("\r") || title.endsWith("\n")))
            title = title.substring(0, title.length() - 1);
        
        dco.setValue(Book._A_TITLE, title);
    }
    
    private void setDescription(DcObject dco, String html) {
        String description = StringUtils.getValueBetween("<table summary", "</table>", html);
        description = description.substring(description.indexOf(">") + 1);
        description = HtmlUtils.toPlainText("<table>" + description + "</table>");
        description = description.replaceAll(":\n", ": ");
        dco.setValue(Book._B_DESCRIPTION, description);
    }
    
    private void setPublisher(DcObject dco, String html) {
        int idx = html.indexOf("<th scope=\"row\">Publicaci&oacute;n:");
        if (idx > -1) {
            String publisher = html.substring(html.indexOf("<A", idx));
            publisher = StringUtils.getValueBetween(">", "</A>", publisher);
            DataLayer.getInstance().createReference(dco, Book._F_PUBLISHER, HtmlUtils.toPlainText(publisher));
        }
    }

    private void setAuthor(DcObject dco, String html) {
        int idx = html.indexOf("<th scope=\"row\">Autor:");        
        if (idx > -1) {
            String author = html.substring(html.indexOf("<A", idx));
            author = StringUtils.getValueBetween(">", "</A>", author);
            DataLayer.getInstance().createReference(dco, Book._G_AUTHOR, HtmlUtils.toPlainText(author));
        }
    }
    
    private void setYear(DcObject dco, String html) {
        String year = getValue(html, "<th scope=\"row\">Publicaci&oacute;n:", "span"); 
        if (year != null) {
            if (year.lastIndexOf("/") > -1) {
                year = year.substring(year.lastIndexOf("/") + 1);
                try {
                    dco.setValue(Movie._C_YEAR, Long.valueOf(year));
                } catch (NumberFormatException nfe) {}
            }
        }
    }
    
    private void setPages(DcObject dco, String html) {
        String description = getValue(html, "<tr><th scope=\"row\">Descripci&oacute;n:</th>", "span");
        
        if (description.indexOf(" p.") > -1) {
            String number = "";
            for (int i = description.indexOf(" p."); i > 0; i--) {
                char character = description.charAt(i - 1);
                if (Character.isDigit(character))
                    number = character + number;
                else
                    break;
            }
     
            if (number.length() > 0) {
                try {
                    dco.setValue(Book._T_NROFPAGES, Long.valueOf(number));
                } catch (NumberFormatException ignore) {}
            }
        }
    }
    
    private void setIsbn(DcObject dco, String html) {
        String isbn13 = getValue(html, "ISBN (13):", "strong");
        String isbn10 = getValue(html, "ISBN (10):", "strong");
        
        if (isbn10 != null) {
            isbn10 = isbn10.replaceAll("-", "");
            if (ISBN.isISBN10(isbn10))
                dco.setValue(Book._J_ISBN10, isbn10);
        }

        if (isbn13 != null) {
            isbn13 = isbn13.replaceAll("-", "");
            if (ISBN.isISBN13(isbn13))
                dco.setValue(Book._N_ISBN13, isbn13);
        }
    }
    
    private String getValue(String html, String tag, String between) {
        int idx = html.indexOf(tag);
        if (idx > -1) {
            String value = html.substring(idx);
            return HtmlUtils.toPlainText(StringUtils.getValueBetween("<" + between + ">", "</" + between + ">", value));
        }
        return null;
    }
}
