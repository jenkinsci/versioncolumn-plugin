package hudson.plugin.versioncolumn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.xml.HasXPath.hasXPath;

import hudson.model.User;
import hudson.security.HudsonPrivateSecurityRealm;
import org.htmlunit.xml.XmlPage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

public class RESTAPITest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();

    private final String USER_NAME = "user-for-RESTAPITest";

    private WebClient webClient = null;

    @Before
    public void setup() throws Exception {
        final String PASSWORD = "password-for-RESTAPITest";
        HudsonPrivateSecurityRealm securityRealm = new HudsonPrivateSecurityRealm(false, false, null);
        rule.jenkins.setSecurityRealm(securityRealm);

        User user = securityRealm.createAccount(USER_NAME, PASSWORD);
        user.setFullName("User for REST API test");
        user.save();

        webClient = rule.createWebClient();
        webClient.login(USER_NAME, PASSWORD);
    }

    @Test
    public void securedAPILoginTest() throws Exception {
        // Check that authnenticated access to the API is allowed
        XmlPage w1 = (XmlPage) webClient.goTo("whoAmI/api/xml", "application/xml");
        assertThat(w1, hasXPath("//name", is(USER_NAME)));
    }

    @Issue("JENKINS-70074")
    @Test
    public void securedAPITest() throws Exception {
        // Interactive testing failed while this automated test did
        // not fail.  Unclear why they differ. Including this in the
        // test suite because it may prevent future issues.
        XmlPage computerApi = (XmlPage) webClient.goTo("computer/api/xml", "application/xml");
        assertThat(computerApi, hasXPath("//name", is("built-in")));
    }
}
