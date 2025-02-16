package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.google.common.collect.Lists;
import com.liferay.portal.model.User;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Jonathan Gamba
 *         Date: 6/20/13
 */

public class RoleAPITest extends IntegrationTestBase {

    private static DotCacheAdministrator cache;
    private static User systemUser;

    //Current group keys
    private String primaryGroup = "dotCMSRoleCache";
    private String keyGroup = "dotCMSRoleKeyCache";
    private String userGroup = "dotCMSUserRoleCache";
    private String layoutGroup = "dotCMSLayoutCache";
    private String rootRolesGroup = "dotCMSRootRolesCache";

    @BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        cache = CacheLocator.getCacheAdministrator();

        //Setting the test user
        systemUser = APILocator.getUserAPI().getSystemUser();
    }

    /**
     * Testing {@link RoleAPI#loadRolesForUser(String, boolean)}, {@link RoleAPI#loadRolesForUser(String)},
     * {@link RoleAPI#doesUserHaveRole(com.liferay.portal.model.User, Role)} and the cache for Roles in general
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotCacheException
     * @see RoleAPI
     * @see RoleFactory
     * @see RoleCache
     */
    @Test
    public void loadRolesForUser () throws DotDataException, DotSecurityException, DotCacheException {

        RoleAPI roleAPI = APILocator.getRoleAPI();
        UserAPI userAPI = APILocator.getUserAPI();

        // Creating test roles
        final Role parentRole1 = new RoleDataGen().name("parentRole1_" + System.currentTimeMillis()).nextPersisted();
        final Role parentRole2 = new RoleDataGen().name("parentRole2_" + System.currentTimeMillis()).parent(parentRole1.getId()).nextPersisted();
        final Role childTestRole1 = new RoleDataGen().name("childTestRole1_" + System.currentTimeMillis()).parent(parentRole2.getId()).nextPersisted();

        // Creating test users
        new UserDataGen().roles(parentRole1).nextPersisted();
        new UserDataGen().roles(parentRole1).nextPersisted();

        //Search for the current root roles
        List<Role> originalRootRoles = roleAPI.findRootRoles();
        assertTrue( originalRootRoles != null && !originalRootRoles.isEmpty() );

        //Search for a user associated with our test role
        List<User> foundUsers = roleAPI.findUsersForRole( parentRole1 );
        assertTrue( foundUsers != null && !foundUsers.isEmpty() );
        User testUser = foundUsers.get( 0 );

        //Verify if we find the implicit roles
        List<Role> foundRoles = roleAPI.loadRolesForUser( testUser.getUserId(), true );
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertTrue( foundRoles.size() == 4 );// 3 roles + User role

        //Cache validations
        List<Role> originalCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertTrue( originalCachedRootRoles != null && !originalCachedRootRoles.isEmpty() );
        assertEquals( originalRootRoles.size(), originalCachedRootRoles.size() );

        //-----------------------------------------------
        //ADDING NEW RECORDS
        //-----------------------------------------------


        //Creating a new test user
        String time = String.valueOf( new Date().getTime() );
        User newUser = userAPI.createUser( time + "@test.com", time + "@test.com" );
        newUser.setFirstName( "Test" );
        newUser.setLastName( "User" );
        userAPI.save( newUser, systemUser, false );

        //Creating test roles
        Role rootRole = new Role();
        rootRole.setName( "Test Root Role_" + time );
        rootRole.setRoleKey( "testRootRole_" + time );
        rootRole.setEditUsers( true );
        rootRole.setEditPermissions( true );
        rootRole.setEditLayouts( true );
        rootRole.setDescription( "Test Root Role" );
        roleAPI.save( rootRole );//This will clear the cache for the root roles -> cache.clearRootRoleCache
        

        
        //Verify for this one
        verifyNewRole( rootRole );
        

        //The root roles changed
        List<Role> newCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertNull( newCachedRootRoles );

        List<Role> newRootRoles = roleAPI.findRootRoles();//This should populate again the cache for the roots roles
        assertTrue( newRootRoles != null && !newRootRoles.isEmpty() );
        assertEquals( newRootRoles.size(), originalRootRoles.size() + 1 );

        newCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertTrue( newCachedRootRoles != null && !newCachedRootRoles.isEmpty() );
        assertEquals( newCachedRootRoles.size(), originalCachedRootRoles.size() + 1 );

        //Child role
        Role childRole = new Role();
        childRole.setName( "Test Child Role 1_" + time );
        childRole.setRoleKey( "testChildRole1_" + time );
        childRole.setEditUsers( true );
        childRole.setEditPermissions( true );
        childRole.setEditLayouts( true );
        childRole.setDescription( "Test Child Role 1" );
        childRole.setParent( rootRole.getId() );
        roleAPI.save( childRole );

        //Verify for this one
        verifyNewRole( childRole );

        //Child role 2
        Role childRole2 = new Role();
        childRole2.setName( "Test Child Role 2_" + time );
        childRole2.setRoleKey( "testChildRole2_" + time );
        childRole2.setEditUsers( true );
        childRole2.setEditPermissions( true );
        childRole2.setEditLayouts( true );
        childRole2.setDescription( "Test Child Role 2" );
        childRole2.setParent( childRole.getId() );
        roleAPI.save( childRole2 );
        //Verify for this one

        //Verify for this one
        verifyNewRole( childRole );


        //Add to the test user the root role
        roleAPI.addRoleToUser( rootRole, newUser );//This save cleans from cache the roles associated to this user
        List<RoleCache.UserRoleCacheHelper> userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );
        assertNull( userRoles );



        //Verify if we find the implicit roles
        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), true );//We know we have too 3 levels here: "Test Root Role" -> "Test Child Role 1" -> "Test Child Role 2" + User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 4 );
        //Cache
        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );//"Test Root Role" -> "Test Child Role 1" -> "Test Child Role 2" + User role
        assertTrue( userRoles != null && !userRoles.isEmpty() );
        assertEquals( userRoles.size(), 4 );

        //Without implicit roles
        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), false );//"Test Root Role" + User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 2 );

        //Test the doesUserHaveRole(...) method
        boolean does = roleAPI.doesUserHaveRole( newUser, rootRole.getId() );
        assertTrue( does );
        does = roleAPI.doesUserHaveRole( newUser, childRole.getId() );
        assertTrue( does );
        does = roleAPI.doesUserHaveRole( newUser, childRole2.getId() );
        assertTrue( does );
        does = roleAPI.doesUserHaveRole( newUser, childTestRole1.getId() );
        assertFalse( does );

        //-----------------------------------------------
        //REMOVING RECORDS
        //-----------------------------------------------
        //Delete the childRole2

        String key = primaryGroup + childRole2.getId();
        roleAPI.delete( childRole2 );//Should clean up the cache
        Object cachedRole = cache.get( key, primaryGroup );
        assertNull( cachedRole );


        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );
        assertNull( userRoles );

        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), true );//2 levels: "Test Root Role" -> "Test Child Role 1" + User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 3 );
        //Cache
        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );//2 levels: "Test Root Role" -> "Test Child Role 1" + User role
        assertTrue( userRoles != null && !userRoles.isEmpty() );
        assertEquals( userRoles.size(), 3 );

        //--------
        //Delete the childRole

        key = primaryGroup + childRole.getId();
        roleAPI.delete( childRole );//Should clean up the cache
        cachedRole = cache.get( key, primaryGroup );
        assertNull( cachedRole );


        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );
        assertNull( userRoles );

        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), true );//1 level: "Test Root Role" + User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 2 );
        //Cache
        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );//1 level: "Test Root Role" + User role
        assertTrue( userRoles != null && !userRoles.isEmpty() );
        assertEquals( userRoles.size(), 2 );

        //--------
        //Delete the rootRole

        key = primaryGroup + rootRole.getId();
        roleAPI.delete( rootRole );//Should clean up the cache
        cachedRole = cache.get( key, primaryGroup );
        assertNull( cachedRole );


        //Cache
        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );
        assertNull( userRoles );

        //Verify the roots
        newCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertNull( newCachedRootRoles );

        newRootRoles = roleAPI.findRootRoles();//This should populate again the cache for the roots roles
        assertTrue( newRootRoles != null && !newRootRoles.isEmpty() );
        assertEquals( newRootRoles.size(), originalRootRoles.size() );

        newCachedRootRoles = CacheLocator.getRoleCache().getRootRoles();
        assertTrue( newCachedRootRoles != null && !newCachedRootRoles.isEmpty() );
        assertEquals( newCachedRootRoles.size(), originalCachedRootRoles.size() );

        //And finally....
        //Verify if we find the implicit roles
        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), true );//At this point we just have the User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 1 );

        foundRoles = roleAPI.loadRolesForUser( newUser.getUserId(), false );//At this point we just have the User role
        assertTrue( foundRoles != null && !foundRoles.isEmpty() );
        assertEquals( foundRoles.size(), 1 );

        userRoles = CacheLocator.getRoleCache().getRoleIdsForUser( newUser.getUserId() );//At this point we just have the User role
        assertTrue( userRoles != null && !userRoles.isEmpty() );
        assertEquals( userRoles.size(), 1 );
    }

    /**
     * Verify if a given Roles was saved properly
     *
     * @param testRole
     * @throws DotCacheException
     * @throws DotDataException
     */
    private void verifyNewRole ( Role testRole ) throws DotCacheException, DotDataException {

        RoleAPI roleAPI = APILocator.getRoleAPI();
        Role foundRole = roleAPI.loadRoleById( testRole.getId() );
        assertNotNull( foundRole );
        
        String key = primaryGroup + testRole.getId();
        Object cachedRole = cache.get( key, primaryGroup );
        assertNotNull( cachedRole );
        assertEquals( cachedRole, testRole );



        assertEquals( cachedRole, foundRole );
    }

    @Test
    public void test_isParentRole() {

        RoleAPI roleAPI = APILocator.getRoleAPI();

        Role parentRole = new Role();
        Role childRole = new Role();
        Role grandChildRole = new Role();

        Role secondParentRole = new Role();
        Role secondChildRole = new Role();

        try {
            // Create Parent Role.
            parentRole.setName("Parent Role");
            parentRole.setEditUsers(true);
            parentRole.setEditPermissions(true);
            parentRole.setEditLayouts(true);
            parentRole.setDescription("Parent Role");
            parentRole = roleAPI.save(parentRole);

            // Create Child Role Role.
            childRole.setName("Child Role");
            childRole.setEditUsers(true);
            childRole.setEditPermissions(true);
            childRole.setEditLayouts(true);
            childRole.setDescription("Child Role");
            childRole.setParent(parentRole.getId());
            childRole = roleAPI.save(childRole);

            // Create Grandchild Role Role.
            grandChildRole.setName("Grandchild Role");
            grandChildRole.setEditUsers(true);
            grandChildRole.setEditPermissions(true);
            grandChildRole.setEditLayouts(true);
            grandChildRole.setDescription("Grandchild Role");
            grandChildRole.setParent(childRole.getId());
            grandChildRole = roleAPI.save(grandChildRole);

            assertTrue(roleAPI.isParentRole(parentRole, childRole));
            assertTrue(roleAPI.isParentRole(parentRole, grandChildRole));
            assertTrue(roleAPI.isParentRole(childRole, grandChildRole));

            assertFalse(roleAPI.isParentRole(grandChildRole, parentRole));
            assertFalse(roleAPI.isParentRole(childRole, parentRole));
            assertFalse(roleAPI.isParentRole(grandChildRole, grandChildRole));

            // Now let's create a sibling branch of roles.
            // Create Second Parent Role.
            secondParentRole.setName("Second Parent Role");
            secondParentRole.setEditUsers(true);
            secondParentRole.setEditPermissions(true);
            secondParentRole.setEditLayouts(true);
            secondParentRole.setDescription("Second Parent Role");
            secondParentRole = roleAPI.save(secondParentRole);

            // Create Second Child Role Role.
            secondChildRole.setName("Second Child Role");
            secondChildRole.setEditUsers(true);
            secondChildRole.setEditPermissions(true);
            secondChildRole.setEditLayouts(true);
            secondChildRole.setDescription("Second Child Role");
            secondChildRole.setParent(secondParentRole.getId());
            secondChildRole = roleAPI.save(secondChildRole);

            assertFalse(roleAPI.isParentRole(parentRole, secondChildRole));
            assertFalse(roleAPI.isParentRole(parentRole, secondParentRole));

        } catch (DotSecurityException | DotDataException e) {
            fail(e.getMessage());
        } finally {
            final List<Role> rolesToDelete = Lists
                    .newArrayList(grandChildRole, childRole, parentRole, secondChildRole,
                            secondParentRole);
            cleanRoles(rolesToDelete);
        }
    }

    /**
     * Tests API method isSiblingRole with sibling and non-sibling roles
     */

    @Test
    public void testIsSiblingRole() throws DotDataException, DotSecurityException {

        Role parentA = null;
        Role childA = null;
        Role childB = null;
        Role grandchildA = null;
        Role parentB = null;
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        final long time = System.currentTimeMillis();

        try {
            // create parent role
            parentA = new RoleDataGen().name("parentA"+time).nextPersisted();
            // create child role
            childA = new RoleDataGen().name("childA"+time).parent(parentA.getId()).nextPersisted();
            // another child
            childB = new RoleDataGen().name("childB"+time).parent(parentA.getId()).nextPersisted();
            // grand child
            grandchildA = new RoleDataGen().name("grandchildA"+time).parent(childA.getId())
                    .nextPersisted();
            // parent's sibling
            parentB = new RoleDataGen().name("parentB"+time).nextPersisted();

            assertTrue(roleAPI.isSiblingRole(childA, childB));

            assertTrue(roleAPI.isSiblingRole(parentA, parentB));

            assertFalse(roleAPI.isSiblingRole(parentA, childA));

            assertFalse(roleAPI.isSiblingRole(grandchildA, childB));
        } finally {
            roleAPI.delete(grandchildA);
            roleAPI.delete(childA);
            roleAPI.delete(childB);
            roleAPI.delete(parentA);
            roleAPI.delete(parentB);
        }
    }

}