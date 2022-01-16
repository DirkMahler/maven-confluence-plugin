package org.bsc.markdown.pegdown;

import org.bsc.confluence.model.Site;
import org.bsc.markdown.MarkdownParserContext;
import org.junit.jupiter.api.Test;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.*;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
    public static final int NONE = 0;
    public static final int SMARTS = 1;
    public static final int QUOTES = 2;
    public static final int SMARTYPANTS = 3;
    public static final int ABBREVIATIONS = 4;
    public static final int HARDWRAPS = 8;
    public static final int AUTOLINKS = 16;
    public static final int TABLES = 32;
    public static final int DEFINITIONS = 64;
    public static final int FENCED_CODE_BLOCKS = 128;
    public static final int WIKILINKS = 256;
    public static final int STRIKETHROUGH = 512;
    public static final int ANCHORLINKS = 768;
    public static final int ALL = 65535;
    public static final int SUPPRESS_HTML_BLOCKS = 65536;
    public static final int SUPPRESS_INLINE_HTML = 131072;
    public static final int SUPPRESS_ALL_HTML = 196608;
*/
/**
 *
 * @author bsorrentino
 */
public abstract class PegdownParse {

    final Site site = new Site();

    PegdownParse() {
        site.setBasedir( Paths.get( System.getProperty("user.dir") ));
    }

    protected Site.Page createPage( String name, String uri  ) throws URISyntaxException {
        final Site.Page result = new Site.Page();
        result.setUri( new java.net.URI(uri));
        result.setName(name);
        return result;
    }
    protected char[] loadResource( String name ) throws IOException {

        final ClassLoader cl = PegdownParse.class.getClassLoader();

        try(final java.io.InputStream is = cl.getResourceAsStream(name) ) {

            java.io.CharArrayWriter caw = new java.io.CharArrayWriter();

            for( int c = is.read(); c!=-1; c = is.read() ) {
                caw.write( c );
            }

            return caw.toCharArray();

        }

    }

    protected abstract char[] loadResource() throws IOException;
    
    static class IfContext {

        static final IfContext IsTrue = new IfContext(true);
        static final IfContext IsFalse = new IfContext(false);

        final boolean condition ;

        public IfContext(boolean condition) {
            this.condition = condition;
        }


        <T extends Node> IfContext elseIf( Object n, Class<T> clazz, Consumer<T> cb ) {
            return ( condition ) ? IsTrue : iF( n, clazz, cb );
        }

        static <T extends Node> IfContext iF( Object n, Class<T> clazz, Consumer<T> cb ) {

            if( clazz.isInstance(n)) {

                cb.accept( clazz.cast(n));
                return IsTrue;
            }
            return IsFalse;
        }

    }

    final Consumer<StrongEmphSuperNode> sesn = ( node ) -> 
           System.out.printf( " chars=[%s], strong=%b, closed=%b", node.getChars(), node.isStrong(), node.isClosed() );

    final Consumer<ExpLinkNode> eln = ( node ) ->
           System.out.printf( " title=[%s], url=[%s]", node.title, node.url );
    
    final Consumer<AnchorLinkNode> aln = ( node ) ->
           System.out.printf( " name=[%s], text=[%s]", node.getName(), node.getText());

    final Consumer<VerbatimNode> vln = ( node ) ->
           System.out.printf( " text=[%s], type=[%s]", node.getText(), node.getType());

    final Consumer<RefLinkNode> rln = ( node ) -> {

        System.out.printf( " separatorSpace=[%s]", node.separatorSpace);

       if( node.referenceKey != null  ) {
           System.out.println();
           node.referenceKey.accept(newVisitor(4));
       }

    };

    Visitor newVisitor( final int start_indent ) {
        final ClassLoader cl = PegdownParse.class.getClassLoader();
        
        final InvocationHandler handler = new InvocationHandler() {

            int indent;
            {
                this.indent = start_indent;
            }

            protected void visitChildren(Object proxy, Node node ) {
                    for (Node child : node.getChildren()) {
                        child.accept((Visitor) proxy);
                    }
            }
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                for( int i = 0; i <indent ; ++i ) System.out.print('\t');
                final Object n = args[0];

                System.out.printf( "[%s]", n );
                IfContext.iF(n, StrongEmphSuperNode.class, sesn)
                            .elseIf(n, ExpLinkNode.class, eln)
                            .elseIf(n, AnchorLinkNode.class, aln)
                            .elseIf(n, VerbatimNode.class, vln)
                            .elseIf(n, RefLinkNode.class, rln)

                        ;
                System.out.println();

                if( n instanceof Node ) {
                    ++indent;
                    visitChildren(proxy, (Node)args[0]);
                    --indent;
                }
                return null;
            }

        };
                
        final Visitor proxy = (Visitor) Proxy.newProxyInstance(
                            cl,
                            new Class[] { Visitor.class },
                            handler);
        
        return proxy;

    }
    
    @Test
    public void parseTest() throws IOException {

        final PegDownProcessor p = new PegDownProcessor(PegdownConfluenceWikiVisitor.extensions() );


        final RootNode root = p.parseMarkdown(loadResource());

        root.accept(newVisitor(0));
    }

    private void notImplementedYet(Node node) {
        throw new UnsupportedOperationException( String.format("Node [%s] not supported yet. ", node.getClass().getSimpleName()) );
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public String serializeToString( Site.Page page ) throws IOException {

        final PegDownProcessor p = new PegDownProcessor(PegdownConfluenceWikiVisitor.extensions());

        final RootNode root = p.parseMarkdown(loadResource());

        PegdownConfluenceWikiVisitor ser =  new PegdownConfluenceWikiVisitor(new MarkdownParserContext() {
            @Override
            public Optional<Site> getSite() {
                return Optional.empty();
            }

            @Override
            public Optional<Site.Page> getPage() {
                return Optional.of(page);
            }

            @Override
            public Optional<String> getPagePrefixToApply() {
                return Optional.of("Parent Page Title");
            }

            @Override
            public boolean isLinkPrefixEnabled() {
                return true;
            }

        }, this::notImplementedYet);

        root.accept( ser );

        return ser.toString() ;

    }
}
