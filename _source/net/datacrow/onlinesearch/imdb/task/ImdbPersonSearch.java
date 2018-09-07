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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.datacrow.core.objects.DcObject;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.util.HtmlUtils;
import net.datacrow.util.StringUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImdbPersonSearch extends ImdbSearch {

    private final static XPath xpath = XPathFactory.newInstance().newXPath();
    private int module;

    public ImdbPersonSearch(IOnlineSearchClient listener, int module, IServer server, Region region, String query) {
        super(listener, server, region, null, query);
        this.module = module;
    }
    
    @Override
    protected DcObject getItem(URL url) throws Exception {
        ImdbPerson personFinder = new ImdbPerson(module, true);
        return personFinder.get(url);
    }
    
    @Override
    protected DcObject getItem(Object id, boolean full) throws Exception {
        ImdbPerson personFinder = new ImdbPerson(module, full);
        DcObject person = personFinder.get(id);
        return person;
    }
    
    @Override
    protected Collection<Object> getItemKeys() throws Exception {
        Document document = HtmlUtils.getDocument(new URL(getAddress() + "/find?s=nm&q=" + getQuery()), "UTF8");
        NodeList nlKeys = (NodeList) xpath.evaluate("//a[@href[starts-with(., '/name/nm')]]", document, XPathConstants.NODESET);
        Collection<Object> keys = new ArrayList<Object>();
        for (int i = 0; i < nlKeys.getLength(); i++) {
            Node node = nlKeys.item(0);
            Node href = node.getAttributes().getNamedItem("href");
            if (href != null) {
                String key = StringUtils.getValueBetween("/name/nm", "/", href.getTextContent());
                if (!keys.contains(key)) keys.add(key);
            }
        }
        return keys;
    }
}
