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

import java.util.Collection;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Movie;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class AmazonMovieSearch extends AmazonSearch {

    private static Logger logger = Logger.getLogger(AmazonMovieSearch.class.getName());
    
    public AmazonMovieSearch(IOnlineSearchClient listener, 
                             IServer server, 
                             Region region,
                             SearchMode mode,
                             String query) {
        
        super(listener, server, region, mode, query);
    }
    
    @Override
    protected DcObject getItem(Object o, boolean full) throws Exception {
        Document document = o instanceof Document ? (Document) o : getDocument((String) o);
        
        DcObject movie = DcModules.get(DcModules._MOVIE).getItem();
        
        setValue(document, "ASIN", movie, Movie._SYS_EXTERNAL_REFERENCES);
        String asin = movie.getExternalReference(DcRepository.ExternalReferences._ASIN);
        setServiceURL(movie, asin);
        
        setValue(document, "ItemAttributes/Title", movie, Movie._A_TITLE);
        setYear(document, "ItemAttributes/TheatricalReleaseDate", movie, Movie._C_YEAR);        
        setValue(document, "DetailPageURL", movie, Movie._G_WEBPAGE);
        setValue(document, "ItemAttributes/EAN", movie, Movie._12_EAN);
        setValue(document, "ItemAttributes/Color", movie, Movie._13_COLOR);
        setValue(document, "ItemAttributes/AspectRatio ", movie, Movie._14_ESPECT_RATIO);
        
        setRating(document, movie, Movie._E_RATING);
        setPlayLength(document, movie);
        setLanguage(document, movie, Movie._D_LANGUAGE);
        
        Collection<String> genres = getValues(document, "BrowseNodes/BrowseNode[Ancestors/BrowseNode//Name='Genres']/Name");
        for (String genre : genres)
            DataLayer.getInstance().createReference(movie, Movie._H_GENRES, genre);
        
        if (full) {
            setDescription(document, movie, Movie._B_DESCRIPTION);
            setImages(movie, document, Movie._X_PICTUREFRONT, null);
            setValue(document, "ItemAttributes/ProductGroup", movie, Movie._7_STORAGEMEDIUM);
            setActors(document, movie);
            setDirectors(document, movie);
        }
        
        return movie;
    }
    
    private void setPlayLength(Document document, DcObject movie) {
        String s = getValue(document, "ItemAttributes/RunningTime[@Units='minutes']");
        try {
            int minutes = Integer.parseInt(s);
            movie.setValue(Movie._L_PLAYLENGTH, minutes > 0 ? Long.valueOf(minutes * 60) : null);
        } catch (NumberFormatException nfe) {
            logger.debug("Unable to parse the playlength", nfe);
        }
    }    

    private void setDirectors(Document document, DcObject movie) {
        Collection<String> directors = getValues(document, "ItemAttributes/Director");
        for (String director : directors)
            DataLayer.getInstance().createReference(movie, Movie._J_DIRECTOR, director);    
    }    
    
    private void setActors(Document document, DcObject movie) {
        Collection<String> directors = getValues(document, "ItemAttributes/Actor");
        for (String director : directors)
            DataLayer.getInstance().createReference(movie, Movie._I_ACTORS, director);    
    }
}
