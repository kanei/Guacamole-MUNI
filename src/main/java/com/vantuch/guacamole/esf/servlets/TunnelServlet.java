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
import com.vantuch.guacamole.esf.event.SessionListenerCollection;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.GuacamoleClientException;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.net.GuacamoleSocket;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.ConnectionGroup;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.event.TunnelCloseEvent;
import org.glyptodon.guacamole.net.event.TunnelConnectEvent;
import org.glyptodon.guacamole.net.event.listener.TunnelCloseListener;
import org.glyptodon.guacamole.net.event.listener.TunnelConnectListener;
import org.glyptodon.guacamole.protocol.GuacamoleClientInformation;
import org.glyptodon.guacamole.servlet.GuacamoleHTTPTunnelServlet;

/**
 * Connects users to a tunnel associated with the authorized connection having
 * the given ID.
 *
 * @author Michael Jumper
 */
public class TunnelServlet extends AuthenticatingHttpServlet {
  private static final Logger logger = Logger.getLogger(TunnelServlet.class.getName());
  
  /**
   * All supported identifier types.
   */
  private static enum IdentifierType {

    /**
     * The unique identifier of a connection.
     */
    CONNECTION("c/"),
    /**
     * The unique identifier of a connection group.
     */
    CONNECTION_GROUP("g/");
    /**
     * The prefix which precedes an identifier of this type.
     */
    final String PREFIX;

    /**
     * Defines an IdentifierType having the given prefix.
     *
     * @param prefix The prefix which will precede any identifier of this type,
     * thus differentiating it from other identifier types.
     */
    IdentifierType(String prefix) {
      PREFIX = prefix;
    }

    /**
     * Given an identifier, determines the corresponding identifier type.
     *
     * @param identifier The identifier whose type should be identified.
     * @return The identified identifier type.
     */
    static IdentifierType getType(String identifier) {

      // If null, no known identifier
      if (identifier == null) {
        return null;
      }

      // Connection identifiers
      if (identifier.startsWith(CONNECTION.PREFIX)) {
        return CONNECTION;
      }

      // Connection group identifiers
      if (identifier.startsWith(CONNECTION_GROUP.PREFIX)) {
        return CONNECTION_GROUP;
      }

      // Otherwise, unknown
      return null;

    }
  };

  @Override
  protected void authenticatedService(
          UserContext context,
          HttpServletRequest request, HttpServletResponse response)
          throws GuacamoleException {

    try {

      // If authenticated, respond as tunnel
      tunnelServlet.service(request, response);
    } catch (ServletException e) {
      logger.log(Level.INFO, "Error from tunnel (see previous log messages): {}", e);
    } catch (IOException e) {
      logger.log(Level.INFO, "I/O error from tunnel (see previous log messages): {}", e);
    }

  }

  /**
   * Notifies all listeners in the given collection that a tunnel has been
   * connected.
   *
   * @param listeners A collection of all listeners that should be notified.
   * @param context The UserContext associated with the current session.
   * @param credentials The credentials associated with the current session.
   * @param tunnel The tunnel being connected.
   * @return true if all listeners are allowing the tunnel to connect, or if
   * there are no listeners, and false if any listener is canceling the
   * connection. Note that once one listener cancels, no other listeners will
   * run.
   * @throws GuacamoleException If any listener throws an error while being
   * notified. Note that if any listener throws an error, the connect is
   * canceled, and no other listeners will run.
   */
  private boolean notifyConnect(Collection listeners, UserContext context,
          Credentials credentials, GuacamoleTunnel tunnel)
          throws GuacamoleException {

    // Build event for auth success
    TunnelConnectEvent event = new TunnelConnectEvent(context,
            credentials, tunnel);

    // Notify all listeners
    for (Object listener : listeners) {
      if (listener instanceof TunnelConnectListener) {

        // Cancel immediately if hook returns false
        if (!((TunnelConnectListener) listener).tunnelConnected(event)) {
          return false;
        }

      }
    }

    return true;

  }

