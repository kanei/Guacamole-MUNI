package com.vantuch.guacamole.esf.servlets;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import com.vantuch.guacamole.esf.DrupalAuthenticationProvider;
import com.vantuch.guacamole.esf.event.SessionListenerCollection;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceNotFoundException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.event.AuthenticationFailureEvent;
import org.glyptodon.guacamole.net.event.AuthenticationSuccessEvent;
import org.glyptodon.guacamole.net.event.listener.AuthenticationFailureListener;
import org.glyptodon.guacamole.net.event.listener.AuthenticationSuccessListener;

/**
 * Abstract servlet which provides an authenticatedService() function that is
 * only called if the HTTP request is authenticated, or the current HTTP session
 * has already been authenticated.
 *
 * The user context is retrieved using the authentication provider defined in
 * guacamole.properties. The authentication provider has access to the request
 * and session, in addition to any submitted username and password, in order to
 * authenticate the user.
 *
 * The user context will be stored in the current HttpSession.
 *
 * Success and failure are logged.
 *
 * @author Michael Jumper
 */
public abstract class AuthenticatingHttpServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(AuthenticatingHttpServlet.class.getName());
  /**
   * The session attribute holding the current UserContext.
   */
  private static final String CONTEXT_ATTRIBUTE = "GUAC_CONTEXT";
  /**
   * The session attribute holding the credentials authorizing this session.
   */
  private static final String CREDENTIALS_ATTRIBUTE = "GUAC_CREDS";
  /**
   * The AuthenticationProvider to use to authenticate all requests.
   */
  private AuthenticationProvider authProvider;

  @Override
  public void init() throws ServletException {

    // Get auth provider instance
    authProvider = new DrupalAuthenticationProvider();
  }

  /**
   * Notifies all listeners in the given collection that authentication has
   * failed.
   *
   * @param listeners A collection of all listeners that should be notified.
   * @param credentials The credentials associated with the authentication
   * request that failed.
   */
  private void notifyFailed(Collection listeners, Credentials credentials) {

    // Build event for auth failure
    AuthenticationFailureEvent event = new AuthenticationFailureEvent(credentials);

    // Notify all listeners
    for (Object listener : listeners) {
      try {
        if (listener instanceof AuthenticationFailureListener) {
          ((AuthenticationFailureListener) listener).authenticationFailed(event);
        }
      } catch (GuacamoleException e) {
        logger.log(Level.SEVERE, "Error notifying AuthenticationFailureListener.", e);
      }
    }

  }

  /**
   * Notifies all listeners in the given collection that authentication was
   * successful.
   *
   * @param listeners A collection of all listeners that should be notified.
   * @param context The UserContext created as a result of authentication
   * success.
   * @param credentials The credentials associated with the authentication
   * request that succeeded.
   * @return true if all listeners are allowing the authentication success, or
   * if there are no listeners, and false if any listener is canceling the
   * authentication success. Note that once one listener cancels, no other
   * listeners will run.
   * @throws GuacamoleException If any listener throws an error while being
   * notified. Note that if any listener throws an error, the success is
   * canceled, and no other listeners will run.
   */
  private boolean notifySuccess(Collection listeners, UserContext context,
          Credentials credentials) throws GuacamoleException {

    // Build event for auth success
    AuthenticationSuccessEvent event =
            new AuthenticationSuccessEvent(context, credentials);

    // Notify all listeners
    for (Object listener : listeners) {
      if (listener instanceof AuthenticationSuccessListener) {

        // Cancel immediately if hook returns false
        if (!((AuthenticationSuccessListener) listener).authenticationSucceeded(event)) {
          return false;
        }

      }
    }

    return true;

  }

  /**
   * Sends an error on the given HTTP response with the given integer error
   * code.
   *
   * @param response The HTTP response to use to send the error.
   * @param code The HTTP status code of the error.
   * @param message A human-readable message that can be presented to the user.
   * @throws ServletException If an error prevents sending of the error code.
   */
  private void sendError(HttpServletResponse response, int code,
          String message) throws ServletException {

    try {

      // If response not committed, send error code
      if (!response.isCommitted()) {
        response.addHeader("Guacamole-Error-Message", message);
        response.sendError(code);
      }

    } catch (IOException ioe) {

      // If unable to send error at all due to I/O problems,
      // rethrow as servlet exception
      throw new ServletException(ioe);

    }

  }

  /**
   * Returns the credentials associated with the given session.
   *
   * @param session The session to retrieve credentials from.
   * @return The credentials associated with the given session.
   */
  protected Credentials getCredentials(HttpSession session) {
    return (Credentials) session.getAttribute(CREDENTIALS_ATTRIBUTE);
  }

  /**
   * Returns the UserContext associated with the given session.
   *
   * @param session The session to retrieve UserContext from.
   * @return The UserContext associated with the given session.
   */
  protected UserContext getUserContext(HttpSession session) {
    return (UserContext) session.getAttribute(CONTEXT_ATTRIBUTE);
  }

  /**
   * Returns whether the request given has updated credentials. If this function
   * returns false, the UserContext will not be updated.
   *
   * @param request The request to check for credentials.
   * @return true if the request contains credentials, false otherwise.
   */
  protected boolean hasNewCredentials(HttpServletRequest request) {
    String username = request.getParameter("username");

    return username != null;
  }

  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
          throws IOException, ServletException {

    // Set character encoding to UTF-8 if it's not already set
    if (request.getCharacterEncoding() == null) {
      try {
        request.setCharacterEncoding("UTF-8");
      } catch (UnsupportedEncodingException exception) {
        throw new ServletException(exception);
      }
    }

    try {

      // Obtain context from session
      HttpSession httpSession = request.getSession(true);
      UserContext context = getUserContext(httpSession);

      // If new credentials present, update/create context
      if (hasNewCredentials(request)) {

        // Retrieve username and password from parms
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Build credentials object
        Credentials credentials = new Credentials();
        credentials.setSession(httpSession);
        credentials.setRequest(request);
        credentials.setUsername(username);
        credentials.setPassword(password);

        SessionListenerCollection listeners = new SessionListenerCollection(httpSession);

        // If no cached context, attempt to get new context
        if (context == null) {

          context = authProvider.getUserContext(credentials);

          // Log successful authentication
          if (context != null) {
            logger.log(Level.INFO, "User {0} successfully authenticated.",
                    context.self().getUsername());
          }

        } // Otherwise, update existing context
        else {
          context = authProvider.updateUserContext(context, credentials);
        }

        // If auth failed, notify listeners
        if (context == null) {
          logger.log(Level.WARNING, "Authentication attempt for user {0} failed.",
                  credentials.getUsername());

          notifyFailed(listeners, credentials);
        } // If auth succeeded, notify and check with listeners
        else if (!notifySuccess(listeners, context, credentials)) {
          logger.info("Successful authentication canceled by hook.");
          context = null;
        } // If auth still OK, associate context with session
        else {
          httpSession.setAttribute(CONTEXT_ATTRIBUTE, context);
          httpSession.setAttribute(CREDENTIALS_ATTRIBUTE, credentials);
        }

      } // end if credentials present

      // If no context, no authorizaton present
      if (context == null) {
        throw new GuacamoleSecurityException("Not authenticated");
      }

      // Allow servlet to run now that authentication has been validated
      authenticatedService(context, request, response);

    } // Catch any thrown guacamole exception and attempt to pass within the
    // HTTP response, logging each error appropriately.
    catch (GuacamoleSecurityException e) {
      logger.log(Level.WARNING, "Permission denied: ", e);
      sendError(response, HttpServletResponse.SC_FORBIDDEN,
              "Permission denied.");
    } catch (GuacamoleResourceNotFoundException e) {
      logger.log(Level.INFO, "Resource not found: ", e);
      sendError(response, HttpServletResponse.SC_NOT_FOUND,
              e.getMessage());
    } catch (GuacamoleException e) {
      logger.log(Level.SEVERE, "Internal server error.", e);
      sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Internal server error: " + e.getMessage());
    }

  }

  /**
   * Function called after the credentials given in the request (if any) are
   * authenticated. If the current session is not associated with valid
   * credentials, this function will not be called.
   *
   * @param context The current UserContext.
   * @param request The HttpServletRequest being serviced.
   * @param response An HttpServletResponse which controls the HTTP response of
   * this servlet.
   *
   * @throws GuacamoleException If an error occurs that interferes with the
   * normal operation of this servlet.
   */
  protected abstract void authenticatedService(
          UserContext context,
          HttpServletRequest request, HttpServletResponse response)
          throws GuacamoleException;
}
