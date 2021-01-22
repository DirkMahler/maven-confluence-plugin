/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsc.mojo;

import lombok.val;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bsc.confluence.ConfluenceService;
import org.bsc.confluence.ConfluenceService.Model.PageSummary;
import org.bsc.confluence.ConfluenceServiceBuilder;
import org.bsc.confluence.model.Site;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 *
 * Delete a confluence pageTitle 
 * 
 * @author bsorrentino
 * @since 3.4.0
 */
@Mojo( name="delete", threadSafe = true, requiresProject = false  )
public class ConfluenceDeleteMojo extends AbstractBaseConfluenceSiteMojo {

    /**
     * perform recursive deletion 
     * 
     * @since 3.4.0
     */
    @Parameter(property = "recursive", defaultValue = "true")
    private boolean recursive;

    /**
     *
     * @param confluence
     * @return
     */
    private boolean deletePage(ConfluenceService confluence)  {
        final Site.Home home;

        if( isSiteDescriptorValid() ) {
            final Site site = createSiteFromModel(getSiteModelVariables());
            home = site.getHome();
        }
        else {
            home = new Site.Home();
            home.setName( getPageTitle() );
        }

        if( home.getName() == null ) {
            getLog().warn( "page title has not been provided!" );
            return false;
        }

        return deletePage( confluence, home ).join();
    }

    /**
     *
     * @param parentPage
     * @param startPageTitle
     * @param removed
     * @return
     */
    private boolean logRemoved(ConfluenceService.Model.Page parentPage, String startPageTitle, boolean removed ) {
        if( removed ) {
            getLog().info(format("Page [%s]/[%s] in [%s] has been removed!",
                    parentPage.getTitle(),startPageTitle, parentPage.getSpace()));
        }
        else {
            getLog().warn(format("Page [%s]/[%s] in [%s] has not been removed!",
                    parentPage.getTitle(),startPageTitle, parentPage.getSpace()));
        }
        return removed;
    }

    /**
     *
     * @param parentPage
     * @param startPageTitle
     * @param descendent
     * @param removed
     * @return
     */
    private boolean logRemoved(ConfluenceService.Model.Page parentPage, String startPageTitle, ConfluenceService.Model.PageSummary descendent, boolean removed ) {
        if( removed ) {
            getLog().info(format("Page [%s]/[%s]/[%s] in [%s] has been removed!",
                    parentPage.getTitle(),startPageTitle, descendent.getTitle(), parentPage.getSpace()));
        }
        else {
            getLog().warn(format("Page [%s]/[%s]/[%s] in [%s] has not been removed!",
                    parentPage.getTitle(),startPageTitle, descendent.getTitle(), parentPage.getSpace()));
        }
        return removed;
    }

    /**
     *
     * @param confluence
     * @return
     */
    private CompletableFuture<Boolean> deletePage(ConfluenceService confluence, Site.Home home )  {

        final String startPageTitle = home.getName();

        getLog().debug( format( "start deleting from page [%s]", startPageTitle ));

        return loadParentPage(confluence, empty())
            .thenCompose( parentPage -> {
                return confluence.getPageByTitle(parentPage.getId(), startPageTitle)
                    .thenCompose( ( start ) -> {

                        if (!start.isPresent()) {
                            getLog().warn(format("Page [%s]/[%s] in [%s] not found!", parentPage.getTitle(), startPageTitle, parentPage.getSpace()));
                            return completedFuture(false);
                        }

                        if (recursive) {

                            confluence.getDescendents(start.get().getId()).thenAccept(descendents -> {
                                if (descendents == null || descendents.isEmpty()) {
                                    getLog().warn(format("Page [%s]/[%s] in [%s] has not descendents!", parentPage.getTitle(), startPageTitle, parentPage.getSpace()));
                                }
                                else {
                                    for (PageSummary descendent : descendents) {

                                        confluence.removePage(descendent.getId())
                                                .thenApply( removed -> logRemoved( parentPage, startPageTitle, descendent, removed ) )
                                                .exceptionally(ex -> {
                                                    getLog().warn(format("cannot remove descendent %s", descendent.getTitle()), ex);
                                                    return false;
                                                })
                                                .join();
                                    }
                                }

                            }).join();

                        }

                        return (home.isAnchor()) ?
                                CompletableFuture.completedFuture(true) :
                                confluence.removePage(start.get().getId())
                                    .thenApply( removed -> logRemoved( parentPage, startPageTitle, removed ) );
                    });
        }).exceptionally( ex  -> {
            getLog().warn( ex.getMessage() );
            return false;
        });

    }

    /**
     *
     * @param confluenceBuilder
     * @throws Exception
     */
    @Override
    protected void execute( ConfluenceServiceBuilder confluenceBuilder ) throws Exception {

        deletePage(confluenceBuilder.build());
        
    }
 
    
}
