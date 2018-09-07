package net.datacrow.onlinesearch.mcu.server;

import java.util.ArrayList;
import java.util.Collection;

import net.datacrow.core.modules.DcModules;
import net.datacrow.core.objects.DcObject;
import net.datacrow.core.objects.helpers.Book;
import net.datacrow.core.services.IOnlineSearchClient;
import net.datacrow.core.services.Region;
import net.datacrow.core.services.SearchMode;
import net.datacrow.core.services.SearchTask;
import net.datacrow.core.services.plugin.IServer;
import net.datacrow.core.settings.Setting;
import net.datacrow.onlinesearch.mcu.mode.IsbnSearchMode;
import net.datacrow.onlinesearch.mcu.mode.KeywordSearchMode;
import net.datacrow.onlinesearch.mcu.task.McuBookSearch;

public class McuBookServer implements IServer {
    
    private Collection<Region> regions = new ArrayList<Region>();
    private Collection<SearchMode> modes = new ArrayList<SearchMode>();
    
    public McuBookServer() {
        modes.add(new KeywordSearchMode(Book._A_TITLE));
        modes.add(new IsbnSearchMode(Book._N_ISBN13));
        
        regions.add(new Region("es", "Spain", "http://www.mcu.es"));
    }

    public int getModule() {
        return DcModules._BOOK;
    }

    public String getName() {
        return "Base de datos de libros editados en España";
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public Collection<SearchMode> getSearchModes() {
        return modes;
    }

    public String getUrl() {
        return "http://www.mcu.es";
    }
    
    public Collection<Setting> getSettings() {
        return null;
    }
    
    public SearchTask getSearchTask( IOnlineSearchClient listener,
                                     SearchMode mode, 
                                     Region region, 
                                     String query,
                                     DcObject client) {
        
        McuBookSearch task = new McuBookSearch(listener, this, region, mode, query);    
        task.setClient(client);
        return task;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}