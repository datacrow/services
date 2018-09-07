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

package net.datacrow.onlinesearch.ofdb.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Movie;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.Utilities;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;

public class OfdbSearch extends SearchTask {

	private static Logger logger = Logger.getLogger(OfdbSearch.class.getName());
    
    public OfdbSearch(IOnlineSearchClient listener, IServer server, SearchMode mode, String query) {
        super(listener, server, null, mode, query);
    }
    
    @Override
    protected DcObject getItem(Object sublink, boolean full) throws Exception {
        return getItem(new URL("http://www.ofdb.de/" + sublink), full);
    }

    @Override
    protected DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }
        
    protected DcObject getItem(URL url, boolean full) throws Exception {    
        DcObject movie = DcModules.get(DcModules._MOVIE).getItem();
        
        String html = HttpConnectionUtil.getInstance().retrievePage(url);
        
        String link = url.toString();
        String id = link.substring(link.lastIndexOf("/") + 1);
        
        setTitle(movie, html);
        setLocalTitle(movie, html);
        setYear(movie, html);
        movie.addExternalReference(DcRepository.ExternalReferences._OFDB, id);

        movie.setValue(Movie._G_WEBPAGE, link);
        movie.setValue(DcObject._SYS_SERVICEURL, link);
        
        if (full) {
            setDescription(movie, html);
            setCover(movie, html);
            setPersons(movie, html);
            setCountry(movie, html);
            setGenres(movie, html);
            setImdbID(movie, html);
        }
        
        return movie;
    }
    
    private void setTitle(DcObject movie, String html) {
    	int idx = html.indexOf("Originaltitel:");
    	if (idx > -1) {
    	    String title = html.substring(html.indexOf("class=\"Daten\"", idx));
    		title = StringUtils.getValueBetween("<b>", "</b>", title);
    		movie.setValue(Movie._A_TITLE, HtmlUtils.toPlainText(title));
    	}
    }
    
    private void setImdbID(DcObject movie, String html) {
        int idx = html.toLowerCase().indexOf("http://www.imdb");
        if (idx > -1) {
            String imdbID = html.substring(idx);
            imdbID = StringUtils.getValueBetween("?", "\"", imdbID);
            
            if (!Utilities.isEmpty(imdbID)) {
                imdbID = imdbID.startsWith("tt") ? imdbID : "tt" + imdbID;
                movie.addExternalReference(DcRepository.ExternalReferences._IMDB, imdbID);
            }
        }
    }

    private void setLocalTitle(DcObject movie, String html) {
    	String local = StringUtils.getValueBetween("<title>", "</title>", html);
    	if (local.startsWith("OFD")) {
    		local = local.substring(7);
    		movie.setValue(Movie._F_TITLE_LOCAL, HtmlUtils.toPlainText(local));
    	}
    }    
    
    private void setCountry(DcObject movie, String html) {
        int idx = html.lastIndexOf("<a href=\"view.php?page=blaettern&Kat=Land&Text=");
        if (idx > -1) {
            String country = html.substring(idx + 1);
            country = StringUtils.getValueBetween(">", "</", country);
            DataLayer.getInstance().createReference(movie, Movie._F_COUNTRY, country);
        }
    }
    
    private void setGenres(DcObject movie, String html) {
        int idx = 0;
        String check = "<a href=\"view.php?page=genre&Genre=";
        while ((idx = html.indexOf(check, idx + check.length())) > -1) {
            String genre = html.substring(idx + check.length()); 
            genre = StringUtils.getValueBetween(">", "</", genre);
            if (genre.length() > 0)
                DataLayer.getInstance().createReference(movie, Movie._H_GENRES, genre);
        }
    }    
    
    private void setYear(DcObject movie, String html) {
    	String title = StringUtils.getValueBetween("<title>", "</title>", html);
    	int idx = title.lastIndexOf("(");
    	
    	if (idx > -1) {
    		String tmp = title.substring(idx);
    		String year = StringUtils.getValueBetween("(", ")", tmp);
    		try {
    			movie.setValue(Movie._C_YEAR, Long.valueOf(year));
    		} catch (NumberFormatException nfe) {}
    	}
    }

    private void setDescription(DcObject movie, String html) { 
        int idx = html.indexOf("<a href=\"plot/");
        if (idx > -1) {
            String link = "http://www.ofdb.de/" + html.substring(idx + 9, html.indexOf("\"", idx + 9));
            
            try {
                html = HttpConnectionUtil.getInstance().retrievePage(link);
                idx = html.indexOf("<p class=\"Blocksatz\">");
                if (idx > -1) {
                    String description = html.substring(idx, html.indexOf("</p>", idx));
                    description = HtmlUtils.toPlainText(description);

                    description = description.indexOf(" Mal gelesen") > -1 ? description.substring(description.indexOf(" Mal gelesen") + 12) : description;
                    
                    while (description.startsWith("\r") || description.startsWith("\n") || description.startsWith(" "))
                        description = description.substring(1);
                    
                    movie.setValue(Movie._B_DESCRIPTION, description);
                }
            } catch (Exception e) {
                logger.error("Could not retrieve description from " + link);
            }        
        }
    }
    
    private void setPersons(DcObject movie, String html) {
        int idx = html.indexOf("view.php?page=film_detail");
        if (idx > -1) {
            String link = html.substring(idx, html.indexOf("\"", idx)); 
            try {
                link = link.startsWith("http://") ? link : "http://www.ofdb.de/" + link;
                String page = HttpConnectionUtil.getInstance().retrievePage(link);
                for (String name : getNames(page, "<i>Regie</i>"))
                    DataLayer.getInstance().createReference(movie, Movie._J_DIRECTOR, name);
                
                for (String name : getNames(page, "<i>Darsteller</i>"))
                    DataLayer.getInstance().createReference(movie, Movie._I_ACTORS, name);
                
            } catch (Exception e) {
                logger.error("Could not retrieve description from " + link);
            }
        }
    }
    
    private Collection<String> getNames(String html, String tag) {
        Collection<String> names = new ArrayList<String>();

        int idx = html.indexOf(tag);
        if (idx > -1) {
            String table = html.substring(idx);
            table = StringUtils.getValueBetween("<table", "</table>", table);
            
            String[] tags = new String[] {"<a href=\"view.php?page=person&id=", 
                                          "<a href=\"view.php?page=liste&Name="};

            for (String check : tags) {
                idx = 0;
                while ((idx = table.indexOf(check, idx + check.length())) > -1) {
                    String name = table.substring(table.indexOf(">", idx + check.length()) + 1);
                    name = HtmlUtils.toPlainText(name.substring(0, name.indexOf("</")));
                    if (!names.contains(name) && name.length() > 0)
                        names.add(name);
                }
            }
        }
        return names;
    }
    
    private void setCover(DcObject movie, String html) {
        int idx = html.indexOf("<img src=\"http://img.ofdb.de/film/");
        if (idx > -1) {
            String link = html.substring(idx + 10, html.indexOf("\"", idx + 10));
            try {
                byte[] image = HttpConnectionUtil.getInstance().retrieveBytes(new URL(link));
                if (image != null && image.length > 0 && image.length != 3554)
                    movie.setValue(Movie._X_PICTUREFRONT, new DcImageIcon(image));
            } catch (Exception e) {
                logger.error("Could not retrieve picture from " + link);
            }        
        }
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Collection<Object> ids = new ArrayList<Object>();
        String html = HttpConnectionUtil.getInstance().retrievePage(getMode().getSearchCommand(getQuery()));
        
        int idx = 0;
        while ((idx = html.indexOf("<a href=\"film/")) > -1) {
        	String link = html.substring(idx + 9, html.indexOf("\"", idx + 9));
        	ids.add(link);
        	html = html.substring(idx + 9);
        }
        
        return ids;
    }
}
