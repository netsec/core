package com.dotmarketing.portlets.contentlet.business;

import static com.dotcms.util.CollectionsUtils.map;
import static com.dotmarketing.business.APILocator.getContentTypeFieldAPI;
import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.api.system.event.ContentletSystemEventUtil;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.content.business.DotMappingException;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RadioField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.mock.request.MockInternalRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.services.VelocityType;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.uuid.shorty.ShortyId;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotcms.uuid.shorty.ShortyIdCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.AssetUtil;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapterImpl;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Jonathan Gamba.
 * Date: 3/20/12
 * Time: 12:12 PM
 */

@RunWith(DataProviderRunner.class)
public class ContentletAPITest extends ContentletBaseTest {

    /**
     * Testing {@link ContentletAPI#findAllContent(int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Ignore ( "Not Ready to Run." )
    @Test
    public void findAllContent () throws DotDataException, DotSecurityException {

        //Getting all contentlets live/working contentlets
        List<Contentlet> contentlets = contentletAPI.findAllContent( 0, 5 );

        //Validations
        assertTrue( contentlets != null && !contentlets.isEmpty() );
        assertEquals( contentlets.size(), 5 );

        //Validate the integrity of the array
        Contentlet contentlet = contentletAPI.find( contentlets.iterator().next().getInode(), user, false );

        //Validations
        assertTrue( contentlet != null && ( contentlet.getInode() != null && !contentlet.getInode().isEmpty() ) );
    }

    @Test
    public void test_invalidate_shorty_cache () throws DotDataException, DotSecurityException {

        Contentlet contentletToDestroy = null;
        ContentType type = null;
        try {
            final String velocityContentTypeName = "InvalidateShortyCacheContentType";
            type = contentTypeAPI.save(
                    ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                            .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("InvalidateShortyCache").owner(APILocator.systemUser().toString())
                            .variable(velocityContentTypeName).build());

            final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(type.fields());

            fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
            fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                    .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

            contentTypeAPI.save(type, fields);

            final Contentlet contentlet = new Contentlet();
            final User user = APILocator.systemUser();
            contentlet.setContentTypeId(type.id());
            contentlet.setOwner(APILocator.systemUser().toString());
            contentlet.setModDate(new Date());
            contentlet.setLanguageId(1);
            contentlet.setStringProperty("title", "Test Save");
            contentlet.setStringProperty("txt", "Test Save Text");
            contentlet.setHost(Host.SYSTEM_HOST);
            contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            // first save
            final ShortyIdAPI shortyIdAPI = APILocator.getShortyAPI();
            final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
            contentletToDestroy = contentlet1;
            final Optional<ShortyId> shortyId = shortyIdAPI.getShorty(contentlet1.getIdentifier());
            Assert.assertTrue(shortyId.isPresent());
            Assert.assertTrue(new ShortyIdCache().get(shortyId.get().shortId).isPresent());
            final Contentlet contentletCheckout = contentletAPI.checkout(contentlet1.getInode(), user, false);
            final String inode = contentletCheckout.getInode();
            this.contentletAPI.copyProperties(contentletCheckout, contentlet.getMap());
            contentletCheckout.setIdentifier(contentlet1.getIdentifier());
            contentletCheckout.setInode(inode);
            contentletAPI.checkin(contentletCheckout, user, false);
            Assert.assertFalse(new ShortyIdCache().get(shortyId.get().shortId).isPresent());
        } finally {


            if (null != contentletToDestroy) {
                try {
                    this.contentletAPI.destroy(contentletToDestroy, user, false);
                } catch (Exception e) {
                    // quiet
                }
            }

            if (null != type) {
                try {
                    contentTypeAPI.delete(type);
                } catch (Exception e) {
                    // quiet
                }
            }
        }
    }

    @Ignore ( "Not Ready to Run." )
    @Test
    public void run_listener_after_save_result_called_all_listeners () throws DotDataException, DotSecurityException {

        ContentType typeResult = null;
        final int        numberOfContents    = 1000;//20000;
        final CountDownLatch countDownLatch  = new CountDownLatch(numberOfContents);
        DotSubmitter dotSubmitter = DotConcurrentFactory.getInstance().getSubmitter(DotConcurrentFactory.DOT_SYSTEM_THREAD_POOL);
        try {

            final long       languageId          = APILocator.getLanguageAPI().getDefaultLanguage().getId();
            final String velocityContentTypeName = "RunListenerAfterSaveTest8";


            LocalTransaction.wrap(() -> {

                final ContentType type = contentTypeAPI.save(
                        ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                                .name("RunListenerAfterSaveTest").owner(APILocator.systemUser().toString())
                                .variable(velocityContentTypeName).build());

                final List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(type.fields());

                fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                        .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
                fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                        .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

                contentTypeAPI.save(type, fields);
            });

            final ContentType type = contentTypeAPI.find(velocityContentTypeName);
            final List<Future> futures = new ArrayList<>();

            for (int i = 0; i < numberOfContents; ++i) {

                futures.add(dotSubmitter.submit(() -> {

                    try {
                        List<Contentlet> contentlets =
                                APILocator.getContentletAPI().search("+type:" + velocityContentTypeName, 1000, 0, null, APILocator.systemUser(), false);
                        if (UtilMethods.isSet(contentlets)) {

                            for (final Contentlet contentlet1 : contentlets) {
                                APILocator.getContentletAPI().find(contentlet1.getInode(), APILocator.systemUser(), false);
                            }
                        }
                    } catch (DotDataException  | DotSecurityException e) {
                        e.printStackTrace();
                    }
                }));

                LocalTransaction.wrapReturnWithListeners(() -> {


                    final Contentlet contentlet = new Contentlet();
                    final User user = APILocator.systemUser();
                    contentlet.setContentTypeId(type.id());
                    contentlet.setOwner(APILocator.systemUser().toString());
                    contentlet.setModDate(new Date());
                    contentlet.setLanguageId(languageId);
                    contentlet.setStringProperty("title", "Test Save");
                    contentlet.setStringProperty("txt", "Test Save Text");
                    contentlet.setHost(Host.SYSTEM_HOST);
                    contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
                    contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);

                    // first save
                    final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
                    if (null != contentlet1) {
                        HibernateUtil.addCommitListener(() -> {

                            try {
                                assertTrue(APILocator.getContentletAPI().indexCount("+inode:" + contentlet1.getInode(), user, false) > 0);
                            } catch (DotDataException | DotSecurityException e) {
                                fail(e.getMessage());
                            }

                            if (APILocator.getContentletAPI().isInodeIndexed(contentlet1.getInode())) {
                                ContentletSystemEventUtil.getInstance().pushSaveEvent(contentlet1, true);
                            }

                            countDownLatch.countDown();
                        }, 1000);
                    }

                    return null;
                });
            }

            for (final Future future: futures) {

                future.get();
                countDownLatch.await(1, TimeUnit.SECONDS);
            }

            if (countDownLatch.getCount() > 0) {

                countDownLatch.await(30, TimeUnit.SECONDS);
            }

            assertEquals(0 , countDownLatch.getCount());

            typeResult = type;
        } catch (Exception e) {

            fail(e.getMessage());
        } finally {

            if (null != typeResult) {

                ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                contentTypeAPI.delete(typeResult);
            }
        }

    }


    /**
     * Testing {@link ContentletAPI#find(String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void find () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        Contentlet foundContentlet = contentletAPI.find( contentlet.getInode(), user, false );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentlet.getInode() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void test_findContentletByIdentifierOrFallback_non_existing_DotContentletStateException_expected () throws DotDataException, DotSecurityException {


        assertFalse(contentletAPI.findContentletByIdentifierOrFallback("noexisting", false, 1, user, false).isPresent());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_DotContentletStateException_expected_true () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWidget = TestDataUtils
                .getWidgetContent(true, languageAPI.getDefaultLanguage().getId(),
                        simpleWidgetContentType.id());
        assertNotNull(contentletWidget);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(contentletWidget.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(languageAPI.getDefaultLanguage().getId(),
                optionalContentlet.get().getLanguageId());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * English version, no Spanish, fallback true, request Spanish -> Return english
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_English_version_no_Spanish_fallback_false_expecting_false () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet genericContent = TestDataUtils.getGenericContentContent(true, 1);

        assertNotNull(genericContent);
        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(genericContent.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertFalse(optionalContentlet.isPresent());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * English version, with Spanish, fallback true, request Spanish -> Return Spanish
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishSpanish_fallback_true_expecting_Spanish_true () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWidget = TestDataUtils.getPageContent(true, spanishLanguage.getId());
        assertNotNull(contentletWidget);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(contentletWidget.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(contentletWidget.getInode(), optionalContentlet.get().getInode());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * English version, no Spanish, fallback false, request Spanish -> 404
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_English_No_Spanish_fallback_false_expecting_False () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet newsContentlet = TestDataUtils
                .getNewsContent(true, languageAPI.getDefaultLanguage().getId(),
                        newsContentType.id());

        assertNotNull(newsContentlet);
        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(newsContentlet.getIdentifier(),
                                false,
                                spanishLanguage.getId(), user, false);

        assertFalse(optionalContentlet.isPresent());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * English version, with Spanish, fallback false, request Spanish -> Return Spanish
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishSpanish_fallback_false_expecting_Spanish_true () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet newsContentlet = TestDataUtils
                .getNewsContent(true, languageAPI.getDefaultLanguage().getId(),
                        newsContentType.id());
        assertNotNull(newsContentlet);

        final Contentlet checkoutContentlet =
                APILocator.getContentletAPI()
                        .checkout(newsContentlet.getInode(), user, false);
        assertNotNull(checkoutContentlet);
        checkoutContentlet.setIdentifier(newsContentlet.getIdentifier());
        checkoutContentlet.setLanguageId(spanishLanguage.getId()); // spanish
        checkoutContentlet.setIndexPolicy(IndexPolicy.FORCE);

        Contentlet spanishNewsContentlet =
                APILocator.getContentletAPI()
                        .checkin(checkoutContentlet, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI()
                        .findContentletByIdentifierOrFallback(newsContentlet.getIdentifier(),
                                false, spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishNewsContentlet.getInode(), optionalContentlet.get().getInode());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long,
     * User, boolean)}
     *
     * No English version, with Spanish, fallback true, request Spanish -> Return Spanish
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_NonEnglish_WithSpanish_fallback_true_expecting_Spanish_true() {

        //Getting our test contentlet
        Contentlet spanishGenericContentContentlet = TestDataUtils.getFileAssetContent(true,
                spanishLanguage.getId());

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        spanishGenericContentContentlet.getIdentifier(), false,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishGenericContentContentlet.getInode(),
                optionalContentlet.get().getInode());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * No English version, with Spanish, fallback false, request Spanish -> Return Spanish
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_NonEnglish_WithSpanish_fallback_false_expecting_Spanish_true () throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet spanishGenericContentContentlet = TestDataUtils.getGenericContentContent(true,
                spanishLanguage.getId());

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        spanishGenericContentContentlet.getIdentifier(), false,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishGenericContentContentlet.getInode(),
                optionalContentlet.get().getInode());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * English version LIVE, with Spanish WORKING, fallback true, request Spanish WORKING -> Return Spanish WORKING
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishLive_WithSpanishWorking_fallback_true_expecting_SpanishWorking_true () throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet fileAssetContentletEngLive = TestDataUtils.getFileAssetContent(true,
                languageAPI.getDefaultLanguage().getId());

        assertNotNull(fileAssetContentletEngLive);
        Contentlet spanishFileAssetContentletWorking = new Contentlet(); // no eng version
        APILocator.getContentletAPI().copyProperties(spanishFileAssetContentletWorking,
                fileAssetContentletEngLive.getMap());
        spanishFileAssetContentletWorking.setHost(fileAssetContentletEngLive.getHost());
        spanishFileAssetContentletWorking
                .setContentType(fileAssetContentletEngLive.getContentType());
        spanishFileAssetContentletWorking.setIdentifier(fileAssetContentletEngLive.getIdentifier());
        spanishFileAssetContentletWorking.setInode(null);
        spanishFileAssetContentletWorking.setIndexPolicy(IndexPolicy.FORCE);
        spanishFileAssetContentletWorking.setLanguageId(spanishLanguage.getId()); // spanish
        spanishFileAssetContentletWorking.setProperty("title", "Spanish Main.scss");

        final File file = fileAssetContentletEngLive.getBinary(FileAssetAPI.BINARY_FIELD);
        final File copy = new File(org.apache.commons.io.FileUtils.getTempDirectory(),
                UUIDGenerator.generateUuid());
        org.apache.commons.io.FileUtils.copyFile(file, copy);
        spanishFileAssetContentletWorking.setBinary(FileAssetAPI.BINARY_FIELD, copy);

        spanishFileAssetContentletWorking =
                APILocator.getContentletAPI()
                        .checkin(spanishFileAssetContentletWorking, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        fileAssetContentletEngLive.getIdentifier(), false,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertEquals(spanishFileAssetContentletWorking.getInode(),
                optionalContentlet.get().getInode());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * English version LIVE, with Spanish WORKING, fallback true, request Spanish LIVE -> Return English
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_EnglishLive_WithSpanishWorking_fallback_true_expecting_English_true () throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet fileAssetContentletEngLive = TestDataUtils.getFileAssetContent(true,
                languageAPI.getDefaultLanguage().getId());

        assertNotNull(fileAssetContentletEngLive);
        Contentlet spanishFileAssetContentletWorking = new Contentlet(); // no eng version
        APILocator.getContentletAPI().copyProperties(spanishFileAssetContentletWorking,
                fileAssetContentletEngLive.getMap());
        spanishFileAssetContentletWorking.setIdentifier(fileAssetContentletEngLive.getIdentifier());
        spanishFileAssetContentletWorking.setHost(fileAssetContentletEngLive.getHost());
        spanishFileAssetContentletWorking
                .setContentType(fileAssetContentletEngLive.getContentType());
        spanishFileAssetContentletWorking.setInode(null);
        spanishFileAssetContentletWorking.setIndexPolicy(IndexPolicy.FORCE);
        spanishFileAssetContentletWorking.setLanguageId(spanishLanguage.getId()); // spanish
        spanishFileAssetContentletWorking.setProperty("title", "Spanish Main.scss");

        final File file = fileAssetContentletEngLive.getBinary(FileAssetAPI.BINARY_FIELD);
        final File copy = new File(org.apache.commons.io.FileUtils.getTempDirectory(),
                UUIDGenerator.generateUuid());
        org.apache.commons.io.FileUtils.copyFile(file, copy);
        spanishFileAssetContentletWorking.setBinary(FileAssetAPI.BINARY_FIELD, copy);

        spanishFileAssetContentletWorking =
                APILocator.getContentletAPI()
                        .checkin(spanishFileAssetContentletWorking, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        fileAssetContentletEngLive.getIdentifier(), true,
                        spanishLanguage.getId(), user, false);

        assertTrue(optionalContentlet.isPresent());
        assertNotEquals(spanishFileAssetContentletWorking.getInode(),
                optionalContentlet.get().getInode());
        assertEquals(fileAssetContentletEngLive.getInode(), optionalContentlet.get().getInode());
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifierOrFallback(String, boolean, long, User, boolean)}
     *
     * Only Spanish WORKING, fallback false, request English Working -> Return Empty
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test()
    public void test_findContentletByIdentifierOrFallback_existing_OnlySpanishWorking_fallback_true_expecting_English_true () throws DotDataException, DotSecurityException, IOException {

        //Getting our test contentlet
        Contentlet newsContentletEng = TestDataUtils
                .getNewsContent(true, languageAPI.getDefaultLanguage().getId(),
                        newsContentType.id());

        assertNotNull(newsContentletEng);
        Contentlet spanishNewNewsContentlet = new Contentlet(); // no eng version
        APILocator.getContentletAPI()
                .copyProperties(spanishNewNewsContentlet, newsContentletEng.getMap());
        spanishNewNewsContentlet.setHost(newsContentletEng.getHost());
        spanishNewNewsContentlet.setContentType(newsContentletEng.getContentType());
        spanishNewNewsContentlet.setIdentifier(null);
        spanishNewNewsContentlet.setInode(null);
        spanishNewNewsContentlet.setIndexPolicy(IndexPolicy.FORCE);
        spanishNewNewsContentlet.setLanguageId(spanishLanguage.getId()); // spanish
        spanishNewNewsContentlet.setProperty("title", "Spanish Test2");
        spanishNewNewsContentlet.setProperty("urlTitle", "/news/spanish_test2");
        spanishNewNewsContentlet
                .setProperty("byline", newsContentletEng.getStringProperty("byline"));
        spanishNewNewsContentlet
                .setProperty("sysPublishDate", newsContentletEng.getMap().get("sysPublishDate"));
        spanishNewNewsContentlet.setProperty("story", newsContentletEng.getMap().get("story"));

        spanishNewNewsContentlet =
                APILocator.getContentletAPI()
                        .checkin(spanishNewNewsContentlet, user, false);

        final Optional<Contentlet> optionalContentlet =
                APILocator.getContentletAPI().findContentletByIdentifierOrFallback(
                        spanishNewNewsContentlet.getIdentifier(), false, 1, user, false);

        assertFalse(optionalContentlet.isPresent());
    }

    /**
     * Testing {@link ContentletAPI#findContentletForLanguage(long, com.dotmarketing.beans.Identifier)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletForLanguage () throws DotDataException, DotSecurityException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for ( Contentlet contentlet : contentlets ) {

            //Verify if we have a contentlet with a language set
            if ( contentlet.getLanguageId() != 0 ) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( contentletWithLanguage );

        //Search the contentlet
        assertNotNull( contentletWithLanguage );
        Contentlet foundContentlet = contentletAPI.findContentletForLanguage( contentletWithLanguage.getLanguageId(), contentletIdentifier );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentletWithLanguage.getInode() );
    }

    /**
     * Testing {@link ContentletAPI#findByStructure(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, boolean, int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findByStructure () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlet
        List<Contentlet> foundContentlets = contentletAPI.findByStructure( contentlet.getStructure(), user, false, 0, 0 );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findByStructure(String, com.liferay.portal.model.User, boolean, int, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findByStructureInode () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findByStructure( contentlet.getStructureInode(), user, false, 0, 0 );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletByIdentifier(String, boolean, long, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletByIdentifier () throws DotSecurityException, DotDataException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for ( Contentlet contentlet : contentlets ) {

            //Verify if we have a contentlet with a language set
            if ( contentlet.getLanguageId() != 0 ) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        //Search the contentlet
        assertNotNull( contentletWithLanguage );
        Contentlet foundContentlet = contentletAPI.findContentletByIdentifier( contentletWithLanguage.getIdentifier(), false, contentletWithLanguage.getLanguageId(), user, false );

        //Validations
        assertNotNull( foundContentlet );
        assertEquals( foundContentlet.getInode(), contentletWithLanguage.getInode() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletsByIdentifiers(String[], boolean, long, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletByIdentifiers () throws DotSecurityException, DotDataException {

        //Getting our test contentlet
        Contentlet contentletWithLanguage = null;
        for ( Contentlet contentlet : contentlets ) {

            //Verify if we have a contentlet with a language set
            if ( contentlet.getLanguageId() != 0 ) {
                contentletWithLanguage = contentlet;
                break;
            }
        }

        //Search the contentlet
        assertNotNull( contentletWithLanguage );
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByIdentifiers( new String[]{ contentletWithLanguage.getIdentifier() }, false, contentletWithLanguage.getLanguageId(), user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findContentlets(java.util.List)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentlets () throws DotSecurityException, DotDataException {

        //Getting our test inodes
        List<String> inodes = new ArrayList<>();
        for ( Contentlet contentlet : contentlets ) {
            inodes.add( contentlet.getInode() );
        }

        //Search for the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentlets( inodes );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
        assertEquals( foundContentlets.size(), contentlets.size() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletsByFolder(com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletsByFolder () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByFolder( folder, user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#findContentletsByHost(com.dotmarketing.beans.Host, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findContentletsByHost () throws DotDataException, DotSecurityException {

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.findContentletsByHost( defaultHost, user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentlet () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, user, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.getHost(), contentlet.getHost() );

        contentletAPI.archive(copyContentlet, user, false);
        contentletAPI.delete(copyContentlet, user, false);
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithFolder () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, folder, user, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals(copyContentlet.getStructureInode(), contentlet.getStructureInode());
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.get("junitTestWysiwyg"), contentlet.get("junitTestWysiwyg") );

        contentletAPI.archive(copyContentlet, user, false);
        contentletAPI.delete(copyContentlet, user, false);
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.beans.Host, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithHost () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, defaultHost, user, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );
        assertEquals( copyContentlet.getHost(), contentlet.getHost() );

        contentletAPI.archive( copyContentlet, user, false );
        contentletAPI.delete( copyContentlet, user, false );
    }

    /**
     * Testing {@link ContentletAPI#copyContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.folders.model.Folder, com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void copyContentletWithFolderAppendCopy () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Getting the folder of the test contentlet
        Folder folder = APILocator.getFolderAPI().find( contentlet.getFolder(), user, false );

        //Copy the test contentlet
        Contentlet copyContentlet = contentletAPI.copyContentlet( contentlet, folder, user, true, false );

        //validations
        assertTrue( copyContentlet != null && !copyContentlet.getInode().isEmpty() );
        assertEquals( copyContentlet.getStructureInode(), contentlet.getStructureInode() );
        assertEquals( copyContentlet.getFolder(), contentlet.getFolder() );
        assertEquals( copyContentlet.get( "junitTestWysiwyg" ), contentlet.get( "junitTestWysiwyg" ) );

        contentletAPI.archive( copyContentlet, user, false );
        contentletAPI.delete( copyContentlet, user, false );
    }
    
    @Test
    public void copyContentletWithSeveralVersionsOrderIssue() throws Exception {
        long defLang=APILocator.getLanguageAPI().getDefaultLanguage().getId();
        
        Host host1=new Host();
        host1.setHostname("copy.contentlet.t1_"+System.currentTimeMillis());
        host1.setDefault(false);
        host1.setLanguageId(defLang);
        host1.setIndexPolicy(IndexPolicy.FORCE);
        host1 = APILocator.getHostAPI().save(host1, user, false);

        Host host2=new Host();
        host2.setHostname("copy.contentlet.t2_"+System.currentTimeMillis());
        host2.setDefault(false);
        host2.setLanguageId(defLang);
        host2.setIndexPolicy(IndexPolicy.FORCE);
        host2 = APILocator.getHostAPI().save(host2, user, false);

        
        java.io.File bin=new java.io.File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary() + separator
                + UUIDGenerator.generateUuid() + separator + "hello.txt");
        bin.getParentFile().mkdirs();
        
        bin.createNewFile();
        FileUtils.writeStringToFile(bin, "this is the content of the file");
        
        Contentlet file=new Contentlet();
        file.setHost(host1.getIdentifier());
        file.setFolder("SYSTEM_FOLDER");
        file.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset").getInode());
        file.setLanguageId(defLang);
        file.setStringProperty(FileAssetAPI.TITLE_FIELD,"test copy");
        file.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "hello.txt");
        file.setBinary(FileAssetAPI.BINARY_FIELD, bin);
        file.setIndexPolicy(IndexPolicy.FORCE);
        file = contentletAPI.checkin(file, user, false);
        final String ident = file.getIdentifier();
        
        // create 20 versions
        for(int i=1; i<=20; i++) {
            file = contentletAPI.findContentletByIdentifier(ident, false, defLang, user, false);
            APILocator.getFileAssetAPI().renameFile(file, "hello"+i, user, false);
            contentletAPI.isInodeIndexed(APILocator.getVersionableAPI().getContentletVersionInfo(ident, defLang).getWorkingInode());
        }
        
        file = contentletAPI.findContentletByIdentifier(ident, false, defLang, user, false);

        // the issue https://github.com/dotCMS/dotCMS/issues/5007 is caused by an order issue
        // when we call copy it saves all the versions in the new location. but it should
        // do it in older-to-newer order. because the last save will be the asset_name in the
        // identifier and the data that will have the "working" (or live) version.
        Contentlet copy = contentletAPI.copyContentlet(file, host1, user, false);
        Identifier copyIdent = APILocator.getIdentifierAPI().find(copy);
        
        copy = contentletAPI.findContentletByIdentifier(copyIdent.getId(), false, defLang, user, false);

        assertEquals("hello20_copy.txt",copyIdent.getAssetName());
        assertEquals("hello20_copy.txt",copy.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        //FileAssetAPI.renameFile no longer renames the binary - so skipping assert
        //assertEquals("hello20_copy.txt",copy.getBinary(FileAssetAPI.BINARY_FIELD).getName());
        assertEquals("this is the content of the file", FileUtils.readFileToString(copy.getBinary(FileAssetAPI.BINARY_FIELD)));

        Contentlet copy2 = contentletAPI.copyContentlet(file, host2, user, false);
        Identifier copyIdent2 = APILocator.getIdentifierAPI().find(copy2);

        assertEquals("hello20.txt",copyIdent2.getAssetName());
        assertEquals("hello20.txt",copy2.getStringProperty(FileAssetAPI.FILE_NAME_FIELD));
        //FileAssetAPI.renameFile no longer renames the binary - so skipping assert
        //assertEquals("hello20.txt",copy2.getBinary(FileAssetAPI.BINARY_FIELD).getName());
        assertEquals("this is the content of the file", FileUtils.readFileToString(copy2.getBinary(FileAssetAPI.BINARY_FIELD)));

        contentletAPI.archive(file,user,false);
        contentletAPI.delete(file,user,false);
        contentletAPI.archive(copy,user,false);
        contentletAPI.delete(copy,user,false);
        contentletAPI.archive(copy2,user,false);
        contentletAPI.delete(copy2,user,false);
        contentletAPI.archive(host1,user,false);
        contentletAPI.delete(host1,user,false);
        contentletAPI.archive(host2,user,false);
        contentletAPI.delete(host2,user,false);
        
    }

    /**
     * Testing {@link ContentletAPI#search(String, int, int, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void search () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery = "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.search( luceneQuery, 1000, -1, "inode", user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#search(String, int, int, String, com.liferay.portal.model.User, boolean, int)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchWithPermissions () throws DotSecurityException, DotDataException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery = "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<Contentlet> foundContentlets = contentletAPI.search( luceneQuery, 1000, -1, "inode", user, false, PermissionAPI.PERMISSION_READ );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#searchIndex(String, int, int, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws com.dotmarketing.exception.DotDataException
     *
     * @throws com.dotmarketing.exception.DotSecurityException
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void searchIndex () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Create the lucene query
        String luceneQuery = "+structureinode:" + contentlet.getStructureInode() + " +deleted:false";

        //Search the contentlets
        List<ContentletSearch> foundContentlets = contentletAPI.searchIndex( luceneQuery, 1000, -1, "inode", user, false );

        //Validations
        assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#publishRelatedHtmlPages(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotCacheException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publishRelatedHtmlPages () throws DotDataException, DotSecurityException, DotCacheException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Making it live
        APILocator.getVersionableAPI().setLive( contentlet );

        //Publish html pages for this contentlet
        contentletAPI.publishRelatedHtmlPages( contentlet );

        //TODO: How to validate this???, good question, basically checking that the html page is not in cache basically the method publishRelatedHtmlPages(...) will just remove the htmlPage from cache

        //Get the contentlet Identifier to gather the related pages
        Identifier identifier = APILocator.getIdentifierAPI().find( contentlet );
        //Get the identifier's number of the related pages
        List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTreesByChild( identifier.getId() );
        for ( MultiTree multitree : multiTrees ) {
            //Get the Identifiers of the related pages
            Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find( multitree.getParent1() );

            //OK..., lets try to find this page in the cache...
            HTMLPageAsset foundPage = ( HTMLPageAsset ) CacheLocator.getCacheAdministrator().get( "HTMLPageCache" + identifier, "HTMLPageCache" );

            //Validations
            assertTrue( foundPage == null || ( foundPage.getInode() == null || foundPage.getInode().equals( "" ) ) );
        }
    }

    /**
     * Testing {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     * with a binary field
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanBinaryField() throws DotDataException, DotSecurityException {
        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure(structure, user, false, 0, 0);
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a known binary field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....

        Field foundBinaryField = FieldFactory.getFieldByVariableName(structure.getInode(), "junitTestBinary" + identifier);


        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue(contentlet, foundBinaryField);

        //Validations
        assertNotNull(value);
        assertTrue(((java.io.File) value).exists());

        //Cleaning the binary field
        contentletAPI.cleanField(structure, foundBinaryField, user, false);

        //Validations
        assertFalse(((java.io.File) value).exists());
    }

    /**
     * Testing {@link ContentletAPI#cleanField(com.dotmarketing.portlets.structure.model.Structure, com.dotmarketing.portlets.structure.model.Field, com.liferay.portal.model.User, boolean)}
     * with a tag field
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanTagField() throws DotDataException, DotSecurityException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
        Contentlet contentlet = contentletList.iterator().next();

        //Getting a known tag field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundTagField = FieldFactory.getFieldByVariableName( structure.getInode(), "junitTestTag" + identifier );

        //Getting the current value for this field
        List<Tag> value = tagAPI.getTagsByInodeAndFieldVarName(contentlet.getInode(), foundTagField.getVelocityVarName());

        //Validations
        assertNotNull( value );
        assertFalse( value.isEmpty() );

        //Cleaning the tag field
        contentletAPI.cleanField( structure, foundTagField, user, false );

        //Getting the current value for this field
        List<Tag> value2 = tagAPI.getTagsByInodeAndFieldVarName(contentlet.getInode(), foundTagField.getVelocityVarName());

        //Validations
        assertTrue( value2.isEmpty() );        
    }

    /**
     * Testing {@link ContentletAPI#cleanHostField(com.dotmarketing.portlets.structure.model.Structure, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotMappingException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void cleanHostField () throws DotDataException, DotSecurityException, DotMappingException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Check the current identifies
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( contentletList.iterator().next() );

        //Cleaning the host field for the identifier
        contentletAPI.cleanHostField( structure, user, false );

        //Now get again the identifier to see if the change was made
        Identifier changedContentletIdentifier = APILocator.getIdentifierAPI().find( contentletList.iterator().next() );

        //Validations
        assertNotNull( changedContentletIdentifier );
        assertNotSame( contentletIdentifier, changedContentletIdentifier );
        assertNotSame( contentletIdentifier.getHostId(), changedContentletIdentifier.getHostId() );
    }

    /**
     * Testing {@link ContentletAPI#getNextReview(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getNextReview () throws DotSecurityException, DotDataException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Search the contentlet for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Getting the next review date
        Date nextReview = contentletAPI.getNextReview( contentletList.iterator().next(), user, false );

        //Validations
        assertNotNull( nextReview );
    }

    /**
     * Tests method {@link ContentletAPI#getContentletReferences(Contentlet, User, boolean)}.
     * <p>
     * Checks that expected containers and pages (in the correct language) are returned by the method.
     */

