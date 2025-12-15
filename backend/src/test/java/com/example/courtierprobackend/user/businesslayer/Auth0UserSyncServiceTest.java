package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0Role;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient.Auth0User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.mockito.Mockito.*;

class Auth0UserSyncServiceTest {

    @Mock
    private Auth0ManagementClient auth0Client;

    @Mock
    private UserAccountRepository userRepository;

    @InjectMocks
    private Auth0UserSyncService syncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void syncUsersFromAuth0_CreateNewUser() {
        // Arrange
        String auth0Id = "auth0|123";
        String email = "test@example.com";
        Auth0User auth0User = new Auth0User(auth0Id, email, "First", "Last", Collections.emptyMap());
        List<Auth0User> auth0Users = List.of(auth0User);
        List<Auth0Role> roles = List.of(new Auth0Role("roleId", "BROKER"));

        when(auth0Client.listAllUsers()).thenReturn(auth0Users);
        when(auth0Client.getUserRoles(auth0Id)).thenReturn(roles);
        when(auth0Client.mapRoleToUserRole(roles)).thenReturn(UserRole.BROKER);
        when(userRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        verify(userRepository).save(argThat(user -> 
            user.getAuth0UserId().equals(auth0Id) &&
            user.getEmail().equals(email) &&
            user.getRole() == UserRole.BROKER
        ));
    }

    @Test
    void syncUsersFromAuth0_UpdateExistingUser() {
        // Arrange
        String auth0Id = "auth0|123";
        String email = "update@example.com";
        Auth0User auth0User = new Auth0User(auth0Id, email, "NewFirst", "NewLast", Collections.emptyMap());
        
        UserAccount existingUser = new UserAccount(auth0Id, email, "OldFirst", "OldLast", UserRole.BROKER, "en");
        
        List<Auth0User> auth0Users = List.of(auth0User);
        List<Auth0Role> roles = List.of(new Auth0Role("roleId", "BROKER"));

        when(auth0Client.listAllUsers()).thenReturn(auth0Users);
        when(auth0Client.getUserRoles(auth0Id)).thenReturn(roles);
        when(auth0Client.mapRoleToUserRole(roles)).thenReturn(UserRole.BROKER);
        when(userRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(existingUser));
        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        verify(userRepository).save(argThat(user -> 
            user.getFirstName().equals("NewFirst") &&
            user.getLastName().equals("NewLast")
        ));
    }

    @Test
    void syncUsersFromAuth0_MatchByEmail_UpdateAuth0Id() {
        // Arrange
        String newAuth0Id = "auth0|new";
        String email = "match@example.com";
        Auth0User auth0User = new Auth0User(newAuth0Id, email, "First", "Last", Collections.emptyMap());
        
        // Existing user has different auth0 ID (e.g. from seed or migration)
        UserAccount existingUser = new UserAccount("auth0|old", email, "First", "Last", UserRole.BROKER, "en");

        when(auth0Client.listAllUsers()).thenReturn(List.of(auth0User));
        when(auth0Client.getUserRoles(newAuth0Id)).thenReturn(Collections.emptyList());
        when(auth0Client.mapRoleToUserRole(any())).thenReturn(UserRole.BROKER);
        
        // Not found by ID, but found by email
        when(userRepository.findByAuth0UserId(newAuth0Id)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        
        when(userRepository.findAll()).thenReturn(List.of(existingUser));

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        verify(userRepository).save(argThat(user -> 
            user.getAuth0UserId().equals(newAuth0Id) &&
            user.getEmail().equals(email)
        ));
    }

    @Test
    void syncUsersFromAuth0_DeleteMissingUsers() {
        // Arrange
        Auth0User activeUser = new Auth0User("auth0|active", "active@exa.com", "A", "B", Collections.emptyMap());
        
        UserAccount userToDelete = new UserAccount("auth0|gone", "gone@exa.com", "G", "O", UserRole.CLIENT, "en");
        UserAccount userToKeep = new UserAccount("auth0|active", "active@exa.com", "A", "B", UserRole.CLIENT, "en");

        when(auth0Client.listAllUsers()).thenReturn(List.of(activeUser));
        when(auth0Client.getUserRoles(any())).thenReturn(Collections.emptyList());
        when(auth0Client.mapRoleToUserRole(any())).thenReturn(UserRole.CLIENT);
        
        when(userRepository.findByAuth0UserId("auth0|active")).thenReturn(Optional.of(userToKeep));
        when(userRepository.findAll()).thenReturn(List.of(userToKeep, userToDelete));

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        verify(userRepository).delete(userToDelete);
        verify(userRepository, never()).delete(userToKeep);
    }
    
    @Test
    void syncUsersFromAuth0_ReactivateUser() {
        // Arrange
        String auth0Id = "auth0|123";
        Auth0User auth0User = new Auth0User(auth0Id, "reactivate@example.com", "First", "Last", Collections.emptyMap());
        
        UserAccount inactiveUser = new UserAccount(auth0Id, "reactivate@example.com", "First", "Last", UserRole.BROKER, "en");
        inactiveUser.setActive(false);

        when(auth0Client.listAllUsers()).thenReturn(List.of(auth0User));
        when(auth0Client.getUserRoles(auth0Id)).thenReturn(Collections.emptyList());
        when(auth0Client.mapRoleToUserRole(any())).thenReturn(UserRole.BROKER);
        when(userRepository.findByAuth0UserId(auth0Id)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.findAll()).thenReturn(List.of(inactiveUser));

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        verify(userRepository).save(argThat(user -> user.isActive()));
    }
    
    @Test
    void syncUsersFromAuth0_DeletesFakeSeededUsers() {
        // Arrange
        // Fake seeded user pattern: auth0|client\d+
        UserAccount fakeUser = new UserAccount("auth0|client123", "fake@exa.com", "Fake", "User", UserRole.CLIENT, "en");
        
        when(auth0Client.listAllUsers()).thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(fakeUser));

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        verify(userRepository).delete(fakeUser);
    }

    @Test
    void syncUsersFromAuth0_HandlesExceptionForSingleUser_AndContinues() {
        // Arrange
        Auth0User badUser = new Auth0User("auth0|bad", "bad@exa.com", "Bad", "User", Collections.emptyMap());
        Auth0User goodUser = new Auth0User("auth0|good", "good@exa.com", "Good", "User", Collections.emptyMap());
        List<Auth0User> users = List.of(badUser, goodUser);
        
        when(auth0Client.listAllUsers()).thenReturn(users);
        
        // First user throws exception
        when(auth0Client.getUserRoles("auth0|bad")).thenThrow(new RuntimeException("Sync fail"));
        
        // Second user succeeds
        List<Auth0Role> roles = List.of(new Auth0Role("roleId", "BROKER"));
        when(auth0Client.getUserRoles("auth0|good")).thenReturn(roles);
        when(auth0Client.mapRoleToUserRole(roles)).thenReturn(UserRole.BROKER);
        when(userRepository.findByAuth0UserId("auth0|good")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("good@exa.com")).thenReturn(Optional.empty());

        // Act
        syncService.syncUsersFromAuth0();

        // Assert
        // Verify good user was saved
        verify(userRepository).save(argThat(user -> user.getAuth0UserId().equals("auth0|good")));
        // Verify bad user caused no save (implicitly, or explicitly verify exact count)
        verify(userRepository, times(1)).save(any(UserAccount.class));
    }
}
