package net.datacrow.onlinesearch.mcu.mode;


public class IsbnSearchMode extends net.datacrow.core.services.IsbnSearchMode {

    public IsbnSearchMode(int fieldBinding) {
        super(fieldBinding);
    }
    
    @Override
    public String getSearchCommand(String s) {
        s = super.getSearchCommand(s);
        s = getIsbn(s);
        return "/cgi-brs/BasesHTML/isbn/BRSCGI?CMD=VERLST&BASE=ISBN&DOCS=1-15&CONF=AEISPA.cnf&OPDEF=AND&SEPARADOR=&WDIS-C=DISPONIBLE&WGEN-C=&WISB-C=" + s;
    }
}