    @Test
    public void getContentletReferences() throws Exception {
        int english = 1;
        long spanish = spanishLanguage.getId();

        try {
            HibernateUtil.startTransaction();
            final String UUID = UUIDGenerator.generateUuid();
            Structure structure = new StructureDataGen().nextPersisted();
            Container container = new ContainerDataGen().withStructure(structure, "").nextPersisted();
            Template template = new TemplateDataGen().withContainer(container.getIdentifier(),UUID).nextPersisted();
            Folder folder = new FolderDataGen().nextPersisted();

            HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, template);
            HTMLPageAsset englishPage = htmlPageDataGen.languageId(english).nextPersisted();
            HTMLPageAsset spanishPage = htmlPageDataGen.pageURL(englishPage.getPageUrl() + "SP").languageId(spanish)
                .nextPersisted();

            ContentletDataGen contentletDataGen = new ContentletDataGen(structure.getInode());
            Contentlet contentInEnglish = contentletDataGen.languageId(english).nextPersisted();
            Contentlet contentInSpanish = contentletDataGen.languageId(spanish).nextPersisted();

            // let's add the content to the page in english (create the page-container-content relationship)
            MultiTree multiTreeEN = new MultiTree(englishPage.getIdentifier(), container.getIdentifier(),
                contentInEnglish.getIdentifier(),UUID,0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTreeEN);

            // let's add the content to the page in spanish (create the page-container-content relationship)
            MultiTree multiTreeSP = new MultiTree(spanishPage.getIdentifier(), container.getIdentifier(),
                contentInSpanish.getIdentifier(),UUID,0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTreeSP);

            // let's get the references for english content
            List<Map<String, Object>> references = contentletAPI.getContentletReferences(contentInEnglish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), english);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(), container.getInode());

            // let's get the references for spanish content
            references = contentletAPI.getContentletReferences(contentInSpanish, user, false);

            assertNotNull(references);
            assertTrue(!references.isEmpty());
            // let's check if the referenced page is in the expected language
            assertEquals(((IHTMLPage) references.get(0).get("page")).getLanguageId(), spanish);
            // let's check the referenced container is the expected
            assertEquals(((Container) references.get(0).get("container")).getInode(), container.getInode());

            ContentletDataGen.remove(contentInEnglish);
            ContentletDataGen.remove(contentInSpanish);
            HTMLPageDataGen.remove(englishPage);
            HTMLPageDataGen.remove(spanishPage);
            TemplateDataGen.remove(template);
            ContainerDataGen.remove(container);
            StructureDataGen.remove(structure);
            FolderDataGen.remove(folder);

            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            throw e;
        }
    }

    /**
     * Testing {@link ContentletAPI#getFieldValue(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Field)}
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getFieldValue () throws DotSecurityException, DotDataException {

        //Getting a known structure
        Structure structure = structures.iterator().next();

        Long identifier = uniqueIdentifier.get(structure.getName());

        //Getting a know field for this structure
        //TODO: The definition of the method getFieldByName receive a parameter named "String:structureType", some examples I saw send the Inode, but actually what it needs is the structure name....
        Field foundWysiwygField = FieldFactory.getFieldByVariableName( structure.getInode(), "junitTestWysiwyg" + identifier );

        //Search the contentlets for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );

        //Getting the current value for this field
        Object value = contentletAPI.getFieldValue( contentletList.iterator().next(), foundWysiwygField );

        //Validations
        assertNotNull( value );
        assertTrue( !( ( String ) value ).isEmpty() );
    }

    /**
     * Testing {@link ContentletAPI#addLinkToContentlet(com.dotmarketing.portlets.contentlet.model.Contentlet, String, String, com.liferay.portal.model.User, boolean)}
     *
     * @throws Exception
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void addLinkToContentlet () throws Exception {

        String RELATION_TYPE = new Link().getType();

        //Getting a known structure
        Structure structure = structures.iterator().next();

        //Create a menu link
        Link menuLink = createMenuLink();

        //Search the contentlets for this structure
        List<Contentlet> contentletList = contentletAPI.findByStructure( structure, user, false, 0, 0 );
        Contentlet contentlet = contentletList.iterator().next();

        //Add to this contentlet a link
        contentletAPI.addLinkToContentlet( contentlet, menuLink.getInode(), RELATION_TYPE, user, false );

        //Verify if the link was associated
        //List<Link> relatedLinks = contentletAPI.getRelatedLinks( contentlet, user, false );//TODO: This method is not working but we are not testing it on this call....

        //Get the contentlet Identifier to gather the menu links
        Identifier menuLinkIdentifier = APILocator.getIdentifierAPI().find( menuLink );

        //Verify if the relation was created
        Tree tree = TreeFactory.getTree( contentlet.getInode(), menuLinkIdentifier.getId(), RELATION_TYPE );

        //Validations
        assertNotNull( tree );
        assertNotNull( tree.getParent() );
        assertNotNull( tree.getChild() );
        assertEquals( tree.getParent(), contentlet.getInode() );
        assertEquals( tree.getChild(), menuLinkIdentifier.getId() );
        assertEquals( tree.getRelationType(), RELATION_TYPE );
        
        try{
        	HibernateUtil.startTransaction();
            menuLinkAPI.delete( menuLink, user, false );
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(ContentletAPITest.class, e.getMessage());
        }


    }

    /**
     * Testing {@link ContentletAPI#findPageContentlets(String, String, String, boolean, long, com.liferay.portal.model.User, boolean)}
     *
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void findPageContentlets () throws DotDataException, DotSecurityException {

        //Iterate throw the test contentles
        for ( Contentlet contentlet : contentlets ) {

            //Get the identifier for this contentlet
            Identifier identifier = APILocator.getIdentifierAPI().find( contentlet );

            //Search for related html pages and containers
            List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTreesByChild( identifier.getId() );
            if ( multiTrees != null && !multiTrees.isEmpty() ) {

                for ( MultiTree multiTree : multiTrees ) {

                    //Getting the identifiers
                    Identifier htmlPageIdentifier = APILocator.getIdentifierAPI().find( multiTree.getParent1() );
                    Identifier containerPageIdentifier = APILocator.getIdentifierAPI().find( multiTree.getParent2() );

                    //Find the related contentlets, at this point should return something....
                    List<Contentlet> pageContentlets = contentletAPI.findPageContentlets( htmlPageIdentifier.getId(), containerPageIdentifier.getId(), null, true, -1, user, false );

                    //Validations
                    assertTrue( pageContentlets != null && !pageContentlets.isEmpty() );
                }

                break;
            }
        }
    }

    /**
     * This test is for ordering purposes.
     * When you get the related content of a content, the ordering should be the same as it was stored.
     */
    @Test
    public void test_getAllRelationships_checkOrdering() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try{
            parentContentType = createContentType("parentContentType",BaseContentType.CONTENT);
            childContentType = createContentType("childContentType",BaseContentType.CONTENT);

            //Create Relationship Field and Text Fields
            createRelationshipField("testRelationship",parentContentType.id(),childContentType.variable());
            final String textFieldString = "title";
            createTextField(textFieldString,parentContentType.id());
            createTextField(textFieldString,childContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            final Contentlet contentletChild1 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();
            final Contentlet contentletChild3 = new ContentletDataGen(childContentType.id()).setProperty(textFieldString,"child Contentlet 3").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild1,contentletChild2,contentletChild3));

            //Checkin of the parent to save Relationships
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //Get All Relationships of the parent contentlet
            ContentletRelationships cRelationships = contentletAPI.getAllRelationships(contentletParent);

            //Check that the content is related and the order of the related content (child1 - child2 - child3)
            assertNotNull(cRelationships);
            assertEquals(contentletChild1,cRelationships.getRelationshipsRecords().get(0).getRecords().get(0));
            assertEquals(contentletChild2,cRelationships.getRelationshipsRecords().get(0).getRecords().get(1));
            assertEquals(contentletChild3,cRelationships.getRelationshipsRecords().get(0).getRecords().get(2));

            //Reorder Relationships
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild3,contentletChild1,contentletChild2));
            contentletParent.setInode("");
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //Get All Relationships of the parent contentlet
            cRelationships = contentletAPI.getAllRelationships(contentletParent);

            //Check that the content is related and the order of the related content (child3 - child1 - child2)
            assertNotNull(cRelationships);
            assertEquals(contentletChild3,cRelationships.getRelationshipsRecords().get(0).getRecords().get(0));
            assertEquals(contentletChild1,cRelationships.getRelationshipsRecords().get(0).getRecords().get(1));
            assertEquals(contentletChild2,cRelationships.getRelationshipsRecords().get(0).getRecords().get(2));
        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
            if(childContentType != null){
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    /**
     * This test is for ordering purposes.
     * When you get the related content of a content, the ordering should be the same as it was stored.
     * For selfRelated Content Types.
     */
    @Test
    public void test_getAllRelationships_checkOrdering_selfRelatedContentType() throws DotSecurityException, DotDataException{
        ContentType parentContentType = null;
        try{
            parentContentType = createContentType("parentContentType",BaseContentType.CONTENT);

            //Create Relationship Field and Text Fields
            createRelationshipField("testRelationship",parentContentType.id(),parentContentType.variable());
            final String textFieldString = "title";
            createTextField(textFieldString,parentContentType.id());

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"parent Contentlet").next();
            final Contentlet contentletChild1 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"child Contentlet 2").nextPersisted();
            final Contentlet contentletChild3 = new ContentletDataGen(parentContentType.id()).setProperty(textFieldString,"child Contentlet 3").nextPersisted();

            //Find Relationship
            final Relationship relationship = relationshipAPI.byParent(parentContentType).get(0);

            //Relate contentlets
            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild1,contentletChild2,contentletChild3));

            //Checkin of the parent to save Relationships
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //Get All Relationships of the parent contentlet
            ContentletRelationships cRelationships = contentletAPI.getAllRelationships(contentletParent);

            //Check that the content is related and the order of the related content (child1 - child2 - child3)
            assertNotNull(cRelationships);
            assertEquals(contentletChild1,cRelationships.getRelationshipsRecords().get(1).getRecords().get(0));
            assertEquals(contentletChild2,cRelationships.getRelationshipsRecords().get(1).getRecords().get(1));
            assertEquals(contentletChild3,cRelationships.getRelationshipsRecords().get(1).getRecords().get(2));

            //Reorder Relationships
            relationshipListMap.put(relationship,CollectionsUtils.list(contentletChild3,contentletChild1,contentletChild2));
            contentletParent.setInode("");
            contentletParent = contentletAPI.checkin(contentletParent,relationshipListMap,user,false);

            //Get All Relationships of the parent contentlet
            cRelationships = contentletAPI.getAllRelationships(contentletParent);

            //Check that the content is related and the order of the related content (child3 - child1 - child2)
            assertNotNull(cRelationships);
            assertEquals(contentletChild3,cRelationships.getRelationshipsRecords().get(1).getRecords().get(0));
            assertEquals(contentletChild1,cRelationships.getRelationshipsRecords().get(1).getRecords().get(1));
            assertEquals(contentletChild2,cRelationships.getRelationshipsRecords().get(1).getRecords().get(2));
        }finally {
            if(parentContentType != null){
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    private com.dotcms.contenttype.model.field.Field createRelationshipField(final String fieldName, final String parentContentTypeID, final String childContentTypeVariable)
            throws DotDataException, DotSecurityException {
        final com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(
                RelationshipField.class)
                .name(fieldName)
                .contentTypeId(parentContentTypeID)
                .values(String.valueOf(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .indexed(true)
                .listed(false)
                .relationType(childContentTypeVariable)
                .build();

        return getContentTypeFieldAPI().save(field, APILocator.systemUser());
    }

    /**
     * Testing {@link ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllRelationships () throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        try {
            //Getting a known contentlet
            Contentlet contentlet = contentlets.iterator().next();

            //Create the test relationship
            testRelationship = createRelationShip(contentlet.getStructure(), false);

            //Find all the relationships for this contentlet
            ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(contentlet.getInode(), user, false);

            //Validations
            assertNotNull(contentletRelationships);
            assertTrue(contentletRelationships.getRelationshipsRecords() != null
                    && !contentletRelationships.getRelationshipsRecords().isEmpty());

        }finally{
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }
        }
    }

    @Test
    public void testCreateSelfJoinedRelationship_contentletAddedAsChild () throws DotDataException, DotSecurityException {

        Contentlet parent = null;
        Contentlet child = null;
        Structure structure = null;
        Relationship selfRelationship = null;

        try {
            //Get Default Language
            Language language = languageAPI.getDefaultLanguage();

            //Create a Structure
            structure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Create the Contentlets
            parent = createContentlet(structure, language, false);
            child = createContentlet(structure, language, false);

            //Create the Self contained relationship
            selfRelationship = createRelationShip(structure.getInode(),
                    structure.getInode(), false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(child);

            //Relate the content
            contentletAPI
                    .relateContent(parent, selfRelationship, contentRelationships, user, false);

            //Find all the relationships for this contentlet
            ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(parent.getInode(), user, false);

            //Validations
            assertNotNull(contentletRelationships);
            assertTrue(contentletRelationships.getRelationshipsRecords() != null
                    && !contentletRelationships.getRelationshipsRecords().isEmpty());
            for (ContentletRelationshipRecords record : contentletRelationships
                    .getRelationshipsRecords()) {
                if (record.isHasParent()) {
                    assertNotNull(record.getRecords());
                }
            }
        } finally {

            //Clean up
            if (parent != null) {
                contentletAPI.archive(parent, user, false);
                contentletAPI.delete(parent, user, false);
            }
            if (child != null) {
                contentletAPI.archive(child, user, false);
                contentletAPI.delete(child, user, false);
            }
            if (selfRelationship != null) {
                relationshipAPI.delete(selfRelationship);
            }
            if (structure != null) {
                APILocator.getStructureAPI().delete(structure, user);
            }
        }

    }

    /**
     * Testing {@link ContentletAPI#getAllRelationships(com.dotmarketing.portlets.contentlet.model.Contentlet)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllRelationshipsByContentlet () throws DotSecurityException, DotDataException {

        Structure testStructure       = null;
        Relationship testRelationship = null;
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);

            //Relate the content
            contentletAPI
                    .relateContent(parentContentlet, testRelationship, contentRelationships, user,
                            false);

            //Getting a known contentlet
//        Contentlet contentlet = contentlets.iterator().next();

            //Find all the relationships for this contentlet
            ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(parentContentlet);

            //Validations
            assertNotNull(contentletRelationships);
            assertTrue(contentletRelationships.getRelationshipsRecords() != null
                    && !contentletRelationships.getRelationshipsRecords().isEmpty());
        }finally{
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null){
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }

    }

    /**
     * Testing {@link ContentletAPI#getAllLanguages(com.dotmarketing.portlets.contentlet.model.Contentlet, Boolean, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void getAllLanguages () throws DotSecurityException, DotDataException {

        Structure st=new Structure();
        st.setStructureType(BaseContentType.CONTENT.getType());
        st.setName("JUNIT-test-getAllLanguages"+System.currentTimeMillis());
        st.setVelocityVarName("testAllLanguages"+System.currentTimeMillis());
        st.setHost(defaultHost.getIdentifier());
        StructureFactory.saveStructure(st);

        Field ff=new Field("title",Field.FieldType.TEXT,Field.DataType.TEXT,st,true,true,true,1,false,false,true);
        FieldFactory.saveField(ff);

        String identifier=null;
        List<Language> list=APILocator.getLanguageAPI().getLanguages();
        Contentlet last=null;
        for(Language ll : list) {
            Contentlet con=new Contentlet();
            con.setStructureInode(st.getInode());
            if(identifier!=null) con.setIdentifier(identifier);
            con.setStringProperty(ff.getVelocityVarName(), "test text "+System.currentTimeMillis());
            con.setLanguageId(ll.getId());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con=contentletAPI.checkin(con, user, false);
            if(identifier==null) identifier=con.getIdentifier();
            APILocator.getVersionableAPI().setLive(con);
            last=con;
        }

        //Get all the contentles siblings for this contentlet (contentlet for all the languages)
        List<Contentlet> forAllLanguages = contentletAPI.getAllLanguages( last, true, user, false );

        //Validations
        assertNotNull( forAllLanguages );
        assertTrue( !forAllLanguages.isEmpty() );
        assertEquals(list.size(), forAllLanguages.size());
        APILocator.getContentTypeAPI(user).delete(new StructureTransformer(st).from());
    }

    /**
     * Testing {@link ContentletAPI#isContentEqual(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void isContentEqual () throws DotDataException, DotSecurityException {

        Iterator<Contentlet> contentletIterator = contentlets.iterator();

        //Getting test contentlets
        Contentlet contentlet1 = contentletIterator.next();
        Contentlet contentlet2 = contentletIterator.next();

        //Compare if the contentlets are equal
        Boolean areEqual = contentletAPI.isContentEqual( contentlet1, contentlet2, user, false );

        //Validations
        assertNotNull( areEqual );
        assertFalse( areEqual );
    }

    /**
     * Testing {@link ContentletAPI#archive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void archive () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        try {
            //Archive this given contentlet (means it will be mark it as deleted)
            contentletAPI.archive( contentlet, user, false );

            //Verify if it was deleted
            Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

            //Validations
            assertNotNull( isDeleted );
            assertTrue( isDeleted );
        } finally {
            contentletAPI.unarchive( contentlet, user, false );
        }
    }

    /**
     * https://github.com/dotCMS/core/issues/11716
     * @throws DotDataException
     * @throws DotSecurityException
     */

    @Test
    public void addRemoveContentFromIndex()
            throws DotDataException, DotSecurityException {
        // respect CMS Anonymous permissions
        boolean respectFrontendRoles = false;
        int num = 5;

        //clean up old reindexed records
        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        Host host = APILocator.getHostAPI().findDefaultHost(user, respectFrontendRoles);
        Folder folder = APILocator.getFolderAPI().findSystemFolder();


        Language lang = APILocator.getLanguageAPI().getDefaultLanguage();
        ContentType type = APILocator.getContentTypeAPI(user).find("webPageContent");
        List<Contentlet> origCons = new ArrayList<>();

        Map map = new HashMap<>();
        map.put("stInode", type.id());
        map.put("host", host.getIdentifier());
        map.put("folder", folder.getInode());
        map.put("languageId", lang.getId());
        map.put("sortOrder", new Long(0));
        map.put("body", "body");

        //add 5 contentlets
        for (int i = 0; i < num; i++) {
            map.put("title", i + "my test title");

            // create a new piece of content backed by the map created above
            Contentlet content = new Contentlet(map);
            content.setIndexPolicy(IndexPolicy.WAIT_FOR);

            // check in the content
            content = contentletAPI.checkin(content, user, respectFrontendRoles);

            assertTrue(content.getIdentifier() != null);
            assertTrue(content.isWorking());
            assertFalse(content.isLive());
            origCons.add(content);
        }

        //commit it index
        HibernateUtil.closeSession();
        for (Contentlet c : origCons) {
            assertEquals(1, contentletAPI.indexCount(
                    "+live:false +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(),
                    user, respectFrontendRoles));
        }

        HibernateUtil.startTransaction();
        try {
            for (Contentlet c : origCons) {
                Contentlet newContentlet = new Contentlet(c);
                newContentlet.setInode("");
                newContentlet.setIndexPolicy(IndexPolicy.DEFER);
                newContentlet.setStringProperty("title", c.getStringProperty("title") + " new");
                newContentlet = contentletAPI.checkin(newContentlet, user, respectFrontendRoles);
                contentletAPI.publish(newContentlet, user, respectFrontendRoles);
                assertTrue(newContentlet.isLive());
            }
            throw new DotDataException("uh oh, what happened?");
        } catch (DotDataException e) {
            HibernateUtil.rollbackTransaction();
        } finally {
            HibernateUtil.closeSession();
        }

        for (Contentlet c : origCons) {
            assertEquals(0, contentletAPI
                    .indexCount("+live:true +identifier:" + c.getIdentifier(), user,
                            respectFrontendRoles));
            assertEquals(1, contentletAPI.indexCount(
                    "+live:false +identifier:" + c.getIdentifier() + " +inode:" + c.getInode(),
                    user, respectFrontendRoles));
        }

    }

    /**
     * Testing {@link ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void delete () throws Exception {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );

        //Now we need to delete it
        contentletAPI.archive(newContentlet, user, false);
        contentletAPI.delete( newContentlet, user, false );

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find( newContentlet.getInode(), user, false );

        //Validations
        assertTrue( foundContentlet == null || foundContentlet.getInode() == null || foundContentlet.getInode().isEmpty() );

        // make sure the db is totally clean up

        AssetUtil.assertDeleted(newContentlet.getInode(), newContentlet.getIdentifier(), "contentlet");

        APILocator.getStructureAPI().delete(testStructure, user);
    }

    @Test
    public void delete_GivenUnarchivedContentAndDontValidateMeInTrue_ShouldDeleteSuccessfully () throws Exception {
        Structure testStructure = null;
        try {
            testStructure = createStructure("JUnit Test Structure_" + String.valueOf(new Date().getTime()), "junit_test_structure_" + String.valueOf(new Date().getTime()));
            final Contentlet newContentlet = createContentlet(testStructure, null, false);
            newContentlet.setStringProperty(Contentlet.DONT_VALIDATE_ME, "anarchy");
            contentletAPI.delete(newContentlet, user, false);
            final Contentlet foundContentlet = contentletAPI.find(newContentlet.getInode(), user, false);
            assertTrue( !UtilMethods.isSet(foundContentlet) || !UtilMethods.isSet(foundContentlet.getInode()) );

            AssetUtil.assertDeleted(newContentlet.getInode(), newContentlet.getIdentifier(), "contentlet");
        } finally {
            APILocator.getStructureAPI().delete(testStructure, user);
        }
    }

    /**
     * Testing {@link ContentletAPI#delete(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteForAllVersions () throws DotSecurityException, DotDataException {

        Language language = APILocator.getLanguageAPI().getDefaultLanguage();

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, language, false );

        // new inode to create a new version
        String newInode=UUIDGenerator.generateUuid();
        newContentlet.setInode(newInode);


        List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }

        newContentlet.setLanguageId(language.getId());

        newContentlet = contentletAPI.checkin(newContentlet, user, false);

        //Now we need to delete it
        contentletAPI.archive(newContentlet, user, false);
        contentletAPI.delete( newContentlet, user, false, true );

        //Try to find the deleted Contentlet
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet.getIdentifier() );
        List<Contentlet> foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false );

        //Validations
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing {@link ContentletAPI#publish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publish () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Publish the test contentlet
        contentletAPI.publish( contentlet, user, false );

        //Verify if it was published
        Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

        //Validations
        assertNotNull( isLive );
        assertTrue( isLive );
    }

    /**
     * Testing {@link ContentletAPI#publish(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void publishCollection () throws DotDataException, DotSecurityException {

        //Publish all the test contentlets
        contentletAPI.publish( contentlets, user, false );

        for ( Contentlet contentlet : contentlets ) {

            //Verify if it was published
            Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

            //Validations
            assertNotNull( isLive );
            assertTrue( isLive );
        }
    }

    /**
     * Testing {@link ContentletAPI#unpublish(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unpublish () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //Verify if it is published
        Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );
        if ( !isLive ) {
            //Publish the test contentlet
            contentletAPI.publish( contentlet, user, false );

            //Verify if it was published
            isLive = APILocator.getVersionableAPI().isLive( contentlet );

            //Validations
            assertNotNull( isLive );
            assertTrue( isLive );
        }

        //Unpublish the test contentlet
        contentletAPI.unpublish( contentlet, user, false );

        //Verify if it was unpublished
        isLive = APILocator.getVersionableAPI().isLive( contentlet );

        //Validations
        assertNotNull( isLive );
        assertFalse( isLive );
    }

    /**
     * Testing {@link ContentletAPI#unpublish(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unpublishCollection () throws DotDataException, DotSecurityException {

        contentletAPI.publish(contentlets, user, false);

        //Unpublish all the test contentlets
        contentletAPI.unpublish( contentlets, user, false );

        for ( Contentlet contentlet : contentlets ) {

            //Verify if it was unpublished
            Boolean isLive = APILocator.getVersionableAPI().isLive( contentlet );

            //Validations
            assertNotNull( isLive );
            assertFalse( isLive );
        }
    }

    /**
     * Testing {@link ContentletAPI#archive(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void archiveCollection () throws DotDataException, DotSecurityException {

        try {
            //Archive this given contentlet collection (means it will be mark them as deleted)
            contentletAPI.archive( contentlets, user, false );

            for ( Contentlet contentlet : contentlets ) {

                //Verify if it was deleted
                Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

                //Validations
                assertNotNull( isDeleted );
                assertTrue( isDeleted );
            }
        } finally {
            contentletAPI.unarchive( contentlets, user, false );
        }
    }

    /**
     * Testing {@link ContentletAPI#unarchive(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unarchiveCollection () throws DotDataException, DotSecurityException {

        //First lets archive this given contentlet collection (means it will be mark them as deleted)
        contentletAPI.archive( contentlets, user, false );

        //Now lets test the unarchive
        contentletAPI.unarchive( contentlets, user, false );

        for ( Contentlet contentlet : contentlets ) {

            //Verify if it continues as deleted
            Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

            //Validations
            assertNotNull( isDeleted );
            assertFalse( isDeleted );
        }
    }

    /**
     * Testing {@link ContentletAPI#unarchive(com.dotmarketing.portlets.contentlet.model.Contentlet, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void unarchive () throws DotDataException, DotSecurityException {

        //Getting a known contentlet
        Contentlet contentlet = contentlets.iterator().next();

        //First lets archive this given contentlet (means it will be mark it as deleted)
        contentletAPI.archive( contentlet, user, false );

        //Now lets test the unarchive
        contentletAPI.unarchive( contentlet, user, false );

        //Verify if it continues as deleted
        Boolean isDeleted = APILocator.getVersionableAPI().isDeleted( contentlet );

        //Validations
        assertNotNull( isDeleted );
        assertFalse( isDeleted );
    }

    /**
     * Testing {@link ContentletAPI#deleteAllVersionsandBackup(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteAllVersionsAndBackup () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet.getIdentifier() );

        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add( newContentlet );
        contentletAPI.deleteAllVersionsandBackup( testContentlets, user, false );

        //Try to find the versions for this Contentlet (Must be only one version)
        List<Contentlet> versions = contentletAPI.findAllVersions( contentletIdentifier, user, false );

        //Validations
        assertNotNull( versions );
        assertEquals( versions.size(), 1 );
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing {@link ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteCollection () throws DotSecurityException, DotDataException {

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Now a new contentlet
        Contentlet newContentlet = createContentlet( testStructure, null, false );

        //Now test this delete
        contentletAPI.archive(newContentlet, user, false);
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add( newContentlet );
        contentletAPI.delete( testContentlets, user, false );

        //Try to find the deleted Contentlet
        Contentlet foundContentlet = contentletAPI.find( newContentlet.getInode(), user, false );

        //Validations
        assertTrue( foundContentlet == null || foundContentlet.getInode() == null || foundContentlet.getInode().isEmpty() );
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing {@link ContentletAPI#delete(java.util.List, com.liferay.portal.model.User, boolean, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteCollectionAllVersions () throws DotSecurityException, DotDataException {

        Language language = APILocator.getLanguageAPI().getDefaultLanguage();

        //First lets create a test structure
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

        //Create a new contentlet with one version
        Contentlet newContentlet1 = createContentlet( testStructure, language, false );

        //Create a new contentlet with two versions
        Contentlet newContentlet2 = createContentlet( testStructure, language, false );

        // new inode to create the second version
        String newInode=UUIDGenerator.generateUuid();
        newContentlet2.setInode(newInode);

        List<Language> languages = APILocator.getLanguageAPI().getLanguages();
        for ( Language localLanguage : languages ) {
            if ( localLanguage.getId() != language.getId() ) {
                language = localLanguage;
                break;
            }
        }

        newContentlet2.setLanguageId(language.getId());

        newContentlet2 = contentletAPI.checkin(newContentlet2, user, false);


        //Now test this delete
        List<Contentlet> testContentlets = new ArrayList<>();
        testContentlets.add( newContentlet1 );
        testContentlets.add( newContentlet2 );
        contentletAPI.delete( testContentlets, user, false, true );

        //Try to find the deleted Contentlets
        Identifier contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet1.getIdentifier() );
        List<Contentlet> foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false );

        //Validations for newContentlet1
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );

        contentletIdentifier = APILocator.getIdentifierAPI().find( newContentlet2.getIdentifier() );
        foundContentlets = contentletAPI.findAllVersions(contentletIdentifier, user, false );

        //Validations for newContentlet2
        assertTrue( foundContentlets == null || foundContentlets.isEmpty() );
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Testing {@link ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteRelatedContent () throws DotSecurityException, DotDataException {

        Structure testStructure = null;
        Relationship testRelationship = null;

        try {
            //First lets create a test structure
            testStructure =
                createStructure("JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet baseContentlet = createContentlet(testStructure, null, false);
            Contentlet contentToRelateAsChild = createContentlet(testStructure, null, false);
            Contentlet contentToRelateAsParent = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure.getInode(), testStructure.getInode(),
                false, 1);

            //Relate content as child
            List<Contentlet> childrenList = new ArrayList<>();
            childrenList.add(contentToRelateAsChild);
            ContentletRelationships childrenRelationships = createContentletRelationships(testRelationship,
                baseContentlet, testStructure, childrenList, true);

            //Relate content as parent
            List<Contentlet> parentList = new ArrayList<>();
            parentList.add(contentToRelateAsParent);
            ContentletRelationships parentRelationshis = createContentletRelationships(testRelationship,
                baseContentlet, testStructure, parentList, false);

            //Relate contents to our test contentlet
            for (final ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : childrenRelationships
                .getRelationshipsRecords()) {
                contentletAPI.relateContent(baseContentlet, contentletRelationshipRecords, user, false);
            }

            //Relate contents to our test contentlet
            for (ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : parentRelationshis
                .getRelationshipsRecords()) {
                contentletAPI.relateContent(baseContentlet, contentletRelationshipRecords, user, false);
            }

            // Let's delete only the children (1 child)
            contentletAPI.deleteRelatedContent(baseContentlet, testRelationship, true, user, false);

            // we should have only one content (1 parent) since we just deleted the one child
            List<Contentlet> foundContentlets = relationshipAPI.dbRelatedContent(testRelationship, baseContentlet,
                false);
            assertTrue(!foundContentlets.isEmpty() && foundContentlets.size() == 1);

            // Let's now delete the parent
            contentletAPI.deleteRelatedContent(baseContentlet, testRelationship, false, user, false);

            // we should get no content back for both hasParent `true` and `false` since the one child and one parent were deleted
            foundContentlets = relationshipAPI.dbRelatedContent(testRelationship, baseContentlet, false);
            assertTrue(!UtilMethods.isSet(foundContentlets));

            foundContentlets = relationshipAPI.dbRelatedContent(testRelationship, baseContentlet, true);
            assertTrue(!UtilMethods.isSet(foundContentlets));
        } finally {
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }
            if(testStructure!=null && testStructure != null) {
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }



    /**
     * Testing {@link ContentletAPI#deleteRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, boolean, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void deleteRelatedContentWithParent () throws DotSecurityException, DotDataException {

        Structure testStructure       = null;
        Relationship testRelationship = null;

        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);
            ContentletRelationships contentletRelationships = createContentletRelationships(
                    testRelationship, parentContentlet, testStructure, contentRelationships);

            //Relate contents to our test contentlet
            for (ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships
                    .getRelationshipsRecords()) {
                contentletAPI.relateContent(parentContentlet, contentletRelationshipRecords, user,
                        false);
            }

            Boolean hasParent = FactoryLocator.getRelationshipFactory()
                    .isParent(testRelationship, parentContentlet.getStructure());

            //Now test this delete
            contentletAPI.deleteRelatedContent(parentContentlet, testRelationship, hasParent, user,
                    false);

            //Try to find the deleted Contentlet
            List<Contentlet> foundContentlets = contentletAPI
                    .getRelatedContent(parentContentlet, testRelationship, user, false);

            //Validations
            assertTrue(foundContentlets == null || foundContentlets.isEmpty());
        }finally{
            if (testRelationship != null && testRelationship.getInode() != null) {
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null){
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#relateContent(Contentlet, ContentletRelationships.ContentletRelationshipRecords, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void relateContent () throws DotSecurityException, DotDataException {

        Structure testStructure       = null;
        Relationship testRelationship = null;
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);
            ContentletRelationships contentletRelationships = createContentletRelationships(
                    testRelationship, parentContentlet, testStructure, contentRelationships);

            //Relate contents to our test contentlet
            for (ContentletRelationships.ContentletRelationshipRecords contentletRelationshipRecords : contentletRelationships
                    .getRelationshipsRecords()) {
                //Testing the relate content...
                contentletAPI.relateContent(parentContentlet, contentletRelationshipRecords, user,
                        false);
            }

            //Verify if the content was related
            Tree tree = TreeFactory
                    .getTree(parentContentlet.getIdentifier(), childContentlet.getIdentifier(),
                            testRelationship.getRelationTypeValue());

            //Validations
            assertNotNull(tree);
            assertNotNull(tree.getParent());
            assertNotNull(tree.getChild());
            assertEquals(tree.getParent(), parentContentlet.getIdentifier());
            assertEquals(tree.getChild(), childContentlet.getIdentifier());
            assertEquals(tree.getRelationType(), testRelationship.getRelationTypeValue());

        }finally {
            if (testRelationship != null && testRelationship.getInode() != null){
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null){
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#relateContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, java.util.List, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Test
    public void relateContentDirect () throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure       = null;
        try{
            //First lets create a test structure
            testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet( testStructure, null, false );
            Contentlet childContentlet = createContentlet( testStructure, null, false );

            //Create the relationship
            testRelationship = createRelationShip( testStructure, false );

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add( childContentlet );

            //Relate the content
            contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

            //Verify if the content was related
            Tree tree = TreeFactory.getTree( parentContentlet.getIdentifier(), childContentlet.getIdentifier(), testRelationship.getRelationTypeValue() );

            //Validations
            assertNotNull( tree );
            assertNotNull( tree.getParent() );
            assertNotNull( tree.getChild() );
            assertEquals( tree.getParent(), parentContentlet.getIdentifier() );
            assertEquals( tree.getChild(), childContentlet.getIdentifier() );
            assertEquals( tree.getRelationType(), testRelationship.getRelationTypeValue() );

        }finally {
            if (testRelationship != null && testRelationship.getInode() != null){
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null){
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#getRelatedContent(com.dotmarketing.portlets.contentlet.model.Contentlet, com.dotmarketing.portlets.structure.model.Relationship, com.liferay.portal.model.User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Ignore ( "Not Ready to Run." )
    @Test
    public void getRelatedContent () throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure       = null;
        try{
            //First lets create a test structure
            testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ), "junit_test_structure_" + String.valueOf( new Date().getTime() ) );

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet( testStructure, null, false );
            Contentlet childContentlet = createContentlet( testStructure, null, false );

            //Create the relationship
            testRelationship = createRelationShip( testStructure, false );

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add( childContentlet );

            //Relate the content
            contentletAPI.relateContent( parentContentlet, testRelationship, contentRelationships, user, false );

            List<Relationship> relationships = FactoryLocator.getRelationshipFactory().byContentType( parentContentlet.getStructure() );
            //Validations
            assertTrue( relationships != null && !relationships.isEmpty() );

            List<Contentlet> foundContentlets = null;
            for ( Relationship relationship : relationships ) {
                foundContentlets = contentletAPI.getRelatedContent( parentContentlet, relationship, user, true );
            }

            //Validations
            assertTrue( foundContentlets != null && !foundContentlets.isEmpty() );

        }finally {
            if (testRelationship != null && testRelationship.getInode() != null){
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null){
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Testing {@link ContentletAPI#getRelatedContent(Contentlet, Relationship, User, boolean)}
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @see ContentletAPI
     * @see Contentlet
     */
    @Ignore ( "Not Ready to Run." )
    @Test
    public void getRelatedContentPullByParent () throws DotSecurityException, DotDataException {

        Relationship testRelationship = null;
        Structure testStructure       = null;
        APILocator.getStructureAPI().delete(testStructure, user);
        try {
            //First lets create a test structure
            testStructure = createStructure(
                    "JUnit Test Structure_" + String.valueOf(new Date().getTime()),
                    "junit_test_structure_" + String.valueOf(new Date().getTime()));

            //Now a new test contentlets
            Contentlet parentContentlet = createContentlet(testStructure, null, false);
            Contentlet childContentlet = createContentlet(testStructure, null, false);

            //Create the relationship
            testRelationship = createRelationShip(testStructure, false);

            //Create the contentlet relationships
            List<Contentlet> contentRelationships = new ArrayList<>();
            contentRelationships.add(childContentlet);

            //Relate the content
            contentletAPI
                    .relateContent(parentContentlet, testRelationship, contentRelationships, user,
                            false);

            Boolean hasParent = FactoryLocator.getRelationshipFactory()
                    .isParent(testRelationship, parentContentlet.getStructure());

            List<Relationship> relationships = FactoryLocator.getRelationshipFactory()
                    .byContentType(parentContentlet.getStructure());
            //Validations
            assertTrue(relationships != null && !relationships.isEmpty());

            List<Contentlet> foundContentlets = null;
            for (Relationship relationship : relationships) {
                foundContentlets = contentletAPI
                        .getRelatedContent(parentContentlet, relationship, hasParent, user, true);
            }

            //Validations
            assertTrue(foundContentlets != null && !foundContentlets.isEmpty());
        }finally {
            if (testRelationship != null && testRelationship.getInode() != null){
                relationshipAPI.delete(testRelationship);
            }

            if (testStructure != null && testStructure.getInode() != null){
                APILocator.getStructureAPI().delete(testStructure, user);
            }
        }
    }

    /**
     * Now we introduce the case when we wanna add content with
     * the inode & identifier we set. The content should not exists
     * for that inode nor the identifier.
     *
     * @throws Exception if test fails
     */
    @Test
    public void saveContentWithExistingIdentifier() throws Exception {
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ) + "zzz", "junit_test_structure_" + String.valueOf( new Date().getTime() ) + "zzz" );

        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, false, true, false, 1, false, false, false );
        FieldFactory.saveField( field );

        Contentlet cont=new Contentlet();
        cont.setStructureInode(testStructure.getInode());
        cont.setStringProperty(field.getVelocityVarName(), "a value");
        cont.setReviewInterval( "1m" );
        cont.setStructureInode( testStructure.getInode() );
        cont.setHost( defaultHost.getIdentifier() );

        // here comes the existing inode and identifier
        // for this test we generate them using the normal
        // generator but the use case for this is when
        // the content comes from another dotCMS instance
        String inode=UUIDGenerator.generateUuid();
        String identifier=UUIDGenerator.generateUuid();
        cont.setInode(inode);
        cont.setIdentifier(identifier);

        Contentlet saved = contentletAPI.checkin(cont, user, false);

        assertEquals(saved.getInode(), inode);
        assertEquals(saved.getIdentifier(), identifier);

        // the inode should hit the index
        contentletAPI.isInodeIndexed(inode, 2);

        CacheLocator.getContentletCache().clearCache();

        // now lets test with existing content
        Contentlet existing=contentletAPI.find(inode, user, false);
        assertEquals(inode, existing.getInode());
        assertEquals(identifier, existing.getIdentifier());

        // new inode to create a new version
        String newInode=UUIDGenerator.generateUuid();
        existing.setInode(newInode);
        existing.setIndexPolicy(IndexPolicy.FORCE);

        saved=contentletAPI.checkin(existing, user, false);
        
        assertEquals(newInode, saved.getInode());
        assertEquals(identifier, saved.getIdentifier());

        APILocator.getStructureAPI().delete(testStructure, user);
    }

    /**
     * Making sure we set pub/exp dates on identifier when saving content
     * and we set them back to the content when reading.
     *
     * https://github.com/dotCMS/dotCMS/issues/1763
     */
    @Test
    public void testUpdatePublishExpireDatesFromIdentifier() throws Exception {
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // set up a structure with pub/exp variables
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ) + "zzzvv", "junit_test_structure_" + String.valueOf( new Date().getTime() ) + "zzzvv" );
        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, false, true, true, 1, false, false, false );
        FieldFactory.saveField( field );
        Field fieldPubDate = new Field( "Pub Date", Field.FieldType.DATE_TIME, Field.DataType.DATE, testStructure, false, true, true, 2, false, false, false );
        FieldFactory.saveField( fieldPubDate );
        Field fieldExpDate = new Field( "Exp Date", Field.FieldType.DATE_TIME, Field.DataType.DATE, testStructure, false, true, true, 3, false, false, false );
        FieldFactory.saveField( fieldExpDate );
        testStructure.setPublishDateVar(fieldPubDate.getVelocityVarName());
        testStructure.setExpireDateVar(fieldExpDate.getVelocityVarName());
        StructureFactory.saveStructure(testStructure);

        // some dates to play with

        String date = "2222-08-11 10:20:56";
        Date d1= dateFormat.parse(date);
        Date d2=new Date(d1.getTime()+60000L);
        Date d3=new Date(d2.getTime()+60000L);
        Date d4=new Date(d3.getTime()+60000L);

        // get default lang and one alternate to play with sibblings
        long deflang=APILocator.getLanguageAPI().getDefaultLanguage().getId();
        long altlang=-1;
        for(Language ll : APILocator.getLanguageAPI().getLanguages())
            if(ll.getId()!=deflang)
                altlang=ll.getId();

        // if we save using d1 & d1 then the identifier should
        // have those values after save
        Contentlet c1=new Contentlet();
        c1.setStructureInode(testStructure.getInode());
        c1.setStringProperty(field.getVelocityVarName(), "c1");
        c1.setDateProperty(fieldPubDate.getVelocityVarName(), d1);
        c1.setDateProperty(fieldExpDate.getVelocityVarName(), d2);
        c1.setLanguageId(deflang);
        c1.setIndexPolicy(IndexPolicy.FORCE);
        c1=APILocator.getContentletAPI().checkin(c1, user, false);

        Identifier idenFromCache = APILocator.getIdentifierAPI().loadFromCache(c1.getIdentifier());
        Logger.info(this, "IdentifierFromCache:" + idenFromCache);

        Identifier ident=APILocator.getIdentifierAPI().find(c1);
        assertNotNull(ident.getSysPublishDate());
        assertNotNull(ident.getSysExpireDate());

        assertTrue(dateFormat.format(d1)
                .equals(dateFormat.format(ident.getSysPublishDate())));
        assertTrue(dateFormat.format(d2).equals(dateFormat.format(ident.getSysExpireDate())));


        // if we save another language version for the same identifier
        // then the identifier should be updated with those dates d3&d4
        Contentlet c2=new Contentlet();
        c2.setStructureInode(testStructure.getInode());
        c2.setStringProperty(field.getVelocityVarName(), "c2");
        c2.setIdentifier(c1.getIdentifier());
        c2.setDateProperty(fieldPubDate.getVelocityVarName(), d3);
        c2.setDateProperty(fieldExpDate.getVelocityVarName(), d4);
        c2.setLanguageId(altlang);
        c2.setIndexPolicy(IndexPolicy.FORCE);
        c2=APILocator.getContentletAPI().checkin(c2, user, false);

        Identifier ident2=APILocator.getIdentifierAPI().find(c2);
        assertNotNull(ident2.getSysPublishDate());
        assertNotNull(ident2.getSysExpireDate());

        assertTrue(dateFormat.format(d3)
                .equals(dateFormat.format(ident2.getSysPublishDate())));
        assertTrue(dateFormat.format(d4)
                .equals(dateFormat.format(ident2.getSysExpireDate())));

        // the other contentlet should have the same dates if we read it again
        Contentlet c11=APILocator.getContentletAPI().find(c1.getInode(), user, false);
        assertTrue(dateFormat.format(d3).equals(dateFormat.format(c11.getDateProperty(fieldPubDate.getVelocityVarName()))));
        assertTrue(dateFormat.format(d4).equals(dateFormat.format(c11.getDateProperty(fieldExpDate.getVelocityVarName()))));


        Thread.sleep(2000); // wait a bit for the index
        
        // also it should be in the index update with the new dates
        String q="+structureName:"+testStructure.getVelocityVarName()+
                " +inode:"+c11.getInode()+
                " +"+testStructure.getVelocityVarName()+"."+fieldPubDate.getVelocityVarName()+":"+ DateUtil.toLuceneDateTime(d3)+
                " +"+testStructure.getVelocityVarName()+"."+fieldExpDate.getVelocityVarName()+":"+ DateUtil.toLuceneDateTime(d4);
        final long count = APILocator.getContentletAPI().indexCount(q, user, false);
        assertEquals(1, count);
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    private boolean compareDates(Date date1, Date date2) {

        DateFormat dateFormat = SimpleDateFormat
                .getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);
        return dateFormat.format(date1).equals(dateFormat.format(date2));
    }


    @Test
    public void rangeQuery() throws Exception {
        // https://github.com/dotCMS/dotCMS/issues/2630
        Structure testStructure = createStructure( "JUnit Test Structure_" + String.valueOf( new Date().getTime() ) + "zzzvv", "junit_test_structure_" + String.valueOf( new Date().getTime() ) + "zzzvv" );
        Field field = new Field( "JUnit Test Text", Field.FieldType.TEXT, Field.DataType.TEXT, testStructure, false, true, true, 1, false, false, false );
        field = FieldFactory.saveField( field );

        List<Contentlet> list=new ArrayList<>();
        String[] letters={"a","b","c","d","e","f","g"};
        for(String letter : letters) {
            Contentlet conn=new Contentlet();
            conn.setStructureInode(testStructure.getInode());
            conn.setStringProperty(field.getVelocityVarName(), letter);
            conn.setIndexPolicy(IndexPolicy.FORCE);
            conn = contentletAPI.checkin(conn, user, false);
            list.add(conn);
        }
        String query = "+structurename:"+testStructure.getVelocityVarName()+
                " +"+testStructure.getVelocityVarName()+"."+field.getVelocityVarName()+":[b   TO f ]";
        String sort = testStructure.getVelocityVarName()+"."+field.getVelocityVarName()+" asc";
        List<Contentlet> search = contentletAPI.search(query, 100, 0, sort, user, false);
        assertEquals(5,search.size());
        assertEquals("b",search.get(0).getStringProperty(field.getVelocityVarName()));
        assertEquals("c",search.get(1).getStringProperty(field.getVelocityVarName()));
        assertEquals("d",search.get(2).getStringProperty(field.getVelocityVarName()));
        assertEquals("e",search.get(3).getStringProperty(field.getVelocityVarName()));
        assertEquals("f",search.get(4).getStringProperty(field.getVelocityVarName()));

        contentletAPI.archive(list, user, false);
        contentletAPI.delete(list, user, false);
        FieldFactory.deleteField(field);
        APILocator.getStructureAPI().delete(testStructure, user);
    }

    @Test
    public void widgetInvalidateAllLang() throws Exception {
        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",true);

        HttpServletRequest requestProxy = new MockInternalRequest().request();
        HttpServletResponse responseProxy = new BaseResponse().response();

        initMessages();

        Language def=APILocator.getLanguageAPI().getDefaultLanguage();
        Contentlet w = new Contentlet();
        w.setStructureInode(simpleWidgetContentType.id());
        w.setStringProperty("widgetTitle", "A testing widget "+UUIDGenerator.generateUuid());
        w.setStringProperty("code", "Initial code");
        w.setLanguageId(def.getId());
        w.setIndexPolicy(IndexPolicy.FORCE);
        w.setIndexPolicyDependencies(IndexPolicy.FORCE);
        w = contentletAPI.checkin(w, user, false);
        contentletAPI.publish(w, user, false);

        /*
         * For every language we should get the same content and contentMap template code
         */
        VelocityEngine engine = VelocityUtil.getEngine();
        SimpleNode contentTester = engine.getRuntimeServices().parse(new StringReader("code:$code"), "tester1");

        contentTester.init(null, null);

        requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        requestProxy.setAttribute(com.liferay.portal.util.WebKeys.USER,APILocator.getUserAPI().getSystemUser());

        org.apache.velocity.Template teng1 = engine.getTemplate(
                File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier() + "_1."
                        + VelocityType.CONTENT.fileExtension);
        org.apache.velocity.Template tesp1 = engine.getTemplate(
                File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier() + "_"
                        + spanishLanguage.getId() + "."
                        + VelocityType.CONTENT.fileExtension);

        Context ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        StringWriter writer=new StringWriter();
        teng1.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Initial code",writer.toString());
        ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        writer=new StringWriter();
        tesp1.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Initial code",writer.toString());

        Contentlet w2=contentletAPI.checkout(w.getInode(), user, false);
        w2.setStringProperty("code", "Modified Code to make templates different");
        w2 = contentletAPI.checkin(w2, user, false);
        contentletAPI.publish(w2, user, false);
        contentletAPI.isInodeIndexed(w2.getInode(),true);
        VelocityResourceKey key = new VelocityResourceKey(w2, PageMode.LIVE,
                spanishLanguage.getId());
        CacheLocator.getVeloctyResourceCache().remove(key);

        // now if everything have been cleared correctly those should match again
        org.apache.velocity.Template teng3 = engine.getTemplate(
                File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier() + "_1."
                        + VelocityType.CONTENT.fileExtension);
        org.apache.velocity.Template tesp3 = engine.getTemplate(
                File.separator + PageMode.LIVE.name() + File.separator + w.getIdentifier() + "_"
                        + spanishLanguage.getId() + "."
                        + VelocityType.CONTENT.fileExtension);
        ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        writer=new StringWriter();
        teng3.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Modified Code to make templates different",writer.toString());
        ctx = VelocityUtil.getWebContext(requestProxy, responseProxy);
        writer=new StringWriter();
        tesp3.merge(ctx, writer);
        contentTester.render(new InternalContextAdapterImpl(ctx), writer);
        assertEquals("code:Modified Code to make templates different",writer.toString());

        // clean up
        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",false);
    }
    @Test
    public void testFileCopyOnSecondLanguageVersion()
            throws DotDataException, DotSecurityException {

        final String contentTypeName = "structure2709" + System.currentTimeMillis();

    	// Structure
        Structure testStructure = new Structure();

        testStructure.setDefaultStructure( false );
        testStructure.setDescription(contentTypeName);
        testStructure.setFixed( false );
        testStructure.setIDate( new Date() );
        testStructure.setName(contentTypeName);
        testStructure.setOwner( user.getUserId() );
        testStructure.setDetailPage( "" );
        testStructure.setStructureType( BaseContentType.CONTENT.getType() );
        testStructure.setType( "structure" );
        testStructure.setVelocityVarName(contentTypeName);

        StructureFactory.saveStructure( testStructure );

        Permission permissionRead = new Permission( testStructure.getInode(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_READ );
        Permission permissionEdit = new Permission( testStructure.getInode(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_EDIT );
        Permission permissionWrite = new Permission( testStructure.getInode(), APILocator.getRoleAPI().loadCMSAnonymousRole().getId(), PermissionAPI.PERMISSION_WRITE );

        APILocator.getPermissionAPI().save( permissionRead, testStructure, user, false );
        APILocator.getPermissionAPI().save( permissionEdit, testStructure, user, false );
        APILocator.getPermissionAPI().save( permissionWrite, testStructure, user, false );


        // Fields

        // title
        Field title = new Field();
        title.setFieldName("testTitle2709");
        title.setFieldType(FieldType.TEXT.toString());
        title.setListed(true);
        title.setRequired(true);
        title.setSearchable(true);
        title.setStructureInode(testStructure.getInode());
        title.setType("field");
        title.setValues("");
        title.setVelocityVarName("testTitle2709");
        title.setIndexed(true);
        title.setFieldContentlet("text4");
        FieldFactory.saveField( title );

        // file
        Field file = new Field();
        file.setFieldName("testFile2709");
        file.setFieldType(FieldType.FILE.toString());
        file.setListed(true);
        file.setRequired(true);
        file.setSearchable(true);
        file.setStructureInode(testStructure.getInode());
        file.setType("field");
        file.setValues("");
        file.setVelocityVarName("testFile2709");
        file.setIndexed(true);
        file.setFieldContentlet("text1");
        FieldFactory.saveField( file );

        // ENGLISH CONTENT
        Contentlet englishContent = new Contentlet();
        englishContent.setReviewInterval( "1m" );
        englishContent.setStructureInode( testStructure.getInode() );
        englishContent.setLanguageId(1);

        //Create a test file asset
        Contentlet fileA = TestDataUtils
                .getFileAssetContent(true, languageAPI.getDefaultLanguage().getId());

        contentletAPI.setContentletProperty( englishContent, title, "englishTitle2709" );
        contentletAPI.setContentletProperty( englishContent, file, fileA.getInode() );

        englishContent = contentletAPI.checkin( englishContent, null, APILocator.getPermissionAPI().getPermissions( testStructure ), user, false );

        // SPANISH CONTENT
		Contentlet spanishContent = new Contentlet();
		spanishContent.setReviewInterval("1m");
		spanishContent.setStructureInode(testStructure.getInode());
        spanishContent.setLanguageId(spanishLanguage.getId());
		spanishContent.setIdentifier(englishContent.getIdentifier());

		contentletAPI.setContentletProperty( spanishContent, title, "spanishTitle2709" );
		contentletAPI.setContentletProperty( spanishContent, file, fileA.getInode() );

		spanishContent = contentletAPI.checkin( spanishContent, null, APILocator.getPermissionAPI().getPermissions( testStructure ), user, false );
		Object retrivedFile = spanishContent.get("testFile2709");
		assertTrue(retrivedFile!=null);
        try{
        	HibernateUtil.startTransaction();
        	APILocator.getStructureAPI().delete(testStructure, user);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(ContentletAPITest.class, e.getMessage());
        }


    }

    @Test
    public void newFileAssetLanguageDifferentThanDefault() throws DotSecurityException, DotDataException, IOException {
        long spanish = spanishLanguage.getId();
        Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");

        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder, file);
        Contentlet fileInSpanish = fileAssetDataGen.languageId(spanish).nextPersisted();
        Contentlet
            result =
            contentletAPI.findContentletByIdentifier(fileInSpanish.getIdentifier(), false, spanish, user, false);
        assertEquals(fileInSpanish.getInode(), result.getInode());

        fileAssetDataGen.remove(fileInSpanish);
    }
    
    @Test
    public void newVersionFileAssetLanguageDifferentThanDefault() throws DotDataException, IOException, DotSecurityException{
    	int english = 1;
        long spanish = spanishLanguage.getId();
    	
    	Folder folder = APILocator.getFolderAPI().findSystemFolder();
    	java.io.File file = java.io.File.createTempFile("file", ".txt");
        FileUtil.write(file, "helloworld");
        
    	FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
    	Contentlet fileAsset = fileAssetDataGen.languageId(english).nextPersisted();
  	  
    	Contentlet contentletSpanish = contentletAPI.findContentletByIdentifier(fileAsset.getIdentifier(), false, english, user, false);
    	contentletSpanish.setInode("");
    	contentletSpanish.setLanguageId(spanish);
    	contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);
  	  
    	Contentlet resultSpanish = contentletAPI.findContentletByIdentifier(fileAsset.getIdentifier(), false, spanish, user, false);
    	assertNotNull( resultSpanish );

        Contentlet resultEnglish = contentletAPI.findContentletByIdentifier(fileAsset.getIdentifier(), false, english, user, false);
    	fileAssetDataGen.remove(resultSpanish);
        fileAssetDataGen.remove(resultEnglish);
    }
    
    /**
     * Deletes a list of contents
     * @throws Exception
     */
    @Test
    public void deleteMultipleContents() throws Exception { // https://github.com/dotCMS/core/issues/7678

    	// languages
    	int english = 1;
        long spanish = spanishLanguage.getId();

        // new template
        Template template = new TemplateDataGen().nextPersisted();
        // new test folder
		Folder testFolder = new FolderDataGen().nextPersisted();
		// sample pages
		HTMLPageAsset pageEnglish1 = new HTMLPageDataGen(testFolder, template).languageId(english).nextPersisted();
		HTMLPageAsset pageEnglish2 = new HTMLPageDataGen(testFolder, template).languageId(english).nextPersisted();
		contentletAPI.publish(pageEnglish1, user, false);
		contentletAPI.publish(pageEnglish2, user, false);
        // delete counter
        int deleted = 0;
        // Page list
        List<HTMLPageAsset> liveHTMLPages = new ArrayList<HTMLPageAsset>();
        // List of contentlets created for this test.
        List<Contentlet> contentletsCreated = new ArrayList<Contentlet>();
        
        liveHTMLPages.add(pageEnglish1);
        liveHTMLPages.add(pageEnglish2);
               
        //We need to create a new copy of pages for Spanish.
        for(HTMLPageAsset liveHTMLPage : liveHTMLPages){
            Contentlet htmlPageContentlet = APILocator.getContentletAPI().find( liveHTMLPage.getInode(), user, false );

            //As a copy we need to remove this info to do a clean checkin.
            htmlPageContentlet.getMap().remove("modDate");
            htmlPageContentlet.getMap().remove("lastReview");
            htmlPageContentlet.getMap().remove("owner");
            htmlPageContentlet.getMap().remove("modUser");

            htmlPageContentlet.getMap().put("inode", "");
            htmlPageContentlet.getMap().put("languageId", new Long(spanish));

            //Checkin and Publish.
            Contentlet working = APILocator.getContentletAPI().checkin(htmlPageContentlet, user, false);
            APILocator.getContentletAPI().publish(working, user, false);
            APILocator.getContentletAPI().isInodeIndexed(working.getInode(), true);

            contentletsCreated.add(working);
        }

        //Now remove all the pages that we created for this tests.
        APILocator.getContentletAPI().unpublish(contentletsCreated, user, false);
        APILocator.getContentletAPI().archive(contentletsCreated, user, false);
        APILocator.getContentletAPI().delete(contentletsCreated, user, false);
        
        for(Contentlet contentlet: contentletsCreated){
        	if(APILocator.getContentletAPI().find(contentlet.getInode(), user, false) == null){
        		deleted++;
        	}
        }
        // 2 Spanish pages created, 2 should have been deleted
        assertEquals(2, deleted);
        
        List<Contentlet> liveEnglish = new ArrayList<Contentlet>();
        for(IHTMLPage page:liveHTMLPages){
        	liveEnglish.add(APILocator.getContentletAPI().find( page.getInode(), user, false ));
        }
        
        APILocator.getContentletAPI().unpublish(liveEnglish, user, false);
        APILocator.getContentletAPI().archive(liveEnglish, user, false);
        APILocator.getContentletAPI().delete(liveEnglish, user, false);
        
        deleted = 0;
        for(Contentlet contentlet: liveEnglish){
        	if(APILocator.getContentletAPI().find(contentlet.getInode(), user, false) == null){
        		deleted++;
        	}
        }
        
        // 2 English pages created, 2 should have been deleted
        assertEquals(2, deleted);

        // dispose other objects
		FolderDataGen.remove(testFolder);
		TemplateDataGen.remove(template);
		
    }

    /*
    Creates one content with 3 versions in English and 3 versions Spanish. Delete the Spanish one,
    should delete all the versions in Spanish not only the live/working version.
     */
    @Test
    public void testDelete_GivenMultiLangMultiVersionContent_WhenDeletingOneSpanishVersion_ShouldDeleteAllSpanishVersions() throws Exception{
        // languages
        int english = 1;
        long spanish = spanishLanguage.getId();

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField1 = null;
        com.dotcms.contenttype.model.field.Field textField2 = null;

        Contentlet contentletEnglish = null;
        Contentlet contentletSpanish = null;

        try {
            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("Test ContentType Two Text Fields")
                    .host(defaultHost.getIdentifier())
                    .name("Test ContentType Two Text Fields")
                    .owner("owner")
                    .variable("testContentTypeWithTwoTextFields")
                    .build();

            contentType = contentTypeAPI.save(contentType);

            //Creating Text Field.
            textField1 = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField1 = fieldAPI.save(textField1, user);

            //Creating Text Field.
            textField2 = ImmutableTextField.builder()
                    .name("Body")
                    .variable("body")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField2 = fieldAPI.save(textField2, user);

            contentletEnglish = new ContentletDataGen(contentType.id()).languageId(english).nextPersisted();
            //new Version
            contentletEnglish = contentletAPI.find(contentletEnglish.getInode(),user,false);
            contentletEnglish.setInode("");
            contentletEnglish = contentletAPI.checkin(contentletEnglish,user,false);
            //new Version
            contentletEnglish = contentletAPI.find(contentletEnglish.getInode(),user,false);
            contentletEnglish.setInode("");
            contentletEnglish = contentletAPI.checkin(contentletEnglish,user,false);

            Identifier contentletIdentifier = APILocator.getIdentifierAPI().find(contentletEnglish.getIdentifier());

            int quantityVersions = contentletAPI.findAllVersions(contentletIdentifier,user,false).size();

            assertEquals(3,quantityVersions);

            contentletSpanish = contentletAPI.find(contentletEnglish.getInode(),user,false);
            contentletSpanish.setInode("");
            contentletSpanish.setLanguageId(spanish);
            contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);
            //new Version
            contentletSpanish = contentletAPI.find(contentletSpanish.getInode(),user,false);
            contentletSpanish.setInode("");
            contentletSpanish.setLanguageId(spanish);
            contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);
            //new Version
            contentletSpanish = contentletAPI.find(contentletSpanish.getInode(),user,false);
            contentletSpanish.setInode("");
            contentletSpanish.setLanguageId(spanish);
            contentletSpanish = contentletAPI.checkin(contentletSpanish, user, false);

            quantityVersions = contentletAPI.findAllVersions(contentletIdentifier,user,false).size();

            assertEquals(6,quantityVersions);

            contentletAPI.archive(contentletSpanish,user,false);
            contentletAPI.delete(contentletSpanish,user,false);

            quantityVersions = contentletAPI.findAllVersions(contentletIdentifier,user,false).size();

            assertEquals(3,quantityVersions);

        }finally {
            contentTypeAPI.delete(contentType);
        }

    }

    /**
     * This JUnit is to check the fix on Issue 10797 (https://github.com/dotCMS/core/issues/10797)
     * It executes the following:
     * 1) create a new structure
     * 2) create a new field
     * 3) create a contentlet
     * 4) set the contentlet property
     * 5) check the contentlet
     * 6) deletes it all in the end
     *
     * @throws Exception Any exception that may happen
     */
    @Test
    public void test_validateContentlet_contentWithTabDividerField() throws Exception {
        Structure testStructure = null;
        Field tabDividerField = null;

        try {
            // Create test structure
            testStructure = createStructure("Tab Divider Test Structure_" + String.valueOf(new Date().getTime()) + "tab_divider", "tab_divider_test_structure_" + String.valueOf(new Date().getTime()) + "tab_divider");

            // Create tab divider field
            tabDividerField = new Field("JUnit Test TabDividerField", FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, testStructure, false, true, true, 1, false, false, false);
            tabDividerField = FieldFactory.saveField(tabDividerField);

            // Create the test contentlet
            Contentlet testContentlet = new Contentlet();
            testContentlet.setStructureInode(testStructure.getInode());

            // Set the contentlet property
            contentletAPI.setContentletProperty(testContentlet, tabDividerField, "tabDividerFieldValue");

            // Checking the contentlet
            testContentlet.setIndexPolicy(IndexPolicy.FORCE);
            testContentlet = contentletAPI.checkin(testContentlet, user, false);
        } catch (Exception ex) {
            Logger.error(this, "An error occurred during test_validateContentlet_contentWithTabDividerField", ex);
            throw ex;
        } finally {
            // Delete field
            FieldFactory.deleteField(tabDividerField);

            // Delete structure
            APILocator.getStructureAPI().delete(testStructure, user);
        }
    }

    /**
     * https://github.com/dotCMS/core/issues/11950
     */
    @Test
    public void testContentWithTwoBinaryFieldsAndSameFile_afterCheckinShouldContainBothFields() {

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField1 = null;
        com.dotcms.contenttype.model.field.Field binaryField2 = null;

        Contentlet contentlet = null;

        try {
            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("Test ContentType Two Fields")
                    .host(defaultHost.getIdentifier())
                    .name("Test ContentType Two Fields")
                    .owner("owner")
                    .variable("testContentTypeWithTwoBinaryFields")
                    .build();

            contentType = contentTypeAPI.save(contentType);

            //Save Fields. 1. Text, 2. Binary, 3. Binary.
            //Creating Text Field.
            textField = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField = fieldAPI.save(textField, user);

            //Creating First Binary Field.
            binaryField1 = ImmutableBinaryField.builder()
                    .name("Image 1")
                    .variable("image1")
                    .contentTypeId(contentType.id())
                    .build();

            binaryField1 = fieldAPI.save(binaryField1, user);

            //Creating Second Binary Field.
            binaryField2 = ImmutableBinaryField.builder()
                    .name("Image 2")
                    .variable("image2")
                    .contentTypeId(contentType.id())
                    .build();

            binaryField2 = fieldAPI.save(binaryField2, user);

            //Creating a temporary File to use in the binary fields.
            File imageFile = temporaryFolder.newFile("ImageFile.png");
            writeTextIntoFile(imageFile, "This is the same image");

            contentlet = new Contentlet();
            contentlet.setStructureInode(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

            contentlet.setStringProperty(textField.variable(), "Test Content with Same Image");
            contentlet.setBinary(binaryField1.variable(), imageFile);
            contentlet.setBinary(binaryField2.variable(), imageFile);

            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet = contentletAPI.checkin(contentlet, user, false);

            //Check that the properties still exist.
            assertTrue(contentlet.getMap().containsKey(binaryField1.variable()));
            assertTrue(contentlet.getMap().containsKey(binaryField2.variable()));

            //Check that the properties have value.
            assertTrue(UtilMethods.isSet(contentlet.getMap().get(binaryField1.variable())));
            assertTrue(UtilMethods.isSet(contentlet.getMap().get(binaryField2.variable())));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                //Delete Contentlet.
                if (contentlet != null) {
                    contentletAPI.archive(contentlet, user, false);
                    contentletAPI.delete(contentlet, user, false);
                }
                //Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                if (binaryField1 != null) {
                    fieldAPI.delete(binaryField1);
                }
                if (binaryField2 != null) {
                    fieldAPI.delete(binaryField2);
                }
                //Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

    }

    /**
     * This case should run once this ticket https://github.com/dotCMS/core/issues/12116 is solved
     */
    @Test
    @Ignore
    public void test_saveMultilingualFileAssetBasedOnLegacyFile_shouldKeepBinaryFile()
        throws IOException, DotSecurityException, DotDataException {

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField = null;

        File imageFile;
        FileAssetDataGen fileAssetDataGen = null;
        Contentlet initialContent = null;
        Contentlet spanishContent = null;
        Contentlet englishContent = null;

        try {

            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("ContentType for Legacy File")
                .host(defaultHost.getIdentifier())
                .name("ContentType for Legacy File")
                .owner("owner")
                .variable("testContentTypeForLegacyFile")
                .build();

            contentType = contentTypeAPI.save(contentType);

            //Save Fields. 1. Text, 2. Binary
            //Creating Text Field.
            textField = ImmutableTextField.builder()
                .name("Title")
                .variable("title")
                .contentTypeId(contentType.id())
                .dataType(DataTypes.TEXT)
                .build();

            textField = fieldAPI.save(textField, user);

            //Creating First Binary Field.
            binaryField = ImmutableBinaryField.builder()
                .name("File")
                .variable("file")
                .contentTypeId(contentType.id())
                .build();

            binaryField = fieldAPI.save(binaryField, user);

            //Creating a temporary binary file
            imageFile = temporaryFolder.newFile("BinaryFile.txt");
            writeTextIntoFile(imageFile, "This is the same file");

            initialContent = new Contentlet();
            initialContent.setStructureInode(contentType.inode());
            initialContent.setLanguageId(languageAPI.getDefaultLanguage().getId());

            initialContent.setStringProperty(textField.variable(), "Test Content with Same File");
            initialContent.setBinary(binaryField.variable(), imageFile);

            //Saving initial contentlet
            initialContent = contentletAPI.checkin(initialContent, user, false);

            //File assets creation based on the initial content
            fileAssetDataGen = new FileAssetDataGen(testFolder, initialContent.getBinary(binaryField.variable()));

            //Creating file asset content in Spanish
            spanishContent = fileAssetDataGen.languageId(spanishLanguage.getId()).nextPersisted();

            //Creating content version in English
            englishContent = contentletAPI.checkout(spanishContent.getInode(), user, false);
            englishContent.setLanguageId(1);
            englishContent = contentletAPI.checkin(englishContent, user, false);

            //Check that the properties still exist.
            assertTrue(initialContent.getMap().containsKey(binaryField.variable()));
            assertTrue(spanishContent.getMap().containsKey(FileAssetAPI.BINARY_FIELD));
            assertTrue(englishContent.getMap().containsKey(FileAssetAPI.BINARY_FIELD));

            //Check that the properties have value.
            assertTrue(UtilMethods.isSet(initialContent.getMap().get(binaryField.variable())));
            assertTrue(UtilMethods.isSet(spanishContent.getMap().get(FileAssetAPI.BINARY_FIELD)));
            assertTrue(UtilMethods.isSet(englishContent.getMap().get(FileAssetAPI.BINARY_FIELD)));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {

            try {
                //Delete initial Contentlet.
                if (initialContent != null) {
                    contentletAPI.archive(initialContent, user, false);
                    contentletAPI.delete(initialContent, user, false);
                }
                //Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                if (binaryField != null) {
                    fieldAPI.delete(binaryField);
                }
                //Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }

                if (fileAssetDataGen != null) {

                    if (spanishContent != null) {
                        fileAssetDataGen.remove(spanishContent);
                    }

                    if (englishContent != null) {
                        fileAssetDataGen.remove(englishContent);
                    }

                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }


    /*
     * https://github.com/dotCMS/core/issues/11978
     * 
     * Creates a new Content Type with a DateTimeField and sets it as Expire Field, saves a new Content a checks that 
     * the value of the expire field is set and retrieve correctly
     */
    @Test
    public void contentOnlyWithExpireFieldTest() throws Exception{
    	ContentTypeAPIImpl contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
		long time = System.currentTimeMillis();

		ContentType contentType = ContentTypeBuilder.builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
				.description("ContentTypeWithPublishExpireFields " + time).folder(FolderAPI.SYSTEM_FOLDER)
				.host(Host.SYSTEM_HOST).name("ContentTypeWithPublishExpireFields " + time)
				.owner(APILocator.systemUser().toString()).variable("CTVariable11").expireDateVar("expireDate").build();
		contentType = contentTypeApi.save(contentType);

		assertThat("ContentType exists", contentTypeApi.find(contentType.inode()) != null);

		List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(contentType.fields());

		com.dotcms.contenttype.model.field.Field fieldToSave = FieldBuilder.builder(DateTimeField.class).name("Expire Date").variable("expireDate")
				.contentTypeId(contentType.id()).dataType(DataTypes.DATE).indexed(true).build();
		fields.add(fieldToSave);

		contentType = contentTypeApi.save(contentType, fields);
		
		Contentlet contentlet = new Contentlet();
		contentlet.setStructureInode(contentType.inode());
        contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

        contentlet.setDateProperty(fieldToSave.variable(), new Date(new Date().getTime()+60000L));

        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet = contentletAPI.checkin(contentlet, user, false);

        contentlet = contentletAPI.find(contentlet.getInode(), user, false);
		Date expireDate = contentlet.getDateProperty("expireDate");
        
        assertNotNull(expireDate);
		
		// Deleting content type.
		contentTypeApi.delete(contentType);
    }

    /**
     * This test will:
     * --- Create a content type called "Nested".
     * --- Add only 1 Text field called Title
     * --- Create a Content "A". Save/publish it.
     * --- Create a Content "B". Save/publish it.
     * --- Create a Content "C". Save/publish it.
     * --- Create a 1:N Relationship, Parent and Child same Content Type: Nested
     * --- Relate Content: Parent: A, Child B.
     * --- Relate Content: Parent: B, Child C.
     * --- Edit Content A, update title to "ABC"
     *
     * Before the fix we were getting an exception when editing content A because validateContentlet
     * validates that if there's a 1-N relationship the parent content can't relate to a child
     * that already has a parent; but we were pulling other related content, not just the parents.
     *
     * https://github.com/dotCMS/core/issues/10656
     */
    @Test
    public void test_validateContentlet_noErrors_whenRelationChainSameContentType() {
        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;

        Contentlet contentletA = null;
        Contentlet contentletB = null;
        Contentlet contentletC = null;

        Relationship relationShip = null;

        try {
            // Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                    .description("Nested" + System.currentTimeMillis())
                    .host(defaultHost.getIdentifier())
                    .name("Nested" + System.currentTimeMillis())
                    .owner("owner")
                    .variable("nested" + System.currentTimeMillis())
                    .build();

            contentType = contentTypeAPI.save(contentType);

            // Save Fields. 1. Text
            // Creating Text Field: Title.
            textField = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            textField = fieldAPI.save(textField, user);

            contentletA = new Contentlet();
            contentletA.setStructureInode(contentType.inode());
            contentletA.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentletA.setStringProperty(textField.variable(), "A");
            contentletA.setIndexPolicy(IndexPolicy.FORCE);
            contentletA.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletA = contentletAPI.checkin(contentletA, user, false);

            contentletB = new Contentlet();
            contentletB.setStructureInode(contentType.inode());
            contentletB.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentletB.setStringProperty(textField.variable(), "B");
            contentletB.setIndexPolicy(IndexPolicy.FORCE);
            contentletB.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletB = contentletAPI.checkin(contentletB, user, false);

            contentletC = new Contentlet();
            contentletC.setStructureInode(contentType.inode());
            contentletC.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentletC.setStringProperty(textField.variable(), "B");
            contentletC.setIndexPolicy(IndexPolicy.FORCE);
            contentletC.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletC = contentletAPI.checkin(contentletC, user, false);

            relationShip = createRelationShip(contentType.inode(),
                    contentType.inode(), false);

            // Relate the content.
            contentletAPI
                    .relateContent(contentletA, relationShip, Lists.newArrayList(contentletB), user,
                            false);
            contentletAPI
                    .relateContent(contentletB, relationShip, Lists.newArrayList(contentletC), user,
                            false);

            Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
            relationshipListMap.put(relationShip, Lists.newArrayList(contentletB));

            contentletA = contentletAPI.checkout(contentletA.getInode(), user, false);
            contentletA.setStringProperty(textField.variable(), "ABC");
            contentletA.setIndexPolicy(IndexPolicy.FORCE);
            contentletA.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentletA = contentletAPI.checkin(contentletA, relationshipListMap, user, false);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                // Delete Relationship.
                if (relationShip != null) {
                    relationshipAPI.delete(relationShip);
                }
                // Delete Contentlet.
                if (contentletA != null) {
                    contentletAPI.archive(contentletA, user, false);
                    contentletAPI.delete(contentletA, user, false);
                }
                if (contentletB != null) {
                    contentletAPI.archive(contentletB, user, false);
                    contentletAPI.delete(contentletB, user, false);
                }
                if (contentletC != null) {
                    contentletAPI.archive(contentletC, user, false);
                    contentletAPI.delete(contentletC, user, false);
                }
                // Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                // Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ShouldDeletePreviousBinary_WhenBinaryIsUpdated()
            throws DotSecurityException, DotDataException, IOException {

        final String FILE_V1_NAME = "textFileVersion1.txt";
        final String FILE_V2_NAME = "textFileVersion2.txt";
        final String FILE_V2_CONTENT = "textFileVersion2 CONTENT";
        ContentType typeWithBinary = null;

        try {
            typeWithBinary = createContentType("testCheckinWithoutVersioning", BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field textField = createTextField("Title", typeWithBinary.id());
            com.dotcms.contenttype.model.field.Field binaryField = createBinaryField("File", typeWithBinary.id());
            File textFileVersion1 = createTempFileWithText(FILE_V1_NAME, FILE_V1_NAME);
            Map<String, Object> fieldValues = map(textField.variable(), "contentV1",
                    binaryField.variable(), textFileVersion1);
            Contentlet contentletWithBinary = createContentWithFieldValues(typeWithBinary.id(), fieldValues);

            // let's verify that newly saved file exists
            assertTrue(getBinaryAsset(contentletWithBinary.getInode(), binaryField.variable(), FILE_V1_NAME).exists());

            File textFileVersion2 = createTempFileWithText(FILE_V2_NAME, FILE_V2_CONTENT);
            // replace old binary with new one
            contentletWithBinary.setBinary(binaryField.variable(), textFileVersion2);
            Contentlet contentWithoutVersioning = contentletAPI.checkinWithoutVersioning(contentletWithBinary,
                    new HashMap<>(), null, permissionAPI.getPermissions(contentletWithBinary), user, false);

            // we've just checkedIn without versioning, so old binary should not exist
            assertFalse(getBinaryAsset(contentletWithBinary.getInode(), binaryField.variable(), FILE_V1_NAME).exists());

            File newBinary = getBinaryAsset(contentWithoutVersioning.getInode(), binaryField.variable(), FILE_V2_NAME);
            // new binary should exist
            assertTrue(newBinary.exists());
            // and content should be the expected
            BufferedReader reader = Files.newReader(newBinary, Charset.defaultCharset());
            String fileContent = reader.readLine();
            assertEquals(fileContent, FILE_V2_CONTENT);

        } finally {
            if(typeWithBinary!=null) contentTypeAPI.delete(typeWithBinary);
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ShouldPreserveBinary_WhenOtherFieldsAreUpdated()
            throws DotDataException, DotSecurityException, IOException {
        final String BINARY_NAME = "testCheckinWithoutVersioningBinary.txt";
        final String BINARY_CONTENT = "testCheckinWithoutVersioningBinary CONTENT";
        ContentType typeWithBinary = null;

        try {
            typeWithBinary = createContentType("testCheckinWithoutVersioning", BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field textField = createTextField("Title", typeWithBinary.id());
            com.dotcms.contenttype.model.field.Field binaryField = createBinaryField("File", typeWithBinary.id());
            File textFileVersion1 = createTempFileWithText(BINARY_NAME, BINARY_CONTENT);
            Map<String, Object> fieldValues = map(textField.variable(), "contentV1",
                    binaryField.variable(), textFileVersion1);
            Contentlet contentletWithBinary = createContentWithFieldValues(typeWithBinary.id(), fieldValues);

            // let's verify that newly saved file exists
            assertTrue(getBinaryAsset(contentletWithBinary.getInode(), binaryField.variable(), BINARY_NAME).exists());

            //let's update a field different from the binary
            contentletWithBinary.setStringProperty(textField.variable(), "contentV2");
            Contentlet contentWithoutVersioning = contentletAPI.checkinWithoutVersioning(contentletWithBinary,
                    new HashMap<>(), null, permissionAPI.getPermissions(contentletWithBinary), user, false);

            // let's verify the binary is still in FS
            File binaryFromAssetsDir = getBinaryAsset(contentWithoutVersioning.getInode(), binaryField.variable(), BINARY_NAME);
            assertTrue(binaryFromAssetsDir.exists());

            // let's also verify file content remains the same
            BufferedReader reader = Files.newReader(binaryFromAssetsDir, Charset.defaultCharset());
            String fileContent = reader.readLine();
            assertEquals(fileContent, BINARY_CONTENT);

            // let's verify the reference is still ok
            File binaryFromContentlet = contentWithoutVersioning.getBinary(binaryField.variable());
            assertEquals(binaryFromContentlet.getName(), BINARY_NAME);

        } finally {
            if(typeWithBinary!=null) contentTypeAPI.delete(typeWithBinary);
        }

    }

    @Test(expected = DotContentletValidationException.class)
    public void testUniqueTextFieldWithDataTypeWholeNumber()
            throws DotDataException, DotSecurityException {
        String contentTypeName = "contentTypeTxtField" + System.currentTimeMillis();
        ContentType contentType = null;
        try{
            contentType = createContentType(contentTypeName, BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field field =  ImmutableTextField.builder()
                    .name("Whole Number Unique")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.INTEGER)
                    .unique(true)
                    .build();
            field = fieldAPI.save(field, user);

            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet.setLongProperty(field.variable(),1);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet = contentletAPI.checkin(contentlet, user, false);
            contentlet = contentletAPI.find(contentlet.getInode(), user, false);

            Contentlet contentlet2 = new Contentlet();
            contentlet2.setContentTypeId(contentType.inode());
            contentlet2.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet2.setLongProperty(field.variable(),1);
            contentlet2 = contentletAPI.checkin(contentlet2, user, false);


        }finally{
            if(contentType != null) contentTypeAPI.delete(contentType);
        }
    }

    private static class testCaseUniqueTextField{
        String fieldValue1;

        testCaseUniqueTextField(final String fieldValue1){
            this.fieldValue1 = fieldValue1;
        }
    }

    @DataProvider
    public static Object[] testCasesUniqueTextField() throws Exception{
        return new Object[]{
                new testCaseUniqueTextField("diffLang"),//test for same value diff lang
                new testCaseUniqueTextField("\"A+\" Student"),//test for special chars
                new testCaseUniqueTextField("CASEINSENSITIVE")//test for case insensitive
        };
    }

    /**
     * This test is for the unique feature of a text field.
     * It creates a content type with a text field marked as unique,
     * and creates a contentlet in English, then a contentlet in Spanish
     * (unique value does not matter the lang)
     * and finally another contentlet in English and this is when it throws the exception
     * (value it's in lowercase because needs to be case insentitive)
     */
    @Test(expected = DotContentletValidationException.class)
    @UseDataProvider("testCasesUniqueTextField")
    public void testUniqueTextFieldContentlets(final testCaseUniqueTextField testCase)
            throws DotDataException, DotSecurityException {
        String contentTypeName = "contentTypeTxtField" + System.currentTimeMillis();
        ContentType contentType = null;
        try{
            contentType = createContentType(contentTypeName, BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field field =  ImmutableTextField.builder()
                    .name("Text Unique")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .unique(true)
                    .build();
            field = fieldAPI.save(field, user);

            //Contentlet in English
            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet.setStringProperty(field.variable(),testCase.fieldValue1);
            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet = contentletAPI.checkin(contentlet, user, false);
            contentlet = contentletAPI.find(contentlet.getInode(), user, false);

            //Contentlet in Spanish (should not be an issue since the unique is per lang)
            Contentlet contentlet2 = new Contentlet();
            contentlet2.setContentTypeId(contentType.inode());
            contentlet2.setLanguageId(spanishLanguage.getId());
            contentlet2.setStringProperty(field.variable(),testCase.fieldValue1);
            contentlet2.setIndexPolicy(IndexPolicy.FORCE);
            contentlet2 = contentletAPI.checkin(contentlet2, user, false);

            //Contentlet in English (throws the error)
            Contentlet contentlet3 = new Contentlet();
            contentlet3.setContentTypeId(contentType.inode());
            contentlet3.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet3.setStringProperty(field.variable(),testCase.fieldValue1.toLowerCase());
            contentlet3.setIndexPolicy(IndexPolicy.FORCE);
            contentlet3 = contentletAPI.checkin(contentlet3, user, false);


        }finally{
            if(contentType != null) contentTypeAPI.delete(contentType);
        }
    }

    /**
     * Creates a contentlet with a working and a live version with diff values,
     * and tries to create another contentlet with the live value this throws
     * the exception (before we were only checking on the working index).
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = DotContentletValidationException.class)
    public void testUniqueTextFieldLiveAndWorkingWithDiffValues()
            throws DotDataException, DotSecurityException {
        final String contentTypeName = "contentTypeTxtField" + System.currentTimeMillis();
        final String fieldValueLive = "Live Value";
        final String fieldValueWorking = "Working Value";
        ContentType contentType = null;
        try{
            contentType = createContentType(contentTypeName, BaseContentType.CONTENT);
            com.dotcms.contenttype.model.field.Field field =  ImmutableTextField.builder()
                    .name("Text Unique")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .unique(true)
                    .build();
            field = fieldAPI.save(field, user);

            //Contentlet in English
            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(contentType.inode());
            contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet.setStringProperty(field.variable(),fieldValueLive);
            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentlet = contentletAPI.checkin(contentlet, user, false);
            contentletAPI.publish(contentlet,user,false);


            contentlet = contentletAPI.checkout(contentlet.getInode(),user,false);
            contentlet.setInode("");
            contentlet.setStringProperty(field.variable(),fieldValueWorking);
            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentletAPI.checkin(contentlet, user, false);

            //Contentlet in English (throws the error)
            Contentlet contentlet3 = new Contentlet();
            contentlet3.setContentTypeId(contentType.inode());
            contentlet3.setLanguageId(languageAPI.getDefaultLanguage().getId());
            contentlet3.setStringProperty(field.variable(),fieldValueLive);
            contentlet3.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentletAPI.checkin(contentlet3, user, false);

        }finally{
            if(contentType != null) contentTypeAPI.delete(contentType);
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats,
     * List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles)
     *
     */
    @Test
    public void testCheckin1_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent, (ContentletRelationships) null, categories,
                null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user, false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent, (ContentletRelationships) null,
                null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            if(blogContent!=null && UtilMethods.isSet(blogContent.getIdentifier())) {
                contentletAPI.destroy(blogContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats,
     * List<Permission> selectedPermissions, User user, boolean respectFrontendRoles, boolean generateSystemEvent)
     *
     */
    @Test
    public void testCheckin2_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent =
                contentletAPI.checkin(blogContent, (Map<Relationship, List<Contentlet>>) null, categories,
                    null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user, false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent, null,
                null, null, user, false, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            if(blogContent!=null && UtilMethods.isSet(blogContent.getIdentifier()))  {
                contentletAPI.destroy(blogContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,
     * List<Permission> permissions, User user,boolean respectFrontendRoles)
     *
     */
    @Test
    public void testCheckin3_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent, (Map<Relationship, List<Contentlet>>) null, categories,
                null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user, false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                (Map<Relationship, List<Contentlet>>) null, null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            if(blogContent!=null && UtilMethods.isSet(blogContent.getIdentifier()))  {
                contentletAPI.destroy(blogContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats,
     * User user,boolean respectFrontendRoles)
     *
     */
    @Test
    public void testCheckin4_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent, (Map<Relationship, List<Contentlet>>) null, categories,
                null, user, false);

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user, false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                (Map<Relationship, List<Contentlet>>) null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            if(blogContent!=null && UtilMethods.isSet(blogContent.getIdentifier()))  {
                contentletAPI.destroy(blogContent, user, false);
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithCats_NullCats_ShouldKeepExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet blogContent = null;

        try {
            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            blogContent = contentletAPI.checkin(blogContent, (Map<Relationship, List<Contentlet>>) null, categories,
                null, user, false);

            Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(blogContent,
                (Map<Relationship, List<Contentlet>>) null, null, null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertTrue(existingCats.containsAll(categories));
        } finally {
            if(blogContent!=null && UtilMethods.isSet(blogContent.getIdentifier()))  {
                contentletAPI.destroy(blogContent, user, false);
            }
        }
    }

    /**
     * Test checkin with a non-existing contentlet identifier, that should fail
     *
     */
    @Test(expected = DotHibernateException.class)
    public void testCheckin_Non_Existing_Identifier_With_Validate_Should_FAIL()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());
            newsContent.setIdentifier(UUIDGenerator.generateUuid());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (ContentletRelationships) null, categories,
                    null, user,false);

            fail("Should throw a constrain exception for an unexisting id");
        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier()) && UtilMethods.isSet(newsContent.getInode())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    /**
     * Test checkin with a non-existing contentlet identifier, that should not fail since the non validate is activated
     *
     */
    @Test
    public void testCheckin_Non_Existing_Identifier_With_Not_Validate_Success()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());
            String identifier = UUIDGenerator.generateUuid();
            newsContent.setIdentifier(identifier);
            newsContent.setInode(UUIDGenerator.generateUuid());
            newsContent.setBoolProperty(Contentlet.DONT_VALIDATE_ME, true);

            final List<Category> categories = getACoupleOfCategories();

            final Contentlet newsContentReturned = contentletAPI.checkin(newsContent, (ContentletRelationships) null, categories,
                    null, user,false);

            assertNotNull(newsContentReturned);
            assertEquals (newsContentReturned.getIdentifier(), identifier);
            newsContent = newsContentReturned;
        }  finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats,
     * List<Permission> selectedPermissions, User user,	boolean respectFrontendRoles)
     *
     */
    @Test
    public void testCheckin1_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (ContentletRelationships) null, categories,
                null, user,false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user, false);

            checkedoutNewsContent.setIndexPolicy(IndexPolicy.FORCE);
            checkedoutNewsContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent, (ContentletRelationships) null,
                new ArrayList<>(), null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet currentContentlet, ContentletRelationships relationshipsData, List<Category> cats,
     * List<Permission> selectedPermissions, User user, boolean respectFrontendRoles, boolean generateSystemEvent)
     *
     */
    @Test
    public void testCheckin2_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, null, categories,
                null, user,false, false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user, false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent, null,
                new ArrayList<>(), null, user, false, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats ,
     * List<Permission> permissions, User user,boolean respectFrontendRoles)
     *
     */
    @Test
    public void testCheckin3_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (Map<Relationship, List<Contentlet>>) null, categories,
                null, user,false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user, false);

            checkedoutNewsContent.setIndexPolicy(IndexPolicy.FORCE);
            checkedoutNewsContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent,
                (Map<Relationship, List<Contentlet>>) null, new ArrayList<>(), null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    /**
     * This one tests the ContentletAPI.checkin with the following signature:
     *
     * checkin(Contentlet contentlet, Map<Relationship, List<Contentlet>> contentRelationships, List<Category> cats,
     * User user,boolean respectFrontendRoles)
     *
     */
    @Test
    public void testCheckin4_ExistingContentWithCats_EmptyCatsList_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (Map<Relationship, List<Contentlet>>) null, categories,
                user,false);

            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user, false);

            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutNewsContent,
                (Map<Relationship, List<Contentlet>>) null, new ArrayList<>(), user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithCats_EmptyCats_ShouldWipeExistingCats()
            throws DotDataException, DotSecurityException {
        Contentlet newsContent = null;

        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());

            final List<Category> categories = getACoupleOfCategories();

            newsContent = contentletAPI.checkin(newsContent, (Map<Relationship, List<Contentlet>>) null, categories, null, user,
                false);

            Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(newsContent,
                null, new ArrayList<>(), null, user, false);

            List<Category> existingCats = APILocator.getCategoryAPI().getParents(reCheckedinContent, user,
                false);

            assertFalse(UtilMethods.isSet(existingCats));
        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    @Test
    public void testCheckin1_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
            throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);
        final Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();
        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();

        final List<Category> categories = getACoupleOfCategories();

        blogContent = contentletAPI.checkin(blogContent, relationships, categories, null, user,
                false);
        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);

        List<Contentlet> relatedContentFromDB = relationshipAPI
                .dbRelatedContent(relationship, blogContent);

        assertTrue(relatedContentFromDB.containsAll(relatedContent));

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
        checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI
                .checkin(checkedoutBlogContent, (ContentletRelationships) null,
                        null, null, user, false);

        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);

        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin1_ExistingContentWithChildAndParentRels_NullRels_ShouldKeepExistingRels()
        throws DotDataException, DotSecurityException {
        Contentlet blogContent    = null;
        Relationship relationship = null;
        List<Contentlet> relatedContent = null;

        try {

            blogContent = TestDataUtils
                    .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                            blogContentType.id());

            final ContentletRelationships relationships = getACoupleOfParentAndChildrenSelfJoinRelationships(blogContent);
            relationship = relationships.getRelationshipsRecords().get(0).getRelationship();
            relatedContent = relationships.getRelationshipsRecords().get(0).getRecords();

            final List<Category> categories = getACoupleOfCategories();
            blogContent.setIndexPolicy(IndexPolicy.FORCE);
            blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            blogContent = contentletAPI.checkin(blogContent, relationships, categories, null, user,
                false);

            List<Contentlet> relatedContentFromDB = relationshipAPI.dbRelatedContent(relationship, blogContent);

            assertTrue(relatedContentFromDB.containsAll(relatedContent));

            Contentlet checkedoutBlogContent = contentletAPI.checkout(blogContent.getInode(), user, false);

            checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
            checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
            Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent, (ContentletRelationships) null,
                null, null, user, false);

            List<Contentlet> existingRelationships = relationshipAPI.dbRelatedContent(relationship, reCheckedinContent);

            assertTrue(existingRelationships.containsAll(relatedContent));
        } finally {

            if (InodeUtils.isSet(blogContent.getInode())) {
                contentletAPI.archive(blogContent, user, false);
                contentletAPI.delete(blogContent, user, false);
            }

            if(UtilMethods.isSet(relatedContent)) {
                relatedContent.forEach(content -> {
                    try {
                        contentletAPI.archive(content, user, false);
                        contentletAPI.delete(content, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            if (relationship != null && relationship.getInode() != null) {
                relationshipAPI.delete(relationship);
            }
        }
    }

    @Test
    public void testCheckin2_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();
        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationships,
                categories, null, user, false, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
        checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI
                .checkin(checkedoutBlogContent, (ContentletRelationships) null,
                null, null, user, false, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin3_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories,
                null, user,false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                (Map<Relationship, List<Contentlet>>) null, null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin4_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        checkedoutBlogContent.setIndexPolicy(IndexPolicy.FORCE);
        checkedoutBlogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                (Map<Relationship, List<Contentlet>>) null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithRels_NullRels_ShouldKeepExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());
        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent.setIndexPolicyDependencies(IndexPolicy.FORCE);
        Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(blogContent,
                null, null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertTrue(existingRelationships.containsAll(relatedContent));
    }

    @Test
    public void testCheckin3_ExistingContentWithRels_EmptyRels_ShouldWipeExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final ContentletRelationships.ContentletRelationshipRecords relationshipRecords =
                relationships.getRelationshipsRecords().get(0);

        final Relationship relationship = relationshipRecords.getRelationship();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationship,
                relationshipRecords.getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories,
                null, user,false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                new HashMap<>(), null, null, user, false);

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();

        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();

        assertFalse(relatedContent.isEmpty());
        assertFalse(UtilMethods.isSet(existingRelationships));
    }

    @Test
    public void testCheckin4_ExistingContentWithRels_EmptyRels_ShouldWipeExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        Contentlet checkedoutBlogContent = contentletAPI
                .checkout(blogContent.getInode(), user, false);

        Contentlet reCheckedinContent = contentletAPI.checkin(checkedoutBlogContent,
                new HashMap<>(), null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();

        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertFalse(UtilMethods.isSet(existingRelationships));
    }

    @Test
    public void testCheckinWithoutVersioning_ExistingContentWithRels_EmptyRels_ShouldWipeExistingRels()
        throws DotDataException, DotSecurityException {

        Contentlet blogContent = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());

        final ContentletRelationships relationships = getACoupleOfChildRelationships(blogContent);

        final List<Category> categories = getACoupleOfCategories();

        final Map<Relationship, List<Contentlet>> relationshipsMap = new HashMap<>();
        relationshipsMap.put(relationships.getRelationshipsRecords().get(0).getRelationship(),
                relationships.getRelationshipsRecords().get(0).getRecords());

        blogContent.setIndexPolicy(IndexPolicy.FORCE);
        blogContent = contentletAPI.checkin(blogContent, relationshipsMap, categories, user, false);

        Contentlet reCheckedinContent = contentletAPI.checkinWithoutVersioning(blogContent,
                new HashMap<>(), null, null, user, false);

        Relationship relationship = relationships.getRelationshipsRecords().get(0)
                .getRelationship();
        List<Contentlet> relatedContent = relationships.getRelationshipsRecords().get(0)
                .getRecords();
        assertFalse(relatedContent.isEmpty());

        RelationshipAPI relationshipAPI = APILocator.getRelationshipAPI();
        List<Contentlet> existingRelationships = relationshipAPI
                .dbRelatedContent(relationship, reCheckedinContent);
        assertFalse(UtilMethods.isSet(existingRelationships));
    }

    @Test
    public void testCopyProperties_TypeWithTagField_shouldCopyTagFieldValue()
        throws DotSecurityException, DotDataException {
        Contentlet newsContent = null;
        try {
            newsContent = TestDataUtils
                    .getNewsContent(false, languageAPI.getDefaultLanguage().getId(),
                            newsContentType.id());
            Map<String, Object> innerMap = new HashMap<>(newsContent.getMap());
            newsContent = contentletAPI.checkin(newsContent, user,false);
            Contentlet checkedoutNewsContent = contentletAPI.checkout(newsContent.getInode(), user, false);
            assertNull(checkedoutNewsContent.getStringProperty("tags"));
            contentletAPI.copyProperties(checkedoutNewsContent, innerMap);
            assertEquals(checkedoutNewsContent.getStringProperty("tags"), "test");

        } finally {
            if(newsContent!=null && UtilMethods.isSet(newsContent.getIdentifier())) {
                contentletAPI.destroy(newsContent, user, false);
            }
        }
    }

    private ContentletRelationships getACoupleOfParentAndChildrenSelfJoinRelationships(final Contentlet contentlet)
        throws DotDataException, DotSecurityException {

        final Relationship selfJoinRelationship = new Relationship();
        selfJoinRelationship.setRelationTypeValue("ParentBlog-ChildBlog");
        selfJoinRelationship.setParentStructureInode(contentlet.getContentTypeId());
        selfJoinRelationship.setChildStructureInode(contentlet.getContentTypeId());
        selfJoinRelationship.setCardinality(1);
        selfJoinRelationship.setParentRelationName("ParentBlog");
        selfJoinRelationship.setChildRelationName("ChildBlog");
        relationshipAPI.save(selfJoinRelationship);

        final ContentletRelationships contentletRelationships = new ContentletRelationships(contentlet);

        final ContentletRelationships.ContentletRelationshipRecords parentRecords =
            contentletRelationships.new ContentletRelationshipRecords(selfJoinRelationship, false);
        Contentlet parentBlog1 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        parentBlog1 = contentletAPI.checkin(parentBlog1, new HashMap<>(), getACoupleOfCategories(),  user, false);
        Contentlet parentBlog2 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        parentBlog2 = contentletAPI.checkin(parentBlog2, new HashMap<>(), getACoupleOfCategories(), user, false);
        parentRecords.setRecords(Arrays.asList(parentBlog1, parentBlog2));

        final ContentletRelationships.ContentletRelationshipRecords childRecords =
            contentletRelationships.new ContentletRelationshipRecords(selfJoinRelationship, true);
        Contentlet childBlog1 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        childBlog1 = contentletAPI.checkin(childBlog1, new HashMap<>(), getACoupleOfCategories(), user, false);
        Contentlet childBlog2 = TestDataUtils
                .getBlogContent(false, APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                        blogContentType.id());
        childBlog2 = contentletAPI.checkin(childBlog2, new HashMap<>(), getACoupleOfCategories(), user, false);
        childRecords.setRecords(Arrays.asList(childBlog1, childBlog2));

        contentletRelationships.setRelationshipsRecords(new ArrayList<>(Arrays.asList(parentRecords, childRecords)));

        return contentletRelationships;
    }

    private ContentletRelationships getACoupleOfChildRelationships(Contentlet contentlet)
        throws DotDataException, DotSecurityException {

        ContentletRelationships relationships = new ContentletRelationships(contentlet);

        Relationship blogComments = APILocator.getRelationshipAPI().byTypeValue(
                "Parent" + blogContentType.variable() + "-" + "Child" + commentsContentType
                        .variable());
        if (blogComments == null) {
            blogComments = new Relationship();
            blogComments.setRelationTypeValue(
                    "Parent" + blogContentType.variable() + "-" + "Child" + commentsContentType
                            .variable());
            blogComments.setParentStructureInode(contentlet.getContentTypeId());
            blogComments.setChildStructureInode(commentsContentType.id());
            blogComments.setCardinality(1);
            blogComments.setParentRelationName("Parent" + blogContentType.variable());
            blogComments.setChildRelationName("Child" + commentsContentType.variable());
            relationshipAPI.save(blogComments);
        }

        ContentletRelationships.ContentletRelationshipRecords records =
            relationships.new ContentletRelationshipRecords(blogComments, true);

        Contentlet comment1 = new Contentlet();
        comment1.setContentTypeId(commentsContentType.id());
        comment1.setStringProperty("title", "comment1");
        comment1.setStringProperty("email", "email");
        comment1.setStringProperty("comment", "comment");
        comment1.setIndexPolicy(IndexPolicy.FORCE);
        comment1.setIndexPolicyDependencies(IndexPolicy.FORCE);

        comment1 = contentletAPI.checkin(comment1, new HashMap<>(), user, false);
        comment1.setIndexPolicy(IndexPolicy.FORCE);
        comment1.setIndexPolicyDependencies(IndexPolicy.FORCE);

        Contentlet comment2 = new Contentlet();
        comment2.setContentTypeId(commentsContentType.id());
        comment2.setStringProperty("title", "comment2");
        comment2.setStringProperty("email", "email");
        comment2.setStringProperty("comment", "comment");
        comment2.setIndexPolicy(IndexPolicy.FORCE);
        comment2.setIndexPolicyDependencies(IndexPolicy.FORCE);

        comment2 = contentletAPI.checkin(comment2, new HashMap<>(), user, false);
        comment2.setIndexPolicy(IndexPolicy.FORCE);
        comment2.setIndexPolicyDependencies(IndexPolicy.FORCE);

        records.setRecords(Arrays.asList(comment1, comment2));
        relationships.setRelationshipsRecords(CollectionsUtils.list(records));

        return relationships;
    }

    @NotNull
    private List<Category> getACoupleOfCategories() throws DotDataException, DotSecurityException {
        return APILocator.getCategoryAPI()
                .findTopLevelCategories(APILocator.systemUser(), false);
    }

    @Test
    public void testDeletePageDefinedAsDetailPage() throws DotSecurityException, DotDataException {

        long time              = System.currentTimeMillis();
        ContentType type       = null;
        Folder testFolder      = null;
        HTMLPageAsset htmlPage = null;
        Template template      = null;

        try{
            // new template
            template = new TemplateDataGen().nextPersisted();

            // new test folder
            testFolder = new FolderDataGen().nextPersisted();

            //new html page
            htmlPage = new HTMLPageDataGen(testFolder, template)
                    .languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            //new content type with detail page
            type = ContentTypeBuilder
                    .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.ordinal()))
                    .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
                    .host(Host.SYSTEM_HOST)
                    .name("ContentTypeWithDetailPage" + time).owner("owner")
                    .variable("velocityVarNameTesting" + time)
                    .detailPage(htmlPage.getIdentifier()).urlMapPattern("mapPatternForTesting")
                    .build();
            type = contentTypeAPI.save(type, null, null);

            //html page is removed
            contentletAPI.archive(htmlPage, user, false);
            contentletAPI.delete(htmlPage, user, false);

            //verify that the content type was unlinked from the deleted page
            type = contentTypeAPI.find(type.id());
            assertNull(type.detailPage());
            assertNull(type.urlMapPattern());
        } finally {
            if (type != null){
                contentTypeAPI.delete(type);
            }

            if (testFolder != null){
                FolderDataGen.remove(testFolder);
            }

            if (template != null){
                TemplateDataGen.remove(template);
            }
        }
    }

    @Test
    public void testSearchFileAssetByMetadata()
            throws DotSecurityException, DotDataException {

        //Creating a test file asset
        TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId());

        final String query = "+contentType:FileAsset +metaData.contentType:*image/jpeg*";
        List<Contentlet> result = contentletAPI.search(query, 100, 0, null, user, false);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertTrue(result.stream().anyMatch(contentlet -> {
            final String fileName = contentlet.getStringProperty("fileName").toLowerCase();
               return fileName.endsWith("jpg") ||  fileName.endsWith("jpeg");
        }));
    }

    @Test
    public void testSetTemplateForAPageMustKeepTheSameTemplateForWorkingVersionsOnly()
            throws Exception {

        Contentlet spanishPage = null;
        Container container = null;
        Folder folder       = null;
        Structure structure = null;

        Template englishTemplate  = null;
        Template spanishTemplate  = null;

        final String UUID = UUIDGenerator.generateUuid();

        try{
            structure = new StructureDataGen().nextPersisted();
            container = new ContainerDataGen().withStructure(structure, "")
                    .nextPersisted();
            englishTemplate = new TemplateDataGen().title("English Template")
                    .withContainer(container.getIdentifier(),UUID).nextPersisted();
            spanishTemplate = new TemplateDataGen().title("Spanish Template")
                    .withContainer(container.getIdentifier(),UUID).nextPersisted();
            folder = new FolderDataGen().nextPersisted();

            //Create a page in English
            final HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, englishTemplate);
            final HTMLPageAsset englishPage = htmlPageDataGen.languageId(1).nextPersisted();

            //Create the Spanish version of the page using a different template
            spanishPage = HTMLPageDataGen.checkout(englishPage);

            spanishPage.setLanguageId(spanishLanguage.getId());
            spanishPage.setProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, spanishTemplate.getIdentifier());
            spanishPage.setProperty(HTMLPageAssetAPI.URL_FIELD, englishPage.getPageUrl() + "SP");

            spanishPage = HTMLPageDataGen.checkin(spanishPage);

            //Verify that both pages have the same template
            assertEquals(spanishPage.get(HTMLPageAssetAPI.TEMPLATE_FIELD), spanishTemplate.getIdentifier());

            //Verify that the initial English version was not modified
            assertEquals(englishTemplate.getIdentifier(),
                    contentletAPI.find(englishPage.getInode(), user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));

            //Verify that a new English version with the Spanish template was created
            final String newEnglishInode = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(englishPage.getIdentifier(),
                            englishPage.getLanguageId()).getWorkingInode();

            assertEquals(spanishPage.get(HTMLPageAssetAPI.TEMPLATE_FIELD),
                    contentletAPI.find(newEnglishInode, user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));


        } finally{

            //Clean up environment
            contentletAPI.destroy(spanishPage, user, false);

            if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())){
                FolderDataGen.remove(folder);
            }

            if (UtilMethods.isSet(spanishTemplate) && UtilMethods.isSet(spanishTemplate.getInode())){
                TemplateDataGen.remove(spanishTemplate);
            }

            if (UtilMethods.isSet(englishTemplate) && UtilMethods.isSet(englishTemplate.getInode())){
                TemplateDataGen.remove(englishTemplate);
            }

            if (UtilMethods.isSet(container) && UtilMethods.isSet(container.getInode())){
                ContainerDataGen.remove(container);
            }

            if (UtilMethods.isSet(structure) && UtilMethods.isSet(structure.getInode())){
                StructureDataGen.remove(structure);
            }
        }

    }

    @Test
    public void testSetTemplateForAPageMustKeepTheSameTemplateForWorkingVersionsNoLive()
            throws Exception {

        Contentlet result   = null;
        Container container = null;
        Folder folder       = null;
        Structure structure = null;

        Template englishTemplate  = null;
        Template spanishTemplate  = null;
        final String UUID = UUIDGenerator.generateUuid();

        try{
            structure = new StructureDataGen().nextPersisted();
            container = new ContainerDataGen().withStructure(structure, "")
                    .nextPersisted();
            englishTemplate = new TemplateDataGen().title("English Template")
                    .withContainer(container.getIdentifier(),UUID).nextPersisted();
            spanishTemplate = new TemplateDataGen().title("Spanish Template")
                    .withContainer(container.getIdentifier(),UUID).nextPersisted();
            folder = new FolderDataGen().nextPersisted();

            //Create a page in English
            HTMLPageDataGen htmlPageDataGen = new HTMLPageDataGen(folder, englishTemplate);
            HTMLPageAsset englishPage = htmlPageDataGen.languageId(1).nextPersisted();

            //Publish this page to set it as working and live
            contentletAPI.publish(englishPage, user, false);
            contentletAPI.isInodeIndexed(englishPage.getInode(), true);
            //Create the Spanish version of the page using a different template
            Contentlet spanishPage = HTMLPageDataGen.checkout(englishPage);

            spanishPage.setLanguageId(spanishLanguage.getId());
            spanishPage.setProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, spanishTemplate.getIdentifier());
            spanishPage.setProperty(HTMLPageAssetAPI.URL_FIELD, englishPage.getPageUrl() + "SP");

            result = HTMLPageDataGen.checkin(spanishPage);

            //Verify that the Spanish page has the same template
            assertEquals(result.get(HTMLPageAssetAPI.TEMPLATE_FIELD), spanishTemplate.getIdentifier());

            //Verify that the initial English version was not modified
            assertEquals(englishTemplate.getIdentifier(),
                    contentletAPI.find(englishPage.getInode(), user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));

            //Verify that a new English version with the Spanish template was created
            final String newEnglishInode = APILocator.getVersionableAPI()
                    .getContentletVersionInfo(englishPage.getIdentifier(),
                            englishPage.getLanguageId()).getWorkingInode();

            assertEquals(spanishPage.get(HTMLPageAssetAPI.TEMPLATE_FIELD),
                    contentletAPI.find(newEnglishInode, user, false)
                            .get(HTMLPageAssetAPI.TEMPLATE_FIELD));

        } finally{

            //Clean up environment
            contentletAPI.destroy(result, user, false);

            if (UtilMethods.isSet(folder) && UtilMethods.isSet(folder.getInode())){
                FolderDataGen.remove(folder);
            }

            if (UtilMethods.isSet(spanishTemplate) && UtilMethods.isSet(spanishTemplate.getInode())){
                TemplateDataGen.remove(spanishTemplate);
            }

            if (UtilMethods.isSet(englishTemplate) && UtilMethods.isSet(englishTemplate.getInode())){
                TemplateDataGen.remove(englishTemplate);
            }

            if (UtilMethods.isSet(container) && UtilMethods.isSet(container.getInode())){
                ContainerDataGen.remove(container);
            }

            if (UtilMethods.isSet(structure) && UtilMethods.isSet(structure.getInode())){
                StructureDataGen.remove(structure);
            }
        }

    }

    @Test
    public void testMoveContentDependenciesFromChild()
            throws DotSecurityException, DotDataException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);
            childContentType = createContentType("childContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> childContentType
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), childContentType.variable());

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(parentField, user);

            Contentlet childContent = new ContentletDataGen(childContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            Contentlet parentContent = new ContentletDataGen(parentContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).next();

            parentContent = contentletAPI.checkin(parentContent, CollectionsUtils
                    .map(relationship, CollectionsUtils.list(childContent)), user, false);

            Map<Relationship, List<Contentlet>> relationshipRecords = contentletAPI
                    .findContentRelationships(parentContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(childContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

            //creates a new version of the child
            childContent.setInode("");
            childContent = contentletAPI
                    .checkin(childContent, (ContentletRelationships) null, null, null,
                            user, false);

            relationshipRecords = contentletAPI
                    .findContentRelationships(parentContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(childContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testMoveContentDependenciesFromChildSelfRelated()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> parentContentType (self-related)
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), parentContentType.variable());

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(parentField, user);

            final ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet childContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            Contentlet parentContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId()).next();

            parentContent = contentletAPI.checkin(parentContent, CollectionsUtils
                    .map(relationship, CollectionsUtils.list(childContent)), user, false);

            List<Contentlet> relatedContent = relationshipAPI.dbRelatedContent(relationship, parentContent, true);

            assertEquals(1, relatedContent.size());
            assertEquals(childContent.getIdentifier(), relatedContent.get(0).getIdentifier());

            //creates a new version of the child
            childContent.setInode("");
            childContent = contentletAPI
                    .checkin(childContent, (ContentletRelationships) null, null, null,
                            user, false);

            relatedContent = relationshipAPI.dbRelatedContent(relationship, parentContent, true);

            assertEquals(1, relatedContent.size());
            assertEquals(childContent.getIdentifier(), relatedContent.get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }

    }

    @Test
    public void testMoveContentDependenciesFromParent()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);
            childContentType = createContentType("childContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> childContentType
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), childContentType.variable());

            //Setting the other side of the relationship childContentType --> parentContentType
            com.dotcms.contenttype.model.field.Field childField = createRelationshipField("parent",
                    childContentType.id(), parentContentType.variable() + StringPool.PERIOD + parentField.variable());

            //Removing parent field
            fieldAPI.delete(parentField, user);

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(childField, user);

            Contentlet childContent = new ContentletDataGen(childContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).next();

            Contentlet parentContent = new ContentletDataGen(parentContentType.id())
                    .languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            childContent = contentletAPI.checkin(childContent, CollectionsUtils
                    .map(relationship, CollectionsUtils.list(parentContent)), user, false);

            Map<Relationship, List<Contentlet>> relationshipRecords = contentletAPI
                    .findContentRelationships(childContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(parentContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

            //creates a new version of the child
            parentContent.setInode("");
            parentContent = contentletAPI
                    .checkin(parentContent, (ContentletRelationships) null, null, null,
                            user, false);

            relationshipRecords = contentletAPI
                    .findContentRelationships(childContent, user);

            assertTrue(relationshipRecords.containsKey(relationship));
            assertEquals(1, relationshipRecords.get(relationship).size());
            assertEquals(parentContent.getIdentifier(),
                    relationshipRecords.get(relationship).get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testMoveContentDependenciesFromParentSelfRelated()
            throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            final long time = System.currentTimeMillis();
            parentContentType = createContentType("parentContentType" + time,
                    BaseContentType.CONTENT);

            //One side of the relationship is set parentContentType --> parentContentType (self-related as parent)
            com.dotcms.contenttype.model.field.Field parentField = createRelationshipField("child",
                    parentContentType.id(), parentContentType.variable());

            //Setting the other side of the relationship parentContentType --> parentContentType (self-related as child)
            com.dotcms.contenttype.model.field.Field childField = createRelationshipField("parent",
                    parentContentType.id(), parentContentType.variable() + StringPool.PERIOD + parentField.variable());

            //Removing parent field
            fieldAPI.delete(parentField, user);

            final Relationship relationship = relationshipAPI
                    .getRelationshipFromField(childField, user);

            final ContentletDataGen dataGen = new ContentletDataGen(parentContentType.id());
            Contentlet parentContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();

            Contentlet childContent = dataGen.languageId(languageAPI.getDefaultLanguage().getId()).next();
            childContent.setRelated(childField.variable(), CollectionsUtils.list(parentContent));

            childContent = contentletAPI.checkin(childContent, user, false);

            List<Contentlet> relatedContent = relationshipAPI.dbRelatedContent(relationship, childContent, false);

            assertEquals(1, relatedContent.size());
            assertEquals(parentContent.getIdentifier(), relatedContent.get(0).getIdentifier());

            //creates a new version of the parent
            parentContent.setInode("");
            parentContent = contentletAPI
                    .checkin(parentContent, (ContentletRelationships) null, null, null,
                            user, false);

            relatedContent = relationshipAPI.dbRelatedContent(relationship, childContent, false);

            assertEquals(1, relatedContent.size());
            assertEquals(parentContent.getIdentifier(), relatedContent.get(0).getIdentifier());

        } finally {
            if (parentContentType != null) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test
    public void test_update_mod_date_contentlet_expect_success() throws Exception {

        //Creating dummy content for testing
        final Contentlet beforeTouch = TestDataUtils.getGenericContentContent(true,
                languageAPI.getDefaultLanguage().getId());
        assertNotNull(beforeTouch);

        //We need to evict the contentlet from hibernate's cache so we can see our changes once we pull-it out from db.
        HibernateUtil.evict(HibernateUtil.load(com.dotmarketing.portlets.contentlet.business.Contentlet.class, beforeTouch.getInode()));

        final Set<String> inodes = Stream.of(beforeTouch).map(Contentlet::getInode).collect(Collectors.toSet());
        int count = contentletAPI.updateModDate(inodes, user);
        assertEquals("update count does not match",1, count);

        final Contentlet afterTouch = contentletAPI.find(beforeTouch.getInode(), APILocator.getUserAPI().getSystemUser(),false);
        assertEquals(beforeTouch.getInode(), afterTouch.getInode());
        assertNotEquals(afterTouch.getModDate(), beforeTouch.getModDate());
        assertEquals(user.getUserId(),afterTouch.getModUser());
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[] testCasesNullRequiredFieldValues() {

        // case 1 setStringProperty
        final TestCaseNullFieldvalues case1 = new TestCaseNullFieldvalues();
        case1.fieldType = TextField.class;
        case1.dataType = DataTypes.TEXT;
        case1.assertion =
                // contentTypeAndField is a Tuple of contentType Id and Field variable
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setStringProperty(contentTypeIdAndFieldVar._2,
                                "this is a valid value");

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        // case 2 setLongProperty
        final TestCaseNullFieldvalues case2 = new TestCaseNullFieldvalues();
        case2.fieldType = TextField.class;
        case2.dataType = DataTypes.INTEGER;
        case2.assertion =
                // contentTypeAndField is a Tuple of contentType and Field
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setLongProperty(contentTypeIdAndFieldVar._2,
                                10000L);

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };


        // case 3 setBoolProperty
        final TestCaseNullFieldvalues case3 = new TestCaseNullFieldvalues();
        case3.fieldType = RadioField.class;
        case3.dataType = DataTypes.BOOL;

        case3.assertion =
                // contentTypeAndField is a Tuple of contentType and Field
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setBoolProperty(contentTypeIdAndFieldVar._2,
                                true);

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        // case 3 setFloatProperty
        final TestCaseNullFieldvalues case4 = new TestCaseNullFieldvalues();
        case4.fieldType = TextField.class;
        case4.dataType = DataTypes.FLOAT;
        case4.assertion =
                // contentTypeAndField is a Tuple of contentType and Field
                (contentTypeIdAndFieldVar) -> {
                    try {
                        final Contentlet testContentlet
                                = tryToCheckinContentWithNullValueForRequiredField(
                                contentTypeIdAndFieldVar);
                        // lets give a value to the required field using setStringProperty

                        testContentlet.setFloatProperty(contentTypeIdAndFieldVar._2,
                                1500);

                        // this time should succeed
                        contentletAPI.checkin(testContentlet, user, false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new DotRuntimeException(e);
                    }
                };

        return new TestCaseNullFieldvalues[] {
                case1,
                case2,
                case3,
                case4
        };

    }

    private static class TestCaseNullFieldvalues {
        Consumer<Tuple2<String, String>> assertion;
        Class<? extends com.dotcms.contenttype.model.field.Field> fieldType;
        DataTypes dataType;
    }


    private static Contentlet tryToCheckinContentWithNullValueForRequiredField(
            final Tuple2<String, String> typeIdFieldVar)
            throws DotSecurityException, DotDataException {

        final String testTypeId = typeIdFieldVar._1;

        final String testFieldVar = typeIdFieldVar._2;

        final ContentletDataGen contentletDataGen = new ContentletDataGen(
                testTypeId);
        final Contentlet testContentlet = contentletDataGen.next();
        testContentlet.setProperty(testFieldVar, null);

        try {
            contentletAPI.checkin(testContentlet, user, false);
        } catch (DotContentletValidationException e) {
            // expected because of required field
            Logger.info(ContentletAPITest.class, "All good");
        }

        return testContentlet;
    }

    @Test
    @UseDataProvider("testCasesNullRequiredFieldValues")
    public void testCheckin_nullRequiredFieldValue(final TestCaseNullFieldvalues testCase)
            throws DotDataException, DotSecurityException {

        long time = System.currentTimeMillis();

        ContentType contentType = ContentTypeBuilder
                .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType()))
                .description("ContentTypeWithPublishExpireFields " + time)
                .folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name("ContentTypeWithPublishExpireFields " + time)
                .owner(APILocator.systemUser().toString())
                .variable("CTVariable711").publishDateVar("publishDate")
                .expireDateVar("expireDate")
                .build();

        final ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(APILocator.systemUser());

        try {
            contentType = contentTypeApi.save(contentType);

            List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>(
                    contentType.fields());

            final String titleFieldVarname = "testTitle" + time;

            final com.dotcms.contenttype.model.field.Field titleField = FieldBuilder
                    .builder(TextField.class)
                    .name(titleFieldVarname)
                    .variable(titleFieldVarname)
                    .contentTypeId(contentType.id())
                    .build();

            fields.add(titleField);

            final String secondFieldVarName = "testSecondField"
                    + System.currentTimeMillis();

            final com.dotcms.contenttype.model.field.Field secondField = FieldBuilder
                    .builder(testCase.fieldType)
                    .dataType(testCase.dataType)
                    .name(secondFieldVarName)
                    .variable(secondFieldVarName)
                    .contentTypeId(contentType.id())
                    .required(true)
                    .build();

            fields.add(secondField);

            contentType = contentTypeApi.save(contentType, fields);

            testCase.assertion.accept(new Tuple2<>(contentType.id(), secondField.variable()));

        } finally {
            // Deleting content type.
            contentTypeApi.delete(contentType);
        }
    }

    private File getBinaryAsset(String inode, String varName, String binaryName) {

        FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();

        File binaryFromAssetsFolder = new File(fileAssetAPI.getRealAssetsRootPath()
                + separator
                + inode.charAt(0)
                + separator
                + inode.charAt(1)
                + separator
                + inode
                + separator
                + varName
                + separator
                + binaryName);

        return binaryFromAssetsFolder;
    }

    private Contentlet createContentWithFieldValues(String contentTypeId, Map<String, Object> fieldValues)
            throws DotSecurityException, DotDataException {
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(contentTypeId);
        contentlet.setLanguageId(languageAPI.getDefaultLanguage().getId());

        for(String fieldVariable : fieldValues.keySet()) {
            contentlet.setProperty(fieldVariable, fieldValues.get(fieldVariable));
        }

        return contentletAPI.checkin(contentlet, user, false);
    }

    private File createTempFileWithText(String name, String text) throws IOException {
        File tempFile = temporaryFolder.newFile(name);
        writeTextIntoFile(tempFile, text);
        return tempFile;
    }

    private ContentType createContentType(String name, BaseContentType baseType)
            throws DotSecurityException, DotDataException {
        ContentType contentType = ContentTypeBuilder.builder(baseType.immutableClass())
                .description(name)
                .host(defaultHost.getIdentifier())
                .name(name)
                .owner("owner")
                .build();

        return contentTypeAPI.save(contentType);
    }

    private com.dotcms.contenttype.model.field.Field createTextField(String name, String contentTypeId)
            throws DotSecurityException, DotDataException {
        com.dotcms.contenttype.model.field.Field field =  ImmutableTextField.builder()
                .name(name)
                .contentTypeId(contentTypeId)
                .dataType(DataTypes.TEXT)
                .build();

        return fieldAPI.save(field, user);
    }

    private com.dotcms.contenttype.model.field.Field createBinaryField(String name, String contentTypeId)
            throws DotSecurityException, DotDataException {
        com.dotcms.contenttype.model.field.Field field = ImmutableBinaryField.builder()
                .name(name)
                .contentTypeId(contentTypeId)
                .build();

        return fieldAPI.save(field, user);
    }

    /**
     * Util method to write dummy text into a file.
     *
     * @param file that we need to write. File should be empty.
     * @param textToWrite text that we are going to write into the file.
     */
    private void writeTextIntoFile(File file, final String textToWrite) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(textToWrite);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_findInDb_returns_properly() throws Exception {


        ContentType type = new ContentTypeDataGen()
                .fields(ImmutableList
                        .of(ImmutableTextField.builder().name("Title").variable("title").searchable(true).listed(true).build()))
                .nextPersisted();


        // test null value
        Optional<Contentlet> conOpt= contentletAPI.findInDb(null);
        assert(conOpt.isPresent()==false);


        // test non-existing
        conOpt= contentletAPI.findInDb("not-here");
        assert(conOpt.isPresent()==false);

        final Contentlet contentlet = new ContentletDataGen(type.id()).setProperty("title", "contentTest " + System.currentTimeMillis()).nextPersisted();
        contentlet.setStringProperty("title", "nope");

        conOpt= contentletAPI.findInDb(contentlet.getInode());
        assert(conOpt.isPresent());

        assertNotEquals(conOpt.get().getTitle(), contentlet.getTitle());



    }

    @Test
    public void testCheckInContentletWithoutHost_shouldUseContentTypeHost()
            throws DotDataException, DotSecurityException {
        ContentType contentType = null;

        try {
            contentType = new ContentTypeDataGen()
                    .fields(ImmutableList
                            .of(ImmutableTextField.builder().name("Title").variable("title")
                                    .build()))
                    .nextPersisted();
            Contentlet contentlet = new Contentlet();
            contentlet.setContentType(contentType);
            contentlet.setProperty("title", "contentTest " + System.currentTimeMillis());
            contentlet = contentletAPI.checkin(contentlet, user, false);

            assertEquals(contentType.host(), contentlet.getHost());
        }finally {
            if (contentType != null){
                contentTypeAPI.delete(contentType);
            }
        }
    }

    /**
     * This test creates one content with versions in English and Spanish
     * when one of these versions is deleted should be removed from the index.
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test
    public void testRemoveContentFromIndexMultilingualContent()
            throws DotDataException, DotSecurityException, InterruptedException {
        ContentType contentType = null;

        try {
            contentType = new ContentTypeDataGen()
                    .name("testRemoveContentFromIndexMultilingualContent"+System.currentTimeMillis())
                    .fields(ImmutableList
                            .of(ImmutableTextField.builder().name("Title").variable("title")
                                    .build()))
                    .nextPersisted();

            final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
            final Contentlet contentInEnglish = contentletDataGen.languageId(languageAPI.getDefaultLanguage().getId()).nextPersisted();
            Contentlet contentInSpanish = contentletDataGen.checkout(contentInEnglish);
            contentInSpanish.setLanguageId(spanishLanguage.getId());
            contentInSpanish.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentInSpanish = contentletDataGen.checkin(contentInSpanish);

            assertEquals(2,
                    contentletAPI.searchIndex("+identifier:"+contentInEnglish.getIdentifier(),-1,0,"",user,true).size());

            contentletDataGen.archive(contentInSpanish);
            contentletDataGen.delete(contentInSpanish);

            assertEquals(1,
                    contentletAPI.searchIndex("+identifier:"+contentInEnglish.getIdentifier(),-1,0,"",user,true).size());

            contentletDataGen.archive(contentInEnglish);
            contentletDataGen.delete(contentInEnglish);

            assertEquals(0,
                    contentletAPI.searchIndex("+identifier:"+contentInEnglish.getIdentifier(),-1,0,"",user,true).size());

        } finally {
            if (contentType != null) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

}