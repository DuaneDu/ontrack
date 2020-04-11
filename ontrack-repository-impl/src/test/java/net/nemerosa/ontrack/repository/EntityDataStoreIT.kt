package net.nemerosa.ontrack.repository

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.node.IntNode
import net.nemerosa.ontrack.common.Time
import net.nemerosa.ontrack.model.structure.Signature
import net.nemerosa.ontrack.repository.support.store.EntityDataStore
import net.nemerosa.ontrack.repository.support.store.EntityDataStoreFilter
import net.nemerosa.ontrack.repository.support.store.EntityDataStoreRecordAuditType
import net.nemerosa.ontrack.test.TestUtils
import org.junit.Assert
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

/**
 * Integration tests for [EntityDataStore].
 */
class EntityDataStoreIT : AbstractRepositoryTestSupport() {

    @Autowired
    private lateinit var store: EntityDataStore

    @Test
    fun add() {
        // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val record = store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(15))
        // Checks
        Assert.assertNotNull(record)
        Assert.assertTrue(record.id > 0)
        Assert.assertEquals(CATEGORY, record.category)
        Assert.assertNotNull(record.signature)
        Assert.assertEquals(name, record.name)
        Assert.assertNull(record.groupName)
        Assert.assertEquals(branch.projectEntityType, record.entity.projectEntityType)
        Assert.assertEquals(branch.id, record.entity.id)
        TestUtils.assertJsonEquals(
                IntNode(15),
                record.data
        )
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun replaceOrAdd() {
        // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val record = store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(15))
        // Checks
        Assert.assertTrue(record.id > 0)
        Assert.assertEquals(CATEGORY, record.category)
        TestUtils.assertJsonEquals(
                IntNode(15),
                record.data
        )
        // Updates the same name
        val secondRecord = store.replaceOrAdd(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(16))
        // Checks
        Assert.assertEquals(record.id.toLong(), secondRecord.id.toLong())
        TestUtils.assertJsonEquals(
                IntNode(16),
                secondRecord.data
        )
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun replaveOrAddForNewRecord() { // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val record = store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(15))
        // Checks
        Assert.assertTrue(record.id > 0)
        Assert.assertEquals(CATEGORY, record.category)
        TestUtils.assertJsonEquals(
                IntNode(15),
                record.data
        )
        // Updates with a different same name
        val secondRecord = store.replaceOrAdd(branch, CATEGORY, name + "2", Signature.of(TEST_USER), null, IntNode(16))
        // Checks
        Assert.assertNotEquals(record.id.toLong(), secondRecord.id.toLong())
        TestUtils.assertJsonEquals(
                IntNode(16),
                secondRecord.data
        )
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun audit() { // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val record = store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(15))
        // Gets the audit
        var audit = store.getRecordAudit(record.id)
        Assert.assertEquals(1, audit.size.toLong())
        Assert.assertEquals(EntityDataStoreRecordAuditType.CREATED, audit[0].type)
        Assert.assertEquals(TEST_USER, audit[0].signature.user.name)
        // Updates the same name
        store.replaceOrAdd(branch, CATEGORY, name, Signature.of("other"), null, IntNode(16))
        // Checks
        audit = store.getRecordAudit(record.id)
        Assert.assertEquals(2, audit.size.toLong())
        Assert.assertEquals(EntityDataStoreRecordAuditType.UPDATED, audit[0].type)
        Assert.assertEquals("other", audit[0].signature.user.name)
        Assert.assertEquals(EntityDataStoreRecordAuditType.CREATED, audit[1].type)
        Assert.assertEquals(TEST_USER, audit[1].signature.user.name)
    }

    @Test
    fun delete_by_name() { // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val id = store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(15)).id
        // Gets by ID
        Assert.assertTrue(store.getById(branch, id).isPresent)
        // Deletes by name
        store.deleteByName(branch, CATEGORY, name)
        // Gets by ID ot possible any longer
        Assert.assertFalse(store.getById(branch, id).isPresent)
    }

    @Test
    fun delete_by_group() { // Entity
        val branch = do_create_branch()
        // Adds some data for the group
        val group = TestUtils.uid("G")
        val name = TestUtils.uid("T")
        val id1 = store.add(branch, CATEGORY, name + 1, Signature.of(TEST_USER), group, IntNode(10)).id
        val id2 = store.add(branch, CATEGORY, name + 2, Signature.of(TEST_USER), group, IntNode(10)).id
        // Gets by ID
        Assert.assertTrue(store.getById(branch, id1).isPresent)
        Assert.assertTrue(store.getById(branch, id2).isPresent)
        // Deletes by group
        store.deleteByGroup(branch, CATEGORY, group)
        // Gets by ID ot possible any longer
        Assert.assertFalse(store.getById(branch, id1).isPresent)
        Assert.assertFalse(store.getById(branch, id2).isPresent)
    }

    @Test
    fun delete_by_category() { // Entities
        val branch1 = do_create_branch()
        val branch2 = do_create_branch()
        // Adds some data with same name for different entities
        val name = TestUtils.uid("T")
        val id1 = store.add(branch1, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(10)).id
        val id2 = store.add(branch2, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(10)).id
        // Gets by ID
        Assert.assertTrue(store.getById(branch1, id1).isPresent)
        Assert.assertTrue(store.getById(branch2, id2).isPresent)
        // Deletes by category
        store.deleteByCategoryBefore(CATEGORY, Time.now())
        // Gets by ID ot possible any longer
        Assert.assertFalse(store.getById(branch1, id1).isPresent)
        Assert.assertFalse(store.getById(branch2, id2).isPresent)
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun last_by_category_and_name() { // Entity
        val branch = do_create_branch()
        // Adds some data, twice, for the same name
        val name = TestUtils.uid("T")
        store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(15)).id
        val id2 = store.add(branch, CATEGORY, name, Signature.of(TEST_USER), null, IntNode(16)).id
        // Gets last by category / name
        val record = store.findLastByCategoryAndName(branch, CATEGORY, name, Time.now()).orElse(null)
        // Checks
        Assert.assertNotNull(record)
        Assert.assertEquals(record.id.toLong(), id2.toLong())
        TestUtils.assertJsonEquals(
                IntNode(16),
                record.data
        )
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun last_by_category_and_group_and_name() { // Entity
        val branch = do_create_branch()
        // Adds some data, twice, for the same name, and several names, but for a same group
        val group = TestUtils.uid("G")
        val name1 = TestUtils.uid("T")
        val name2 = TestUtils.uid("T")
        store.add(branch, CATEGORY, name1, Signature.of(TEST_USER), group, IntNode(11)).id
        val id12 = store.add(branch, CATEGORY, name1, Signature.of(TEST_USER), group, IntNode(12)).id
        store.add(branch, CATEGORY, name2, Signature.of(TEST_USER), group, IntNode(21)).id
        store.add(branch, CATEGORY, name2, Signature.of(TEST_USER), group, IntNode(22)).id
        // Gets last by category / name / group
        val record = store.findLastByCategoryAndGroupAndName(branch, CATEGORY, group, name1).orElse(null)
        // Checks
        Assert.assertNotNull(record)
        Assert.assertEquals(record.id.toLong(), id12.toLong())
        TestUtils.assertJsonEquals(
                IntNode(12),
                record.data
        )
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun last_by_category() { // Entity
        val branch = do_create_branch()
        // Adds some data, twice, for the same name, and several names, but for a same group
        val name1 = TestUtils.uid("T")
        val name2 = TestUtils.uid("T")
        val name3 = TestUtils.uid("T")
        store.add(branch, CATEGORY, name1, Signature.of(TEST_USER), null, IntNode(11)).id
        val id12 = store.add(branch, CATEGORY, name1, Signature.of(TEST_USER), null, IntNode(12)).id
        store.add(branch, CATEGORY, name2, Signature.of(TEST_USER), null, IntNode(21)).id
        val id22 = store.add(branch, CATEGORY, name2, Signature.of(TEST_USER), null, IntNode(22)).id
        store.add(branch, CATEGORY, name3, Signature.of(TEST_USER), null, IntNode(31)).id
        store.add(branch, CATEGORY, name3, Signature.of(TEST_USER), null, IntNode(32)).id
        val id33 = store.add(branch, CATEGORY, name3, Signature.of(TEST_USER), null, IntNode(33)).id
        // Gets last by name in category
        val records = store.findLastRecordsByNameInCategory(branch, CATEGORY)
        Assert.assertEquals(3, records.size.toLong())
        // Checks
        Assert.assertEquals(id33.toLong(), records[0].id.toLong())
        Assert.assertEquals(id22.toLong(), records[1].id.toLong())
        Assert.assertEquals(id12.toLong(), records[2].id.toLong())
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun addObject() { // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val record = store.addObject(branch, CATEGORY, name, Signature.of(TEST_USER), null, 15)
        // Checks
        Assert.assertNotNull(record)
        Assert.assertTrue(record.id > 0)
        Assert.assertEquals(CATEGORY, record.category)
        Assert.assertNotNull(record.signature)
        Assert.assertEquals(name, record.name)
        Assert.assertNull(record.groupName)
        Assert.assertEquals(branch.projectEntityType, record.entity.projectEntityType)
        Assert.assertEquals(branch.id, record.entity.id)
        TestUtils.assertJsonEquals(
                IntNode(15),
                record.data
        )
    }

    @Test
    @Throws(JsonProcessingException::class)
    fun replaceOrAddObject() { // Entity
        val branch = do_create_branch()
        // Adds some data
        val name = TestUtils.uid("T")
        val record = store.addObject(branch, CATEGORY, name, Signature.of(TEST_USER), null, 15)
        // Checks
        Assert.assertTrue(record.id > 0)
        Assert.assertEquals(CATEGORY, record.category)
        TestUtils.assertJsonEquals(
                IntNode(15),
                record.data
        )
        // Updates the same name
        val secondRecord = store.replaceOrAddObject(branch, CATEGORY, name, Signature.of(TEST_USER), null, 16)
        // Checks
        Assert.assertEquals(record.id.toLong(), secondRecord.id.toLong())
        TestUtils.assertJsonEquals(
                IntNode(16),
                secondRecord.data
        )
    }

    // Entity
    @Test
    fun funByCategoryAndName() {
        // Entity
        val branch = do_create_branch()
        // Adds some data
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 1) // offset: 3
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 2) // offset: 2
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 3) // offset: 1
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 4) // offset: 0
        store.addObject(branch, "C1", "N2", Signature.of(TEST_USER), null, 5)
        store.addObject(branch, "C2", "N3", Signature.of(TEST_USER), null, 6)
        // Query with pagination
        val records = store.getByCategoryAndName(branch, "C1", "N1", 2, 1)
        // Checks the results
        Assert.assertEquals(1, records.size.toLong())
        Assert.assertEquals(2, records[0].data.asInt().toLong())
        // Count
        Assert.assertEquals(4, store.getCountByCategoryAndName(branch, "C1", "N1").toLong())
    }

    // Entity
    @Test
    fun getByCategory() {
        // Entity
        val branch = do_create_branch()
        // Adds some data
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 1) // offset: 4
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 2) // offset: 3
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 3) // offset: 2
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 4) // offset: 1
        store.addObject(branch, "C1", "N2", Signature.of(TEST_USER), null, 5) // offset: 0
        store.addObject(branch, "C2", "N3", Signature.of(TEST_USER), null, 6)
        // Query with pagination
        val records = store.getByCategory(branch, "C1", 2, 1)
        // Checks the results
        Assert.assertEquals(1, records.size.toLong())
        Assert.assertEquals(3, records[0].data.asInt().toLong())
        // Count
        Assert.assertEquals(5, store.getCountByCategory(branch, "C1").toLong())
    }

    // Entities
    @Test
    fun getByFilter() { // Entities
        val branch1 = do_create_branch()
        val branch2 = do_create_branch()
        // Adds some data
        store.deleteAll()
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 1)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 2)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 3)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 4)
        store.addObject(branch1, "C1", "N2", Signature.of(TEST_USER), null, 5)
        store.addObject(branch1, "C2", "N3", Signature.of(TEST_USER), null, 6)
        store.addObject(branch2, "C1", "N1", Signature.of(TEST_USER), null, 7)
        store.addObject(branch2, "C1", "N2", Signature.of(TEST_USER), null, 8)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 9)
        // Checks
        Assert.assertEquals(6, store.getByFilter(
                EntityDataStoreFilter(branch1)
        ).size.toLong())
        Assert.assertEquals(5, store.getByFilter(
                EntityDataStoreFilter(branch1)
                        .withCategory("C1")
        ).size.toLong())
        Assert.assertEquals(4, store.getByFilter(
                EntityDataStoreFilter(branch1)
                        .withCategory("C1")
                        .withName("N1")
        ).size.toLong())
    }

    @Test
    fun countByFilter() { // Entities
        val branch1 = do_create_branch()
        val branch2 = do_create_branch()
        // Adds some data
        store.deleteAll()
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 1)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 2)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 3)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 4)
        store.addObject(branch1, "C1", "N2", Signature.of(TEST_USER), null, 5)
        store.addObject(branch1, "C2", "N3", Signature.of(TEST_USER), null, 6)
        store.addObject(branch2, "C1", "N1", Signature.of(TEST_USER), null, 7)
        store.addObject(branch2, "C1", "N2", Signature.of(TEST_USER), null, 8)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 9)
        // Checks
        Assert.assertEquals(9, store.getCountByFilter(
                EntityDataStoreFilter()
        ).toLong())
        Assert.assertEquals(6, store.getCountByFilter(
                EntityDataStoreFilter()
                        .withEntity(branch1)
        ).toLong())
        Assert.assertEquals(5, store.getCountByFilter(
                EntityDataStoreFilter()
                        .withEntity(branch1)
                        .withCategory("C1")
        ).toLong())
        Assert.assertEquals(4, store.getCountByFilter(
                EntityDataStoreFilter()
                        .withEntity(branch1)
                        .withCategory("C1")
                        .withName("N1")
        ).toLong())
        // Combinations
        Assert.assertEquals(7, store.getCountByFilter(
                EntityDataStoreFilter()
                        .withCategory("C1")
        ).toLong())
        Assert.assertEquals(2, store.getCountByFilter(
                EntityDataStoreFilter()
                        .withName("N3")
        ).toLong())
    }

    @Test
    fun deleteAllByFilter() { // Entities
        val branch1 = do_create_branch()
        val branch2 = do_create_branch()
        // Adds some data
        store.deleteAll()
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 1)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 2)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 3)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 4)
        store.addObject(branch1, "C1", "N2", Signature.of(TEST_USER), null, 5)
        store.addObject(branch1, "C2", "N3", Signature.of(TEST_USER), null, 6)
        store.addObject(branch2, "C1", "N1", Signature.of(TEST_USER), null, 7)
        store.addObject(branch2, "C1", "N2", Signature.of(TEST_USER), null, 8)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 9)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 10)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 11)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 12)
        // Checks
        Assert.assertEquals(12, store.deleteByFilter(
                EntityDataStoreFilter()
        ).toLong())
    }

    @Test
    fun deleteRangeByFilter() {
        val branch = do_create_branch()
        // Adds some data
        store.deleteAll()
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 1)
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 2)
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 3)
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 4) // Deletion
        store.addObject(branch, "C1", "N2", Signature.of(TEST_USER), null, 5) // Deletion
        store.addObject(branch, "C2", "N3", Signature.of(TEST_USER), null, 6) // Deletion
        store.addObject(branch, "C1", "N1", Signature.of(TEST_USER), null, 7)
        store.addObject(branch, "C1", "N2", Signature.of(TEST_USER), null, 8)
        store.addObject(branch, "C2", "N3", Signature.of(TEST_USER), null, 9)
        // Checks
        Assert.assertEquals(3, store.deleteRangeByFilter(
                EntityDataStoreFilter()
                        .withOffset(3)
                        .withCount(3)
        ).toLong())
        // Checks end & start records are still there
        val list = store.getByFilter(EntityDataStoreFilter(entity = branch)).map {
            it.data.asInt()
        }
        assertEquals(
                listOf(9, 8, 7, 3, 2, 1), // 6, 5 & 4 have been deleted
                list
        )
    }

    @Test
    fun deleteByBranchByFilter() { // Entities
        val branch1 = do_create_branch()
        val branch2 = do_create_branch()
        // Adds some data
        store.deleteAll()
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 1)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 2)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 3)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 4)
        store.addObject(branch1, "C1", "N2", Signature.of(TEST_USER), null, 5)
        store.addObject(branch1, "C2", "N3", Signature.of(TEST_USER), null, 6)
        store.addObject(branch2, "C1", "N1", Signature.of(TEST_USER), null, 7)
        store.addObject(branch2, "C1", "N2", Signature.of(TEST_USER), null, 8)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 9)
        // Checks
        Assert.assertEquals(6, store.deleteByFilter(
                EntityDataStoreFilter()
                        .withEntity(branch1)
        ).toLong())
    }

    @Test
    fun deleteByBranchAndCategoryByFilter() { // Entities
        val branch1 = do_create_branch()
        val branch2 = do_create_branch()
        // Adds some data
        store.deleteAll()
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 1)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 2)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 3)
        store.addObject(branch1, "C1", "N1", Signature.of(TEST_USER), null, 4)
        store.addObject(branch1, "C1", "N2", Signature.of(TEST_USER), null, 5)
        store.addObject(branch1, "C2", "N3", Signature.of(TEST_USER), null, 6)
        store.addObject(branch2, "C1", "N1", Signature.of(TEST_USER), null, 7)
        store.addObject(branch2, "C1", "N2", Signature.of(TEST_USER), null, 8)
        store.addObject(branch2, "C2", "N3", Signature.of(TEST_USER), null, 9)
        // Checks
        Assert.assertEquals(5, store.deleteByFilter(
                EntityDataStoreFilter()
                        .withEntity(branch1)
                        .withCategory("C1")
        ).toLong())
    }

    companion object {
        const val CATEGORY = "TEST"
        const val TEST_USER = "test"
    }
}