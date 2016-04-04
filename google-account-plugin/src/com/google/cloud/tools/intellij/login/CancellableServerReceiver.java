/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.tools.intellij.login;

import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.repackaged.com.google.common.base.Throwables;
import com.google.api.client.repackaged.org.mortbay.jetty.Connector;
import com.google.api.client.repackaged.org.mortbay.jetty.Request;
import com.google.api.client.repackaged.org.mortbay.jetty.Server;
import com.google.api.client.repackaged.org.mortbay.jetty.handler.AbstractHandler;
import com.google.cloud.tools.intellij.util.IntelliJPlatform;

import com.intellij.util.PlatformUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A cancellable Server Receiver.
 */
class CancellableServerReceiver implements VerificationCodeReceiver {
  private static final String CALLBACK_PATH = "/Callback";
  public static final String REQUEST_CANCELLED_STRING = "Request cancelled.";

  /** Server or {@code null} before {@link #getRedirectUri()}. */
  private Server server;

  /** Verification code or {@code null} for none. */
  String code;

  /** Error code or {@code null} for none. */
  String error;

  /** Lock on the code and error. */
  final Lock lock = new ReentrantLock();

  /** Condition for receiving an authorization response. */
  final Condition gotAuthorizationResponse = lock.newCondition();

  /** Port to use or {@code -1} to select an unused port in {@link #getRedirectUri()}. */
  private int port;

  /** Host name to use. */
  private final String host;

  /**
   * Constructor that starts the server on {@code "localhost"} selects an unused port.
   *
   * <p>
   * Use {@link Builder} if you need to specify any of the optional parameters.
   * </p>
   */
  public CancellableServerReceiver() {
    this("localhost", -1);
  }

  /**
   * Constructor.
   *
   * @param host Host name to use
   * @param port Port to use or {@code -1} to select an unused port
   */
  CancellableServerReceiver(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public String getRedirectUri() throws IOException {
    if (port == -1) {
      port = getUnusedPort();
    }
    server = new Server(port);
    for (Connector c : server.getConnectors()) {
      c.setHost(host);
    }
    server.addHandler(new CallbackHandler());
    try {
      server.start();
    } catch (Exception e) {
      Throwables.propagateIfPossible(e);
      throw new IOException(e);
    }
    return "http://" + host + ":" + port + CALLBACK_PATH;
  }

  @Override
  public String waitForCode() throws IOException, RequestCancelledException {
    lock.lock();
    try {
      while (code == null && error == null) {
        gotAuthorizationResponse.awaitUninterruptibly();
      }
      if (error != null) {
        // If the user cancels the request from Studio (Request Cancelled.), or they cancel the request from the auth page ('access_denied')
        // then return null and let the UI handle the cancellation.
        if (error.equals(REQUEST_CANCELLED_STRING) || error.equals("access_denied")) {
          throw new RequestCancelledException();
        }
        throw new IOException("User authorization failed (" + error + ")");
      }
      return code;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void stop() throws IOException {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        Throwables.propagateIfPossible(e);
        throw new IOException(e);
      }
      lock.lock();
      try {
        error = REQUEST_CANCELLED_STRING;
        code = null;
        gotAuthorizationResponse.signal();
      }
      finally {
        lock.unlock();
      }
      server = null;
    }
  }

  /** Returns the host name to use. */
  public String getHost() {
    return host;
  }

  /**
   * Returns the port to use or {@code -1} to select an unused port in {@link #getRedirectUri()}.
   */
  public int getPort() {
    return port;
  }

  private static int getUnusedPort() throws IOException {
    Socket s = new Socket();
    s.bind(null);
    try {
      return s.getLocalPort();
    } finally {
      s.close();
    }
  }

  /**
   * Builder.
   *
   * <p>
   * Implementation is not thread-safe.
   * </p>
   */
  public static final class Builder {

    /** Host name to use. */
    private String host = "localhost";

    /** Port to use or {@code -1} to select an unused port. */
    private int port = -1;

    /** Builds the {@link LocalServerReceiver}. */
    public CancellableServerReceiver build() {
      return new CancellableServerReceiver(host, port);
    }

    /** Returns the host name to use. */
    public String getHost() {
      return host;
    }

    /** Sets the host name to use. */
    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    /** Returns the port to use or {@code -1} to select an unused port. */
    public int getPort() {
      return port;
    }

    /** Sets the port to use or {@code -1} to select an unused port. */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }
  }

  /**
   * Jetty handler that takes the verifier token passed over from the OAuth provider and stashes it
   * where {@link #waitForCode} will find it.
   */
  class CallbackHandler extends AbstractHandler {
    @Override
    public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException {
      if (!CALLBACK_PATH.equals(target)) {
        return;
      }

      ((Request)request).setHandled(true);
      lock.lock();
      try {
        error = request.getParameter("error");
        code = request.getParameter("code");
        gotAuthorizationResponse.signal();
      }
      finally {
        lock.unlock();
      }

      response.sendRedirect(error == null ? getSuccessLandingPage() : getFailureLandingPage());
      response.flushBuffer();
    }

    @NotNull
    private String getFailureLandingPage() {
      IntelliJPlatform currentPlatform = IntelliJPlatform
          .fromPrefix(PlatformUtils.getPlatformPrefix());
      if (currentPlatform == IntelliJPlatform.ANDROID_STUDIO) {
        return LandingPages.ANDROID_STUDIO.getFailurePage();
      }
      return LandingPages.INTELLIJ.getFailurePage();
    }

    @NotNull
    private String getSuccessLandingPage() {
      IntelliJPlatform currentPlatform = IntelliJPlatform
          .fromPrefix(PlatformUtils.getPlatformPrefix());
      if (currentPlatform == IntelliJPlatform.ANDROID_STUDIO) {
        return LandingPages.ANDROID_STUDIO.getSuccessPage();
      }
      return LandingPages.INTELLIJ.getSuccessPage();
    }
  }
}
