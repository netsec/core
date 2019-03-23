package com.dotmarketing.common.business.journal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * Test for {@link DistributedJournalAPI}
 */
public class DistributedJournalAPITest extends IntegrationTestBase {

    int numberToTest = 20;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

    }

    @Test
    public void test_highestpriority_reindex_vrs_normal_reindex() throws DotDataException {

        final List<Contentlet> contentlets = new ArrayList<>();

        ContentType type = new ContentTypeDataGen().nextPersisted();

        for (int i = 0; i < numberToTest; i++) {
            contentlets.add(new ContentletDataGen(type.id()).nextPersisted());
        }

        final DistributedJournalAPI distributedJournalAPI = APILocator.getDistributedJournalAPI();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        final List<Contentlet> contentletsHighPriority = contentlets.subList(0, numberToTest / 2);
        final List<Contentlet> contentletsLowPriority = contentlets.subList(numberToTest / 2, contentlets.size());

        assertNotNull(contentletsHighPriority);
        assertTrue(contentletsHighPriority.size() == numberToTest / 2);

        assertNotNull(contentletsLowPriority);
        assertTrue(contentletsLowPriority.size() == numberToTest / 2);

        final Set<String> highIdentifiers =
                contentletsHighPriority.stream().filter(Objects::nonNull).map(Contentlet::getIdentifier).collect(Collectors.toSet());
        final Set<String> lowIdentifiers =
                contentletsLowPriority.stream().filter(Objects::nonNull).map(Contentlet::getIdentifier).collect(Collectors.toSet());

        distributedJournalAPI.addIdentifierReindex(lowIdentifiers);
        distributedJournalAPI.addReindexHighPriority(highIdentifiers);

        // fetch 50
        Map<String, IndexJournal> indexJournals = distributedJournalAPI.findContentToReindex(numberToTest / 2);

        assertNotNull(indexJournals);
        assertTrue(indexJournals.size() == numberToTest / 2);
        assertTrue(indexJournals.keySet().containsAll(highIdentifiers));

        indexJournals = distributedJournalAPI.findContentToReindex(numberToTest / 2);

        assertNotNull(indexJournals);
        assertTrue(indexJournals.size() == numberToTest / 2);
        assertTrue(indexJournals.keySet().containsAll(lowIdentifiers));

    }

    @Test
    public void test_content_type_reindex() throws Exception {

        ContentType type = new ContentTypeDataGen().nextPersisted();

        for (int i = 0; i < numberToTest; i++) {
            new ContentletDataGen(type.id()).nextPersisted();
        }

        final DistributedJournalAPI distributedJournalAPI = APILocator.getDistributedJournalAPI();

        new DotConnect().setSQL("delete from dist_reindex_journal").loadResult();

        Map<String, IndexJournal> indexJournals = distributedJournalAPI.findContentToReindex(numberToTest / 2);

        assertTrue(indexJournals.size() == 0);
        List<Field> origFields = new ArrayList<>();
        List<Field> newFields = new ArrayList<>();
        origFields.addAll(type.fields());
        newFields.addAll(type.fields());

        newFields.add(
                ImmutableTextField.builder().name("asdasdasd").variable("asdasdasd").searchable(true).contentTypeId(type.id()).build());
        
        APILocator.getContentTypeAPI(APILocator.systemUser()).save(type, newFields);
        APILocator.getContentTypeAPI(APILocator.systemUser()).save(type, origFields);
        indexJournals = distributedJournalAPI.findContentToReindex(numberToTest);
        assertTrue(indexJournals.size() == numberToTest);
        assertTrue(indexJournals.values().iterator().next().getPriority() == DistributedJournalFactory.Priority.STRUCTURE.dbValue());

    }

}
