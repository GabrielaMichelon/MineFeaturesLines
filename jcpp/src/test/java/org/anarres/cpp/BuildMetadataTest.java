package org.anarres.cpp;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 *
 * @author shevek
 */
public class BuildMetadataTest {

    private static final Logger LOG = LoggerFactory.getLogger(BuildMetadataTest.class);

    @Test
    public void testProperties() throws Exception {
        URL url = Resources.getResource("META-INF/jcpp.properties");
        String text = Resources.asCharSource(url, Charsets.ISO_8859_1).read();
        LOG.info("Metadata is " + text);
    }

    @Test
    public void testMetadata() throws Exception {
        BuildMetadata metadata = BuildMetadata.getInstance();
        LOG.info("Version is " + metadata.getVersion());
        LOG.info("BuildDate is " + metadata.getBuildDate());
        LOG.info("ChangeId is " + metadata.getChangeId());
    }

}
