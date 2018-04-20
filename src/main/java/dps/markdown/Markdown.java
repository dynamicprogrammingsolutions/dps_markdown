package dps.markdown;

import dps.webapplication.resources.Resources;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;

@ApplicationScoped
public class Markdown {

    @Inject
    Resources resources;

    static public Markdown getCurrent() {
        return CDI.current().select(Markdown.class).get();
    }

    static final Parser PARSER = Parser.builder().build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    private void writeMd(Node document, Writer writer)
    {
        RENDERER.render(document,writer);
    }

    public void writeMdResource(String name, Writer out) throws IOException, JspException
    {
        Path resourcePath = resources.getResourcePath(name);
        String cacheName = resources.getCacheName(name);
        Date updated = resources.getUpdated(resourcePath);
        writeMdContents(cacheName,updated,out,(writer) -> {
            Reader resource = resources.getResource(resourcePath);
            resources.writeTo(resource,writer);
        });
    }

    public void writeMdContents(String name, Date updated, Writer out, ParseSource parseSource) throws IOException, JspException {
        StringWriter stringWriter = new StringWriter();

        Reader cacheReader = this.getCacheReader(name, updated);

        if (cacheReader == null) {

            Writer cacheWriter = this.getCacheWriter(name, updated);

            Writer writer = new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    stringWriter.write(cbuf, off, len);
                }

                @Override
                public void flush() throws IOException {

                }

                @Override
                public void close() throws IOException {
                    stringWriter.close();
                    Node document = PARSER.parse(stringWriter.toString());
                    writeMd(document, out);
                    if (cacheWriter != null) writeMd(document,cacheWriter);
                }
            };

            parseSource.parse(writer);
            writer.close();
            if (cacheWriter != null) cacheWriter.close();
        } else {
            resources.writeTo(cacheReader,out);
        }

    }

    public interface ParseSource {
        void parse(Writer writer) throws JspException, IOException;
    }

    public String renderToString(Node node)
    {
        return RENDERER.render(node);
    }

    public Node getMd(String cacheName, String content)
    {
        Node document = PARSER.parse(content);
        return document;
    }

    public Reader getCacheReader(String name, Date updated)
    {
        if (name == null || updated == null) return null;

        String tempFolderPath = System.getProperty("jboss.server.temp.dir");

        try {

            Path mdparsedPath = Paths.get(tempFolderPath, "mdparsed", name + ".html");

            if (Files.exists(mdparsedPath)) {

                FileTime renderedLastModified = Files.getLastModifiedTime(mdparsedPath);

                if (renderedLastModified.toMillis() >= updated.getTime()) {
                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(Files.newInputStream(mdparsedPath)));
                    return streamReader;
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Writer getCacheWriter(String name, Date updated)
    {
        if (name == null || updated == null) return null;

        String tempFolderPath = System.getProperty("jboss.server.temp.dir");

        try {

            Path mdparsedPath = Paths.get(tempFolderPath, "mdparsed", name + ".html");
            Path mdparsedFolder = mdparsedPath.getParent();

            if (!Files.exists(mdparsedFolder)) {
                Files.createDirectories(mdparsedFolder);
            }

            if (!Files.exists(mdparsedPath)) {
                Files.createFile(mdparsedPath);
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(mdparsedPath));

            return outputStreamWriter;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public Reader getMd(String page)
    {
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource("pages/" + page + ".md");
        if (resourceUrl == null) return null;
        Path pagePath = Paths.get(resourceUrl.getFile());
        if (!Files.exists(pagePath)) {
            return null;
        }
        FileTime pageLastModified;
        try {
            pageLastModified = Files.getLastModifiedTime(pagePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String tempFolderPath = System.getProperty("jboss.server.temp.dir");
        Path mdparsedDir = Paths.get(tempFolderPath, "mdparsed");

        try {

            Path mdparsedPath = Paths.get(tempFolderPath, "mdparsed", page + ".html");
            Path mdparsedFolder = mdparsedPath.getParent();

            if (!Files.exists(mdparsedFolder)) {
                Files.createDirectories(mdparsedFolder);
            }

            boolean refresh = false;
            if (!Files.exists(mdparsedPath)) {
                Files.createFile(mdparsedPath);
                refresh = true;
            } else {
                FileTime renderedLastModified = Files.getLastModifiedTime(mdparsedPath);
                if (renderedLastModified.compareTo(pageLastModified) < 0) {
                    refresh = true;
                }
            }

            if (refresh) {

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(pagePath)))) {
                    Node document = PARSER.parseReader(reader);

                    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(mdparsedPath))) {
                        RENDERER.render(document, outputStreamWriter);
                    }

                }

            }

            BufferedReader streamReader = new BufferedReader(new InputStreamReader(Files.newInputStream(mdparsedPath)));
            return streamReader;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