  /**
   * Notifies all listeners in the given collection that a tunnel has been
   * closed.
   *
   * @param listeners A collection of all listeners that should be notified.
   * @param context The UserContext associated with the current session.
   * @param credentials The credentials associated with the current session.
   * @param tunnel The tunnel being closed.
   * @return true if all listeners are allowing the tunnel to close, or if there
   * are no listeners, and false if any listener is canceling the close. Note
   * that once one listener cancels, no other listeners will run.
   * @throws GuacamoleException If any listener throws an error while being
   * notified. Note that if any listener throws an error, the close is canceled,
   * and no other listeners will run.
   */
  private boolean notifyClose(Collection listeners, UserContext context,
          Credentials credentials, GuacamoleTunnel tunnel)
          throws GuacamoleException {

    // Build event for auth success
    TunnelCloseEvent event = new TunnelCloseEvent(context,
            credentials, tunnel);

    // Notify all listeners
    for (Object listener : listeners) {
      if (listener instanceof TunnelCloseListener) {

        // Cancel immediately if hook returns false
        if (!((TunnelCloseListener) listener).tunnelClosed(event)) {
          return false;
        }

      }
    }

    return true;

  }
  /**
   * Wrapped GuacamoleHTTPTunnelServlet which will handle all authenticated
   * requests.
   */
  private GuacamoleHTTPTunnelServlet tunnelServlet = new GuacamoleHTTPTunnelServlet() {
    @Override
    protected GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException {

      HttpSession httpSession = request.getSession(true);

      // Get listeners
      final SessionListenerCollection listeners;
      try {
        listeners = new SessionListenerCollection(httpSession);
      } catch (GuacamoleException e) {
        logger.log(Level.SEVERE, "Failed to retrieve listeners. Authentication canceled.", e);
        throw e;
      }


      // Get credentials
      final Credentials credentials = getCredentials(httpSession);

      // Get context
      final UserContext context = getUserContext(httpSession);

      // If no context or no credentials, not logged in
      if (context == null || credentials == null) {
        throw new GuacamoleSecurityException("Cannot connect - user not logged in.");
      }

      // Get client information
      GuacamoleClientInformation info = new GuacamoleClientInformation();

      // Set width if provided
      String width = request.getParameter("width");
      if (width != null) {
        info.setOptimalScreenWidth(Integer.parseInt(width));
      }

      // Set height if provided
      String height = request.getParameter("height");
      if (height != null) {
        info.setOptimalScreenHeight(Integer.parseInt(height));
      }

      // Add audio mimetypes
      String[] audio_mimetypes = request.getParameterValues("audio");
      if (audio_mimetypes != null) {
        info.getAudioMimetypes().addAll(Arrays.asList(audio_mimetypes));
      }

      // Add video mimetypes
      String[] video_mimetypes = request.getParameterValues("video");
      if (video_mimetypes != null) {
        info.getVideoMimetypes().addAll(Arrays.asList(video_mimetypes));
      }

      // Create connected socket from identifier
      GuacamoleSocket socket;

      // Get connection directory
      Directory<String, Connection> directory =
              context.getRootConnectionGroup().getConnectionDirectory();

      // In our situation, there is always only single connection per session, so no need to worry about other ones
      // or loading it from the session parameters of whatever
      Set<String> identifiers = directory.getIdentifiers();

      if (identifiers.isEmpty()) {
        throw new GuacamoleException("No identifiers were found for current session.");
      }

      String id = identifiers.iterator().next();

      Connection connection = directory.get(id);

      if (connection == null) {
        logger.log(Level.WARNING, "Connection id={0} not found.", id);
        throw new GuacamoleSecurityException("Requested connection is not authorized.");
      }

      // Connect socket
      socket = connection.connect(info);
      logger.log(Level.INFO, "Successful connection to {0}.", request.getRemoteAddr());

      // Associate socket with tunnel
      GuacamoleTunnel tunnel = new GuacamoleTunnel(socket) {
        @Override
        public void close() throws GuacamoleException {

          // Only close if not canceled
          if (!notifyClose(listeners, context, credentials, this)) {
            throw new GuacamoleException("Tunnel close canceled by listener.");
          }

          // Close if no exception due to listener
          super.close();

        }
      };

      // Notify listeners about connection
      if (!notifyConnect(listeners, context, credentials, tunnel)) {
        logger.info("Connection canceled by listener.");
        return null;
      }

      return tunnel;

    }
  };

  @Override
  protected boolean hasNewCredentials(HttpServletRequest request) {

    String query = request.getQueryString();
    if (query == null) {
      return false;
    }

    // Only connections are given new credentials
    return query.equals("connect");

  }
}
