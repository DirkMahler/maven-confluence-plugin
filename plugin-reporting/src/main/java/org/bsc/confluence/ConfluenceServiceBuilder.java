/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsc.confluence;

import org.bsc.confluence.ConfluenceService.Credentials;
import org.bsc.confluence.model.Site;
import org.bsc.confluence.rest.RESTConfluenceService;
import org.bsc.confluence.rest.model.Page;
import org.bsc.confluence.rest.scrollversions.ScrollVersionsConfluenceService;
import org.bsc.confluence.xmlrpc.XMLRPCConfluenceService;
import org.bsc.mojo.configuration.ScrollVersionsInfo;
import org.bsc.ssl.SSLCertificateInfo;

import javax.json.JsonObjectBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.bsc.confluence.xmlrpc.XMLRPCConfluenceService.createInstanceDetectingVersion;

/**
 *
 * @author bsorrentino
 */
public class ConfluenceServiceBuilder {

    /**
     *
     * @param log
     * @return
     */
    public static ConfluenceServiceBuilder of(org.apache.maven.plugin.logging.Log log) {
        return new ConfluenceServiceBuilder(log);
    }

    private static class MixedConfluenceService implements ConfluenceService {
        final XMLRPCConfluenceService xmlrpcService;
        final RESTConfluenceService restService;

        public MixedConfluenceService(String endpoint, Credentials credentials, ConfluenceProxy proxyInfo, SSLCertificateInfo sslInfo) throws Exception {
            
            this.xmlrpcService = createInstanceDetectingVersion(endpoint, credentials, proxyInfo, sslInfo);
            
            final String restEndpoint = new StringBuilder()
            		.append(ConfluenceService.Protocol.XMLRPC.removeFrom(endpoint))
            		.append(ConfluenceService.Protocol.REST.path())
            		.toString();
            
            this.restService = new RESTConfluenceService(restEndpoint, credentials, sslInfo);
        }
        
        @Override
        public Credentials getCredentials() {
            return xmlrpcService.getCredentials();
        }

        @Override
        public CompletableFuture<Optional<? extends Model.PageSummary>> getPageByTitle(Model.ID parentPageId, String title)  {
            return xmlrpcService.getPageByTitle(parentPageId, title);
        }

        @Override
        public CompletableFuture<Optional<Model.Page>> getPage(String spaceKey, String pageTitle) {
            return xmlrpcService.getPage(spaceKey, pageTitle);
        }

        @Override
        public CompletableFuture<Optional<Model.Page>> getPage(Model.ID pageId) {
            return xmlrpcService.getPage(pageId);
        }

        @Override
        public CompletableFuture<List<Model.PageSummary>> getDescendents(Model.ID pageId)  {
            return xmlrpcService.getDescendents(pageId);
        }

        @Override
        public CompletableFuture<Boolean> removePage(Model.ID pageId) {
            return xmlrpcService.removePage(pageId);
        }

        @Override
        public CompletableFuture<Boolean> removePage(Model.Page parentPage, String title) {
            return xmlrpcService.removePage(parentPage, title);
        }

        @Override
        public CompletableFuture<Model.Page> createPage(Model.Page parentPage, String title, Storage content)  {
            return xmlrpcService.createPage(parentPage, title, content);
        }
        @Override
        public CompletableFuture<Model.Page> storePage(Model.Page page)  {
            return xmlrpcService.storePage(page);
        }

        @Override
        public CompletableFuture<Model.Page> storePage(Model.Page page, Storage content)  {
            
            if( Storage.Representation.STORAGE == content.rapresentation ) {
                
                if( page.getId()==null ) { 
                    final JsonObjectBuilder inputData = 
                            restService.jsonForCreatingContent( RESTConfluenceService.ContentType.page,
                                                                page.getSpace(),
                                                                page.getParentId().getValue(),
                                                                page.getTitle(),
                                                                content);
                    return restService.createPage(inputData.build())
                            .thenApply( p -> p.map(Page::new).get() );
                    
                }

                return restService.storePage(page, content);
            }
            return xmlrpcService.storePage(page, content);
        }

        @Override
        public CompletableFuture<Void> addLabelsByName(Model.ID id, String[] labels) {
            return xmlrpcService.addLabelsByName(id, labels);
        }

        @Override
        public Model.Attachment createAttachment() {
            return xmlrpcService.createAttachment();
        }

        @Override
        public CompletableFuture<Optional<Model.Attachment>> getAttachment(Model.ID pageId, String name, String version) {
            return xmlrpcService.getAttachment(pageId, name, version);
        }

