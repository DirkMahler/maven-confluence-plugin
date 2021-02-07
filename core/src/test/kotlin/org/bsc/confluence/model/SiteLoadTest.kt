package org.bsc.confluence.model

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths


class SiteLoadTest {

    private fun loadFromYAML(resource: String, c: ( site:Site ) -> Unit = {})  {
        val mapper = ObjectMapper(YAMLFactory())
        javaClass.classLoader.getResourceAsStream(resource).use { `is` ->
            val site = mapper.readValue(`is`, Site::class.java)
            assertNotNull(site)
            c(site)
        }
    }
    private fun loadFromXML(resource: String, c: ( site:Site ) -> Unit = {})  {
        val mapper = XmlMapper() //ObjectMapper(XmlFactory())
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.registerModule(JaxbAnnotationModule())

        javaClass.classLoader.getResourceAsStream(resource).use { `is` ->
            val site = mapper.readValue(`is`, Site::class.java)
            assertNotNull(site)
            c(site)
        }
    }

    @Test
    fun testIssue182() = assertThrows(JsonMappingException::class.java) {
            loadFromYAML("site-issue182.yaml")
    }

    @Test
    fun testLoadFromXML() {
        val tempDirectory = Files.createTempDirectory(null)

        loadFromXML("site.xml") { site ->

            val basedir = Paths.get(tempDirectory.toString())
            site.basedir = basedir

            val home:Site.IPageAnchor = site.getHomeAnchor()
            assertNotNull(home)
            assertTrue(home.isPage)

            val uri = Paths.get(basedir.toString(), "encoding.confluence").toUri()
            assertEquals( uri, home.asPage().uri)

            val children = home.children
            assertNotNull(children)
            assertEquals(2, children.size)

            val attachments = home.asPage().attachments
            assertNotNull(attachments)
            assertEquals(1, attachments.size)

            val labels = site.labels
            assertNotNull(labels)
            assertEquals(2, labels.size)
        }
    }

    @Test
    fun testLoadFromYAML() {
        val tempDirectory = Files.createTempDirectory(null)

        loadFromYAML("site.yaml") { site ->

            val basedir = Paths.get(tempDirectory.toString())
            site.basedir = basedir

            val home:Site.IPageAnchor = site.getHomeAnchor()
            assertNotNull(home)
            assertTrue(home.isPage)

            val uri = Paths.get(basedir.toString(), "encoding.confluence").toUri()
            assertEquals( uri, home.asPage().uri)

            val children = home.children
            assertNotNull(children)
            assertEquals(2, children.size)

            val attachments = home.asPage().attachments
            assertNotNull(attachments)
            assertEquals(1, attachments.size)

            val labels = site.labels
            assertNotNull(labels)
            assertEquals(2, labels.size)

        }
    }


}