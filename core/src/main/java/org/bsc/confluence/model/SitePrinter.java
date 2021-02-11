package org.bsc.confluence.model;

import org.bsc.confluence.model.Site.Attachment;
import org.bsc.confluence.model.Site.Page;
import org.bsc.confluence.model.Site.Source;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SitePrinter {
    
    /**
     * 
     * @param site
     * @param source
     * @return
     */
    public static String getPrintableStringForResource( final Site site, final Source source ) {
        return getPrintableStringForResource( site, source.getUri());
    }
    
    /**
     * 
     * @param site
     * @param uri
     * @return
     */
    public static String getPrintableStringForResource( final Site site, final java.net.URI uri ) {
        if (uri == null)
            throw new java.lang.IllegalArgumentException("uri is null!");

        try {
            Path p = Paths.get( uri );
            return site.getBasedir().relativize(p).toString();
            
        } catch (Exception e) {
            return uri.toString();
        }
        
    }

    /**
     * 
     * @param site
     * @param out
     * @param level
     * @param c
     * @param source
     */
    public static void printSource(final Site site, PrintStream out, int level, char c, final Source source) {
        for (int i = 0; i < level; ++i) {
            System.out.print(c);
        }
        out.print(" ");
        out.println(getPrintableStringForResource(site, source));
        
        //out.println();
        //out.print( state.getBasedir() ); out.print( " - " ); out.print( source );
        //out.println();
    }

    /**
     * 
     * @param site
     * @param out
     * @param level
     * @param parent
     */
    public static void printChildren(final Site site, PrintStream out, int level, Page parent) {

        printSource(site, out, level, '-', parent);

        for (Attachment attach : parent.getAttachments()) {

            printSource(site, out, level + 1, '#', attach);

        }
        for (Page child : parent.getChildren()) {

            printChildren(site, out, level + 1, child);

        }
    }

    /**
     * 
     * @param site
     * @param out
     */
    public static void print(final Site site,PrintStream out) {

        out.println("Site");

        if (!site.getLabels().isEmpty()) {
            out.println(" Labels");
            for (String label : site.getLabels()) {

                out.printf("  %s\n", label);

            }
        }

        printChildren(site, out, 0, site.getHome());

    }

}
