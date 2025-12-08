import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.datamapperlayer.UserMapper;
import com.example.courtierprobackend.email.EmailService;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccount;
import com.example.courtierprobackend.user.dataaccesslayer.UserAccountRepository;
import com.example.courtierprobackend.user.dataaccesslayer.UserRole;
import com.example.courtierprobackend.user.domainclientlayer.auth0.Auth0ManagementClient;
import com.example.courtierprobackend.user.presentationlayer.request.CreateUserRequest;
import com.example.courtierprobackend.user.presentationlayer.request.UpdateStatusRequest;
import com.example.courtierprobackend.user.presentationlayer.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/**
 * Unit test for UserProvisioningService.
 * Tests business logic with mocked dependencies (repository, mapper, auth0 client).
 */
class UserProvisioningServiceTest {

    @Mock
    private OrganizationSettingsService organizationSettingsService;

    @Mock
    private Auth0ManagementClient auth0ManagementClient;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrganizationSettingsService organizationSettingsService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserProvisioningService service;

    @Test
    void getAllUsers_ReturnsAllUsers() {
        // Arrange
        UserAccount user1 = new UserAccount("auth0|123", "user1@test.com", "John", "Doe", UserRole.BROKER, "en");
        UserAccount user2 = new UserAccount("auth0|456", "user2@test.com", "Jane", "Smith", UserRole.ADMIN, "fr");

        UserResponse response1 = UserResponse.builder()
                .id(user1.getId())
                .email("user1@test.com")
                .firstName("John")
                .lastName("Doe")
                .role("BROKER")
                .active(true)
                .preferredLanguage("en")
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(user2.getId())
                .email("user2@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .role("ADMIN")
                .active(true)
                .preferredLanguage("fr")
                .build();

        when(userAccountRepository.findAll()).thenReturn(List.of(user1, user2));
        when(userMapper.toResponse(user1)).thenReturn(response1);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        // Act
        List<UserResponse> result = service.getAllUsers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@test.com");
        assertThat(result.get(1).getEmail()).isEqualTo("user2@test.com");
        verify(userAccountRepository).findAll();
    }

    @Test
    void createUser_WithValidRequest_CreatesSuccessfully() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("newuser@test.com")
                .firstName("New")
                .lastName("User")
                .role("BROKER")
                .preferredLanguage("en")
                .build();
                
        // Mock Organization Settings
        when(organizationSettingsService.getSettings())
             .thenReturn(OrganizationSettingsResponseModel.builder().defaultLanguage("en").build());

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .build();

        UserAccount savedUser = new UserAccount("auth0|789", "newuser@test.com", "New", "User", UserRole.BROKER, "en");

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId())
                .email("newuser@test.com")
                .firstName("New")
                .lastName("User")
                .role("BROKER")
                .active(true)
                .preferredLanguage("en")
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|789|https://setup-password-url.com");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        // Act
        UserResponse result = service.createUser(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("newuser@test.com");
        assertThat(result.getRole()).isEqualTo("BROKER");
        verify(auth0ManagementClient).createUser("newuser@test.com", "New", "User", UserRole.BROKER, "en");
        verify(emailService).sendPasswordSetupEmail("newuser@test.com", "https://setup-password-url.com", "en");
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void createUser_UsesOrgDefaultLanguageWhenNotProvided() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("newuser@test.com")
                .firstName("New")
                .lastName("User")
                .role("ADMIN")
                .preferredLanguage(null) // No preferred language
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("fr")
                .build();

        UserAccount savedUser = new UserAccount("auth0|999", "newuser@test.com", "New", "User", UserRole.ADMIN, "fr");

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId())
                .email("newuser@test.com")
                .preferredLanguage("fr")
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), eq("fr")))
                .thenReturn("auth0|999|https://setup-url.com");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), eq("fr"))).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        // Act
        UserResponse result = service.createUser(request);

        // Assert
        assertThat(result).isNotNull();
        verify(auth0ManagementClient).createUser("newuser@test.com", "New", "User", UserRole.ADMIN, "fr");
        verify(emailService).sendPasswordSetupEmail("newuser@test.com", "https://setup-url.com", "fr");
    }

    @Test
    void createUser_SendsPasswordSetupEmail() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("CLIENT")
                .preferredLanguage("en")
                .build();
                
        when(organizationSettingsService.getSettings())
             .thenReturn(OrganizationSettingsResponseModel.builder().defaultLanguage("en").build());

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();

        UserAccount savedUser = new UserAccount("auth0|555", "test@example.com", "Test", "User", UserRole.CLIENT, "en");

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|555|https://password-setup.com");
        when(emailService.sendPasswordSetupEmail("test@example.com", "https://password-setup.com", "en"))
                .thenReturn(true);
        when(userMapper.toNewUserEntity(any(), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any())).thenReturn(savedUser);
        when(userMapper.toResponse(any())).thenReturn(mock(UserResponse.class));

        // Act
        service.createUser(request);

        // Assert
        verify(emailService).sendPasswordSetupEmail("test@example.com", "https://password-setup.com", "en");
    }

    @Test
    void createUser_WithoutPasswordUrl_DoesNotSendEmail() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role("CLIENT")
                .preferredLanguage("en")
                .build();
                
        when(organizationSettingsService.getSettings())
             .thenReturn(OrganizationSettingsResponseModel.builder().defaultLanguage("en").build());

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();

        UserAccount savedUser = new UserAccount("auth0|555", "test@example.com", "Test", "User", UserRole.CLIENT, "en");

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0"); // No separator -> no password URL
        when(userMapper.toNewUserEntity(any(), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any())).thenReturn(savedUser);
        when(userMapper.toResponse(any())).thenReturn(mock(UserResponse.class));

        // Act
        service.createUser(request);

        // Assert
        verify(emailService, never()).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    @Test
    void updateStatus_WithValidRequest_UpdatesSuccessfully() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserAccount existingUser = new UserAccount("auth0|123", "user@test.com", "John", "Doe", UserRole.BROKER, "en");

        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .active(false)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .email("user@test.com")
                .active(false)
                .build();

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(existingUser);
        when(userMapper.toResponse(existingUser)).thenReturn(userResponse);

        // Act
        UserResponse result = service.updateStatus(userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isFalse();
        verify(userAccountRepository).findById(userId);
        verify(userAccountRepository).save(existingUser);
        verify(auth0ManagementClient).setBlocked("auth0|123", true); // active=false -> blocked=true
    }

    @Test
    void updateStatus_SyncsWithAuth0() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserAccount existingUser = new UserAccount("auth0|456", "user@test.com", "Jane", "Doe", UserRole.BROKER, "en");

        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .active(true)
                .build();

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(existingUser);
        when(userMapper.toResponse(any())).thenReturn(mock(UserResponse.class));

        // Act
        service.updateStatus(userId, request);

        // Assert
        verify(auth0ManagementClient).setBlocked("auth0|456", false); // active=true -> blocked=false
    }

    @Test
    void updateStatus_UserNotFound_ThrowsException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UpdateStatusRequest request = UpdateStatusRequest.builder()
                .active(false)
                .build();

        when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.updateStatus(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");

        verify(userAccountRepository).findById(userId);
        verify(userAccountRepository, never()).save(any());
        verify(auth0ManagementClient, never()).setBlocked(anyString(), anyBoolean());
    }
}
