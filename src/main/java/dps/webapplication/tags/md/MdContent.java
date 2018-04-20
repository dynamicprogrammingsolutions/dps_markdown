package dps.webapplication.tags.md;

import dps.markdown.Markdown;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Date;

public class MdContent extends SimpleTagSupport {

    @Inject
    Markdown md;

    String name = null;
    Date updated = null;

    public void setName(String name)
    {
        this.name = name;
    }
    public void setUpdated(String updated) { this.updated = new Date(Long.valueOf(updated)); }

    @Override
    public void doTag() throws JspException, IOException {
        md.writeMdContents(name,updated,getJspContext().getOut(),(writer) -> {
            getJspBody().invoke(writer);
        });
    }
}
