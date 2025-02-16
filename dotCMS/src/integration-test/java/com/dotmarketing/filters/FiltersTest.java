package com.dotmarketing.filters;

import static com.dotcms.datagen.TestDataUtils.getNewsLikeContentType;
import static com.dotcms.vanityurl.business.VanityUrlAPIImpl.LEGACY_CMS_HOME_PAGE;
import static org.mockito.Matchers.startsWith;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.rendering.velocity.servlet.VelocityServlet;
import com.dotcms.util.FiltersUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.servlets.SpeedyAssetServlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class FiltersTest {

    private static ContentletAPI contentletAPI;
    private static Host site;
    private static User user;
    private static long defaultLanguageId;
    private static FiltersUtil filtersUtil;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
        Mockito.when(Config.CONTEXT.getRealPath(startsWith("/"))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });

        Mockito.when(Config.CONTEXT.getResourceAsStream(startsWith("/")))
                .thenAnswer(
                        (Answer<InputStream>) invocation ->
                                Files.newInputStream(Paths.get((String) invocation.getArguments()[0])));
        IntegrationTestInitService.getInstance().mockStrutsActionModule();

        /* Default user */
        user = APILocator.systemUser();

        /* APIs initialization */
        contentletAPI = APILocator.getContentletAPI();
        filtersUtil = FiltersUtil.getInstance();

        /* Default variables */
        site = new SiteDataGen().nextPersisted();
        defaultLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        //Create Folders
        final Folder aboutUsFolder = new FolderDataGen().site(site)
                .name("about-us")
                .title("about-us")
                .nextPersisted();
        final Folder productsFolder = new FolderDataGen().site(site)
                .name("products")
                .title("products")
                .nextPersisted();
        final Folder intranetFolder = new FolderDataGen().site(site)
                .name("intranet")
                .title("intranet")
                .nextPersisted();

        //Create Template.
        Template template = new TemplateDataGen().nextPersisted();

        //Create test pages
        HTMLPageAsset aboutUsIndexPage = new HTMLPageDataGen(aboutUsFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted();
        HTMLPageDataGen.publish(aboutUsIndexPage);
        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(aboutUsIndexPage.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                aboutUsIndexPage, APILocator.systemUser(), false);

        HTMLPageAsset productsIndexPage = new HTMLPageDataGen(productsFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted();
        HTMLPageDataGen.publish(productsIndexPage);
        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(productsIndexPage.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                productsIndexPage, APILocator.systemUser(), false);

        HTMLPageAsset intranetIndexPage = new HTMLPageDataGen(intranetFolder, template)
                .friendlyName("index")
                .pageURL("index")
                .title("index")
                .nextPersisted();
        HTMLPageDataGen.publish(intranetIndexPage);
        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(intranetIndexPage.getPermissionId(),
                        TestUserUtils.getOrCreateIntranetRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                intranetIndexPage, APILocator.systemUser(), false);
    }

    @Test
    public void shouldWorkVanityUrl() throws IOException, DotDataException {

        //Init APIs and test values
        Contentlet vanityUrl1 = null;
        Contentlet vanityUrl2 = null;
        Contentlet vanityUrl3 = null;
        Contentlet vanityUrl4 = null;
        Contentlet vanityUrl5 = null;

        // build them up
        try {
            vanityUrl1 = filtersUtil.createVanityUrl("test link1", Host.SYSTEM_HOST, "/testLink1",
                    "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityUrl1);

            vanityUrl2 = filtersUtil.createVanityUrl("test link2", site.getIdentifier(),
                    "/testLink2", "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1,
                    defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityUrl2);

            vanityUrl3 = filtersUtil.createVanityUrl("test link3", site.getIdentifier(),
                    "/testLink3", "http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    301, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityUrl3);

            vanityUrl4 = filtersUtil.createVanityUrl("test link4", site.getIdentifier(),
                    "/testLink4", "http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    301, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityUrl4);

            vanityUrl5 = filtersUtil.createVanityUrl("test link5", Host.SYSTEM_HOST,
                    "forbidden", "/products/" + CMSFilter.CMS_INDEX_PAGE, 302, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityUrl5);

            VanityURLFilter filter = new VanityURLFilter();
            HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
            MockResponseWrapper response = new MockResponseWrapper(res);
            FilterChain chain = Mockito.mock(FilterChain.class);
            Logger.info(this.getClass(),
                    "/testLink1 should forward to /about-us/" + CMSFilter.CMS_INDEX_PAGE);
            HttpServletRequest request = getMockRequest(site.getHostname(), "/testLink1");
            filter.doFilter(request, response, chain);
            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));

            Logger.info(this.getClass(),
                    "/testLink2 should forward to /about-us/" + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest(site.getHostname(), "/testLink2");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));

            Logger.info(this.getClass(),
                    "/testLink3 should redirect to http://demo.dotcms.com/about-us/"
                            + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest(site.getHostname(), "/testLink3");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Assert.assertEquals(301, response.getStatus());
            Assert.assertEquals("http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    response.getRedirectLocation());

            Logger.info(this.getClass(),
                    "/testLink4 should redirect to http://demo.dotcms.com/about-us/"
                            + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest(site.getHostname(), "/testLink4");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Assert.assertEquals(301, response.getStatus());
            Assert.assertEquals("http://demo.dotcms.com/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    response.getRedirectLocation());

            Logger.info(this.getClass(),
                    "/forbidden should forward to /products/" + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest(site.getHostname(), "/forbidden");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Assert.assertEquals(200, response.getStatus());

        } catch (Exception e) {

            e.printStackTrace();
            Assert.fail();

        } finally {
            // cleanup
            try {
                if (vanityUrl1 != null) {
                    contentletAPI.archive(vanityUrl1, user, false);
                    contentletAPI.delete(vanityUrl1, user, false);
                }
                if (vanityUrl2 != null) {
                    contentletAPI.archive(vanityUrl2, user, false);
                    contentletAPI.delete(vanityUrl2, user, false);
                }
                if (vanityUrl3 != null) {
                    contentletAPI.archive(vanityUrl3, user, false);
                    contentletAPI.delete(vanityUrl3, user, false);
                }
                if (vanityUrl4 != null) {
                    contentletAPI.archive(vanityUrl4, user, false);
                    contentletAPI.delete(vanityUrl4, user, false);
                }
                if (vanityUrl5 != null) {
                    contentletAPI.archive(vanityUrl5, user, false);
                    contentletAPI.delete(vanityUrl5, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error cleaning up Vanity Url Links");
            }

        }
    }

    /**
     * Creates a vanity url that will change the cmsHomePage(redirect) to about-us/index.
     *
     * @throws IOException
     * @throws DotDataException
     */
    @Test
    public void redirectHomePageTest() throws IOException, DotDataException {

        Contentlet vanityURLContentlet = null;

        String forwardTo = "/about-us/" + CMSFilter.CMS_INDEX_PAGE;
        try {

            //Create the VanityURL
            vanityURLContentlet = filtersUtil
                    .createVanityUrl("cmsHomePage", site.getIdentifier(),
                            LEGACY_CMS_HOME_PAGE,
                            forwardTo, 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            VanityURLFilter filter = new VanityURLFilter();
            FilterChain chain = Mockito.mock(FilterChain.class);

            Logger.info(this.getClass(),
                    "/cmsHomePage should forward to /about-us/" + CMSFilter.CMS_INDEX_PAGE);
            HttpServletRequest request = getMockRequest(site.getHostname(), "/");
            MockResponseWrapper response = new MockResponseWrapper(
                    Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got:" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /about-us/" + CMSFilter.CMS_INDEX_PAGE + ", got:" + request
                            .getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));
            //Delete the test Vanity URL
            contentletAPI.archive(vanityURLContentlet, user, false);
            contentletAPI.delete(vanityURLContentlet, user, false);

            //And save it
            vanityURLContentlet = filtersUtil.createVanityUrl("cmsHomePage Host", Host.SYSTEM_HOST,
                    LEGACY_CMS_HOME_PAGE,
                    "/about-us/" + CMSFilter.CMS_INDEX_PAGE, 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            Logger.info(this.getClass(), "demo.dotcms.com:/cmsHomePage should forward to /about-us/"
                    + CMSFilter.CMS_INDEX_PAGE);
            request = getMockRequest(site.getHostname(), "/");
            response = new MockResponseWrapper(Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got:" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for /about-us" + CMSFilter.CMS_INDEX_PAGE + ", got:" + request
                            .getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/about-us/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));

        } catch (Exception e) {

            e.printStackTrace();
            Assert.fail();

        } finally {
            try {
                //Delete the test Vanity URL
                if(vanityURLContentlet != null) {
                    contentletAPI.archive(vanityURLContentlet, user, false);
                    contentletAPI.delete(vanityURLContentlet, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error deleting Vanity URL");
            }

        }
    }

    /**
     * Creates a vanity url that will redirect to a content that is a url map (news).
     *
     * @throws IOException
     * @throws DotDataException
     */
    @Test
    public void vanityRedirectURLMAP() throws IOException, DotDataException {

        Contentlet vanityURLContentlet = null;

        try {

            final String newsPatternPrefix =
                    "/testpattern" + System.currentTimeMillis() + "/";

            final String parent1Name = "news-events";
            Folder parent1 = new FolderDataGen().name(parent1Name).title(parent1Name).site(site)
                    .nextPersisted();
            final String parent2Name = "news";
            Folder parent2 = new FolderDataGen().name(parent2Name).title(parent2Name)
                    .parent(parent1)
                    .nextPersisted();

            Template template = new TemplateDataGen().nextPersisted();
            HTMLPageAsset pageDetail = new HTMLPageDataGen(parent2, template)
                    .pageURL("news-detail")
                    .title("news-detail")
                    .nextPersisted();

            final ContentType newsContentType = getNewsLikeContentType(
                    "News" + System.currentTimeMillis(),
                    site,
                    pageDetail.getIdentifier(),
                    newsPatternPrefix + "{urlTitle}");

            final Contentlet newsTestContent = TestDataUtils
                    .getNewsContent(true, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            newsContentType.id(), site);

            final String forwardTo =
                    newsPatternPrefix + newsTestContent.getStringProperty("urlTitle");

            //Create the VanityURL
            vanityURLContentlet = filtersUtil
                    .createVanityUrl("urlmap", site.getIdentifier(),
                            "/urlnews",
                            forwardTo, 200, 1, defaultLanguageId);
            filtersUtil.publishVanityUrl(vanityURLContentlet);

            VanityURLFilter filter = new VanityURLFilter();
            FilterChain chain = Mockito.mock(FilterChain.class);

            Logger.info(this.getClass(),
                    "/urlnews should forward to " + forwardTo);
            final HttpServletRequest request = getMockRequest(site.getHostname(), "/urlnews");
            final MockResponseWrapper response = new MockResponseWrapper(
                    Mockito.mock(HttpServletResponse.class));
            filter.doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got:" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Logger.info(this.getClass(),
                    "looking for " + forwardTo + ", got:" + request
                            .getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals(forwardTo,
                    request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));

        } catch (Exception e) {

            e.printStackTrace();
            Assert.fail();

        } finally {
            try {
                //Delete the test Vanity URL
                if(vanityURLContentlet != null) {
                    contentletAPI.archive(vanityURLContentlet, user, false);
                    contentletAPI.delete(vanityURLContentlet, user, false);
                }
            } catch (Exception e) {
                Logger.error(this.getClass(), "Error deleting Vanity URL");
            }

        }
    }

    private MockResponseWrapper getMockResponse() throws IOException {
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(response.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));
        return new MockResponseWrapper(response);
    }

    private HttpServletRequest getMockRequest(String hostName, String uri) {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getAttribute("host")).thenReturn(site);
        Mockito.when(request.getRequestURI()).thenReturn(uri);
        Mockito.when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://" + hostName + uri));
        Mockito.when(request.getCookies()).thenReturn(new Cookie[]{});
        Mockito.when(request.getServerName()).thenReturn(hostName);
        Mockito.when(request.getSession()).thenReturn(new MockSession());
        Mockito.when(request.getSession(Mockito.anyBoolean())).thenReturn(new MockSession());
        Mockito.when(request.getRequestDispatcher("/servlets/VelocityServlet"))
                .thenReturn(new RequestDispatcher() {

                    @Override
                    public void include(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                    }

                    @Override
                    public void forward(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                        VelocityServlet servlet = new VelocityServlet();
                        servlet.init(null);
                        servlet.service(arg0, arg1);
                    }
                });
        Mockito.when(request.getRequestDispatcher(Mockito.startsWith("/dotAsset/")))
                .thenReturn(new RequestDispatcher() {

                    @Override
                    public void include(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                    }

                    @Override
                    public void forward(ServletRequest arg0, ServletResponse arg1)
                            throws ServletException, IOException {

                        SpeedyAssetServlet servlet = new SpeedyAssetServlet();
                        servlet.init(null);
                        servlet.service(arg0, arg1);
                    }
                });

        MockRequestWrapper reqWrap = new MockRequestWrapper(request);
        reqWrap.setAttribute("host", site);
        return reqWrap;

    }


    public void runTests() throws IOException, DotDataException {

        shouldRedirectToFolderIndex();
        shouldWorkVanityUrl();
        shouldForwardToImage();
        shouldRedirect401();
        redirectHomePageTest();
    }

    @Test
    public void shouldReturnStrutsPage() throws IOException {

        Logger.info(this.getClass(),
                "/dotCMS/login should forward to /application/login/" + CMSFilter.CMS_INDEX_PAGE);
        MockResponseWrapper response = new MockResponseWrapper(
                Mockito.mock(HttpServletResponse.class));
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest(site.getHostname(), "/dotCMS/login");

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
            Mockito.verify(chain).doFilter(request, response);
        } catch (ServletException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /**
     * This tests the demo site for its 404 image
     */
    @Test
    public void shouldForwardToImage() throws IOException {

        ServletOutputStream sos = Mockito.mock(ServletOutputStream.class);

        Logger.info(this.getClass(), "/images/404.jpg should give us a 200");
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        Mockito.when(res.getOutputStream()).thenReturn(sos);
        MockResponseWrapper response = new MockResponseWrapper(res);

        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest(site.getHostname(), "/images/404.jpg");

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());


        } catch (ServletException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /**
     * This tests the demo site for its 404 image
     */
    @Test
    public void shouldRedirect401() throws IOException {

        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        MockResponseWrapper response = new MockResponseWrapper(res);

        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest(site.getHostname(), "/intranet/");

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(), "looking for 401, got;" + response.getStatus());
            Assert.assertEquals(401, response.getStatus());


        } catch (ServletException e) {
            Assert.fail();
            e.printStackTrace();
        }

    }

    /*
     * This tests if the cms filter correctly redirects a user from /products to
     * /products/
     */
    @Test
    public void shouldRedirectToFolderIndex() throws IOException {
        Logger.info(this.getClass(), "/products should redirect to /products/");

        MockResponseWrapper response = new MockResponseWrapper(
                Mockito.mock(HttpServletResponse.class));
        FilterChain chain = Mockito.mock(FilterChain.class);
        HttpServletRequest request = getMockRequest(site.getHostname(), "/products");

        try {
            new CMSFilter().doFilter(request, response, chain);

            Assert.assertEquals("/products/",
                    response.getRedirectLocation());
            Assert.assertEquals(301, response.getStatus());
        } catch (ServletException e) {
            Assert.fail();
        }

        Logger.info(this.getClass(),
                "/home/ should forward to /products/" + CMSFilter.CMS_INDEX_PAGE);
        request = getMockRequest(site.getHostname(), "/products/");
        response = getMockResponse();

        try {
            new CMSFilter().doFilter(request, response, chain);
            Logger.info(this.getClass(),
                    "looking for /products/" + CMSFilter.CMS_INDEX_PAGE + " , got;" + request
                            .getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));
            Assert.assertEquals("/products/" + CMSFilter.CMS_INDEX_PAGE,
                    request.getAttribute(Constants.CMS_FILTER_URI_OVERRIDE));
            Logger.info(this.getClass(), "looking for 200, got;" + response.getStatus());
            Assert.assertEquals(200, response.getStatus());
        } catch (ServletException e) {
            Assert.fail();
        }


    }

    class MockRequestWrapper extends HttpServletRequestWrapper {

        Map<String, Object> valmap = new HashMap<>();

        public MockRequestWrapper(HttpServletRequest request) {
            super(request);

        }

        @Override
        public void setAttribute(String arg0, Object arg1) {
            valmap.put(arg0, arg1);

        }

        @Override
        public Object getAttribute(String arg0) {
            return valmap.get(arg0);
        }

    }

    class MockResponseWrapper extends HttpServletResponseWrapper {

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        int status = 200;
        String location = null;
        Map<String, String> headers = new HashMap<String, String>();

        @Override
        public PrintWriter getWriter() throws IOException {
            return writer;
        }

        public MockResponseWrapper(HttpServletResponse response) {
            super(response);

        }

        @Override
        public int getStatus() {
            // TODO Auto-generated method stub
            return status;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            status = sc;
            Logger.info(this.getClass(), msg);
        }

        @Override
        public void sendError(int sc) throws IOException {
            status = sc;
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            this.location = location;
            Logger.info(this.getClass(), "redirecting;" + location);
            status = 301;
        }

        @Override
        public void setStatus(int sc, String sm) {
            Logger.info(this.getClass(), sm);
            status = sc;
        }

        @Override
        public void setStatus(int sc) {
            status = sc;
        }

        public String getRedirectLocation() {
            if (location != null && !location.isEmpty()) {
                return location;
            }
            return headers.get("Location");
        }

        @Override
        public void setHeader(String key, String value) {
            headers.put(key, value);
        }
    }

    class MockSession implements HttpSession {

        Map<String, Object> valmap = new HashMap<>();

        @Override
        public void setMaxInactiveInterval(int arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setAttribute(String arg0, Object arg1) {
            valmap.put(arg0, arg1);

        }

        @Override
        public void removeValue(String arg0) {
            valmap.remove(arg0);

        }

        @Override
        public void removeAttribute(String arg0) {
            valmap.remove(arg0);

        }

        @Override
        public void putValue(String arg0, Object arg1) {
            valmap.put(arg0, arg1);

        }

        @Override
        public boolean isNew() {

            return true;
        }

        @Override
        public void invalidate() {
            valmap = new HashMap<>();

        }

        @Override
        public String[] getValueNames() {

            return valmap.keySet().toArray(new String[valmap.size()]);
        }

        @Override
        public Object getValue(String arg0) {
            return valmap.get(arg0);
        }

        @Override
        public ServletContext getServletContext() {

            return null;
        }

        @Override
        public int getMaxInactiveInterval() {

            return 0;
        }

        @Override
        public long getLastAccessedTime() {

            return 0;
        }

        @Override
        public String getId() {

            return null;
        }

        @Override
        public long getCreationTime() {

            return 0;
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getAttribute(String arg0) {
            return valmap.get(arg0);
        }

        @Override
        public HttpSessionContext getSessionContext() {
            // TODO Auto-generated method stub
            return null;
        }
    }

}