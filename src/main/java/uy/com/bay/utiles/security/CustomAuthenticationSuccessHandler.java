package uy.com.bay.utiles.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uy.com.bay.utiles.data.Role;
import uy.com.bay.utiles.data.User;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final AuthenticatedUser authenticatedUser;
	private final AuthenticationSuccessHandler defaultSuccessHandler = new SavedRequestAwareAuthenticationSuccessHandler();

	public CustomAuthenticationSuccessHandler(@Lazy AuthenticatedUser authenticatedUser) {
		this.authenticatedUser = authenticatedUser;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		User user = authenticatedUser.get().orElse(null);

		if (user != null) {
			Set<Role> roles = user.getRoles();
			if (roles.contains(Role.ENCUESTADORES)) {
				response.sendRedirect("/utiles/surveyor-expense-request");
				return;
			}
		}
		defaultSuccessHandler.onAuthenticationSuccess(request, response, authentication);
	}
}
