package com.dotcms.rendering.velocity.viewtools.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ContentToolTest extends IntegrationTestBase {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static Language defaultLanguage;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static User user;

	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        user    = userAPI.getSystemUser();

        contentletAPI  = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        fieldAPI       = APILocator.getContentTypeFieldAPI();
        languageAPI    = APILocator.getLanguageAPI();

        relationshipAPI = APILocator.getRelationshipAPI();
        defaultHost     = hostAPI.findDefaultHost(user, false);
        defaultLanguage = languageAPI.getDefaultLanguage();
	}

    public static class TestCase {
        int cardinality;
        Class parentExpectedType;
        Class childExpectedType;

        public TestCase(final int cardinality, final Class parentExpectedType,
                final Class childExpectedType) {
            this.cardinality = cardinality;
            this.parentExpectedType = parentExpectedType;
            this.childExpectedType = childExpectedType;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                new TestCase(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal(), List.class,
                        List.class),
                new TestCase(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), ContentMap.class,
                        List.class),
                new TestCase(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal(), ContentMap.class,
                        ContentMap.class)
        };
    }

    @Test
    public void testPullMultiLanguage() throws Exception { // https://github.com/dotCMS/core/issues/11172

    	// Test uses Spanish language
    	final long languageId = TestDataUtils.getSpanishLanguage().getId();

        // Get "News" content-type
        final ContentType contentType = contentTypeAPI.search(" velocity_var_name = 'News'").get(0);


        // Create dummy "News" content in Spanish language
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode()).host(defaultHost).languageId(languageId);

        contentletDataGen.setProperty("title", "El Titulo");
        contentletDataGen.setProperty("byline", "El Sub Titulo");
        contentletDataGen.setProperty("story", "EL Relato");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/el-titulo");

        // Persist dummy "News" contents to ensure at least one result will be returned
        final Contentlet contentlet = contentletDataGen.nextPersisted();

        final ContentTool contentTool = getContentTool(languageId);

        try {

            // Query contents through Content Tool
            final List<ContentMap> results = contentTool.pull(
            	"+structurename:news +(conhost:"+defaultHost.getIdentifier()+" conhost:system_host) +working:true", 6, "score News.sysPublishDate desc"
            );

            // Ensure that every returned content is in Spanish Language
            Assert.assertFalse(results.isEmpty());
            for(ContentMap cm : results) {
    	    	Assert.assertEquals(cm.getContentObject().getLanguageId(), languageId);
            }
	    } finally {

	    	// Clean-up contents (delete dummy "News" content)
	    	contentletDataGen.remove(contentlet);
	    }
    }

    @Test
    public void testPullRelated() throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType = null;

        final long time = System.currentTimeMillis();

        try {
            //creates parent content type
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            //creates child content type
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            Field field = createField(childContentType.variable(), parentContentType.id(),
                    childContentType.variable(),
                    String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            //creates a new parent contentlet
            ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            //creates children contentlets
            contentletDataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContentlet1 = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            final Contentlet childContentlet2 = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //relates parent contentlet with the child contentlet
            contentletAPI.relateContent(parentContentlet, relationship,
                    CollectionsUtils.list(childContentlet1, childContentlet2), user, false);

            //refresh relationships in the ES index
            contentletAPI.reindex(parentContentlet);
            contentletAPI.reindex(childContentlet1);
            contentletAPI.reindex(childContentlet2);

            final ContentTool contentTool = getContentTool(defaultLanguage.getId());

            final List<ContentMap> result = contentTool
                    .pullRelated(relationship.getRelationTypeValue(),
                            parentContentlet.getIdentifier(), "+working:true", false, -1, null);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.stream().map(elem -> elem.getContentObject().getIdentifier())
                    .allMatch(identifier -> identifier.equals(childContentlet1.getIdentifier())
                            || identifier.equals(childContentlet2.getIdentifier())));

        } finally {

            //clean up environment
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testPullRelatedField_success() throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType = null;

        final long time = System.currentTimeMillis();

        try {
            //creates parent content type
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            //creates child content type
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            Field field = createField(childContentType.variable(), parentContentType.id(),
                    childContentType.variable(),
                    String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            //creates a new parent contentlet
            ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            //creates a new child contentlet
            contentletDataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //relates parent contentlet with the child contentlet
            contentletAPI.relateContent(parentContentlet, relationship,
                    CollectionsUtils.list(childContentlet), user, false);

            //refresh relationships in the ES index
            contentletAPI.reindex(parentContentlet);
            contentletAPI.reindex(childContentlet);

            final ContentTool contentTool = getContentTool(defaultLanguage.getId());

            final List<ContentMap> result = contentTool
                    .pullRelatedField(
                            parentContentlet.getIdentifier(), fullFieldVar,"+working:true");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(childContentlet.getIdentifier(),result.get(0).getContentObject().getIdentifier());

        } finally {

            //clean up environment
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void testPullRelatedField_whenInvalidFieldIsSent_throwsAnException()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        ContentType childContentType = null;

        final long time = System.currentTimeMillis();

        try {
            //creates parent content type
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            //creates child content type
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            Field field = createField(childContentType.variable(), parentContentType.id(),
                    childContentType.variable(),
                    String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            //creates a new parent contentlet
            ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet parentContenlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            final ContentTool contentTool = getContentTool(defaultLanguage.getId());

            contentTool.pullRelatedField(
                            parentContenlet.getIdentifier(), field.variable(),"+working:true");

        } finally {

            //clean up environment
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    @UseDataProvider("testCases")
    public void testPullRelatedContent_whenRelationshipFieldExists(final TestCase testCase)
            throws DotSecurityException, DotDataException {

        ContentType parentContentType = null;
        ContentType childContentType = null;

        final long time = System.currentTimeMillis();

        try {
            //creates parent content type
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            //creates child content type
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            Field parentField = createField(childContentType.variable(), parentContentType.id(),
                    childContentType.variable(), String.valueOf(testCase.cardinality));

            //One side of the relationship is set parentContentType --> childContentType
            parentField = fieldAPI.save(parentField, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentField.variable();

            Field childField = createField(parentContentType.variable(), childContentType.id(),
                    fullFieldVar, String.valueOf(testCase.cardinality));

            //The other side of the relationship is set childContentType --> parentContentType
            childField = fieldAPI.save(childField, user);

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //creates a new parent contentlet
            ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            //creates a new child contentlet
            contentletDataGen = new ContentletDataGen(childContentType.id());
            Contentlet childContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            //relates parent contentlet with the child contentlet
            contentletAPI.relateContent(parentContentlet, relationship,
                    CollectionsUtils.list(childContentlet), user, false);

            //refresh relationships in the ES index
            contentletAPI.reindex(parentContentlet);
            contentletAPI.reindex(childContentlet);

            //pull and validate child
            validateRelationshipSide(testCase.childExpectedType, parentField, parentContentlet,
                    childContentlet);

            //pull and validate parent
            validateRelationshipSide(testCase.parentExpectedType, childField, childContentlet,
                    parentContentlet);

        } finally {

            //clean up environment
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    private void validateRelationshipSide(final Class expectedType, final Field field,
            final Contentlet leftSideContentlet, final Contentlet rightSideContentlet) {
        final ContentTool contentTool = getContentTool(defaultLanguage.getId());


        final List<ContentMap> result = contentTool
                .pull("+identifier:" + leftSideContentlet.getIdentifier() + " +working:true", 1,
                        null);
        assertNotNull(result);
        assertEquals(1, result.size());

        //lazy load related contentlet
        final Object relatedContent = result.get(0).get(field.variable());
        assertNotNull(relatedContent);
        assertTrue(expectedType.isInstance(relatedContent));

        if (expectedType.equals(List.class)){
            final List relatedContentList = (List)relatedContent;
            assertEquals(1, relatedContentList.size());
            assertEquals(rightSideContentlet.getIdentifier(),
                    ((ContentMap) relatedContentList.get(0)).get("identifier"));
        }
    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }


    private Field createField(String fieldName, String contentTypeId, String relationType, String cardinality){
        return FieldBuilder.builder(RelationshipField.class).name(fieldName)
                .contentTypeId(contentTypeId).values(cardinality)
                .relationType(relationType).required(false).build();
    }

    private ContentTool getContentTool(final long languageId){
        // Mock ContentTool to retrieve content in Spanish language
        final ViewContext viewContext = mock(ViewContext.class);
        final Context velocityContext = mock(Context.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getParameter("host_id")).thenReturn(defaultHost.getInode());
        when(request.getParameter("language_id")).thenReturn(String.valueOf(languageId));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(user);

        final ContentTool contentTool = new ContentTool();
        contentTool.init(viewContext);
        return contentTool;
    }
}
