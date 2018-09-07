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

package net.datacrow.onlinesearch.imdb.task;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.DcRepository;
import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcAssociate;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Movie;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.datalayer.DataLayer;
import net.datacrow.util.DcImageIcon;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;
import net.datacrow.util.Utilities;
import net.datacrow.util.http.HttpConnectionUtil;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImdbMovieSearch extends ImdbSearch {
    
    private static Logger logger = Logger.getLogger(ImdbMovieSearch.class.getName());
    
    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    
    private final static int MAX = 20;
    
    public ImdbMovieSearch(IOnlineSearchClient listener, 
                           IServer server, 
                           Region region,
                           SearchMode mode,
                           String query) {

        super(listener, server, region, mode, query);
    }
    
    @Override
    public DcObject getItem(Object key, boolean full) throws Exception {
        URL url = new URL(getAddress() + "/title/tt" + key);
        return getItem(url, full);
    }      
    
    @Override
    public DcObject getItem(URL url) throws Exception {
        return getItem(url, true);
    }
    
    public DcObject getItem(URL url, boolean full) throws Exception {

        DcObject movie = DcModules.get(DcModules._MOVIE).getItem();
        
        try {
            Document document = HtmlUtils.getDocument(url, "ISO-8859-1");
            
            String serviceURL = url.toString();
            String id = serviceURL.substring(serviceURL.indexOf("/title/tt") + 9, serviceURL.length());

            movie.setValue(Movie._G_WEBPAGE, getAddress() + "/title/tt" + id);
            movie.setValue(DcObject._SYS_SERVICEURL, serviceURL);
            movie.addExternalReference(DcRepository.ExternalReferences._IMDB, "tt" + id);
            
            setTitleAndYear(document, movie);
            setDescription(id, movie);
            
            setGenres(document, movie);
            setRating(document, movie);
            setRuntime(document, movie);
            
            if (full) {
                setAspectRatio(document, movie);
                setColor(document, movie);
                setLanguages(document, movie);
                setCountries(document, movie);
                setCertification(document, movie);
                setDirector(document, movie);
                setActors(id, movie);

                setPoster(document, id, movie);
            }
        } catch (Exception e) {
            logger.error(e, e);
        }

        return movie;
    }
    
    @Override
    public Collection<Object> getItemKeys() throws Exception {
        URL url = new URL(getAddress() + "/find?s=tt&q=" + getQuery());
        Document document = HtmlUtils.getDocument(url, "UTF8");
        NodeList nlKeys = (NodeList) xpath.evaluate("//a[@href[starts-with(., '/title/tt')]]", document, XPathConstants.NODESET);
        Collection<Object> keys = new ArrayList<Object>();
        for (int i = 0; nlKeys != null && i < nlKeys.getLength(); i++) {
            Node node = nlKeys.item(i);
            Node href = node.getAttributes().getNamedItem("href");
            if (href != null) {
                String key = StringUtils.getValueBetween("/title/tt", "/", href.getTextContent());
                if (!keys.contains(key) && keys.size() < MAX) keys.add(key);
            }
        }
        return keys;
    }
    
    private void setTitleAndYear(Document document, DcObject movie) throws Exception {
        Node node = (Node) xpath.evaluate("//title", document, XPathConstants.NODE);
        
        if (node == null) return;
        
        String title = node.getTextContent();
        movie.setValue(Movie._A_TITLE, title);
        
        for (String year : StringUtils.getValuesBetween("(", ")", title)) {
            try {
                movie.setValue(Movie._C_YEAR, Long.parseLong(year));
                break;
            } catch (NumberFormatException nfe) {}
        }
    }
    
    private void setActors(String movieId, DcObject movie) throws Exception {
        Document document = HtmlUtils.getDocument(new URL(getAddress() + "/title/tt" + movieId + "/fullcredits#cast"), "UTF8");
        NodeList nlActors = (NodeList) xpath.evaluate("//table[@class='cast']//a[@href[starts-with(., '/name/nm')]]", document, XPathConstants.NODESET);
        for (int i = 0; nlActors != null && i < nlActors.getLength(); i++) {
            try {
                Node node = nlActors.item(i);
                Node href = node.getAttributes().getNamedItem("href");
                if (href != null) {
                    String id = StringUtils.getValueBetween("/name/nm", "/", href.getTextContent());
                    String name = node.getTextContent();
                    if (!Utilities.isEmpty(name)) {
                        DcObject person = getConcretePerson("nm" + id, name, DcModules._ACTOR);
                        DataLayer.getInstance().createReference(movie, Movie._I_ACTORS, person);
                    }
                }
            } catch (Exception e) {
                logger.error("Error while creating actor", e);
            }                
        }
    }
    
    private void setCertification(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_CERTIFICATION) + "']";
        Node div = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (div == null) return;
        
        String certification = div.getTextContent();
        if (!Utilities.isEmpty(certification)) {
            certification = clean(certification, getTag(_CERTIFICATION));
            certification = certification.replaceAll("\\|", ", ");
            certification = certification.replaceAll(" ,", ",");
            certification = certification.replaceAll(" ,", ",");
            certification = certification.replaceAll("\n", "");
            movie.setValue(Movie._3_CERTIFICATION, certification);       
        }
    }
    
    private String clean(String s, String tag) {
        String value = s;
        value = s.substring(value.indexOf(tag) + tag.length());
        value = value.replaceAll("\n|\t|\r", "");
        return value;
    }
    
    private void setCountries(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_COUNTRY) + "']";
        Node div = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (div == null) return;

        String countries = div.getTextContent();
        if (!Utilities.isEmpty(countries)) {
            countries = clean(countries, getTag(_COUNTRY));
            StringTokenizer st = new StringTokenizer(countries, "|");
            while (st.hasMoreElements()) {
                String country = ((String) st.nextElement()).trim();
                DataLayer.getInstance().createReference(movie, Movie._F_COUNTRY, country);
            }
        }
    }

    private void setRating(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_USERRATING) + "']";
        Node div = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (div == null) return;

        String rating = div.getTextContent();
        if (!Utilities.isEmpty(rating)) {
            rating = StringUtils.getValueBetween(getTag(_USERRATING), "/", rating);
            rating = rating.replaceAll("\n|\t|\r", "");
            rating = rating.replace(",", ".");
            try {
                int value = Math.round(Float.valueOf(rating));
                movie.setValue(Movie._E_RATING, value);
            } catch (NumberFormatException nfe) {
                logger.debug("Could not create rating from " + rating + " for " + movie);
            }
        }
    }
    
    private void setDirector(Document document, DcObject movie) throws Exception{
        Node div = (Node) xpath.evaluate("//div[@id='director-info']/a", document, XPathConstants.NODE);
        if (div != null) {
            String name = div.getTextContent();
            String path = div.getAttributes().getNamedItem("href").getTextContent();
            String id = StringUtils.getValueBetween("name/nm", "/", path);
            try {
                DcObject person = getConcretePerson("nm" + id, name, DcModules._DIRECTOR);
                DataLayer.getInstance().createReference(movie, Movie._J_DIRECTOR, person);
            } catch (Exception e) {
                logger.error("Error while creating director " + name, e);
            }
        }
    }

    private void setLanguages(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_LANGUAGE) + "']/a";
        Node node = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (node != null) {
            String languages = node.getParentNode().getTextContent();
            languages = languages.substring(1);
            if (languages.indexOf("\n") > 0) {
                languages = languages.substring(languages.indexOf("\n"));
                languages = languages.startsWith("\n") ? languages.substring(1) : languages;
                languages = StringUtils.trim(languages);
                
                StringTokenizer st = new StringTokenizer(languages, "|");
                while (st.hasMoreElements()) {
                    String language = ((String) st.nextElement()).trim();
                    DataLayer.getInstance().createReference(movie, Movie._D_LANGUAGE, language.trim());
                }
            }
        }
    }     
    
    private void setColor(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_COLOR) + "']/text()";
        Node node = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (node != null) {
            String color = node.getParentNode().getTextContent();
            color = color.substring(1);
            color = StringUtils.trim(color); 
            color = color.substring(getTag(_COLOR).length());
            color = StringUtils.trim(color);
            
            DataLayer.getInstance().createReference(movie, Movie._13_COLOR, color);
        }
    }    
    
    private void setAspectRatio(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_ASPECT_RATIO) + "']/a";
        Node node = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (node != null) {
            String ratio = node.getParentNode().getTextContent();
            ratio = ratio.substring(1);
            if (ratio.indexOf("\n") > 0) {
                ratio = ratio.substring(ratio.indexOf("\n"));
                ratio = ratio.indexOf(getTag(_MORE)) > -1 ? ratio.substring(0, ratio.indexOf(getTag(_MORE))) : ratio;
                
                while (ratio.startsWith("\n"))
                    ratio= ratio.substring(1);
                
                while (ratio.endsWith("\n"))
                    ratio= ratio.substring(0, ratio.length() -1);

                ratio = ratio.startsWith("\n") ? ratio.substring(1) : ratio;
                DataLayer.getInstance().createReference(movie, Movie._14_ESPECT_RATIO, ratio.trim());
            }
        }
    }
    
    private void setGenres(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_GENRE) + "']/a";
        NodeList genres = (NodeList) xpath.evaluate(syntax, document, XPathConstants.NODESET);
        if (genres != null && genres.getLength() > 0) {
            // skip the last one (the 'more' link)
            for (int i = 0; i < genres.getLength() - 1; i++) {
                Node genre = genres.item(i);
                String s = StringUtils.trim(genre.getTextContent());
                s = s.toLowerCase().endsWith(" more") ? s.substring(0, s.lastIndexOf(" ")) : s;
                DataLayer.getInstance().createReference(movie, Movie._H_GENRES, s);
            }
        } else {
            syntax = "//div[h5='" + getTag(_GENRE) + "']";
            Node div = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
            
            if (div == null) return;
            
            String text = div.getTextContent();
            
            if (!Utilities.isEmpty(text)) {
                text = clean(text, getTag(_GENRE));
                StringTokenizer st = new StringTokenizer(text, "|");
                while (st.hasMoreElements()) {
                    String s = StringUtils.trim((String) st.nextElement());
                    s = s.toLowerCase().endsWith(" more") ? s.substring(0, s.lastIndexOf(" ")) : s;
                    DataLayer.getInstance().createReference(movie, Movie._H_GENRES, s);
                }
            }
        }
    }
    
    /**
     * Queries Imdb for the specific person (using the imdb id). Then it checks if the
     * person exists already in the database. If not, it is created. The new or existing
     * person is then returned.
     * @throws Exception
     */
    private DcObject getConcretePerson(String imdbId, String name, int module) throws Exception {
        
        DcObject person = DataLayer.getInstance().getObjectByExternalID(module, DcRepository.ExternalReferences._IMDB, imdbId);
        person = person == null ? DataLayer.getInstance().getObjectForString(module, name) : person;
        
        if (person == null) {
            if (DcModules.get(DcModules._MOVIE).getSettings().getBoolean(DcRepository.ModuleSettings.stOnlineSearchSubItems)) {
                try {
                    ImdbPerson imdbPerson = new ImdbPerson(module, true);
                    person = imdbPerson.get(imdbId);
                    sleep(500);
                } catch (Exception e) {
                    person = DcModules.get(module).getItem();
                    person.setValue(DcAssociate._A_NAME, name);
                }
            } else {
                person = DcModules.get(module).getItem();
                person.setValue(DcAssociate._A_NAME, name);
            }
            
            person.addExternalReference(DcRepository.ExternalReferences._IMDB, imdbId);
            person.setIDs();
        }
        return person;
    }        

    private void setRuntime(Document document, DcObject movie) throws Exception {
        String syntax = "//div[h5='" + getTag(_RUNTIME) + "']";
        Node div = (Node) xpath.evaluate(syntax, document, XPathConstants.NODE);
        
        if (div == null) return;
        
        String text = div.getTextContent();
        
        if (!Utilities.isEmpty(text)) {
            text = text.indexOf("|") > -1 ? text.substring(0, text.indexOf("|")) : text;
            text = text.indexOf("(") > -1 ? text.substring(0, text.indexOf("(")) : text;
            String duration = StringUtils.getContainedNumber(text);
            if (!Utilities.isEmpty(duration))
                movie.setValue(Movie._L_PLAYLENGTH, Long.valueOf(duration) * 60);
        }
    }
    
    private void setDescription(String id, DcObject movie) {
        try {
            Document document = HtmlUtils.getDocument(new URL(getAddress() + "/Plot?" + id), "ISO-8859-1");
            Node p = (Node) xpath.evaluate("//p[@class='plotpar']/text()", document, XPathConstants.NODE);
            if (p != null) {
                String description = p.getTextContent();
                while (description.startsWith("\n") || description.startsWith("\r"))
                    description = description.substring(1);
                
                movie.setValue(Movie._B_DESCRIPTION, description);
            }
        } catch (Exception e) {
            logger.error(e, e);
        }
    }
    
    private void setPoster(Document document, String id, DcObject movie) throws Exception {
        Node nUrl = (Node) xpath.evaluate("//a[@name='poster']", document, XPathConstants.NODE);
        byte[] image = null;
        if (nUrl != null) {
            String link = nUrl.getAttributes().getNamedItem("href").getTextContent();
            Document documentMedia = HtmlUtils.getDocument(new URL(getAddress() + link), "UTF8");
            Node nodeImg = (Node) xpath.evaluate("//table[@id='principal']/tr/td/a/img", documentMedia, XPathConstants.NODE);
            if (nodeImg != null) {
                image = HttpConnectionUtil.getInstance().retrieveBytes(nodeImg.getAttributes().getNamedItem("src").getTextContent());
            } else {
                NodeList images = nUrl.getChildNodes();
                if (images != null && images.getLength() > 0)
                    image = HttpConnectionUtil.getInstance().retrieveBytes(images.item(0).getAttributes().getNamedItem("src").getTextContent());
            }
        }
        DcImageIcon picture = image == null || image.length < 100 ? null : new DcImageIcon(image);
        movie.setValue(Movie._X_PICTUREFRONT, picture);
    }    
}
