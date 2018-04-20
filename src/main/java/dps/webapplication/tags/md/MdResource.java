package dps.webapplication.tags.md;

import dps.markdown.Markdown;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

public class MdResource extends SimpleTagSupport {

    @Inject Markdown md;

    String name = null;

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void doTag() throws JspException, IOException {
        md.writeMdResource(name,getJspContext().getOut());
    }
}