        @Override
        public CompletableFuture<Model.Attachment> addAttachment(Model.Page page, Model.Attachment attachment, InputStream source)  {
            return xmlrpcService.addAttachment(page, attachment, source);
        }

        @Override
        public void exportPage(String url, String spaceKey, String pageTitle, ExportFormat exfmt, File outputFile) throws Exception {
            xmlrpcService.exportPage(url, spaceKey, pageTitle, exfmt, outputFile);
        }

        /**
         * factory method
         *
         * @param space   space id
         * @param title   post's title
         * @param content post's content
         * @return
         */
        @Override
        public Model.Blogpost createBlogpost(String space, String title, Storage content, int version) {
            return xmlrpcService.createBlogpost(space, title, content, version);
        }

        @Override
        public CompletableFuture<Model.Blogpost> addBlogpost(Model.Blogpost blogpost )  {
            return xmlrpcService.addBlogpost(blogpost);
        }

        @Override
        public String toString() {
            return xmlrpcService.toString();
        }

        /**
         * 
         */
        @Override
        public void close() throws IOException {
            xmlrpcService.logout();
            
        }
        
    }

    final org.apache.maven.plugin.logging.Log log;
    private String endpoint;
    private Credentials credentials;
    private SSLCertificateInfo sslInfo;
    private ScrollVersionsInfo svi;
    private ConfluenceProxy proxyInfo;
    private Optional<Site> optSite = empty();

    public ConfluenceServiceBuilder endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ConfluenceServiceBuilder credentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public ConfluenceServiceBuilder sslInfo(SSLCertificateInfo sslInfo) {
        this.sslInfo = sslInfo;
        return this;
    }

    /**
     *
     * @param svi scroll versions addon parameters
     * @return
     */
    public ConfluenceServiceBuilder scrollVersionInfo(ScrollVersionsInfo svi) {
        this.svi = svi;
        return this;
    }

    public ConfluenceServiceBuilder proxyInfo(ConfluenceProxy proxyInfo) {
        this.proxyInfo = proxyInfo;
        return this;
    }

    public ConfluenceServiceBuilder site(Site site) {
        this.optSite = ofNullable(site);
        return this;
    }

    private ConfluenceService newService() throws Exception {
        if( ConfluenceService.Protocol.XMLRPC.match(endpoint)) {
            return new MixedConfluenceService(endpoint, credentials, proxyInfo, sslInfo);
        }
        if( ConfluenceService.Protocol.REST.match(endpoint)) {
            return svi.optVersion()
                    .map( version -> (ConfluenceService)new ScrollVersionsConfluenceService(endpoint, version, credentials, sslInfo) )
                    .orElseGet( () -> new RESTConfluenceService(endpoint, credentials /*, proxyInfo*/, sslInfo))
                    ;
        }

        throw new IllegalArgumentException(
                format("endpoint doesn't contain a valid API protocol\nIt must be '%s' or '%s'",
                        ConfluenceService.Protocol.XMLRPC.path(),
                        ConfluenceService.Protocol.REST.path())
        );
    }

    private ConfluenceService proxyService( ConfluenceService service ) {
            return (ConfluenceService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { ConfluenceService.class },
                (proxy, method, methodArgs) -> {
                    if( log.isDebugEnabled() ) {

                        final String args =
                                ofNullable(methodArgs)
                                    .map( a -> stream(a).map(String::valueOf).collect(joining(",")))
                                    .orElse("");

                        log.debug( format("ConfluenceService.%s( %s )\n", method.getName(), args));

                    }
//                    if( "storePage".equals(method.getName())) {
//
//                        if( methodArgs!=null && methodArgs.length > 0 && methodArgs[0] instanceof ConfluenceService.Model.Page ) {
//                            final ConfluenceService.Model.Page page =
//                                    (ConfluenceService.Model.Page) methodArgs[0];
//
//                            final String title = page.getTitle();
//                            final boolean isAnchor = site.getHome()
//                                    .findPage( p -> p.getName().equals(title))
//                                    .map( p -> p.isAnchor() )
//                                    .orElse( false );
//                            if( isAnchor ) {
//                                log.info( format("page '%s' is an anchor! update skipped!", title));
//                                return CompletableFuture.completedFuture(page);
//                            }
//
//                        }
//                    }
                    return method.invoke( service, methodArgs);
                });

    }

    public ConfluenceService build() throws Exception {
        return newService();

    }

    private ConfluenceServiceBuilder( org.apache.maven.plugin.logging.Log log ) {
        if( log == null ) throw new IllegalArgumentException("log argument is null!");
        this.log = log;
    }



}
