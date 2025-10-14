package ch.martinelli.fun.kututipp.view;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * Login view for user authentication.
 * Implements UC-002: Login (placeholder for now).
 */
@AnonymousAllowed
@Route("login")
@PageTitle("Login - Kutu-Tipp")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm;

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        var title = new H1("Kutu-Tipp Login");

        loginForm = new LoginForm();
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        var registerLink = new RouterLink("Don't have an account? Register here", RegistrationView.class);

        add(title, loginForm, registerLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Show error message if login failed
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
