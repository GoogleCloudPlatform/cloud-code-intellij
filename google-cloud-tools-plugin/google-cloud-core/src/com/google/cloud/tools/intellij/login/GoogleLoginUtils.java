/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.cloud.tools.intellij.service.PluginInfoService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import java.awt.Image;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;

/** Utility methods of Google Login. */
public final class GoogleLoginUtils {

  public static final Logger LOG = Logger.getInstance(GoogleLoginUtils.class);
  public static final int DEFAULT_PICTURE_SIZE = 96;

  /**
   * Gets the profile picture that corresponds to the {@code userInfo} and sets it on the provided
   * {@code pictureCallback}.
   *
   * @param userInfo the class to be parsed
   * @param pictureCallback the user image will be set on this callback
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static void provideUserPicture(Userinfoplus userInfo, Consumer<Image> pictureCallback) {
    // set the size of the image before it is served
    String urlString = userInfo.getPicture() + "?sz=" + DEFAULT_PICTURE_SIZE;
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException ex) {
      LOG.warn(String.format("The picture URL: %s,  is not a valid URL string.", urlString), ex);
      return;
    }

    final URL newUrl = url;

    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () -> {
              try {
                pictureCallback.accept(ImageIO.read(newUrl));
              } catch (IOException exception) {
                pictureCallback.accept(null);
              }
            });
  }

  /** Sets the user info on the callback. */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static void getUserInfo(
      @NotNull final Credential credential, final IUserPropertyCallback<Userinfoplus> callback) {
    final Oauth2 userInfoService =
        new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
            .setApplicationName(ServiceManager.getService(PluginInfoService.class).getUserAgent())
            .build();

    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () -> {
              Userinfoplus userInfo = null;
              try {
                userInfo = userInfoService.userinfo().get().execute();
              } catch (IOException ex) {
                // The core IDE functionality still works, so this does
                // not affect anything right now. The user will receive
                // error messages when they attempt to do something that
                // requires a logged in state.
                LOG.warn("Error retrieving user information.", ex);
              }

              if (userInfo != null && userInfo.getId() != null) {
                callback.setProperty(userInfo);
              } else {
                callback.setProperty(null);
              }
            });
  }

  /**
   * Opens an error dialog with the specified title. Ensures that the error dialog is opened on the
   * UI thread.
   *
   * @param message The message to be displayed.
   * @param title The title of the error dialog to be displayed
   */
  public static void showErrorDialog(final String message, @NotNull final String title) {
    if (ApplicationManager.getApplication().isDispatchThread()) {
      Messages.showErrorDialog(message, title);
    } else {
      ApplicationManager.getApplication()
          .invokeLater(
              () -> Messages.showErrorDialog(message, title), ModalityState.defaultModalityState());
    }
  }
}
