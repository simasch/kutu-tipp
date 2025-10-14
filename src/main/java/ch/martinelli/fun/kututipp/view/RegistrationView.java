package ch.martinelli.fun.kututipp.view;

import ch.martinelli.fun.kututipp.exception.EmailAlreadyExistsException;
import ch.martinelli.fun.kututipp.exception.RegistrationException;
import ch.martinelli.fun.kututipp.exception.UsernameAlreadyExistsException;
import ch.martinelli.fun.kututipp.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration view for new user account creation.
 * Implements UC-001: Register Account.
 */
@AnonymousAllowed
@Route("register")
@PageTitle("Register - Kutu-Tipp")
public class RegistrationView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(RegistrationView.class);

    private final UserService userService;

    private final TextField usernameField;
    private final EmailField emailField;
    private final PasswordField passwordField;
    private final PasswordField passwordConfirmationField;
    private final Binder<RegistrationForm> binder;

    public RegistrationView(UserService userService) {
        this.userService = userService;

        // Configure layout
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Create form
        var title = new H1("Create Your Account");
        title.getStyle().set("margin-bottom", "0");

        usernameField = new TextField("Username");
        usernameField.setRequired(true);
        usernameField.setHelperText("3-30 characters, letters, numbers, underscore, and hyphen");
        usernameField.setMaxLength(30);

        emailField = new EmailField("Email");
        emailField.setRequired(true);
        emailField.setHelperText("We'll use this for account recovery");
        emailField.setMaxLength(255);

        passwordField = new PasswordField("Password");
        passwordField.setRequired(true);
        passwordField.setHelperText("Minimum 8 characters with uppercase, lowercase, digit, and special character");

        passwordConfirmationField = new PasswordField("Confirm Password");
        passwordConfirmationField.setRequired(true);

        // Create binder for form validation
        binder = new Binder<>(RegistrationForm.class);
        binder.forField(usernameField)
                .asRequired("Username is required")
                .bind(RegistrationForm::getUsername, RegistrationForm::setUsername);
        binder.forField(emailField)
                .asRequired("Email is required")
                .bind(RegistrationForm::getEmail, RegistrationForm::setEmail);
        binder.forField(passwordField)
                .asRequired("Password is required")
                .bind(RegistrationForm::getPassword, RegistrationForm::setPassword);
        binder.forField(passwordConfirmationField)
                .asRequired("Password confirmation is required")
                .bind(RegistrationForm::getPasswordConfirmation, RegistrationForm::setPasswordConfirmation);

        // Form layout
        var formLayout = new FormLayout();
        formLayout.add(usernameField, emailField, passwordField, passwordConfirmationField);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        formLayout.setMaxWidth("400px");

        // Buttons
        var registerButton = new Button("Register", event -> handleRegistration());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelButton = new Button("Cancel", event -> navigateToLogin());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        var buttonLayout = new HorizontalLayout(registerButton, cancelButton);
        buttonLayout.setMaxWidth("400px");
        buttonLayout.setWidthFull();

        // Login link
        var loginLink = new RouterLink("Already have an account? Login", LoginView.class);
        var loginLinkContainer = new Span(loginLink);
        loginLinkContainer.getStyle().set("margin-top", "1rem");

        // Add all components
        var formContainer = new VerticalLayout(
                title,
                formLayout,
                buttonLayout,
                loginLinkContainer
        );
        formContainer.setMaxWidth("400px");
        formContainer.setAlignItems(Alignment.STRETCH);
        formContainer.setPadding(true);

        add(formContainer);
    }

    private void handleRegistration() {
        try {
            var form = new RegistrationForm();
            binder.writeBean(form);

            // Call service to register user
            userService.registerUser(
                    form.getUsername(),
                    form.getEmail(),
                    form.getPassword(),
                    form.getPasswordConfirmation()
            );

            // Show success notification
            var notification = Notification.show(
                    "Registration successful! Please login with your credentials.",
                    5000,
                    Notification.Position.TOP_CENTER
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Navigate to login page
            navigateToLogin();

        } catch (ValidationException e) {
            log.debug("Form validation failed", e);
            Notification.show(
                    "Please check the form for errors",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (UsernameAlreadyExistsException e) {
            log.debug("Username already exists: {}", e.getMessage());
            usernameField.setInvalid(true);
            usernameField.setErrorMessage("Username already exists. Please choose another username.");
            Notification.show(
                    "Username already exists",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (EmailAlreadyExistsException e) {
            log.debug("Email already exists: {}", e.getMessage());
            emailField.setInvalid(true);
            emailField.setErrorMessage("This email address is already registered.");
            Notification.show(
                    "Email already registered",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (RegistrationException e) {
            log.debug("Registration failed: {}", e.getMessage());
            Notification.show(
                    "Registration failed: " + e.getMessage(),
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (Exception e) {
            log.error("Unexpected error during registration", e);
            Notification.show(
                    "An error occurred during registration. Please try again.",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void navigateToLogin() {
        UI.getCurrent().navigate(LoginView.class);
    }

    /**
     * Form bean for registration data binding.
     */
    public static class RegistrationForm {
        private String username;
        private String email;
        private String password;
        private String passwordConfirmation;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordConfirmation() {
            return passwordConfirmation;
        }

        public void setPasswordConfirmation(String passwordConfirmation) {
            this.passwordConfirmation = passwordConfirmation;
        }
    }
}
