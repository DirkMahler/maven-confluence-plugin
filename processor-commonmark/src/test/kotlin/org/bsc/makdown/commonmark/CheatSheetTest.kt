package org.bsc.makdown.commonmark;

import org.bsc.confluence.model.Site
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.file.Paths


class CheatSheetTest {

    var site = Site().apply {
        basedir = Paths.get(System.getProperty("user.dir"))

        val home = Site.Home()
        home.uri = URI("./page.md")
        home.name = "page"

        setHome( home )

    }

    private fun parse( name:String ):String? = parseResource( this.javaClass, "cheatsheet/$name", this.site )

    @Test
    //@Ignore
    fun `parse headers`()  = Assertions.assertEquals( """
        h1. H1
        h2. H2
        h3. H3
        h4. H4
        h5. H5
        h6. H6
        h1. Alt-H1
        h2. Alt-H2
    """.trimIndent(), parse( "headers") )

    @Test
    //@Ignore
    fun `parse emphasis`()  = Assertions.assertEquals( """
        Emphasis, aka italics, with _asterisks_ or _underscores_.
        
        Strong emphasis, aka bold, with *asterisks* or *underscores*.
        
        Combined emphasis with *asterisks and _underscores_*.

        Strikethrough uses two tildes. -Scratch this.-
    """.trimIndent(), parse( "emphasis") )

    @Test
    //@Ignore
    fun `parse lists`() = Assertions.assertEquals(  """
    # First ordered list item
    # Another item
    #* Unordered sub-list.
    # Actual numbers don't matter, just that it's a number
    ## Ordered sub-list
    # And another item.
    You can have properly indented paragraphs within list items. Notice the blank line above, and the leading spaces (at least one, but we'll use three here to also align the raw Markdown).
    To have a line break without a paragraph, you will need to use two trailing spaces.
    Note that this line is separate, but within the same paragraph.
    (This is contrary to the typical GFM line break behaviour, where trailing spaces are not required.)
    
    Unordered list can use asterisks
    
    * asterisks 1
    * asterisks 2
    
    Or minuses
    
    * minuses 1
    * minuses 2
    
    Or pluses
    
    * pluses 1
    * pluses 2
    
    h1. Test break after ListBlock
    """.trimIndent(), parse( "lists") )

    @Test
    //@Ignore
    fun `parse images`() = Assertions.assertEquals( """
        Here's our logo (hover to see the title text):

        Inline-style:
        !https://github.com/adam-p/markdown-here/raw/master/src/common/images/icon48.png|alt text!
        
        Reference-style:
        !https://github.com/adam-p/markdown-here/raw/master/src/common/images/icon48.png|alt text!
    """.trimIndent(), parse( "images") )

    @Test
    //@Ignore
    fun `parse Block quote`() = Assertions.assertEquals( """
        {quote}
        Blockquotes are very handy in email to emulate reply text.
        This line is part of the same quote.
        {quote}

        Quote break.

        {quote}
        This is a very long line that will still be quoted properly when it wraps. Oh boy let's keep writing to make sure this is long enough to actually wrap for everyone. Oh, you can _put_ *Markdown* into a blockquote.
        {quote}
    """.trimIndent(), parse( "blockquote") )

    @Test
    //@Ignore
    fun `parse links`() = Assertions.assertEquals( """
        [I'm an inline-style link|https://www.google.com]

        [I'm an inline-style link with title|https://www.google.com|Google's Homepage]

        [I'm a reference-style link|https://www.mozilla.org]

        [I'm a relative reference to a repository file|../blob/master/LICENSE]

        [You can use numbers for reference-style link definitions|http://slashdot.org]

        Or leave it empty and use the [link text itself|http://www.reddit.com].

        URLs and URLs in angle brackets will automatically get turned into links.
        http://www.example.com or [http://www.example.com|http://www.example.com] and sometimes
        example.com (but not on Github, for example).

        Some text to show that the reference links can follow later.
    """.trimIndent(), parse( "links") )

    @Test
    //@Ignore
    fun `parse Code block`()  = Assertions.assertEquals( """
        Inline {{code}} has {{back-ticks around}} it.
        
        {code:javascript}
        var s = "JavaScript syntax highlighting";
        alert(s);
        {code}
        {code:python}
        s = "Python syntax highlighting"
        print s
        {code}
        h3. Shell
        {code}
        No language indicated, so no syntax highlighting. 
        But let's throw in a <b>tag</b>.
        {code}
    """.trimIndent(), parse( "code") )

    @Test
    //@Ignore
    fun `parse Horizontal rule`() = Assertions.assertEquals( """
        Three or more...
        
        ----
        Hyphens
        
        ----
        Asterisks
        
        ----
        Underscores
    """.trimIndent(), parse( "horizontalrule") )

    @Test
    //@Ignore
    fun `parse tables`() = Assertions.assertEquals( """
        Colons can be used to align columns.
        
        ||Tables||Are||Cool||
        |col 3 is|right-aligned|${'$'}1600|
        |col 2 is|centered|${'$'}12|
        |zebra stripes|are neat|${'$'}1|
        There must be at least 3 dashes separating each header cell.
        The outer pipes (|) are optional, and you don't need to make the
        raw Markdown line up prettily. You can also use inline Markdown.
        
        ||Markdown||Less||Pretty||
        |_Still_|{{renders}}|*nicely*|
        |1|2|3|
    """.trimIndent(), parse( "tables") )

    @Test
    //@Ignore
    fun `parse inline html`() = Assertions.assertEquals( """
        {html}
        <dl>
          <dt>Definition list</dt>
          <dd>Is something people use sometimes.</dd>
          <dt>Markdown in HTML</dt>
          <dd>Does *not* work **very** well. Use HTML <em>tags</em>.</dd>
        </dl>
        {html}
    """.trimIndent(), parse( "inlinehtml") )

}
