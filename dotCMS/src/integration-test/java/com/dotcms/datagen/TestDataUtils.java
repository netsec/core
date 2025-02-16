package com.dotcms.datagen;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.CustomField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.LineDividerField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.TagField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.ConfigTestHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.UtilMethods;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Jonathan Gamba 2019-04-16
 */
public class TestDataUtils {

    public static ContentType getBlogLikeContentType() {
        return getBlogLikeContentType("Blog" + System.currentTimeMillis());
    }

    public static ContentType getBlogLikeContentType(final String contentTypeName) {
        return getBlogLikeContentType(contentTypeName, null, null);
    }

    public static ContentType getBlogLikeContentType(final String contentTypeName,
            final Host site) {
            return getBlogLikeContentType(contentTypeName, site,null);
    }

    public static ContentType getBlogLikeContentType(final Host site) {
        return getBlogLikeContentType("Blog" + System.currentTimeMillis(), site);
    }

    public static ContentType getBlogLikeContentType(final String contentTypeName,
            final Host site, final Set <String> workflowIds) {

        ContentType blogType = null;
        try {
            try {
                blogType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (blogType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("author")
                                .velocityVarName("author")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("body")
                                .velocityVarName("body")
                                .searchable(true).indexed(true)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Publish")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );

                //Category field
                final Collection<Category> topLevelCategories = APILocator.getCategoryAPI()
                        .findTopLevelCategories(APILocator.systemUser(), false);
                final Optional<Category> anyTopLevelCategory = topLevelCategories.stream()
                        .findAny();

                anyTopLevelCategory.map(category -> new FieldDataGen()
                        .type(CategoryField.class)
                        .defaultValue(null)
                        .values(category.getInode())
                        .next()).ifPresent(fields::add);

                /*//Relationships field
                fields.add(
                        new FieldDataGen()
                                .name("Blog-Comments")
                                .velocityVarName("blogComments")
                                .defaultValue(null)
                                .type(RelationshipField.class)
                                .values(String
                                        .valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                                .relationType("Comments")
                                .next()
                );*/

                /*//Relationships field
                fields.add(
                        new FieldDataGen()
                                .name("Blog-Blog")
                                .velocityVarName("blogBlog")
                                .defaultValue(null)
                                .type(RelationshipField.class)
                                .values(String
                                        .valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()))
                                .relationType(contentTypeName + StringPool.PERIOD + "blogBlog")
                                .next()
                );*/

                blogType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return blogType;
    }

    public static ContentType getCommentsLikeContentType() {
        return getCommentsLikeContentType("Comments" + System.currentTimeMillis());
    }

    public static ContentType getCommentsLikeContentType(final String contentTypeName){
        return getCommentsLikeContentType(contentTypeName, null);
    }

    public static ContentType getCommentsLikeContentType(final String contentTypeName, final Set<String> workflowIds) {

        ContentType commentsType = null;
        try {
            try {
                commentsType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (commentsType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("email")
                                .velocityVarName("email")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("comment")
                                .velocityVarName("comment")
                                .next()
                );

                commentsType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return commentsType;
    }

    public static ContentType getEmployeeLikeContentType() {
        return getEmployeeLikeContentType("Employee" + System.currentTimeMillis());
    }

    public static ContentType getEmployeeLikeContentType(final String contentTypeName) {
        return getEmployeeLikeContentType(contentTypeName, null, null);
    }

    public static ContentType getEmployeeLikeContentType(final String contentTypeName,
    final Host site) {
        return getEmployeeLikeContentType(contentTypeName, site, null);
    }

    public static ContentType getEmployeeLikeContentType(final String contentTypeName,
            final Host site,  final Set<String> workflowIds) {

        ContentType employeeType = null;
        try {
            try {
                employeeType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (employeeType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("First Name")
                                .velocityVarName("firstName")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Last Name")
                                .velocityVarName("lastName")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("jobTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Phone")
                                .velocityVarName("phone")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Mobile")
                                .velocityVarName("mobile")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Fax")
                                .velocityVarName("fax")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Email")
                                .velocityVarName("email")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Photo")
                                .velocityVarName("photo")
                                .type(BinaryField.class)
                                .next()
                );

                employeeType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return employeeType;
    }

    public static ContentType getNewsLikeContentType() {
        return getNewsLikeContentType("News" + System.currentTimeMillis(), null, null, null, null,null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName) {
        return getNewsLikeContentType(contentTypeName, null, null, null, null,null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName, final String parentCategoryInode) {
        return getNewsLikeContentType(contentTypeName, null, null, null, null,parentCategoryInode);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final Host site) {
        return getNewsLikeContentType(contentTypeName, site, null, null, null,null);
    }

    public static ContentType getNewsLikeContentType(final Host site) {
        return getNewsLikeContentType("News" + System.currentTimeMillis(), site, null, null, null,null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final Host site,
            final String detailPageIdentifier,
            final String urlMapPattern) {
        return getNewsLikeContentType(contentTypeName, site, detailPageIdentifier, urlMapPattern, null,null);
    }

    public static ContentType getNewsLikeContentType(final String contentTypeName,
            final Host site,
            final String detailPageIdentifier,
            final String urlMapPattern,
            final Set<String> workflowIds,
            String parentCategoryInode) {

        ContentType newsType = null;
        try {
            try {
                newsType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (newsType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("By line")
                                .velocityVarName("byline")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Publish")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Story")
                                .velocityVarName("story")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Tags")
                                .velocityVarName("tags")
                                .defaultValue(null)
                                .type(TagField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Geolocation")
                                .velocityVarName("latlong")
                                .type(TextField.class)
                                .indexed(true)
                                .next()
                );
                if(!UtilMethods.isSet(parentCategoryInode)) {
                    parentCategoryInode = new CategoryDataGen().nextPersisted().getInode();
                }
                fields.add(
                        new FieldDataGen()
                                .name("Categories")
                                .velocityVarName("categories")
                                .type(CategoryField.class)
                                .defaultValue(null)
                                .values(parentCategoryInode)
                                .next()
                );

                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                if (null != detailPageIdentifier) {
                    contentTypeDataGen.detailPage(detailPageIdentifier);
                }

                if (null != urlMapPattern) {
                    contentTypeDataGen.urlMapPattern(urlMapPattern);
                }

                newsType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return newsType;
    }

    public static ContentType getWikiLikeContentType() {
        return getWikiLikeContentType("Wiki" + System.currentTimeMillis(), null);
    }

    public static ContentType getWikiLikeContentType(final String contentTypeName, final Set<String> workflowIds) {

        ContentType wikiType = null;
        try {
            try {
                wikiType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (wikiType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("By line")
                                .velocityVarName("byline")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Publish")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Story")
                                .velocityVarName("story")
                                .next()
                );

                wikiType = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return wikiType;
    }

    public static ContentType getWidgetLikeContentType() {
        return getWidgetLikeContentType("SimpleWidget" + System.currentTimeMillis(), null);
    }

    public static ContentType getWidgetLikeContentType(final String contentTypeName, final Set<String> workflowIds) {

        ContentType simpleWidgetContentType = null;
        try {
            try {
                simpleWidgetContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (simpleWidgetContentType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("Code")
                                .velocityVarName("code")
                                .required(true)
                                .next()
                );

                simpleWidgetContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.WIDGET)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return simpleWidgetContentType;
    }

    public static ContentType getFormLikeContentType() {
        return getFormLikeContentType("Form" + System.currentTimeMillis(), null);
    }

    public static ContentType getFormLikeContentType(final String contentTypeName, final Set<String> workflowIds) {

        ContentType formContentType = null;
        try {
            try {
                formContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (formContentType == null) {

                formContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.FORM)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return formContentType;
    }

    public static ContentType getFormWithRequiredFieldsLikeContentType() {
            return getFormWithRequiredFieldsLikeContentType("Form" + System.currentTimeMillis(), null);
    }

    public static ContentType getFormWithRequiredFieldsLikeContentType(final String contentTypeName, Set<String> workFlowsId) {

        ContentType formContentType = null;
        try {
            try {
                formContentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (formContentType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("formId")
                                .velocityVarName("formId")
                                .type(TextField.class)
                                .required(true)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("widgetTitle")
                                .velocityVarName("widgetTitle")
                                .type(TextField.class)
                                .required(true)
                                .next()
                );

                formContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.FORM)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workFlowsId)
                        .fields(fields)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return formContentType;
    }

    public static Contentlet getEmptyFormWithRequiredFieldsContent(long languageId) {

        String contentTypeId = getFormWithRequiredFieldsLikeContentType().id();

        try {
            return new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("formId", null)
                    .setProperty("widgetTitle", null).skipValidation(true).nextPersisted();
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getWikiContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getWikiLikeContentType().id();
        }

        ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "wikiContent")
                .setProperty("urlTitle", "wikiContent")
                .setProperty("story", "story")
                .setProperty("sysPublishDate", new Date())
                .setProperty("byline", "byline");

        if (persist) {
            return contentletDataGen.nextPersisted();
        } else {
            return contentletDataGen.next();
        }
    }

    public static Contentlet getGenericContentContent(Boolean persist, long languageId) {
        return getGenericContentContent(persist, languageId, null);
    }

    public static Contentlet getGenericContentContent(Boolean persist, long languageId, Host site) {

        try {

            if (null == site) {
                site = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
            }

            ContentType webPageContentContentType = APILocator
                    .getContentTypeAPI(APILocator.systemUser())
                    .find("webPageContent");

            ContentletDataGen contentletDataGen = new ContentletDataGen(
                    webPageContentContentType.id())
                    .languageId(languageId)
                    .host(site)
                    .setProperty("contentHost", site)
                    .setProperty("title", "genericContent")
                    .setProperty("author", "systemUser")
                    .setProperty("body", "Generic Content Body");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getWidgetContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getWidgetLikeContentType().id();
        }

        try {
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("widgetTitle", "titleContent")
                    .setProperty("code", "Widget code");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getFormContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getFormLikeContentType().id();
        }

        try {
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("formTitle", "title" + System.currentTimeMillis())
                    .setProperty("formEmail", "email@" + System.currentTimeMillis() + ".com")
                    .setProperty("formHost", APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false));

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Relationship relateContentTypes(final ContentType parentContentType,
            final ContentType childContentType) {
        final String relationTypeValue = parentContentType.name() + "-" + childContentType.name();
        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        Relationship relationship;
        relationship = relationshipAPI.byTypeValue(relationTypeValue);
        if (null != relationship) {
            return relationship;
        } else {
            relationship = new Relationship();
            if ((parentContentType == childContentType) || (parentContentType.id().equals(childContentType.id()))) {
                relationship.setParentRelationName("Child " + parentContentType.name());
                relationship.setChildRelationName("Parent " + childContentType.name());
            } else {
                relationship.setParentRelationName(parentContentType.name());
                relationship.setChildRelationName(childContentType.name());
            }
            relationship.setRelationTypeValue(relationTypeValue);
            relationship.setParentStructureInode(parentContentType.inode());
            relationship.setChildStructureInode(childContentType.id());
            try {
                APILocator.getRelationshipAPI().create(relationship);
            } catch (Exception e) {
                throw new DotRuntimeException(e);
            }
        }
        return relationship;
    }

    public static Contentlet getFileAssetContent(Boolean persist, long languageId) {

        try {
            Folder folder = new FolderDataGen().nextPersisted();

            //Test file
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "test_image1" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);

            ContentletDataGen fileAssetDataGen = new FileAssetDataGen(folder, testImage)
                    .languageId(languageId);

            if (persist) {
                return ContentletDataGen.publish(fileAssetDataGen.nextPersisted());
            } else {
                return fileAssetDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId) {
        return getBlogContent(persist, languageId, null, null);
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId,
            String contentTypeId) {
        return getBlogContent(persist, languageId, contentTypeId, null);
    }

    public static Contentlet getBlogContent(Boolean persist, long languageId,
            String contentTypeId, final Host site) {

        if (null == contentTypeId) {
            contentTypeId = getBlogLikeContentType().id();
        }

        ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                .languageId(languageId)
                .setProperty("title", "blogContent")
                .setProperty("urlTitle", "blogContent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody");

        if (null != site) {
            contentletDataGen = contentletDataGen.host(site)
                    .setProperty("hostfolder", site);
        }

        if (persist) {
            return contentletDataGen.nextPersisted();
        } else {
            return contentletDataGen.next();
        }
    }

    public static Contentlet getEmployeeContent(Boolean persist, long languageId,
            String contentTypeId) {
        return getEmployeeContent(persist, languageId,
                contentTypeId, null);
    }

    public static Contentlet getEmployeeContent(Boolean persist, long languageId,
            String contentTypeId, final Host site) {

        if (null == contentTypeId) {
            contentTypeId = getEmployeeLikeContentType().id();
        }

        try {
            final long millis = System.currentTimeMillis();

            //Photo
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testPhoto = new File(Files.createTempDir(),
                    "photo" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testPhoto);

            //Creating the content
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("firstName", "Test Name" + millis)
                    .setProperty("lastName", "Test Last name" + millis)
                    .setProperty("phone", "99999999")
                    .setProperty("mobile", "99999999")
                    .setProperty("fax", "99999999")
                    .setProperty("email", "test@test" + millis + ".com")
                    .setProperty("photo", testPhoto);

            if (null != site) {
                contentletDataGen = contentletDataGen.host(site)
                        .setProperty("hostfolder", site);
            }

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId) {
        return getNewsContent(persist, languageId, contentTypeId, null);
    }

    public static Contentlet getNewsContent(Boolean persist, long languageId,
            String contentTypeId, Host site) {

        if (null == contentTypeId) {
            contentTypeId = getNewsLikeContentType().id();
        }

        try {

            final long millis = System.currentTimeMillis();
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(null != site ? site : APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "newsContent Title" + millis)
                    .setProperty("urlTitle", "news-content-url-title" + millis)
                    .setProperty("byline", "byline")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("story", "newsStory")
                    .setProperty("tags", "test");

            if (null != site) {
                contentletDataGen = contentletDataGen.host(site)
                        .setProperty("hostfolder", site);
            }

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Contentlet getPageContent(Boolean persist, long languageId) {

        return getPageContent(persist,languageId, null);
    }

    public static Contentlet getPageContent(Boolean persist, long languageId, Folder folder) {

        try {
            //Create a container for the given contentlet
            Container container = new ContainerDataGen()
                    .nextPersisted();

            //Create a template
            Template template = new TemplateDataGen().withContainer(container.getIdentifier())
                    .nextPersisted();

            if(null == folder) {
               final Host defaultHost = APILocator.getHostAPI()
                       .findDefaultHost(APILocator.systemUser(), false);
               final User systemUser = APILocator.systemUser();

               //Create the html page
               folder = APILocator.getFolderAPI()
                       .createFolders("/folder" + System.currentTimeMillis() + "/",
                               defaultHost, systemUser, false);
            }
            ContentletDataGen contentletDataGen = new HTMLPageDataGen(folder, template)
                    .languageId(languageId);

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static Language getSpanishLanguage() {

        //Search for the Spanish language, if does not exist we need to create it
        Language spanishLanguage = APILocator.getLanguageAPI().getLanguage("es", "ES");
        if (null == spanishLanguage || spanishLanguage.getId() < 1) {
            spanishLanguage = new LanguageDataGen()
                    .country("Spain")
                    .countryCode("ES")
                    .languageCode("es")
                    .languageName("Spanish").nextPersisted();
        }

        return spanishLanguage;
    }

    public static ContentType getDocumentLikeContentType() {
       return getDocumentLikeContentType("Document" + System.currentTimeMillis(), null);
    }

    public static ContentType getDocumentLikeContentType(final String contentTypeName, Set<String> workflowIds) {

        ContentType simpleWidgetContentType = null;
        try {
            try {
                simpleWidgetContentType = APILocator.getContentTypeAPI(APILocator.systemUser()).find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (simpleWidgetContentType == null) {

                final WorkflowScheme documentWorkflow = TestWorkflowUtils.getDocumentWorkflow();
                final Set<String> collectedWorkflowIds = new HashSet<>();
                collectedWorkflowIds.add(documentWorkflow.getId());
                if(null != workflowIds){
                   collectedWorkflowIds.addAll(workflowIds);
                }

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                fields.add(
                        new FieldDataGen()
                                .name("hostFolder")
                                .velocityVarName("hostFolder")
                                .type(HostFolderField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("fileAsset")
                                .velocityVarName("fileAsset")
                                .type(BinaryField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("fileName")
                                .velocityVarName("fileName")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("description")
                                .velocityVarName("description1")
                                .type(TextAreaField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("PublishDate")
                                .velocityVarName("sysPublishDate")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Tags")
                                .velocityVarName("tags")
                                .defaultValue(null)
                                .type(TagField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Topic")
                                .velocityVarName("topic")
                                .type(CategoryField.class)
                                .next()
                );

                simpleWidgetContentType = new ContentTypeDataGen()
                        .baseContentType(BaseContentType.CONTENT)
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .fields(fields)
                        .workflowId(workflowIds)
                        .nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return simpleWidgetContentType;
    }


    public static Contentlet getDocumentLikeContent(Boolean persist, long languageId,
            String contentTypeId) {

        if (null == contentTypeId) {
            contentTypeId = getDocumentLikeContentType().id();
        }

        try {
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "test_image1" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);

            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .setProperty("title", "document")
                    .setProperty("urlTitle", "blogContent")
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("tags", "test")
                    .setProperty("fileAsset", testImage)
                    .setProperty("topic", "lol");

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static ContentType getBannerLikeContentType() {
        return getBannerLikeContentType("Banner" + System.currentTimeMillis(),
                APILocator.systemHost(), null);
    }


    public static ContentType getBannerLikeContentType(final String contentTypeName, final Host site) {
        return getBannerLikeContentType(contentTypeName, site, null);
    }

    public static ContentType getBannerLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType bannerType = null;
        try {
            try {
                bannerType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (bannerType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Tags")
                                .velocityVarName("tags")
                                .defaultValue(null)
                                .type(TagField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Image")
                                .velocityVarName("image")
                                .type(BinaryField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Caption")
                                .velocityVarName("caption")
                                .type(WysiwygField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("TextColor")
                                .velocityVarName("textColor")
                                .type(SelectField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Layout")
                                .velocityVarName("layout")
                                .type(CustomField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("BackgroundColor")
                                .velocityVarName("BackgroundColor")
                                .type(CustomField.class)
                                .next()
                );

                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                bannerType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return bannerType;
    }

    public static Contentlet getBannerLikeContent(Boolean persist, long languageId,
            String contentTypeId, Host site) {

        if (null == contentTypeId) {
            contentTypeId = getBannerLikeContentType().id();
        }
        try {

            //Photo
            final String testImagePath = "com/dotmarketing/portlets/contentlet/business/test_files/test_image1.jpg";
            final File originalTestImage = new File(
                    ConfigTestHelper.getUrlToTestResource(testImagePath).toURI());
            final File testImage = new File(Files.createTempDir(),
                    "image" + System.currentTimeMillis() + ".jpg");
            FileUtil.copyFile(originalTestImage, testImage);


            final long millis = System.currentTimeMillis();
            ContentletDataGen contentletDataGen = new ContentletDataGen(contentTypeId)
                    .languageId(languageId)
                    .host(null != site ? site : APILocator.getHostAPI()
                            .findDefaultHost(APILocator.systemUser(), false))
                    .setProperty("title", "banner like Title" + millis)
                    .setProperty("sysPublishDate", new Date())
                    .setProperty("layout", "")
                    .setProperty("textColor", "")
                    .setProperty("image", testImage)
                    .setProperty("tags", "test");

            if (null != site) {
                contentletDataGen = contentletDataGen.host(site)
                        .setProperty("hostfolder", site);
            }

            if (persist) {
                return contentletDataGen.nextPersisted();
            } else {
                return contentletDataGen.next();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public static ContentType getProductLikeContentType(){
        return  getProductLikeContentType("Product" + System.currentTimeMillis(), APILocator.systemHost(),null);
    }

    public static ContentType getProductLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType productType = null;
        try {
            try {
                productType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (productType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Site or Folder")
                                    .velocityVarName("hostfolder")
                                    .required(Boolean.TRUE)
                                    .type(HostFolderField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Title")
                                .velocityVarName("title")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("urlTitle")
                                .velocityVarName("urlTitle")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Type")
                                .velocityVarName("type")
                                .type(RadioField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Inception")
                                .velocityVarName("inception")
                                .defaultValue(null)
                                .type(DateField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Description")
                                .velocityVarName("description")
                                .type(LineDividerField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Expense Ration")
                                .velocityVarName("expenseRatio")
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Risk")
                                .velocityVarName("risk")
                                .type(SelectField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Asset Class")
                                .velocityVarName("assetClass")
                                .type(CustomField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Market Cap")
                                .velocityVarName("marketCap")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Style")
                                .velocityVarName("style")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Quality")
                                .velocityVarName("quality")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Maturity")
                                .velocityVarName("maturity")
                                .type(SelectField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Summary")
                                .velocityVarName("summary")
                                .type(WysiwygField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Tags")
                                .velocityVarName("tags")
                                .defaultValue(null)
                                .type(TagField.class)
                                .next()
                );
                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                productType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return productType;
    }


    public static ContentType getYoutubeLikeContentType(){
        return  getYoutubeLikeContentType("Youtube" + System.currentTimeMillis(), APILocator.systemHost(),null);
    }

    public static ContentType getYoutubeLikeContentType(final String contentTypeName,
            final Host site,
            final Set<String> workflowIds) {

        ContentType youtubeType = null;
        try {
            try {
                youtubeType = APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(contentTypeName);
            } catch (NotFoundInDbException e) {
                //Do nothing...
            }
            if (youtubeType == null) {

                List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
                if (null != site) {
                    fields.add(
                            new FieldDataGen()
                                    .name("Widget Usage")
                                    .velocityVarName("widgetUsage")
                                    .type(ConstantField.class)
                                    .next()
                    );
                }
                fields.add(
                        new FieldDataGen()
                                .name("Widget Code")
                                .velocityVarName("widgetCode")
                                .type(ConstantField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Widget Pre-Executed")
                                .velocityVarName("widgetPreExecuted")
                                .type(ConstantField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Search")
                                .velocityVarName("search")
                                .type(CustomField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("WidgetTitle")
                                .velocityVarName("widgetTitle")
                                .defaultValue(null)
                                .required(true)
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Author")
                                .velocityVarName("author")
                                .defaultValue(null)
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Length")
                                .velocityVarName("length")
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Thumbnail Small")
                                .velocityVarName("thumbnail")
                                .type(TextField.class)
                                .next()
                );

                fields.add(
                        new FieldDataGen()
                                .name("Thumbnail Large")
                                .velocityVarName("thumbnail2")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("Published")
                                .velocityVarName("published")
                                .type(TextField.class)
                                .next()
                );
                fields.add(
                        new FieldDataGen()
                                .name("URL")
                                .velocityVarName("url")
                                .type(TextField.class)
                                .next()
                );

/*
                fields.add(
                        new FieldDataGen()
                                .name("Products-Youtube")
                                .velocityVarName("productsYoutube")
                                .defaultValue(null)
                                .type(RelationshipField.class)
                                .values(
                                    String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal())
                                 )
                                .relationType(contentTypeName + StringPool.PERIOD + "products")
                                .next()
                );
*/

                ContentTypeDataGen contentTypeDataGen = new ContentTypeDataGen()
                        .name(contentTypeName)
                        .velocityVarName(contentTypeName)
                        .workflowId(workflowIds)
                        .fields(fields);

                if (null != site) {
                    contentTypeDataGen.host(site);
                }

                youtubeType = contentTypeDataGen.nextPersisted();
            }
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

        return youtubeType;
    }



}