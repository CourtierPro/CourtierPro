package com.example.courtierprobackend.user.businesslayer;

import com.example.courtierprobackend.Organization.businesslayer.OrganizationSettingsService;
import com.example.courtierprobackend.Organization.presentationlayer.model.OrganizationSettingsResponseModel;
import com.example.courtierprobackend.common.exceptions.InternalServerException;

import com.example.courtierprobackend.user.mapper.UserMapper;
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
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private com.example.courtierprobackend.notifications.businesslayer.NotificationService notificationService;

    @InjectMocks
    private UserProvisioningService service;

    @Test
    void getAllUsers_ReturnsAllUsers() {
        // Arrange
        UserAccount user1 = new UserAccount("auth0|123", "user1@test.com", "John", "Doe", UserRole.BROKER, "en");
        UserAccount user2 = new UserAccount("auth0|456", "user2@test.com", "Jane", "Smith", UserRole.ADMIN, "fr");

        UserResponse response1 = UserResponse.builder()
                .id(user1.getId().toString())
                .email("user1@test.com")
                .firstName("John")
                .lastName("Doe")
                .role("BROKER")
                .active(true)
                .preferredLanguage("en")
                .build();

        UserResponse response2 = UserResponse.builder()
                .id(user2.getId().toString())
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
    void getClients_ReturnsOnlyClients() {
        // Arrange
        UserAccount client1 = new UserAccount("auth0|c1", "c1@test.com", "Client", "One", UserRole.CLIENT, "en");
        UserAccount client2 = new UserAccount("auth0|c2", "c2@test.com", "Client", "Two", UserRole.CLIENT, "fr");

        UserResponse resp1 = UserResponse.builder().email("c1@test.com").role("CLIENT").build();
        UserResponse resp2 = UserResponse.builder().email("c2@test.com").role("CLIENT").build();

        when(userAccountRepository.findByRole(UserRole.CLIENT)).thenReturn(List.of(client1, client2));
        when(userMapper.toResponse(client1)).thenReturn(resp1);
        when(userMapper.toResponse(client2)).thenReturn(resp2);

        // Act
        List<UserResponse> result = service.getClients();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("CLIENT");
        assertThat(result.get(1).getRole()).isEqualTo("CLIENT");
        verify(userAccountRepository).findByRole(UserRole.CLIENT);
    }

    @Test
    void getBrokers_ReturnsOnlyBrokers() {
        UserAccount broker1 = new UserAccount("auth0|b1", "b1@test.com", "Broker", "One", UserRole.BROKER, "en");
        UserAccount broker2 = new UserAccount("auth0|b2", "b2@test.com", "Broker", "Two", UserRole.BROKER, "fr");

        UserResponse resp1 = UserResponse.builder().email("b1@test.com").role("BROKER").build();
        UserResponse resp2 = UserResponse.builder().email("b2@test.com").role("BROKER").build();

        when(userAccountRepository.findByRole(UserRole.BROKER)).thenReturn(List.of(broker1, broker2));
        when(userMapper.toResponse(broker1)).thenReturn(resp1);
        when(userMapper.toResponse(broker2)).thenReturn(resp2);

        List<UserResponse> result = service.getBrokers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo("BROKER");
        verify(userAccountRepository).findByRole(UserRole.BROKER);
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
                .id(savedUser.getId().toString())
                .email("newuser@test.com")
                .firstName("New")
                .lastName("User")
                .role("BROKER")
                .active(true)
                .preferredLanguage("en")
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|789");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://setup-password-url.com");
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
                .id(savedUser.getId().toString())
                .email("newuser@test.com")
                .preferredLanguage("fr")
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), eq("fr")))
                .thenReturn("auth0|999");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://setup-url.com");
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
                .thenReturn("auth0|555");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://password-setup.com");
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
                .id(userId.toString())
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
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.NotFoundException.class)
                .hasMessageContaining("not found");

        verify(userAccountRepository).findById(userId);
        verify(userAccountRepository, never()).save(any());
        verify(auth0ManagementClient, never()).setBlocked(anyString(), anyBoolean());
    }
    @Test
    void triggerPasswordReset_Success() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("auth0|123", "user@test.com", "John", "Doe", UserRole.BROKER, "en");
        
        // when(organizationSettingsService.getSettings())
        //      .thenReturn(OrganizationSettingsResponseModel.builder().defaultLanguage("en").build());
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auth0ManagementClient.createPasswordChangeTicket("auth0|123")).thenReturn("https://ticket.url");
        when(emailService.sendPasswordSetupEmail("user@test.com", "https://ticket.url", "en")).thenReturn(true);

        // Act
        service.triggerPasswordReset(userId);

        // Assert
        verify(emailService).sendPasswordSetupEmail("user@test.com", "https://ticket.url", "en");
    }

    @Test
    void triggerPasswordReset_UserNotFound_ThrowsException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.triggerPasswordReset(userId))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.NotFoundException.class)
                .hasMessageContaining("User not found");
        
        verify(emailService, never()).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    @Test
    void triggerPasswordReset_NoAuth0Id_ThrowsException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount(null, "user@test.com", "John", "Doe", UserRole.BROKER, "en");
        
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> service.triggerPasswordReset(userId))
                .isInstanceOf(com.example.courtierprobackend.common.exceptions.BadRequestException.class)
                .hasMessageContaining("User has no Auth0 ID");
                
        verify(emailService, never()).sendPasswordSetupEmail(anyString(), anyString(), anyString());
    }

    @Test
    void createUser_ClientRole_SendsWelcomeNotification() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("client@test.com")
                .firstName("Client")
                .lastName("User")
                .role("CLIENT")
                .preferredLanguage("en")
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();

        UserAccount savedUser = new UserAccount("auth0|client", "client@test.com", "Client", "User", UserRole.CLIENT, "en");

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId().toString())
                .email("client@test.com")
                .firstName("Client")
                .lastName("User")
                .role("CLIENT")
                .active(true)
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|client");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://password-url.com");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        // Act
        service.createUser(request);

        // Assert - Verify welcome notification was sent for CLIENT
        verify(notificationService).createNotification(
                eq(savedUser.getId().toString()),
                eq("notifications.welcome.title"),
                eq("notifications.welcome.message"),
                anyMap(),
                isNull(),
                eq(com.example.courtierprobackend.notifications.datalayer.enums.NotificationCategory.WELCOME)
        );
    }

    @Test
    void createUser_BrokerRole_DoesNotSendWelcomeNotification() {
        // Arrange
        CreateUserRequest request = CreateUserRequest.builder()
                .email("broker@test.com")
                .firstName("Broker")
                .lastName("User")
                .role("BROKER")
                .preferredLanguage("en")
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();

        UserAccount savedUser = new UserAccount("auth0|broker", "broker@test.com", "Broker", "User", UserRole.BROKER, "en");

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId().toString())
                .email("broker@test.com")
                .firstName("Broker")
                .lastName("User")
                .role("BROKER")
                .active(true)
                .build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|broker");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://password-url.com");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(userResponse);

        // Act
        service.createUser(request);

        // Assert - Verify NO welcome notification was sent for BROKER
        verify(notificationService, never()).createNotification(
                any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void createUser_WhenRequestAndOrgLanguageBlank_FallsBackToEnglish() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("english@test.com")
                .firstName("Eng")
                .lastName("User")
                .role("BROKER")
                .preferredLanguage(null)
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("   ")
                .build();

        UserAccount savedUser = new UserAccount("auth0|eng", "english@test.com", "Eng", "User", UserRole.BROKER, null);
        UserResponse response = UserResponse.builder().email("english@test.com").build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), eq("en")))
                .thenReturn("auth0|eng");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://ticket");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), eq("en"))).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        service.createUser(request);

        verify(auth0ManagementClient).createUser("english@test.com", "Eng", "User", UserRole.BROKER, "en");
        verify(emailService).sendPasswordSetupEmail("english@test.com", "https://ticket", "en");
    }

    @Test
    void createUser_WhenMappedEntityLanguageBlank_SetsEffectiveLanguageBeforeSave() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("mapped@test.com")
                .firstName("Mapped")
                .lastName("User")
                .role("BROKER")
                .preferredLanguage("fr")
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();

        UserAccount mapped = new UserAccount("auth0|mapped", "mapped@test.com", "Mapped", "User", UserRole.BROKER, " ");
        UserResponse response = UserResponse.builder().email("mapped@test.com").preferredLanguage("fr").build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|mapped");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://ticket");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(mapped);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(UserAccount.class))).thenReturn(response);

        service.createUser(request);

        verify(userAccountRepository).save(argThat(user -> "fr".equals(user.getPreferredLanguage())));
    }

    @Test
    void createUser_WhenPasswordSetupFails_DoesNotFailCreation() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("warn@test.com")
                .firstName("Warn")
                .lastName("User")
                .role("BROKER")
                .preferredLanguage("en")
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();
        UserAccount savedUser = new UserAccount("auth0|warn", "warn@test.com", "Warn", "User", UserRole.BROKER, "en");
        UserResponse response = UserResponse.builder().email("warn@test.com").build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|warn");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString()))
                .thenThrow(new RuntimeException("ticket failure"));
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.createUser(request));
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    void createUser_ClientRole_WhenWelcomeNotificationThrows_DoesNotFailCreation() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("notify-fail@test.com")
                .firstName("Notify")
                .lastName("Fail")
                .role("CLIENT")
                .preferredLanguage("en")
                .build();

        OrganizationSettingsResponseModel orgSettings = OrganizationSettingsResponseModel.builder()
                .defaultLanguage("en")
                .build();
        UserAccount savedUser = new UserAccount("auth0|notify", "notify-fail@test.com", "Notify", "Fail", UserRole.CLIENT, "en");
        UserResponse response = UserResponse.builder().email("notify-fail@test.com").build();

        when(organizationSettingsService.getSettings()).thenReturn(orgSettings);
        when(auth0ManagementClient.createUser(anyString(), anyString(), anyString(), any(UserRole.class), anyString()))
                .thenReturn("auth0|notify");
        when(auth0ManagementClient.createPasswordChangeTicket(anyString())).thenReturn("https://ticket");
        when(emailService.sendPasswordSetupEmail(anyString(), anyString(), anyString())).thenReturn(true);
        when(userMapper.toNewUserEntity(any(CreateUserRequest.class), anyString())).thenReturn(savedUser);
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedUser);
        when(userMapper.toResponse(savedUser)).thenReturn(response);
        doThrow(new RuntimeException("notification failure")).when(notificationService)
                .createNotification(anyString(), anyString(), anyString(), anyMap(), any(), any());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.createUser(request));
        verify(notificationService).createNotification(anyString(), anyString(), anyString(), anyMap(), any(), any());
    }

    @Test
    void triggerPasswordReset_WhenPreferredLanguageNull_UsesOrgDefaultLanguage() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("auth0|lang", "lang@test.com", "Lang", "User", UserRole.BROKER, null);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auth0ManagementClient.createPasswordChangeTicket("auth0|lang")).thenReturn("https://ticket.lang");
        when(organizationSettingsService.getSettings())
                .thenReturn(OrganizationSettingsResponseModel.builder().defaultLanguage("fr").build());
        when(emailService.sendPasswordSetupEmail("lang@test.com", "https://ticket.lang", "fr")).thenReturn(true);

        service.triggerPasswordReset(userId);

        verify(emailService).sendPasswordSetupEmail("lang@test.com", "https://ticket.lang", "fr");
    }

    @Test
    void triggerPasswordReset_WhenTicketGenerationFails_ThrowsInternalServerException() {
        UUID userId = UUID.randomUUID();
        UserAccount user = new UserAccount("auth0|fail", "fail@test.com", "Fail", "User", UserRole.BROKER, "en");

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(auth0ManagementClient.createPasswordChangeTicket("auth0|fail"))
                .thenThrow(new RuntimeException("auth0 failure"));

        assertThatThrownBy(() -> service.triggerPasswordReset(userId))
                .isInstanceOf(InternalServerException.class)
                .hasMessageContaining("Failed to trigger password reset");
    }
}
